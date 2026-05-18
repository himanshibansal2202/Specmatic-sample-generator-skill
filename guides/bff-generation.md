# BFF Generation Guide

The BFF sits between the Frontend and the Backend. It provides the Product and
Order BFF API without a local database, and it consumes the Backend Products and
Orders APIs through a configurable base URL. Exact API behavior, schemas,
examples, content types, and status codes must be verified against the
executable contracts referenced by `specmatic.yaml`.

During tests, the Frontend calls the BFF, and the BFF calls a Backend API stub
started by Specmatic.

## Role-Specific Requirements

- A Specmatic dependency mock for the Backend API.
- Source code with no local database.
- Backend base URL from configuration such as `STUB_URL`.

## Specmatic Requirements

- Use `guides/specmatic-config.md` for Specmatic config assembly and contract test adapter behavior.
- Configure the BFF OpenAPI contract as the system under test.
- Configure the Backend OpenAPI contract as a Specmatic mock dependency.
- Start the Backend mock before running BFF contract tests and always stop it afterward.
- Keep mock host, mock port, BFF host, BFF port, and Backend base URL configurable for tests.

## Implementation Notes

- Forward every request header and query parameter declared by both the BFF system-under-test contract and the matching Backend dependency contract (for example idempotency, auth, pagination). Do not hardcode header or query parameter names that the executable contracts do not declare.
- Catch backend errors and return their status and response body.
- BFF samples have no seed data.
- Implement BFF endpoints from the BFF system-under-test contract, then map
  those calls to Backend dependency endpoints from the Backend mock contract.
- Before coding the BFF adapter, build a comparison table from the two
  executable contracts that records each BFF operation, its dependency
  operation, and any required path, status, request body, response body,
  query/header, or security transformation.
- Satisfy Backend dependency security schemes in outbound calls even when the
  BFF contract does not declare the same incoming credential. Use a sample
  credential value only to satisfy the mock contract shape.
- Do not assume BFF requests and Backend requests have identical paths,
  required fields, status codes, headers, content types, or response schemas.
- When the contracts differ, implement the smallest adapter transformation
  required by the executable contracts and verify it with Specmatic.
- Preserve request headers and query parameters needed by either contract.
- Do not re-create schema definitions from markdown; read exact request and response fields from the executable OpenAPI contracts or Specmatic report output.
