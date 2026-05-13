# Specmatic Configuration & Contract Test Patterns

## specmatic.yaml

Place this at the project root. Only change the `baseUrl` port to match your app's port.

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
        baseUrl: http://localhost:8080
```

## Contract Test Adapter Patterns

The contract test adapter starts your app, tells Specmatic to test it, then stops the app. It's ~5-10 lines per language.

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

const PORT = 8080;

const app = (await import("../../src/app.js")).default || require("../../src/app.js");
const server = http.createServer(app);
await new Promise((resolve) => server.listen(PORT, "0.0.0.0", resolve));

await specmatic.testWithApiCoverage(app);
specmatic.showTestResults(test);

await new Promise((resolve) => server.close(resolve));
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
