# Specmatic Sample Generator Skill — Gap Analysis

> Last updated: 2026-05-19 (post merge of multi-protocol support to main)

## Current Status

The skill is a portable AI agent plugin (Claude Code, Codex, Kiro) that generates self-contained Specmatic contract-testing sample projects. It follows an interactive workflow: collect inputs → validate combination → resolve contracts → generate code → verify until green.

Multi-protocol support (Kafka/AsyncAPI, gRPC, GraphQL, SOAP/WSDL) is now merged to main alongside REST/OpenAPI.

---

## What's Covered

| Requirement | Status | Notes |
|---|---|---|
| **Application Types** | | |
| Backend, BFF, Frontend | ✅ | All three supported for all protocols |
| **Protocols** | | |
| HTTP REST / OpenAPI | ✅ | Fully supported, `requires_enterprise: false` |
| Kafka / AsyncAPI | ✅ Guides ready | `requires_enterprise: true`, official samples configured, discovery patterns defined |
| gRPC / Protobuf | ✅ Guides ready | `requires_enterprise: true`, official samples configured, discovery patterns defined |
| GraphQL SDL | ✅ Guides ready | `requires_enterprise: true`, official samples configured, discovery patterns defined |
| SOAP / WSDL | ✅ Guides ready | `requires_enterprise: true`, official samples configured, discovery patterns defined |
| Protocol alias normalization | ✅ | `rest/openapi`, `kafka/asyncapi`, `grpc`, `graphql`, `soap/wsdl` |
| Protocol-specific contract facts | ✅ | `guides/protocol-generation.md` |
| **Languages & Frameworks** | | |
| JavaScript, TypeScript, Java, Python | ✅ | Reasoned validation (no hardcoded matrix) |
| Express, Spring Boot, Flask | ✅ | Common examples in prompts |
| **Data Fetch** | | |
| In-memory, REST API | ✅ | Backend=in-memory, BFF=rest-api |
| gRPC-service, Kafka-broker | ✅ Prompt examples | Listed in Step 1 prompts for non-REST |
| **Contract Resolution** | | |
| Central contract repo (`specmatic-order-contracts`) | ✅ | All protocols resolve from same repo |
| Discovery patterns per protocol/role | ✅ | Defined in `contract-resolution.yaml` |
| Official sample repos as fallback | ✅ | Configured for Kafka, gRPC, GraphQL, SOAP |
| Version resolution (default v3) | ✅ | Pattern-based with `{version}` placeholder |
| **Generation** | | |
| `specmatic.yaml` (protocol-aware) | ✅ | `run_option_key` per protocol |
| Self-contained sample folder | ✅ | Key design rule |
| README.md | ✅ | Required file |
| Build files | ✅ | Per stack |
| Source code (routes/services/resolvers/handlers) | ✅ | Protocol-native generation |
| Contract test adapter | ✅ | Minimal adapter pattern |
| Dockerfile + .dockerignore | ✅ | Required files |
| `.github/workflows/ci.yml` | ✅ | Multi-OS matrix + Enterprise CI notes |
| `.gitignore` | ✅ | Required |
| `.specmatic-sample-manifest.json` | ✅ | Tracks generated files |
| Lockfile generation | ✅ | When package manager produces one |
| **Verification** | | |
| Local single-command test | ✅ | `npm test` / `./mvnw test` / `pytest` |
| Verify until green (max 3 retries) | ✅ | Workflow Step 6 |
| Failure classification | ✅ | 8 categories including protocol adapter mismatch |
| **Configuration** | | |
| Ports configurable via env vars | ✅ | Including broker URLs, service endpoints |
| Seed data for Backend | ✅ | Derived from executable contract examples |
| BFF: no local DB, calls backend via env var | ✅ | `STUB_URL` pattern |
| Frontend: consumes BFF contract | ✅ | Client workflows |
| **Infrastructure** | | |
| Works across Claude Code, Codex, Kiro | ✅ | All three skill wrappers present |
| Mono-repo destination with root CI update | ✅ | Updates `samples-ci.yml` |
| JRE 17 setup in CI | ✅ | In acceptance criteria |
| Multi-OS CI matrix | ✅ | `[ubuntu-latest, macos-latest, windows-latest]` |
| **Safety Rules** | | |
| Never read existing generated samples | ✅ | Includes `.specmatic/` caches, build outputs |
| No hardcoded stack matrix | ✅ | Reasoned feasibility validation |
| Report-driven fixes only | ✅ | No speculative validation |
| Executable contract wins over guides | ✅ | Source-of-truth hierarchy |

---

## Gaps — Not Yet Handled

### End-to-End Verification

| Requirement | Gap | Impact |
|---|---|---|
| Verified green run for Kafka/AsyncAPI | No confirmed test pass | Guides exist but no proof the generated sample actually works |
| Verified green run for gRPC | No confirmed test pass | Same |
| Verified green run for GraphQL | No confirmed test pass | Same |
| Verified green run for SOAP/WSDL | No confirmed test pass | Same |
| Specmatic Enterprise setup documented concretely | Only "use the documented Enterprise Docker image/artifact" — no actual image name, version, or license mechanism | Blocks non-REST generation in practice |

### Languages & Frameworks

| Requirement | Gap | Impact |
|---|---|---|
| Kotlin | Not in examples; referenced in gRPC official samples | Can't generate gRPC-Kotlin without testing |
| GoLang | Not in examples; referenced in gRPC official samples | Can't generate gRPC-Go without testing |
| React (Frontend) | No frontend framework support | Frontend samples are headless clients only |

### Data Layers

| Requirement | Gap | Impact |
|---|---|---|
| JDBC | Not supported | No database-backed samples |
| Redis | Not supported | No cache-backed samples |
| Sqlite | Not supported | No lightweight DB samples |
| TestContainers | Not referenced | No container-based data layer testing |

### CI / DevOps

| Requirement | Gap | Impact |
|---|---|---|
| Code coverage (CI + local) | Not in acceptance criteria | No coverage reporting |
| Publish results to Specmatic Insights | No integration | No centralized test result tracking |
| License setup step for Enterprise | Not templated | Non-REST CI will fail without license |
| Multi-platform Docker image (`buildx`) | Single-platform only | No ARM/multi-arch support |
| Push Docker image to registry | No registry config (org, naming, secrets) | CI "build and push" step incomplete |

### Skill Lifecycle

| Requirement | Gap | Impact |
|---|---|---|
| Update/regenerate existing samples | No update mode | Manual maintenance of samples repo |
| Regression harness (all combos stay green) | No meta-CI | No automated verification across all combinations |
| Feedback mechanism / session logging | Not implemented | No observability into skill usage |
| Model version recommendation | Not documented | Users don't know which AI model works best |

### Contracts / Implementation Details

| Requirement | Gap | Impact |
|---|---|---|
| PATCH endpoints | Deferred to executable contract | May fail if contract has PATCH and skill doesn't handle it |
| DELETE endpoints | Deferred to executable contract | Same |
| PUT /products/{id}/image (multipart) | Mentioned but not detailed | May need special handling |
| Inventory as external service (vs in-memory) | In-memory only | No Specmatic mock for inventory boundary |
| Async Request-Reply (bidirectional) | AsyncAPI fire-and-forget covered; request-reply unclear | May not handle correlation IDs / reply channels |

---

## Questions for Client

### Resolved

| # | Question | Answer |
|---|---|---|
| 1 | Protocol expansion scope | In scope — now merged |
| 2 | Kotlin and GoLang | Future additions |
| 4 | JDBC/Redis/Sqlite | Not initial delivery |
| 12 | Model choice | Decide later |
| 15 | Idempotency-Key | No need to implement |

### Open

| # | Question | Status |
|---|---|---|
| 3 | Frontend framework (React app vs headless client) | Check from existing samples and confirm |
| 5 | Specmatic Insights API/format | Unanswered |
| 6 | License setup mechanism | Get from enterprise specs repo |
| 7 | Docker Hub org/account/naming | Unanswered |
| 8 | Code coverage tool/threshold | Unanswered |
| 9 | Update mode approach | Not Done — planning in progress |
| 10 | Regression harness | Yes, needed — tied to update mode |
| 11 | Feedback mechanism | Unanswered |
| 13 | Inventory as external service | Refer to existing Specmatic samples |
| 14 | Auth handling | Verify fix |

### New Questions (Post Multi-Protocol Merge)

| # | Question | Context |
|---|---|---|
| 16 | Enterprise Docker image/artifact name and version | `requires_enterprise: true` is set but no concrete image reference (e.g., `specmatic/enterprise:latest`?) |
| 17 | Enterprise license mechanism | Env var? License file? Docker secret? Needed for CI template and README |
| 18 | Protocol-specific test data | Should there be `test-data/kafka-seed-messages.md`, `test-data/grpc-fixtures.md`? Or is executable contract sufficient? |
| 19 | `specmatic-config.md` → `specmatic-runtime.md` rename | Was this intentional? References in BFF guide updated but any external docs pointing to old name? |

---

## Priority Recommendations

### P0 — Blocks Practical Use of Non-REST

1. **Document Specmatic Enterprise setup** — concrete image name, license mechanism, CI secrets needed
2. **Verify one non-REST sample end-to-end** — pick Kafka or gRPC, generate, run, confirm green

### P1 — Enables Samples Repo Maintenance at Scale

3. **Update mode** — regenerate from manifest inputs, delete + regenerate + verify
4. **Regression harness** — run update mode across all samples, report pass/fail

### P2 — Completeness

5. **Kotlin + Go** — add to examples, verify with gRPC official samples
6. **Code coverage** — add to acceptance criteria and CI template
7. **Docker registry push** — configure org, naming, secrets in CI

### P3 — Nice to Have

8. **Specmatic Insights integration**
9. **Frontend React framework**
10. **JDBC/Redis data layers**
11. **Feedback mechanism**

---

## Summary

The skill now covers **5 protocols** (REST, Kafka, gRPC, GraphQL, SOAP) across Backend/BFF/Frontend app types with protocol-aware contract resolution, `specmatic.yaml` generation, and role guides. The main remaining gaps are:

- **Verification**: No confirmed green test run for any non-REST protocol
- **Enterprise**: License/Docker setup not concretely documented — blocks non-REST in practice
- **Lifecycle**: No update/regenerate mode or regression harness for maintaining samples at scale
- **Breadth**: Kotlin, Go, React, database-backed data layers not yet supported
- **CI maturity**: Coverage, Insights, Docker registry push incomplete
