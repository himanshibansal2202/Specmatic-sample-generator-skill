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
- Configure every discovered dependency contract as a Specmatic mock/stub
  dependency. BFF dependencies may include REST backend mocks and broker/message
  mocks such as AsyncAPI/Kafka.
- Start all dependency mocks before running BFF contract tests and always stop
  them afterward.
- Keep mock host, mock port, BFF host, BFF port, Backend base URL, service
  endpoint, broker URL, and protocol-specific settings configurable for tests.

## Implementation Notes

- Forward every request header, query parameter, metadata field, message header,
  or SOAP header declared by both the BFF system-under-test contract and the
  matching Backend dependency contract (for example idempotency, auth,
  pagination). Do not hardcode names that the executable contracts do not
  declare.
- BFF samples have no seed data.
- Implement every endpoint the executable BFF contract declares, including
  monitor or polling endpoints such as `/monitor/{id}`. Do not filter a
  contract-declared path. For endpoint discovery and path-filter syntax, follow
  `guides/specmatic-runtime.md`, which sources the filter syntax from the
  official Specmatic documentation.

### Dependency boundary integrity (required)

A BFF sample exists to prove the BFF preserves its backend dependency contract.
The contract test must fail when that boundary breaks — it must not pass on
fabricated data. Optimizing only for "the Specmatic suite is green" instead of
"the BFF preserves the backend contract" produces a sample that is hollow at the
dependency boundary: Specmatic only sees the BFF's outward responses, so a broken
backend integration stays invisible.

- Derive every backend-backed response from the actual dependency response (the
  Specmatic dependency mock or the dependency contract). Never return hardcoded
  or default payloads such as a canned id, product, or order.
- Do not wrap outbound dependency calls in a blanket `catch (Exception)` (or the
  language equivalent) that swallows the error. Catch only to rethrow, or to map
  a real backend error onto the status/response shape the BFF contract declares.
- A backend failure must be visible: if the dependency mock is unreachable or
  returns an unexpected payload, the test must fail rather than silently pass.
- Only synthesize data for behavior the BFF contract itself defines (for example
  monitor or aggregation state), never for core dependency results.
- Return only the status codes the BFF contract declares for that operation. Do
  not add a 4xx/5xx the contract does not define, even for inputs that look
  invalid; doing so also produces "missing in spec" coverage entries.
- Verify the dependency mock is actually exercised, not just that the BFF returns
  the right outward shape.

## Endpoint Mapping
- Implement BFF endpoints from the BFF system-under-test contract, then map
  those calls to Backend dependency endpoints from the Backend mock contract.
- Before coding the BFF adapter, build a comparison table across all executable
  dependency contracts that records each BFF workflow, its dependency
  operation/message, and any required path/topic/method/action, status/error,
  request body/message, response body/message, query/header/metadata, or
  security transformation.
- Do not infer dependencies only from direct references inside the SUT OpenAPI
  file. Use the user-provided repository's nearby contract structure, parsed
  contract semantics, role intent, and operation/message compatibility across
  protocols. If an async dependency is implied by the selected contract family,
  include it in `specmatic.yaml` even when the OpenAPI contract does not
  reference it directly. If multiple async candidates remain plausible, stop and
  report the structured ambiguity JSON instead of skipping them.
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

## Caveats and Findings

Behavior that is not obvious from the schema and not currently covered by the
official Specmatic documentation. Each entry must be verifiable from a real run,
not an assumption. These are recorded so the Specmatic team can review whether
they belong in the official docs; remove an entry once the documentation covers
it.

```json
[
  {
    "id": "BFF-001",
    "area": "operation with multiple success responses",
    "finding": "When an operation declares more than one success response for the same request (for example 201 and 202 on POST /products), the request alone cannot determine which response to return. Specmatic sends a request header 'Specmatic-Response-Code' to the provider indicating which response status it is currently testing; the provider must branch on it to return the matching response.",
    "evidence": "product_search_bff_v6.yaml POST /products and POST /orders each declare both 201 and 202. The CTRF coverage report marks the 202 operation covered with 12 matches, and the only provider code path returning 202 is the branch on the 'Specmatic-Response-Code' header.",
    "docStatus": "not found in searchable Specmatic documentation; documented Specmatic headers are X-Specmatic-Result, X-Specmatic-Type, and X-Specmatic-Group",
    "specmaticTeamReview": true
  }
]
```
