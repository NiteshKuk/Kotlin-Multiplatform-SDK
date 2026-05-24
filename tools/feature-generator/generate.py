#!/usr/bin/env python3
"""KmpSDK feature scaffold generator.

Usage:
  python tools/feature-generator/generate.py \
    --config tools/feature-generator/examples/order.yaml \
    --output shared/src/commonMain/kotlin \
    --package com.yourapp.feature

Example order.yaml:
  feature: Order
  endpoint: /orders
  sync: list
  fields:
    - name: id
      type: String
    - name: title
      type: String
    - name: amount
      type: Double
"""

from __future__ import annotations

import argparse
import pathlib
import textwrap

try:
    import yaml
except ImportError:
    yaml = None


TEMPLATE_MODULE = '''\
object {Feature}FeatureModule : KmpSdkModule {{
    override fun register(registry: KmpSdkRegistry) {{
        registry.register<{Feature}Repository> {{ ctx ->
            {Feature}RepositoryImpl(
                local = {Feature}LocalDataSource(registry.resolve()),
                remote = {Feature}RemoteDataSource(ctx.networkClient),
                ctx = ctx,
            )
        }}
        registry.register<Get{Feature}sUseCase> {{ Get{Feature}sUseCase(registry.resolve()) }}
        registry.registerSyncTarget("{feature}s", registry.resolve<{Feature}Repository>())
    }}
}}
'''

TEMPLATE_REMOTE = '''\
class {Feature}RemoteDataSource(
    private val networkClient: KmpNetworkClient,
) : RemoteListDataSource<{Feature}Dto> {{
    override suspend fun fetchAll(): KmpSdkResult<List<{Feature}Dto>> =
        networkClient.get("{endpoint}")
}}
'''


def load_config(path: pathlib.Path) -> dict:
    text = path.read_text(encoding="utf-8")
    if yaml is not None:
        return yaml.safe_load(text)
    # minimal fallback parser for single-feature yaml
    feature = None
    endpoint = "/items"
    for line in text.splitlines():
        line = line.strip()
        if line.startswith("feature:"):
            feature = line.split(":", 1)[1].strip()
        if line.startswith("endpoint:"):
            endpoint = line.split(":", 1)[1].strip()
    if not feature:
        raise SystemExit("Could not parse config. Install PyYAML: pip install pyyaml")
    return {"feature": feature, "endpoint": endpoint}


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate KmpSDK feature scaffold")
    parser.add_argument("--config", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument(
        "--package",
        default="com.yourapp.feature",
        help="Base Kotlin package for generated files (default: com.yourapp.feature)",
    )
    args = parser.parse_args()

    cfg = load_config(pathlib.Path(args.config))
    feature = cfg["feature"]
    endpoint = cfg.get("endpoint", f"/{feature.lower()}s")
    pkg = f"{args.package.rstrip('.')}.{feature.lower()}"
    base = pathlib.Path(args.output) / pkg.replace(".", "/")
    base.mkdir(parents=True, exist_ok=True)

    (base / f"{feature}FeatureModule.kt").write_text(
        TEMPLATE_MODULE.format(Feature=feature, feature=feature.lower()),
        encoding="utf-8",
    )
    remote_dir = base / "remote"
    remote_dir.mkdir(exist_ok=True)
    (remote_dir / f"{feature}RemoteDataSource.kt").write_text(
        TEMPLATE_REMOTE.format(Feature=feature, endpoint=endpoint),
        encoding="utf-8",
    )

    print(f"Generated {feature} feature under {base}")


if __name__ == "__main__":
    main()
