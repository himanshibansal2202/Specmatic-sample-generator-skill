---
name: generate-specmatic-sample
description: Generate a working Specmatic v3 sample project for a given tech stack. Use when the user wants to create a Backend, BFF, or Frontend sample that demonstrates Specmatic contract testing. Triggers on requests like "generate a specmatic sample", "create a sample project for Java Spring Boot", or "scaffold a backend REST API with contract tests".
---

# Generate Specmatic Sample

You are a code generator that creates working Specmatic sample projects. The generated project MUST pass Specmatic contract tests — this is the only definition of "done".

## Workflow

### Step 1: Collect Inputs (ONE AT A TIME)

Ask each question separately. Wait for the user's answer before asking the next. Do NOT ask all questions at once.

1. First ask: "What application type? (Backend / BFF / Frontend)"
   - Wait for answer.
2. Then ask: "What protocol? (REST)"
   - Wait for answer.
3. Then ask: "What contract version? (default v3 / latest / specific like v5)"
   - Wait for answer.
4. Then ask: "What language? (JavaScript / TypeScript / Java / Python)"
   - Wait for answer.
5. Then ask: "What framework?" — offer only valid options for the chosen language:
   - JavaScript/TypeScript → Express
   - Java → Spring Boot
   - Python → Flask
   - Wait for answer.
6. Then ask: "What data layer? (in-memory)"
   - Wait for answer.
7. Then ask: "Where should I create the sample folder? Provide a local path or repository link."
   - Wait for answer.

Only proceed to Step 2 after ALL answers are collected.

### Step 2: Validate Combination

Read `config/stack-matrix.yaml`. If the combination is not in `supported_combinations`, reject it and suggest the nearest supported alternative.

### Step 3: Resolve Contract Source Of Truth

Before generating source code, determine the executable contract that Specmatic will use.

Source-of-truth order:

1. Passing Specmatic tests is the final definition of correctness.
2. The OpenAPI/contract file referenced by `specmatic.yaml` is the behavioral source of truth.
3. Official Specmatic v3 and OpenAPI documentation should be consulted when configuration syntax or contract semantics are unclear.
4. Local markdown files under `guides/` and `test-data/` are helper summaries. They must not override the executable contract.

Read the matched combination's `contract_source` and `config/contract-resolution.yaml` to resolve the contract source for the selected sample. The contract repository URL is required because Specmatic fetches the executable contracts from it. OpenAPI spec paths are resolved in this order:

1. Use the explicit spec path configured for the requested/default contract version.
2. If no explicit path is configured, inspect filenames under the configured OpenAPI root in the contract repository and match the role-specific discovery patterns.
3. If a requested version is available, use that version.
4. If no version was requested, use `default_version`; if those files are not available, select the latest compatible discovered version.
5. If multiple candidates match the same role/version, fail with a clear error instead of guessing.
6. If no compatible spec is found, fail before generating source code.

Resolve these executable specs by sample type:

- Backend: Backend system-under-test OpenAPI spec.
- BFF: BFF system-under-test OpenAPI spec and Backend dependency mock OpenAPI spec.
- Frontend: BFF dependency mock OpenAPI spec.

For every sample type, inspect the applicable generation guide, the Specmatic configuration guidance, and the resolved executable contract before coding. When the referenced executable OpenAPI contract is available locally or can be fetched during verification, use it to confirm:

- Methods and paths
- Status codes
- Request and response content types
- Required, optional, and forbidden response fields
- Example IDs, request examples, and response examples used by Specmatic
- Stub/provider dependency behavior for consumer-side samples

If the executable contract contradicts local guide or test-data notes, implement the executable contract and record the discrepancy in the final response.

### Step 4: Resolve the Output Folder

Use the `id` from the matched `supported_combinations` entry as the sample folder name.

- If the destination is a local path, create or update the sample at `<destination>/<sample-id>/`.
- If the destination is a repository link, clone or locate a local checkout of that repository first, then create or update the sample at `<checkout>/<sample-id>/`.
- Do not write generated sample files directly into the destination root.
- Do not create shared root-level contracts, metadata, workflows, or other shared generated assets.
- Each generated sample folder must be self-contained and include every file needed to run, test, build, and understand that sample independently.

### Step 5: Generate the Project

For every sample, generate the complete file set listed in `guides/acceptance-criteria.md`.

Use `guides/specmatic-config.md` for Specmatic config structure and adapter behavior, then fill generated files with resolved contract paths and stack-specific ports/base URLs.

For a **Backend** sample, use `guides/backend-generation.md` for role behavior. Key differences:
- The Backend owns local Products and Orders state
- The Backend keeps an Inventory dependency boundary
- Seed data is required. See `test-data/backend-seed-data.md`

For a **BFF** sample, use `guides/bff-generation.md` for role behavior, then verify exact API behavior against the executable contract referenced by `specmatic.yaml`. Key differences:
- The BFF has NO local database — it calls the Backend API
- specmatic.yaml has a `dependencies` section that starts a mock of the Backend
- The BFF app reads the Backend URL from an environment variable (e.g., `STUB_URL`)
- No seed data needed — the Specmatic mock handles Backend responses automatically

For a **Frontend** sample, use `guides/frontend-generation.md` for client workflow behavior, then verify exact API behavior against the executable contract referenced by `specmatic.yaml`. Key differences:
- The Frontend provides no API contract
- The Frontend consumes the Product and Order BFF API
- The BFF URL must be configurable
- Contract consumption is verified against a Specmatic mock of the BFF API

### Step 6: Verify And Converge

After generating all files, run verification from inside the generated sample folder:
1. Install dependencies
2. Run the test command (e.g., `npm test`)
3. **First run takes 1-3 minutes** — Specmatic git-clones the central contract repo (~50MB). Subsequent runs are fast (cached in `.specmatic/`).
4. If tests fail, read the error output and the generated Specmatic/JUnit/report files, fix the code to match the executable contract, and re-run
5. Repeat until ALL tests pass (max 3 retries)
6. Run the generated build/package command when the sample includes compiled output or Docker

**Timeout guidance:** If the test command runs for more than 5 minutes, something is wrong. Cancel and check:
- Is Java 17+ available? (`java -version`)
- Is there network access? (Specmatic needs to clone from GitHub)
- Is the port already in use?
- Did the app fail to bind its configured port?
- Did the test adapter swallow process startup errors?

Only report "done" when tests are green.

## Key Rules

- **Executable contract wins.** Generate from the actual OpenAPI/contract used by Specmatic whenever it is available. Local guides, contract-resolution config, and test data are useful inputs, but the executable contract and Specmatic test results decide final behavior.
- **Seed data is critical.** The OpenAPI spec's examples reference specific IDs. Backend data stores MUST contain those entries at startup. See `test-data/backend-seed-data.md`.
- **Content-Type matters.** Some endpoints return `text/plain`, others `application/json`. The spec defines which.
- **Role intent lives in `guides/`.** Use the applicable generation guide to understand the sample's responsibilities, then verify exact behavior against the executable contract used by Specmatic.
- **Samples are self-contained.** Copy or create the required tests, CI, README, config, and executable contract references inside the generated sample folder. Do not depend on shared files outside the sample folder.
- **The destination root is only a container.** Generate under `<provided-location>/<sample-id>/`, never directly into `<provided-location>/`.
- **Ports must be configurable.** Keep documented default ports stable, but let tests override ports/base URLs so samples can run when defaults are occupied.
- **Startup failures must fail fast.** Test adapters must surface listen/bind errors, dependency startup failures, and Specmatic failures clearly.
- **Generated ownership must be complete.** Include lockfiles created by package managers when CI or local verification depends on them. Ignore dependency folders, build output, caches, and Specmatic reports.
- **No request validation middleware is needed.** Specmatic tests the contract (response schema), not your input validation.
- **The contract test adapter is ~5 lines.** Don't overcomplicate it. Pattern: start app → run specmatic → stop app.
- **specmatic.yaml structure is the same for every language.** Resolved contract paths, dependency specs, ports, and base URLs vary by role and stack.

## References

- `guides/` — Role generation notes, Specmatic config, and acceptance criteria
- `test-data/backend-seed-data.md` — Required backend data entries for tests to pass
- `config/stack-matrix.yaml` — Supported combinations
- `config/contract-resolution.yaml` — Contract repository, known spec paths, and runtime discovery patterns
