# Plan Travel

Plan Travel coordinates group travel planning, including group membership, trip destination setting, itinerary planning, destination recommendations, invitations, expense splitting, and travel safety information.

## Language

### Identity

**User**:
A registered account in Plan Travel, identified by email/password or Google OAuth. Every group participant must be a User — there are no anonymous or name-only members.
_Avoid_: Account, player, participant

**User Profile**:
The public-facing information a User shares across all Travel Groups they belong to: display name, phone number, and profile photo. Set once; visible to all Group Members in any shared group.
_Avoid_: Account details, settings, personal info

**Group Member**:
A User who has joined a specific Travel Group. Always linked to exactly one User via that User's UUID. The role is either ADMIN (the creator) or USER.
_Avoid_: Participant, traveler, person

### Groups & Invites

**Travel Group**:
A named collection of Group Members organized around a single trip. Has one ADMIN and zero or more USER members. Always has a single currency (ARS in MVP). Each User belongs to at most one Travel Group at a time — enforced as a hard database constraint.
_Avoid_: Trip, team, party

**Invite Token**:
A time-limited code that allows a User to join a Travel Group as a Group Member. Consumed once; invalidated on use or expiry.
_Avoid_: Join code, link, QR

**Leave Group**:
The action of a USER-role Group Member voluntarily removing themselves from a Travel Group. Not available to the ADMIN (who must delete the group instead). Distinct from the ADMIN kicking a member.
_Avoid_: Exit group, quit group, remove self

### Trip Planning

**Trip Destination**:
The physical location a Travel Group is traveling to. Set by the ADMIN via the Destinations tab. Stored as a Google Place ID, a human-readable display name, and WGS-84 coordinates (latitude + longitude). The Place ID anchors Place Recommendation queries; the coordinates anchor the Google Places Nearby Search radius for Level 2 POI queries.
_Avoid_: Location, place, city

**Place Recommendation**:
A point of interest suggested for the Trip Destination, sourced from Google Places. Belongs to one of four categories: Alojamiento (lodging), Gastronomía (restaurants/bars/cafes), Actividades (tourist attractions), or Naturaleza (parks/hiking). Displayed in two sections: a top section for places with rating ≥ 4.2 AND reviewCount ≥ 50 (sorted by rating × log(reviewCount) descending), and an "Otros" section below the threshold. Each Place Recommendation carries a name, photo, rating, review count, address, and Google Place ID.
_Avoid_: Activity, suggestion, result

**Group Itinerary**:
The ordered collection of Itinerary Events for a Travel Group. Shared and editable by all Group Members in real time. Accessed via the Destinations tab; not a separate bottom-nav tab.
_Avoid_: Schedule, plan, agenda

**Itinerary Event**:
A single scheduled activity or milestone in the Group Itinerary, with a date, optional time, name, and optional description. Can be created manually or pre-filled from a Place Recommendation via "Añadir al itinerario".
_Avoid_: Activity, appointment, event (too generic)

**Group Poll**:
An optional, ADMIN-created vote among Group Members over a typed set of candidates. Two types: Destination Poll (candidates are Trip Destination options sourced from Level 1) and Activity Poll (candidates are Place Recommendations sourced from Level 2). The ADMIN is prompted to create a poll before committing to a destination or activity, but may skip it. Candidates are added individually or in bulk from the browse screen while the poll is active. The ADMIN creates and closes the poll; any Group Member can add Poll Candidates and cast thumbs-up votes. Only one Group Poll may be active at a time. The poll banner is dismissible by any member and recoverable via a persistent chip on the Destinations tab. An optional expiry date can be set at creation. Once closed by the ADMIN, the app prompts the ADMIN to select the winner and applies the corresponding action (set Trip Destination or add to Group Itinerary).
_Avoid_: Survey, vote, question

**Poll Candidate**:
A single option in a Group Poll, sourced from the Places API. Carries a name, photo, and Place ID. Members can thumb-up any number of Poll Candidates; the thumbs-up count updates live via Realtime.
_Avoid_: Option, choice, item

### Safety & Documents

**Trip Contact**:
A group-level reference entry for emergency or logistical use during the trip: a name, phone number, category (Emergency, Accommodation, Transport, Medical, Other), and optional notes. Belongs to the Travel Group, visible to all Group Members, and editable by any Group Member.
_Avoid_: Emergency contact, phone book, directory

**Travel Document**:
A personal credential stored by a Group Member for their own reference during travel: a document type (DNI, Passport, Travel Insurance, Health Insurance, Driver's License, Vaccination Certificate, or custom), a number or ID, an expiry date, optional notes, and an optional photo attachment. Private by default; the owner can explicitly share it with all members of a specific Travel Group.
_Avoid_: File, attachment, record

### Expenses & Settlement

**Expense Group**:
A named container of Expense Items within a Travel Group, with its own independent settlement and total. Created and deleted by any Group Member. Has exactly two states: Open (editable) or Finalized (read-only).
_Avoid_: Expense category, expense tab, sub-group

**Expense Group State**:
Open or Finalized. An Open Expense Group accepts new Expense Items and assignment changes. A Finalized Expense Group is read-only; finalizing it triggers push notifications to all Group Members for any remaining Unassigned Quantity. Only the ADMIN can finalize an Expense Group.
_Avoid_: Active, closed, archived

**Expense Item**:
A shared purchase or planned cost inside an Expense Group, with a total price and an item quantity.
_Avoid_: Product, line item

**Item Quantity**:
The total count of units represented by an Expense Item.
_Avoid_: Stock, inventory

**Assigned Quantity**:
The part of an Expense Item's Item Quantity that a specific group member consumed or accepted. Total Assigned Quantity for an Expense Item must not exceed its Item Quantity; the UI should prevent invalid assignment changes, and the domain should reject them as a safety net.
_Avoid_: Claimed amount, allocation

**Unassigned Quantity**:
The part of an Expense Item's Item Quantity that has not been assigned to any group member. It remains unassigned and must be shown as a warning; it is not charged to the group admin.
_Avoid_: Admin remainder, leftover charge

**Member Settlement**:
The amount a group member owes for their Assigned Quantity across the Expense Items of a single Expense Group. Rounding cents are spread across assigned members by ascending member id, not assigned to the group admin.
_Avoid_: Balance, bill

**Peer-to-Peer Debt**:
The calculated amount one Group Member owes another, derived by running a debt-simplification algorithm over all Member Settlements within an Expense Group. Produces the minimum number of transfers required to settle all debts. Displayed as directed pairs: "A owes B $X".
_Avoid_: Transfer, payment direction, net balance

**MP Alias**:
The MercadoPago alias stored on a User Profile. Used as the recipient identifier when constructing a Payment Deep Link. Set once globally; shared across all Travel Groups the user belongs to.
_Avoid_: CBU, CVU, account, MP handle

**Payment Deep Link**:
A `mercadopago://` URI pre-filled with the creditor's MP Alias and the Peer-to-Peer Debt amount, opening the MercadoPago app to the send-money screen. Generated automatically from the settled Peer-to-Peer Debt when the debtor taps "Pay via MercadoPago". Payment confirmation remains a manual Payment Status acknowledgement.
_Avoid_: Checkout link, payment URL, MP link

**Settlement Warning**:
Structured information about an Expense Item that cannot be fully settled, such as an Unassigned Quantity and its unassigned amount. UI text is derived from this warning outside the domain.
_Avoid_: Error message, snackbar text

**Assignment Outcome**:
The typed result of an attempt to set an Assigned Quantity for a group member. Either `Accepted` (the assignment was valid and persisted) or `Rejected` with a typed reason (`OverAssigned` or `NegativeQuantity`). Data-integrity failures (e.g. item not found) are not Assignment Outcomes — they are programming errors surfaced as unchecked exceptions.
_Avoid_: assignment result, validation result, error code

**Payment Status**:
Whether a Group Member has marked a Peer-to-Peer Debt as paid. The debtor taps "Pay via MercadoPago" (generating a Payment Deep Link) and then manually confirms; the creditor acknowledges. Does not represent a verified money transfer — it is a shared acknowledgement only.
_Avoid_: Paid flag, balance status, settlement state

## Example Dialogue

Planner: "This Expense Item has six tickets, but only four are assigned."

Developer: "Then the two remaining tickets are an Unassigned Quantity. The Member Settlement should include only the four assigned tickets and show a warning for the two unassigned tickets."

Planner: "What happens when the price does not split evenly?"

Developer: "The Member Settlement uses proportional shares and spreads rounding across assigned members by ascending member id."

Planner: "How should the app explain unassigned quantities?"

Developer: "The settlement result carries a Settlement Warning, and the screen chooses the Spanish text."
