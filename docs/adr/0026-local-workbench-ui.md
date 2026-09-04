# ADR 0026 — Show Operations and Revision State to People

The local HTML/CSS/JavaScript UI calls the Java JSON API. Do not add another frontend framework; keep business flows in Java domain, service, repository, and controller layers.

The Prompt page provides manual/automatic rule selection, knowledge search and selection, ordered blocks, variables, and comparison before saving. A task frame uses only task text; a prompt template adds blocks. Preview is triggered by a button rather than every keystroke. For the same project, show only the most recent preview response; after switching projects, discard the previous project's response. A new draft resets mode, variables, and selections.

The Skill page preserves source text and references and binds state to a revision. Unsaved content cannot be approved, activated, exported, or evaluated, avoiding actions against an old version while a draft is visible. Refresh state and stale evaluations after saving. Import and export use explicit names within the project; activation does not mean installation.

Display external text through `textContent` or form values; do not execute Markdown or HTML. Keep sources and evaluation data visible, and wrap long sources. On narrow screens, retain rule and knowledge selection. Custom IDs follow Java slug rules: begin with a lowercase letter, contain lowercase letters, digits, or hyphens, and have at most 40 characters.

Playwright Java tests real-browser success flows, missing variables, stale saves, HTML text, and narrow-screen selection. Tests block external requests and store screenshots under the local build directory. Browser tests may use installed Edge or a specified Chromium version; see the [official browser documentation](https://playwright.dev/java/docs/browsers).
