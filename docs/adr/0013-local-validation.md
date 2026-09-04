# ADR 0013 — Validate Rule Changes Independently

- Status: Accepted
- Date: 2026-09-22

## Context

After a person or agent edits `.rule` files or knowledge cards, format errors should be found without waiting for a task to happen to exercise them. Users also need clear feedback before reviewing a change in Git.

## Decision

Add `--validate --project <directory>` to the CLI. It reads all rules and knowledge cards and reports their counts on success; format errors retain exit code 3 and identify the source. Validation checks format and known boundaries only; it does not claim that rule content is correct. After an update, verify behavior with real task framing and queries. `--validate` is mutually exclusive with `--task`, `--query`, and `--stdin`.

## Consequences

An agent can validate proposed rule changes on a branch before review. Users decide whether to accept them through Git diffs; there is no automatic promotion or silent update.
