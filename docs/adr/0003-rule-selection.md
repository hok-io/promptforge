# ADR 0003 — Start with Explainable All-Keyword Matching

- Status: Accepted
- Date: 2026-09-22

## Context

The tool must not include every rule in every task, and a program without an LLM must not pretend to understand a sentence's true meaning. The first selection method needs to be predictable and testable by people.

## Decision

All keywords in a rule must appear in the task text for the rule to match. English letters are case-insensitive; Chinese is matched by substring. An empty task matches nothing. Sort results by rule ID, independent of file order. The CLI must show matching rules and sources; it must not add hidden semantic expansion.

## Consequences

False inclusions are easy to explain and diagnose, but synonyms and paraphrases may be missed. An explicit `any` keyword group or local retrieval can be considered later, after recording misses and false matches on real tasks.
