# ADR 0012 — Package a Directly Executable JAR

- Status: Accepted
- Date: 2026-09-22

## Context

`frame.ps1` and `query.ps1` recompile on every run, making routine CLI use inconvenient. The user also asked whether Java needs to be packaged as `.o` first. The project needs a portable, executable Java artifact.

## Decision

Use the JDK's `javac` and `jar` to build `build/PromptForge.jar` with a manifest naming `promptforge.Main`. Run tests locally and in CI before `scripts/package.ps1`, then smoke-test a Chinese example with `java -jar` and **no encoding flags**. JAR stdout and stderr use UTF-8. The JAR is a build artifact and is not committed; source, ADRs, tests, and packaging scripts are committed. Do not introduce Maven while there are no external Java dependencies.

## Consequences

After packaging, use requires only a compatible Java runtime; Java source changes require rebuilding. If a library such as Lucene is introduced, write an ADR to move to Maven or another build tool instead of manually assembling a classpath.
