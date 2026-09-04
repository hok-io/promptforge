# ADR 0014 — Direction for a Local Prompt and Skill Workbench

Status: The implementation approach was settled by [ADR 0019](0019-web-build-and-test-boundary.md); this file preserves the earlier proposal.

## Context

The CLI already provides rule-based frames and local knowledge queries. The new need is to inspect and edit prompt/Skill structure, preserve evaluation evidence, and record human approval. These interactions are inconvenient in a CLI alone.

## Proposal

Add a local Spring Boot MVC Web adapter. Share the ordinary Java core between CLI and Web. `.promptforge` files and Git remain the source of truth; no required LLM or paid service is added.

Imported content starts as a draft. Evaluation and approval are separate, and approval is bound to a content hash. A preview is a candidate for an agent; it does not claim to observe the agent's actual context.

## Costs and Alternatives

This requires learning Maven, Spring injection, controllers, templates, and HTTP testing, as well as handling safe rendering, revision conflicts, and atomic writes. Extending the CLI would be simpler but would not meet the visual editing need. The proposal chose MVC/Thymeleaf to avoid introducing another large frontend framework at the outset.

This does not reverse earlier decisions: the core without Spring remains valid. Current scope is defined in the [behavior specification](../specs/behavior.md).
