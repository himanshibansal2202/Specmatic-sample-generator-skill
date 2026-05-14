# BFF Generation Guide

The BFF sits between the Frontend and the Backend. It provides the API in
`contracts/bff.md` and consumes the Backend APIs in `contracts/backend.md`.

```text
Frontend -> BFF -> Backend API (stubbed by Specmatic during tests)
```

## Required Pieces

- `specmatic.yaml` with the BFF contract as the system under test.
- A Specmatic dependency mock for the Backend API.
- Source code with no local database.
- Backend base URL from configuration such as `STUB_URL`.
- Contract test adapter that starts the Backend mock before testing the BFF.

## specmatic.yaml Shape

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

## Implementation Notes

- Start the Specmatic Backend mock in the test setup.
- Forward `Idempotency-Key`, auth headers, and `pageSize` when present.
- Catch backend errors and return their status and response body.
- BFF samples have no seed data.
