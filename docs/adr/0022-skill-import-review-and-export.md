# ADR 0022 — Skill Import and Version State

Import only from a registered project `.promptforge/inbox/<name>`; do not accept arbitrary paths. Preserve the original `SKILL.md`, text references/scripts, and unknown front matter. Store workbench metadata in a sidecar. The first version is a UTF-8 text bundle with at most 128 entries and 256 KiB. Do not run included scripts or handle binary assets.

The revision is a hash of the entire Skill text bundle, including references, not only `SKILL.md`. Human events in the Imported → Reviewed → Approved → Active flow are bound to this hash. Any content change invalidates existing approval and active state. Model evaluation never approves a Skill automatically. Active means selected by this workbench; it does not mean installed in an agent.

Show a diff before saving, then check the bundle revision. Export only an approved revision to `exports/<name>` inside the same project, creating a new directory atomically and never overwriting. Do not install silently. Stage imports and exports before renaming. Keep metadata and raw Skill data in separate files; derive state from hashes so changed content cannot retain old approval.

Tests first verify preservation of unknown fields, approval ordering, stale revisions, invalidation after reference changes, and no-overwrite export. Test the same flow over HTTP and in the UI.
