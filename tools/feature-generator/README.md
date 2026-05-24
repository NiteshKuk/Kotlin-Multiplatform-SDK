# KmpSDK Feature Generator

Generates feature module + remote data source scaffold from YAML.

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
