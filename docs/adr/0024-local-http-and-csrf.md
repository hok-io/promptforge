# ADR 0024 — Local Spring MVC Interface

Bind the Web service to `127.0.0.1` and use registered project IDs. Controllers call ordinary Java services. Keep Spring Security CSRF enabled; expose a token through `GET /api/csrf`, and require mutating fetch requests to send it in a header. Reject unknown Host values and cross-origin writes. Limit HTTP bodies to 64 KiB, including chunked bodies. Render data as plain text and use no CDN.

MockMvc tests cover each route and error: 400 for malformed JSON, 404 for an unregistered project or document, 409 for a stale revision, 422 for a domain error, 413 for oversized bodies, and 403 for CSRF/Host failures. Add a real localhost-port test; do not claim the server works based only on mocks.

All Skill state changes, saves, and exports require a revision. Preview does not save data. Services do not call HTTP or a shell; the core remains separate from the CLI.
