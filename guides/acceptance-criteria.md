# Acceptance Criteria

A generated sample is "done" when ALL of the following are true.

All checks apply inside the generated sample folder at `<provided-location>/<sample-id>/`.

## Must Pass

- [ ] `npm test` (or equivalent) exits with code 0
- [ ] All Specmatic contract tests pass (0 failures)
- [ ] Generated behavior matches the executable OpenAPI/contract used by Specmatic
- [ ] No manual intervention needed after generation
- [ ] Any mismatch between local guides/test data and the executable contract is resolved in favor of the executable contract
- [ ] No generated files are written at the provided location root except the sample folder itself, unless updating an existing root `.github/workflows/samples-ci.yml`
- [ ] The sample folder is self-contained and does not require shared generated assets outside the folder
- [ ] If the destination root has `.github/workflows/samples-ci.yml`, it includes a job for this sample
- [ ] Ports and dependency base URLs used by tests can be overridden from environment variables
- [ ] Consumer samples document and implement the contract-derived mapping between SUT/consumer operations and dependency mock operations
- [ ] The generated test adapter uses the verified Specmatic package interface for the selected runtime
- [ ] Local verification artifacts are ignored and are not left as source files in the generated sample folder

## Required Files

| File | Purpose |
|------|---------|
| `specmatic.yaml` | Points to central contract repo, defines test config |
| Build file (`package.json` / `pom.xml` / `build.gradle` / `requirements.txt`) | Dependencies including Specmatic |
| Lockfile when produced by the package manager | Enables reproducible installs and CI locked installs |
| Source code (controllers/routes) | Implements all endpoints from the contract |
| Data layer (db/store) | In-memory store with seed data when the role needs local state |
| Contract test file | Adapter that starts app + runs Specmatic |
| `Dockerfile` | Production container image |
| `.dockerignore` when `Dockerfile` is generated | Keeps dependencies, virtualenvs, reports, caches, Specmatic repos, and local files out of image build context |
| `.github/workflows/ci.yml` | CI pipeline: test + Docker build |
| `README.md` | Prerequisites, how to run, how it works |
| `.gitignore` | Ignore dependency folders, virtualenvs, build output, reports, caches, and `.specmatic` |
| `.specmatic-sample-manifest.json` | Records generated files owned by this sample |

## CI Workflow Must Include

1. Setup JRE 17 (Specmatic requires Java)
2. Setup language runtime (Node 20 / JDK 17 / Python 3.x)
3. Install dependencies
4. Run tests
5. Upload test report artifact
6. (On main branch) Build and push Docker image

## Root Samples CI

When the destination root already has `.github/workflows/samples-ci.yml`, add or update one job for the generated sample:

- Use the sample id as the job name.
- Run commands with `working-directory: <sample-id>`.
- Use the install and test commands generated for the selected language and framework.
- Set up JRE 17 before running Specmatic tests.
- Set up the language runtime required by the selected language.
- Upload the generated Specmatic/JUnit report artifact when the test command
  produces one.

## Local Run Must Work With

```bash
# Node.js
npm install && npm test

# Java
./mvnw test

# Python
pip install -r requirements.txt && pytest test -v -s
```

Single command, green output.

If the default service port is occupied, the test command may use documented environment overrides such as `SUT_PORT`, `SUT_BASE_URL`, or stack-equivalent names. Normal service startup should still use the documented default port unless the user overrides it.

## What "Green" Means

Specmatic output shows:
```text
Tests run: N, Successes: N, Failures: 0, Errors: 0
```

Or in Jest format:
```text
Test Suites: 1 passed, 1 total
Tests:       N passed, N total
```

Any failures = not done. Fix and re-run.

When tests fail, use the generated Specmatic/JUnit/report files to identify the exact contract mismatch. Fix status codes, content types, request/response shape, examples, stubs, or startup configuration until the report has zero failures.
