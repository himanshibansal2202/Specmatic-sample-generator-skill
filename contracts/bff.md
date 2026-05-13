# BFF Contracts

BFF samples provide a Product and Order BFF API. They do not have a local
database; they consume the Backend Products and Orders APIs.

See `contracts/schemas.md` for shared `Product`, `Order`, and `Error` shapes.

## Provides: Product and Order BFF API

| Method | Path | Request | Response | Status |
|--------|------|---------|----------|--------|
| POST | /products | Header: `Idempotency-Key` UUID; Body: `{name: string, type: book\|food\|gadget\|other, inventory: int}` | `{id: int}` | 201 |
| GET | /findAvailableProducts | Query: `type`; Header: `pageSize` | `[Product]` | 200 |
| POST | /orders | Header: `Idempotency-Key` UUID; Body: `{productid: int, count: int}` | `{id: int}` | 201 |

## Consumes: Backend APIs

| BFF endpoint | Backend call |
|--------------|--------------|
| POST /products | POST /products with request body, auth header, and `Idempotency-Key` |
| GET /findAvailableProducts | GET /products with `type` query and `pageSize` header |
| POST /orders | POST /orders with request body, auth header, and `Idempotency-Key` |

## Rules

- The Backend API URL comes from configuration such as `STUB_URL`.
- Forward `Idempotency-Key`, auth headers, and `pageSize` when present.
- Return backend error status and body when a backend call fails.
