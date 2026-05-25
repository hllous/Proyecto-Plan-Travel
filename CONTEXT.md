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
A named collection of Group Members organized around a single trip. Has one ADMIN and zero or more USER members. Always has a single currency (ARS in MVP).
_Avoid_: Trip, team, party

**Invite Token**:
A time-limited code that allows a User to join a Travel Group as a Group Member. Consumed once; invalidated on use or expiry.
_Avoid_: Join code, link, QR

**Leave Group**:
The action of a USER-role Group Member voluntarily removing themselves from a Travel Group. Not available to the ADMIN (who must delete the group instead). Distinct from the ADMIN kicking a member.
_Avoid_: Exit group, quit group, remove self

### Trip Planning

**Trip Destination**:
The physical location a Travel Group is traveling to. Set by the ADMIN at group creation or later. Used as the anchor for destination recommendations, Place Recommendations, and weather data.
_Avoid_: Location, place, city

**Place Recommendation**:
A point of interest (landmark, activity, restaurant, etc.) suggested for the Trip Destination, sourced from an external places API (Google Places primary; OpenTripMap / Foursquare as fallback).
_Avoid_: Activity, suggestion, result

**Group Itinerary**:
The ordered collection of Itinerary Events for a Travel Group. Shared and editable by all Group Members.
_Avoid_: Schedule, plan, agenda

**Itinerary Event**:
A single scheduled activity or milestone in the Group Itinerary, with a date, optional time, name, and optional description.
_Avoid_: Activity, appointment, event (too generic)

### Safety & Documents

**Trip Contact**:
A group-level reference entry for emergency or logistical use during the trip: a name, phone number, category (Emergency, Accommodation, Transport, Medical, Other), and optional notes. Belongs to the Travel Group, visible to all Group Members, and editable by any Group Member.
_Avoid_: Emergency contact, phone book, directory

**Travel Document**:
A personal credential stored by a Group Member for their own reference during travel: a document type (DNI, Passport, Travel Insurance, Health Insurance, Driver's License, Vaccination Certificate, or custom), a number or ID, an expiry date, optional notes, and an optional photo attachment. Private by default; the owner can explicitly share it with all members of a specific Travel Group.
_Avoid_: File, attachment, record

### Expenses & Settlement

**Expense Item**:
A shared purchase or planned cost inside a travel group, with a total price and an item quantity.
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
The amount a group member owes for their Assigned Quantity across the group's Expense Items. Rounding cents are spread across assigned members by ascending member id, not assigned to the group admin.
_Avoid_: Balance, bill

**Settlement Warning**:
Structured information about an Expense Item that cannot be fully settled, such as an Unassigned Quantity and its unassigned amount. UI text is derived from this warning outside the domain.
_Avoid_: Error message, snackbar text

**Assignment Outcome**:
The typed result of an attempt to set an Assigned Quantity for a group member. Either `Accepted` (the assignment was valid and persisted) or `Rejected` with a typed reason (`OverAssigned` or `NegativeQuantity`). Data-integrity failures (e.g. item not found) are not Assignment Outcomes — they are programming errors surfaced as unchecked exceptions.
_Avoid_: assignment result, validation result, error code

**Payment Status**:
Whether a Group Member has marked their Member Settlement as paid. Set by the member themselves; confirmed by the group ADMIN. Does not represent a real money transfer — it is a shared acknowledgement only.
_Avoid_: Paid flag, balance status, settlement state

## Example Dialogue

Planner: "This Expense Item has six tickets, but only four are assigned."

Developer: "Then the two remaining tickets are an Unassigned Quantity. The Member Settlement should include only the four assigned tickets and show a warning for the two unassigned tickets."

Planner: "What happens when the price does not split evenly?"

Developer: "The Member Settlement uses proportional shares and spreads rounding across assigned members by ascending member id."

Planner: "How should the app explain unassigned quantities?"

Developer: "The settlement result carries a Settlement Warning, and the screen chooses the Spanish text."
