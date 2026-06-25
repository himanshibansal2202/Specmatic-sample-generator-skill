# Specmatic Runtime & Contract Test Patterns

Generator guidance for assembling the Specmatic runtime configuration and the
contract test adapter in a generated sample. It describes intent and structure;
contract behavior always comes from the executable contract, not this guide.

For all config syntax, use the official Specmatic documentation as the source of
truth (the syntax is version-dependent and changes between releases):

| Topic | Official doc |
|---|---|
| Full `specmatic.yaml` / `specmatic.json` attributes | https://docs.specmatic.io/documentation/specmatic_json.html |
| Contract tests: `filter`, `actuatorUrl`, coverage | https://docs.specmatic.io/documentation/contract_tests.html |
| Contract testing concepts | https://docs.specmatic.io/contract_driven_development/contract_testing.html |
| Resiliency / generative tests | https://docs.specmatic.io/getting_started/mcp_auto_test.html |

Concrete contract repository URLs, spec formats, run option keys, and spec paths
come from `config/contract-resolution.yaml`, user input, or runtime contract
discovery. Default ports come from the root `SKILL.md` conventions.

## Required Inputs

Resolve these before writing the generated sample's `specmatic.yaml`:

| Input | Notes |
|---|---|
| `CONTRACT_REPO_URL` | Source Specmatic fetches contracts from |
| `SPEC_FORMAT` | `openapi`, `asyncapi`, `protobuf`, `graphqlsdl`, `wsdl` |
| `RUN_OPTION_KEY` | Resolved from `config/contract-resolution.yaml` |
| `SUT_SPEC_PATH` | System-under-test contract path |
| `SUT_ENDPOINT_ENV` / `SUT_DEFAULT_ENDPOINT` | Endpoint + env override |
| `DEPENDENCY_*` (repo, spec path, endpoint env, default) | One set per dependency mock, when needed |
| `SPECMATIC_INTEGRATION_MODE` | `cli`, `docker-cli`, `test-container`, `native` |
| Protocol-specific values | broker URL/host/port, import paths, protoc version, request timeout, examples dirs — only when the protocol requires them |

## specmatic.yaml Assembly

Rules of intent (for exact shape, follow the docs linked above):

- The generated `specmatic.yaml` sits at the sample root and is the **single**
  Specmatic config source. Test adapters must not build YAML strings, write a
  second config, copy YAML over the root config, or mutate it during tests.
- Define the SUT and each dependency once and reference them, so service and
  run-option definitions are not duplicated. See `specmatic_json.html`.
- Consumer samples (BFF, Frontend) declare each dependency mock as a service.
- Keep base URLs, endpoints, broker URLs, ports, import paths, and examples
  directories overridable via environment variables consumed by the config, so
  tests can avoid occupied resources. The generated app config and
  `specmatic.yaml` must use the same env var names.

Use the resolved `run_option_key` from `config/contract-resolution.yaml`:

| Protocol | Spec format | Run option key | Endpoint/config shape |
|---|---|---|---|
| REST/OpenAPI | `openapi` | `openapi` | HTTP base URL |
| Kafka/AsyncAPI | `asyncapi` | `asyncapi` | broker/server settings from the resolved contract |
| gRPC | `protobuf` | `protobuf` | host, port, import paths, protoc version |
| GraphQL | `graphqlsdl` | `graphqlsdl` | host, port, examples directory when required |
| SOAP/WSDL | `wsdl` | `wsdl` | HTTP base URL plus WSDL SOAP metadata |

Recommended overridable value names (defaults from the root workflow):

| Purpose | Env var → default |
|---|---|
| SUT HTTP base URL | `SUT_BASE_URL` → `http://localhost:8080` |
| SUT gRPC host / port | `SPECMATIC_SUT_HOST` (`host.docker.internal` for Docker, `localhost` for host-network) / `SUT_PORT` → `8080` |
| Dependency HTTP mock URL | `STUB_BASE_URL` → `http://localhost:8090` |
| Dependency gRPC mock host / port | `SPECMATIC_STUB_HOST` → `localhost` / `STUB_PORT` → `8090` |
| Kafka/AsyncAPI broker | `BROKER_HOST` / `BROKER_PORT` (`9092`) / `BROKER_URL` |

## Schema Resiliency Tests

`schemaResiliencyTests` controls how many tests Specmatic generates beyond the
named examples. Configure it per the docs (resiliency link above). Levels:

| Value | Behavior |
|---|---|
| `none` | Tests from named examples only |
| `positiveOnly` | Adds all valid request combinations (enum permutations, optional fields present/absent) |
| `all` | Adds negative/boundary tests (nulls, wrong types, missing required fields — expects 4xx) |

The skill uses progressive verification (none → positiveOnly → all) during
generation to isolate failures by category (see `SKILL.md` Step 6). Ship the
highest level that passes cleanly; only drop below `all` for documented,
unresolvable contract gaps (see Findings).

## Governance and Coverage

Generated Backend and BFF samples must report API coverage with governance
enforced. Configure `successCriteria` (threshold, max missed operations,
`enforce`) per `contract_tests.html`. Principles:

- Endpoint discovery + infra filtering is a **hard requirement** — coverage is
  meaningless without it. A sample reporting "cannot calculate actual coverage"
  is not done.
- Set the threshold to a value the fully-implemented, infra-filtered sample
  actually achieves; aim high, never lower it just to go green.
- Treat every missed/Not-Implemented operation as something to implement.

## Path Filtering and Actuator

Configure `filter` and `actuatorUrl` per `contract_tests.html` (syntax is
version-dependent; verify against the resolved runtime).

- Filter only framework/infrastructure endpoints that are not contract-owned
  (e.g. `/health`, `/swagger`). Never filter contract-declared endpoints — if a
  contract declares `/monitor/{id}`, implement and verify it.
- Endpoint discovery is required, not optional. Wire it with whatever the
  framework supports, then point `specmatic.yaml` at it:
  - Spring Boot: enable the actuator, expose `/actuator/mappings`.
  - Other frameworks: expose a route/endpoint listing, or serve the OpenAPI
    document via a Swagger UI / OpenAPI endpoint Specmatic can read.
- The app config and `specmatic.yaml` must agree on the discovery URL, and it
  must be reachable during the run.

## Integration Modes

The mode is a user input. It controls test wiring, dependencies, README, and CI.
It does **not** change contract resolution, `specmatic.yaml`, or app behavior.

| Mode | When | Runtime artifact | Local/CI prereqs | Key constraint |
|---|---|---|---|---|
| `cli` | Run Specmatic as an external executable | `io.specmatic.enterprise:executable-all:<latest>` (Maven Central) | Java 17+ | Invoke `java -jar <jar> test` directly. Do NOT route through a JUnit/`ContractTest`/pytest adapter — that is `native`. |
| `docker-cli` | Run Specmatic as a direct `docker run` | `specmatic/enterprise:<tag>` | Docker, no local Java for Specmatic | Mount root `specmatic.yaml` + contracts/reports; reach the app via host networking / shared network / host alias. |
| `test-container` | Run Specmatic from Docker inside the test suite | `specmatic/enterprise:<tag>` | Docker, no local Java outside the container | Use the language's Testcontainers dep; stream container logs; fail on non-zero exit. |
| `native` | Language has an official Enterprise-native test integration | `io.specmatic.enterprise:*` (JVM) or documented Enterprise-native package | Per package (JVM normally JDK/JRE 17+) | Use the official Enterprise API. Reject `native` if only the open-source/public package exists — ask the user for another mode. |

Common to all modes: start the app (and any dependency mocks/stubs) first; let
Specmatic resolve/cache contracts from `specmatic.yaml` (do not clone/copy the
repo yourself); run against the checked-in root config in place (do not relocate
it into build dirs); capture Specmatic's own reports (do not render your own);
fail on non-zero exit and assert zero reported failures.

### Protocol notes

- All protocols, including REST/OpenAPI, use an official Enterprise runtime.
- Prefer `docker-cli` or `test-container` when a native Enterprise language
  artifact is not verified. Do not accept a native adapter that parses
  `specmatic.yaml` but reports no executable contract tests.
- gRPC with Docker runtimes: keep host, port, import paths, `protocVersion`, and
  request timeout in `specmatic.yaml`; staging imported protos into an ignored
  runtime dir is allowed, staging another config is not.

## Contract Test Adapter

The adapter starts the app, runs Specmatic, then stops the app. It must surface
startup/listen errors and Specmatic failures clearly.

| Requirement | Detail |
|---|---|
| Resolve runtime values | host, port, base URL, endpoint, broker URL, import paths, examples dirs, timeouts — from app config/env, mirrored into `specmatic.yaml` template values |
| Start dependencies first | Start dependency mocks/stubs before consumer-side tests; ensure mock and app agree on the same endpoint/broker |
| Fail fast | If the app or any mock cannot bind its port, fail immediately |
| Assert, don't print | Assert zero Specmatic failures, not just log results |
| Teardown always | Stop app and all mocks in teardown, even when Specmatic fails |
| Stage support files only | If a runtime needs imported protos etc., stage only those into ignored runtime dirs |

Language notes: Node/TS samples include the test-framework dep and configure ESM
when needed; Java samples use the JUnit 5 support dep (`native`), Testcontainers
(`test-container`), direct Docker (`docker-cli`), or CLI/JAR (`cli`); Python
samples use pytest + the Enterprise-native package (`native`) or the equivalent
wiring per mode.

## How Specmatic Tests Work

1. Reads the generated `specmatic.yaml`.
2. Fetches the configured contract source.
3. Parses the resolved contract for the selected protocol.
4. Sends requests / RPC calls / GraphQL ops / SOAP messages / broker messages
   to the app at the configured endpoint.
5. Validates status, content type, schema, payload, metadata, or protocol output.
6. Reports pass/fail.

On failure, read the JUnit XML / Specmatic report before changing code, then
classify: SUT contract mismatch · dependency mock mismatch · runtime/tooling
mismatch · startup/config mismatch · runtime config duplication. Fix the
reported mismatch only.

## Contract Source Of Truth

See `SKILL.md` Step 3 for contract resolution and source-of-truth rules. This
file only covers runtime wiring after the contract paths are resolved.

## Test-Library / Framework Dependency Conflicts

The Specmatic test library ships transitive dependencies at specific versions; a
chosen framework may pin the same transitives older, causing
linkage/missing-method/class-not-found errors at test time. Resolve as a build
fix: look up the latest Enterprise version online (do not rely on training
data), pick a library version that supports the generated config schema version,
then override the conflicting transitive via the build tool's standard
mechanism — pinning only to the version Specmatic declares. See Findings.

## Exceptions / Caveats / Findings

Hard-won knowledge. These are findings to account for, not config to copy.

- **Resiliency yaml path is silent on error.** `schemaResiliencyTests` belongs
  under `specmatic.settings.test` (top-level `specmatic:` key), not under
  `components`. A wrong path is silently ignored — the run looks green with no
  generative tests. Verify the test count actually increases none → positiveOnly
  → all; a count that does not increase means the setting is being ignored.
- **Generative tests default.** Official docs indicate generative/resiliency
  tests are ON by default and disabled via `DISABLE_GENERATIVE_TESTS=true`.
  Reconcile with the `schemaResiliencyTests` value against the installed runtime
  version rather than assuming.
- **Test count must never drop across levels.** A decrease signals
  misconfiguration — stop and investigate, do not accept it as "all pass."
- **Enum without a 4xx response = unresolvable contract gap.** At `all`,
  Specmatic sends invalid enum values expecting 4xx; if the contract defines no
  4xx for that endpoint, 200 fails ("expected 4xx") and 400 fails ("spec has no
  4xx response"). This is a contract gap, not an app bug — document it and ship
  the highest cleanly-passing level. Confirm you are on the latest runtime first.
- **Reference repos do not ship 100% coverage.** `specmatic-order-bff-java` uses
  threshold 70 + max missed 1; the backend reference is ~65%. 100% is often
  unreachable because declared error responses (404/422/specific 400s) lack
  examples to exercise them — that requires examples in the contract, not a
  config or filter change.
- **Unexpectedly low coverage usually means discovery is not wired.** A ~35%
  result is typically the actuator/swaggerUI not being reachable, not a real
  gap. Log signatures: `Failed to query swaggerUI, status code: 404`,
  `EndpointsAPI and SwaggerUI URL missing; cannot calculate actual coverage`,
  `Actuator is not enabled`. Wire discovery before treating the gap as real.
- **License message ≠ Enterprise proof.** A license-initialization log does not
  prove the Enterprise runtime is in use. Verify the artifact coordinate,
  package name, jar path, or Docker image.
- **Config syntax is version-dependent.** Always confirm `specmatic.yaml`
  attribute names and shapes against the official docs for the resolved runtime
  version, not from memory or older samples.
