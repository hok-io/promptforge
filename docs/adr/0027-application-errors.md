# ADR 0027 — Represent Failures with Application Error Types

Workbench services use `FailureKind` to represent failure reasons and do not carry HTTP status codes in the application layer. Categories include invalid input, missing resources, revision conflicts, and storage failures. The Web adapter maps these categories to HTTP statuses, keeping the workbench service separate from the transport protocol.

Preserve the existing HTTP contract: `INVALID_INPUT` maps to 422, `NOT_FOUND` to 404, `CONFLICT` to 409, and `STORAGE_FAILURE` to 500. Adapter tests verify the mapping from every error type to its response status.

Date: 2026-09-24
