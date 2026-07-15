# List + Mutation Feature Kit

**You need this if:** Path C list sync + POST/PUT/DELETE without a hand-written repository class.

## Generator

```bash
pip install pyyaml
python tools/feature-generator/generate.py \
  --config tools/feature-generator/examples/product.yaml \
  --output shared/src/commonMain/kotlin \
  --package com.yourapp.feature \
  --sql
```

From OpenAPI:

```bash
python tools/openapi-import/import_openapi.py \
  --spec openapi.json \
  --feature Product \
  --path /products \
  --out product.yaml \
  --generate \
  --output shared/src/commonMain/kotlin \
  --package com.yourapp.feature
```

See `tools/feature-generator/README.md` and `tools/openapi-import/README.md`.

## Runtime install

```kotlin
registry.installRestResourceFeature(
    RestResourceFeatureConfig<Product, ProductDto>(
        name = "products",
        path = "/products",
        observeLocal = { store.observeAll() },
        countLocal = { store.count() },
        replaceLocal = { dtos -> store.replaceAll(dtos.map { it.toDomain() }) },
    ),
)
```

## Use

```kotlin
val api = KmpSdk.get<RestResourceApi<Product>>()
api.refresh()
api.observeAll()
api.create(CreateProductBody(...))
api.update(id, UpdateProductBody(...))
api.delete(id)
```

- List-only: `installRestListFeature(...)`  
- Single mutation: `RestMutationUseCase.create(...)`  
- Sync UI: `KmpSdk.syncStatus.observe("products")` — see [offline-sync.md](offline-sync.md)
