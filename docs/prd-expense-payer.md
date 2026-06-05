## Problem Statement

Members have no way to record who actually paid the bill for an expense group. The current settlement model assumes costs are split equally by default and treats item assignments as offsets from that equal-share baseline. This produces wrong results: when a member assigns items to themselves, the system tells them they owe money to others who consumed less — the opposite of what actually happened at the table. The core bug is that `ExpenseItem` has no `paidByMemberId` field, so the system never knows who fronted the cash.

## Solution

Add a **payer** field to each expense group. The group admin can select any member as payer at any point while the group is open, and can change it before finalization. Once all items are fully assigned and a payer is selected, the admin can finalize the group.

Settlement becomes payer-centric: each non-payer member owes the payer exactly the sum of their assigned item costs. The payer's net is zero (their own consumption is subtracted from what they are owed). The settlement view shows two perspectives — non-payers see what they owe the payer, the payer sees the list of members who owe them.

Finalization is a hard gate that blocks unless both conditions are met:
1. All item quantities are fully assigned (zero unassigned units across all items)
2. A payer is selected

Groups that have no payer set show a prompt banner instead of settlement numbers.

## User Stories

1. As an admin, I want to select which member paid the full bill for an expense group, so that the settlement correctly reflects who is owed money.
2. As an admin, I want to change the payer at any time while the group is open, so that I can correct mistakes before finalizing.
3. As an admin, I want to see a persistent "Who paid?" selector at the top of the expense group detail screen, so that the payer is always visible and easy to update.
4. As an admin, I want the finalize button to be blocked if no payer is selected, so that I cannot produce a meaningless settlement.
5. As an admin, I want the finalize button to be blocked if any item has unassigned quantity, so that no consumption goes unaccounted.
6. As a non-payer member, I want to see a single card saying "You owe [Payer Name] $X", so that I know exactly how much to transfer and to whom.
7. As the payer, I want to see a list of "Member owes you $X" cards for each non-payer, so that I know who still needs to pay me back.
8. As any member, I want the settlement to only appear once a payer is selected, so that I never see confusing equal-split numbers.
9. As any member opening an old expense group with no payer set, I want to see a banner explaining that a payer must be selected, so that I understand why settlement numbers are missing.
10. As an admin, I want the payer selector to show member display names (aliases), so that I can identify group members easily.
11. As an admin, I want the payer selection to persist in real time for all group members, so that everyone sees the updated payer without refreshing.
12. As a non-payer member, I want to confirm I have paid the payer via MercadoPago deep link, so that the payer knows the transfer is done.
13. As the payer, I want to confirm receipt of payment from each member, so that settled debts are marked as complete.

## Implementation Decisions

- **Schema change:** Add `paid_by_member_id UUID REFERENCES group_members(id) ON DELETE SET NULL` (nullable) to the `expense_groups` table. Nullable means not yet selected.
- **Domain model:** `ExpenseGroup` gains `paidByMemberId: String?`. No breaking change to existing serialisation — old rows have null.
- **New repository method:** `setExpenseGroupPayer(expenseGroupId: String, memberId: String)` — updates `paid_by_member_id` on the row.
- **New use case:** `SetExpenseGroupPayerUseCase` — thin delegation to the repository method.
- **Settlement calculator rewrite:** `ExpenseSettlementCalculator.calculate()` gains a `payerMemberId: String?` parameter. When non-null: non-payer members retain their positive assigned amounts (positive = debtor); the payer settlement entry is negative, equal to -(total - payerOwnCost) (negative = creditor). When null: return all-zero settlements and emit no debts.
- **`toNetBalances()` removed:** The equal-split normalisation in `ExpenseViewModel` is deleted. The calculator emits absolute amounts (what each person owes the payer), not net offsets from equal share.
- **`DebtSimplifier` removed:** With one payer all debts are already minimal (N-1 transfers). The class, all usages, and its test file are deleted.
- **`SettlementWarning` semantics:** Unassigned items become a hard finalization blocker surfaced in the UI, not a soft warning shown alongside settlement numbers.
- **Finalization validation:** `ExpenseViewModel.finalizeExpenseGroup()` must check: (a) paidByMemberId is non-null and (b) every item's total assigned quantity equals item.quantity. If either fails, emit an error message and abort without calling the repository.
- **`PeerToPerDebt` reuse:** The existing model and MP-alias deep-link flow are kept. Under the new model all `PeerToPerDebt` entries have `toMemberId == payerMemberId`. The payer vs non-payer perspective is determined by comparing `currentMember.id`.
- **UI — payer selector:** A Material 3 dropdown at the top of the expense group detail card, visible to all members, editable only by the admin when the group is open. Shows the selected member name or a placeholder when null.
- **UI — settlement section:** Hidden (replaced by a banner) when `paidByMemberId == null`. When set: non-payers see their single debt card to the payer; the payer sees one card per non-payer member.
- **Real-time:** `paid_by_member_id` changes propagate via the existing `observeExpenseGroups` Realtime flow — no new channels needed.

## Testing Decisions

Good tests exercise the external contract of a module (inputs to outputs, state transitions visible to callers) without coupling to internal implementation details.

**`ExpenseSettlementCalculator` (unit, pure)**
- Primary seam for all settlement logic — no I/O, no coroutines.
- New test cases: payer gets negative amountCents equal to (total minus their own consumption); non-payer members get positive amountCents equal to their assigned cost; null payer returns all-zero settlements; payer who consumed nothing is owed the full total; single-member group with that member as payer produces zero debt.
- Prior art: `ExpenseSettlementCalculatorTest` — follow the same builder helper pattern.

**`ExpenseViewModel` (unit, via `FakeTravelRepository`)**
- Test payer selection updates `expenseGroups` state; finalization blocked when payer null; finalization blocked when items have unassigned quantity; finalization succeeds when both conditions met; settlement state emits correct per-member debts after payer set.
- Prior art: `ExpenseViewModelTest` and `ExpenseViewModelRealtimeTest`.

## Out of Scope

- Multiple payers per expense group (one payer is the agreed model).
- Tracking partial payments or payment schedules.
- Currency conversion (ARS-only, unchanged).
- Push notifications for payer selection events (covered by FCM PRD #44).
- Splitting unassigned item costs across members (unassigned items must reach zero before finalization).

## Further Notes

- The `DebtSimplifier` class was purpose-built for many-to-many debt graphs. Under the one-payer model it adds no value and should be fully deleted (class, all usages, and its test file) to avoid confusion.
- Old expense groups with `paid_by_member_id = null` should not be auto-migrated to any default payer — they require explicit admin action.
