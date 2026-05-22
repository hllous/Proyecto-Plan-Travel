# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Layout

This is a single-context repo.

## Before exploring, read these

- `CONTEXT.md` at the repo root, if present.
- `docs/adr/` at the repo root, if present. Read ADRs that touch the area being changed.

If these files do not exist, proceed silently. Do not flag their absence or suggest creating them upfront. Producer skills such as `grill-with-docs` can create them lazily when terms or decisions are resolved.

## Use the glossary's vocabulary

When output names a domain concept, use the term as defined in `CONTEXT.md`. Do not drift to synonyms the glossary explicitly avoids.

If the concept is not in the glossary yet, either reconsider the language or note the gap for `grill-with-docs`.

## Flag ADR conflicts

If output contradicts an existing ADR, surface it explicitly rather than silently overriding it.
