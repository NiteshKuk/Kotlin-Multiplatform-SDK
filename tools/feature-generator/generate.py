#!/usr/bin/env python3
"""KmpSDK List + Mutation Feature Kit generator.

Generates a compile-ready Path C feature module:
  - domain + DTO models
  - in-memory local store (works without host SQLDelight wiring)
  - optional SQLDelight .sq schema
  - FeatureModule using installRestResourceFeature (GET + POST/PUT/DELETE)

Usage:
  python tools/feature-generator/generate.py \\
    --config tools/feature-generator/examples/product.yaml \\
    --output shared/src/commonMain/kotlin \\
    --package com.yourapp.feature

  # also emit SQLDelight schema:
  python tools/feature-generator/generate.py ... --sql
"""

from __future__ import annotations

import argparse
import pathlib
import re
import textwrap

try:
    import yaml
except ImportError:
    yaml = None

KOTLIN_TYPES = {
    "String": "String",
    "string": "String",
    "Int": "Int",
    "int": "Int",
    "Long": "Long",
    "long": "Long",
    "Double": "Double",
    "double": "Double",
    "Boolean": "Boolean",
    "boolean": "Boolean",
    "Float": "Float",
    "float": "Float",
}

SQL_TYPES = {
    "String": "TEXT",
    "Int": "INTEGER",
    "Long": "INTEGER",
    "Double": "REAL",
    "Float": "REAL",
    "Boolean": "INTEGER",
}


def load_config(path: pathlib.Path) -> dict:
    text = path.read_text(encoding="utf-8")
    if yaml is not None:
        data = yaml.safe_load(text)
        if not isinstance(data, dict) or "feature" not in data:
            raise SystemExit(f"Invalid config: {path}")
        return data
    raise SystemExit("PyYAML required. Install with: pip install pyyaml")


def normalize_fields(raw_fields: list | None) -> list[dict]:
    if not raw_fields:
        return [
            {"name": "id", "type": "String", "primaryKey": True},
            {"name": "title", "type": "String"},
        ]
    fields = []
    for item in raw_fields:
        name = item["name"]
        ktype = KOTLIN_TYPES.get(str(item.get("type", "String")), None)
        if ktype is None:
            raise SystemExit(f"Unsupported field type for '{name}': {item.get('type')}")
        fields.append(
            {
                "name": name,
                "type": ktype,
                "json": item.get("json", name),
                "primaryKey": bool(item.get("primaryKey", name == "id")),
            }
        )
    if not any(f["primaryKey"] for f in fields):
        fields[0]["primaryKey"] = True
    return fields


def kotlin_props(fields: list[dict], serial_name: bool = False) -> str:
    lines = []
    for f in fields:
        prefix = f'    @SerialName("{f["json"]}")\n' if serial_name and f["json"] != f["name"] else ""
        lines.append(f"{prefix}    val {f['name']}: {f['type']},")
    return "\n".join(lines)


def mapper_args(fields: list[dict], src: str = "this") -> str:
    return ",\n".join(f"        {f['name']} = {src}.{f['name']}" for f in fields)


def sql_columns(fields: list[dict]) -> str:
    lines = []
    for f in fields:
        sql_t = SQL_TYPES[f["type"]]
        pk = " PRIMARY KEY" if f["primaryKey"] else ""
        nullability = " NOT NULL"
        lines.append(f"  {f['name']} {sql_t}{nullability}{pk}")
    return ",\n".join(lines)


def insert_columns(fields: list[dict]) -> str:
    names = ", ".join(f["name"] for f in fields)
    qs = ", ".join("?" for _ in fields)
    return names, qs


def validate_feature(name: str) -> str:
    if not re.fullmatch(r"[A-Z][A-Za-z0-9]*", name):
        raise SystemExit(
            f"feature must be PascalCase identifier (e.g. Product), got: {name}"
        )
    return name


def render_models(package: str, feature: str, fields: list[dict]) -> str:
    needs_serial_name = any(f["json"] != f["name"] for f in fields)
    serial_import = (
        "import kotlinx.serialization.SerialName\n" if needs_serial_name else ""
    )
    return f'''\
package {package}

{serial_import}import kotlinx.serialization.Serializable

/** Domain model for {feature} (UI / repository). */
data class {feature}(
{kotlin_props(fields)}
)

/** API DTO for {feature} list/detail responses. */
@Serializable
data class {feature}Dto(
{kotlin_props(fields, serial_name=needs_serial_name)}
)

/** POST body — same fields as domain by default; edit as needed. */
@Serializable
data class Create{feature}Body(
{kotlin_props(fields, serial_name=needs_serial_name)}
)

/** PUT/PATCH body — same fields as domain by default; edit as needed. */
@Serializable
data class Update{feature}Body(
{kotlin_props(fields, serial_name=needs_serial_name)}
)

fun {feature}Dto.toDomain(): {feature} = {feature}(
{mapper_args(fields)}
)

fun {feature}.toDto(): {feature}Dto = {feature}Dto(
{mapper_args(fields)}
)

fun Create{feature}Body.toDomain(): {feature} = {feature}(
{mapper_args(fields)}
)
'''

def render_store(package: str, feature: str) -> str:
    return f'''\
package {package}

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory Path C local store for {feature}.
 * Swap for SQLDelight using the generated .sq schema when ready.
 */
class InMemory{feature}Store {{
    private val items = MutableStateFlow<List<{feature}>>(emptyList())

    fun observeAll(): Flow<List<{feature}>> = items.asStateFlow()

    suspend fun count(): Long = items.value.size.toLong()

    suspend fun replaceAll(next: List<{feature}>) {{
        items.value = next
    }}

    suspend fun upsert(item: {feature}, idSelector: ({feature}) -> String) {{
        items.update {{ current ->
            val id = idSelector(item)
            val without = current.filterNot {{ idSelector(it) == id }}
            without + item
        }}
    }}

    suspend fun remove(id: String, idSelector: ({feature}) -> String) {{
        items.update {{ current -> current.filterNot {{ idSelector(it) == id }} }}
    }}
}}
'''


def render_module(
    package: str,
    feature: str,
    endpoint: str,
    resource_name: str,
    pk_name: str,
    mutations: dict,
) -> str:
    create_path = mutations.get("create", {}).get("path", endpoint)
    update_path = mutations.get("update", {}).get("path", f"{endpoint}/{{id}}")
    delete_path = mutations.get("delete", {}).get("path", f"{endpoint}/{{id}}")
    enable_mutations = bool(mutations.get("enabled", True))

    mutation_cfg = ""
    if enable_mutations:
        mutation_cfg = f'''
            createPath = "{create_path}",
            updatePath = "{update_path}",
            deletePath = "{delete_path}",
            refreshAfterMutation = true,'''

    return f'''\
package {package}

import com.kmpsdk.core.di.KmpSdkModule
import com.kmpsdk.core.di.KmpSdkRegistry
import com.kmpsdk.data.rest.RestResourceFeatureConfig
import com.kmpsdk.data.rest.installRestResourceFeature

/**
 * Generated List + Mutation Feature Kit module for {feature}.
 *
 * Install:
 * ```
 * KmpSdk.init(this) {{
 *     baseUrl = "https://api.example.com"
 *     install({feature}FeatureModule)
 * }}
 * ```
 *
 * Resolve:
 * ```
 * val api = KmpSdk.get<RestResourceApi<{feature}>>()
 * api.refresh()
 * api.create(Create{feature}Body(...))
 * api.update(id, Update{feature}Body(...))
 * api.delete(id)
 * ```
 */
object {feature}FeatureModule : KmpSdkModule {{
    override fun register(registry: KmpSdkRegistry) {{
        val store = InMemory{feature}Store()
        registry.installRestResourceFeature(
            RestResourceFeatureConfig<{feature}, {feature}Dto>(
                name = "{resource_name}",
                path = "{endpoint}",
                observeLocal = {{ store.observeAll() }},
                countLocal = {{ store.count() }},
                replaceLocal = {{ dtos -> store.replaceAll(dtos.map {{ it.toDomain() }}) }},{mutation_cfg}
            ),
        )
    }}
}}

// Primary key field used by host apps when calling update/delete: {pk_name}
'''


def render_sql(feature: str, fields: list[dict]) -> str:
    cols = sql_columns(fields)
    names, qs = insert_columns(fields)
    entity = f"{feature}Entity"
    pk = next(f["name"] for f in fields if f["primaryKey"])
    return f'''\
CREATE TABLE {entity} (
{cols}
);

selectAll:
SELECT * FROM {entity};

countAll:
SELECT COUNT(*) FROM {entity};

deleteAll:
DELETE FROM {entity};

insert:
INSERT INTO {entity}({names}) VALUES ({qs});

deleteById:
DELETE FROM {entity} WHERE {pk} = ?;
'''


def render_readme_snip(feature: str, package: str, endpoint: str) -> str:
    return textwrap.dedent(
        f"""\
        # {feature} feature (generated)

        Package: `{package}`
        Endpoint: `{endpoint}`

        ## Install

        ```kotlin
        KmpSdk.init(this) {{
            baseUrl = "https://api.example.com"
            install({feature}FeatureModule)
        }}
        ```

        ## Use

        ```kotlin
        val api = KmpSdk.get<com.kmpsdk.data.rest.RestResourceApi<{feature}>>()

        // GET list → local store
        api.refresh()
        api.observeAll()

        // POST
        api.create(Create{feature}Body(/* fields */))

        // PUT
        api.update(id = "...", body = Update{feature}Body(/* fields */))

        // DELETE
        api.delete(id = "...")
        ```

        Replace `InMemory{feature}Store` with SQLDelight using `{feature}.sq` when you need durable storage.
        """
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate KmpSDK List + Mutation Feature Kit scaffold")
    parser.add_argument("--config", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument(
        "--package",
        default="com.yourapp.feature",
        help="Base Kotlin package (feature name is appended)",
    )
    parser.add_argument(
        "--sql",
        action="store_true",
        help="Also generate SQLDelight .sq schema under sqldelight/",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print planned files without writing",
    )
    args = parser.parse_args()

    cfg = load_config(pathlib.Path(args.config))
    feature = validate_feature(str(cfg["feature"]))
    endpoint = str(cfg.get("endpoint", f"/{feature.lower()}s"))
    fields = normalize_fields(cfg.get("fields"))
    mutations = cfg.get("mutations") or {"enabled": True}
    if isinstance(mutations, bool):
        mutations = {"enabled": mutations}

    resource_name = str(cfg.get("name", f"{feature.lower()}s"))
    pk_name = next(f["name"] for f in fields if f["primaryKey"])

    pkg = f"{args.package.rstrip('.')}.{feature.lower()}"
    base = pathlib.Path(args.output) / pkg.replace(".", "/")

    files = {
        base / f"{feature}Models.kt": render_models(pkg, feature, fields),
        base / f"InMemory{feature}Store.kt": render_store(pkg, feature),
        base / f"{feature}FeatureModule.kt": render_module(
            pkg, feature, endpoint, resource_name, pk_name, mutations
        ),
        base / "FEATURE.md": render_readme_snip(feature, pkg, endpoint),
    }

    if args.sql:
        sql_dir = base / "sqldelight"
        pk = next(f for f in fields if f["primaryKey"])
        # ensure deleteById uses pk
        files[sql_dir / f"{feature}.sq"] = render_sql(feature, fields)

    if args.dry_run:
        for path in files:
            print(f"Would write {path}")
        return

    for path, content in files.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        print(f"Wrote {path}")

    print()
    print(f"Generated {feature} List + Mutation Feature Kit under {base}")
    print(f"Next: install({feature}FeatureModule) inside KmpSdk.init {{ }}")


if __name__ == "__main__":
    main()
