# BFF (Backend-for-Frontend) Role

## Architecture

The BFF sits between the Frontend and the Backend. It:
- **Provides** its own API (tested by Specmatic contract tests)
- **Consumes** the Backend API (stubbed by Specmatic during tests)

```
Frontend ──▶ BFF (your app) ──▶ Backend API (STUBBED by Specmatic)
                │                        │
                │                        └── Specmatic mock on stub port (e.g., 8090)
                └── Specmatic tests your BFF on app port (e.g., 8080)
```

## BFF Spec: `product_search_bff_v3.yaml`

The BFF provides these endpoints:

| Method | Path | Request | Response | Status |
|--------|------|---------|----------|--------|
| POST | /products | `{name, type: enum, inventory: int}` | `{id: int}` | 201 |
| GET | /findAvailableProducts | Query: `type`; Header: `pageSize` (required) | `[Product]` | 200 |
| POST | /ordres | `{productid: int, count: int}` | `{id: int}` | 201 |

**Note:** The BFF spec uses `/ordres` (typo in the actual spec). Your route MUST match the spec literally.

## How the BFF Works

The BFF does NOT have its own database. It calls the Backend API:
- `POST /products` → calls Backend's `POST /products` → returns the result
- `GET /findAvailableProducts` → calls Backend's `GET /products?type=X` → returns the result
- `POST /ordres` → calls Backend's `POST /orders` → returns the result

The Backend API URL comes from an environment variable (e.g., `STUB_URL`).

## specmatic.yaml for BFF

The key difference from Backend: the `dependencies` section tells Specmatic to mock the Backend API.

```yaml
version: 3
systemUnderTest:
  service:
    definitions:
      - definition:
          source:
            git:
              url: https://github.com/specmatic/specmatic-order-contracts.git
          specs:
            - io/specmatic/examples/store/openapi/product_search_bff_v3.yaml
    runOptions:
      openapi:
        type: test
        baseUrl: http://localhost:8080
dependencies:
  services:
    - service:
        definitions:
          - definition:
              source:
                git:
                  url: https://github.com/specmatic/specmatic-order-contracts.git
              specs:
                - io/specmatic/examples/store/openapi/api_order_v3.yaml
        runOptions:
          openapi:
            type: mock
            baseUrl: http://localhost:8090
```

## Contract Test Adapter for BFF (Node.js)

**Important:** You must start the Backend mock (stub) in your test setup. Use `specmatic.startStub()`.

```javascript
import { test } from "@jest/globals";
import specmatic from "specmatic";
import http from "node:http";

process.env.STUB_URL = "http://localhost:8090";

const PORT = 8080;
const app = (await import("../../src/app.js")).default;
const server = http.createServer(app);
await new Promise((resolve) => server.listen(PORT, "0.0.0.0", resolve));

const stub = await specmatic.startStub("localhost", 8090);

try {
  await specmatic.testWithApiCoverage(app);
} finally {
  await specmatic.stopStub(stub);
  await new Promise((resolve) => server.close(resolve));
}

specmatic.showTestResults(test);
```

## Key Architectural Patterns

1. **Start the stub in your test.** The Specmatic test command does not auto-start mocks from the `dependencies` section. You must start it yourself.

2. **Error handling is mandatory.** Every BFF route must catch errors from the backend and return a proper error response. Without try/catch, Express hangs and Specmatic times out:
```javascript
app.post("/products", async (req, res) => {
  try {
    const result = await createProduct(req.body);
    res.status(201).json(result);
  } catch (e) {
    const status = e.response?.status || 503;
    res.status(status).json(e.response?.data || { error: e.message });
  }
});
```

3. **Handle spec mismatches gracefully.** The BFF spec may accept values that the Backend spec rejects (e.g., BFF accepts any string for `type`, Backend has an enum). When the backend returns an error for an invalid value, handle it gracefully — either return the error or retry without the problematic parameter.

## Seed Data

The BFF has NO local data store. The Specmatic mock handles Backend responses automatically based on the Backend's OpenAPI spec examples. No seed data needed in the BFF.
