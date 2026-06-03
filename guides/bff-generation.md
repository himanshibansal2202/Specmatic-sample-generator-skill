# BFF Generation Guide

The BFF sits between the Frontend and the Backend. It provides the Product and
Order BFF contract without a local database, and it consumes the Backend
Products and Orders contract through a configurable endpoint or broker. Exact
protocol behavior, schemas, examples, content types, statuses/errors, messages,
and metadata must be verified against the executable contracts referenced by
`specmatic.yaml`.

During tests, the Frontend/client calls the BFF, and the BFF calls a Backend
mock/stub started by Specmatic.

## Role-Specific Requirements

- A Specmatic dependency mock/stub for the Backend contract.
- Source code with no local database.
- Backend base URL, service endpoint, or broker settings from configuration
  such as `STUB_URL`.

## Specmatic Requirements

- Use `guides/specmatic-runtime.md` for Specmatic runtime assembly and contract test adapter behavior.
- Configure the BFF contract as the system under test.
- Configure the Backend contract as a Specmatic mock/stub dependency.
- Start the Backend mock before running BFF contract tests and always stop it afterward.
- Keep mock host, mock port, BFF host, BFF port, Backend base URL, service
  endpoint, broker URL, and protocol-specific settings configurable for tests.

## Implementation Notes

- Forward every request header, query parameter, metadata field, message header,
  or SOAP header declared by both the BFF system-under-test contract and the
  matching Backend dependency contract (for example idempotency, auth,
  pagination). Do not hardcode names that the executable contracts do not
  declare.
- Catch backend errors and return their status/response body or protocol-native
  error shape.
- BFF samples have no seed data.

## Smart Resiliency Handling (429 and 202)

If the BFF's dependency contract defines 429 (Too Many Requests) responses:
- Implement retry logic in the BFF's outbound client calls.
- Respect `Retry-After` header if present; otherwise use exponential backoff.
- Specmatic Enterprise's mock will return 429 during contract tests to verify
  this behavior.

If the BFF's dependency contract defines 202 (Accepted) responses with a
`Link` header pointing to a monitor endpoint:
- The BFF should poll the monitor URL until the operation completes.
- The `/monitor/{id}` path is an infrastructure endpoint for async polling —
  the BFF does not implement it as its own endpoint.

## Path Filtering

If the BFF contract includes paths that the BFF does not implement (e.g.,
`/monitor/{id}` used for Specmatic's 202 Accepted polling), filter them from
contract tests using the Specmatic filter configuration:

```yaml
# In specmatic.yaml or test configuration
filter: "PATH!='/monitor/*'"
```

This prevents Specmatic from generating tests for paths the BFF doesn't serve.

## Endpoint Mapping
- Implement BFF endpoints from the BFF system-under-test contract, then map
  those calls to Backend dependency endpoints from the Backend mock contract.
- Before coding the BFF adapter, build a comparison table from the two
  executable contracts that records each BFF operation, its dependency
  operation, and any required path/topic/method/action, status/error, request
  body/message, response body/message, query/header/metadata, or security
  transformation.
- Satisfy Backend dependency security schemes in outbound calls even when the
  BFF contract does not declare the same incoming credential. Use a sample
  credential value only to satisfy the mock contract shape.
- Do not assume BFF requests and Backend requests have identical paths, topics,
  RPC methods, GraphQL fields, SOAP actions, required fields, status codes,
  headers, metadata, content types, or response schemas.
- When the contracts differ, implement the smallest adapter transformation
  required by the executable contracts and verify it with Specmatic.
- Preserve request headers, query parameters, metadata, and message headers
  needed by either contract.
- Do not re-create schema definitions from markdown; read exact request,
  response, message, and error fields from the executable contracts or
  Specmatic report output.
