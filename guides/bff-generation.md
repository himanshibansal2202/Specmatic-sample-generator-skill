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
the BFF must implement the full async monitor pattern. This is tested through
**external examples with delayed stubs** — not magic headers or automatic mock
timeouts.

### How it works:

1. The BFF contract defines both 201 and 202 responses for an operation
2. **External BFF test examples** tell Specmatic which requests should trigger
   the 202 path (e.g., a request with a specific field value like `"name": "UniqueName"`)
3. **External domain service stub examples** tell the backend mock to **delay**
   its response for those same requests (using `"delay-in-seconds": N` and
   `"transient": true`)
4. The BFF has a RestTemplate/HTTP client timeout shorter than the mock's delay
5. When the test runs: BFF calls backend → mock delays → BFF times out →
   BFF creates a monitor → returns 202 with `Link: </monitor/{id}>` header
6. Specmatic polls `GET /monitor/{id}` to check completion
7. The transient stub is consumed after first use → BFF's retry gets instant
   response → monitor completes → Specmatic's poll gets the final result

### Required components when 202 is in the BFF contract:

- **External BFF examples** (in a directory like `src/test/resources/bff/`):
  Define which requests expect 202. Format is a partial example JSON with
  `"http-response": {"status": 202}`.
- **External domain service stub examples** (in `src/test/resources/domain_service/`):
  Define delayed stubs with `"transient": true` and `"delay-in-seconds": N`
  that match the same request patterns.
- **`specmatic.yaml` data.examples** section pointing to these directories.
- **Monitor Controller**: implements `GET /monitor/{id}`.
- **Monitor Service**: manages monitor lifecycle — stores pending requests,
  retries them, stores completed responses.
- **Timeout handling in service layer**: catch timeout exceptions from backend
  calls → create a monitor → return 202.
- **Background scheduler or immediate retry**: execute the backend call again
  (the delayed stub is transient, so the retry gets an instant response).
- **Monitor response model**: must match the schema defined in the BFF contract
  (includes `request`, `response` with `statusCode`, `body`, `headers`).

### The specmatic.yaml examples configuration:

```yaml
systemUnderTest:
  service:
    data:
      examples:
        - directories:
            - ./src/test/resources/bff
dependencies:
  services:
    - service:
        data:
          examples:
            - directories:
                - ./src/test/resources/domain_service
```

### 429 (Too Many Requests) pattern:

For GET endpoints that define a 429 response:
- An external BFF example triggers the 429 test path
- A domain service stub with delay simulates backend slowness
- BFF times out → returns 429 with `Retry-After` header

### Response processor hooks:

If the BFF contract requires dynamic response values (e.g., a `createdOn` date
derived from request query parameters), use a Specmatic response processor hook:

```yaml
dependencies:
  data:
    adapters:
      post_specmatic_response_processor: ./hooks/post_specmatic_response_processor.sh
```

The hook is a shell script that receives the mock response JSON on stdin and
outputs modified JSON. Use this for computed fields that can't be static
examples.

### Key points:

- The BFF **implements** `/monitor/{id}` — it is NOT filtered from tests
- SRO is driven by **external examples and delayed transient stubs**, not by
  a `Specmatic-Response-Code` header or automatic mock timeouts
- The client timeout must be shorter than the stub delay for the timeout to
  trigger
- Transient stubs (`"transient": true`) are consumed after first use, so
  retries succeed immediately
- Reference implementation: `specmatic-order-bff-java`

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
