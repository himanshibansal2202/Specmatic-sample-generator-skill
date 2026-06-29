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
- Catch backend errors and return their status/response body or protocol-native
  error shape.
- BFF samples have no seed data.

## Path Filtering

Filter only paths that the BFF does not implement and that are not declared in
the executable BFF contract, such as framework or documentation endpoints:
- `/health` — handled by framework actuator
- `/swagger` — served by a documentation library

Do not filter contract-declared BFF paths. If the executable BFF contract
contains a monitor or polling endpoint such as `/monitor/{id}`, implement it and
verify it with Specmatic. Add `filter` config only after verifying the exact
runtime-supported object syntax for the selected Specmatic version.

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
