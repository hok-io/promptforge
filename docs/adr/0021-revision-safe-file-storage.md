# ADR 0021 — Revision-Safe File Storage

Data lives in project `.promptforge`; Git remains under human control. Use a content SHA-256 as the revision and `new` when creating a file. Compare revisions before saving and return 409 on mismatch. Check size first, write to a temporary file in the same directory, then use an atomic move. If atomic moves are unsupported, reject the save instead of risking a partial overwrite.

`WorkspaceFiles` rejects absolute and escaping paths, symbolic links, and resolved paths outside the project. `RevisionFileStore` synchronizes access to each file within one process to avoid overwrites between concurrent requests. If an external tool changes a file, requests with the old revision are rejected. This does not claim to prevent a malicious, non-cooperating process from writing in the brief check/rename window.

Tests cover Chinese content, external edits, stale revisions, path escape, and preservation of the old file after an oversized write. A pre-save diff is only a candidate; the final PUT checks the revision again.
