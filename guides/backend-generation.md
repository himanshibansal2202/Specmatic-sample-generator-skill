# Backend Generation Guide

Backend samples implement `contracts/backend.md` and consume
`contracts/inventory.md`.

## Required Pieces

- `specmatic.yaml` using the backend contract under test.
- Build file with the language runtime, test framework, and Specmatic dependency.
- Source code implementing Products and Orders APIs.
- In-memory product/order store seeded from `test-data/backend-seed-data.md`.
- Inventory client boundary; use an in-memory implementation for local tests unless the stack selection says otherwise.
- Contract test adapter using `guides/specmatic-config.md`.
- Dockerfile, GitHub Actions workflow, README, and `.gitignore`.

## Implementation Notes

- Keep product persistence separate from inventory count lookup.
- Create product calls Inventory `addInventory`.
- Read product calls Inventory `getInventory`.
- Create order calls Inventory `reduceInventory`.
- Store `Idempotency-Key` values for create requests during the process lifetime.
