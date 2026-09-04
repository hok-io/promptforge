# ADR 0009 — Read ADRs and Policies as Citable Local Cards

- Status: Accepted
- Date: 2026-09-22

## Context

Rule files are suited to short instructions, while full ADRs, agent behavior, and policies need local retrieval with sources. Adding entire documents to a task frame would be too long and obscure relevance.

## Decision

Store UTF-8 Markdown under `.promptforge/knowledge/` in subdirectories such as `adr/`, `behavior/`, `policy/`, and `practice/`. The first non-empty line of each file must be a `# Heading`; the rest is its body. The first version treats each file as one card, caps each at 8 KiB, and rejects symlink files. Results record relative sources. The loader reads data; it does not execute Markdown as instructions. Retrieval and whether to attach results to a frame are separate future decisions.

## Consequences

Git diffs remain reviewable and sources are clear. Long ADRs must be split into cards or handled with chunking later. Front matter and version fields are not supported yet, avoiding premature format complexity.
