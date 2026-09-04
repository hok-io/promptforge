# ADR 0016 — Add Human-Selected Knowledge Cards to a Frame

Status: Accepted. Implemented 2026-09-26.

Evaluation found that queries could locate needed sources, but extra results increased review work and users still had to merge content manually. Add `--knowledge` with comma-separated project-relative sources, such as `.promptforge/knowledge/adr/idempotency.md`.

Select only from cards already loaded by `KnowledgeCardLoader`; never open an arbitrary input path. Allow at most three cards in the requested order. Reject unknown, duplicate, or empty sources; an entirely empty string means select none. Normalize Windows separators to `/`. Filenames containing commas are unsupported by this format.

Include each selected card's full body, heading, and source. Do not silently truncate or automatically use the query top three. Rules and knowledge together remain limited to 6,000 characters; exceeding the budget returns exit code 5 with empty stdout so the user can reduce the selection. Without a knowledge option, preserve existing behavior. The option is available only in task/standard-input modes.

`KnowledgeSelection` selects loaded cards; `PromptComposer` handles rendering and the final budget. Do not change BM25 scores or claim better automatic retrieval or that users need not understand the knowledge. The same command can now produce a complete candidate prompt without manually copying sources and bodies.
