# Contracts to Implement

Generated samples must implement the standard business contracts below. The
contract responsibilities depend on the selected application type.

## Contract Responsibilities

| Application type | Provides | Consumes |
|------------------|----------|----------|
| Backend | Products API, Orders API | Inventory service |
| BFF | Product and Order BFF API | Products API, Orders API |
| Frontend | No API contract | Product and Order BFF API |

## Shared Schemas

### Product
```json
{
  "id": "integer",
  "name": "string",
  "type": "book | food | gadget | other",
  "inventory": "integer",
  "createdOn": "date"
}
```

### Order
```json
{
  "id": "integer",
  "productid": "integer",
  "count": "integer",
  "status": "pending | fulfilled | cancelled"
}
```

### Error
```json
{
  "timestamp": "string",
  "status": "integer",
  "error": "string",
  "message": "string"
}
```

## Backend Contracts

Backend samples provide the Products and Orders APIs. They consume the Inventory
service when creating products, reading products, and creating orders.

### Backend Provides: Products API

| Method | Path | Auth | Request | Response | Status |
|--------|------|------|---------|----------|--------|
| POST | /products | Yes | Header: `Idempotency-Key` UUID; Body: `{name: string, type: book\|food\|gadget\|other, inventory: int}` | `{id: int}` | 201 |
| GET | /products/:id | No | — | `{id, name, type, inventory, createdOn}` | 200 |
| PATCH | /products/:id | Yes | Body: `{name: string, type: book\|food\|gadget\|other, inventory: int}` | Empty body or acknowledgement | 200 |
| DELETE | /products/:id | Yes | — | Empty body or acknowledgement | 200 |
| GET | /products | No | Query: `name`, `type`, `status`, `from-date`, `to-date`; Header: `pageSize` | `[Product]` | 200 |
| PUT | /products/:id/image | Yes | multipart/form-data with `image` field | `{message: string}` | 200 |
| GET | /products/:id | No | Unknown ID | `Error` | 404 |

### Backend Provides: Orders API

| Method | Path | Auth | Request | Response | Status |
|--------|------|------|---------|----------|--------|
| POST | /orders | Yes | Header: `Idempotency-Key` UUID; Body: `{productid: int, count: int}` | `{id: int}` | 201 |
| GET | /orders/:id | No | — | `{id, productid, count, status}` | 200 |
| PATCH | /orders/:id | Yes | Body: `{productid: int, count: int, status: pending\|fulfilled\|cancelled}` | Empty body or acknowledgement | 200 |
| DELETE | /orders/:id | Yes | — | Empty body or acknowledgement | 200 |
| GET | /orders | No | Query: `status`, `productid` | `[Order]` | 200 |
| GET | /orders/:id | No | Unknown ID | `Error` | 404 |

### Backend Consumes: Inventory Service

The Inventory Service owns live inventory counts. Backend samples should model
this as an external dependency, even when the generated sample uses an in-memory
stub for local tests.

| Operation | Request | Response | Purpose |
|-----------|---------|----------|---------|
| addInventory(productId, inventory) | `{productid: int, inventory: int}` | `{message: string}` | Register initial stock for a newly created product |
| getInventory(productId) | `{productid: int}` | `{productid: int, inventory: int}` | Fetch live stock for product read responses |
| reduceInventory(productId, inventory) | `{productid: int, inventory: int}` | `{message: string}` | Decrement stock when an order is placed |

Rules:
- Product persistence stores product identity and descriptive fields.
- Inventory count comes from the Inventory Service for read responses.
- Creating a product calls `addInventory`.
- Creating an order calls `reduceInventory` with the order count.
- Deleting an order does not restore inventory.

## BFF Contracts

BFF samples provide a Product and Order BFF API. They do not have a local
database; they consume the Backend Products and Orders APIs.

### BFF Provides: Product and Order BFF API

| Method | Path | Request | Response | Status |
|--------|------|---------|----------|--------|
| POST | /products | Header: `Idempotency-Key` UUID; Body: `{name: string, type: book\|food\|gadget\|other, inventory: int}` | `{id: int}` | 201 |
| GET | /findAvailableProducts | Query: `type`; Header: `pageSize` | `[Product]` | 200 |
| POST | /orders | Header: `Idempotency-Key` UUID; Body: `{productid: int, count: int}` | `{id: int}` | 201 |

### BFF Consumes: Backend APIs

| BFF endpoint | Backend call |
|--------------|--------------|
| POST /products | POST /products with request body, auth header, and `Idempotency-Key` |
| GET /findAvailableProducts | GET /products with `type` query and `pageSize` header |
| POST /orders | POST /orders with request body, auth header, and `Idempotency-Key` |

Rules:
- The Backend API URL comes from configuration such as `STUB_URL`.
- Forward `Idempotency-Key`, auth headers, and `pageSize` when present.
- Return backend error status and body when a backend call fails.

## Frontend Contracts

Frontend samples do not provide an API contract. They consume the Product and
Order BFF API and demonstrate contract consumption from the browser/client
application.

### Frontend Consumes: Product and Order BFF API

| User workflow | BFF call | Request | Expected response |
|---------------|----------|---------|-------------------|
| Create product | POST /products | Header: `Idempotency-Key` UUID; Body: `{name, type, inventory}` | `{id: int}` with status 201 |
| Find available products | GET /findAvailableProducts | Query: `type`; Header: `pageSize` | `[Product]` with status 200 |
| Create order | POST /orders | Header: `Idempotency-Key` UUID; Body: `{productid, count}` | `{id: int}` with status 201 |

Rules:
- The BFF base URL must be configurable.
- Client code must send `Idempotency-Key` for create product and create order workflows.
- Client code must send `pageSize` for product search.
- Frontend samples should verify contract consumption using the generated test framework and Specmatic mock for the BFF API.

## Common Notes

- Authenticated operations must accept an auth header without requiring local credential setup.
- Create endpoints must store `Idempotency-Key` values for the process lifetime when the sample has server-side state.
- Unknown resource reads must return the `Error` shape.
- Use a multipart parser for `PUT /products/:id/image`.
