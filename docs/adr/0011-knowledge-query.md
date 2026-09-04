# ADR 0011 — Inspect Retrieval Results Separately First

- Status: Accepted
- Date: 2026-09-22

## Context

BM25 can rank cards, but people need a way to use it. Automatically attaching results to a prompt before review might treat a merely similar ADR as correct context.

## Decision

Add `query.ps1 -Query "..." -Project <directory>`. The Java CLI uses `--query` to search `.promptforge/knowledge` and displays at most three headings, sources, and short excerpts. State clearly when no local data matches. `--query` and `--task` are mutually exclusive. The query command returns evidence only; it does not draw conclusions or modify rules.

## Consequences

Users can inspect sources before deciding whether to add or adjust a rule. An agent may later use this as a read tool. This is not a multi-step agent loop; decide whether one is worthwhile based on actual tasks.
