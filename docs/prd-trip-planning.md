## Problem Statement

Group Members have no way to discover where to travel in Argentina, browse activities and lodging at a chosen destination, collaboratively vote on options before committing, or build a shared trip itinerary — all from within the app. The Destinations tab is a non-functional stub, `TravelGroup` has no Trip Destination field, and `DestinationViewModel` / `ItineraryViewModel` are empty. Members cannot answer the two most important questions of group travel planning: "Where are we going?" and "What are we doing?"

## Solution

Replace the stub Destinations tab with a two-level discovery and planning surface anchored to the group's Trip Destination. Level 1 lets Group Members browse and search Argentine destinations by region, with every card showing a real photo from Google Places. Level 2 shows Place Recommendations (lodging, food, activities, nature) for the chosen destination, ranked by review quality. Any Group Member can add a Place Recommendation to the Group Itinerary or to an active Group Poll. The ADMIN sets the Trip Destination — prompted to run a poll first — and finalizes poll results. A separate Itinerary screen shows the group's scheduled events in a chronological, day-grouped list.

**Prerequisite:** A separate Group Rework issue must enforce the one-group-per-user hard constraint (unique index on `group_members.user_id`, remove multi-group list UI, simplify `GroupsScreen` to single-group view) before this module ships. The Destinations tab assumes exactly one group per user.

## User Stories

### Trip Destination

1. As a Group Member, I want to open the Destinations tab and see a row of region chips (Patagonia, Cuyo, Noroeste, Litoral, Buenos Aires, Córdoba), so that I can browse Argentine destinations by region.
2. As a Group Member, I want to tap a region chip and see a list of destination cards sourced from Google Places, so that I can discover real places in that region.
3. As a Group Member, I want each destination card to show a photo, name, and rating sourced from the Places API, so that I can visually evaluate each option.
4. As a Group Member, I want to search for a destination by name using a search bar with Places Autocomplete filtered to Argentina, so that I can go directly to a known city without browsing.
5. As a Group Member, I want to tap a destination card and see a bottom sheet with a larger photo, description, rating, and review count, so that I can evaluate it before any action is taken.
6. As an ADMIN, I want to see an "Establecer como destino" button on the destination bottom sheet, so that I can set the group's Trip Destination.
7. As an ADMIN, when no Trip Destination is set, I want the app to prompt me to create a Destination Poll before committing, with an option to skip and set it directly, so that the group can vote before I decide.
8. As an ADMIN, when replacing an already-set Trip Destination, I want a confirmation dialog warning me that the current destination will be replaced, so that I don't overwrite it accidentally.
9. As a USER-role member, I want the "Establecer como destino" button to be hidden, so that only the ADMIN can commit to a destination.
10. As a Group Member, I want the Destinations tab to show Level 2 (Place Recommendations) automatically once a Trip Destination is set, so that I land directly on useful content.
11. As a Group Member, I want a "Cambiar destino" action on the Level 2 screen, so that I can return to Level 1 to browse other destinations.

### Place Recommendations

12. As a Group Member, I want to see Place Recommendations for the Trip Destination organized under category chips (Alojamiento, Gastronomía, Actividades, Naturaleza), so that I can focus on the type of place I care about.
13. As a Group Member, I want the top section of each category to show only places with rating ≥ 4.2 AND at least 50 reviews, sorted by rating × log(reviewCount), so that the best-reviewed places appear first.
14. As a Group Member, I want places below the rating or review-count threshold shown in a separate "Otros" section below the top results, so that lower-quality options are visible but not prioritized.
15. As a Group Member, I want each Place Recommendation card to show a photo, name, rating, and review count, so that I can compare options at a glance.
16. As a Group Member, I want to tap a Place Recommendation card and open a bottom sheet with a larger photo, address, rating, review count, and action buttons, so that I can get more detail and take action.
17. As a Group Member, I want the bottom sheet to include "Añadir al itinerario", "Añadir a encuesta", and "Ver en Maps" buttons, so that I can act on a recommendation without navigating away.
18. As a Group Member, I want "Ver en Maps" to open the place in Google Maps via a deep link, so that I can see its exact location and get directions.

### Group Itinerary

19. As a Group Member, I want to access the Group Itinerary via a "Ver itinerario" button on the Level 2 screen, so that I can see the group's planned events without leaving the Destinations tab context.
20. As a Group Member, I want the Group Itinerary to display Itinerary Events in a chronological list grouped by day with sticky date headers, so that the schedule is easy to scan.
21. As a Group Member, I want to tap "Añadir al itinerario" on a Place Recommendation bottom sheet and be prompted only to pick a date, so that the event name, description, and place reference are pre-filled from the POI automatically.
22. As a Group Member, I want to manually create an Itinerary Event from the Itinerary screen with a name, date, optional time, and optional description, so that I can add events not sourced from Place Recommendations.
23. As a Group Member, I want to edit or delete any Itinerary Event, so that the group can adjust the plan at any time.
24. As a Group Member, I want Itinerary Event changes made by any member to appear in real time via Realtime, so that everyone sees the latest plan without refreshing.
25. As a Group Member with no events in the itinerary yet, I want an empty state prompting me to browse destinations or add an event, so that the screen has a clear call to action.

### Group Poll

26. As an ADMIN, I want to create a Destination Poll by tapping "Crear encuesta" when prompted before setting a Trip Destination, so that the group can vote before I commit.
27. As an ADMIN, I want to create an Activity Poll from the Level 2 screen, so that the group can vote on which Place Recommendations to add to the itinerary.
28. As an ADMIN, I want the app to block a second poll from being created while one is already active, so that only one Group Poll is open at a time.
29. As an ADMIN, I want to optionally set an expiry date when creating a poll, so that the poll closes automatically if I forget to close it manually.
30. As a Group Member, I want to see an active poll banner at the top of the Destinations tab, so that I am immediately aware of an ongoing vote.
31. As a Group Member, I want to dismiss the poll banner with a close button, so that it doesn't take up screen space while I browse.
32. As a Group Member, I want to recover the dismissed poll via an "Encuesta activa" chip pinned to the start of the chip row, so that I can always get back to the poll.
33. As a Group Member, I want to add individual destination or activity candidates to an active poll from the Level 1 or Level 2 browse screen, so that I can nominate options for the group to vote on.
34. As a Group Member, I want to add multiple candidates at once using a multi-select mode on the browse screen, so that I can nominate several options without tapping one by one.
35. As a Group Member, I want to thumbs-up any number of Poll Candidates in the poll detail screen, so that I can express support for multiple options.
36. As a Group Member, I want the thumbs-up count on each Poll Candidate to update in real time, so that I can see the group's preferences as they form.
37. As a Group Member, I want each Poll Candidate card to show its photo and name from the Places API, so that I can identify what I'm voting on.
38. As an ADMIN, I want to close the poll manually from the poll detail screen, so that I can end voting when the group has had enough time.
39. As an ADMIN, after closing a poll, I want the app to prompt me to select the winning candidate, so that the result is explicit rather than automatic.
40. As an ADMIN, after selecting the winning Destination Poll candidate, I want the Trip Destination to be set automatically, so that I don't have to navigate back and set it separately.
41. As an ADMIN, after selecting the winning Activity Poll candidate, I want to be taken to the date-picker flow to add it to the Group Itinerary, so that the poll result lands in the plan.
42. As a Group Member, I want to see poll results (final thumbs-up counts) after the poll is closed, so that I understand how the group voted even after the ADMIN has chosen.

## Implementation Decisions

- **Prerequisite schema change — one-group-per-user**: Add a unique constraint on `group_members(user_id)` before this module ships. Attempting to join a second group must fail at the database level, not just in the UI.

- **Schema change — Trip Destination on `travel_groups`**: Add four nullable columns: `trip_destination_place_id text`, `trip_destination_name text`, `trip_destination_lat double precision`, `trip_destination_lng double precision`. The ADMIN-only RLS update policy already covers this via the existing `travel_groups: admin update` policy. The `TravelGroup` domain model gains four new nullable fields. `observeGroups()` propagates changes via the existing Realtime subscription.

- **Schema change — `itinerary_events` table**: Columns: `id uuid PK`, `group_id uuid FK travel_groups`, `name text`, `date date`, `time_of_day time nullable`, `description text nullable`, `place_id text nullable` (Google Place ID of the source POI, if created from a recommendation), `created_by_member_id uuid FK group_members`, `created_at timestamptz`. RLS: all group members may read, insert, update, delete. REPLICA IDENTITY FULL + realtime publication. `ItineraryEvent` domain model matches these columns.

- **Schema change — `group_polls` table**: Columns: `id uuid PK`, `group_id uuid FK travel_groups`, `type text CHECK ('destination', 'activity')`, `state text CHECK ('open', 'closed')`, `expires_at timestamptz nullable`, `winner_place_id text nullable`, `created_at timestamptz`. Unique partial index on `group_id WHERE state = 'open'` enforces the one-active-poll constraint at the database level. RLS: all members read; ADMIN insert/update; no member delete.

- **Schema change — `poll_candidates` table**: Columns: `id uuid PK`, `poll_id uuid FK group_polls ON DELETE CASCADE`, `place_id text`, `name text`, `photo_url text`, `added_by_member_id uuid FK group_members`, `created_at timestamptz`. Unique constraint on `(poll_id, place_id)` prevents duplicate candidates. RLS: all members read/insert; no update; no delete.

- **Schema change — `poll_votes` table**: Columns: `id uuid PK`, `candidate_id uuid FK poll_candidates ON DELETE CASCADE`, `member_id uuid FK group_members`, `created_at timestamptz`. Unique constraint on `(candidate_id, member_id)` — one thumbs-up per member per candidate, toggled by delete. RLS: all members read/insert/delete own row.

- **Places API client — new interface `PlacesApiClient`**: Two methods: `searchDestinations(region: String): List<PlaceResult>` (Level 1 region queries) and `searchPois(lat: Double, lng: Double, type: String): List<PlaceResult>` (Level 2 nearby search). `PlaceResult` carries `placeId`, `name`, `photoUrl`, `rating`, `reviewCount`, `address`, `lat`, `lng`. Implementation calls the Google Places New API (v1) Text Search and Nearby Search endpoints. Injected as a Hilt singleton with the API key from `local.properties`. A `FakePlacesApiClient` is provided for ViewModel tests.

- **Region query strings**: Each chip maps to a fixed query sent to Google Places Text Search: Patagonia → `"turismo Patagonia Argentina"`, Cuyo → `"turismo Cuyo Argentina"`, Noroeste → `"turismo Noroeste Argentina"`, Litoral → `"turismo Litoral Argentina"`, Buenos Aires → `"turismo Buenos Aires Argentina"`, Córdoba → `"turismo Córdoba Argentina"`.

- **Place Recommendation ranking — new domain object `PlaceRecommendationRanker`**: Pure function. Input: `List<PlaceResult>`. Output: `RankedRecommendations(top: List<PlaceResult>, others: List<PlaceResult>)`. Top: rating ≥ 4.2 AND reviewCount ≥ 50, sorted descending by `rating × ln(reviewCount)`. Others: everything below either threshold, same sort within the section.

- **`TravelRepository` additions**: `suspend fun setTripDestination(groupId, placeId, name, lat, lng)`; `fun observeItineraryEvents(groupId): Flow<List<ItineraryEvent>>`; `suspend fun createItineraryEvent(...)`; `suspend fun updateItineraryEvent(...)`; `suspend fun deleteItineraryEvent(eventId)`; `fun observeActivePoll(groupId): Flow<Poll?>`; `suspend fun createPoll(groupId, type, expiresAt?): String`; `suspend fun addPollCandidate(pollId, placeId, name, photoUrl): String`; `suspend fun toggleVote(candidateId, memberId)`; `suspend fun closePoll(pollId)`; `suspend fun setPollWinner(pollId, placeId)`.

- **`DestinationViewModel`**: Replace the empty stub. Exposes: `regionResults: StateFlow<UiState<List<PlaceResult>>>`, `searchResults: StateFlow<UiState<List<PlaceResult>>>`, `poisByCategory: StateFlow<UiState<RankedRecommendations>>`, `activePoll: StateFlow<Poll?>`, `tripDestination: StateFlow<TripDestinationState>`. Methods: `selectRegion(region)`, `search(query)`, `setTripDestination(placeResult)`, `selectPoiCategory(category)`. Observes `observeGroups()` for the single group's trip destination changes.

- **`PollViewModel` (new)**: Exposes `poll: StateFlow<Poll?>`, `candidates: StateFlow<List<PollCandidateUiModel>>` (each carrying thumbs-up count and whether the current member has voted). Methods: `addCandidate(placeResult)`, `addCandidates(List<PlaceResult>)`, `toggleVote(candidateId)`, `closePoll()`, `selectWinner(candidateId)`.

- **`ItineraryViewModel` (replace stub)**: Exposes `events: StateFlow<UiState<List<ItineraryEventByDay>>>` where `ItineraryEventByDay` groups events by calendar date. Methods: `createEvent(name, date, time?, description?, placeId?)`, `updateEvent(...)`, `deleteEvent(eventId)`. Pre-fill helper: `buildEventFromPoi(PlaceResult): ItineraryEventDraft` — maps POI name to event name, formatted address to description, placeId to placeId; date left null for the user to fill.

- **Navigation**: Add two new composable routes within the existing inner NavHost: `"itinerary"` (full-screen push from destinations tab) and `"poll_detail"` (full-screen push accessible from the poll banner chip and from Level 1/Level 2 screens). No new bottom nav tab.

- **Realtime**: `itinerary_events` and `group_polls`/`poll_candidates`/`poll_votes` are subscribed via the existing per-table Realtime pattern. Poll vote counts are derived by counting `poll_votes` rows per candidate in the Realtime-updated local state — no separate count column.

- **Google Places Photos**: Photo URLs are fully resolved at fetch time and stored as plain URLs in `poll_candidates.photo_url`. Do not store raw photo references — resolve them when the POI is loaded to avoid re-fetching on every render.

- **Poll winner cross-ViewModel signal**: `PollViewModel.selectWinner()` for an Activity Poll navigates to the itinerary creation screen with the winning POI pre-filled via navigation arguments. Do not couple `PollViewModel` directly to `ItineraryViewModel`.

## Testing Decisions

A good test exercises the external contract of a module — its observable outputs given controlled inputs — without coupling to internal implementation details.

**`PlaceRecommendationRanker` (unit, pure)**
Primary seam for all ranking logic. No I/O, no coroutines. Test cases: top section contains only places meeting both thresholds; Otros section contains places missing either threshold; both sections sorted by rating × ln(reviewCount) descending; empty input returns empty sections; exactly-threshold values (4.2 rating, 50 reviews) land in top section; single below-threshold place appears only in Otros. Prior art: `ExpenseSettlementCalculatorTest`.

**`DestinationViewModel` (unit, via `FakeTravelRepository` + `FakePlacesApiClient`)**
Test cases: selecting a region triggers a Places API query and emits results into `regionResults`; searching emits into `searchResults`; `setTripDestination` persists via repository and updates `tripDestination` state; trip destination change observed from repository propagates to state; selecting a POI category emits ranked recommendations; API error emits `UiState.Error`. Prior art: `ExpenseViewModelTest`, `GroupViewModelTest`.

**`PollViewModel` (unit, via `FakeTravelRepository`)**
Test cases: `addCandidate` calls repository and updates `candidates` state; `toggleVote` adds a vote when not already voted; `toggleVote` removes a vote when already voted; `closePoll` transitions poll state to closed; `selectWinner` on a Destination Poll triggers `setTripDestination`; second poll creation blocked when one is already open. Prior art: `ExpenseViewModelTest`.

**`ItineraryViewModel` (unit, via `FakeTravelRepository`)**
Test cases: `createEvent` persists and appears in `events` state; Realtime update propagates to `events`; events grouped by calendar date with correct day headers; `buildEventFromPoi` maps POI name, address, and placeId correctly with null date; `deleteEvent` removes the event from state. Prior art: `ExpenseViewModelRealtimeTest`.

## Out of Scope

- Weather data (Open-Meteo integration) — deferred to v2.
- OpenTripMap / Foursquare fallback APIs — Google Places is the sole source for MVP (ADR-0003 covers fallback strategy if credits are exhausted).
- Free-form Group Polls with arbitrary question text — only typed Destination and Activity polls are supported (ADR-0009).
- Profile photo support on Poll Candidate cards or Itinerary Event avatars — initials only.
- Offline caching of Places API results — network-only per ADR-0002.
- Collaborative voting on Place Recommendations outside a Group Poll context (v3 per roadmap).
- Travel Documents (v2 per roadmap).
- Device calendar sync for Itinerary Events (v3 per roadmap).
- Push notifications for itinerary changes or poll votes — covered by FCM issue #44.

## Further Notes

- The Places New API (v1) replaces the legacy Places API; use `places.googleapis.com/v1/places:searchText` and `places.googleapis.com/v1/places:searchNearby` endpoints. The API key must be restricted to the Android app's SHA-1 fingerprint in Google Cloud Console.
- The one-active-poll database constraint (unique partial index on `group_polls(group_id) WHERE state = 'open'`) is enforced at the DB level, not just in the ViewModel. The ViewModel should surface a clear error message when the constraint is violated.
- `ItineraryEventByDay` is a UI model, not a domain model — grouping by day is a presentation concern and should live in the ViewModel, not the repository.
- ADR-0008 (one-group-per-user) and ADR-0009 (typed polls only) document the key trade-offs for this module.
