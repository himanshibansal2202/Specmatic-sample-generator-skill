# Frontend Generation Guide

Frontend samples implement the workflows in `contracts/frontend.md`. They do not
provide an API; they consume the Product and Order BFF API.

## Required Pieces

- Client application with create product, find available products, and create order workflows.
- Configurable BFF base URL.
- Client API module for BFF calls.
- Contract consumption tests using a Specmatic mock of the BFF API.
- Build file, README, CI workflow, Dockerfile when applicable, and `.gitignore`.

## Implementation Notes

- Generate `Idempotency-Key` values for create product and create order requests.
- Send `pageSize` when searching for available products.
- Keep BFF URL configuration environment-specific.
