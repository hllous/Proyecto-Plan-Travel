# Roadmap

Complete feature timeline for Plan Travel, from MVP through future releases.

## MVP

Features that must ship for the app to be functional. Each has a full PRD as a GitHub issue.

| Feature | GitHub Issue | Status | Summary |
|---|---|---|---|
| **Auth & Identity** | [#9](https://github.com/hllous/Proyecto-Plan-Travel/issues/9) | 🟡 Partial | Email+password and Google OAuth login via Supabase Auth. Every GroupMember is a registered User linked by userId. Invite flow updated to link to real accounts. User Profile (display name, phone number, profile photo) set at registration and visible to all shared group members. |
| **Backend Migration (Room → Supabase)** | [#10](https://github.com/hllous/Proyecto-Plan-Travel/issues/10) | ⬜ Not started | Remove Room entirely. Rewrite repository layer against Supabase Kotlin client. Supabase Realtime channels replace Flow-backed queries. Split MainViewModel into AuthViewModel, GroupViewModel, ExpenseViewModel, DestinationViewModel, ItineraryViewModel. Remove debug artifacts. |
| **Expense Enhancements** | [#11](https://github.com/hllous/Proyecto-Plan-Travel/issues/11) | ⬜ Not started | Edit Expense Items (name, price, quantity) with domain guard rejecting quantity below total Assigned Quantity. Payment Status flag: member marks settlement as paid, ADMIN confirms. |
| **Trip Planning Module** | [#12](https://github.com/hllous/Proyecto-Plan-Travel/issues/12) | ⬜ Not started | Trip Destination on TravelGroup. Google Places API for Place Recommendations. Open-Meteo for weather (free, no key). Shared Group Itinerary with Itinerary Events editable by all members in real time. |
| **Trip Contacts** | [#13](https://github.com/hllous/Proyecto-Plan-Travel/issues/13) | ⬜ Not started | Group-level reference list for emergency numbers, accommodation, transport, and medical contacts. Name, phone, category, optional notes. Editable by any Group Member. |

### #9 Auth & Identity — implementation detail

| Sub-issue | Status | Notes |
|---|---|---|
| [#14](https://github.com/hllous/Proyecto-Plan-Travel/issues/14) Supabase CLI + schema | ✅ Done | Migration applied to cloud. 6 tables with RLS. |
| [#15](https://github.com/hllous/Proyecto-Plan-Travel/issues/15) Email/password auth + profile setup | ✅ Done | `AuthViewModel`, `AuthRepository`, `SupabaseAuthRepository`, `SessionProvider`, Login/Register/ProfileSetup screens, NavHost auth guard. Unit tested. Two bugs fixed post-implementation (see ADR-0005, `getDisplayName` DTO mismatch, `upsert` instead of `insert` in `createProfile`). |
| [#16](https://github.com/hllous/Proyecto-Plan-Travel/issues/16) Google OAuth + deep link | 🔴 Blocked | Requires human to configure Google Cloud Console (SHA-1 fingerprint) and enable Google provider in Supabase Auth dashboard. Do not start until user gives the go-ahead. |
| [#17](https://github.com/hllous/Proyecto-Plan-Travel/issues/17) Invite flow with real accounts | ⬜ Not started | Depends on #15 (done). Removes `AddMemberUseCase`, removes `currentMemberId` from `MainViewModel`, links `ConsumeInviteUseCase` to `SessionProvider`. |

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
