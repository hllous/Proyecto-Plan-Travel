## Problem Statement

Group Members do not receive live UI updates when other Group Members take collaborative actions. The most critical case: when User B joins a Travel Group via an Invite Token, User A (already in the group) does not see the new Group Member appear, and User B does not see the Travel Group appear in their list — both users must relaunch the app to see current state. The same class of bug likely affects every other collaborative flow (Trip Destination changes, Group Itinerary edits, Group Poll actions, Expense Group mutations) due to the same two structural gaps: mutations do not emit Realtime broadcasts, and some observers do not listen to broadcasts.

## Solution

For every collaborative action in the app, the acting Group Member's mutation should emit a Realtime broadcast immediately after the DB write, and every observer flow should merge both a Postgres Changes listener and a broadcast listener — so that Group Members whose RLS policies block the raw Postgres Change still receive the update via the broadcast fallback. Each section (Groups & Membership, Destinations, Polls, Itinerary, Expenses, Invites) is treated as an independent unit of work so that context stays manageable.

## User Stories

### Group Membership & Join Flow
1. As a Group Member, I want to see a newly joined member appear in the member list immediately after they join, so that I do not need to relaunch the app.
2. As a User who just consumed an Invite Token, I want my new Travel Group to appear in my group list immediately, so that I can start using the group without relaunching.
3. As a Group Member, I want to see the member list update immediately when another Group Member leaves the Travel Group, so that I always have an accurate view of who is in the trip.
4. As an ADMIN, I want the member list to update immediately after I kick a Group Member, so that I see the action reflected without relaunching.
5. As a kicked Group Member, I want the Travel Group to disappear from my list immediately after the ADMIN removes me, so that I am not confused about my membership status.
6. As a Group Member, I want to see the Travel Group name update immediately when the ADMIN renames it, so that the name stays consistent across all members.
7. As a Group Member, I want to see the Travel Group disappear from my list immediately when the ADMIN deletes it, so that I am not navigating a group that no longer exists.

### Trip Destination
8. As a Group Member, I want to see the Trip Destination update on the map immediately when the ADMIN sets or changes it, so that all members are looking at the same destination without relaunching.
9. As a Group Member, I want to see the Trip Destination cleared immediately when the ADMIN removes it, so that the Destinations tab reflects the current state.

### Group Polls
10. As a Group Member, I want to see a new Group Poll banner appear immediately when the ADMIN creates it, so that I can start voting without relaunching.
11. As a Group Member, I want to see a Group Poll close immediately when the ADMIN closes it, so that I know voting is over.
12. As a Group Member, I want to see a Group Poll disappear immediately when the ADMIN deletes it, so that the Destinations tab stays clean.
13. As a Group Member, I want to see a new Poll Candidate appear in the active poll immediately after any Group Member adds it, so that I can vote on it right away.
14. As a Group Member, I want to see the thumbs-up count on a Poll Candidate update immediately when another Group Member toggles their vote, so that I see live vote counts.

### Group Itinerary
15. As a Group Member, I want to see a new Itinerary Event appear in the Group Itinerary immediately after any Group Member creates it, so that I see the latest plan.
16. As a Group Member, I want to see an Itinerary Event update immediately when any Group Member edits it, so that I always see the correct event details.
17. As a Group Member, I want to see an Itinerary Event disappear immediately when any Group Member deletes it, so that the itinerary stays accurate.

### Expense Groups & Items
18. As a Group Member, I want to see a new Expense Group appear immediately after any Group Member creates it, so that I can start adding Expense Items.
19. As a Group Member, I want to see an Expense Group disappear immediately after any Group Member deletes it, so that the expenses list is accurate.
20. As a Group Member, I want to see an Expense Group renamed immediately after any Group Member renames it, so that the name is consistent.
21. As a Group Member, I want to see an Expense Group finalized immediately after the ADMIN finalizes it, so that I know it is read-only.
22. As a Group Member, I want to see the payer on an Expense Group update immediately when it is changed, so that settlement calculations reflect the right creditor.
23. As a Group Member, I want to see a new Expense Item appear immediately after any Group Member adds it, so that the total cost is always current.
24. As a Group Member, I want to see an Expense Item disappear immediately after any Group Member deletes it, so that the item list is accurate.
25. As a Group Member, I want to see Item Assignments update immediately after any Group Member assigns or re-assigns quantities, so that I see the latest settlement calculations.

### Invites
26. As an ADMIN, I want to see a generated Invite Token appear immediately in the invite management screen, so that I can share it right away.
27. As an ADMIN, I want to see a deleted Invite Token disappear immediately, so that I know it is no longer valid.

## Implementation Decisions

### Pattern: dual-listener observer (existing, to be applied consistently)
All `observe*` repository functions must merge two change sources:
- A Postgres Changes listener (`postgresChangeFlow`) for same-user mutations and cases where RLS grants cross-user visibility.
- A broadcast listener (`broadcastFlow`) as a fallback for cross-user mutations blocked by RLS.

`observeMembers` and `observeItineraryEvents` already implement this dual-listener pattern. `observeGroups` does not — it must be updated to add a broadcast fallback channel.

### Pattern: broadcast-after-write (existing, to be applied consistently)
Every suspend mutation that modifies data visible to other Group Members must call `sendBroadcast(channelName, event)` after the DB write succeeds. `sendBroadcast` must NOT call `subscribe()` on the channel it broadcasts to (to avoid evicting the observer's live channel from the realtime subscriptions map).

### Section 1 — Groups & Membership
- `consumeInvite` must call `broadcastMemberJoined(groupId)` after the `group_members` INSERT.
- `observeGroups` must be extended with a broadcast fallback channel (analogous to `observeMembers`). The broadcast event name should be `group_list_changed`.
- Every mutation that changes the `group_members` or `travel_groups` tables and affects other Group Members (leave, delete group, rename group, kick member) must emit a `group_list_changed` broadcast so that all affected users' `observeGroups` flows re-fetch.
- RLS policies on `group_members` and `travel_groups` must be audited to confirm whether SELECT policies allow cross-member row visibility; any gap must be covered by the broadcast fallback.

### Section 2 — Trip Destination
- `setTripDestination` mutates `travel_groups`. `observeGroups` already listens to `travel_groups` via Postgres Changes, but the same RLS gap applies. Once Section 1 adds the `group_list_changed` broadcast fallback to `observeGroups`, `setTripDestination` should also emit `group_list_changed` so all members' `observeGroups` flows re-fetch the updated Trip Destination.

### Section 3 — Group Polls
- `createPoll`, `closePoll`, and `deletePoll` in the repository must each emit a `poll_changed` broadcast on the `group-polls-broadcast-$groupId` channel.
- `observeActivePoll` already merges Postgres Changes and `poll_changed` broadcast — no structural change needed; verify that the above mutations actually call `sendBroadcast`.
- `addCandidate` and `toggleVote` must emit a `poll_candidate_changed` broadcast on `poll-candidates-broadcast-$pollId`.
- `observePollCandidates` already merges Postgres Changes and `poll_candidate_changed` broadcast — verify that mutations call `sendBroadcast`.

### Section 4 — Group Itinerary
- `createItineraryEvent`, `updateItineraryEvent`, and `deleteItineraryEvent` must each emit an `itinerary_event_changed` broadcast on `itinerary-events-broadcast-$groupId`.
- `observeItineraryEvents` already implements the dual-listener pattern — verify that the above mutations all call `sendBroadcast`.

### Section 5 — Expense Groups & Items
- All expense mutations (`createExpenseGroup`, `deleteExpenseGroup`, `renameExpenseGroup`, `setExpenseGroupPinned`, `setPayer`, `finalizeExpenseGroup`) must emit `expense_group_changed` on `expense-groups-broadcast-$groupId`.
- `addExpenseItem` and `deleteExpenseItem` must emit `expense_item_changed` on `expense-items-broadcast-$expenseGroupId`.
- `assignItemToMember`, `divideEqually`, and `resetAllAssignments` must emit `assignment_changed` on `assignments-broadcast-$expenseGroupId`.
- All three `observe*` expense functions already implement the dual-listener pattern — verify each mutation calls `sendBroadcast`.

### Section 6 — Invites
- `observeInvites` uses only a Postgres Changes listener. The invite management screen is typically accessed only by the ADMIN, and the ADMIN performs the mutations, so RLS self-visibility should cover this. Audit the `invite_tokens` SELECT policy to confirm — if only ADMIN rows are visible, no broadcast fallback is needed. If not, add one.
- `generateInvite` and `deleteInvite` do not need a broadcast because the invite list is admin-only — but verify with the RLS audit.

### ViewModel-side reload calls
After every mutation, the acting user's own ViewModel must call the relevant `reload*()` function to force a flow re-subscription. This is the actor's guarantee: they see their own change even if both Postgres Changes self-echo and the broadcast are delayed. Verify this is in place for every mutation listed above.

## Testing Decisions

### What makes a good test
Tests should verify observable ViewModel state (UI state flows, emitted lists) in response to simulated repository events — not internal channel or broadcast plumbing. Use the fake repository pattern already in place (`FakeTravelRepository`) to push remote changes directly into flows and assert that ViewModel state updates correctly.

### Modules to test
- `GroupViewModel` — push a remote `group_members` INSERT (simulating User B joining) and assert `groups` and `members` state updates without a `reloadGroups()` call from the actor.
- `GroupViewModel` — simulate a remote group deletion and assert the group disappears from the list.
- `DestinationViewModel` — simulate a remote `travel_groups` UPDATE (Trip Destination changed) and assert `tripDestination` state updates.
- `PollViewModel` — simulate a remote poll INSERT and assert the active poll banner appears; simulate close and assert state changes.
- `PollViewModel` — simulate a remote `poll_candidates` INSERT and assert the candidate list updates.
- `ItineraryViewModel` — simulate a remote `itinerary_events` INSERT and assert the event list updates (prior art: `ItineraryViewModelRealtimeTest`).
- `ExpenseViewModel` — simulate remote mutations for each expense flow and assert state updates (prior art: `ExpenseViewModelRealtimeTest`).

### Prior art
- `ItineraryViewModelRealtimeTest` — reference pattern for how remote pushes are simulated via `FakeTravelRepository`.
- `ExpenseViewModelRealtimeTest` — covers expense items, assignments, and expense groups under remote-push scenarios.
- `PollViewModelRealtimeTest` — covers poll and candidate flows.
- `SupabaseTravelRepositoryRealtimeContractTest` — structural checks on `blockUntilSubscribed` and broadcast observer count.

## Out of Scope

- Presence indicators (e.g. "User X is currently viewing this screen") — no presence channel exists and none is planned.
- Push notifications for realtime events — notifications are a separate feature.
- Offline / optimistic UI — the project is network-only per ADR-0002.
- Reconnection and back-off on Realtime WebSocket drops — out of scope for this audit.
- Any new Realtime channels for tables not currently observed (Trip Contacts, Travel Documents, User Profiles).

## Further Notes

- Per `CLAUDE.md`: `filter()` must never be used on `postgresChangeFlow`. All observers filter in the fetch layer, not in the Postgres Changes subscription.
- The `group-polls-broadcast-$groupId` channel is shared between `DestinationViewModel` and `PollViewModel` — it must never have `removeChannel` called on it from within either ViewModel.
- Per `CLAUDE.md`: FK columns `poll_candidates.added_by_member_id`, `poll_votes.member_id`, and `itinerary_events.created_by_member_id` reference `group_members(id)`, not `auth.users.id`.
- Each section should be implemented and reviewed independently to keep context manageable.
- 213 tests must pass before any commit.
