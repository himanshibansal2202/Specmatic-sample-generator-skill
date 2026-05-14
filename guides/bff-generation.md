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

- Forward `Idempotency-Key`, auth headers, and `pageSize` when present.
- Catch backend errors and return their status and response body.
- BFF samples have no seed data.
- Create product calls the Backend product creation API with the request body, auth header, and `Idempotency-Key`.
- Find available products calls the Backend product search API with the `type` query and `pageSize` header.
- Create order calls the Backend order creation API with the request body, auth header, and `Idempotency-Key`.
- Do not re-create schema definitions from markdown; read exact request and response fields from the executable OpenAPI contracts or Specmatic report output.
