# README Generation Guide

Generated READMEs serve two purposes: help developers run the sample AND demonstrate why Specmatic matters. The README is a selling point for Specmatic, not just setup instructions.

## Required Sections (in order)

### 1. Title

```
# Specmatic Sample: <Framework> <App-Type> (<Protocol>)
```

Example: `# Specmatic Sample: Spring Boot Order API (REST/OpenAPI)`

### 2. What This Is

One paragraph explaining what this sample demonstrates. Include:
- The application type (backend, BFF, frontend)
- The protocol being contract-tested
- That Specmatic auto-generates tests from the API spec

Example tone: "This sample demonstrates how Specmatic contract tests a Spring Boot REST API in complete isolation — no running dependencies, no hand-written mocks, no integration environment needed."

### 3. Why Specmatic

Bullet list of benefits. Keep it to 3-4 points, protocol-aware. Focus on what
Specmatic uniquely offers, not generic contract testing concepts:

- **Auto-generated tests from your API spec** — Specmatic reads your OpenAPI/AsyncAPI/gRPC/GraphQL/WSDL spec and generates test cases automatically. No hand-written test code or mocks to maintain.
- **Intelligent service virtualisation** — Specmatic generates realistic stubs/mocks from the same spec, so consumers can develop in parallel without waiting for providers.
- **Backward compatibility detection** — Specmatic compares spec versions and flags breaking changes before they reach production.
- **Single source of truth** — one spec drives contract tests, stubs, and backward compatibility checks. No drift between tests and documentation.

For protocol-specific benefits, add one relevant point:
- REST/OpenAPI: "Works with your existing OpenAPI spec — no new DSL to learn."
- AsyncAPI/Kafka: "Validates message producers and consumers against your AsyncAPI contract without a running broker."
- gRPC: "Tests your Protobuf service definitions without spinning up dependent services."
- GraphQL: "Validates queries, mutations, and schema compliance from your GraphQL SDL."
- SOAP/WSDL: "Contract tests your SOAP services using existing WSDL definitions."

### 4. Tech

Numbered list of key technologies:
1. Language + Framework (e.g., "Spring Boot service written in Java")
2. Specmatic (mention the version if relevant)
3. Any notable runtime requirement (e.g., "JRE 17+", "Node 20+")

### 5. Run Contract Tests

Structure:
- **Prerequisites** — list what must be installed (JRE 17+, language runtime, etc.)
- **Using the build tool** — the primary single-command path (e.g., `./mvnw test`, `npm test`, `pytest`)
- **Using Docker** — if applicable, show the Docker-based test run
- Platform-aware commands (Unix/macOS vs Windows) when they differ

Include a note about first-run timing: "First run takes 1-2 minutes as Specmatic clones the contract repository. Subsequent runs are fast (cached in `.specmatic/`)."

Link to the specmatic.yaml file and explain it configures which contracts are tested.

### 6. How It Works

Brief explanation of the contract testing flow specific to this sample's role:

**Backend:**
```
API Spec (OpenAPI/Proto/etc.) → specmatic.yaml → Specmatic generates requests → Your service responds → Specmatic validates responses against the spec
```

**BFF:**
```
BFF Spec → specmatic.yaml → Specmatic generates requests to your BFF → Your BFF calls dependency (Specmatic mock) → Specmatic validates both sides
```

**Frontend:**
```
BFF Spec → specmatic.yaml → Specmatic mocks the BFF API → Your frontend calls the mock → Contract compliance verified
```

Explain in 2-3 sentences what happens when you run the test command. Mention that Specmatic reads `specmatic.yaml`, fetches the contract, and auto-generates test cases.

### 7. Project Structure

Table of key files:

```markdown
| File | Purpose |
|------|---------|
| `specmatic.yaml` | Contract test configuration — points to the API spec |
| `src/...` or equivalent | Application source code |
| `test/...` or equivalent | Contract test adapter that starts the app and runs Specmatic |
| `Dockerfile` | Production container image |
| `.github/workflows/ci.yml` | CI pipeline: test + Docker build |
```

Adjust paths for the actual generated structure.

### 8. For More Info

- [Specmatic Website](https://specmatic.io)
- [Specmatic Documentation](https://docs.specmatic.io)
- Link to the specific contract/spec used by this sample

## Optional Sections

Include these when relevant to the sample:

### Endpoints (Backend and BFF samples)

Table of API endpoints implemented by the sample:

```markdown
| Method | Path | Description |
|--------|------|-------------|
| GET | /products | List products |
| POST | /products | Create a product |
```

Derive from the contract facts. Helps developers quickly see what the service implements.

### Configuration

Table of environment variables that can override defaults:

```markdown
| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| SUT_PORT | 8080 | Application port |
| SUT_BASE_URL | http://localhost:8080 | Base URL for Specmatic tests |
```

Include when the sample exposes configurable ports, URLs, or broker settings.

Place optional sections between "How It Works" and "Project Structure".

## Tone and Style

- Written for a developer who just found this repo — they should understand WHY in 30 seconds
- Concise, not marketing-heavy — benefits backed by what the sample actually demonstrates
- Use code blocks for commands
- Keep the total README under 120 lines when possible

## Maintain-Mode Update Rules

When updating an existing README during maintain mode:

1. **Preserve user-added sections** — any section not in the required list above should be kept in place
2. **Add missing required sections** — if "Why Specmatic" or "How It Works" doesn't exist, add it in the correct position
3. **Refresh factual content** — update version numbers, commands, prerequisite versions, and contract links to match current state
4. **Don't rewrite working prose** — if a section exists and is accurate, leave it alone
5. **Update Project Structure table** — reflect any file changes from the maintain session
