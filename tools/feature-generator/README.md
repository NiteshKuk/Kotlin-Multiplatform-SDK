# KmpSDK List + Mutation Feature Kit generator

Generates a **ready-to-install** offline list feature with GET sync **and** POST / PUT / DELETE helpers.

Uses SDK APIs:

- `installRestResourceFeature` → list sync + `RestResourceApi`
- In-memory local store (swap for SQLDelight later)
- Optional `.sq` schema via `--sql`

## Requirements

```bash
pip install pyyaml
```

## Usage

```bash
python tools/feature-generator/generate.py \
  --config tools/feature-generator/examples/product.yaml \
  --output shared/src/commonMain/kotlin \
  --package com.yourapp.feature
```

With SQLDelight schema:

```bash
python tools/feature-generator/generate.py \
  --config tools/feature-generator/examples/product.yaml \
  --output shared/src/commonMain/kotlin \
  --package com.yourapp.feature \
  --sql
```

Preview without writing files:

```bash
python tools/feature-generator/generate.py \
  --config tools/feature-generator/examples/product.yaml \
  --output /tmp/out \
  --dry-run
```

## YAML schema

```yaml
feature: Product          # PascalCase
endpoint: /products       # GET list path
name: products            # sync target / logical name
fields:
  - name: id
    type: String          # String | Int | Long | Double | Float | Boolean
    primaryKey: true
  - name: title
    type: String
    json: title           # optional SerialName override
mutations:
  enabled: true
  create:
    path: /products
  update:
    path: /products/{id}
  delete:
    path: /products/{id}
```

## Output

| File | Purpose |
|------|---------|
| `{Feature}Models.kt` | Domain, DTO, Create/Update bodies, mappers |
| `InMemory{Feature}Store.kt` | Local observe / count / replace |
| `{Feature}FeatureModule.kt` | `installRestResourceFeature(...)` |
| `FEATURE.md` | Copy-paste install + usage |
| `sqldelight/{Feature}.sq` | Only with `--sql` |

## Wire into the app

```kotlin
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    install(ProductFeatureModule)
}

val api = KmpSdk.get<RestResourceApi<Product>>()
api.refresh()                          // GET /products → local store
api.observeAll()                       // UI list
api.create(CreateProductBody(...))     // POST
api.update(id, UpdateProductBody(...)) // PUT
api.delete(id)                         // DELETE
```

## Notes

- Generated module is **Path C-style** (local store + refresh). Durable SQL is optional.
- Mutations refresh the list after success (`refreshAfterMutation = true`).
- Enable `queueMutationsWhenOffline = true` in `KmpSdk.init` if POST/PUT/DELETE should queue when offline.
