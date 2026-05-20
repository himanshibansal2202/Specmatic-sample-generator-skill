# Acceptance Criteria

A generated sample is "done" when ALL of the following are true.

All checks apply inside the generated sample folder at `<provided-location>/<sample-id>/`.

## Must Pass

- [ ] `npm test` (or equivalent) exits with code 0
- [ ] All Specmatic contract tests pass (0 failures)
- [ ] Generated behavior matches the executable contract used by Specmatic
- [ ] No manual intervention needed after generation
- [ ] Any mismatch between local guides/test data and the executable contract is resolved in favor of the executable contract
- [ ] No generated files are written at the provided location root except the sample folder itself, unless updating an existing root `.github/workflows/verify-all.yml`
- [ ] The sample folder is self-contained and does not require shared generated assets outside the folder
- [ ] If the destination root has `.github/workflows/verify-all.yml`, it includes the sample in the appropriate language matrix
- [ ] Ports, dependency base URLs, service endpoints, broker URLs, and
  protocol-specific test settings can be overridden from environment variables
- [ ] Consumer samples document and implement the contract-derived mapping between SUT/consumer operations and dependency mock operations
- [ ] The generated test adapter uses the verified Specmatic package interface for the selected runtime
- [ ] Local verification artifacts are ignored and are not left as source files in the generated sample folder

## Required Files

| File | Purpose |
|------|---------|
| `specmatic.yaml` | Points to central contract repo, defines test config |
| Build file (`package.json` / `pom.xml` / `build.gradle` / `requirements.txt`) | Dependencies including Specmatic |
| Lockfile when produced by the package manager | Enables reproducible installs and CI locked installs |
| Source code (controllers/routes/services/resolvers/handlers) | Implements all operations from the contract |
| Data layer (db/store) | In-memory store with seed data when the role needs local state |
| Contract test file | Adapter that starts app + runs Specmatic |
| `Dockerfile` | Production container image |
| `.dockerignore` when `Dockerfile` is generated | Keeps dependencies, virtualenvs, reports, caches, Specmatic repos, and local files out of image build context |
| `.github/workflows/ci.yml` | CI pipeline: test + Docker build |
| `README.md` | Prerequisites, how to run, how it works |
| `.gitignore` | Ignore dependency folders, virtualenvs, build output, reports, caches, and `.specmatic` |
| `.specmatic-sample-manifest.json` | Records generated files and generation inputs |

## Manifest Schema

The manifest enables maintain mode to identify the sample's stack and test
command without re-asking the user.

```json
{
  "schema_version": 1,
  "generated_at": "<ISO-8601 timestamp>",
  "inputs": {
    "application_type": "<backend|bff|frontend>",
    "protocol": "<rest|kafka|grpc|graphql|soap>",
    "contract_version": "<v3|...>",
    "language": "<javascript|typescript|java|python|...>",
    "framework": "<express|spring-boot|flask|...>",
    "data_fetch": "<in-memory|rest-api|...>"
  },
  "contract_source": {
    "url": "<contract-repo-git-url>",
    "spec_path": "<resolved-spec-path>"
  },
  "files": ["<list of all skill-generated file paths relative to sample root>"]
}
```

## CI Workflow Must Include

1. Multi-OS matrix strategy: `[ubuntu-latest, macos-latest, windows-latest]`
2. Setup JRE 17 (Specmatic requires Java)
3. Setup language runtime (Node 20 / JDK 17 / Python 3.x)
4. Install dependencies
5. Run tests
6. Upload test report artifact
7. (On main branch) Build and push Docker image

For non-REST protocols that require Specmatic Enterprise, CI must also install
or run the documented Enterprise Docker image/artifact and expose any required
license or setup variables through the generated README.

## Root Samples CI (verify-all.yml)

When the destination is a monorepo with multiple samples, the skill must
generate or update a root-level `.github/workflows/verify-all.yml` that
verifies all samples. Per-sample `ci.yml` files do not trigger in a monorepo —
only the root workflow runs.

Structure:

- Group samples by language runtime into separate jobs (e.g.,
  `test-node-samples`, `test-java-samples`, `test-python-samples`).
- Each job uses a `strategy.matrix.sample` listing the sample folder names for
  that language.
- Use `working-directory: samples/${{ matrix.sample }}` (or the configured
  samples subdirectory).
- Each job sets up JRE 17 + the language runtime, installs deps, runs tests,
  verifies Docker build, and uploads the Specmatic/JUnit report artifact.
- Add a final `regression-check` job that depends on all test jobs and confirms
  all passed.

**Multi-OS matrix is required.** Some protocols (Kafka, gRPC) and platform-
specific path/process behaviors fail on certain OS targets. Running on all three
OS catches issues that local-only verification misses.

When a new sample is generated or an existing sample is regenerated, update the
matrix list in the appropriate language job. If no job exists for the sample's
language, create one following the same pattern.

Example structure:

```yaml
name: Verify All Samples
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test-node-samples:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
        sample:
          - backend-rest-javascript-express-in-memory
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          java-package: jre
      - uses: actions/setup-node@v4
        with:
          node-version: 20
      - name: Install and test
        working-directory: samples/${{ matrix.sample }}
        run: |
          npm install
          npm test
      - name: Verify Docker build
        working-directory: samples/${{ matrix.sample }}
        run: docker build -t ${{ matrix.sample }}:test .
      - name: Upload test report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: report-${{ matrix.sample }}
          path: samples/${{ matrix.sample }}/build/reports/specmatic/html

  test-java-samples:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
        sample:
          - backend-rest-java-spring-boot-in-memory
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
      - name: Run tests
        working-directory: samples/${{ matrix.sample }}
        run: ./mvnw test
      - name: Verify Docker build
        working-directory: samples/${{ matrix.sample }}
        run: docker build -t ${{ matrix.sample }}:test .
      - name: Upload test report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: report-${{ matrix.sample }}
          path: samples/${{ matrix.sample }}/build/reports/specmatic/html

  regression-check:
    needs: [test-node-samples, test-java-samples]
    runs-on: ubuntu-latest
    steps:
      - name: All samples passed
        run: echo "✅ All samples verified green"
```

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

If the default service port or broker port is occupied, the test command may use
documented environment overrides such as `SUT_PORT`, `SUT_BASE_URL`,
`BROKER_PORT`, `BROKER_URL`, or stack-equivalent names. Normal service startup
should still use the documented default endpoint unless the user overrides it.

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

When tests fail, use the generated Specmatic/JUnit/report files to identify the
exact contract mismatch. Fix status codes, content types, request/response
shape, RPC/message shape, GraphQL selection/variables, SOAP XML, examples,
stubs, or startup configuration until the report has zero failures.

## Regression Mode Acceptance Criteria

- [ ] All combinations in `config/regression-matrix.yaml` are attempted
- [ ] Each combination runs the full generation workflow (Steps 2–6)
- [ ] Each generated sample passes local verification before push
- [ ] Failed combinations are reported with failure reason and do not block others
- [ ] Successfully generated samples are pushed to the destination repo
- [ ] A summary report lists pass/fail status for every combination

## Maintain Mode Acceptance Criteria

- [ ] All samples with a `.specmatic-sample-manifest.json` are discovered
- [ ] Samples without a manifest are skipped and reported as unmanaged
- [ ] Each sample's test command is run using the language/framework from the manifest
- [ ] Failing samples are fixed using report-driven convergence (same as Step 6)
- [ ] Fixes are committed with descriptive messages
- [ ] Unfixable samples are reported with the failure reason
- [ ] A summary report lists: already green, fixed, and unfixable samples
