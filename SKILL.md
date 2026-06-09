---
name: generate-specmatic-sample
description: Generate or maintain working Specmatic sample projects using the current supported Specmatic configuration schema for a given tech stack and protocol. Use when the user wants to create a Backend, BFF, or Frontend sample that demonstrates Specmatic contract testing, or when they want to update existing samples to align with contract changes, dependency upgrades, or Specmatic runtime/configuration updates. Triggers on requests like "generate a specmatic sample", "create a sample project for Java Spring Boot", "scaffold a backend REST, gRPC, GraphQL, AsyncAPI, or SOAP service with contract tests", "maintain my specmatic samples", or "update my samples repo".
---

# Specmatic Sample Skill

You are a code generator and maintainer that creates and keeps working Specmatic sample projects. The generated or maintained project MUST pass Specmatic contract tests — this is the only definition of "done".

Terminology:

- **Specmatic configuration schema version** is the `version` field in
  `specmatic.yaml` (for example `version: 3`). It describes the configuration
  file shape, not the Specmatic product/runtime release.
- **Specmatic runtime version** is the CLI, JAR, Docker image, native package,
  or Enterprise artifact version used to execute tests or mocks.
- **Contract spec version** is the API/contract file version resolved from the
  contract repository, such as `api_order_v3.yaml` or
  `product_search_bff_v4.yaml`.

## Mode Selection

When the skill is invoked, ask the user:

> "What would you like to do? (generate, maintain)"

- **generate** — Create a new sample project from scratch for a given tech stack.
- **maintain** — Update existing sample(s) to align with contract changes,
  dependency upgrades, or Specmatic runtime/configuration updates.

Wait for the user's answer before proceeding to the corresponding workflow.

---

## Generate Workflow

### Step 1: Collect Inputs (ONE AT A TIME)

The generation flow is **spec-driven**: the user provides the API spec first,
and the skill derives as much as possible from it before asking remaining
questions.

Ask each question separately. Wait for the user's answer before asking the next.

1. First ask: "What is your contract repo and spec path? (for example:
   https://github.com/specmatic/specmatic-order-contracts.git with spec
   io/specmatic/examples/store/openapi/product_search_bff_v6.yaml)"
   - Wait for answer. Accept either:
     - A git repo URL + spec path within it
     - A local file path to a spec
     - Just a contract repo URL (then ask which spec within it)

2. Then ask: "What application type? (for example: backend, bff, frontend)"
   - Wait for answer.

3. Then ask: "What language? (for example: javascript, typescript, java, python)"
   - Wait for answer.

4. Then ask: "What framework? (for example: express, spring-boot, flask)" —
   examples may be adjusted for the selected application type, protocol, or
   language.
   - Wait for answer.

5. Then ask: "What Specmatic integration mode? (for example: cli, docker-cli, test-container, native)"
   - Wait for answer.

6. Then ask: "Where should I create the sample folder? Provide a local path."
   - Wait for answer.

The **protocol** is inferred from the spec format (OpenAPI → REST, AsyncAPI →
Kafka, .proto → gRPC, .graphql → GraphQL, .wsdl → SOAP). Do not ask the user
for the protocol — derive it from the spec they provided.

The **data layer** is always `in-memory` for all generated samples. Do not ask.
Backend samples keep state in memory. BFF and Frontend samples have no local
state — their dependencies are auto-discovered from the contract specs.

The **contract version** is inferred from the spec filename or content. Do not
ask unless it cannot be determined.

Only proceed to Step 2 after ALL answers are collected.

### Step 2: Validate Combination

Validate the selected stack in two passes:

1. Reasoned feasibility: decide whether the selected framework, language,
   application type, protocol, Specmatic integration mode, and data-fetch mode
   are coherent using general framework ecosystem knowledge, the selected role
   guide, the selected integration mode, and the architecture implied by the
   data-fetch mode. Reject only combinations that are clearly incoherent, and
   explain the reason briefly.
2. Verification feasibility: if the combination is coherent but support is
   uncertain, proceed to generation and let dependency installation, build, and
   Specmatic tests prove whether the sample is actually supported.

Do not maintain a hardcoded list of valid or invalid stack combinations in this
skill. Example values in prompts are guidance only; the skill reasons about
whether a chosen set of values makes sense.

Validate Specmatic integration modes as follows:

- Normalize `extend-class` to `native` if the user provides the older name, but
  use `native` in generated documentation and final responses.
- `cli`: allowed for any coherent stack when the required local executable
  runtime is available, such as Java for JAR-based execution.
- `docker-cli`: allowed when Docker is available. This mode must not require
  local Java for Specmatic itself.
- `test-container`: allowed when Docker is available and the selected
  language/test ecosystem can run Docker Testcontainers or an equivalent
  in-test Docker container. This mode must not require local Java outside the
  container.
- `native`: allowed only when the selected language has an official native
  Specmatic test integration for the selected protocol.

Apply protocol-specific runtime constraints:

- REST/OpenAPI may use `cli`, `docker-cli`, `test-container`, or `native` when
  the selected language/framework can support the chosen adapter.
- gRPC/Protobuf, GraphQL SDL, SOAP/WSDL, and Kafka/AsyncAPI must use an
  Enterprise-capable Specmatic runtime unless current official documentation
  proves community support for that protocol and mode.
- `native` for Enterprise protocols is valid only when the matching Enterprise
  language artifact/API is verified during generation. Do not use a community
  native adapter that merely parses `specmatic.yaml` but reports no executable
  contract tests.

Before proceeding, resolve the application framework version and language
runtime target yourself, then ask the user to approve or override only those two
values. Do not ask about package dependencies, test frameworks, SDK packages,
parsers, plugins, images, or Specmatic runtime versions; choose those yourself
during generation and verification. Phrase the question as: "I will use
<framework> <version> with <language runtime target>. Is that okay, or would you
like to change either?"

### Step 3: Resolve Contract Source Of Truth

Before generating source code, determine the executable contract that Specmatic will use.

Source-of-truth order:

1. Passing Specmatic tests is the final definition of correctness.
2. The executable contract/spec referenced by `specmatic.yaml` is the behavioral source of truth.
3. Official Specmatic configuration and protocol documentation should be
   consulted when configuration syntax or contract semantics are unclear.
4. Local markdown files under `guides/` and `test-data/` are helper summaries. They must not override the executable contract.
5. Existing generated samples and official sample repositories must not be used
   as references for generation.

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
   across all discovered compatible contract versions.
3. Select the latest compatible discovered version for each required
   system-under-test or dependency contract role.
4. Resolve exactly one system-under-test contract and exactly one contract for
   each required dependency. Write these exact resolved paths into the
   generated `specmatic.yaml`.
5. If multiple candidates match the same role/version, fail with a clear error
   instead of guessing.
6. If any required system-under-test or dependency contract cannot be resolved
   from the configured central contract repo, stop before generating source code
   and ask the user for an explicit contract repo/path to use.

Before fetching from the network, check only approved contract-source locations:
the current generated sample's own `.specmatic/repos/<repo-name>` cache, an
isolated temporary checkout, or a user-provided contract path. Do not inspect
contract caches that live inside other generated sample folders.

Resolve these executable specs by sample type:

- Backend: provider/system-under-test contract for the selected protocol.
- BFF: system-under-test contract AND all dependency contracts required by
  the BFF architecture. Inspect the contract repository for ALL specs that
  the BFF's system-under-test contract implies — this may include REST backend
  dependencies AND async dependencies (e.g., Kafka/AsyncAPI). Discover
  dependencies across ALL protocols, not just the primary selected protocol.
- Frontend: dependency mock contract for the API, service, broker, or endpoint
  consumed by the generated client.

For BFF samples, examine the contract repository's spec structure to discover
every dependency. For example, the `specmatic-order-bff-java` reference has:
- SUT: `product_search_bff_v6.yaml` (REST/OpenAPI)
- Dependency 1: `api_order_v5.yaml` (REST backend mock)
- Dependency 2: `kafka.yaml` (AsyncAPI/Kafka mock)

Do not rely only on direct `$ref` entries, `servers`, operation descriptions,
file names, or textual references inside the BFF system-under-test OpenAPI file
to discover dependencies. BFF dependency discovery must combine the selected
SUT contract, the contract repository's role structure, and this skill's role
mapping across all protocols. As a reference example, the Store BFF contract
`io/specmatic/examples/store/openapi/product_search_bff_v6.yaml` maps to both a
REST backend mock (`io/specmatic/examples/store/openapi/api_order_v5.yaml`) and
an AsyncAPI/Kafka mock (`io/specmatic/examples/store/asyncapi/kafka.yaml`).
Use this as an example of cross-protocol discovery, not as a hardcoded mapping
for unrelated samples.

If async specs are present in the same contract domain and the BFF role mapping
or reference structure names one as a dependency, include it even when the SUT
OpenAPI file does not reference it directly. If multiple async dependency
candidates are plausible and the role mapping does not resolve them, stop with
an explicit ambiguity report instead of silently skipping async dependencies.

All discovered dependencies must be included in the generated `specmatic.yaml`
under `dependencies.services` with appropriate `runOptions` for each protocol.

After resolving the applicable specs, inspect the applicable generation guide,
`guides/protocol-generation.md`, the Specmatic runtime guidance, and the
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

Do not include the Specmatic integration mode in the sample folder name. If the
same application/protocol/language/framework/data-layer combination is
generated again with a different integration mode, update the same sample
folder.

- Create or update the sample at `<destination>/<sample-id>/`.
- Do not write generated sample files directly into the destination root.
- Do not create shared root-level contracts, metadata, workflows, or other shared generated assets.
- If the destination root already has `.github/workflows/samples-ci.yml`, update it to include the generated sample.
- Each generated sample folder must be self-contained and include every file needed to run, test, build, and understand that sample independently.

### Step 5: Generate the Project

For every sample, generate the complete file set listed in `guides/acceptance-criteria.md`.

Use `guides/specmatic-runtime.md` for Specmatic runtime structure and adapter
behavior for the selected integration mode, then fill generated files with
resolved contract paths, spec format, run option key, and role-specific
ports/base URLs/broker settings.
Generate routes/controllers, message handlers, RPC services, GraphQL resolvers,
SOAP handlers, client calls, schemas, seed data, examples, and adapter
transformations from the contract facts summary produced in Step 3. Use role
guides only for architecture and responsibilities.
Use `guides/readme-generation.md` for README structure and content — the README
must include the value proposition, not just setup instructions.

Default port conventions:

- Backend and BFF system-under-test services default to port `8080`.
- Specmatic dependency mocks/stubs default to port `8090`.
- Frontend dev servers default to port `3000` when a dev server is generated.
- AsyncAPI/Kafka samples default to the broker settings declared by the
  resolved contract, with host/port/topic overrides in the generated app config
  and checked-in `specmatic.yaml`.
- All ports, base URLs, broker URLs, and service endpoints must be configurable
  through environment variables consumed by the generated app config and
  checked-in `specmatic.yaml` so tests can avoid occupied resources.

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
2. Discover and verify the selected Specmatic integration interface for the chosen
   integration mode and language: CLI command, direct Docker command,
   Docker/Testcontainers image, test library API, or bundled JAR. Confirm it supports the generated
   `specmatic.yaml` configuration schema version before relying on it. If the
   runtime fails on config fields such as `version`, `systemUnderTest`, or
   `components`, classify it as a runtime compatibility failure and switch to a
   verified runtime/config-schema combination before changing generated app
   behavior.
3. Run progressive test verification (see below).
4. Run the generated build/package command when the sample includes compiled output or Docker.
5. Remove local verification artifacts from the generated sample folder when
   they are not source files, then re-run any build command affected by ignore
   files or build context.

#### Progressive Test Verification

Verify the generated sample in three levels, fixing failures at each level
before advancing. This prevents the AI from being overwhelmed by many failures
at once and isolates issues by category.

**Progressive verification applies only when Specmatic runs in test mode**
(`type: test` in `specmatic.yaml`). In test mode, Specmatic generates requests
and sends them to the app — more levels means more generated requests.

For samples where Specmatic acts only as a stub/mock (`type: mock`) — such as
frontend samples or consumer-side samples — skip the progressive levels. Run the
test command once and verify all tests pass. The `schemaResiliencyTests` setting
has no effect on stub mode because Specmatic is not generating requests; the
app's own test suite determines the test count.

**Level 1 — Examples only** (`schemaResiliencyTests: none`):
Set `schemaResiliencyTests: none` in `specmatic.yaml` and run the test command.
These are the fewest tests — derived from named examples in the contract.
Fix any failures (routing, schema shape, status codes). Record the passing test
count.

**Level 2 — Positive resiliency** (`schemaResiliencyTests: positiveOnly`):
Switch to `positiveOnly` and re-run. This adds all valid request combinations
(enum permutations, optional fields present/absent) without negative tests.
Fix any new failures. Verify the test count is **strictly greater than** Level 1.

**Level 3 — Full resiliency** (`schemaResiliencyTests: all`):
Switch to `all` and re-run. This adds negative/boundary tests (nulls to
non-nullable fields, wrong types, missing required fields). The app must return
`400` for invalid inputs. Fix any new failures — typically adding input
validation. Verify the test count is **strictly greater than** Level 2.

**Test count did not increase?** This means the `schemaResiliencyTests` setting
is not taking effect — Specmatic is silently ignoring it. Do NOT proceed or
treat this as "all tests pass". Stop and diagnose:

1. Verify the setting is under the correct yaml path: `specmatic.settings.test.schemaResiliencyTests`
   (top-level `specmatic:` key, not under `components:`).
2. Verify the value is a recognized string (`none`, `positiveOnly`, `all`).
3. Verify the Specmatic version supports this setting. If not, use the
   `SPECMATIC_GENERATIVE_TESTS=true` environment variable as a fallback.
4. If the env var works but the yaml setting doesn't, the yaml path is wrong.
5. After fixing, re-run and confirm the count increases before advancing.

If the count is identical across all three levels, the setting is being silently
ignored in every case — do not accept this as "all tests pass at every level."

#### Level 3 Known Patterns

Level 3 (`all`) adds negative/boundary tests that stress input validation. The
following patterns are common across all stacks:

**Framework error handler override:** Most frameworks include extra fields in
their default error responses (e.g., `path`, `trace`, `requestId`) that are not
in the contract's error response schema. Specmatic rejects these extra fields.
Override the framework's default error handler to return only the fields defined
in the contract's error response schema.

**Only validate what the contract defines:** Return 4xx only on endpoints where
the contract explicitly defines a 4xx response. If the contract only defines 2xx
responses for an endpoint, the app must not return 4xx — even if the input seems
invalid. The contract is the source of truth for what responses are allowed.

**Enum without 4xx response (unresolvable contract gap):** When the contract
defines an enum parameter but no 4xx response for that endpoint, Specmatic's
`all` mode sends invalid enum values expecting 4xx, but cannot verify any 4xx
response because none is defined. This creates a paradox: returning 200 fails
("expected 4xx"), returning 400 also fails ("specification does not contain a
4xx response"). This is a contract gap, not an app bug. Accept these failures,
document them in the final report, and do not loop trying to fix them.

**Before classifying any issue as an unresolvable contract gap**, verify you are
using the latest Specmatic version. Newer releases may have resolved the
behavior. If you are not on the latest version, upgrade and re-run before
accepting the gap.

**Transitive dependency conflicts:** After installing dependencies, if the first
test run fails with linkage, class-not-found, or missing-method errors pointing
at a third-party class, identify the conflicting transitive between the
Specmatic library and the framework. Override it using the build tool's standard
dependency override mechanism. This is a build fix, not a behavior fix.

If Level 3 passes fully (zero failures), ship the final `specmatic.yaml` with
`schemaResiliencyTests: all` to deliver maximum test coverage out of the box.

If Level 3 has unresolvable contract-gap failures only, ship with
`schemaResiliencyTests: none` and document the gaps in the manifest learnings.

At each level, apply the same fix approach: max 3 retries per level, read
Specmatic/JUnit/report output, make the smallest change to match the contract.

When modifying `specmatic.yaml` between levels, use your environment's native
file-editing tools (file write/replace operations). Do not use shell text
processors like `sed`, `perl`, or `awk` for file modifications.

**First run takes 1-3 minutes** — Specmatic git-clones the central contract repo (~50MB). Subsequent runs are fast (cached in `.specmatic/`).

**Timeout guidance:** If the test command runs for more than 5 minutes, something is wrong. Cancel and check:
- Is the required runtime available? For example, Java 17+ for CLI/JAR or JVM
  native modes, Docker for `docker-cli` or `test-container`, or Node/Python for
  package-native modes.
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
- duplicated or mutated Specmatic runtime config

Use the classification to make the smallest behavior change needed to match the
executable contract, then re-run the documented test command.

After verification completes, update `.specmatic-sample-manifest.json` with:
- `testCoverage`: record the test count, passed, and failed at each level.
  Set `shipped_level` to the `schemaResiliencyTests` value in the delivered
  `specmatic.yaml`.
- `learnings`: record any issues encountered during generation — dependency
  conflicts, contract gaps, framework quirks, workarounds applied. Use an
  empty array if generation was clean.

Only report "done" when tests are green.

## Key Rules

- **Executable contract wins.** Local guides and test data are helper context; the executable contract and Specmatic results decide behavior.
- **Role intent lives in `guides/`.** Guides define responsibilities and architecture; Step 3 contract facts define contract behavior.
- **Samples are self-contained.** Include every file needed to run, test, build, and understand the sample inside the sample folder.
- **The destination root is only a container.** Generate under `<provided-location>/<sample-id>/`, never directly into `<provided-location>/`.
- **Ports must be configurable through the single Specmatic config.** Keep
  documented defaults stable and follow `guides/specmatic-runtime.md` for
  endpoint template values and adapter behavior.
- **Startup failures must fail fast.** Test adapters must surface listen/bind errors, dependency startup failures, and Specmatic failures clearly.
- **Generated ownership must be complete.** Include lockfiles created by package managers when CI or local verification depends on them. Ignore dependency folders, build output, caches, and Specmatic reports.
- **Prompts must be example-driven.** User-facing stack questions include a few examples in parentheses, but compatibility is reasoned from framework knowledge, role guides, and verification results rather than hardcoded combination rows.
- **Report-driven fixes only.** On failures, read Specmatic/JUnit/report output and fix the reported contract mismatch rather than adding speculative validation or fallback logic.
- **Never use samples as references.** In generate mode, do not read, inspect, or copy from other
  sample projects, including existing generated sample folders in the
  destination repository or monorepo and official/public sample repositories.
  This includes their `.specmatic/` caches, build outputs, source files, tests,
  docs, configs, manifests, workflows, dependency versions, and Specmatic
  runtime versions. Any other existing sample must not be referred to for code,
  configuration, documentation, dependency choices, or runtime choices. Every
  file must be generated from this skill's `guides/`, `test-data/`,
  `config/contract-resolution.yaml`, the configured central contract repo,
  isolated temporary checkouts of that contract repo, official product/protocol
  documentation when syntax is unclear, or user-provided contract paths only.
  Existing samples may target a different stack or contract version and will
  silently corrupt the new sample if used as a reference. In maintain mode, reading the target sample is required.
- **No request validation middleware is needed.** Specmatic tests the contract (response schema), not your input validation.
- **Honor the selected Specmatic integration mode.** Generate test dependencies,
  test adapters, README instructions, and CI around `cli`, `docker-cli`,
  `test-container`, or `native`; do not silently switch modes unless the
  selected mode is impossible for the stack and the user chooses another one.
- **Keep the test adapter minimal.** Let the selected and verified Specmatic
  integration mode determine whether it uses a CLI, direct Docker command,
  Testcontainers-managed Docker container, native library API, or bundled JAR.
  Test adapters may start apps and mocks, set environment variables, stage
  protocol support files such as imported protos, and run Specmatic.
- **specmatic.yaml structure is protocol-aware.** Resolved contract paths,
  dependency specs, ports, base URLs, broker settings, and run option keys vary
  by protocol, role, and stack.

## References

- `guides/` — Role generation notes, Specmatic runtime guidance, and acceptance criteria
- `test-data/backend-seed-data.md` — Required backend data entries for tests to pass
- `config/contract-resolution.yaml` — Contract repositories, protocol roots, and runtime discovery patterns

---

## Maintain Workflow

### Step 1: Collect Target

Ask the user:

> "Point me to the local path of the sample to maintain."

Wait for the user's answer. The path should contain a single sample with a `.specmatic-sample-manifest.json` at its root.

If the user provides a monorepo path with multiple samples, list the discovered samples and ask which one to maintain. Process one sample per session to avoid context overload and ensure focused fixes.

### Step 2: Discover and Read

Scan the provided path for `.specmatic-sample-manifest.json`. Read the manifest to recover the original inputs (application type, protocol, language, framework, data layer, contract source).

If no manifest is found, inform the user and ask if they want to generate a new sample instead.

Read the sample's source files, config, and build files to understand the current state. In maintain mode, reading the existing sample IS required (unlike generate mode where it is forbidden).

### Step 3: Plan and Confirm

After reading the current state, determine what updates are available and
present a plan to the user before making changes. Check:

- **Specmatic version**: compare installed vs latest available
- **Framework/runtime version**: compare installed vs latest compatible
- **Contract changes**: check if the resolved contract has new/changed operations
- **Infrastructure**: check if CI actions, Dockerfile base images, or ignore
  patterns are outdated

Present the plan:

```
Maintenance plan for <sample-name>:

- Specmatic: <current> → <latest>
- <Framework>: <current> → <latest>
- <Runtime> base image: <current> → <latest>
- CI actions: <list outdated actions>
- Contract: <no changes / N new operations detected>
- README: <missing sections to add>

Proceed with all? Or tell me which to skip.
```

Wait for the user's answer. The user may say:
- "proceed" / "all" — apply everything
- "just specmatic" — only bump Specmatic version
- "skip framework upgrade" — do everything except framework version bump
- "only infra" — only update CI, Dockerfile, gitignore
- Any combination

Record the user's choices and apply only the approved updates in Step 4.

### Step 4: Update Layers

Apply only the updates the user approved in Step 3.

#### 4a: Update Contract Config (always)

Re-resolve the contract source using `config/contract-resolution.yaml` and the manifest inputs. Regenerate `specmatic.yaml` (or `specmatic.json` for stacks that require it) with the latest resolved contract paths.

#### 4b: Update Dependencies (when approved)

Bump only the approved dependency versions. For example:
- If user said "just specmatic" — only bump the Specmatic package version
- If user said "all" — bump Specmatic, framework, and all dev/test dependencies
  to latest compatible releases
- If user said "skip framework" — bump Specmatic and other deps but keep the
  framework at its current version

Identify the build file for the sample's language (e.g., `package.json` for npm,
`pom.xml` for Maven) and update the approved versions.

Install dependencies after updating.

#### 4c: Update Infrastructure (when approved)

Regenerate from current best practices (only the approved items):
- `Dockerfile` — latest base image, optimized layers
- `.github/workflows/ci.yml` — latest action versions, correct setup steps
- `.gitignore` / `.dockerignore` — complete ignore patterns
- `README.md` — update per `guides/readme-generation.md`: add missing required
  sections (e.g., "Why Specmatic", "How It Works"), refresh versions/commands/links
  to match current state, preserve any user-added sections

#### 4d: Run Tests

Run the sample's test command. If tests pass, this sample is done.

### Step 5: Fix Failures (only if tests fail)

If tests fail after the layer updates, read the Specmatic test output and fix the code — same approach as generate mode Step 6 (Verify And Converge). Make the smallest change needed to match the executable contract, re-run tests, repeat up to 3 times.

If the same failure persists after 3 fix attempts:
1. Regenerate the broken file from scratch using the contract facts (same approach as generate mode Step 5)
2. Run tests again
3. If still failing, report the sample as unfixable with a clear reason

Do NOT leave a sample in a worse state than you found it. If escalation fails, revert app code changes and report.

### Step 6: Report

After processing the sample, report:

```
Maintain Summary:
- sample-name: ✅ updated (deps bumped, config refreshed, framework upgraded)
```

or

```
Maintain Summary:
- sample-name: ✅ fixed (brief description of what was fixed)
```

or

```
Maintain Summary:
- sample-name: ❌ unfixable (reason — manual intervention needed)
```

### Maintain Mode Key Rules

- **Read existing code.** Unlike generate mode, maintain mode MUST read and understand the current sample before making changes.
- **Plan before acting.** Always present the maintenance plan and wait for user confirmation before applying updates.
- **Respect user choices.** If the user says "skip framework upgrade", do not upgrade the framework — even if it would be best practice.
- **Preserve manual customizations.** Only change what's needed. Don't rewrite working app code just because the skill would generate it differently today.
- **Contract config is always refreshed.** Regardless of what else the user skips, `specmatic.yaml` is always re-resolved to ensure contract alignment.
- **App code is only touched when tests fail.** If tests pass after approved updates, don't touch app code.
- **One sample per session.** Maintain one sample at a time to ensure focused, high-quality fixes.
- **Report-driven fixes only.** Same as generate mode — read the actual test failure, don't guess.
- **Never make the sample worse.** If a fix attempt breaks more tests than it fixes, revert and try a different approach.
- **Manifest is required.** Skip directories without `.specmatic-sample-manifest.json` — they can't be maintained without knowing the original inputs.
