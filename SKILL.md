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
3. Then ask: "What language? (JavaScript / TypeScript / Java / Python)"
   - Wait for answer.
4. Then ask: "What framework?" — offer only valid options for the chosen language:
   - JavaScript/TypeScript → Express
   - Java → Spring Boot
   - Python → Flask
   - Wait for answer.
5. Then ask: "What data layer? (in-memory)"
   - Wait for answer.

Only proceed to Step 2 after ALL answers are collected.

### Step 2: Validate Combination

Read `assets/config/stack-matrix.yaml`. If the combination is not in `supported_combinations`, reject it and suggest the nearest supported alternative.

### Step 3: Generate the Project

For a **Backend** sample, generate these files:

1. **specmatic.yaml** — See `references/specmatic-config.md` for the exact template
2. **Build file** (package.json / pom.xml / etc.) — Include Specmatic as a test dependency
3. **Source code** — Implement all endpoints from the contract. See `references/contracts.md`
4. **Seed data** — Pre-populate the data store with required entries. See `references/seed-data.md`
5. **Contract test adapter** — See `references/specmatic-config.md` for the pattern per language
6. **Dockerfile** — Multi-stage production build
7. **CI workflow** — GitHub Actions: setup JRE 17 + language runtime, run tests, build Docker
8. **README.md** — Prerequisites, how to run locally, how it works

For a **BFF** sample, see `references/bff-role.md`. Key differences:
- The BFF has NO local database — it calls the Backend API
- specmatic.yaml has a `dependencies` section that starts a mock of the Backend
- The BFF app reads the Backend URL from an environment variable (e.g., `STUB_URL`)
- No seed data needed — the Specmatic mock handles Backend responses automatically

For a **Frontend** sample, see the Frontend Contracts section in `references/contracts.md`. Key differences:
- The Frontend provides no API contract
- The Frontend consumes the Product and Order BFF API
- The BFF URL must be configurable
- Contract consumption is verified against a Specmatic mock of the BFF API

### Step 4: Verify

After generating all files:
1. Install dependencies
2. Run the test command (e.g., `npm test`)
3. **First run takes 1-3 minutes** — Specmatic git-clones the central contract repo (~50MB). Subsequent runs are fast (cached in `.specmatic/`).
4. If tests fail, read the error output, fix the code, and re-run
5. Repeat until ALL tests pass (max 3 retries)

**Timeout guidance:** If the test command runs for more than 5 minutes, something is wrong. Cancel and check:
- Is Java 17+ available? (`java -version`)
- Is there network access? (Specmatic needs to clone from GitHub)
- Is the port already in use?

Only report "done" when tests are green.

## Key Rules

- **Seed data is critical.** The OpenAPI spec's examples reference specific IDs. Your data store MUST contain those entries at startup. See `references/seed-data.md`.
- **Content-Type matters.** Some endpoints return `text/plain`, others `application/json`. The spec defines which.
- **Contract behavior lives in `references/contracts.md`.** Implement those endpoints, statuses, headers, and schemas exactly.
- **No request validation middleware is needed.** Specmatic tests the contract (response schema), not your input validation.
- **The contract test adapter is ~5 lines.** Don't overcomplicate it. Pattern: start app → run specmatic → stop app.
- **specmatic.yaml is the same for every language.** Only the port/baseUrl changes.

## References

- `references/contracts.md` — Backend, BFF, and Frontend contract responsibilities
- `references/specmatic-config.md` — specmatic.yaml template and contract test patterns per language
- `references/seed-data.md` — Required data entries for tests to pass
- `references/acceptance-criteria.md` — What files must exist, what "done" looks like
- `assets/config/stack-matrix.yaml` — Supported combinations
