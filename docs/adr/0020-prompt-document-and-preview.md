# ADR 0020 — Structured Prompts and Plain-Java Preview

Represent `PromptDocument` as an immutable Java record with ordered custom blocks, variables, and explicit selection. Frame mode preserves a short task; template mode appends non-empty blocks in order, with no required role. Variables use a single literal substitution pass. Missing values return a clear 422 error; do not execute or recursively expand substitutions.

Plain-Java `PromptPreviewService` calls the shared `FrameService`; CLI and Web both use the same core without a shell. Tests first verify ordering, missing values, literal substitution, and the difference between automatic and empty selection, then verify parity with the CLI using the same data. The domain does not know about HTTP; `WorkbenchException` stores application error categories, which the HTTP adapter maps to status codes.

The trade-off is that people must choose suitable blocks during structured editing; there is no semantic completion. A preview is still a candidate prompt and does not represent the agent's actual context.
