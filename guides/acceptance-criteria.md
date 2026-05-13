# Acceptance Criteria

A generated sample is "done" when ALL of the following are true.

All checks apply inside the generated sample folder at `<provided-location>/<sample-id>/`.

## Must Pass

- [ ] `npm test` (or equivalent) exits with code 0
- [ ] All Specmatic contract tests pass (0 failures)
- [ ] No manual intervention needed after generation
- [ ] Generated behavior matches the applicable files in `contracts/`
- [ ] No generated files are written at the provided location root except the sample folder itself
- [ ] The sample folder is self-contained and does not require shared generated assets outside the folder

## Required Files

| File | Purpose |
|------|---------|
| `specmatic.yaml` | Points to central contract repo, defines test config |
| Build file (`package.json` / `pom.xml` / `build.gradle` / `requirements.txt`) | Dependencies including Specmatic |
| Source code (controllers/routes) | Implements all endpoints from the contract |
| Data layer (db/store) | In-memory store with seed data when the role needs local state |
| Contract test file | Adapter that starts app + runs Specmatic |
| `Dockerfile` | Production container image |
| `.github/workflows/ci.yml` | CI pipeline: test + Docker build |
| `README.md` | Prerequisites, how to run, how it works |
| `.gitignore` | Ignore node_modules/build/target/.specmatic |
| `.specmatic-sample-manifest.json` | Records generated files owned by this sample |

## CI Workflow Must Include

1. Setup JRE 17 (Specmatic requires Java)
2. Setup language runtime (Node 20 / JDK 17 / Python 3.x)
3. Install dependencies
4. Run tests
5. Upload test report artifact
6. (On main branch) Build and push Docker image

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
