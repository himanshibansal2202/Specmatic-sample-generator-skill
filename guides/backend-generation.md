# Backend Generation Guide

Backend samples provide the Products and Orders APIs and consume an Inventory
service when creating products, reading products, and creating orders. Exact API
behavior, schemas, examples, content types, and status codes must be verified
against the executable contract referenced by `specmatic.yaml`.

## Role-Specific Requirements

- Source code implementing Products and Orders APIs.
- In-memory product/order store seeded from `test-data/backend-seed-data.md`.
- Inventory client boundary with an in-memory implementation for local tests unless the stack selection says otherwise.

## Implementation Notes

- Keep product persistence separate from inventory count lookup.
- Create product calls Inventory `addInventory`.
- Read product calls Inventory `getInventory`.
- Create order calls Inventory `reduceInventory`.
- Accept every request header the executable contract declares for an operation (for example idempotency, auth, pagination). Headers not declared by the contract should not be required.
- Authenticated operations should accept an auth header without requiring local credential setup.
- Unknown resource reads should return the executable contract's error response.
- Use a multipart parser for product image uploads when the executable contract includes that endpoint.
- Do not re-create schema definitions from markdown; read exact request and response fields from the executable OpenAPI contract or Specmatic report output.
