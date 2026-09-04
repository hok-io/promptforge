# ADR 0031 — Make the Web Home Page Task-First

- Status: Accepted
- Date: 2026-09-28 (date of the actual change)

## Context

The original home page showed knowledge cards, project rules, prompt sections, JSON variables, and Skill reviews at the same time. First-time users could not tell where to begin and could mistake the Web app for a tool that manages and verifies the entire foundation. In reality, the current Web app can read rule and knowledge files, assemble prompts, and manage Skill drafts. It cannot edit project rules or ADRs, or prove that rules work in real tasks.

## Decision

Make `/` a plain-language starting point: describe the flow as “state the task → let the Codex Skill clarify gaps → confirm an actionable task.” Link directly to the guided workflow at `/guide.html`. Keep the existing features available at `/workbench.html` and label it as the advanced workbench. The home page must state what the app can and cannot do, so changing labels does not imply that functionality is complete.

Knowledge cards are local document search results; project rules are fixed requirements; clarification preferences configure the conversation flow. Foundation editing and real-use verification must have their own operations and acceptance criteria instead of being mixed into the prompt assembly form.

## Verification and Follow-up

Pass the non-browser Maven tests and `scripts/check-skill.ps1`, then start the local service and check `/`, `/guide.html`, and `/workbench.html`. These checks confirm that the new entry point works and the existing features remain reachable. They do **not** mean foundation management or outcome tracking is complete. Before the next feature batch, define which rules and decisions users can edit, and how versions and evidence are handled; then implement the Java API, tests, and Web form.
