## Problem Statement

The Expense tab currently uses minimal, utilitarian UI patterns that don't reflect the polish of a production app. Creating an Expense Group opens a plain inline card with only a name field; the detail view shows a narrow strip for the total; and Expense Item cards present information in a dense, hard-to-parse layout. Users lack quick shortcuts when naming an Expense Group and cannot see the Expense Group's category context at a glance.

## Solution

Redesign three surfaces in the Expense tab in a single cohesive pass:

1. **Expense Group creation** becomes a full-screen flow with a name field, quick-fill suggestion chips, and a category selector.
2. **Expense Group detail** gains a prominent hero card showing the total, date, and category at a glance.
3. **Expense Item cards** are redesigned with a cleaner layout, stacked assignee avatars, and an Equal Assignment toggle.

## User Stories

1. As a Group Member, I want to create an Expense Group from a full-screen form rather than an inline card, so that the creation flow feels intentional and has room for more structured input.
2. As a Group Member, I want to tap a quick-fill suggestion chip (Combustible, Peajes, Supermercado) to pre-fill the Expense Group name, so that common expense names require zero typing.
3. As a Group Member, I want to select a category (Comida, Transporte, Alojamiento, Entretenimiento, Otros) when creating an Expense Group, so that I can classify the expense event at creation time.
4. As a Group Member, I want the selected category chip to appear visually distinct (filled/primary) while unselected ones appear outlined, so that the active selection is unambiguous.
5. As a Group Member, I want to see the Expense Group's total displayed prominently at the top of the detail screen as a hero card, so that the most important number is immediately visible without scrolling.
6. As a Group Member, I want the hero card to show the Expense Group's creation date alongside a category icon, so that I can confirm I am looking at the right event.
7. As a Group Member, I want a "Detalle de items" section header with an inline "+ Añadir item" text button, so that adding a new Expense Item feels integrated with the list rather than detached in a floating button.
8. As a Group Member, I want each Expense Item card to show the item name and a quantity stepper on the same top row, so that the most actionable control is immediately reachable.
9. As a Group Member, I want each Expense Item card to show the item's total price directly below the item name, so that cost is visible without expanding or scrolling.
10. As a Group Member, I want the "ASIGNADO A:" row on each Expense Item card to show stacked overlapping avatar circles for each Group Member who has an Assigned Quantity, so that I can see at a glance who has claimed units.
11. As a Group Member, I want a "+" button at the end of the assignee avatar stack, so that I can tap it to increment my own Assigned Quantity directly from the card.
12. As a Group Member, I want each Expense Item card to have a blue left accent border, so that the item cards are visually distinct from other card types in the list.
13. As a Group Member, I want a "Dividir todo por igual" toggle above the items list, so that I can auto-distribute equal Assigned Quantities to all Group Members in a single tap.
14. As a Group Member, I want the equal-split toggle to carry a subtitle "Asigna partes iguales a todos", so that its behavior is described inline without needing help text elsewhere.
15. As a Group Member, I want the Expense Group creation FAB label to read "Nuevo gasto" instead of "Nuevo grupo", so that the terminology matches the new creation screen title.
16. As a Group Member, I want the creation screen top bar to read "Agregar Gasto" with a back arrow, so that I can navigate back without losing context.
17. As a Group Member, I want the hero card to display a subtle decorative background shape in the bottom-right corner, so that the card has visual depth without heavy graphics.
18. As a Group Member, I want the progress bar and "Mis unidades" surface removed from Expense Item cards, so that the card surface is focused on the new stacked-avatar assignment UI.

## Implementation Decisions

- **Expense Group Category**: A new `category` field (nullable string, values: comida, transporte, alojamiento, entretenimiento, otros) is added to the `expense_groups` table as a Supabase migration. The `ExpenseGroup` domain model gains `category: String?`. The repository create/read methods pass this field through. Default is `null` for existing rows.

- **Expense Group creation date**: The `expense_groups` table already has a Postgres `created_at` timestamp. The `ExpenseGroup` domain model gains `createdAt: kotlinx.datetime.Instant`. The hero card formats this as the display date.

- **Full-screen creation flow**: The current state-nav pattern (a Boolean flag in the composable) is extended: when `createExpenseGroupExpanded` is true, a `CreateExpenseGroupScreen` composable replaces the entire list screen content. No NavController change is required, consistent with how the existing group drill-in works.

- **Quick-fill chips**: A fixed set of suggestion strings (Combustible, Peajes, Supermercado). Tapping one calls `onNameChange(suggestion)`. No backend involvement.

- **Category chips**: A sealed class or enum `ExpenseGroupCategory` with entries mapping to a display label and a Material icon vector. Rendered as a `LazyRow` of `FilterChip`s; the selected chip uses filled/primary colors, unselected use outlined style.

- **Hero card decorative blob**: A large `Box` with `clip(CircleShape)` positioned with negative offset into the bottom-right corner of the card, filled with a low-opacity primary color. Pure composable; no asset needed.

- **Equal Assignment toggle**: Client-side only. When toggled ON, the composable calls `onAssignQuantity` for every Group Member with `floor(item.quantity / members.size)`. No new backend field; the toggle state is `rememberSaveable` and does not persist across sessions.

- **Stacked avatar circles**: Rendered as a `Box` with each subsequent avatar offset by `-8.dp` horizontally. Each circle uses the existing `memberColor` and `memberInitial` utilities, plus an outlined ring using `border(2.dp, surfaceColor, CircleShape)` to create separation. No profile photos in this pass.

- **Quantity stepper in item card**: Reuses the existing `StepperButton` composable; styled in a pill-shaped outlined container using `Surface` with `RoundedCornerShape(50)` and `border`.

- **Left accent border on item card**: Implemented as a `Row` where the first child is a `Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(primaryColor))` before the main card content column. The outer container uses `RoundedCornerShape(18.dp)` with `clip`.

- **No NavController changes**: All navigation remains state-based within `ExpenseScreen` and its sub-composables, consistent with ADR-0002 and the existing drill-in pattern.

- **"Pagado por" section**: Explicitly excluded from this redesign.

## Testing Decisions

- The Equal Assignment calculation (distribute Item Quantity evenly among N Group Members) is pure logic and should be unit-tested as a standalone function. Verify floor division and that the sum of Assigned Quantities does not exceed Item Quantity.
- The `ExpenseGroupCategory` label/icon mapping should be tested to confirm every entry has a non-null icon and a non-empty display label.
- UI smoke: composable preview tests for `CreateExpenseGroupScreen`, `ExpenseHeroCard`, and the redesigned `ExpenseItemCard` covering empty state, fully assigned, and partially assigned cases.
- Prior art: existing settlement unit tests in the domain layer are the model for pure-logic tests.

## Out of Scope

- Profile photo support for "ASIGNADO A:" avatars — initials only in this pass.
- Persisting the "Dividir todo por igual" toggle state to the server or across sessions.
- Editing an Expense Item's category or name after creation.
- The "Pagado por" section shown in the reference design.
- Any changes to settlement logic, Peer-to-Peer Debt display, or the bottom settlement panel.
- Push notifications triggered by the new category field.

## Further Notes

- Reference images show a blue-dominant palette matching the app's existing `primary` color. All new components should derive colors from `MaterialTheme.colorScheme` to support dark mode automatically.
- The Fraunces font family already used for monetary amounts and headings should continue for the hero card's total amount display.
- The domain glossary does not currently define a term for "Expense Group Category". If this concept expands (e.g. filtering by category across Expense Groups), consider adding it to `CONTEXT.md`.
