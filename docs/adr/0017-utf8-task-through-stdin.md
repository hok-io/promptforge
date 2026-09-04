# ADR 0017 — Pass Task Text Through UTF-8 Standard Input

Status: Accepted. Observed in practice on 2026-09-26.

A retest of the real F05 workflow found that Windows' native command-line handling on the local Java 15 machine changed the Chinese character `进` to `?`. Setting `file.encoding` or `sun.jnu.encoding` did not preserve it. The text was already corrupted before Java received the argument, which a unit test of `Main.run` could not reproduce.

Change `frame.ps1` to use the existing `--stdin` option and send task text as UTF-8; selection IDs and sources remain arguments. Save and restore PowerShell's `OutputEncoding`. The stdin contract removes trailing whitespace and does not preserve it, while preserving the body. Smoke tests include characters outside the local code page and emoji.

The task mode of the use-case runner uses the same UTF-8 stdin path to verify the real workflow. Unicode arguments passed directly to Java 15 with `--task` or `--query` may still be limited by the local code page; do not claim that UTF-8 `PrintStream` fixes input corruption. This change fixes only the task wrapper. Document the query limitation in the README rather than adding a new query mode in this patch.
