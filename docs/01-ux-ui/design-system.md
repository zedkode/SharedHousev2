# UI Design System

## SharedHouse premium v2

SharedHouse uses a product-owned Compose design system rather than stock Material components. The
visual character is precise, contemporary and slightly playful in low-risk moments, while money, access,
sync and destructive actions remain direct. Visual decoration must never imply that a payment,
message delivery or household action occurred when the server has not confirmed it.

The Android implementation source of truth is `ui/theme/` plus the Foundation primitives in
`ui/atmosphere/`. External design generators are not runtime or source dependencies.

## Exact colour tokens

The authored dark experience uses these fixed tokens:

| Role | Value |
|---|---:|
| Base background | `#0B0C16` |
| Home ambient wash | `#1A1233` |
| Card level 1 | `#15162B` |
| Card level 2 | `#1B1D3A` |
| Standard border | `#2A2B45` |
| Active border | `#3D2E6B` |
| Hero gradient start | `#7C3AED` |
| Hero gradient middle | `#A855F7` |
| Hero gradient end | `#EC4899` |
| Alternate gradient | `#3B82F6` → `#8B5CF6` |
| Primary text | `#F5F5FA` |
| Secondary text | `#9599B8` |

The accessible light fallback uses `#F7F7FC` background, `#FFFFFF` level-1 cards,
`#F0EFFE` secondary surfaces, `#17172B` primary text and `#5F6380` secondary text. Primary violet,
blue and pink accents remain brand-authored. System light/dark selection is supported. The dynamic
colour parameter is retained for source compatibility but is intentionally ignored: a device
palette may not replace the product palette or alter financial/status meaning.

High-contrast mode strengthens foregrounds and outlines without changing the semantic category of
content.

## Status semantics

| Status | Dark token | Examples |
|---|---:|---|
| Neutral | `#6B7094` | scheduled, draft, informational |
| Positive | `#22C55E` | confirmed, completed, live |
| Attention | `#F59E0B` | due soon, pending, reconnecting |
| Negative | `#F43F5E` | overdue, disputed, failed sync |
| Disabled | `#4B4F6B` | waived, cancelled, unavailable |

The light fallback uses `#5A5F82`, `#16883F`, `#9B5C00`, `#D82D4E` and `#777B93` respectively.
Every status requires an icon, a visible text label and an accessible description. Colour is only
supporting evidence; never encode state through red/green or opacity alone.

## Typography

The current implementation uses the platform Sans family with a product-owned scale. It does not
claim a bundled display font. Money uses tabular figures.

| Token | Size / line height | Weight |
|---|---:|---|
| Display medium | 48 / 52 sp | Extra bold |
| Display small | 40 / 45 sp | Extra bold |
| Headline large | 32 / 38 sp | Bold |
| Headline medium | 28 / 34 sp | Bold |
| Headline small | 24 / 30 sp | Bold |
| Title large | 18 / 24 sp | Semi-bold |
| Title medium | 16 / 22 sp | Semi-bold |
| Title small | 15 / 20 sp | Semi-bold |
| Body large | 16 / 24 sp | Regular |
| Body medium | 14 / 20 sp | Regular |
| Body small | 12 / 18 sp | Regular |
| Label large | 14 / 20 sp | Semi-bold |
| Label medium | 12 / 17 sp | Medium |
| Label small | 11 / 15 sp | Medium |

Text must scale without clipping or hiding actions. Use a dominant number only for the screen's
primary financial value; do not make every metric a hero.

## Shape, depth and density

- Shape radii are 8, 12, 16, 20 and 28 dp. A screen may use a deliberately asymmetric bubble or
  action shape, but repeated content must stay coherent.
- Use at most one 28 dp hero surface per screen. The current hero uses a 135-degree
  violet–purple–pink gradient, 20 dp content padding and a diffuse 20 dp shadow.
- Level-1 cards sit directly above the background. Level-2/actionable cards use the stronger
  surface, a violet-to-border 1 dp outline and an 8 dp default shadow.
- Information-first screens use compact rows and grouped sections. Do not restore equal-height card
  grids when a list, metric strip or agenda communicates more in the first viewport.
- Interactive targets remain at least 48 × 48 dp, including icon-only actions.
- Content and custom overlays respect safe drawing insets and the software keyboard. Floating
  actions may not cover ledger rows, calendar content or bottom navigation.

### Blur limitation

The current Android implementation does **not** provide true backdrop blur. Compose blur effects
normally blur a component's own rendered content, while reliable sampling of content behind a
surface varies by Android version, renderer and device cost. Therefore the floating dock and
glass-like cards simulate depth with an 85–94% surface alpha, a subtle white wash, a 1 dp outline
and coloured/black shadows. Documentation, screenshots and release notes must call this a
translucent layered surface, not frosted glass or real background blur.

Do not add a blur dependency merely for decoration. A future backdrop implementation requires an
API/performance fallback, large-text/high-contrast review and physical-device evidence.

## Iconography

Primary navigation, household domains and status actions use the SharedHouse vector family:
24 × 24 viewboxes, 1.8 dp rounded strokes, rounded joins and monochrome paths that accept semantic
tint. Domain glyphs include Home, Calendar, Money, Tasks, House, Chat, Rent, Maintenance, Utilities
and Cleaning; status/action glyphs include Approved, Pending, Reversed, People, Add and More.

Prefer the domain-specific glyph over a generic clipboard, flag or wallet. Icons do not replace
labels for status, destructive actions or unfamiliar navigation.

## Navigation and motion

The phone navigation is a floating 32 dp dock, not a stock navigation bar. It uses a 48 × 36 dp
gradient indicator, custom icons and an animated spring transition. Selection remains explicit in
semantics and text. Large layouts use the equivalent product-owned rail.

Chips and segmented controls animate selected colour, depth and scale. Calendar transitions, task
completion and navigation may use short springs. Reduced-motion mode replaces these transitions
with immediate state changes. Never animate totals in a way that delays or obscures their final
value.

## Core component behaviour

- **Home:** one clear greeting, a prioritized overview, a distinct chat entry and hierarchical quick
  actions. Due, task, event and request modules remain independently actionable.
- **Money:** the user's authoritative outstanding share is the hero. Expenses use category icons,
  status icon + label, exact minor-unit formatting and drill-down for allocations, actors and audit
  history. Payment history keeps the declarer visible separately from a later confirmation,
  dispute or reversal actor.
- **Tasks:** Mine, Overdue and Requests are not visually equal when risk differs. Task cards retain
  explicit status text and expose only server-permitted actions. Request history names the
  server-provided requester and, after resolution, the separate decision actor.
- **Calendar:** period selection, current day, selected day and event type use shape + icon + text,
  not a coloured dot alone. The selected-day agenda consumes otherwise empty space.
- **House:** household identity, member access and configuration form separate hierarchy levels.
  Member initials/avatars are distinguishable; three-dot actions are capability-derived.
- **Chat:** consecutive messages may group by sender and time. Sender, avatar, localized timestamp
  and date divider remain visible. Connection status reflects the real stream state. A failed send
  keeps the draft and offers reconnection; no read/delivery badge is shown without a server model.

The reusable catalogue still includes household switcher, cycle selector, money summary, due-item
row, avatar stack, task assignment, calendar event chip, split visualiser, payment declaration,
help/swap/postpone sheet, shopping row, permission education, destructive confirmation and
audit/history timeline.

## Empty, loading, offline and error states

Every empty state explains why it is empty and provides only actions the current role can perform.
Loading keeps existing authoritative content when a silent refresh is in progress. Errors
distinguish offline, permission, validation, conflict and server failure without exposing raw
backend details.

Money and membership corrections must describe their consequences. “Mark as paid” records a
declaration; it does not move money. Expense removal is a reasoned reversal, and editing creates a
linked revision rather than rewriting history.
