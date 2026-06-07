# ProyectoPlanTravel — Claude Code Instructions

## Security: never commit secrets

**Before staging or committing any file, check for API keys and credentials.**

- Never hardcode API keys, tokens, or secrets in source files, SQL migrations, scripts, or docs
- Migration files are high risk — backfill scripts often carry full API URLs with `?key=...` params; strip those before committing
- Keys must come from `local.properties` (git-ignored) or environment variables at runtime
- If a key is found in any file about to be committed: stop, tell the user to rotate it, do not push

Patterns to flag: `AIza`, `sk-`, `eyJ`, `anon`, `service_role`, `key=`, `token=`, `secret=`

## Commit messages

In Spanish.

## Test discipline

- Write failing tests first (RED), then fix (GREEN) — one test at a time
- Always call `reload*()` after create/update/delete mutations; never rely on Realtime alone
- 213 tests must pass before any commit (`./gradlew :app:testDebugUnitTest`)

## Realtime rules

- NEVER use `filter()` on `postgresChangeFlow`
- `group-polls-broadcast-$groupId` is shared between `DestinationViewModel` and `PollViewModel` — never call `removeChannel` on it

## FK invariant

`poll_candidates.added_by_member_id`, `poll_votes.member_id`, and `itinerary_events.created_by_member_id` all reference `group_members(id)` — NOT `auth.users.id`.

## DB casing

State stored as `'open'`/`'closed'`, type as `'destination'`/`'activity'` (all lowercase).
