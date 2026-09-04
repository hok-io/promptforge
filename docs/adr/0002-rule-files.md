# ADR 0002 — One Readable Text File per Rule

- Status: Accepted
- Date: 2026-09-22

## Context

Users need to update project rules in Git and trace each output to its source file. Adding a YAML parser, schema, and index in the first version would increase installation and comprehension costs.

## Decision

Store UTF-8 text files under `.promptforge/rules/*.rule`, with one `key=value` pair per line. Blank lines and `#` comments are allowed. `id`, `keywords`, and `instruction` are required; keyword matching semantics will be defined in a later feature. Unknown fields, duplicate fields, empty values, and duplicate IDs produce clear errors rather than guesses or silent omissions. Derive `source` from the file's relative path for traceability.

## Consequences

People can edit files with an ordinary editor and review them easily in Git diffs. An instruction is limited to one line for now; support for multiline values requires a separate ADR. Rule files are data sources, not executable instructions.
