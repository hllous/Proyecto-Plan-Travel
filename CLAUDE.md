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

### Local-device UI update after group mutations

`sendBroadcast` and Postgres Changes are **not reliable for updating the local device's UI** after self-mutations on `travel_groups` or `group_members`. Root causes:

- **INSERT** (createGroup): `group_members` INSERT fires after `travel_groups`, so the first PG Change event calls `fetchGroupsForUser` before membership exists → returns stale data. RLS on `group_members` INSERT is self-referential and may not deliver.
- **DELETE** (deleteGroup, leaveGroup): CASCADE deletes the user's `group_members` row in the same transaction. By the time Supabase evaluates RLS for the PG Change event, membership is gone → event blocked.
- **UPDATE** (updateGroupName, endTrip, reactivateTrip, setTripDestination): `sendBroadcast` via HTTP REST may fail silently (wrapped in `runCatching`); PG Changes also unreliable in practice.

**Rule: every mutation to `travel_groups` or `group_members` that affects the current user must call `_observeGroupsVersion.value++` immediately after the DB write.** This restarts `createObserveGroupsChannelFlow`, which re-fetches from DB and emits the correct state immediately — no reliance on Realtime for the local device.

`sendBroadcast` is still sent after, as the cross-device signal for other users.

`consumeInvite` was the reference implementation that already followed this pattern correctly.

## FK invariant

`poll_candidates.added_by_member_id`, `poll_votes.member_id`, and `itinerary_events.created_by_member_id` all reference `group_members(id)` — NOT `auth.users.id`.

## DB casing

State stored as `'open'`/`'closed'`, type as `'destination'`/`'activity'` (all lowercase).
