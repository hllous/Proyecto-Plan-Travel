## Agent skills

Project agent skills are local-only. Maintain skills only under `.agents/skills/` and `.claude/skills/`; do not install, lock, or rely on global or GitHub-backed skill sources for this repo.

### Issue tracker

Issues and PRDs are tracked in GitHub Issues for `hllous/Proyecto-Plan-Travel`. See `docs/agents/issue-tracker.md`.

### Triage labels

Use the default five-label triage vocabulary. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context repo: read root `CONTEXT.md` and root `docs/adr/` when present. See `docs/agents/domain.md`.

### UI palette

When editing or adding UI, always use the app's existing Atlas Material 3 palette from `app/src/main/java/com/hllous/plantravel/ui/theme/`.
Do not introduce one-off colors, pastel drifts, or screen-specific palettes unless the user explicitly asks for a new palette direction.
If a screen needs styling adjustments, derive them from `MaterialTheme.colorScheme` so they stay consistent with the rest of the app in light and dark mode.
