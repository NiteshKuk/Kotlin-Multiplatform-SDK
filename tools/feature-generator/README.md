# KmpSDK Feature Generator

Generates feature module + remote data source scaffold from YAML.

**Integration path:** [Path C (full offline-first)](../../README.md#path-c--full-offline-first-your-sql) only. For online-only features, use `KmpSdk.networkClient` directly — see [Path A](../../README.md#path-a--online-only-no-your-sql).

## Requirements

```bash
pip install pyyaml
```

## Usage

```bash
python tools/feature-generator/generate.py \
  --config tools/feature-generator/examples/order.yaml \
  --output shared/src/commonMain/kotlin \
  --package com.yourapp.feature
```

## Output

- `{Feature}FeatureModule.kt`
- `remote/{Feature}RemoteDataSource.kt`

Copy the generated files into your host shared module and complete local SQLDelight tables, mappers, local data source, and repository wiring using the integration guide in the root `README.md`.
