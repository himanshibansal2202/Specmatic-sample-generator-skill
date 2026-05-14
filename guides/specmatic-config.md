# Specmatic Configuration & Contract Test Patterns

## specmatic.yaml

Place this at the project root. Keep the documented service default stable, but allow test runs to override the base URL when a local port is already occupied.

**Inline format (works across all languages):**
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
            - io/specmatic/examples/store/openapi/api_order_v3.yaml
    runOptions:
      openapi:
        type: test
        baseUrl: "{SUT_BASE_URL:http://localhost:8080}"
```

### BFF With Backend Mock Dependency

BFF samples use the BFF OpenAPI contract as the system under test and the
Backend OpenAPI contract as a mock dependency. Keep service and mock URLs
configurable so tests can avoid occupied ports.

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
        baseUrl: "{SUT_BASE_URL:http://localhost:8080}"
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
            baseUrl: "{BACKEND_MOCK_BASE_URL:http://localhost:8090}"
```

## Contract Source Of Truth

The OpenAPI/contract file referenced by `specmatic.yaml` is the behavioral source of truth. The local `guides/` and `test-data/` markdown files help generation, but they must not override the executable contract.

Before finalizing generated behavior, use the executable contract or Specmatic report output to confirm:

- HTTP methods and paths
- Status codes
- Request and response content types
- Required and forbidden response fields
- Example IDs and request/response examples
- Provider stubs or dependency contracts for consumer samples

If local guide/test-data notes and the executable contract disagree, implement the executable contract because Specmatic will verify that behavior.

## Contract Test Adapter Patterns

The contract test adapter starts your app, tells Specmatic to test it, then stops the app. It must surface startup/listen errors and Specmatic failures clearly.

Adapter requirements for every language:

- Use configurable host, port, and base URL values for test runs.
- Default to the documented service port for normal local runs.
- Fail fast if the app cannot bind its test port.
- Always stop the app/stubs in teardown, even when Specmatic fails.
- Assert the Specmatic result has zero failures instead of only printing results.

### Node.js / JavaScript (Jest + specmatic npm package)

**Required dependencies:** `specmatic` (devDep), `jest` (devDep), `cross-env` (devDep), `axios` (peer dep of specmatic)

**jest.config.json:**
```json
{
  "testMatch": ["**/test/**/*.test.mjs"],
  "transform": {}
}
```

**Test script in package.json:**
```json
"test": "cross-env NODE_OPTIONS=--experimental-vm-modules NODE_NO_WARNINGS=1 jest --detectOpenHandles"
```

**test/contract/contract.test.mjs:**
```javascript
import { test } from "@jest/globals";
import specmatic from "specmatic";
import http from "node:http";

const PORT = Number(process.env.SUT_PORT ?? 18080);
process.env.SUT_BASE_URL = process.env.SUT_BASE_URL ?? `http://127.0.0.1:${PORT}`;

const app = (await import("../../src/app.js")).default || require("../../src/app.js");
const server = http.createServer(app);
await new Promise((resolve) => server.listen(PORT, "0.0.0.0", resolve));

try {
  const result = await specmatic.testWithApiCoverage(app, "127.0.0.1", PORT);
  if (!result || result.failure > 0) {
    throw new Error(`Specmatic contract tests failed: ${JSON.stringify(result)}`);
  }
} finally {
  await new Promise((resolve) => server.close(resolve));
}
```

### Java / Kotlin (JUnit 5 + specmatic junit5-support)

**Dependency (pom.xml):**
```xml
<dependency>
  <groupId>io.specmatic</groupId>
  <artifactId>junit5-support</artifactId>
  <version>LATEST</version>
  <scope>test</scope>
</dependency>
```

**ContractTest.java:**
```java
package com.example;

import io.specmatic.test.SpecmaticContractTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class ContractTest implements SpecmaticContractTest {
    private static ConfigurableApplicationContext context;

    @BeforeAll
    public static void setUp() {
        context = SpringApplication.run(Application.class);
    }

    @AfterAll
    public static void tearDown() {
        context.close();
    }
}
```

### Python (pytest + specmatic pip package)

**Dependency:** `pip install specmatic`

**test/test_contract.py:**
```python
import os
from specmatic.core.specmatic import Specmatic
from your_app import app

PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))

class TestContract:
    pass

Specmatic() \
    .with_project_root(PROJECT_ROOT) \
    .with_wsgi_app(app, "127.0.0.1", 8080) \
    .test(TestContract) \
    .run()
```

## How Specmatic Tests Work

1. Specmatic reads `specmatic.yaml`
2. Clones the central contract repo
3. Parses the OpenAPI spec
4. Sends example requests to your app at `baseUrl`
5. Validates response status and schema
6. Reports pass/fail

When tests fail, read the generated report files before changing code. Prefer the JUnit XML or Specmatic report output because it identifies the exact status, content type, schema, and example mismatches that must be fixed.
