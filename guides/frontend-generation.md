# Frontend Generation Guide

Frontend samples do not provide an API. They implement client workflows that
consume the Product and Order BFF API through a configurable base URL. Exact
consumed API behavior, schemas, examples, content types, and status codes must
be verified against the executable contract referenced by `specmatic.yaml`.

## Role-Specific Requirements

- Client application with create product, find available products, and create order workflows.
- Configurable BFF base URL.
- Client API module for BFF calls.
- Contract consumption tests using a Specmatic mock of the BFF API.

## Implementation Notes

- Send every request header and query parameter declared by the BFF executable contract for each call (for example idempotency, auth, pagination). Do not hardcode header or query parameter names the contract does not declare.
- Keep BFF URL configuration environment-specific.
- Create product calls the BFF product creation API.
- Find available products calls the BFF product search API with the query parameters the contract declares for that operation.
- Create order calls the BFF order creation API.
- Do not re-create schema definitions from markdown; read exact request and response fields from the executable OpenAPI contract or Specmatic report output.
