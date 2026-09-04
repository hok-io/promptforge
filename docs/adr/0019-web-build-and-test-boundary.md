# ADR 0019 — Web Build and Test Boundaries

Status: Accepted. The Web adapter is implemented and the CLI shares its core.

Use Java 21, Spring Boot 3.5.16, and Maven 3.9.11. Keep the core in plain Java with the original CLI. The workbench contains ordinary Java domain/application services and file adapters. The Web layer is limited to Spring MVC, HTTP, CSRF, and local assets. There is no paid model, database, or remote agent.

To keep build and runtime dependencies small, use Spring MVC with local plain HTML/CSS/JavaScript. This replaces the earlier Thymeleaf proposal and does not introduce React. The UI calls Java services through JSON, keeping Controller → Service → Repository visible. Render all content as text.

For each feature, add a JUnit contract, temporary-directory file integration tests, HTTP MockMvc/real-port tests, and finally browser interaction and offline-start checks. Existing main-method tests are first connected through JUnit dynamic tests; do not claim they have all been rewritten as fine-grained JUnit tests. Convert them class by class. Do not claim remote CI passed unless it ran.

Sources: [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html) and [Apache Maven Wrapper](https://maven.apache.org/tools/wrapper/). Download JDK/Maven under the project `.tools` directory and verify official checksums; do not modify global system settings.
