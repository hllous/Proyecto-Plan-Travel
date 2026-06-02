# Roadmap

Complete feature timeline for Plan Travel, from MVP through future releases.

## MVP

Features that must ship for the app to be functional. Each has a full PRD as a GitHub issue.

| Feature | GitHub Issue | Status | Summary |
|---|---|---|---|
| **Auth & Identity** | [#9](https://github.com/hllous/Proyecto-Plan-Travel/issues/9) | ✅ Done | Email+password and Google OAuth login via Supabase Auth. Every GroupMember is a registered User linked by userId. Invite flow updated to link to real accounts. User Profile (display name, phone number, profile photo) set at registration and visible to all shared group members. |
| **Backend Migration (Room → Supabase)** | [#10](https://github.com/hllous/Proyecto-Plan-Travel/issues/10) | ✅ Done | Removed Room entirely. Rewrote repository layer against Supabase Kotlin client. Supabase Realtime channels for groups, members, invites, expenses, and assignments. Split MainViewModel into GroupViewModel, ExpenseViewModel, DestinationViewModel, ItineraryViewModel. Fixed invite/QR cross-device flow. UiState error handling with retry in all screens. |
| **Group Rework** | [#18](https://github.com/hllous/Proyecto-Plan-Travel/issues/18) | ✅ Done | Drop free-text admin name from create-group (derive from User Profile). Real Leave Group action for USER-role members with confirmation dialog. Multi-group list UX with group switching and member count. ADMIN kick with confirmation dialog. RLS policy for user self-delete. |
| **UI Redesign** | [#19](https://github.com/hllous/Proyecto-Plan-Travel/issues/19) | ✅ Done | Layout overhaul following MD3 principles. Atlas palette + Fraunces/Plus Jakarta Sans typography. Bottom nav as sole primary navigation (Inicio/Dashboard, Grupos, Destinos, Gastos). Drawer repurposed for Profile, theme toggle, logout only. Dashboard home screen with contextual greeting, group card, quick actions, and suggestion tiles. New ProfileScreen. QR Scanner as contextual icon in Grupos. BallroomScreen renamed to ExpenseScreen. Auth screens redesigned with brand panel. |
| **Expense Enhancements** | [#11](https://github.com/hllous/Proyecto-Plan-Travel/issues/11) | ⬜ Not started | Edit Expense Items (name, price, quantity) with domain guard rejecting quantity below total Assigned Quantity. Payment Status flag: member marks settlement as paid, ADMIN confirms. |
| **Expense Groups + P2P Payments** | TBD | ⬜ Not started | Expense Groups as named containers of Expense Items with independent per-group settlement. Open/Finalized states; ADMIN finalizes. Debt simplification to produce Peer-to-Peer Debts. Payment Deep Links via `mercadopago://` using MP Alias stored on User Profile. Push notifications (FCM) for new item, fully assigned item, and missing assignments on finalize. |
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

### #19 UI Redesign — implementation detail

| Sub-issue | Status | Notes |
|---|---|---|
| [#26](https://github.com/hllous/Proyecto-Plan-Travel/issues/26) Atlas palette + typography foundation | ✅ Done | Replaced legacy palette with Atlas token set. Added Fraunces (display/headings) and Plus Jakarta Sans (all UI chrome) via Google Fonts. |
| [#27](https://github.com/hllous/Proyecto-Plan-Travel/issues/27) Navigation scaffold overhaul | ✅ Done | Bottom nav reduced to 4 tabs. Drawer repurposed for user-level actions only. Theme toggle moved to drawer. `BallroomScreen` renamed to `ExpenseScreen`. `profile` and `group_detail` stub routes added. |
| [#29](https://github.com/hllous/Proyecto-Plan-Travel/issues/29) Shared components: ErrorCard, LoadingIndicator, Snackbar | ✅ Done | `ErrorCard` with icon + title. `LoadingIndicator`. `travelTextFieldColors`. Snackbar wired into root scaffold. |
| [#28](https://github.com/hllous/Proyecto-Plan-Travel/issues/28) Auth screens redesign | ✅ Done | Login, Register, ProfileSetup use brand panel (primary blue top, form slides up). `AuthBrandPanel` shared component extracted. |
| [#31](https://github.com/hllous/Proyecto-Plan-Travel/issues/31) ProfileScreen | ✅ Done | Brand panel header with initials avatar, Fraunces name, active-member badge. Info rows (name, email, phone). Group card. Logout button. |
| [#32](https://github.com/hllous/Proyecto-Plan-Travel/issues/32) GroupsScreen + GroupDetailScreen | ✅ Done | GroupsScreen: LargeTopAppBar, ElevatedCard list with 4dp accent border, role chips, FAB. GroupDetailScreen: pushed route with members, invite, danger zone sections. |
| [#30](https://github.com/hllous/Proyecto-Plan-Travel/issues/30) Dashboard (HomeScreen) | ✅ Done | Contextual greeting (Buenos días/tardes/noches) from `greetingForHour()`. Empty state CTA. Group status card, quick-action FilledTonalButtons, contextual tile LazyRow. |
| [#34](https://github.com/hllous/Proyecto-Plan-Travel/issues/34) DestinationScreen | ✅ Done | LargeTopAppBar with collapse on scroll. Horizontal FilterChip LazyRow. ElevatedCard destination items with LocationOn icon + Fraunces title. Two distinct empty states. |
| [#33](https://github.com/hllous/Proyecto-Plan-Travel/issues/33) ExpenseScreen polish | ✅ Done | LargeTopAppBar. Group/member chip selectors removed; first group auto-selected in ViewModel. ElevatedCard items with 4dp primaryContainer/errorContainer left border. Simplified bottom panel (plain Surface, TextButton toggle). SettlementCard uses surfaceVariant. SettlementWarningCard adds Warning icon. |

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
| **MercadoPago OAuth / Checkout Pro** | Deep-link P2P payments ship in MVP (ADR-0007). Full OAuth flow — Checkout Pro links generated server-side, webhook auto-confirms Payment Status — deferred here as it requires MP MCP setup and per-user OAuth. |
| **OpenTripMap / Foursquare fallback for Place Recommendations** | Activated if Google Places API free credits are exhausted (ADR-0003). OpenTripMap is free with no key; Foursquare free tier allows 1,000 calls/day. |
| **Travel Documents** | Personal credentials (DNI, passport, insurance, etc.) with structured fields, expiry date, optional notes, and optional photo attachment via Supabase Storage. Private by default; owner can share with a specific Travel Group. |

## v3

| Feature | Rationale |
|---|---|
| **Device calendar sync for Itinerary Events** | Export Group Itinerary events to the user's Android calendar via `CalendarContract`. Requires calendar read/write permissions. Deferred because the Group Itinerary must be stable first. |
| **MercadoPago OAuth / Checkout Pro (full P2P)** | Each user connects their MP account via OAuth. App generates Checkout Pro payment preferences server-side via an Edge Function using the MP MCP. Webhook auto-confirms Payment Status. Supersedes the deep-link approach from MVP (ADR-0007). |
| **Guest (name-only) members** | Allow an ADMIN to add a member by name without requiring a Plan Travel account. Useful for participants without a smartphone. Adds complexity to RLS and settlement attribution. |
| **Collaborative voting on Place Recommendations** | Thumbs up/down on Place Recommendations so the group can surface the most popular activities. Requires a new Supabase table and Realtime subscription. |
| **Expense Item history / audit log** | Track who added or edited each Expense Item and when. Useful for dispute resolution within a group. |
