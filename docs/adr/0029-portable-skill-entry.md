# ADR 0029 — Make the Skill Entry Point Configurable

Status: Accepted. Supersedes the fixed installation path in ADR 0007.

Public source code may be placed in any directory. The Skill first reads `PROMPTFORGE_HOME`; if still inside the repository, it can locate the root relative to itself. If a separately copied Skill has no setting and cannot find the CLI, ask the user for the checkout location rather than guessing a machine-specific path.

This changes only how the CLI is located. Tasks still use the existing `frame.ps1`, and the target project is a separate `-Project` argument. Contract checks reject hard-coded drive paths; a wrapper smoke test verifies it still works after moving the checkout.
