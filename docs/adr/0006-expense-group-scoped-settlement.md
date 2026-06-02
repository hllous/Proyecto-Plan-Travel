# Expense Group-scoped settlement

Settlements and Peer-to-Peer Debts are calculated per Expense Group, not across the entire Travel Group. Each Expense Group has its own independent total and debt graph.

The alternative — a single global settlement across all Expense Items — was rejected because the product model explicitly groups expenses by event (e.g. "Tonight's Dinner", "Yesterday's Activity"). A global settlement would obscure which event drove a debt, making it harder for members to verify or dispute specific charges. Group-scoped settlement also enables Expense Groups to be Finalized independently, triggering notifications only when a specific event is closed out.
