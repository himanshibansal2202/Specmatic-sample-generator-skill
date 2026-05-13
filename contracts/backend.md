# Backend Contracts

Backend samples provide the Products and Orders APIs. They consume the Inventory
service when creating products, reading products, and creating orders.

See `contracts/schemas.md` for shared `Product`, `Order`, and `Error` shapes.

## Provides: Products API

| Method | Path | Auth | Request | Response | Status |
|--------|------|------|---------|----------|--------|
| POST | /products | Yes | Header: `Idempotency-Key` UUID; Body: `{name: string, type: book\|food\|gadget\|other, inventory: int}` | `{id: int}` | 201 |
| GET | /products/:id | No | - | `{id, name, type, inventory, createdOn}` | 200 |
| PATCH | /products/:id | Yes | Body: `{name: string, type: book\|food\|gadget\|other, inventory: int}` | Empty body or acknowledgement | 200 |
| DELETE | /products/:id | Yes | - | Empty body or acknowledgement | 200 |
| GET | /products | No | Query: `name`, `type`, `status`, `from-date`, `to-date`; Header: `pageSize` | `[Product]` | 200 |
| PUT | /products/:id/image | Yes | multipart/form-data with `image` field | `{message: string}` | 200 |
| GET | /products/:id | No | Unknown ID | `Error` | 404 |

## Provides: Orders API

| Method | Path | Auth | Request | Response | Status |
|--------|------|------|---------|----------|--------|
| POST | /orders | Yes | Header: `Idempotency-Key` UUID; Body: `{productid: int, count: int}` | `{id: int}` | 201 |
| GET | /orders/:id | No | - | `{id, productid, count, status}` | 200 |
| PATCH | /orders/:id | Yes | Body: `{productid: int, count: int, status: pending\|fulfilled\|cancelled}` | Empty body or acknowledgement | 200 |
| DELETE | /orders/:id | Yes | - | Empty body or acknowledgement | 200 |
| GET | /orders | No | Query: `status`, `productid` | `[Order]` | 200 |
| GET | /orders/:id | No | Unknown ID | `Error` | 404 |

## Rules

- Authenticated operations must accept an auth header without requiring local credential setup.
- Create endpoints must accept `Idempotency-Key` and store keys for the process lifetime.
- Unknown resource reads must return the `Error` shape.
- Use a multipart parser for `PUT /products/:id/image`.
