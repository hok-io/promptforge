# ADR 0023 — Skill Evaluation Evidence

Skill evaluations are immutable records attached to a bundle revision. A `TEST` record must reference at least one existing workspace evidence file and store its SHA-256. A `MODEL_REVIEW` must include at least one textual finding. Neither record runs scripts, computes a score, nor changes human review state. Evaluations may reference a current or older valid SHA-256; on read, compare it with the current bundle hash and mark stale so old evidence is not mistaken for current evidence.

Store records at `.promptforge/evaluations/<skillId>/<subjectHash>/<evalId>.json`. IDs are lowercase slugs and files cannot be overwritten. Use ISO-8601 timestamps, with at most 20 findings of up to 2,000 characters each. `WorkspaceFiles` keeps evidence paths inside the registered project and limits text-file size. Metadata and evaluation records do not participate in the Skill revision.

This lets the workbench retain test results and model-review rationale while keeping approval an explicit human action. Changing a reference or `SKILL.md` makes related evaluations appear stale.
