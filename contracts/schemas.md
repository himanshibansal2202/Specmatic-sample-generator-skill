# Shared Schemas

These schemas are shared across Backend, BFF, and Frontend sample contracts.

## Product

```json
{
  "id": "integer",
  "name": "string",
  "type": "book | food | gadget | other",
  "inventory": "integer",
  "createdOn": "date"
}
```

## Order

```json
{
  "id": "integer",
  "productid": "integer",
  "count": "integer",
  "status": "pending | fulfilled | cancelled"
}
```

## Error

```json
{
  "timestamp": "string",
  "status": "integer",
  "error": "string",
  "message": "string"
}
```
