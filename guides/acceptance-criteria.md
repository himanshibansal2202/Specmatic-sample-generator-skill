# Acceptance Criteria

A generated sample is "done" only when every **blocker** check below holds.

This file is both the human-readable spec and the basis for an **executable
validator**. Each check has a stable ID and is marked as `auto` (machine-
verifiable from artifacts) or `manual` (requires judgement). Auto checks name
the **Artifact** to inspect, the **Assert** that must hold, and the **Oracle**
(the source of truth to compare against). The intent is that the auto checks can
be run by a script that exits non-zero on any blocker failure — a checklist the
producer self-grades is not a check.

All checks apply inside the generated sample folder at
`<provided-location>/<sample-id>/`.

## How validation must run (preconditions)

These three properties are what make the checks below trustworthy. Without them
the criteria are advisory prose, not a gate.

- **VAL-1 (independent re-run) · blocker.** Validation runs on a clean checkout
  of the generated folder and **re-executes** the test command itself. It does
  not trust the generator's natural-language summary ("tests passed", "coverage
  68%") as evidence — only artifacts produced by an observed run.
- **VAL-2 (preserve evidence) · blocker.** The run artifacts the checks depend
  on — the Specmatic test/coverage report and the test output — must exist when
  validation runs. The sample must not be stripped of these before validation;
  cleaning local caches is allowed only after the checks have consumed them.
- **VAL-3 (derive oracles, never declare) · blocker.** Every value with a single
  correct answer is **computed at validation time** from a source of truth — the
  resolved contract (operation count, protocol), the resolved build toolchain
  (compiler/runtime versions), the artifact registry (latest Enterprise
  version). Such values are never read from an example in the guides, and the
  guides must not present a liftable literal for them.

---

## Test execution

- **TEST-1 (single command green) · blocker · auto.**
  Artifact: test command exit code + stdout. Assert: the single documented test
  command exits `0` and prints zero failures/errors (`Failures: 0, Errors: 0`,
  or the Jest-format equivalent). Oracle: process exit status and report.
- **TEST-2 (no contract failures) · blocker · auto.**
  Artifact: Specmatic/JUnit report. Assert: total failures == 0 and errors == 0.
  Oracle: parsed report.
- **TEST-3 (behaviour matches the executable contract) · blocker · manual.**
  The implementation conforms to the executable contract used by Specmatic; any
  mismatch between local guides/test data and the executable contract is
  resolved in favour of the contract. (Largely enforced by TEST-1/2; manual
  spot-check for areas tests cannot reach.)
- **TEST-4 (no manual intervention) · blocker · manual.**
  Generation produced a sample that is green with no post-generation hand-edits.

## Specmatic configuration

- **CFG-1 (single config source) · blocker · auto.**
  Artifact: file tree + test sources. Assert: exactly one checked-in
  `specmatic.yaml`; tests do not generate, copy, overwrite, or mutate another
  Specmatic config, and it is not relocated into a build directory (e.g.
  `build/.../specmatic.yaml`). Oracle: file tree + static scan of test code.
- **CFG-2 (config schema version) · blocker · auto.**
  Artifact: `specmatic.yaml`. Assert: top-level `version` is the current
  supported config-schema version. Oracle: supported schema version for the
  resolved runtime.
- **CFG-3 (schema resiliency shipped ON) · blocker · auto.**
  Artifact: `specmatic.yaml`. Assert:
  `specmatic.settings.test.schemaResiliencyTests == all`, unless the manifest
  learnings document an unresolvable contract gap that forced a lower level.
  Oracle: parsed config + manifest learnings.

## Endpoint discovery and infrastructure filtering

- **DISC-1 (discovery wired and reachable) · blocker · auto.**
  Applies to: backend, bff. Artifact: `specmatic.yaml` + run log. Assert: a
  discovery source is configured and was reachable during the run — no
  `Failed to query swaggerUI` / `Actuator is not enabled` /
  `cannot calculate actual coverage`. Oracle: parsed config + run log.
- **DISC-2 (discovery implies a filter) · blocker · auto.**
  Applies to: backend, bff. Statement: discovery reports a superset of contract
  operations (frameworks add infrastructure/health/diagnostic/management/error
  routes), so whenever discovery is configured a filter excluding every
  discovered non-contract route must also be configured — or the coverage
  universe is distorted. A passing run or an above-threshold percentage is **not**
  evidence the filter was unnecessary. Artifact: `specmatic.yaml` + coverage
  report. Assert: if a discovery source is present, a matching filter is present,
  AND the coverage report shows no non-contract route and an eligible-operation
  count equal to the contract operation count. Oracle: coverage report +
  resolved contract operation count.
- **DISC-3 (discovery source kind matches the mechanism) · blocker · auto.**
  Statement: use the discovery mechanism the application's framework actually
  provides, pointed at input of the kind that mechanism expects. A discovery
  source read in the wrong format is a defect even when the run passes. Artifact:
  `specmatic.yaml` + the served discovery source. Assert: the configured
  discovery source returns the format its key expects. Oracle: the live
  discovery response shape.

## Coverage governance

- **COV-1 (governance present and enforced) · blocker · auto.**
  Applies to: backend, bff. Statement: coverage governance is protocol-
  independent — every backend and bff sample enforces it regardless of protocol
  (REST, Kafka, gRPC, GraphQL, SOAP). Artifact: `specmatic.yaml`. Assert: a
  governance/success-criteria block exists with `minCoveragePercentage`,
  `maxMissedOperationsInSpec`, and `enforce: true`. Oracle: parsed config +
  role/protocol from manifest.
- **COV-2 (threshold is achieved, with margin, not lowered to pass) · blocker · auto.**
  Artifact: `specmatic.yaml` + coverage report. Assert: achieved coverage `>=`
  configured `minCoveragePercentage`, AND configured `>=` the role baseline, AND
  the threshold is not pinned exactly to the achieved value (leave margin so
  normal variance does not red the build). The threshold is never lowered below
  what the sample actually achieves to go green. Oracle: achieved coverage from
  the report + role baselines from `guides/specmatic-runtime.md`.

## Version and toolchain consistency

- **VER-1 (declared toolchain versions are consistent and verified) · blocker · auto.**
  Statement: any toolchain version written into config must match the **same
  artifact** in the build and be a version that actually works — declared values
  are derived from the resolved toolchain, never copied from an example. Compare
  like with like: a compiler version (e.g. `protocVersion`, the protoc compiler)
  matches the build's compiler version (e.g. `protoc.version` /
  `protocArtifact`), which is a *different artifact* from the runtime library
  (e.g. `protobuf-java`) and may legitimately differ from it. A non-latest
  version pinned because a newer one fails is acceptable when the reason is
  recorded in the manifest learnings. Artifact: `specmatic.yaml` + build file
  (`pom.xml`/`build.gradle`/`package.json`/`requirements.txt`) + lockfile +
  the run (descriptor/codegen step succeeded). Assert: each config version
  literal equals the corresponding build artifact's version (not a different
  artifact's), and the toolchain step the version controls succeeded in the run.
  Oracle: the resolved build toolchain + the run result.
- **VER-2 (runtime artifact version is resolved) · warning · auto.**
  Artifact: build file + manifest. Assert: the Enterprise artifact version is a
  current/verified version resolved at generation time, not an arbitrary
  hardcoded constant. Oracle: the artifact registry's latest/verified version.

## Runtime is Specmatic Enterprise

- **RT-1 (Enterprise-only artifacts) · blocker · auto.**
  Artifact: all generated files. Assert: the adapter and CI reference only
  `io.specmatic.enterprise:*`, `specmatic/enterprise:*`, or a documented
  Enterprise-native language artifact/API; the build file actually declares such
  an artifact. Oracle: static scan of generated files.
- **RT-2 (no open-source / public fallbacks) · blocker · auto.**
  Artifact: all generated files. Assert: no reference to `npm exec specmatic`,
  `npx specmatic`, `specmatic@`, `node_modules/specmatic/specmatic.jar`, or
  `specmatic/specmatic`. Oracle: static scan.
- **RT-3 (manifest records real runtime, reconciled) · blocker · auto.**
  Artifact: manifest + build file. Assert: `specmaticRuntime`
  artifact/version/source/invocation are present AND match the artifact actually
  referenced by the build/adapter. A license-initialization log line alone is
  not accepted as proof. Oracle: build file reconciliation.
- **REP-1 (Specmatic owns reports) · blocker · auto.**
  Artifact: generated files. Assert: no custom report HTML/templates/renderers;
  generated files may configure/capture/ignore/link Specmatic's report output
  only. Oracle: static scan.

## Integration mode

- **MODE-1 (adapter uses the selected, verified mode) · blocker · auto.**
  Artifact: manifest + contract test adapter. Assert: the adapter implements the
  declared `integration_mode` (`cli`/`docker-cli`/`test-container`/`native`),
  and the mode is verified for the stack. Oracle: adapter static scan.
- **MODE-2 (cli is invoked directly) · blocker · auto.**
  Applies to: `cli`. Assert: Specmatic is invoked directly (the executable JAR
  runs `test`); it is not routed through a native test adapter; the sample does
  not clone the contract repo or build its own report — Specmatic resolves and
  caches contracts itself. Oracle: adapter static scan.
- **MODE-3 (native requires a verified Enterprise-native artifact) · blocker · manual+auto.**
  Applies to: `native`. Assert: an official Enterprise-native artifact/API
  exists for the stack and is used; if none is verified, `native` is rejected
  and a supported mode chosen — never a fake adapter. Oracle: artifact registry
  lookup. (Treat a failed registry lookup as inconclusive, not as proof of
  absence — confirm before rejecting.)

## Environment overridability

- **ENV-1 (runtime values overridable) · blocker · auto.**
  Artifact: `specmatic.yaml`. Assert: ports, dependency base URLs, service
  endpoints, broker URLs, and protocol-specific test settings are env-override
  templates (e.g. `{SUT_PORT:...}`). Oracle: parsed config.

## Consumer / dependency wiring

- **DEP-1 (dependency mapping documented and implemented) · blocker · manual.**
  Applies to: bff, frontend, any consumer. The contract-derived mapping between
  SUT/consumer operations and dependency mock operations is documented and
  implemented.

## Files and structure

- **FILE-1 (no stray files at destination root) · blocker · auto.**
  Artifact: destination root. Assert: nothing written at the provided root
  except the sample folder, except an updated existing root
  `.github/workflows/samples-ci.yml`. Oracle: file tree diff.
- **FILE-2 (self-contained) · blocker · auto.**
  Assert: the sample folder needs no shared generated assets outside itself.
- **FILE-3 (Dockerfile builds from source) · blocker · auto.**
  Applies to: when a `Dockerfile` is generated. Assert: it builds from source and
  does not copy local build outputs excluded by `.dockerignore` (`target/`,
  `dist/`, `build/`). Oracle: Dockerfile + `.dockerignore` scan.
- **FILE-4 (no local verification artifacts shipped) · blocker · auto.**
  Assert: local run/verification artifacts are git-ignored and not left as source
  files in the folder.
- **FILE-5 (required files present) · blocker · auto.**
  Assert: every file in **Required Files** below exists and is non-empty.

## Documentation

- **DOC-1 (README compliant) · blocker · manual+auto.**
  Artifact: `README.md`. Assert: complies with `guides/readme-generation.md` —
  required sections present and ordered, contract links point to the resolved
  executable specs, role-appropriate architecture assets included when available,
  run commands cover Unix/macOS + Windows PowerShell + Windows Command Prompt,
  test modes documented, project structure matches generated files.
- **DOC-2 (README assets shipped and listed) · blocker · auto.**
  Assert: README-required assets (e.g. architecture GIFs from `assets/`) are
  inside the sample folder and listed in the manifest.

## Manifest

- **MANI-1 (required fields present) · blocker · auto.**
  Assert: `.specmatic-sample-manifest.json` contains all fields in **Manifest
  Required Fields** below.
- **MANI-2 (manifest reconciles with reality) · blocker · auto.**
  Artifact: manifest + report + resolved spec. Assert: `protocol` matches the
  resolved spec format; `testCoverage` counts match the counts in the re-run
  report; `generated_files`/`assets` exist on disk. The manifest records claims
  that validation verifies — it is not taken on trust. Oracle: report + spec +
  file tree.

## CI

- **CI-1 (per-mode OS matrix) · blocker · auto.**
  Assert: `native`/`cli` → `[ubuntu, macos, windows]`; `docker-cli`/
  `test-container` → `ubuntu-latest` only. Oracle: workflow + manifest mode.
- **CI-2 (runtime + license setup) · blocker · auto.**
  Assert: CI sets up the required JRE/language runtime and the documented
  Enterprise runtime/license (`SPECMATIC_LICENSE_KEY`), uploads the report, and
  does not rely on incidental local state. Oracle: workflow scan.
- **CI-3 (root samples-ci job) · blocker · auto.**
  Applies to: when the destination root has `.github/workflows/samples-ci.yml`.
  Assert: it includes a job for this sample (job named by sample id,
  `working-directory: <sample-id>`, correct install/test commands, runtime +
  Docker setup per mode, report upload). Oracle: workflow scan.

---

## Required Files

| File | Purpose |
|------|---------|
| `specmatic.yaml` | Points to central contract repo, defines test config, and contains environment-template runtime values |
| Build file (`package.json` / `pom.xml` / `build.gradle` / `requirements.txt`) | Dependencies including Specmatic |
| Lockfile when produced by the package manager | Enables reproducible installs and CI locked installs |
| Source code (controllers/routes/services/resolvers/handlers) | Implements all operations from the contract |
| Data layer (db/store) | In-memory store with seed data when the role needs local state |
| Contract test file | Adapter that starts app + runs Specmatic through the selected integration mode. Name the test class/function `ContractTest` (e.g., `ContractTest.java`, `contract.test.ts`, `test_contract.py`). |
| `Dockerfile` | Production container image that builds from source; use a multi-stage build for compiled stacks instead of copying local build outputs from ignored folders |
| `.dockerignore` when `Dockerfile` is generated | Keeps dependencies, virtualenvs, reports, caches, Specmatic repos, and local files out of image build context |
| `.github/workflows/ci.yml` | CI pipeline: test + Docker build |
| `README.md` | Generated per `guides/readme-generation.md` — includes value prop, run instructions, and architecture |
| `.gitignore` | Ignore dependency folders, virtualenvs, build output, reports, caches, and `.specmatic` |
| `.specmatic-sample-manifest.json` | Records generated files, test coverage, and learnings |

## Manifest Required Fields

The `.specmatic-sample-manifest.json` must include:

```json
{
  "id": "<sample-id>",
  "application_type": "<backend|bff|frontend>",
  "protocol": "<rest|kafka|grpc|graphql|soap>",
  "language": "<language>",
  "framework": "<framework>",
  "data_layer": "<data-layer>",
  "integration_mode": "<native|cli|docker-cli|test-container>",
  "specmaticRuntime": {
    "artifact": "<io.specmatic.enterprise:executable-all|specmatic/enterprise|enterprise-native-artifact>",
    "version": "<version-or-tag>",
    "source": "<maven-central|docker-hub|official-enterprise-docs>",
    "invocation": "<java -jar|docker run|testcontainers|native-api>"
  },
  "contract_version": "<version>",
  "port": 8080,
  "test_command": "<command>",
  "install_command": "<command>",
  "generated_files": ["<list of generated file paths>"],
  "testCoverage": {
    "level1_none": { "tests": 0, "passed": 0, "failed": 0 },
    "level2_positiveOnly": { "tests": 0, "passed": 0, "failed": 0 },
    "level3_all": { "tests": 0, "passed": 0, "failed": 0 },
    "shipped_level": "all"
  },
  "learnings": [
    "Brief description of any issue encountered and how it was resolved"
  ]
}
```

- `testCoverage`: records test counts at each progressive verification level.
  `shipped_level` indicates which `schemaResiliencyTests` value the delivered
  `specmatic.yaml` uses. (Validated by MANI-2 against the re-run report.)
- `specmaticRuntime`: records the official Enterprise runtime artifact used by
  the generated sample. It must never identify the public npm `specmatic`
  package, the bundled npm `specmatic.jar`, or the `specmatic/specmatic` Docker
  image. (Validated by RT-3.)
- `learnings`: array of strings documenting issues encountered during
  generation — dependency conflicts, contract gaps, framework quirks, or
  workarounds applied. Empty array if generation was clean.

## CI Workflow Must Include

1. Multi-OS matrix strategy based on integration mode:
   - `native` or `cli` modes: `[ubuntu-latest, macos-latest, windows-latest]`
     (only requires Java/language runtime, available on all OSes)
   - `docker-cli` or `test-container` modes: `ubuntu-latest` only
     (Docker requires a Linux kernel; macOS/Windows CI runners do not have
     Docker pre-installed, and installing it adds fragile VM setup + minutes
     of startup time)
2. Setup JRE 17 when the selected integration mode or language runtime requires it
3. Setup language runtime (Node 20 / JDK 17 / Python 3.x)
4. Install dependencies, including CLI/JAR, Docker image access, Testcontainers, or native Specmatic test support for the selected integration mode
5. Run tests
6. Upload test report artifact
7. (On main branch) Build and push Docker image

All generated samples must use Specmatic Enterprise (`specmatic/enterprise`
Docker image, `io.specmatic.enterprise` Maven artifacts, or documented
Enterprise-native language artifacts). CI must document the required
`SPECMATIC_LICENSE_KEY` environment variable in the generated README. Do not
rely on accidental local license files, preinstalled CLIs, private caches,
checked-out contract repo state, the public npm `specmatic` package, or
open-source Docker images.

For `docker-cli`, CI must make Docker available and run Specmatic through a
direct `docker run` command or wrapper script using the official Docker image.

For `test-container`, CI must make Docker available to the test process and run
Specmatic through the official Docker image from the generated test suite
instead of requiring a local Java installation for Specmatic itself.

For `cli`, CI must install or download the verified Enterprise executable JAR
from an `io.specmatic.enterprise:*` Maven artifact and run it from the
generated test adapter.

For `native`, CI must install the official Enterprise-native Specmatic test
dependency for the selected language and run the generated native test class or
module. If no Enterprise-native artifact is verified, `native` is invalid for
that stack.

## Root Samples CI

When the destination root already has `.github/workflows/samples-ci.yml`, add or update one job for the generated sample:

- Use the sample id as the job name.
- Run commands with `working-directory: <sample-id>`.
- Use the install and test commands generated for the selected language and framework.
- Set up JRE 17 before running Specmatic tests when required by the selected
  language or integration mode.
- Set up the language runtime required by the selected language.
- Set up Docker when the selected integration mode is `docker-cli` or
  `test-container`.
- Include the documented Enterprise runtime/license setup for every generated
  sample.
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

The single test command must use the selected Specmatic integration mode:

- `cli`: starts the app and runs the verified Enterprise executable JAR from an
  `io.specmatic.enterprise:*` artifact.
- `docker-cli`: starts the app and runs the official Specmatic Docker image
  through a direct Docker command.
- `test-container`: starts the app and runs the official Specmatic Docker image
  from the generated test suite.
- `native`: runs the generated native Specmatic test class/module.

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

---

## Check Index

Quick map of blocker checks for the validator. `auto` checks are intended to be
script-enforced; `manual` checks need judgement.

| ID | Method | What it guards |
|----|--------|----------------|
| VAL-1..3 | auto | Independent re-run, preserved evidence, derived oracles |
| TEST-1..2 | auto | Single command green, zero failures |
| TEST-3..4 | manual | Behaviour matches contract, no manual fixups |
| CFG-1..3 | auto | One config source, schema version, resiliency `all` |
| DISC-1..3 | auto | Discovery wired, discovery⇒filter, source-kind match |
| COV-1..2 | auto | Governance present (role-based), threshold achieved with margin |
| VER-1 | auto | Declared toolchain versions equal resolved versions |
| VER-2 | warning | Runtime artifact version resolved, not hardcoded |
| RT-1..3, REP-1 | auto | Enterprise-only, no public fallbacks, manifest reconciled, Specmatic owns reports |
| MODE-1..2 | auto | Adapter matches mode, cli invoked directly |
| MODE-3 | manual+auto | Native only with a verified artifact |
| ENV-1 | auto | Runtime values env-overridable |
| DEP-1 | manual | Dependency mapping documented + implemented |
| FILE-1..5 | auto | No stray root files, self-contained, Dockerfile from source, no local artifacts, required files present |
| DOC-1..2 | manual+auto | README compliant, assets shipped + listed |
| MANI-1..2 | auto | Manifest fields present and reconciled with reality |
| CI-1..3 | auto | Per-mode OS matrix, runtime/license setup, root samples-ci job |
