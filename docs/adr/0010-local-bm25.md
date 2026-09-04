# ADR 0010 — Rank Knowledge Cards with Local BM25

- Status: Accepted
- Date: 2026-09-22

## Context

ADR and policy cards can be read, but browsing them individually does not scale. The project needs reproducible relevance ranking with no LLM, embeddings, or API tokens, and an opportunity to practice Java collections and search algorithms.

## Decision

Implement `Bm25Index` independently in the new project, based on the hand-written BM25 concept from the earlier spike. Lowercase English terms and split consecutive Chinese text into bigrams. Index headings and body together, with a modest boost for heading matches. Return only cards with scores above zero, ordered by score descending and source path ascending for ties. Retrieval provides citations but does not generate answers or automatically add results to a task frame in the first version.

## Consequences

Retrieval is testable locally without model costs. It is lexical retrieval, so synonyms and semantic paraphrases may be missed. The local knowledge base is small enough for now; consider a Lucene adapter only if the data grows, and do not add a dependency preemptively.
