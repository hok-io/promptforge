# ADR 0018 — Bound Loader Data Size and Paths

Status: Accepted. Observed in practice on 2026-09-26.

The knowledge loader accepts at most 100 Markdown cards and 256 KiB total of their raw UTF-8 bytes. Each rule and knowledge file remains limited to 8 KiB. The total is calculated from file-system-reported sizes. Exceeding any limit stops loading with a format error. Apply limits before loading all content so an unbounded collection cannot reach retrieval.

Both loaders share `ProjectPaths`. Check every path component from the project root to the file-system root, reject detectable symbolic links, and verify on access that each configured directory and content file's real path stays under the project's real path. This covers `.promptforge`, rules/knowledge directories, nested card directories, and candidate files. Java NIO's classification of Windows junctions depends on the file system's `isSymbolicLink` and `toRealPath` behavior; even if a junction is not identified as a symbolic link, a real-path escape is rejected. If a junction occurs before the selected project root, the resolved root is the boundary for that load call.

Keep the validation order for required rule fields fixed as `id`, `keywords`, `instruction`, so repeated validation of the same invalid file reports the same first field.

These limits define loading boundaries, not semantic validation; users remain responsible for card content. The loader does not partially load data after a total limit is exceeded.
