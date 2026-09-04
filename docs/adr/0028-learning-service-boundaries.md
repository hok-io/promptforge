# ADR 0028 — Separate Application Service Responsibilities

`SkillService` handled approval, file copying, and evaluation JSON together, coupling storage-format changes to business-flow changes. Split it into `SkillService` (operation entry point), `SkillBundleStore` (bundle I/O and revisions), `SkillTransfer` (import/export), and `SkillEvaluationService` (evaluation validation and storage). Reuse `WorkspaceFiles` and `RevisionFileStore`; do not add an interface with one implementation and no replacement need.

`PromptService` provides load/list/diff/save without exposing the repository to HTTP. `ProjectContextService` provides context and search. Controllers retain HTTP routes and conversion to display DTOs. Centralize object assembly in Spring configuration.

Keep the behavior contracts in `SkillWorkflowTest`, `EvaluationWorkflowTest`, `PromptWorkflowTest`, and `WebContractTest`; add module dependency and service-entry architecture checks. Run the full test suite and browser flows after the refactor. The refactor adds no features and does not promise cross-process file transactions.
