# Frontend Contracts

Frontend samples do not provide an API contract. They consume the Product and
Order BFF API and demonstrate contract consumption from the browser/client
application.

See `contracts/schemas.md` for shared `Product` and `Order` shapes.

## Consumes: Product and Order BFF API

| User workflow | BFF call | Request | Expected response |
|---------------|----------|---------|-------------------|
| Create product | POST /products | Header: `Idempotency-Key` UUID; Body: `{name, type, inventory}` | `{id: int}` with status 201 |
| Find available products | GET /findAvailableProducts | Query: `type`; Header: `pageSize` | `[Product]` with status 200 |
| Create order | POST /orders | Header: `Idempotency-Key` UUID; Body: `{productid, count}` | `{id: int}` with status 201 |

## Rules

- The BFF base URL must be configurable.
- Client code must send `Idempotency-Key` for create product and create order workflows.
- Client code must send `pageSize` for product search.
- Frontend samples should verify contract consumption using the generated test framework and Specmatic mock for the BFF API.
