# ADR 0006 — Bound Rule File Access and Size

- Status: Accepted
- Date: 2026-09-22

## Context

The CLI reads rules from a selected project. A `.rule` symlink pointing outside the project or an oversized rule file could add unexpected content to a task frame or waste context.

## Decision

Reject symlink rule directories and files. Limit each rule file to 8 KiB and each instruction to 500 characters. Exceeding a limit is an error; never silently truncate. Limit task input to 4,000 characters and the final frame to 6,000 characters. These are conservative character/byte limits, **not exact token counts**.

## Consequences

Output bounds are easier to predict, and trusted sources remain text files managed inside the project. This cannot stop someone authorized to edit rules from writing a malicious instruction; users must still review rule diffs. For long ADRs, retrieve small evidence excerpts instead of using the whole document as an instruction.
