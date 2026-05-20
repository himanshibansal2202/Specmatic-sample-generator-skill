# Regression & Maintenance Guide

This guide describes the regression and maintain modes of the skill. Both modes
operate on a samples repository (local checkout or git URL) containing
previously generated samples.

## Mode Selection

When the user invokes the skill, ask:

> "What would you like to do? (generate, regression, maintain)"

- **generate** — interactive generation of a single new sample (existing workflow).
- **regression** — generate all predefined combinations from the regression
  matrix, verify locally, and push to the configured destination repo.
- **maintain** — run tests on all existing samples in a repo, fix failures,
  and push fixes.

## Regression Mode

### Purpose

Verify that the skill can generate working samples for all supported
combinations. The generated samples are pushed to a repository where CI
validates them across OS/environment matrix.

### Workflow

1. Read `config/regression-matrix.yaml` for the list of combinations.
2. Ask: "Where should I generate the samples? Provide a local path or
   repository link."
   - If the destination already contains samples (existing repo), the skill
     generates new samples alongside them without modifying existing ones.
   - If the destination is empty, generate fresh.
3. For each combination in the matrix:
   a. Run the full generation workflow (Steps 2–6 from the main workflow) using
      the combination's inputs. Skip Step 1 (inputs are predefined).
   b. Verify green locally.
   c. If generation or verification exceeds 10 minutes for a single
      combination, abandon it, record as timed out, and move to the next.
   d. If verification fails after max retries, record the failure and continue
      to the next combination.
4. Generate or update the root `.github/workflows/verify-all.yml` to include
   all successfully generated samples in the appropriate language matrix.
5. Push all successfully generated samples to the destination repo.
6. Report: which passed, which failed, which timed out, total count, and
   destination.
   ```
   Regression complete:
   ✅ backend-rest-javascript-express-in-memory (passed, 34s)
   ✅ backend-rest-java-spring-boot-in-memory (passed, 52s)
   ⏱️ backend-rest-python-flask-in-memory (timed out after 10m)
   ❌ bff-rest-javascript-express-rest-api (failed: dependency conflict)
   ...
   5/7 passed. 1 timed out. 1 failed. Pushed to <repo>. CI will validate across OS matrix.
   ```

### Regression Matrix Format

```yaml
regression:
  contract_version: v3
  destination:
    samples_directory: samples
  combinations:
    - application_type: backend
      protocol: rest
      language: javascript
      framework: express
      data_fetch: in-memory
    # ... more combinations
```

Each combination uses the same fields as the generation workflow inputs. The
`contract_version` at the top applies to all combinations unless overridden
per entry.

### Adding Combinations

Add entries to `config/regression-matrix.yaml` as new stacks are validated.
Non-REST protocol combinations should only be added after confirming that
Specmatic Enterprise setup is available and documented.

## Maintain Mode

### Purpose

Keep existing generated samples green over time. When contracts change,
Specmatic upgrades, or dependencies drift, maintain mode detects failures and
fixes them using the same convergence loop as generation.

### Workflow

1. Ask: "Which samples repo should I maintain? (local path or git URL)"
2. If git URL, clone or locate a local checkout.
3. Find all `.specmatic-sample-manifest.json` files in the repo.
4. For each sample with a manifest:
   a. Read the manifest to determine language, framework, and test command.
   b. Install dependencies.
   c. Run the test command.
   d. If green → skip, record as passing.
   e. If red → read Specmatic/JUnit report output, classify the failure, apply
      the smallest fix needed, re-run (max 3 retries, same as Step 6).
   f. If still red after retries → record as unfixable.
5. Report results:
   ```
   Maintenance complete:
   ✅ backend-rest-javascript-express-in-memory (already green)
   ✅ backend-rest-java-spring-boot-in-memory (fixed: bumped specmatic dep)
   ❌ backend-grpc-kotlin-spring-boot-in-memory (unfixable: Enterprise license missing)
   ...
   5/6 green. 1 unfixable.
   ```
6. If fixes were applied, commit them with a descriptive message and push (or
   open a PR, depending on user preference).

### What Maintain Mode Can Fix

- Dependency version bumps (Specmatic library, framework, runtime)
- Contract schema changes (new fields, changed status codes, new endpoints)
- CI template updates (new runtime versions, changed action versions)
- Specmatic config syntax changes (specmatic.yaml schema updates)

### What Maintain Mode Cannot Fix

- Missing Specmatic Enterprise license/setup
- Fundamental protocol support gaps
- Broken contract repository (unreachable, deleted specs)

These are reported as unfixable with a clear reason.

### Samples Without a Manifest

If a sample folder has no `.specmatic-sample-manifest.json`, maintain mode
skips it and reports: "Unmanaged sample, skipping: `<folder-name>`"

## Manifest Requirements

Both regression and maintain modes depend on the manifest. The manifest must
include generation inputs so the skill can identify the sample's stack without
re-asking the user.

See `guides/acceptance-criteria.md` for the manifest schema.

## CI Integration

The skill generates or updates a root-level `.github/workflows/verify-all.yml`
in the destination repo (see `guides/acceptance-criteria.md` for the full
pattern). This workflow:

1. Groups samples by language into separate jobs with matrix strategy.
2. Sets up JRE 17 + language runtime, installs deps, runs tests, verifies
   Docker build, and uploads report artifacts.
3. **Must run across multi-OS matrix: `[ubuntu-latest, macos-latest,
   windows-latest]`.** Some protocols (Kafka, gRPC) have been observed to fail
   on specific OS combinations — multi-OS testing catches these platform-specific
   issues that local verification misses.
4. Includes a final `regression-check` job that gates on all test jobs.

The regression mode generates samples and pushes them. CI validates them across
all OS targets. If CI fails on a specific OS, the maintain mode can be invoked
to fix the platform-specific failures.
