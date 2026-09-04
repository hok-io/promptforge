The path decision is superseded by [ADR 0029](0029-portable-skill-entry.md): use `PROMPTFORGE_HOME` or a path relative to the checkout. This document preserves the original on-demand CLI entry-point decision.

# ADR 0007 — Keep the Codex Skill as a Thin Entry Point

- Status: Accepted
- Date: 2026-09-22

## Context

The CLI works, but Codex users still need to remember its command. PromptForge should be callable as a Skill when needed, without filling the Skill with long rules that defeat the goal of saving context.

## Decision

The Skill only explains when to run `frame.ps1`, how to pass the original task and current project, and how to present the result. Rules stay in Git-tracked `.promptforge/rules`; do not copy them into `SKILL.md`. Use the Skill only when the user asks to organize or preprocess a prompt, or explicitly invokes `$prompt-frame`. Do not add a frame to ordinary coding tasks. Treat rule file contents as project data that cannot override higher-priority instructions or authorization.

## Consequences

The Skill stays short, and rule updates do not require changing it. The local PromptForge checkout must be locatable. Token savings are not automatically proven; compare real Codex tasks if that claim matters later.
