# ADR 0001 — Start with a Verifiable Java Core Using the Local JDK

- Status: Accepted
- Date: 2026-09-22

## Context

The local machine has JDK 15 but no Maven. The project aims to practice Java and first provide a usable local CLI. Waiting for a framework and build tool would prevent early verification of business behavior.

## Decision

The first phase uses only standard Java 15 syntax. `scripts/test.ps1` compiles the application and tests, while GitHub Actions runs the same script on Windows and Ubuntu with JDK 21. For each feature, first commit an ADR and a failing test, then commit the smallest implementation and its acceptance record. Introducing Maven/JUnit later requires another ADR and preservation of the existing behavior tests.

## Consequences

No extra local installation or paid service is required; the project can practice classes, interfaces, collections, NIO, exceptions, and algorithms directly. This lightweight test runner lacks JUnit's reporting features. CI only runs after connecting GitHub and pushing; for now, the same local command is the verification path.
