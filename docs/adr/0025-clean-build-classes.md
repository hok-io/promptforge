# ADR 0025 — Rebuild Class Directories Before Every Compilation

Status: Accepted. Observed in practice on 2026-09-26.

The CLI, tests, and executable JAR all call `scripts/prepare-classes.ps1` first. It clears each output directory before compiling so stale `.class` files cannot leave deleted or renamed types in the result. Set the test JVM's `java.io.tmpdir` to `build/test-tmp` so temporary test files do not spread across the system temp directory.

The cleaner accepts only output directories below the absolute `build` root. Before recursive deletion, inspect each level of the output path and its ancestors for the ReparsePoint attribute. Reject symbolic links, junctions, or other detectable reparse points. After validation, remove the old directory with `LiteralPath` and create an empty directory for `javac`.

`prepare-classes-smoke.ps1` places stale markers in `classes` and `package-classes` to prove the shared build cleanup removes them. It also replaces an output directory with a junction (Windows) or symbolic link (other platforms) to another build temp directory, then verifies the cleaner rejects it and preserves an external sentinel. Smoke cleanup removes only the link itself; before recursive cleanup, it verifies the target remains beneath the build root. These checks reduce accidental deletion risk but do not remove the file-system race window between checking and deleting.
