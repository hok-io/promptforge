# ADR 0005 — Get a Task Frame with One Local Command

- Status: Accepted
- Date: 2026-09-22

## Context

The Java core can compile a task frame, but users should not have to write Java for every task. It needs a stable entry point callable from PowerShell and other tools.

## Decision

`frame.ps1 -Task "..." -Project <directory>` compiles and runs the Java CLI. Java accepts `--project <directory> --task <text>` or `--stdin`. On success, stdout contains only the copyable frame; errors go to stderr with a nonzero exit code. Invalid arguments or an empty task return 2, malformed rules return 3, file I/O errors return 4, an oversized frame returns 5, and conflicting matched rules return 6. If `--project` is omitted, use the current directory. Do not start an HTTP server or depend on Spring Boot.

## Consequences

The CLI works locally on Windows and can be called by a Codex hook or another program. It recompiles on every run, so startup is slower. Add a JAR, Maven, and caching only if frequent use justifies them.
