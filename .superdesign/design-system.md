# SharedHouse design system

## Product context

SharedHouse is a privacy-first household coordination product for adult shared homes. It brings
money obligations, chores, shopping, calendar items and requests into one calm workspace. It records
payment declarations but does not move money.

The first mobile foundation screen is Home. It should communicate the future information hierarchy
without pretending that authentication, household data, balances or sync are configured.

## Experience principles

- Calm, trustworthy and practical.
- Clear responsibility without blame or public shaming.
- Household warmth without childish illustration or gamification.
- Financial clarity without banking or debt-collection imagery.
- Native Android and iOS behaviour with a shared visual character.
- Accessible by default: large text, high contrast, visible focus, screen-reader semantics and
  reduced motion.

## Visual language

Use a quiet sage and deep-forest palette taken from the existing administration shell.

### Light

- Background: `#F2F5F2`
- Surface: `#FFFFFF`
- Primary text: `#17231D`
- Secondary text: `#435248`
- Brand/action: `#3E654F`
- Border: `#C8D3CA`
- Focus/attention accent: `#755C00`

### Dark

- Background: `#111914`
- Surface: `#19241D`
- Primary text: `#ECF4EE`
- Secondary text: `#B4C6BA`
- Brand/action: `#9AC9AC`
- Border: `#35453B`

Semantic positive, attention and negative colours must remain distinguishable from the selectable
accent and must always be paired with an icon and text label.

## Typography

Use the native platform system typeface: Roboto/system on Android and San Francisco/system on iOS.
Do not introduce a decorative font. Use tabular figures for monetary totals. Support large text
without truncating totals, dates or primary actions.

Suggested hierarchy:

- Screen title: platform large title / 28–34sp equivalent, bold.
- Personal total: 36–44sp equivalent, bold with tabular figures.
- Section title: 18–20sp equivalent, semibold.
- Body: 16sp equivalent.
- Supporting label: 13–14sp equivalent, medium.

## Shape, spacing and elevation

- Spacing rhythm: 4, 8, 12, 16, 24, 32 and 48 logical pixels.
- Mobile page horizontal inset: 20 logical pixels.
- Cards: 16 logical pixel radius.
- Pills and status badges: full or 12 logical pixel radius depending on platform convention.
- Light theme cards may use a restrained green-tinted shadow.
- Dark theme uses borders and tonal surfaces instead of large shadows.
- Touch targets meet native minimums and primary actions remain reachable one-handed.

## Mobile Home foundation structure

1. Compact top app bar with SharedHouse identity and a profile placeholder.
2. Household selector labelled as a demo or not-configured state.
3. Personal overview card that clearly says setup is not complete; it must not show a fabricated
   debt or payment.
4. Two next-action cards: next due item and next household task, both with honest placeholder state.
5. Pending requests/household notice section, empty with a useful explanation.
6. Stable five-item bottom navigation: Home, Calendar, Money, Tasks and House.

Use “Not configured” or “Set up your household” for unfinished capability. Never display fake paid,
overdue, subscription or sync-success states.

## Core components

- Household switcher
- Cycle selector
- Money summary card
- Due-item row
- Avatar and avatar stack
- Task assignment card
- Calendar event chip
- Status badge with icon and text
- Empty, loading, offline, denied, conflict and error states
- Permission education sheet
- Destructive action confirmation

## Motion and feedback

Use short platform-native transitions for navigation and state changes. Respect reduced-motion
preferences. Never animate a total in a way that delays or obscures its final value. Haptics may
supplement but never replace visible and spoken feedback.

## Localisation

English is the source language and Romanian is required. Layouts must tolerate longer Romanian copy.
Do not concatenate sentences. Currency, date, time and first-day-of-week formatting are locale-aware
and independent from the household currency/timezone.

## Hard constraints

- Do not use colour alone to communicate status.
- Do not imitate government emergency alerts.
- Do not use “Pay” when the action only records a payment; use “Record payment”.
- Do not imply that platform staff can see household content.
- Do not fabricate data in foundation or empty states.
- Use only the fonts, colours, spacing and component styles in this document.
