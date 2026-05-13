# Required Seed Data

The OpenAPI spec contains named examples that reference specific IDs. Your in-memory data store MUST contain these entries at application startup. Without them, Specmatic sends requests for these IDs and gets 404 instead of 200 — causing test failures.

## Products (must exist at startup)

| ID | name | type | inventory | createdOn |
|----|------|------|-----------|-----------|
| 10 | "XYZ Phone" | "gadget" | 10 | "2024-01-10" |
| 20 | "Gemini" | "other" | 10 | "2024-01-20" |

**Why:** The spec has examples `GET_PRODUCT` (id: 10), `UPDATE_PRODUCT` (id: 10), `DELETE_PRODUCT` (id: 20), `INVALID_ID` (id: 344 — should NOT exist, triggers 404).

## Orders (must exist at startup)

| ID | productid | count | status |
|----|-----------|-------|--------|
| 10 | 10 | 2 | "pending" |
| 20 | 10 | 1 | "pending" |

**Why:** The spec has examples `GET_ORDER` (id: 10), `UPDATE_ORDER` (id: 10), `DELETE_ORDER` (id: 20), `INVALID_ID` (id: 433 — should NOT exist, triggers 404).

## Rules

1. IDs 10 and 20 MUST exist for both products and orders
2. ID 344 (products) and 433 (orders) must NOT exist — they test the 404 path
3. Product read responses MUST include `createdOn` as an ISO date string
4. The exact field values for ID 10 should match the spec examples (Specmatic validates schema not values, but matching avoids confusion)
5. New items created via POST should use IDs that don't conflict (start from 100+)
6. Store `Idempotency-Key` values for create requests so a repeated key can return the same generated ID during the process lifetime
