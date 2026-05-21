# Specmatic Runtime & Contract Test Patterns

This guide describes how generated samples should assemble Specmatic runtime
configuration and contract test adapters. It is generator guidance only: do not
copy it verbatim into a sample, and do not use it to define contract behavior.

Concrete contract repository URLs, spec formats, run option keys, and spec paths
come from `config/contract-resolution.yaml`, user input, or runtime contract
discovery. Default ports come from the root `SKILL.md` generation conventions.

## Required Inputs

Resolve these values before writing a generated sample's `specmatic.yaml`:

- `<CONTRACT_REPO_URL>`
- `<SPEC_FORMAT>` (`openapi`, `asyncapi`, `protobuf`, `graphqlsdl`, or `wsdl`)
- `<RUN_OPTION_KEY>` (`openapi`, `asyncapi`, `protobuf`, `graphqlsdl`, or `wsdl`)
- `<SUT_SPEC_PATH>`
- `<SUT_ENDPOINT_ENV>`
- `<SUT_DEFAULT_ENDPOINT>`
- `<DEPENDENCY_CONTRACT_REPO_URL>` when a dependency mock is needed
- `<DEPENDENCY_SPEC_PATH>` when a dependency mock is needed
- `<DEPENDENCY_ENDPOINT_ENV>` when a dependency mock is needed
- `<DEPENDENCY_DEFAULT_ENDPOINT>` when a dependency mock is needed
- protocol-specific values such as broker URL, host, port, import paths,
  protoc version, request timeout, or examples directories when required

## specmatic.yaml

Place the generated `specmatic.yaml` at the generated sample root. Keep base
URLs, service endpoints, broker URLs, ports, import paths, and examples
directories configurable so tests can avoid occupied resources.

Use the resolved `run_option_key` from `config/contract-resolution.yaml` rather
than duplicating protocol-specific examples in this guide.

| Protocol | Spec format | Run option key | Endpoint/config shape |
|---|---|---|---|
| REST/OpenAPI | `openapi` | `openapi` | HTTP `baseUrl` |
| Kafka/AsyncAPI | `asyncapi` | `asyncapi` | broker/server settings from the resolved contract |
| gRPC | `protobuf` | `protobuf` | host, port, import paths, protoc version |
| GraphQL | `graphqlsdl` | `graphqlsdl` | host, port, examples directory when required |
| SOAP/WSDL | `wsdl` | `wsdl` | HTTP `baseUrl` plus WSDL SOAP metadata |

```yaml
version: 3
systemUnderTest:
  service:
    definitions:
      - definition:
          source:
            git:
              url: <CONTRACT_REPO_URL>
          specs:
            - <SUT_SPEC_PATH>
    runOptions:
      <RUN_OPTION_KEY>:
        type: test
        <PROTOCOL_SPECIFIC_OPTIONS>: <VALUES_FROM_RESOLVED_CONTRACT>
```

Consumer samples such as BFF or Frontend samples may need one or more Specmatic
mock dependencies. Add each dependency under `dependencies.services`.

```yaml
dependencies:
  services:
    - service:
        definitions:
          - definition:
              source:
                git:
                  url: <DEPENDENCY_CONTRACT_REPO_URL>
              specs:
                - <DEPENDENCY_SPEC_PATH>
        runOptions:
          <RUN_OPTION_KEY>:
            type: mock
            <PROTOCOL_SPECIFIC_OPTIONS>: <VALUES_FROM_RESOLVED_CONTRACT>
```

## Contract Source Of Truth

See `SKILL.md` Step 3 for contract source resolution and source-of-truth rules.
This file only describes how to assemble Specmatic runtime wiring after the
executable contract paths have been resolved.

## Test-Library / Runtime-Framework Dependency Conflicts

The Specmatic test library ships transitive dependencies at specific versions.
Runtime frameworks chosen by the user often pin the same transitives at
different versions through a dependency-management block, lockfile, or
equivalent. When the chosen stack's pinned version is older than what the
Specmatic test library was built against, the test command fails at runtime
with a linkage / missing-method / class-not-found error referencing a
third-party class.

Resolve generically:

1. Pick a Specmatic test-library version that supports the generated
   `specmatic.yaml` schema version. Each language binding (JVM, Node.js,
   Python, etc.) publishes its own schema-version-to-library-version mapping
   in its release notes.
2. Run the generated test command once as part of Step 6 of the workflow.
3. If the command fails with a JVM/runtime linkage error pointing at a
   third-party class, identify the conflicting transitive and override it
   using the chosen stack's standard override mechanism (the package manager
   and build file's property, resolution, or override syntax).
4. Pin only to the version the Specmatic test library declares; do not
   over-pin or upgrade unrelated dependencies.

Treat this as a build-fix step, not a content-generation step. The broader
rule already lives in Step 6 of the workflow: a generated sample is not done
until its test command exits cleanly.

## Specmatic Package Interface Discovery

After installing dependencies, verify how the selected language package invokes
Specmatic before finalizing the generated test adapter:

- Prefer the package's documented test-library API when it cleanly starts mocks,
  starts the app, runs tests, returns failures, and shuts down.
- If the package exposes a CLI, use that CLI in the generated test command.
- If the package exposes a bundled Specmatic JAR rather than a CLI, invoke the
  JAR from the generated test adapter and assert its process exit code.
- If the installed package cannot parse the generated `specmatic.yaml` version,
  select a compatible package/runtime combination and reinstall before changing
  contract behavior.

## Contract Test Adapter Patterns

The contract test adapter starts the generated app, runs Specmatic, then stops
the app. It must surface startup/listen errors and Specmatic failures clearly.

Adapter requirements for every language:

- Resolve host, port, base URL, and service endpoint from generated config or
  environment.
- Resolve broker URL, service endpoint, import paths, examples directories, and
  protocol-specific timeouts from generated config or environment.
- Start dependency mocks/stubs before running consumer-side contract tests.
- Start the generated app on the configured host, port, endpoint, or broker.
- Fail fast if the app or any dependency mock cannot bind its configured port.
- Ensure the mock process and generated app agree on the same configured mock
  endpoint or broker; pass the dependency endpoint into both sides when tests
  allocate dynamic resources.
- Run Specmatic against the configured endpoint.
- Assert the Specmatic result has zero failures instead of only printing results.
- Stop the app and all dependency mocks in teardown, even when Specmatic fails.

### Language Notes

- Node.js / JavaScript / TypeScript samples should include the Specmatic package
  and a test framework dependency.
- Node.js samples using ES modules must configure the test runner so ESM imports
  work under the selected framework.
- Prefer Specmatic's language wrapper APIs for starting and stopping dependency
  mocks when available. If a wrapper leaves test-runner handles open after
  successful teardown, document and configure the minimal runner option needed
  for the documented test command to exit cleanly.
- Java samples should include the Specmatic JUnit 5 support dependency.
- Python samples should include the Specmatic Python package and pytest or the
  chosen generated test framework.
- Non-REST protocols that require `specmatic/enterprise` should run that Docker
  image or the matching Enterprise language artifact in local tests and CI.

## How Specmatic Tests Work

1. Specmatic reads the generated `specmatic.yaml`.
2. Specmatic fetches the configured contract source.
3. Specmatic parses the resolved contract for the selected protocol.
4. Specmatic sends requests, RPC calls, GraphQL operations, SOAP messages, or
   broker messages to the generated app at the configured endpoint.
5. Specmatic validates response status, content type, schema, message payload,
   metadata, or protocol-specific output.
6. Specmatic reports pass/fail results.

When tests fail, read the generated report files before changing code. Prefer
the JUnit XML or Specmatic report output because it identifies the exact status,
content type, schema, and example mismatches that must be fixed.

Classify failures before editing generated behavior:

- SUT contract mismatch: generated app response does not match the SUT spec.
- Dependency mock mismatch: generated client call does not match a dependency
  spec, including dependency-only security, required headers, query params, or
  path differences.
- Runtime/tooling mismatch: selected package, runtime, or adapter cannot run
  the configured Specmatic version.
- Startup/config mismatch: app, mock, port, base URL, endpoint, or broker
  wiring failed.
