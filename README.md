# Nova Architecture Rules

JUnit 5 abstract test classes that enforce the architectural styles
supported by the Nova Platform meta-framework. Built on top of
[ArchUnit](https://www.archunit.org/), the library turns architectural
drift into a failed CI run.

## What's inside

| Style | Abstract test class | Package layout |
|---|---|---|
| **Layered** | `LayeredArchitectureTest` | `controller..`, `service..`, `repository..`, `entity..`, `dto..` |
| **Clean** *(phase 2)* | `CleanArchitectureTest` | `domain..`, `application..`, `infrastructure..`, `api..` |
| **Hexagonal** *(phase 2)* | `HexagonalArchitectureTest` | `domain..`, `application..`, `adapters..` |

## Install

```xml
<dependency>
    <groupId>pe.edu.nova.java.libs</groupId>
    <artifactId>nova-architecture-rules</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

## Use

Subclass the abstract test that matches your application's style and
declare the base package to scan:

```java
package com.acme.my;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import pe.edu.nova.java.archunit.LayeredArchitectureTest;

@AnalyzeClasses(
    packages = "com.acme.my",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest extends LayeredArchitectureTest {

    @Override
    protected String basePackage() {
        return "com.acme.my";
    }
}
```

Run `mvn test` — ArchUnit reports every violation as a JUnit failure
with the exact offending class.

## Layered rules enforced

- `controller..` depends on `service..`, `entity..`, `dto..`, `shared..` — never on `repository..`.
- `service..` depends on `repository..`, `entity..`, `shared..` — never on `controller..`.
- `repository..` depends on `entity..`, `shared..` — never on anything above.
- `entity..` is a pure data layer — no imports from other layers.
- `dto..` never imports `entity..` (decoupling transport from persistence).
- No service method declares `throws Exception`.
- No `@Autowired` field injection on services.

## License

Apache 2.0 — see [LICENSE](LICENSE).
## Maintainer

Run on each push to main (and every PR). The pipeline is reused from
hincho/nova-devops via reusable workflows.
