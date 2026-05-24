# Network-Only Data Access for MVP

The MVP does not implement local caching (Room is removed entirely). All reads and writes go directly to Supabase. If the device is offline, the app shows an error state.

The app's core use case — coordinating shared expenses in real time during a trip — requires all members to be online for the coordination to be meaningful. Offline-first adds significant complexity (two sources of truth, sync conflict resolution, cache invalidation) that is not justified for the MVP. Offline support is planned for a later release.

## Consequences

Room and all related DAOs, entities, and database migrations are removed. The repository layer is reimplemented against the Supabase Kotlin client with Realtime subscriptions replacing `Flow`-backed Room queries.
