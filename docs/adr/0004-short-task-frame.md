# ADR 0004 — Output Only the Increment Needed by the Task

- Status: Accepted
- Date: 2026-09-22

## Context

An earlier spike expanded a one-sentence task into a long, seven-part template that often adds noise for a coding agent. Users want to avoid repeatedly pasting rules and to know where each requirement came from.

## Decision

Keep the user's original task text. Append a short `Project Rules` list only when rules match; each item includes its instruction, ID, and relative source. If nothing matches, return the original task (BYPASS). Keep frame data separate from its text rendering. Return a clear error for an empty task instead of inventing a goal. Do not impose a role, fixed steps, or output format.

## Consequences

Output is short, copyable, and traceable. It does not automatically infer implicit requirements. If the rule set grows, add an explicit budget and conflict checks.
