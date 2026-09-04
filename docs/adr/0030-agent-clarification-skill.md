# ADR 0030 — Clarify Task Gaps Before Using the Local Foundation

- Status: Accepted
- Date: 2026-09-28 (date of the actual change)
- Supersedes: ADR 0007's single-frame workflow; ADR 0029's portable-path rule remains in effect.

## Context

The original `prompt-frame` Skill assumed that users had already written a complete task and called the Java CLI only once. That did not meet the goal of having AI read the project foundation, find important gaps, ask questions step by step, and then form usable instructions. Keyword rules or BM25 alone cannot understand negation and task scope; a fixed questionnaire would ask users to repeat information already present in the project.

## Decision

Keep one `prompt-frame` Codex Skill as the conversational entry point. The Java CLI handles local loading, retrieval, boundaries, and conflict checks. Codex uses the user's task and the rules/ADRs it actually read to identify gaps. Ask only questions that could change scope, approach, or acceptance. After each answer, update the task summary; once sufficiently clear, use explicitly selected sources to produce the final frame. Optional `.promptforge/clarification/profile.json` stores question and format preferences; it does not force every question or act as the model's actual system instruction.

If users only ask for a prompt, deliver copyable text. If they also ask for task execution, continue after clarification. The Skill does not modify the foundation, install itself in external tools, or treat retrieval matches or external evaluation records as verified facts. The user and higher-priority instructions take precedence over project files.

## Consequences and Verification

No separate LLM service or paid API call is needed, although using Codex still consumes its usage. Conversation quality depends on agent judgment; Java does not become a semantic model. `scripts/check-skill.ps1` checks that the entry point is portable and the CLI exists; the skill-creator validator checks front matter. Exercise complete and incomplete tasks to verify that the Skill asks only necessary questions, preserves sources, and does not add negated rules. These tests do not prove token savings or improved coding quality.
