# SharedHouse Horizon design system

## Direction

SharedHouse Horizon is a calm, operational interface for people who share a home. It replaces the former neon-violet treatment with **deep evergreen, teal and sky** surfaces that feel organised without becoming corporate or financial. The visual hierarchy is built for the questions a member asks every day: *What needs attention? What do I owe? What do I need to do next?*

The Android source of truth is `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/theme/` and the shared Foundation primitives in `ui/atmosphere/`. The product-owned system deliberately avoids a runtime dependency on a third-party material theme.

## Product principles

| Principle | Application |
|---|---|
| **Calm clarity** | One primary outcome per screen. Supporting totals, activity and actions should remain quiet until needed. |
| **Trust before decoration** | A visual accent must never imply that a payment, invitation, message or action has been confirmed by the server. |
| **Human household tone** | The product feels like a well-kept shared space, not a banking portal, task board or social feed. |
| **Accessible by default** | Status always uses icon + text + colour; touch targets are at least 48 × 48 dp and text is never clipped at large font scales. |
| **Practical depth** | Cards and navigation use restrained contrast, one soft highlight and a low contact shadow. They do not claim real backdrop blur. |

## Colour system

### Dark Horizon theme

| Role | Token | Value |
|---|---|---:|
| App canvas | `Base` | `#0D1714` |
| Ambient wash | `HomeGlow` | `#123D35` |
| Primary card | `CardLevel1` | `#14211D` |
| Raised card | `CardLevel2` | `#1B2B26` |
| Standard border | `Border` | `#2B3D36` |
| Active border | `ActiveBorder` | `#3F7564` |
| Action / focus | `AccentPrimary` | `#2DD4BF` |
| Orientation accent | `AccentSecondary` | `#38BDF8` |
| Main text | `TextPrimary` | `#F2FBF6` |
| Secondary text | `TextSecondary` | `#A8BCB3` |

The single hero on a screen moves from deep teal (`#0F766E`) through teal (`#14B8A6`) to sky (`#38BDF8`). Amber is reserved for attention, while rose remains a negative/error accent. Device-derived dynamic colour remains intentionally disabled so it cannot change financial or status meaning.

### Light Horizon theme

Light mode uses a matte paper canvas (`#F4F7F4`), white primary cards, pale mineral-green secondary surfaces (`#E8F0EC`) and dark-green text (`#17231E`). It is not a low-contrast inverse of dark mode. The same semantic labels, icons and status states apply in both modes.

### Status semantics

| Meaning | Token | User-facing use |
|---|---|---|
| Neutral | `StatusNeutral` | Draft, scheduled, informational |
| Positive | `StatusPositive` | Confirmed, completed, live |
| Attention | `StatusAttention` | Due soon, pending, reconnecting |
| Negative | `StatusNegative` | Overdue, disputed, failed sync |
| Disabled | `StatusDisabled` | Waived, cancelled, unavailable |

Every status must remain understandable in monochrome: a visible label and relevant icon are required alongside colour.

## Type, shape and spacing

The type scale uses the platform sans family, with tabular figures for monetary values. A `46sp` display is reserved for the one decisive number on a screen. Headings are 23–31sp, while body text remains 14–16sp with generous line heights. Do not promote multiple metrics to display size.

Controls use a compact `10–18dp` radius scale; standard cards use `26dp` and the hero uses `32dp`. Buttons use `18dp`, filters remain pill-shaped and modal sheets use rounded top corners. The standard visual rhythm is based on 4, 8, 12, 16, 20 and 24dp spacing.

## Core components

| Component | Horizon behaviour |
|---|---|
| **Ambient background** | Deep evergreen canvas with quiet teal light from the upper edge and a very low amber pool below. |
| **Hero card** | Exactly one per primary screen; teal–sky gradient, 32dp corners, restrained glow and a clear primary outcome. |
| **Cards** | Matte, layered surfaces with a soft top highlight, low shadow and subtle outline. Avoid decorative gradients inside dense lists. |
| **Primary action** | Teal–sky gradient, high-contrast label, tactile 120ms compression and no false completion state. |
| **Input** | Raised surface, concise label, clear focus outline and room for supporting/error text. |
| **Navigation dock** | A compact elevated island with opaque-enough background for legibility. The selected destination has a 52 × 38dp teal–sky indicator, label and explicit semantics. |
| **Icon badge** | Circular, independently lit badge; domain icon uses semantic tint rather than arbitrary colour. |

## Screen hierarchy

**Home** gives a short greeting, one trusted overview, an obvious household conversation entry and grouped quick actions. **Money** gives the personal outstanding balance dominance without making payment declaration look like money movement. **Tasks** separates mine, overdue and requests according to risk. **Calendar** uses type, icon, shape and label for period/event meaning, not a coloured dot. **House** separates household identity, members and settings. **Chat** preserves sender, timestamp, connection state and failure/retry information.

## Motion and accessibility

Selected navigation, filters and action surfaces use short spring responses only when reduced-motion is disabled. Cards compress to `0.985`, buttons to `0.97` and icon controls to `0.94`; final financial values never wait for animation. All custom overlays honour safe drawing insets and the software keyboard. Maintain semantic headings, live regions for status messages and accessible content descriptions for icon-only controls.

## Implementation boundary

The redesign changes **presentation only**. Existing server states, permissions, financial audit behaviour, localisation keys, accessibility semantics and the product’s restriction against moving user money are preserved. A subsequent feature must extend the reusable theme/primitives first rather than recreating local colours, card treatments or navigation behaviour in a screen.
