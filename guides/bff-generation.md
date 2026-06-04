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

## Smart Resiliency Orchestration (202 Accepted Pattern)

If the BFF contract defines 202 (Accepted) responses for any operation,
the BFF must implement the full async monitor pattern. Specmatic Enterprise
tests this by simulating backend timeouts during contract tests.

### How it works:

1. Specmatic sends a request to the BFF (e.g., `POST /products`)
2. BFF calls the backend dependency
3. Specmatic's mock **simulates a timeout** (the backend doesn't respond in time)
4. BFF catches the timeout → stores the request in a monitor → returns
   **202 Accepted** with headers:
   - `Link: </monitor/{id}>; title="monitor"`
   - `Retry-After: <seconds>`
5. Specmatic polls `GET /monitor/{id}` to check completion
6. BFF's background scheduler retries the backend call
7. When backend eventually responds, the monitor stores the result
8. Specmatic's next poll gets the completed response from `/monitor/{id}`

### Required components when 202 is in the BFF contract:

- **Monitor Controller**: implements `GET /monitor/{id}` — returns the monitor
  status (pending or completed with the original response)
- **Monitor Service**: manages monitor lifecycle — stores pending requests,
  retries them on a schedule, stores completed responses
- **Monitor Database/Store**: in-memory store of monitor entries
- **Timeout handling in service layer**: catch `SocketTimeoutException` (or
  equivalent) from backend calls → create a monitor → return 202
- **Background scheduler**: periodically retries pending monitors against the
  backend dependency
- **Monitor response model**: must match the schema defined in the BFF contract
  (typically includes `request`, `response` with `statusCode`, `body`, `headers`)

### Key points:

- The BFF **implements** `/monitor/{id}` — it is NOT filtered out from tests
- The timeout comes from the backend mock (Specmatic simulates it), not from
  a 429 response
- The BFF returns 429 (Too Many Requests) to its own consumers for GET
  endpoints that time out (with `Retry-After` header)
- The BFF returns 202 (Accepted) for POST/mutation endpoints that time out
  (with `Link` header pointing to the monitor)
- If the BFF contract does NOT define 202 responses, skip this entire pattern

### Reference implementation:

See `specmatic-order-bff-java` for the complete working pattern in Kotlin/Spring Boot.

## Path Filtering

Only filter paths that the BFF genuinely does not implement. Do NOT filter
`/monitor/{id}` if the BFF contract defines 202 responses — the BFF must
implement and serve that endpoint.

Paths to filter:
- `/health` — if present in spec but handled by framework actuator
- `/swagger` — if present in spec but served by a library

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
