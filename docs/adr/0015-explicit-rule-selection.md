# ADR 0015 — Let People Confirm and Correct Rule Selection

Status: Accepted. Implemented 2026-09-26.

Evaluation found missed synonyms, negations, and false matches on document tasks. Add confirmable CLI selection rather than pretending keyword matching understands a task.

Add `--list-rules` to show IDs, keywords, instructions, and sources. `--rules` selects an explicit comma-separated list of IDs and replaces automatic matching; an empty string means select none. `--exclude` excludes IDs. Unknown or duplicate IDs, and an ID that is both selected and excluded, produce input errors rather than being ignored. Select at most 20 rules and sort them deterministically by ID.

Without the new options, preserve existing behavior. Selected rules still undergo conflict and frame-size checks. List mode cannot be combined with task, query, validation, or selection options. Ordinary Java `RuleSelection` owns selection; `Main` only connects the CLI to the compiler.

Adding synonyms or special cases for negation could introduce new false matches and overfit the test set. Explicit selection avoids retyping sources but still requires people to know which rules apply; it does not claim to improve automatic accuracy.

The PowerShell frame wrapper exposes `-Rules`, `-Exclude`, and `-Knowledge`, forwarding them only when supplied so that omission differs from an explicitly empty selection. Cross-platform smoke tests first revealed that PowerShell native calls drop empty strings. Therefore, `--no-rules` represents `-Rules ''`; empty `Exclude`/`Knowledge` are omitted because they contain no entries. Combining `--no-rules` with `--rules` is an input error. CI runs the same wrapper smoke command.
