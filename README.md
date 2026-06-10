# Specmatic Sample Generator Skill

An AI skill for Claude Code and Codex that generates and maintains working Specmatic contract testing sample projects. Give it a tech stack, and it produces a complete, tested, ready-to-run sample — verified against real Specmatic contract tests.

## What It Does

- **Generate mode**: Creates a new sample project from scratch for any supported stack combination
- **Maintain mode**: Updates existing samples — bumps versions, refreshes config, fixes contract drift

The generated sample is only "done" when tests pass with zero Specmatic failures.

## Supported Stacks

| Dimension | Options |
|-----------|---------|
| App Type | Backend, BFF, Frontend |
| Protocol | REST/OpenAPI, Kafka/AsyncAPI, gRPC, GraphQL, SOAP/WSDL |
| Language | Java, TypeScript, JavaScript, Python, Go, C#, etc. |
| Framework | Spring Boot, Express, React, Flask, Gin, ASP.NET Core, Apollo Server, etc. |
| Integration Mode | `native`, `cli`, `docker-cli`, `test-container` |

## How to Use

### Codex

Install as a Codex skill:

```bash
mkdir -p ~/.codex/skills
git clone https://github.com/himanshibansal2202/Specmatic-sample-generator-skill.git ~/.codex/skills/generate-specmatic-sample
```

### Claude Code

Install as a personal skill:

```bash
mkdir -p ~/.claude/skills
git clone https://github.com/himanshibansal2202/Specmatic-sample-generator-skill.git ~/.claude/skills/generate-specmatic-sample
```

### Recommended Models

This skill has been tested and works best with these models:

| Tool | Tested On | Recommended |
|------|-----------|-------------|
| Claude Code | claude-opus-4-7 | claude-opus-4-7 or above |
| Codex | gpt-5.5-medium | gpt-5.5-medium or above |

### Example Prompt

```
Read the SKILL.md file and all referenced files. Then follow the generate workflow.

Inputs:
1. Contract repo: https://github.com/specmatic/specmatic-order-contracts.git
   Spec: io/specmatic/examples/store/openapi/api_order_v5.yaml
2. Application type: backend
3. Language: java
4. Framework: spring-boot
5. Specmatic integration mode: native
6. Destination path: /tmp/my-sample

Skip interactive questions and proceed through all workflow steps.
Report test counts at each level.
```

## Skill Architecture

The skill is structured so that the AI loads only what it needs for a given generation. `SKILL.md` is the entry point — it defines the workflow and tells the AI which guide files to read based on the user's inputs.

### File Purposes

| File | Purpose | When the AI reads it |
|------|---------|---------------------|
| `SKILL.md` | Defines the generate/maintain workflow, input collection, and step sequencing | Always (first file read) |
| `guides/specmatic-runtime.md` | How to assemble `specmatic.yaml`, configure test/mock modes, schema resiliency settings, coverage governance | Every generation — this is the Specmatic config bible |
| `guides/acceptance-criteria.md` | Definition of "done" — what must be true before the sample is considered complete (tests pass, CI works, manifest written) | Every generation — checked at the end |
| `guides/backend-generation.md` | Backend-specific patterns: in-memory data store, seed data, how to handle CRUD endpoints | When app type = backend |
| `guides/bff-generation.md` | BFF-specific patterns: forwarding to backend mock, path filtering, dependency mapping | When app type = bff |
| `guides/frontend-generation.md` | Frontend-specific patterns: stub mode, UI test setup | When app type = frontend |
| `guides/protocol-generation.md` | Protocol-specific notes: Kafka broker config, gRPC proto handling, WSDL specifics | When protocol ≠ REST |
| `guides/readme-generation.md` | Template for the generated sample's README (architecture diagrams, run instructions, OS variants) | At the end of generation |
| `config/contract-resolution.yaml` | Maps contract repos to spec file paths, defines discovery patterns for dependencies | During dependency resolution |
| `test-data/backend-seed-data.md` | Exact seed data (product/order IDs and fields) that backend samples must pre-load so contract test assertions pass | When app type = backend |
| `assets/*.gif` | Architecture diagrams embedded in generated READMEs | During README generation |

### Design Patterns

**Separation of concerns**: `SKILL.md` handles workflow orchestration. Guides handle domain knowledge. This means you can fix a BFF-specific issue without touching the core workflow.

**Spec-driven**: The user provides a contract spec file path. The skill infers protocol, dependencies, and required behaviors from the spec itself — no manual enumeration of endpoints.

**Progressive verification**: Samples are tested at three resiliency levels (none → positiveOnly → all). This catches silent misconfigurations — if test count doesn't increase between levels, something is wrong.

**Learnings accumulate**: Each failed generation run produces insights that get added to the relevant guide. The skill gets smarter over time without changing its workflow.

**Contract as source of truth**: The guides provide patterns and heuristics, but when there's a conflict, the executable contract wins. The AI reads the actual spec to determine exact schemas, status codes, and behaviors.

## Repository Structure

```
├── SKILL.md                    # Core workflow (the AI reads this first)
├── config/
│   └── contract-resolution.yaml  # Contract repo URLs, spec paths, discovery patterns
├── guides/
│   ├── acceptance-criteria.md    # Definition of "done" for a generated sample
│   ├── readme-generation.md      # How to generate the sample's README
│   ├── specmatic-runtime.md      # specmatic.yaml structure, test config, integration modes
│   ├── backend-generation.md     # Backend role behavior
│   ├── bff-generation.md         # BFF role behavior
│   ├── frontend-generation.md    # Frontend role behavior
│   └── protocol-generation.md    # Protocol-specific generation notes
├── test-data/
│   └── backend-seed-data.md      # Required seed data for backend samples
├── assets/
│   ├── specmatic-order-backend-architecture.gif
│   ├── specmatic-order-bff-architecture.gif
│   └── specmatic-order-frontend-architecture.gif
```

## Key Design Decisions

### Progressive Test Verification

Generated samples are verified in three levels:

| Level | Setting | What it tests |
|-------|---------|---------------|
| 1 | `schemaResiliencyTests: none` | Named examples only — basic routing, schema shape |
| 2 | `positiveOnly` | All valid input combinations — enum permutations, optional fields |
| 3 | `all` | Negative/boundary tests — nulls, wrong types, missing fields (expects 400) |

Test count must strictly increase between levels. A flat count means the config is silently ignored.

### Contract Source of Truth

The executable contract (from [specmatic-order-contracts](https://github.com/specmatic/specmatic-order-contracts)) always wins. If local guides contradict the contract, implement the contract.

### Stub vs Test Mode

- **Backend/BFF**: Specmatic runs in `test` mode — generates requests and validates responses. Progressive verification applies.
- **Frontend**: Specmatic runs in `stub` mode — responds to the app's requests. Progressive verification is skipped.

## Maintaining This Skill

### How it works

The AI reads `SKILL.md` first, which references other files via relative paths. The AI follows those references to load guides, config, and test data as needed. The skill is self-contained — no external dependencies beyond the contract repo.

### Adding learnings

When a generation run reveals a new pattern or pitfall:
1. Add it to the relevant guide file (not SKILL.md unless it's a workflow change)
2. Be specific and actionable — describe the symptom, root cause, and fix
3. If it's a specmatic.yaml config issue, add it to `guides/specmatic-runtime.md`
4. If it's role-specific (backend/BFF/frontend), add it to the corresponding guide

### Editing guidelines

- **Don't delete content** unless it's explicitly wrong or redundant — make additive edits
- **Keep guides generic** — they should help the AI generate for any language/framework, not just Java
- **Test after changes** — run a generation and verify test counts still match expectations
- **Use branches + PRs** — never commit directly to main

### Reference repos for validation

These are the "bible" — generated samples should achieve similar test counts:
- Backend: [specmatic-order-api-java](https://github.com/specmatic/specmatic-order-api-java) (293 tests)
- BFF: [specmatic-order-bff-java](https://github.com/specmatic/specmatic-order-bff-java) (269 tests)

### Known patterns (from real generation runs)

| Pattern | Resolution |
|---------|-----------|
| Kotlin version conflict with Spring Boot | Override `kotlin.version` to match Specmatic's requirement |
| Framework adds `path` field to error responses | Override default error handler to return only contract-defined fields |
| `schemaResiliencyTests` silently ignored | Must be under `specmatic.settings.test`, not `components.settings.test` |
| Docker not available on macOS/Windows CI | `docker-cli`/`test-container` modes → ubuntu-only CI |

## Links

- [Specmatic Website](https://specmatic.io)
- [Specmatic Documentation](https://docs.specmatic.io)
- [Contract Repository](https://github.com/specmatic/specmatic-order-contracts)
