# Plan Travel

Plan Travel coordinates group travel planning, including group membership, destination recommendations, invitations, and expense splitting.

## Language

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

## Example Dialogue

Planner: "This Expense Item has six tickets, but only four are assigned."

Developer: "Then the two remaining tickets are an Unassigned Quantity. The Member Settlement should include only the four assigned tickets and show a warning for the two unassigned tickets."

Planner: "What happens when the price does not split evenly?"

Developer: "The Member Settlement uses proportional shares and spreads rounding across assigned members by ascending member id."

Planner: "How should the app explain unassigned quantities?"

Developer: "The settlement result carries a Settlement Warning, and the screen chooses the Spanish text."
