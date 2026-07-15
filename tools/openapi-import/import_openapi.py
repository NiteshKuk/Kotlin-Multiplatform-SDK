#!/usr/bin/env python3
"""Import OpenAPI 3 spec → Feature Kit YAML (+ optional generate.py call).

Usage:
  python tools/openapi-import/import_openapi.py \\
    --spec https://api.example.com/openapi.json \\
    --feature Product \\
    --path /products \\
    --out tools/feature-generator/examples/product.generated.yaml

  # also run feature generator:
  python tools/openapi-import/import_openapi.py ... --generate \\
    --output shared/src/commonMain/kotlin \\
    --package com.myapp.feature
"""

from __future__ import annotations

import argparse
import json
import pathlib
import subprocess
import sys
import urllib.request

try:
    import yaml
except ImportError:
    yaml = None

OPENAPI_TYPE_MAP = {
    "string": "String",
    "integer": "Long",
    "number": "Double",
    "boolean": "Boolean",
}


def load_spec(path_or_url: str) -> dict:
    if path_or_url.startswith("http://") or path_or_url.startswith("https://"):
        with urllib.request.urlopen(path_or_url) as resp:
            text = resp.read().decode("utf-8")
    else:
        text = pathlib.Path(path_or_url).read_text(encoding="utf-8")
    if path_or_url.endswith(".yaml") or path_or_url.endswith(".yml") or text.lstrip().startswith("openapi:"):
        if yaml is None:
            raise SystemExit("PyYAML required for YAML specs: pip install pyyaml")
        return yaml.safe_load(text)
    return json.loads(text)


def resolve_ref(spec: dict, ref: str) -> dict:
    # #/components/schemas/Product
    parts = ref.lstrip("#/").split("/")
    node: dict | list | str = spec
    for part in parts:
        node = node[part]  # type: ignore[index]
    assert isinstance(node, dict)
    return node


def schema_properties(spec: dict, schema: dict) -> list[dict]:
    if "$ref" in schema:
        schema = resolve_ref(spec, schema["$ref"])
    props = schema.get("properties") or {}
    required = set(schema.get("required") or [])
    fields = []
    for name, prop in props.items():
        if "$ref" in prop:
            # nested object → String JSON fallback
            ktype = "String"
        else:
            ktype = OPENAPI_TYPE_MAP.get(prop.get("type", "string"), "String")
        fields.append(
            {
                "name": name,
                "type": ktype,
                "primaryKey": name == "id" or (name in required and name.endswith("Id") and name == list(props)[0]),
            }
        )
    if fields and not any(f.get("primaryKey") for f in fields):
        fields[0]["primaryKey"] = True
    return fields


def find_schema_for_path(spec: dict, path: str) -> dict | None:
    paths = spec.get("paths") or {}
    item = paths.get(path) or {}
    get_op = item.get("get") or {}
    content = (
        (((get_op.get("responses") or {}).get("200") or {}).get("content") or {})
        .get("application/json")
        or {}
    )
    schema = content.get("schema") or {}
    if schema.get("type") == "array":
        return schema.get("items")
    return schema or None


def to_yaml(feature: str, endpoint: str, fields: list[dict]) -> str:
    if yaml is not None:
        data = {
            "feature": feature,
            "endpoint": endpoint,
            "name": f"{feature.lower()}s",
            "fields": fields,
            "mutations": {
                "enabled": True,
                "create": {"method": "POST", "path": endpoint},
                "update": {"method": "PUT", "path": f"{endpoint}/{{id}}"},
                "delete": {"method": "DELETE", "path": f"{endpoint}/{{id}}"},
            },
        }
        return yaml.safe_dump(data, sort_keys=False)
    # minimal fallback
    lines = [
        f"feature: {feature}",
        f"endpoint: {endpoint}",
        f"name: {feature.lower()}s",
        "fields:",
    ]
    for f in fields:
        lines.append(f"  - name: {f['name']}")
        lines.append(f"    type: {f['type']}")
        if f.get("primaryKey"):
            lines.append("    primaryKey: true")
    lines += [
        "mutations:",
        "  enabled: true",
        "  create:",
        f"    path: {endpoint}",
        "  update:",
        f"    path: {endpoint}/{{id}}",
        "  delete:",
        f"    path: {endpoint}/{{id}}",
        "",
    ]
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser(description="OpenAPI → KmpSDK Feature Kit YAML")
    parser.add_argument("--spec", required=True, help="OpenAPI file path or URL")
    parser.add_argument("--feature", required=True, help="PascalCase feature name")
    parser.add_argument("--path", required=True, help="REST collection path, e.g. /products")
    parser.add_argument("--out", required=True, help="Output YAML path")
    parser.add_argument("--generate", action="store_true", help="Also run feature-generator")
    parser.add_argument("--output", help="Kotlin output root for --generate")
    parser.add_argument("--package", default="com.yourapp.feature")
    parser.add_argument("--sql", action="store_true")
    args = parser.parse_args()

    spec = load_spec(args.spec)
    schema = find_schema_for_path(spec, args.path)
    if not schema:
        raise SystemExit(f"Could not find JSON schema for GET {args.path}")
    fields = schema_properties(spec, schema)
    text = to_yaml(args.feature, args.path, fields)
    out = pathlib.Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(text, encoding="utf-8")
    print(f"Wrote {out}")

    if args.generate:
        if not args.output:
            raise SystemExit("--output is required with --generate")
        gen = pathlib.Path(__file__).resolve().parents[1] / "feature-generator" / "generate.py"
        cmd = [
            sys.executable,
            str(gen),
            "--config",
            str(out),
            "--output",
            args.output,
            "--package",
            args.package,
        ]
        if args.sql:
            cmd.append("--sql")
        subprocess.check_call(cmd)


if __name__ == "__main__":
    main()
