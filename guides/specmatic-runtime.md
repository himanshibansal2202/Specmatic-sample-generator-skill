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
- `<SPECMATIC_INTEGRATION_MODE>` (`cli`, `docker-cli`, `test-container`, or
  `native`)
- protocol-specific values such as broker URL, host, port, import paths,
  protoc version, request timeout, or examples directories when required

## specmatic.yaml

Place the generated `specmatic.yaml` at the generated sample root. It is the
only Specmatic configuration source for the sample. Test adapters must not build
Specmatic YAML strings, write a second generated config file, copy generated
YAML over the root config, or mutate `specmatic.yaml` during tests.

Keep base URLs, service endpoints, broker URLs, ports, import paths, and
examples directories configurable so tests can avoid occupied resources. Prefer
Specmatic-supported template values in the checked-in `specmatic.yaml`; test
adapters should set environment variables consumed by those template values.

Use the resolved `run_option_key` from `config/contract-resolution.yaml` rather
than duplicating protocol-specific examples in this guide.

| Protocol | Spec format | Run option key | Endpoint/config shape |
|---|---|---|---|
| REST/OpenAPI | `openapi` | `openapi` | HTTP `baseUrl` |
| Kafka/AsyncAPI | `asyncapi` | `asyncapi` | broker/server settings from the resolved contract |
| gRPC | `protobuf` | `protobuf` | host, port, import paths, protoc version |
| GraphQL | `graphqlsdl` | `graphqlsdl` | host, port, examples directory when required |
| SOAP/WSDL | `wsdl` | `wsdl` | HTTP `baseUrl` plus WSDL SOAP metadata |

When the selected runtime supports Specmatic configuration schema `version: 3`,
use the official component/reference style so SUT and dependency service
definitions and run options are not duplicated:

```yaml
version: 3
systemUnderTest:
  service:
    $ref: "#/components/services/<SUT_SERVICE_ID>"
    runOptions:
      $ref: "#/components/runOptions/<SUT_RUN_OPTIONS_ID>"
components:
  sources:
    <CONTRACT_SOURCE_ID>:
      git:
        url: <CONTRACT_REPO_URL>
  services:
    <SUT_SERVICE_ID>:
      definitions:
        - definition:
            source:
              $ref: "#/components/sources/<CONTRACT_SOURCE_ID>"
            specs:
              - <SUT_SPEC_PATH>
  runOptions:
    <SUT_RUN_OPTIONS_ID>:
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
        $ref: "#/components/services/<DEPENDENCY_SERVICE_ID>"
        runOptions:
          $ref: "#/components/runOptions/<DEPENDENCY_RUN_OPTIONS_ID>"
```

If a verified runtime requires a different official configuration shape, use
that runtime's documented shape, but preserve the single-config rule.

## External Examples and Data Directories

BFF and other consumer samples that need Smart Resiliency Orchestration (202/429
patterns) must include external example JSON files. These drive Specmatic's test
generation and mock behavior.

Configure example directories in `specmatic.yaml` under the `data` key:

- **SUT examples**: tell Specmatic which request/response combinations to test.
  Located under `systemUnderTest.service.data.examples`.
- **Dependency stub examples**: tell the mock how to behave for specific
  requests (including delayed responses with `"delay-in-seconds"` and
  `"transient": true`). Located under each dependency service's `data.examples`.
- **Response processor hooks**: shell scripts that transform mock responses
  dynamically. Located under `dependencies.data.adapters`.

Example directory structure:
```
src/test/resources/
├── bff/                          # SUT examples (test expectations)
│   ├── test_accepted_product_request.json   # expect 202 for specific request
│   └── test_products_too_many_requests.json # expect 429 for specific request
└── domain_service/               # Dependency stub examples (mock behavior)
    ├── stub_product_201.json     # normal response
    ├── stub_timeout_post_product.json  # delayed response (triggers BFF timeout)
    └── stub_products_200.json    # normal list response
```

Transient delayed stubs (`"transient": true, "delay-in-seconds": N`) are
consumed after first use. This enables the retry pattern: first call times out,
retry gets an instant response.

## Runtime Value Templates

Use stable default ports from the root workflow, but make every runtime endpoint
overridable through the root `specmatic.yaml`. The generated application config
and Specmatic config must use the same environment variable names.

Recommended names:

- SUT HTTP base URL: `{SUT_BASE_URL:http://localhost:8080}`.
- SUT gRPC host and port: `{SPECMATIC_SUT_HOST:host.docker.internal}` and
  `{SUT_PORT:8080}` for Docker-based verification, or
  `{SPECMATIC_SUT_HOST:localhost}` for host-network modes.
- Dependency HTTP mock URL: `{STUB_BASE_URL:http://localhost:8090}`.
- Dependency gRPC mock host and port: `{SPECMATIC_STUB_HOST:localhost}` and
  `{STUB_PORT:8090}`.
- Kafka/AsyncAPI broker settings: `{BROKER_HOST:localhost}`,
  `{BROKER_PORT:9092}`, or `{BROKER_URL:localhost:9092}` depending on the
  resolved contract shape.

Examples:

```yaml
openapi:
  type: test
  baseUrl: "{SUT_BASE_URL:http://localhost:8080}"
```

```yaml
protobuf:
  type: test
  host: "{SPECMATIC_SUT_HOST:host.docker.internal}"
  port: "{SUT_PORT:8080}"
  importPaths:
    - .specmatic_grpc_working_dir
  protocVersion: 3.23.4
  requestTimeout: "{SPECMATIC_REQUEST_TIMEOUT_MS:10000}"
```

```yaml
asyncapi:
  type: mock
  inMemoryBroker:
    host: "{BROKER_HOST:localhost}"
    port: "{BROKER_PORT:9092}"
  servers:
    - host: "{BROKER_URL:localhost:9092}"
      protocol: kafka
```

When verification needs a non-default port because the default is occupied, set
the corresponding environment variable before running the generated test
command.

## Schema Resiliency Tests Configuration

Generated samples include a `schemaResiliencyTests` setting under the
top-level `specmatic:` key. This controls how many tests Specmatic generates
beyond the named examples in the contract.

**CRITICAL: Do NOT look up specmatic.yaml configuration syntax from online
documentation.** The online docs are inconsistent and will lead to silently
broken config. Instead, use the structure from the reference repo
`specmatic-order-bff-java` (`specmatic.yaml` at the repo root) as the
authoritative template for `specmatic.yaml` generation.

The correct path for settings is `specmatic.settings.test`, NOT
`components.settings.test`. The correct path for governance is
`specmatic.governance`, NOT under `components`. The `filter` and `actuatorUrl`
go under `runOptions.<protocol>.filter` and `runOptions.<protocol>.actuatorUrl`.

```yaml
# ✅ CORRECT — from specmatic-order-bff-java reference
specmatic:
  governance:
    successCriteria:
      minCoveragePercentage: 70
      maxMissedOperationsInSpec: 1
      enforce: true
  settings:
    test:
      schemaResiliencyTests: all
```

```yaml
# ❌ WRONG — silently ignored, DO NOT USE even if online docs show this
components:
  settings:
    test:
      schemaResiliencyTests: all
```

| Value | Behavior |
|-------|----------|
| `none` | Tests from named examples only (default in delivered samples) |
| `positiveOnly` | Adds all valid request combinations (enum permutations, optional fields present/absent) |
| `all` | Adds negative/boundary tests (nulls, wrong types, missing required fields — expects 400 responses) |

During generation, the skill uses progressive verification (none → positiveOnly
→ all) to fix issues incrementally. The final delivered `specmatic.yaml` ships
with `none` so users get immediate green tests. The generated README documents
how to enable higher levels.

Test count must not decrease when moving to a higher level. A count drop signals
misconfiguration — stop and investigate rather than proceeding.

### Known Behaviors

- Specmatic silently ignores `schemaResiliencyTests` if placed under the wrong
  yaml path. The correct path is `specmatic.settings.test` (top-level
  `specmatic:` key). Placing it under `components.settings.test` has no effect.
- If the `SPECMATIC_GENERATIVE_TESTS=true` env var produces more tests but the
  yaml setting does not, the yaml path is wrong.
- At `all` level, Specmatic generates negative tests for enum parameters even
  when the contract does not define a 4xx response for that endpoint. This
  creates unresolvable failures — see SKILL.md Step 6 "Level 3 Known Patterns".

## Governance and Coverage Threshold

Generated samples must include governance configuration in `specmatic.yaml`
that reports API coverage. Configure:

- **Coverage threshold**: align with Specmatic's reference samples. The
  reference BFF uses 70%, the reference backend uses 65%. Use these as
  baselines — do not set to 100% since WIP tags and filtered paths reduce
  achievable coverage.
- **Max missed operations**: 1 for BFF, up to 4 for backend (mirrors reference repos).
- **Enforce**: true — makes coverage failures break the build when below threshold.
- **Report formats**: include HTML for readable reports.

For the exact syntax, consult the Specmatic configuration docs at
https://docs.specmatic.io/references/configuration/reports or reference the
`specmatic.yaml` in `specmatic-order-bff-java` for a working example. The
config format may evolve between Specmatic versions — always use the syntax
supported by the resolved Specmatic version.

**Never lower the coverage threshold to make tests pass.** If coverage is below
the threshold, the correct fix is to implement the missing operations or fix
the actuator/endpoint-discovery configuration — not to weaken the gate.

## Path Filtering and Actuator

For BFF and Backend samples, configure path filters to exclude infrastructure
paths that the service does not implement as business endpoints (e.g.,
`/health`, `/monitor/{id}`, `/swagger`).

Also configure the actuator/endpoint-discovery URL when the framework supports
it (e.g., Spring Boot's `/actuator/mappings`). This enables Specmatic to report
coverage accurately — endpoints in the spec but not implemented show as "Not
Implemented" in the coverage report.

For the exact filter and actuator syntax, consult Specmatic's test configuration
docs or reference the `specmatic-order-bff-java` repo's `specmatic.yaml`.
The filter expression format and actuator config shape may change between
Specmatic versions.

## Contract Source Of Truth

See `SKILL.md` Step 3 for contract source resolution and source-of-truth rules.
This file only describes how to assemble Specmatic runtime wiring after the
executable contract paths have been resolved.

## Build Tool Selection for JVM Samples

For JVM (Java/Kotlin) samples using Specmatic Enterprise, prefer **Gradle** over
Maven. The Enterprise `executable` artifact declares `jackson-bom` as a
compile-scope dependency without specifying `type=pom`, which causes Maven to
fail resolving it as a JAR. Gradle handles BOM-type dependencies correctly
without workarounds.

If Maven must be used, apply these workarounds:
- Exclude `jackson-bom` from the Enterprise `executable` dependency
- Import `jackson-bom` separately in `<dependencyManagement>` with `<type>pom</type>` and `<scope>import</scope>`
- Exclude `org.webjars.npm` and `org.webjars` groups (contain missing artifacts)

The reference samples (`specmatic-order-bff-java`, `specmatic-order-api-java`)
use Gradle. Follow the same pattern when generating JVM samples.

## Test-Library / Runtime-Framework Dependency Conflicts

The Specmatic test library ships transitive dependencies at specific versions.
Runtime frameworks chosen by the user often pin the same transitives at
different versions through a dependency-management block, lockfile, or
equivalent. When the chosen stack's pinned version is older than what the
Specmatic test library was built against, the test command fails at runtime
with a linkage / missing-method / class-not-found error referencing a
third-party class.

Resolve generically:

1. Look up the latest released Specmatic Enterprise version before setting the
   dependency version. Do not rely on training data — always check online.
   - **JVM**: check Docker Hub tags for `specmatic/enterprise` for the latest
     version, then use the corresponding Maven artifact
     `io.specmatic.enterprise:specmatic-enterprise`
   - **Node.js**: check npm for `specmatic`
   - **Python**: check PyPI for `specmatic`
   - **Docker image**: `specmatic/enterprise` (always use Enterprise, not the
     open-source `specmatic/specmatic` image)

   Always use Specmatic Enterprise for ALL generated samples regardless of
   protocol. Enterprise includes schema resiliency tests, Smart Resiliency
   Orchestration (429/202), full API coverage reporting, and all protocol
   support. Open-source Specmatic should not be used in generated samples.
2. Pick a Specmatic test-library version that supports the generated
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

## Specmatic Integration Interface Discovery

After installing dependencies, verify how the selected language/runtime invokes
Specmatic for the selected integration mode before finalizing the generated test
adapter:

- For `cli`, verify the package CLI, standalone CLI, or downloaded JAR command.
  Prefer current official documentation or Maven Central executable artifacts
  when selecting the runtime. Do not copy or infer Specmatic runtime versions
  from existing samples.
- For `docker-cli`, verify Docker availability and the official Specmatic image
  tag needed for the selected protocol.
- For `test-container`, verify the language's Testcontainers dependency and the
  official Specmatic Docker image tag needed for the selected protocol.
- For `native`, verify the official native test dependency and API for the
  selected language, protocol, and test framework.
- If the installed package cannot parse the generated `specmatic.yaml` version,
  select a compatible package/runtime combination and reinstall before changing
  contract behavior.

## Integration Modes

The selected mode is a user input. It controls test wiring, dependencies,
README instructions, and CI setup. It does not change contract resolution,
`specmatic.yaml`, or generated application behavior.

### CLI

Use `cli` when the sample should invoke Specmatic as an external executable.

- Start the generated app and any dependency mocks/stubs needed by the sample.
- Run Specmatic with the verified CLI command, standalone JAR, or package CLI.
  For standalone JAR wiring, the command shape is `java -jar specmatic.jar test`
  from the generated sample root unless the verified distribution documents a
  different invocation.
- Pass endpoint, port, broker, examples, and report settings through
  `specmatic.yaml`, environment variables, or CLI flags supported by the
  verified Specmatic version.
- Capture Specmatic stdout/stderr and generated reports, fail on non-zero exit,
  and assert that reported failures are zero.
- Local and CI prerequisites include Java 17+ when the selected CLI/JAR requires
  it.

### Docker CLI

Use `docker-cli` when Specmatic should run as a direct Docker command rather
than as a language dependency or a Testcontainers-managed container.

- Start the generated app and any dependency mocks/stubs needed by the sample.
- Run Specmatic with `docker run` from the generated test command or test
  adapter. Use the official `specmatic/enterprise` image for all protocols.
- Mount the root `specmatic.yaml`, any local contract/example files, and the
  report output directory into the container. Mount the config read-only when
  the selected tooling supports it. If contracts are fetched from git, pass the
  network and credential configuration needed by Specmatic.
- Configure the SUT endpoint so the container can reach the generated app. Use
  host networking, a shared Docker network, or documented host aliases
  appropriate for the target OS instead of assuming container-local
  `localhost`. Pass endpoint overrides as environment variables consumed by
  `specmatic.yaml`.
- Capture container logs and reports, fail on non-zero exit, and assert that
  reported failures are zero.
- Local and CI prerequisites include Docker, but must not require local Java for
  Specmatic itself.

### Test Container

Use `test-container` when Specmatic should run from Docker inside the generated
test suite.

- Add the selected language's standard Testcontainers dependency and generate a
  test adapter that starts the generated app, then starts the Specmatic
  container.
- Use the official `specmatic/enterprise` image for all protocols.
- Mount the root `specmatic.yaml`, any local contract/example files, and the
  report output directory into the container. Mount the config read-only when
  the selected tooling supports it. If contracts are fetched from git, pass
  network and credential configuration needed by Specmatic.
- Configure the SUT endpoint so the container can reach the generated app. Use
  Testcontainers host access or network aliases rather than hardcoded
  localhost assumptions. Pass endpoint overrides as environment variables
  consumed by `specmatic.yaml`.
- Stream container logs into the test output, fail on non-zero container exit,
  and assert that reported failures are zero.
- Local and CI prerequisites include Docker, but must not require local Java
  outside the container.

### Native

Use `native` when the selected language has an official native Specmatic test
integration for the selected protocol. This covers Java/Kotlin test interfaces,
Python pytest APIs, Node.js/TypeScript package APIs, and other documented
language-level integrations.

- Generate the native contract test class/module using the current official API
  for the language and test framework. For JVM/JUnit 5 stacks, prefer
  `io.specmatic.test.SpecmaticContractTest` when current official documentation
  uses it, rather than older support-class names.
- Keep `specmatic.yaml` at the sample root and configure the native test
  integration to read it, start the generated app, and run the contract tests.
- For Python, use the official Specmatic Python API/decorator style published
  for the selected package version, with pytest if that is the generated test
  framework.
- For Node.js/TypeScript, use the official Specmatic npm package API when it
  supports the selected protocol, such as `testWithApiCoverage` for OpenAPI.
- Fail fast when the app cannot start or the native Specmatic result contains
  failures. Do not hide failures behind ordinary unit-test assertions.
- Local and CI prerequisites follow the native package. JVM native integration
  normally still requires JDK/JRE 17+.

## Protocol Support By Mode

- REST/OpenAPI may use `cli`, `docker-cli`, `test-container`, or `native` when
  the selected language/framework supports the chosen adapter.
- gRPC/Protobuf, GraphQL SDL, SOAP/WSDL, and Kafka/AsyncAPI must use an
  Enterprise-capable Specmatic runtime unless current official documentation
  proves community support for that protocol and mode.
- For Enterprise protocols, prefer `docker-cli` or `test-container` when a
  native Enterprise language artifact is not verified. Do not accept a native
  adapter that parses `specmatic.yaml` but reports no executable contract tests.
- For gRPC/Protobuf with Docker-based Enterprise runtimes, keep host, port,
  import paths, `protocVersion`, and request timeout in the root
  `specmatic.yaml`. Pin or pass the verified `PROTOC_VERSION` when the runtime
  requires it on the current platform. Staging imported proto files into an
  ignored runtime directory is allowed; staging or generating another
  Specmatic config is not allowed.

## Contract Test Adapter Patterns

The contract test adapter starts the generated app, runs Specmatic, then stops
the app. It must surface startup/listen errors and Specmatic failures clearly.

Adapter requirements for every language:

- Resolve host, port, base URL, and service endpoint from the generated app
  config or environment, and make sure the same values are exposed to
  Specmatic through template values in the root `specmatic.yaml`.
- Resolve broker URL, service endpoint, import paths, examples directories, and
  protocol-specific timeouts from the generated app config or environment, and
  keep Specmatic runtime settings in the root `specmatic.yaml`.
- Start dependency mocks/stubs before running consumer-side contract tests.
- Start the generated app on the configured host, port, endpoint, or broker.
- Fail fast if the app or any dependency mock cannot bind its configured port.
- Ensure the mock process and generated app agree on the same configured mock
  endpoint or broker; pass the dependency endpoint into both sides when tests
  allocate dynamic resources.
- Run Specmatic against the configured endpoint.
- Assert the Specmatic result has zero failures instead of only printing results.
- Stop the app and all dependency mocks in teardown, even when Specmatic fails.
- If a runtime needs support files such as imported protos, stage only those
  support files into ignored runtime directories.

### Language Notes

- Node.js / JavaScript / TypeScript samples should include the Specmatic package
  and a test framework dependency when the selected mode needs a package-level
  integration.
- Node.js samples using ES modules must configure the test runner so ESM imports
  work under the selected framework.
- In `native` mode, prefer Specmatic's language wrapper APIs for starting
  and stopping dependency mocks when available. If a wrapper leaves test-runner
  handles open after successful teardown, document and configure the minimal
  runner option needed for the documented test command to exit cleanly.
- Java samples should include the Specmatic JUnit 5 support dependency in
  `native` mode, the selected Testcontainers dependency in `test-container`
  mode, direct Docker wiring in `docker-cli` mode, or the CLI/JAR wiring in
  `cli` mode.
- Python samples should include the Specmatic Python package and pytest for
  native integration, the selected Testcontainers dependency for
  `test-container`, direct Docker wiring for `docker-cli`, or CLI/JAR wiring for
  `cli`.

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
- Startup/config mismatch: app, mock, port, base URL, endpoint, broker wiring,
  or Specmatic template value wiring failed.
- Runtime config duplication: generated tests create or mutate another
  Specmatic config instead of using the checked-in root `specmatic.yaml`.
