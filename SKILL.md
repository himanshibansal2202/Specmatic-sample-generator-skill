---
name: generate-specmatic-sample
description: Generate a working Specmatic v3 sample project for a given tech stack and protocol. Use when the user wants to create a Backend, BFF, or Frontend sample that demonstrates Specmatic contract testing. Triggers on requests like "generate a specmatic sample", "create a sample project for Java Spring Boot", or "scaffold a backend REST, gRPC, GraphQL, AsyncAPI, or SOAP service with contract tests".
---

# Generate Specmatic Sample

You are a code generator that creates working Specmatic sample projects. The generated project MUST pass Specmatic contract tests — this is the only definition of "done".

## Workflow

### Step 1: Collect Inputs (ONE AT A TIME)

Ask open-ended stack questions with a few example values in parentheses so the
user understands the expected shape of the answer. Examples are guidance, not an
exhaustive supported list. As answers are collected, use normal framework and
application architecture knowledge to avoid offering obviously incoherent later
choices. If uncertain, keep the option available and let validation plus
verification decide.

Ask each question separately. Wait for the user's answer before asking the next. Do NOT ask all questions at once.

1. First ask: "What application type? (for example: backend, bff, frontend)"
   - Wait for answer.
2. Then ask: "What protocol? (for example: rest/openapi, kafka/asyncapi, grpc, graphql, soap/wsdl)"
   - Wait for answer.
3. Then ask: "What contract version? (default v3 / latest / specific like v5)"
   - Wait for answer.
4. Then ask: "What language? (for example: javascript, typescript, java, python)"
   - Wait for answer.
5. Then ask: "What framework? (for example: express, spring-boot, flask)" —
   examples may be adjusted for the selected application type, protocol, or
   language.
   - Wait for answer.
6. Then ask: "What data layer? (for example: in-memory, rest-api, grpc-service, kafka-broker)" — examples
   may be adjusted for the selected application type or architecture.
   - Wait for answer.
7. Then ask: "Where should I create the sample folder? Provide a local path or repository link."
   - Wait for answer.

Only proceed to Step 2 after ALL answers are collected.

### Step 2: Validate Combination

Validate the selected stack in two passes:

1. Reasoned feasibility: decide whether the selected framework, language,
   application type, protocol, and data-fetch mode are coherent using general
   framework ecosystem knowledge, the selected role guide, and the architecture
   implied by the data-fetch mode. Reject only combinations that are clearly
   incoherent, and explain the reason briefly.
2. Verification feasibility: if the combination is coherent but support is
   uncertain, proceed to generation and let dependency installation, build, and
   Specmatic tests prove whether the sample is actually supported.

Do not maintain a hardcoded list of valid or invalid stack combinations in this
skill. Example values in prompts are guidance only; the skill reasons about
whether a chosen set of values makes sense.

### Step 3: Resolve Contract Source Of Truth

Before generating source code, determine the executable contract that Specmatic will use.

Source-of-truth order:

1. Passing Specmatic tests is the final definition of correctness.
2. The executable contract/spec referenced by `specmatic.yaml` is the behavioral source of truth.
3. Official Specmatic v3 and protocol documentation should be consulted when configuration syntax or contract semantics are unclear.
4. Local markdown files under `guides/` and `test-data/` are helper summaries. They must not override the executable contract.

Normalize protocol aliases before resolving contracts:

- `rest` and `openapi` use the OpenAPI spec format.
- `kafka` and `asyncapi` use the AsyncAPI spec format.
- `grpc` uses the Protobuf spec format.
- `graphql` uses the GraphQL SDL spec format.
- `soap` and `wsdl` use the WSDL spec format.

Read `config/contract-resolution.yaml` and use the selected protocol plus
application type to resolve the contract source for the selected sample. The
contract repository URL is required because Specmatic fetches the executable
contracts from it. Contract spec paths are resolved the same way for every
protocol:

1. Inspect filenames under the configured `spec_root` in the contract
   repository.
2. Match the selected role's discovery patterns for the selected spec format
   and requested/default contract version.
3. If a requested version is available, use that version.
4. If no version was requested, use `default_version`; if those files are not
   available, select the latest compatible discovered version.
5. Resolve exactly one system-under-test contract and exactly one contract for
   each required dependency. Write these exact resolved paths into the
   generated `specmatic.yaml`.
6. If multiple candidates match the same role/version, fail with a clear error
   instead of guessing.
7. Use configured official sample repositories only when no compatible contract
   is found in the central contract repo. If none is found there either, ask for
   a user-provided contract repo/path or fail before generating source code.

Before fetching from the network, check only approved contract-source locations:
the current generated sample's own `.specmatic/repos/<repo-name>` cache, an
isolated temporary checkout, or a user-provided contract path. Do not inspect
contract caches that live inside other generated sample folders.

Resolve these executable specs by sample type:

- Backend: provider/system-under-test contract for the selected protocol.
- BFF: system-under-test contract and any Backend dependency mock contract for
  the selected protocol.
- Frontend: dependency mock contract for the API, service, broker, or endpoint
  consumed by the generated client.

After resolving the applicable specs, inspect the applicable generation guide,
`guides/protocol-generation.md`, the Specmatic configuration guidance, and the
executable contracts. Build a contract facts summary before writing source code.
Use a structured parser for the selected spec format when available; otherwise
inspect the contract files directly. For OpenAPI and WSDL, the summary must
list, for every relevant operation:

- HTTP method and path
- request path/query/header parameters, including required flags
- request content types and request body schemas/examples
- response status codes, content types, schemas, and examples
- required, optional, and forbidden response fields
- security schemes and operation-level security requirements
- referenced external schema/response files that must be followed
- Stub/provider dependency behavior for consumer-side samples

For AsyncAPI, Protobuf/gRPC, and GraphQL, include the protocol-specific facts
listed in `guides/protocol-generation.md`.

For BFF and Frontend samples, compare the SUT/consumer contract with each
dependency contract before generating source code:

1. Identify candidate dependency operations by compatible role intent, method,
   request shape, and response shape.
2. Record any required adapter behavior: path translation, status translation,
   request body transformation, response body transformation, query/header
   filtering or forwarding, and dependency-only security headers.
3. If multiple dependency operations are equally plausible, or none can satisfy
   the SUT/consumer operation, fail before generating source code and report the
   ambiguous operation rather than guessing.
4. For dependency security schemes, satisfy the dependency contract in the
   generated client even when the SUT/consumer contract does not expose the same
   credential input. Specmatic mocks validate contract shape, not real
   credentials.

If the executable contract contradicts local guide or test-data notes, implement the executable contract and record the discrepancy in the final response.

### Step 4: Resolve the Output Folder

Derive the sample folder name from the validated enum values:

```text
<application-type>-<protocol>-<language>-<framework>-<data-fetch>
```

Use the selected enum values directly. For example,
`backend + rest + javascript + express + in-memory` becomes
`backend-rest-javascript-express-in-memory`.

- If the destination is a local path, create or update the sample at `<destination>/<sample-id>/`.
- If the destination is a repository link, clone or locate a local checkout of that repository first, then create or update the sample at `<checkout>/<sample-id>/`.
- Do not write generated sample files directly into the destination root.
- Do not create shared root-level contracts, metadata, workflows, or other shared generated assets.
- If the destination root already has `.github/workflows/samples-ci.yml`, update it to include the generated sample.
- Each generated sample folder must be self-contained and include every file needed to run, test, build, and understand that sample independently.

### Step 5: Generate the Project

For every sample, generate the complete file set listed in `guides/acceptance-criteria.md`.

Use `guides/specmatic-config.md` for Specmatic config structure and adapter
behavior, then fill generated files with resolved contract paths, spec format,
run option key, and role-specific ports/base URLs/broker settings.
Generate routes/controllers, message handlers, RPC services, GraphQL resolvers,
SOAP handlers, client calls, schemas, seed data, examples, and adapter
transformations from the contract facts summary produced in Step 3. Use role
guides only for architecture and responsibilities.

Default port conventions:

- Backend and BFF system-under-test services default to port `8080`.
- Specmatic dependency mocks/stubs default to port `8090`.
- Frontend dev servers default to port `3000` when a dev server is generated.
- AsyncAPI/Kafka samples default to the broker settings declared by the
  resolved contract, with host/port/topic overrides in generated config.
- All ports, base URLs, broker URLs, and service endpoints must be configurable
  through environment variables or generated config so tests can avoid occupied
  resources.

For a **Backend** sample, use `guides/backend-generation.md` for role behavior. Key differences:
- The Backend owns local Products and Orders state
- The Backend keeps an Inventory dependency boundary
- Seed data is required. See `test-data/backend-seed-data.md`

For a **BFF** sample, use `guides/bff-generation.md` for role behavior. Key differences:
- The BFF has NO local database — it calls the Backend contract boundary
- specmatic.yaml has a `dependencies` section that starts a mock/stub of the Backend
- The BFF app reads the Backend URL, service endpoint, or broker settings from environment variables (for example `STUB_URL`)
- No seed data needed — the Specmatic mock/stub handles Backend responses automatically

For a **Frontend** sample, use `guides/frontend-generation.md` for client workflow behavior. Key differences:
- The Frontend provides no API contract
- The Frontend consumes the Product and Order BFF contract for the selected protocol
- The BFF URL, service endpoint, or broker settings must be configurable
- Contract consumption is verified against a Specmatic mock/stub of the BFF contract

### Step 6: Verify And Converge

After generating all files, run verification from inside the generated sample folder:
1. Install dependencies
2. Discover and verify the selected Specmatic package interface for the chosen
   language: CLI command, test-library API, or bundled JAR. Confirm it supports
   the generated `specmatic.yaml` version before relying on it.
3. Run the test command (e.g., `npm test`)
4. **First run takes 1-3 minutes** — Specmatic git-clones the central contract repo (~50MB). Subsequent runs are fast (cached in `.specmatic/`).
5. If tests fail, read the error output and the generated Specmatic/JUnit/report files, fix the code to match the executable contract, and re-run
6. Repeat until ALL tests pass (max 3 retries)
7. Run the generated build/package command when the sample includes compiled output or Docker
8. Remove local verification artifacts from the generated sample folder when
   they are not source files, then re-run any build command affected by ignore
   files or build context.

**Timeout guidance:** If the test command runs for more than 5 minutes, something is wrong. Cancel and check:
- Is Java 17+ available? (`java -version`)
- Is there network access? (Specmatic needs to clone from GitHub)
- Is the port already in use?
- Did the app fail to bind its configured port?
- Did the test adapter swallow process startup errors?

When tests fail, classify the failure before changing code:
- status mismatch
- content type mismatch
- request or response schema mismatch
- missing route or method
- dependency mock mismatch
- protocol adapter mismatch
- runtime or Specmatic package compatibility mismatch
- startup or port binding failure

Use the classification to make the smallest behavior change needed to match the
executable contract, then re-run the documented test command.

Only report "done" when tests are green.

## Key Rules

- **Executable contract wins.** Local guides and test data are helper context; the executable contract and Specmatic results decide behavior.
- **Role intent lives in `guides/`.** Guides define responsibilities and architecture; Step 3 contract facts define contract behavior.
- **Samples are self-contained.** Include every file needed to run, test, build, and understand the sample inside the sample folder.
- **The destination root is only a container.** Generate under `<provided-location>/<sample-id>/`, never directly into `<provided-location>/`.
- **Ports must be configurable.** Keep documented default ports stable, but let
  tests override ports, base URLs, service endpoints, and broker settings so
  samples can run when defaults are occupied.
- **Startup failures must fail fast.** Test adapters must surface listen/bind errors, dependency startup failures, and Specmatic failures clearly.
- **Generated ownership must be complete.** Include lockfiles created by package managers when CI or local verification depends on them. Ignore dependency folders, build output, caches, and Specmatic reports.
- **Prompts must be example-driven.** User-facing stack questions include a few examples in parentheses, but compatibility is reasoned from framework knowledge, role guides, and verification results rather than hardcoded combination rows.
- **Report-driven fixes only.** On failures, read Specmatic/JUnit/report output and fix the reported contract mismatch rather than adding speculative validation or fallback logic.
- **Never read existing generated samples.** Do not read, inspect, or copy from other sample folders already present in the destination repository or monorepo, including their `.specmatic/` caches, build outputs, source files, tests, docs, configs, and manifests. Every file must be generated from the skill's `contracts/`, `guides/`, `test-data/`, configured official contract sources, isolated temporary checkouts, or user-provided contract paths only. Existing samples may target a different stack or contract version and will silently corrupt the new sample if used as a reference.
- **No request validation middleware is needed.** Specmatic tests the contract (response schema), not your input validation.
- **Keep the test adapter minimal.** Let the verified Specmatic package interface determine whether it uses a CLI, library API, or bundled JAR.
- **specmatic.yaml structure is protocol-aware.** Resolved contract paths,
  dependency specs, ports, base URLs, broker settings, and run option keys vary
  by protocol, role, and stack.

## References

- `guides/` — Role generation notes, Specmatic config, and acceptance criteria
- `test-data/backend-seed-data.md` — Required backend data entries for tests to pass
- `config/contract-resolution.yaml` — Contract repositories, protocol roots, and runtime discovery patterns
