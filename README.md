# Specmatic Sample Generator Skill

An AI skill that generates and maintains working Specmatic contract testing sample projects. Give it a tech stack, and it produces a complete, tested, ready-to-run sample — verified against real Specmatic contract tests.

## What It Does

- **Generate mode**: Creates a new sample project from scratch for any supported stack combination
- **Maintain mode**: Updates existing samples — bumps versions, refreshes config, fixes contract drift

The generated sample is only "done" when `./mvnw test` (or equivalent) passes with zero Specmatic failures.

## Supported Stacks

| Dimension | Options |
|-----------|---------|
| App Type | Backend, BFF, Frontend |
| Protocol | REST/OpenAPI, Kafka/AsyncAPI, gRPC, GraphQL, SOAP/WSDL |
| Language | Java, TypeScript, JavaScript, Python, Go, C#, etc. |
| Framework | Spring Boot, Express, React, Flask, Gin, ASP.NET Core, Apollo Server, etc. |
| Integration Mode | `native`, `cli`, `docker-cli`, `test-container` |
| Data Layer | in-memory, rest-api, grpc-service, kafka-broker |

## Repository Structure

```
├── SKILL.md                    # Core workflow (the AI reads this)
├── USAGE.md                    # How to invoke the skill in different AI tools
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
└── agents/
    └── openai.yaml               # OpenAI agent configuration
```

## How to Use

### Kiro CLI

```
Read the SKILL.md file at /path/to/Specmatic-sample-generator-skill/SKILL.md
and all referenced files. Then follow the generate workflow.

Inputs:
1. Application type: backend
2. Protocol: rest
3. Contract version: v3
4. Language: java
5. Framework: spring-boot
6. Specmatic integration mode: native
7. Data layer: in-memory
8. Destination path: /tmp/my-sample

Skip interactive questions and proceed through all workflow steps.
Report test counts at each level.
```

### Claude Code / Codex

See [USAGE.md](USAGE.md) for installation as a personal skill.

## Key Design Decisions

### Progressive Test Verification

Generated samples are verified in three levels before delivery:

| Level | Setting | What it tests |
|-------|---------|---------------|
| 1 | `schemaResiliencyTests: none` | Named examples only — basic routing, schema shape |
| 2 | `positiveOnly` | All valid input combinations — enum permutations, optional fields |
| 3 | `all` | Negative/boundary tests — nulls, wrong types, missing fields (expects 400) |

If Level 3 passes fully → ship with `all`. If unresolvable contract gaps remain → ship with `none`.

Test count must strictly increase between levels. A flat count means the config is silently ignored.

### Contract Source of Truth

The executable contract (fetched from [specmatic-order-contracts](https://github.com/specmatic/specmatic-order-contracts)) always wins. If local guides contradict the contract, implement the contract.

### Stub vs Test Mode

- **Backend/BFF**: Specmatic runs in `test` mode — it generates requests and validates responses. Progressive verification applies.
- **Frontend**: Specmatic runs in `stub` mode — it responds to the app's requests. Progressive verification is skipped (test count is determined by the app's own test suite).

### Maintain Mode: Plan Before Acting

Maintain mode shows what will be updated and waits for confirmation before changing anything. Users can selectively skip framework upgrades while still bumping Specmatic.

## Known Patterns (from real generation runs)

| Pattern | Resolution |
|---------|-----------|
| Kotlin version conflict with Spring Boot | Override `kotlin.version` to match Specmatic's requirement |
| Framework adds `path` field to error responses | Override default error handler to return only contract-defined fields |
| `schemaResiliencyTests` silently ignored | Must be under `specmatic.settings.test`, not `components.settings.test` |
| Enum without 4xx response in contract | Unresolvable gap — accept failures, document in manifest |
| Docker not available on macOS/Windows CI | `docker-cli`/`test-container` modes → ubuntu-only CI |

## Quick Verification

After a sample is generated:

```bash
# Java/Spring Boot
cd <sample-folder> && ./mvnw test

# Node.js/TypeScript
cd <sample-folder> && npm install && npm test

# Python
cd <sample-folder> && pip install -r requirements.txt && pytest test -v -s
```

## Links

- [Specmatic Website](https://specmatic.io)
- [Specmatic Documentation](https://docs.specmatic.io)
- [Contract Repository](https://github.com/specmatic/specmatic-order-contracts)
- [Sample Monorepo](https://github.com/himanshibansal2202/specmatic-sample-monorepo)
