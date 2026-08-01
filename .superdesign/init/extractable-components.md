# Extractable components

No component currently meets the extraction threshold. The only React UI is a single administration
page, and its masthead and cards are not reused across multiple routes.

The future mobile shell will need reusable components such as `HouseholdSwitcher`,
`MoneySummaryCard`, `TaskAssignmentCard`, `StatusBadge` and `BottomNavigation`, but these are
specifications rather than source components and must not be represented as existing code.
