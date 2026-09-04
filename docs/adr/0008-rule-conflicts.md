# ADR 0008 — Allow Rules to Declare Conflicts Explicitly

- Status: Accepted
- Date: 2026-09-22

## Context

Two rules may both match a task and still contradict each other. If both are inserted into a prompt, Codex has to guess. This is also a realistic case for practicing Java domain modeling and collection checks.

## Decision

Rule files may include `conflicts=another-rule-id,...` to declare that two rules cannot appear in the same frame. A one-sided declaration is sufficient; treat it as mutual during checks. Store `conflicts` as an immutable list. Empty IDs, duplicate IDs, and self-conflicts are format errors. If both rules match, the compiler reports both IDs and sources and emits no contradictory frame. Unmatched rules do not cause conflicts.

## Consequences

People can review conflict relationships in Git diffs. There is no automatic priority or `supersedes` behavior; avoid silent choices. Add an ADR and cycle tests if a `requires`/`supersedes` graph is needed later.
