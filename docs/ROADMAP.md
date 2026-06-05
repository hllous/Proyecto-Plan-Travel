# Roadmap

Complete feature timeline for Plan Travel, from MVP through future releases.

## MVP

Features that must ship for the app to be functional. Each has a full PRD as a GitHub issue.

| Feature | GitHub Issue | Status | Summary |
|---|---|---|---|
| **Auth & Identity** | [#9](https://github.com/hllous/Proyecto-Plan-Travel/issues/9) | ✅ Done | Email+password and Google OAuth login via Supabase Auth. Every GroupMember is a registered User linked by userId. Invite flow updated to link to real accounts. User Profile (display name, phone number, profile photo) set at registration and visible to all shared group members. |
| **Backend Migration (Room → Supabase)** | [#10](https://github.com/hllous/Proyecto-Plan-Travel/issues/10) | ✅ Done | Removed Room entirely. Rewrote repository layer against Supabase Kotlin client. Supabase Realtime channels for groups, members, invites, expenses, and assignments. Split MainViewModel into GroupViewModel, ExpenseViewModel. Fixed invite/QR cross-device flow. UiState error handling with retry in all screens. Realtime filter bug fixed (wildcard + server-side filter silently breaks subscriptions). |
| **Group Rework** | [#18](https://github.com/hllous/Proyecto-Plan-Travel/issues/18) | ✅ Done | Drop free-text admin name from create-group (derive from User Profile). Real Leave Group action for USER-role members with confirmation dialog. Multi-group list UX with group switching and member count. ADMIN kick with confirmation dialog. RLS policy for user self-delete. |
| **UI Redesign** | [#19](https://github.com/hllous/Proyecto-Plan-Travel/issues/19) | ✅ Done | Layout overhaul following MD3 principles. Atlas palette + Fraunces/Plus Jakarta Sans typography. Bottom nav as sole primary navigation (Inicio/Dashboard, Grupos, Destinos, Gastos). Drawer repurposed for Profile, theme toggle, logout only. Dashboard home screen with contextual greeting, group card, quick actions, and suggestion tiles. New ProfileScreen. QR Scanner as contextual icon in Grupos. Auth screens redesigned with brand panel. |
| **Expense Groups + P2P Payments** | [#36](https://github.com/hllous/Proyecto-Plan-Travel/issues/36) | ✅ Done | Expense Groups as named containers of Expense Items with independent per-group settlement. Open/Finalized states; ADMIN finalizes. Payer-centric settlement: ADMIN designates one payer per group; non-payers owe the payer their assigned cost. Payment Deep Links via `mercadopago://` using MP Alias stored on User Profile. |
| **Single Payer + Payer-centric Settlement** | [#48](https://github.com/hllous/Proyecto-Plan-Travel/issues/48) | ✅ Done | `paid_by_member_id` on `expense_groups`. `SetExpenseGroupPayerUseCase`. Settlement rewritten: non-payers owe payer their assigned cost; payer sees what each member owes them. `DebtSimplifier` removed (one-payer graphs need no simplification). Finalization blocked unless payer is set and all item quantities are fully assigned. `PayerSelectorCard` dropdown in detail screen; propagates via existing Realtime flow. PRD: `docs/prd-expense-payer.md`. |
| **Expense Screen Redesign** | — | ✅ Done | Full-screen Expense Group creation with quick-fill chips and category selector (`ExpenseGroupCategory` enum: comida, transporte, alojamiento, entretenimiento, otros). Hero card showing total, creation date, and category icon. Stacked assignee avatar circles on item cards. Left-accent item card border. Pin/unpin Expense Groups. `pinned_at` and `category` columns on `expense_groups` table. PRD: `docs/prd-expense-redesign.md`. |
| **Expense Enhancements** | [#11](https://github.com/hllous/Proyecto-Plan-Travel/issues/11) | ⬜ Not started | Edit Expense Items (name, price, quantity) with domain guard rejecting quantity below total Assigned Quantity. Payment Status flag: member marks settlement as paid, ADMIN confirms. |
| **Group Rework v2: One Group Per User** | — | ⬜ Not started | Hard DB constraint (unique index on `group_members.user_id`). Remove multi-group list UI. Simplify `GroupsScreen` to single-group view. Remove `SelectedGroupHolder` complexity. Prerequisite for Trip Planning Module. ADR-0008. |
| **Trip Planning Module** | [#49](https://github.com/hllous/Proyecto-Plan-Travel/issues/49) | ✅ Done | Two-level Destinations tab: Level 1 browse/search Argentine destinations by region (Patagonia, Cuyo, Noroeste, Litoral, Buenos Aires, Córdoba) via Google Places; Level 2 Place Recommendations ranked by review quality (rating ≥ 4.2, count ≥ 50 soft threshold). Trip Destination stored as Place ID + name + lat/lng on TravelGroup. Group Itinerary with day-grouped Itinerary Events, real-time, pre-fillable from POIs. Group Polls (Destination + Activity types): optional, ADMIN creates/closes, any member votes thumbs-up or adds candidates, one active poll at a time. PRD: `docs/prd-trip-planning.md`. Supersedes #12. Implemented as issues #50–54. |
| **Trip Contacts** | [#13](https://github.com/hllous/Proyecto-Plan-Travel/issues/13) | ⬜ Not started | Group-level reference list for emergency numbers, accommodation, transport, and medical contacts. Name, phone, category, optional notes. Editable by any Group Member. |
| **FCM Push Notifications** | [#44](https://github.com/hllous/Proyecto-Plan-Travel/issues/44) | ⬜ Not started | Push alerts for new item added, item fully assigned, and missing assignments on finalize. |
| **Optimistic updates** | [#45](https://github.com/hllous/Proyecto-Plan-Travel/issues/45) | ⬜ Not started | Replace retry-trigger channel teardown on local mutations with immediate local StateFlow update + rollback on failure. Eliminates 200–500ms lag after every write and brief Realtime coverage gap during channel rebuild. |
| **Realtime channel pooling** | [#46](https://github.com/hllous/Proyecto-Plan-Travel/issues/46) | ⬜ Not started | Merge 6 per-table Realtime channels into 1 per travel group. Increases concurrent user capacity 6× on the same Supabase plan (free tier: ~33 → ~200 concurrent users). |

### #10 Backend Migration — implementation detail

| Sub-issue | Status | Notes |
|---|---|---|
| [#20](https://github.com/hllous/Proyecto-Plan-Travel/issues/20) Supabase Realtime dep + Room removal | ✅ Done | Replaced Room with Supabase Kotlin client. Fixed Realtime channel naming (UUID suffix), `postgresChangeFlow` filter API, `SelectedGroupHolder` singleton scope. |
| [#21](https://github.com/hllous/Proyecto-Plan-Travel/issues/21) Groups + Members via Supabase Realtime | ✅ Done | `GroupViewModel` split from `MainViewModel`. `createGroup`/`deleteGroup`/`deleteMember`/`updateGroupName` use cases. `runCatching` on all mutations. Unit tested. |
| [#22](https://github.com/hllous/Proyecto-Plan-Travel/issues/22) Cross-device Invite flow via Supabase | ✅ Done | `generateInvite`/`deleteInvite`/`consumeInvite` wired to Supabase. `generateInviteThrows`/`deleteInviteThrows` flags in `FakeTravelRepository`. Regression tests added. |
| [#23](https://github.com/hllous/Proyecto-Plan-Travel/issues/23) Expense Items + Assignments via Supabase Realtime | ✅ Done | `ExpenseViewModel` fully wrapped in `runCatching`. `assignItem` catch broadened from ISE to all exceptions. `MainActivity` snackbar wired for `groupViewModel.message` and `expenseViewModel.message`. `fetchAssignmentsByItemIds` extracted to eliminate N+1 in `calculateSettlement`. 6 regression tests. |
| [#24](https://github.com/hllous/Proyecto-Plan-Travel/issues/24) UiState, error handling, ViewModel stubs | ✅ Done | `UiState` sealed class (`Loading`/`Success`/`Error`). `ErrorCard` component with retry. `groupsUiState`/`expenseItemsUiState` flows with `_retryTrigger`. `DestinationViewModel` and `ItineraryViewModel` stubs. Fixed missing `runCatching` on `GroupViewModel.createGroup`. |
| [#25](https://github.com/hllous/Proyecto-Plan-Travel/issues/25) FakeTravelRepository update + ViewModel unit tests | ✅ Done | `FakeTravelRepository` extended with reactive `MutableStateFlow` state, `calculateSettlementCallCount`, and throw flags. `GroupViewModelTest` and `ExpenseViewModelTest` coverage for member list reactivity, settlement recalculation, and error paths. |

### #36 Expense Groups + P2P Payments — implementation detail

| Sub-issue | Status | Notes |
|---|---|---|
| [#37](https://github.com/hllous/Proyecto-Plan-Travel/issues/37) Schema: expense_groups table, RLS, Realtime | ✅ Done | `expense_groups` table with `id`, `group_id`, `name`, `state`, `created_at`. RLS policies for member read/insert/delete and admin update. REPLICA IDENTITY FULL + realtime publication. |
| [#38](https://github.com/hllous/Proyecto-Plan-Travel/issues/38) Scope expense_items to Expense Groups | ✅ Done | Added `expense_group_id uuid REFERENCES expense_groups ON DELETE CASCADE` to `expense_items`. Migration, RLS update, and repository interface updated. |
| [#39](https://github.com/hllous/Proyecto-Plan-Travel/issues/39) Expense Group list — create, browse, delete, real-time | ✅ Done | `ExpenseGroupListScreen` with create panel, card list, delete confirmation dialog. Realtime via `observeExpenseGroups`. Reload trigger pattern on create/delete. |
| [#40](https://github.com/hllous/Proyecto-Plan-Travel/issues/40) Expense Group drill-in — items, assignments, settlement | ✅ Done | Drill-in screen with item list, per-member quantity stepper, assignment progress bar, bottom panel with settlement summary. |
| [#41](https://github.com/hllous/Proyecto-Plan-Travel/issues/41) ADMIN finalize Expense Group | ✅ Done | Finalize confirmation dialog, `state = 'finalized'` update, read-only enforcement on all UI controls. |
| [#42](https://github.com/hllous/Proyecto-Plan-Travel/issues/42) DebtSimplifier + Peer-to-Peer Debt view | ✅ Done | `DebtSimplifier` (later removed in #48) and `PeerToPerDebt` / `PeerToPerDebtUiModel` shown in bottom panel. Under #48 settlement was rewritten to payer-centric; `DebtSimplifier` deleted. |
| [#43](https://github.com/hllous/Proyecto-Plan-Travel/issues/43) MP Alias on Profile + Payment Deep Links + Payment Status | ✅ Done | `mp_alias` on `profiles`. `mercadopago://send` deep link in P2P debt rows. Debtor/creditor confirmation flags in `payment_status` table. |

### #19 UI Redesign — implementation detail

| Sub-issue | Status | Notes |
|---|---|---|
| [#26](https://github.com/hllous/Proyecto-Plan-Travel/issues/26) Atlas palette + typography foundation | ✅ Done | Replaced legacy palette with Atlas token set. Added Fraunces (display/headings) and Plus Jakarta Sans (all UI chrome) via Google Fonts. |
| [#27](https://github.com/hllous/Proyecto-Plan-Travel/issues/27) Navigation scaffold overhaul | ✅ Done | Bottom nav reduced to 4 tabs. Drawer repurposed for user-level actions only. Theme toggle moved to drawer. `BallroomScreen` renamed to `ExpenseScreen`. `profile` and `group_detail` stub routes added. |
| [#29](https://github.com/hllous/Proyecto-Plan-Travel/issues/29) Shared components: ErrorCard, LoadingIndicator, Snackbar | ✅ Done | `ErrorCard` with icon + title. `LoadingIndicator`. `travelTextFieldColors`. Snackbar wired into root scaffold. |
| [#28](https://github.com/hllous/Proyecto-Plan-Travel/issues/28) Auth screens redesign | ✅ Done | Login, Register, ProfileSetup use brand panel (primary blue top, form slides up). `AuthBrandPanel` shared component extracted. |
| [#31](https://github.com/hllous/Proyecto-Plan-Travel/issues/31) ProfileScreen | ✅ Done | Brand panel header with initials avatar, Fraunces name, active-member badge. Info rows (name, email, phone). Group card. Logout button. |
| [#32](https://github.com/hllous/Proyecto-Plan-Travel/issues/32) GroupsScreen + GroupDetailScreen | ✅ Done | GroupsScreen: immersive primary-color header, member avatar stack, invite section with code pill, copy/share, QR, expiry. GroupDetail: ADMIN settings sheet, leave/delete confirmation dialogs. |
| [#30](https://github.com/hllous/Proyecto-Plan-Travel/issues/30) Dashboard (HomeScreen) | ✅ Done | Contextual greeting (Buenos días/tardes/noches) from `greetingForHour()`. Empty state CTA. Group status card, quick-action FilledTonalButtons, contextual tile LazyRow. |
| [#34](https://github.com/hllous/Proyecto-Plan-Travel/issues/34) DestinationScreen | ✅ Done | LargeTopAppBar with collapse on scroll. Horizontal FilterChip LazyRow. ElevatedCard destination items with LocationOn icon + Fraunces title. Two distinct empty states. |
| [#33](https://github.com/hllous/Proyecto-Plan-Travel/issues/33) ExpenseScreen polish | ✅ Done | LargeTopAppBar with primary-color header. Group/member chip selectors removed; first group auto-selected. ElevatedCard items with assignment progress bar. Bottom panel with settlement summary and P2P debt rows. |

### #9 Auth & Identity — implementation detail

| Sub-issue | Status | Notes |
|---|---|---|
| [#14](https://github.com/hllous/Proyecto-Plan-Travel/issues/14) Supabase CLI + schema | ✅ Done | Migration applied to cloud. 6 tables with RLS. |
| [#15](https://github.com/hllous/Proyecto-Plan-Travel/issues/15) Email/password auth + profile setup | ✅ Done | `AuthViewModel`, `AuthRepository`, `SupabaseAuthRepository`, `SessionProvider`, Login/Register/ProfileSetup screens, NavHost auth guard. Unit tested. Two bugs fixed post-implementation (see ADR-0005, `getDisplayName` DTO mismatch, `upsert` instead of `insert` in `createProfile`). |
| [#16](https://github.com/hllous/Proyecto-Plan-Travel/issues/16) Google OAuth + deep link | ✅ Done | Code implemented. Google Cloud Console (SHA-1 fingerprint) and Supabase Google provider configured by user. |
| [#17](https://github.com/hllous/Proyecto-Plan-Travel/issues/17) Invite flow with real accounts | ✅ Done | Removes `AddMemberUseCase`, removes `currentMemberId` from `MainViewModel`, links `ConsumeInviteUseCase` to `SessionProvider`. |

## v2

| Feature | GitHub Issue | Rationale |
|---|---|---|
| **Offline-first (Room as local cache)** | — | Room removed in MVP for simplicity (ADR-0002). Re-introduce as a sync layer over Supabase once the network model is stable. |
| **MercadoPago OAuth / Checkout Pro** | — | Deep-link P2P payments ship in MVP (ADR-0007). Full OAuth flow — Checkout Pro links generated server-side, webhook auto-confirms Payment Status — deferred as it requires MP MCP setup and per-user OAuth. |
| **OpenTripMap / Foursquare fallback for Place Recommendations** | — | Activated if Google Places API free credits are exhausted (ADR-0003). OpenTripMap is free with no key; Foursquare free tier allows 1,000 calls/day. |
| **Travel Documents** | — | Personal credentials (DNI, passport, insurance, etc.) with structured fields, expiry date, optional notes, and optional photo attachment via Supabase Storage. Private by default; owner can share with a specific Travel Group. |

## v3

| Feature | Rationale |
|---|---|
| **Device calendar sync for Itinerary Events** | Export Group Itinerary events to the user's Android calendar via `CalendarContract`. Requires calendar read/write permissions. Deferred because the Group Itinerary must be stable first. |
| **MercadoPago OAuth / Checkout Pro (full P2P)** | Each user connects their MP account via OAuth. App generates Checkout Pro payment preferences server-side via an Edge Function using the MP MCP. Webhook auto-confirms Payment Status. Supersedes the deep-link approach from MVP (ADR-0007). |
| **Guest (name-only) members** | Allow an ADMIN to add a member by name without requiring a Plan Travel account. Useful for participants without a smartphone. Adds complexity to RLS and settlement attribution. |
| **Collaborative voting on Place Recommendations** | Thumbs up/down on Place Recommendations so the group can surface the most popular activities. Requires a new Supabase table and Realtime subscription. |
| **Expense Item history / audit log** | Track who added or edited each Expense Item and when. Useful for dispute resolution within a group. |
