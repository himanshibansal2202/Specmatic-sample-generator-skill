# Specmatic Sample: Spring Boot Order API (REST/OpenAPI)

## What This Is

This sample demonstrates how Specmatic contract tests a Spring Boot REST API in complete isolation — no running dependencies, no hand-written mocks, no integration environment needed. Specmatic reads the OpenAPI spec and auto-generates test cases that validate your service responses.

## Why Specmatic

- **Auto-generated tests from your API spec** — Specmatic reads your OpenAPI spec and generates test cases automatically. No hand-written test code or mocks to maintain.
- **Intelligent service virtualisation** — Specmatic generates realistic stubs/mocks from the same spec, so consumers can develop in parallel without waiting for providers.
- **Backward compatibility detection** — Specmatic compares spec versions and flags breaking changes before they reach production.
- **Works with your existing OpenAPI spec** — no new DSL to learn.

## Tech

1. Spring Boot 3.4 service written in Java 17
2. Specmatic 2.46.1 (native JUnit 5 integration)
3. JRE 17+

## Run Contract Tests

### Prerequisites

- JDK 17+

### Using the build tool

```bash
./mvnw test
```

First run takes 1-2 minutes as Specmatic clones the contract repository. Subsequent runs are fast (cached in `.specmatic/`).

The [`specmatic.yaml`](specmatic.yaml) file configures which contracts are tested.

### Test Modes

This sample ships with `schemaResiliencyTests: none` for fast, predictable tests. You can increase test coverage by changing the mode in `specmatic.yaml`:

| Mode | What it does |
|------|-------------|
| `none` | Runs tests from named examples only (default) |
| `positiveOnly` | Adds all valid input combinations |
| `all` | Adds negative/boundary tests (expects 400 for invalid inputs) |

To enable, update `specmatic.yaml`:
```yaml
specmatic:
  settings:
    test:
      schemaResiliencyTests: all
```

## How It Works

```
API Spec (OpenAPI) → specmatic.yaml → Specmatic generates requests → Your service responds → Specmatic validates responses against the spec
```

When you run `./mvnw test`, the `ContractTest` class extends `SpecmaticContractTest` which starts the Spring Boot app, reads `specmatic.yaml`, fetches the contract from the configured git repo, and auto-generates test cases for every endpoint and example defined in the spec.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /products | List products (filterable by type) |
| POST | /products | Create a product |
| GET | /products/{id} | Get product by ID |
| PATCH | /products/{id} | Update a product |
| DELETE | /products/{id} | Delete a product |
| PUT | /products/{id}/image | Upload product image |
| GET | /orders | List orders |
| POST | /orders | Create an order |
| GET | /orders/{id} | Get order by ID |
| PATCH | /orders/{id} | Update an order |
| DELETE | /orders/{id} | Cancel an order |

## Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| SUT_PORT | 8080 | Application port |
| SUT_BASE_URL | http://localhost:8080 | Base URL for Specmatic tests |

## Project Structure

| File | Purpose |
|------|---------|
| `specmatic.yaml` | Contract test configuration — points to the API spec |
| `src/main/java/...` | Application source code (controllers, models, services) |
| `src/test/java/.../ContractTest.java` | Contract test adapter that starts the app and runs Specmatic |
| `Dockerfile` | Production container image |
| `.github/workflows/ci.yml` | CI pipeline: test + Docker build |

## For More Info

- [Specmatic Website](https://specmatic.io)
- [Specmatic Documentation](https://docs.specmatic.io)
- [Order API Contract](https://github.com/specmatic/specmatic-order-contracts/blob/main/io/specmatic/examples/store/openapi/api_order_v5.yaml)
