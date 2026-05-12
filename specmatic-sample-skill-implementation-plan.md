# Specmatic Sample Generator Skill Implementation Plan

## Goal

Create a standardized `generate-specmatic-sample` skill that works in both Codex and Claude Code. The skill should generate and update Specmatic v3 sample projects by validating a requested stack combination, composing reusable project blocks, and producing CI-verifiable samples inside a separate mono-repo.

Primary repositories:

- Skill/plugin repository: `https://github.com/himanshibansal2202/Specmatic-sample-generator-skill`
- Generated sample mono-repo: `https://github.com/himanshibansal2202/specmatic-sample-monorepo`

The implementation should follow the standardized skill structure so the same domain instructions, references, assets, and scripts can be used from both Codex and Claude Code. The Claude Code plugin packaging can wrap this structure, but the core skill content should remain portable.

## Key Decisions

- Use a composable generator instead of one full static template per combination.
- Generate samples into the separate mono-repo, not into one repository per sample.
- Validate combinations before writing files.
- Default to Specmatic v3 and write `version: 3` in generated `specmatic.yaml` files.
- Use existing Specmatic v3 samples as structural references, but do not copy their content blindly.
- Build one complete vertical slice first: Backend + REST + Kotlin + Spring Boot + in-memory + Specmatic v3.
- Keep future combinations documented as planned expansions until the first slice is stable.
- Support create and update flows in the same skill.
- Use GitHub Actions as the final acceptance gate once CI details are available.
- Keep Specmatic CLI, license, Specmatic Insights, Docker Hub publishing, and exact CI secret details as placeholders until provided.
- Record model choice as a recommendation in skill/plugin documentation. Current Codex skill metadata does not provide a standard field that hard-codes the runtime model.

## Initial Supported Combination

Start with one supported combination:

1. Backend + Sync + REST/OpenAPI + Kotlin + Spring Boot + in-memory + Specmatic v3

Unsupported combinations should be rejected clearly with nearest supported alternatives. The following are planned expansions, not initially supported outputs:

- Backend + REST + Java + Spring Boot
- Backend + REST + Node.js/TypeScript + Express
- BFF + REST + Java/Kotlin Spring Boot
- BFF + REST + Node.js/TypeScript Express
- Frontend + REST + React
- Kafka, gRPC, GraphQL, JDBC, Redis, Arazzo, WSDL, and MCP-oriented samples

## Skill Structure

```text
generate-specmatic-sample/
├── SKILL.md
├── agents/
│   └── openai.yaml
├── references/
│   ├── classification.md
│   ├── contract-catalog.md
│   ├── generation-workflow.md
│   ├── upstream-samples.md
│   ├── update-strategy.md
│   ├── acceptance-criteria.md
│   ├── model-recommendation.md
│   └── specmatic-placeholders.md
├── assets/
│   ├── config/
│   │   └── stack-matrix.yaml
│   ├── contracts/
│   │   ├── products.yaml
│   │   ├── orders.yaml
│   │   ├── inventory.yaml
│   │   └── product-order-bff.yaml
│   ├── blocks/
│   │   ├── roles/
│   │   │   ├── backend-provider/
│   │   │   ├── bff-provider-consumer/
│   │   │   └── frontend-consumer/
│   │   ├── protocols/
│   │   │   └── rest-openapi/
│   │   ├── frameworks/
│   │   │   ├── kotlin-spring/
│   │   │   ├── java-spring/
│   │   │   ├── ts-express/
│   │   │   └── react/
│   │   ├── data-fetch/
│   │   │   ├── in-memory/
│   │   │   └── rest-api/
│   │   └── ci/
│   │       ├── github-actions/
│   │       ├── docker/
│   │       └── coverage/
│   └── golden-samples/
│       └── backend-rest-kotlin-spring-inmemory/
└── scripts/
    ├── validate_selection.py
    ├── scaffold_sample.py
    └── verify_generated_sample.py
```

Do not add auxiliary files such as separate README, installation guide, quick reference, or changelog files inside the skill unless a consuming platform explicitly requires them. Keep detailed guidance in the listed reference files.

## Reference Files

### `references/classification.md`

Document the classification taxonomy:

- application type: Backend, BFF, Frontend
- protocol type: Sync, Async fire-and-forget, Async request-reply
- protocol: REST, Kafka, gRPC, GraphQL, etc.
- language
- framework
- data fetch
- Specmatic version

The initial implementation supports only the Kotlin Spring Boot backend REST slice, but the taxonomy should describe the broader classification model.

### `references/contract-catalog.md`

Document the canonical business contracts and keep them aligned with `assets/contracts/`.

Backend samples:

- provide Products
- provide Orders
- consume Inventory

BFF samples:

- provide Product and Order BFF
- consume Product API endpoints
- consume Order API endpoints

Frontend samples:

- consume Product and Order BFF

### `references/generation-workflow.md`

Explain how the skill should:

1. Collect or infer user selections.
2. Validate the combination.
3. Select role, protocol, framework, data-fetch, contract, and CI blocks.
4. Generate or update the sample in the mono-repo.
5. Register the sample in mono-repo metadata.
6. Run structural verification.
7. Point users to CI as the final acceptance gate.

### `references/upstream-samples.md`

Document upstream Specmatic v3 repositories to inspect for structure and conventions. These are references only, not sources to copy blindly:

- `https://github.com/specmatic/specmatic-order-api-java`
- `https://github.com/specmatic/specmatic-order-api-nodejs`
- `https://github.com/specmatic/specmatic-order-bff-java`
- `https://github.com/specmatic/specmatic-order-bff-nodejs`
- `https://github.com/specmatic/specmatic-order-ui-react`

The generated samples should follow the broad project shape, naming conventions, Specmatic v3 configuration style, and verification expectations from upstream samples while keeping code and contracts purpose-built for this generator.

### `references/update-strategy.md`

Document the update behavior:

1. Read existing mono-repo metadata.
2. Validate the requested update.
3. Read `.specmatic-sample-manifest.json` to identify generator-owned files.
4. Recompose generator-owned files.
5. Preserve user-owned files where possible.
6. Show `git diff`.
7. Run structural verification.

Prefer update-in-place. Delete-and-regenerate is only a fallback or regression strategy, not the default workflow.

### `references/acceptance-criteria.md`

Document per-sample acceptance criteria:

- sample code illustrates contract publication, workflow, and contract consumption where applicable
- README exists
- build files exist
- source code exists
- tests exist
- contracts exist
- local single command for the full build is documented
- Dockerfile exists
- GitHub Actions workflow exists
- code coverage is configured
- Specmatic Insights publishing placeholder exists until CI details are provided
- license setup placeholder exists until CI details are provided

Local environment bootstrap is out of scope. The skill should not generate scripts to install Java, Node, Docker, or similar prerequisites from scratch.

### `references/model-recommendation.md`

Record the recommended model for developing and using the skill as guidance. Do not claim that `SKILL.md` or `agents/openai.yaml` can force a specific model unless the consuming platform later adds a supported field for that.

### `references/specmatic-placeholders.md`

Track details intentionally deferred until they are provided:

- Specmatic v3 CLI installation
- local CLI commands
- CI commands
- license setup
- Specmatic Insights publishing
- Docker Hub naming and secrets
- GitHub secret names

Known configuration decision:

- Generated samples should use `version: 3` in `specmatic.yaml`.

## Standard Business Contracts

Create canonical REST/OpenAPI contract assets first:

```text
assets/contracts/products.yaml
assets/contracts/orders.yaml
assets/contracts/inventory.yaml
assets/contracts/product-order-bff.yaml
```

### Products

- `POST /products`
  - Auth: Yes
  - Request: `{name: string, type: book|food|gadget|other, inventory: int}` plus `Idempotency-Key` UUID header
  - Response: `{id: int}`, status `201`
- `GET /products/{id}`
  - Auth: No
  - Response: `{id, name, type, inventory, createdOn: date}`
- `PATCH /products/{id}`
  - Auth: Yes
  - Request: `{name: string, type: enum, inventory: int}`
  - Response: status `200`
- `DELETE /products/{id}`
  - Auth: Yes
  - Response: status `200`
- `GET /products`
  - Auth: No
  - Request: query parameters `name`, `type`, `status`, `from-date`, `to-date`; header `pageSize`
  - Response: `[Product]`
- `PUT /products/{id}/image`
  - Auth: Yes
  - Request: `multipart/form-data` with `image` field
  - Response: `{message: string}`, status `200`

### Orders

- `POST /orders`
  - Auth: Yes
  - Request: `{productid: int, count: int}` plus `Idempotency-Key` UUID header
  - Response: `{id: int}`, status `201`
- `GET /orders/{id}`
  - Auth: No
  - Response: `{id, productid, count, status: pending|fulfilled|cancelled}`
- `PATCH /orders/{id}`
  - Auth: Yes
  - Request: `{productid: int, count: int, status: enum}`
  - Response: status `200`
- `DELETE /orders/{id}`
  - Auth: Yes
  - Response: status `200`
- `GET /orders`
  - Auth: No
  - Request: query parameters `status`, `productid`
  - Response: `[Order]`

### Inventory

Represent Inventory as the external dependency consumed by backend samples:

- `addInventory(productId, inventory)`
  - Request: `{productid: int, inventory: int}`
  - Response: `{message: string}`
  - Purpose: registers initial stock for a newly created product
- `getInventory(productId)`
  - Request: `{productid: int}`
  - Response: `{productid: int, inventory: int}`
  - Purpose: fetches live stock count for a product
- `reduceInventory(productId, inventory)`
  - Request: `{productid: int, inventory: int}`
  - Response: `{message: string}`
  - Purpose: decrements stock when an order is placed; deleting an order does not restore inventory

### Product and Order BFF

The BFF contract exposes:

- `POST /orders`
- `POST /products`
- `GET /findAvailableProducts` with `pageSize` header

Later protocol-specific assets can be added under:

```text
assets/contracts/asyncapi/
assets/contracts/grpc/
assets/contracts/graphql/
```

## Stack Matrix

Create `assets/config/stack-matrix.yaml` as the source of truth for supported combinations. The initial matrix should contain only the first supported slice.

Example:

```yaml
specmatic:
  supported_versions:
    - v3
  default_version: v3

roles:
  backend:
    provides:
      - products
      - orders
    consumes:
      - inventory
  bff:
    provides:
      - product-order-bff
    consumes:
      - products-api
      - orders-api
  frontend:
    consumes:
      - product-order-bff

supported_combinations:
  - id: backend-rest-kotlin-spring-inmemory
    specmatic_version: v3
    application_type: backend
    protocol_type: sync
    protocol: rest
    language: kotlin
    framework: spring-boot
    data_fetch: in-memory
    provides:
      - products
      - orders
    consumes:
      - inventory
    required_blocks:
      - roles/backend-provider
      - protocols/rest-openapi
      - frameworks/kotlin-spring
      - data-fetch/in-memory
      - ci/github-actions
      - ci/docker
      - ci/coverage
```

## Generator Blocks

Use composable blocks rather than one full template per combination.

Initial blocks required for the first slice:

- role block: backend provider
- protocol block: REST/OpenAPI
- framework block: Kotlin Spring Boot
- data-fetch block: in-memory
- CI blocks: GitHub Actions, Docker, coverage

Planned later blocks:

- role blocks: BFF provider-consumer, frontend consumer
- framework blocks: Java Spring, Node.js/TypeScript Express, React
- data-fetch block: REST API
- protocol blocks: Kafka, gRPC, GraphQL, and others

The generator composes selected blocks into a sample project, writes sample metadata, and records generated file ownership in a manifest.

## Scripts

### `scripts/validate_selection.py`

Responsibilities:

- parse requested selection
- read `stack-matrix.yaml`
- validate application type
- validate protocol type
- validate protocol
- validate language/framework pair
- validate data fetch strategy
- validate Specmatic version
- validate contract role
- check that required blocks exist
- check that required contract assets exist
- return the exact supported match or reject with nearest supported alternatives

### `scripts/scaffold_sample.py`

Responsibilities:

- create or update output inside the mono-repo
- copy canonical contracts
- compose role, protocol, framework, data-fetch, and CI blocks
- write sample metadata
- write `specmatic.yaml` with `version: 3`
- write `.specmatic-sample-manifest.json` listing generated files owned by the generator
- preserve user-owned files on update where possible

### `scripts/verify_generated_sample.py`

Responsibilities:

- check README exists
- check local full-build command is documented
- check contracts exist
- check tests exist
- check build file exists
- check Dockerfile exists where required
- check GitHub Actions workflow exists
- check sample is registered in mono-repo metadata
- check `specmatic.yaml` exists and contains `version: 3`
- check placeholder CI/license/Insights sections exist until exact details are supplied

When Specmatic CLI and CI details are available, extend verification to run the real local and CI commands.

## Mono-Repo Output Shape

Generated samples should be placed in the separate mono-repo:

```text
specmatic-sample-monorepo/
├── samples.yaml
├── contracts/
│   ├── products.yaml
│   ├── orders.yaml
│   ├── inventory.yaml
│   └── product-order-bff.yaml
├── samples/
│   └── backend-rest-kotlin-spring-inmemory/
└── .github/
    └── workflows/
        └── samples-ci.yml
```

The skill should ask for the output mono-repo path if it is not provided.

## Golden Samples

Keep generated golden samples for reference and regression testing:

```text
assets/golden-samples/
└── backend-rest-kotlin-spring-inmemory/
```

Golden samples should be generated from the skill after the first vertical slice is stable. They are regression anchors, not the primary generation mechanism. Upstream Specmatic repositories remain structural references only.

## Update Behavior

Preferred update flow:

1. Read existing `samples.yaml`.
2. Validate that the requested sample exists or can be generated.
3. Read `.specmatic-sample-manifest.json` to identify generator-owned files.
4. Recompose generator-owned files.
5. Preserve user-owned files where possible.
6. Show `git diff`.
7. Run `verify_generated_sample.py`.

This avoids deleting the whole repository and reduces accidental loss of manual fixes. Delete-and-regenerate can be used only as a fallback or regression comparison strategy.

## Validation Plan

Run skill validation:

```bash
quick_validate.py generate-specmatic-sample
```

Run functional script checks:

```bash
python scripts/validate_selection.py \
  --application-type backend \
  --protocol-type sync \
  --protocol rest \
  --language kotlin \
  --framework spring-boot \
  --data-fetch in-memory
```

Run generation and structural verification once scripts exist:

```bash
python scripts/scaffold_sample.py ...
python scripts/verify_generated_sample.py ...
```

Final acceptance should happen through the mono-repo GitHub Actions workflow once CI details are provided.

## Forward Testing

Forward-test with realistic prompts:

```text
Use generate-specmatic-sample to create a backend REST Kotlin Spring Boot sample with in-memory data fetch in the Specmatic sample mono-repo.
```

```text
Use generate-specmatic-sample to update the backend REST Kotlin Spring Boot sample for a new Specmatic config requirement.
```

```text
Use generate-specmatic-sample to create a frontend REST React sample that consumes the Product and Order BFF contract.
```

The first two prompts should succeed after the initial slice is implemented. The frontend prompt should be rejected clearly until that combination is added.

Success criteria:

- unsupported combinations are rejected clearly
- supported combinations generate deterministic output
- generated samples include contracts, tests, README, Docker, CI, and metadata
- verifier catches missing required files
- update flow avoids unnecessary rewrites
- all supported samples are creatable from the final plugin build
- generated mono-repo CI proves supported samples are regression-safe once CI details are added

## Recommended First Implementation Slice

Build one vertical slice first:

1. Create skill skeleton.
2. Add reference files.
3. Add `stack-matrix.yaml` with only `backend-rest-kotlin-spring-inmemory`.
4. Add `validate_selection.py`.
5. Add canonical REST contract assets.
6. Add Kotlin Spring Boot backend blocks.
7. Add in-memory data-fetch block.
8. Add GitHub Actions, Docker, and coverage blocks with placeholders for deferred CI details.
9. Add `scaffold_sample.py`.
10. Add `verify_generated_sample.py`.
11. Generate `backend-rest-kotlin-spring-inmemory` into a local checkout of the mono-repo.
12. Validate the skill and generated sample structurally.
13. Add real Specmatic CLI, license, Insights, and Docker publishing commands when details are provided.
14. Expand to Node.js/TypeScript Express backend, BFF, and React frontend after the first slice is stable.
