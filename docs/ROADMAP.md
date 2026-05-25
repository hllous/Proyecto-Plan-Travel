# Roadmap

Complete feature timeline for Plan Travel, from MVP through future releases.

## MVP

Features that must ship for the app to be functional. Each has a full PRD as a GitHub issue.

| Feature | GitHub Issue | Status | Summary |
|---|---|---|---|
| **Auth & Identity** | [#9](https://github.com/hllous/Proyecto-Plan-Travel/issues/9) | ✅ Done | Email+password and Google OAuth login via Supabase Auth. Every GroupMember is a registered User linked by userId. Invite flow updated to link to real accounts. User Profile (display name, phone number, profile photo) set at registration and visible to all shared group members. |
| **Backend Migration (Room → Supabase)** | [#10](https://github.com/hllous/Proyecto-Plan-Travel/issues/10) | ✅ Done | Removed Room entirely. Rewrote repository layer against Supabase Kotlin client. Supabase Realtime channels for groups, members, invites, expenses, and assignments. Split MainViewModel into GroupViewModel, ExpenseViewModel, DestinationViewModel, ItineraryViewModel. Fixed invite/QR cross-device flow. UiState error handling with retry in all screens. |
| **Group Rework** | [#18](https://github.com/hllous/Proyecto-Plan-Travel/issues/18) | ⬜ Not started | Drop free-text admin name from create-group (derive from User Profile). Real Leave Group action for USER-role members. Multi-group list UX with group switching. ADMIN kick with confirmation dialog. Depends on #10 for Supabase data. |
| **UI Redesign** | [#19](https://github.com/hllous/Proyecto-Plan-Travel/issues/19) | ⬜ Not started | Layout overhaul following MD3 principles. Bottom nav as sole primary navigation (Inicio/Dashboard, Grupos, Destinos, Gastos). Drawer repurposed for Profile, theme toggle, logout only. Dashboard home screen (static shell, wired in #10). New ProfileScreen. QR Scanner as contextual icon in Grupos. BallroomScreen renamed to ExpenseScreen. Auth screens redesigned. Existing color palette preserved. |
| **Expense Enhancements** | [#11](https://github.com/hllous/Proyecto-Plan-Travel/issues/11) | ⬜ Not started | Edit Expense Items (name, price, quantity) with domain guard rejecting quantity below total Assigned Quantity. Payment Status flag: member marks settlement as paid, ADMIN confirms. |
| **Trip Planning Module** | [#12](https://github.com/hllous/Proyecto-Plan-Travel/issues/12) | ⬜ Not started | Trip Destination on TravelGroup. Google Places API for Place Recommendations. Open-Meteo for weather (free, no key). Shared Group Itinerary with Itinerary Events editable by all members in real time. |
| **Trip Contacts** | [#13](https://github.com/hllous/Proyecto-Plan-Travel/issues/13) | ⬜ Not started | Group-level reference list for emergency numbers, accommodation, transport, and medical contacts. Name, phone, category, optional notes. Editable by any Group Member. |

### #10 Backend Migration — implementation detail

| Sub-issue | Status | Notes |
|---|---|---|
| [#20](https://github.com/hllous/Proyecto-Plan-Travel/issues/20) Supabase Realtime dep + Room removal | ✅ Done | Replaced Room with Supabase Kotlin client. Fixed Realtime channel naming (UUID suffix), `postgresChangeFlow` filter API, `SelectedGroupHolder` singleton scope. |
| [#21](https://github.com/hllous/Proyecto-Plan-Travel/issues/21) Groups + Members via Supabase Realtime | ✅ Done | `GroupViewModel` split from `MainViewModel`. `createGroup`/`deleteGroup`/`deleteMember`/`updateGroupName` use cases. `runCatching` on all mutations. Unit tested. |
| [#22](https://github.com/hllous/Proyecto-Plan-Travel/issues/22) Cross-device Invite flow via Supabase | ✅ Done | `generateInvite`/`deleteInvite`/`consumeInvite` wired to Supabase. `generateInviteThrows`/`deleteInviteThrows` flags in `FakeTravelRepository`. Regression tests added. |
| [#23](https://github.com/hllous/Proyecto-Plan-Travel/issues/23) Expense Items + Assignments via Supabase Realtime | ✅ Done | `ExpenseViewModel` fully wrapped in `runCatching`. `assignItem` catch broadened from ISE to all exceptions. `MainActivity` snackbar wired for `groupViewModel.message` and `expenseViewModel.message`. `fetchAssignmentsByItemIds` extracted to eliminate N+1 in `calculateSettlement`. 6 regression tests. |
| [#24](https://github.com/hllous/Proyecto-Plan-Travel/issues/24) UiState, error handling, ViewModel stubs | ✅ Done | `UiState` sealed class (`Loading`/`Success`/`Error`). `ErrorCard` component with retry. `groupsUiState`/`expenseItemsUiState` flows with `_retryTrigger`. `DestinationViewModel` and `ItineraryViewModel` stubs. Fixed missing `runCatching` on `GroupViewModel.createGroup`. |
| [#25](https://github.com/hllous/Proyecto-Plan-Travel/issues/25) FakeTravelRepository update + ViewModel unit tests | ✅ Done | `FakeTravelRepository` extended with reactive `MutableStateFlow` state, `calculateSettlementCallCount`, and throw flags. `GroupViewModelTest` and `ExpenseViewModelTest` coverage for member list reactivity, settlement recalculation, and error paths. |

### #9 Auth & Identity — implementation detail

| Sub-issue | Status | Notes |
|---|---|---|
| [#14](https://github.com/hllous/Proyecto-Plan-Travel/issues/14) Supabase CLI + schema | ✅ Done | Migration applied to cloud. 6 tables with RLS. |
| [#15](https://github.com/hllous/Proyecto-Plan-Travel/issues/15) Email/password auth + profile setup | ✅ Done | `AuthViewModel`, `AuthRepository`, `SupabaseAuthRepository`, `SessionProvider`, Login/Register/ProfileSetup screens, NavHost auth guard. Unit tested. Two bugs fixed post-implementation (see ADR-0005, `getDisplayName` DTO mismatch, `upsert` instead of `insert` in `createProfile`). |
| [#16](https://github.com/hllous/Proyecto-Plan-Travel/issues/16) Google OAuth + deep link | ✅ Done | Code implemented. Google Cloud Console (SHA-1 fingerprint) and Supabase Google provider configured by user. |
| [#17](https://github.com/hllous/Proyecto-Plan-Travel/issues/17) Invite flow with real accounts | ✅ Done | Removes `AddMemberUseCase`, removes `currentMemberId` from `MainViewModel`, links `ConsumeInviteUseCase` to `SessionProvider`. |

## v2

| Feature | Rationale |
|---|---|
| **Offline-first (Room as local cache)** | Room removed in MVP for simplicity (ADR-0002). Re-introduce as a sync layer over Supabase once the network model is stable. |
| **Mercado Pago payment integration** | MVP uses mark-as-paid (Payment Status flag). Mercado Pago is the preferred next step: generate a payment link per Member Settlement, record confirmation automatically on payment. |
| **Push notifications** | Notify members when a new Expense Item is added, an assignment changes, or a payment is confirmed. Requires Firebase Cloud Messaging + Supabase webhook trigger. |
| **OpenTripMap / Foursquare fallback for Place Recommendations** | Activated if Google Places API free credits are exhausted (ADR-0003). OpenTripMap is free with no key; Foursquare free tier allows 1,000 calls/day. |
| **Travel Documents** | Personal credentials (DNI, passport, insurance, etc.) with structured fields, expiry date, optional notes, and optional photo attachment via Supabase Storage. Private by default; owner can share with a specific Travel Group. |

## v3

| Feature | Rationale |
|---|---|
| **Device calendar sync for Itinerary Events** | Export Group Itinerary events to the user's Android calendar via `CalendarContract`. Requires calendar read/write permissions. Deferred because the Group Itinerary must be stable first. |
| **Guest (name-only) members** | Allow an ADMIN to add a member by name without requiring a Plan Travel account. Useful for participants without a smartphone. Adds complexity to RLS and settlement attribution. |
| **Collaborative voting on Place Recommendations** | Thumbs up/down on Place Recommendations so the group can surface the most popular activities. Requires a new Supabase table and Realtime subscription. |
| **Expense Item history / audit log** | Track who added or edited each Expense Item and when. Useful for dispute resolution within a group. |
