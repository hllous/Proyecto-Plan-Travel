# Supabase as Backend and Auth Provider

We chose Supabase (Postgres + Auth + Realtime) over Firebase (Firestore + Auth) as the backend for Plan Travel. The domain model is relational — `ItemAssignment` links `ExpenseItem` to `GroupMember`, `InviteToken` links to `TravelGroup` — and a document store would have forced denormalization or expensive client-side joins. Supabase row-level security also maps directly to the access rule "a User can only see groups they belong to," which Firestore's security model handles less cleanly. Real-time expense updates are implemented via Supabase Realtime channels scoped per Travel Group.

## Considered Options

- **Firebase (Firestore + Auth):** Rejected because the relational shape of the domain (assignments, memberships, invites) fits SQL better than documents, and because Firestore's query limitations would have required denormalization that conflicts with the settlement calculation logic.
- **Custom REST API:** Rejected for MVP due to infrastructure and maintenance cost. Can be introduced in a later release if Supabase becomes a bottleneck.
