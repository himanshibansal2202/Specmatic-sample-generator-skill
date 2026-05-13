# API Contracts to Implement

The Backend sample implements the Order API from the Specmatic central contract repo (`api_order_v3.yaml`).

## Schemas

### Product
```
{
  id: integer (required)
  name: string (required)
  type: string enum [book, food, gadget, other] (required)
  inventory: integer (required)
}
```

### Order
```
{
  id: integer (required)
  productid: integer (required)
  count: integer (required)
  status: string enum [pending, fulfilled, cancelled] (required)
}
```

## Endpoints

### Products

| Method | Path | Auth | Request Body | Response | Status |
|--------|------|------|-------------|----------|--------|
| GET | /products | No | — | `[Product]` | 200 |
| GET | /products | No | Query: `type` (enum) | `[Product]` filtered | 200 |
| POST | /products | Yes | `{name, type, inventory}` | `{id: int}` | 200 |
| GET | /products/:id | No | — | `Product` | 200 |
| GET | /products/:id | No | — (ID not found) | `{timestamp, status, error, message}` | 404 |
| POST | /products/:id | Yes | `Product` (full object) | `"success"` (text/plain) | 200 |
| DELETE | /products/:id | Yes | — | `"success"` (text/plain) | 200 |
| PUT | /products/:id/image | No | multipart/form-data with `image` field | `{message: string, productId: int}` | 200 |

### Orders

| Method | Path | Auth | Request Body | Response | Status |
|--------|------|------|-------------|----------|--------|
| GET | /orders | No | Query: `productid` (number), `status` (string) | `[Order]` | 200 |
| POST | /orders | Yes | `{productid, count, status}` | `{id: int}` | 200 |
| GET | /orders/:id | No | — | `Order` | 200 |
| GET | /orders/:id | No | — (ID not found) | `{timestamp, status, error, message}` | 404 |
| POST | /orders/:id | Yes | `Order` (full object) | `"success"` (text/plain) | 200 |
| DELETE | /orders/:id | Yes | — | `"success"` (text/plain) | 200 |

## Important Notes

- **Auth** is via `Authenticate` header (API key). Specmatic handles this during testing — your app does NOT need to validate it. Just accept the header and proceed.
- **POST /:id** is used for updates (not PATCH). Returns `text/plain` "success", not JSON.
- **DELETE** returns `text/plain` "success", not JSON.
- **404 responses** must return JSON with shape: `{timestamp: string, status: number, error: string, message: string}`
- **GET /products** accepts optional `type` query param to filter by product type.
- **GET /orders** accepts optional `productid` and `status` query params.
- **PUT /products/:id/image** accepts `multipart/form-data` with a field named `image`. You need a multipart parser (e.g., multer for Express).
