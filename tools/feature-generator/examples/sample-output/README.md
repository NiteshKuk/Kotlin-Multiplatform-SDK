# Sample generator shape

This folder documents expected output. Generate real files into your app module:

```bash
python tools/feature-generator/generate.py \
  --config tools/feature-generator/examples/product.yaml \
  --output shared/src/commonMain/kotlin \
  --package com.yourapp.feature \
  --sql
```

Expected files under `…/product/`:

- `ProductModels.kt` — `Product`, `ProductDto`, `CreateProductBody`, `UpdateProductBody`
- `InMemoryProductStore.kt` — observe / count / replace
- `ProductFeatureModule.kt` — `installRestResourceFeature(...)`
- `FEATURE.md` — install + usage
- `sqldelight/Product.sq` — only with `--sql`

Then:

```kotlin
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    install(ProductFeatureModule)
}

val api = KmpSdk.get<RestResourceApi<Product>>()
api.refresh()
api.create(CreateProductBody(...))
api.update(id, UpdateProductBody(...))
api.delete(id)
```
