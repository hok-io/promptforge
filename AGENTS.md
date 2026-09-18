# PromptForge UI/UX development

For interface design or usability changes, use a locally installed UI/UX Pro Max Skill when available: read `.agents/skills/ui-ux-pro-max/SKILL.md` and apply relevant rules from its local search data and references. Fresh checkouts do not include `.agents/skills/`; install the optional Skill locally before relying on it. Do not assume it was read merely because a service is reachable.

- Read `docs/uiux-2026-09.md` for product-specific decisions and acceptance criteria.
- Keep user-facing navigation task-oriented: use, connect, verify, manage. Keep technical setup out of ordinary task entry.
- A reachable Web/MCP service is not proof that an agent read a Skill. Connection receipts must be bound to an actual token-bearing read and saved revision; task correctness remains a separate check.
- Preserve full outline and selection data; present advanced settings progressively.
- Verify meaningful flows in a real browser and at mobile widths. Do not report a check as passed based only on file presence or HTTP success.
