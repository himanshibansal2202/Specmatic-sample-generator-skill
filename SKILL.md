---
name: generate-specmatic-sample
description: Generate or maintain working Specmatic v3 sample projects for a given tech stack and protocol. Use when the user wants to create a Backend, BFF, or Frontend sample that demonstrates Specmatic contract testing, or when they want to update existing samples to align with contract changes, dependency upgrades, or Specmatic version updates. Triggers on requests like "generate a specmatic sample", "create a sample project for Java Spring Boot", "scaffold a backend REST, gRPC, GraphQL, AsyncAPI, or SOAP service with contract tests", "maintain my specmatic samples", or "update my samples repo".
---

# Specmatic Sample Skill

You are a code generator and maintainer that creates and keeps working Specmatic sample projects. The generated or maintained project MUST pass Specmatic contract tests — this is the only definition of "done".

## Mode Selection

When the skill is invoked, ask the user:

> "What would you like to do? (generate, maintain)"

- **generate** — Create a new sample project from scratch for a given tech stack.
- **maintain** — Update existing sample(s) to align with contract changes, dependency upgrades, or Specmatic version updates.

Wait for the user's answer before proceeding to the corresponding workflow.

---

## Generate Workflow

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
7. If any required system-under-test or dependency contract cannot be resolved
   from the configured central contract repo, stop before generating source code
   and ask the user for an explicit contract repo/path to use.

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

- If the destination is a local path, create or update the sample at `<destination>/<sample-id>/`.
- If the destination is a repository link, derive `<repo-name>` from the Git URL
  basename without `.git`, then clone or locate the repository at
  `<skill-repo-parent>/<repo-name>/`, where `<skill-repo-parent>` is the parent
  directory of this skill repo. Create or update the sample at
  `<skill-repo-parent>/<repo-name>/<sample-id>/`.
- Do not clone destination repositories inside this skill repo or inside a
  generated sample folder.
- Do not write generated sample files directly into the destination root.
- Do not create shared root-level contracts, metadata, workflows, or other shared generated assets.
- If the destination root already has `.github/workflows/samples-ci.yml`, update it to include the generated sample.
- Each generated sample folder must be self-contained and include every file needed to run, test, build, and understand that sample independently.

### Step 5: Generate the Project

For every sample, generate the complete file set listed in `guides/acceptance-criteria.md`.

Use `guides/specmatic-runtime.md` for Specmatic runtime structure and adapter
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

Only report "done" when tests are green and any required repository-link
publish step has completed or been skipped for a documented reason.

### Step 7: Publish Repository-Link Destinations

Run this step only when the destination provided by the user was a repository
link. Local path destinations are not committed or pushed automatically.

After Step 6 passes:

1. From the destination repository checkout, inspect the git worktree.
2. If there are unrelated dirty files outside the generated `<sample-id>/`
   folder and optional root `.github/workflows/samples-ci.yml`, stop and report
   the blocker. Do not overwrite, stage, commit, or push unrelated changes.
3. If there are no generated changes to commit, skip commit and push.
4. Stage only the generated `<sample-id>/` folder and the root
   `.github/workflows/samples-ci.yml` if it was updated.
5. Commit with message `Add <sample-id> Specmatic sample`.
6. Ask the user for explicit permission before running `git push`.
7. If approved, push the current branch to its configured upstream. If no
   upstream is configured, push to `origin <current-branch>`.
8. If the push fails because authentication, authorization, or branch protection
   blocks it, report the failure and leave the committed local branch intact.

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
- **Never read existing generated samples (generate mode only).** In generate mode, do not read, inspect, or copy from other sample folders already present in the destination repository or monorepo, including their `.specmatic/` caches, build outputs, source files, tests, docs, configs, and manifests. Every file must be generated from the skill's `contracts/`, `guides/`, `test-data/`, configured central contract repo, isolated temporary checkouts of that repo, or user-provided contract paths only. Existing samples may target a different stack or contract version and will silently corrupt the new sample if used as a reference. In maintain mode, reading the target sample is required.
- **No request validation middleware is needed.** Specmatic tests the contract (response schema), not your input validation.
- **Keep the test adapter minimal.** Let the verified Specmatic package interface determine whether it uses a CLI, library API, or bundled JAR.
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

### Step 3: Update Layers

#### 3a: Update Contract Config (always)

Re-resolve the contract source using `config/contract-resolution.yaml` and the manifest inputs. Regenerate `specmatic.yaml` (or `specmatic.json` for stacks that require it) with the latest resolved contract paths.

#### 3b: Update Dependencies (always)

Bump Specmatic and framework/library dependency versions to the latest compatible release. Identify the build file for the sample's language (e.g., `package.json` for npm, `pom.xml` for Maven) and update all relevant versions — including the framework itself, Specmatic packages, and dev/test dependencies.

Install dependencies after updating.

#### 3c: Update Infrastructure (always)

Regenerate from current best practices:
- `Dockerfile` — latest base image, optimized layers
- `.github/workflows/ci.yml` — latest action versions, correct setup steps
- `.gitignore` / `.dockerignore` — complete ignore patterns

#### 3d: Run Tests

Run the sample's test command. If tests pass, this sample is done.

### Step 4: Fix Failures (only if tests fail)

If tests fail after the layer updates, read the Specmatic test output and fix the code — same approach as generate mode Step 6 (Verify And Converge). Make the smallest change needed to match the executable contract, re-run tests, repeat up to 3 times.

If the same failure persists after 3 fix attempts:
1. Regenerate the broken file from scratch using the contract facts (same approach as generate mode Step 5)
2. Run tests again
3. If still failing, report the sample as unfixable with a clear reason

Do NOT leave a sample in a worse state than you found it. If escalation fails, revert app code changes and report.

### Step 5: Report

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
- **Preserve manual customizations.** Only change what's needed. Don't rewrite working app code just because the skill would generate it differently today.
- **Config, deps, and infra are always safe to update.** These layers don't contain user customizations. This includes framework version upgrades.
- **App code is only touched when tests fail.** If tests pass after config/dep/infra updates, don't touch app code.
- **One sample per session.** Maintain one sample at a time to ensure focused, high-quality fixes.
- **Report-driven fixes only.** Same as generate mode — read the actual test failure, don't guess.
- **Never make the sample worse.** If a fix attempt breaks more tests than it fixes, revert and try a different approach.
- **Manifest is required.** Skip directories without `.specmatic-sample-manifest.json` — they can't be maintained without knowing the original inputs.
