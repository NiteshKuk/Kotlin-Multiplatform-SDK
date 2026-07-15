# OpenAPI → Feature Kit import

Converts an OpenAPI 3 `GET /collection` schema into Feature Kit YAML, optionally running the feature generator.

```bash
pip install pyyaml

python tools/openapi-import/import_openapi.py \
  --spec openapi.json \
  --feature Product \
  --path /products \
  --out tools/feature-generator/examples/product.generated.yaml \
  --generate \
  --output shared/src/commonMain/kotlin \
  --package com.myapp.feature \
  --sql
```
