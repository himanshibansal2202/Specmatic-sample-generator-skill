# Backend Seed Data

The OpenAPI spec contains named examples that reference specific IDs. The
backend in-memory data store MUST contain these entries at application startup.
Without them, Specmatic sends requests for these IDs and gets 404 instead of
200, causing test failures.

## Products

| ID | name | type | inventory | createdOn |
|----|------|------|-----------|-----------|
| 10 | "XYZ Phone" | "gadget" | 10 | "2024-01-10" |
| 20 | "Gemini" | "other" | 10 | "2024-01-20" |

## Orders

| ID | productid | count | status |
|----|-----------|-------|--------|
| 10 | 10 | 2 | "pending" |
| 20 | 10 | 1 | "pending" |

## Rules

1. IDs 10 and 20 MUST exist for both products and orders.
2. ID 344 for products and 433 for orders must NOT exist; they test 404 paths.
3. Product read responses MUST include `createdOn` as an ISO date string.
4. New items created via POST should use IDs that do not conflict; start from 100+.
5. Store `Idempotency-Key` values for create requests so a repeated key can return the same generated ID during the process lifetime.
