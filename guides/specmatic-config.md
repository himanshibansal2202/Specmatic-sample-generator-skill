# Specmatic Configuration & Contract Test Patterns

This guide describes how generated samples should assemble Specmatic
configuration. It is generator guidance only: do not copy it verbatim into a
sample, and do not use it to define API behavior.

Concrete contract repository URLs, OpenAPI spec paths, and default ports come
from `config/contract-resolution.yaml`, `config/stack-matrix.yaml`, user input,
or runtime contract discovery.

## Required Inputs

Resolve these values before writing a generated sample's `specmatic.yaml`:

- `<CONTRACT_REPO_URL>`
- `<SUT_OPENAPI_SPEC_PATH>`
- `<SUT_BASE_URL_ENV>`
- `<SUT_DEFAULT_BASE_URL>`
- `<DEPENDENCY_CONTRACT_REPO_URL>` when a dependency mock is needed
- `<DEPENDENCY_OPENAPI_SPEC_PATH>` when a dependency mock is needed
- `<DEPENDENCY_BASE_URL_ENV>` when a dependency mock is needed
- `<DEPENDENCY_DEFAULT_BASE_URL>` when a dependency mock is needed

## specmatic.yaml

Place the generated `specmatic.yaml` at the generated sample root. Keep base
URLs configurable so tests can avoid occupied ports.

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
            - <SUT_OPENAPI_SPEC_PATH>
    runOptions:
      openapi:
        type: test
        baseUrl: "{<SUT_BASE_URL_ENV>:<SUT_DEFAULT_BASE_URL>}"
```

### With Dependency Mocks

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
                - <DEPENDENCY_OPENAPI_SPEC_PATH>
        runOptions:
          openapi:
            type: mock
            baseUrl: "{<DEPENDENCY_BASE_URL_ENV>:<DEPENDENCY_DEFAULT_BASE_URL>}"
```

## Contract Source Of Truth

See `SKILL.md` Step 3 for contract source resolution and source-of-truth rules.
This file only describes how to assemble Specmatic configuration after the
executable contract paths have been resolved.

## Contract Test Adapter Patterns

The contract test adapter starts the generated app, runs Specmatic, then stops
the app. It must surface startup/listen errors and Specmatic failures clearly.

Adapter requirements for every language:

- Resolve host, port, and base URL from generated config or environment.
- Start dependency mocks before running consumer-side contract tests.
- Start the generated app on the configured host and port.
- Fail fast if the app or any dependency mock cannot bind its configured port.
- Run Specmatic against the configured base URL.
- Assert the Specmatic result has zero failures instead of only printing results.
- Stop the app and all dependency mocks in teardown, even when Specmatic fails.

### Language Notes

- Node.js / JavaScript / TypeScript samples should include the Specmatic package
  and a test framework dependency.
- Java samples should include the Specmatic JUnit 5 support dependency.
- Python samples should include the Specmatic Python package and pytest or the
  chosen generated test framework.

## How Specmatic Tests Work

1. Specmatic reads the generated `specmatic.yaml`.
2. Specmatic fetches the configured contract source.
3. Specmatic parses the resolved OpenAPI spec.
4. Specmatic sends example requests to the generated app at the configured base URL.
5. Specmatic validates response status, content type, and schema.
6. Specmatic reports pass/fail results.

When tests fail, read the generated report files before changing code. Prefer
the JUnit XML or Specmatic report output because it identifies the exact status,
content type, schema, and example mismatches that must be fixed.
