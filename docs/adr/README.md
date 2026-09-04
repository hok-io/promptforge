# Architecture Decision Records

ADRs explain why a decision was made; the current behavior is defined by the [specification](../specs/behavior.md). Newer ADRs may revise historical decisions; do not treat early proposals as the current implementation.

| Topic | Decisions |
|---|---|
| CLI foundation and rules | 0001–0006: Java core, file format, selection, and boundaries |
| Skill entry and conflicts | 0007–0008: on-demand CLI entry point and rule conflicts |
| Retrieval and distribution | 0009–0013: knowledge cards, BM25, queries, JAR, and validation |
| Human confirmation | 0015–0018: rule selection, knowledge inclusion, UTF-8, and loader limits |
| Web direction | [0014](0014-local-workbench-direction.md) is an early proposal; [0019](0019-web-build-and-test-boundary.md) defines the implementation approach |
| Prompt storage | [0020](0020-prompt-document-and-preview.md), [0021](0021-revision-safe-file-storage.md) |
| Skills and evidence | [0022](0022-skill-import-review-and-export.md), [0023](0023-evaluation-evidence.md) |
| Local HTTP and UI | [0024](0024-local-http-and-csrf.md), [0025](0025-clean-build-classes.md), [0026](0026-local-workbench-ui.md) |
| Application boundaries | [0027](0027-application-errors.md), [0028](0028-learning-service-boundaries.md) |
| Portable Skill entry | [0029](0029-portable-skill-entry.md) |
| Task clarification | [0030](0030-agent-clarification-skill.md) |
| Task-first Web home | [0031](0031-web-task-first-home.md) |
