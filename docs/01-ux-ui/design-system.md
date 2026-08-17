# SharedHouse Cupertino design system

## Direction

SharedHouse Cupertino is a content-first mobile interface for people who share a home. It uses grouped surfaces, system-blue actions, carefully restrained depth and a calm typographic hierarchy inspired by polished native mobile experiences. The product should make the next useful step evident without turning household coordination into a financial dashboard or a decorative social feed.

The Android source of truth is `apps/android/app/src/main/kotlin/com/sharedhouse/android/ui/theme/` together with the shared Foundation primitives in `ui/atmosphere/`. The product-owned system deliberately avoids a runtime dependency on a third-party material theme.

## Product principles

| Principle | Application |
|---|---|
| **Content leads** | A screen has one primary value or action. Supporting information is grouped and visually quieter. |
| **Native calm** | Surfaces are neutral, controls are familiar, and motion is fast and subtle rather than ornamental. |
| **Trust before style** | Blue highlights an available action; it must never imply that payment, invitation, message or sync has already completed. |
| **Accessible by default** | Status combines label, icon and colour; touch targets are at least 48 × 48 dp; text must accommodate large font scales. |
| **Light and dark parity** | Both themes preserve the same hierarchy and semantic state meaning, rather than treating dark mode as a filtered light theme. |

## Experience hierarchy

Every primary surface should answer three questions in order: **what needs attention now, what can I do next, and what can wait**. Home leads with the balance or priority state, then the four actions members use most often: add an expense, add a task, view the calendar and open household settings. Calendar appears before chat and workspace content because it directly affects the current day; chat remains available as a concise continuity card rather than competing with the next action.

The money and tasks areas use the same pattern. A personal summary or attention count appears first, then filters and operational rows. Each row presents a readable title, one key supporting detail and an explicit status; secondary metadata is delayed or collapsed. Controls that lead to a distinct outcome use solid blue, while management and recovery routes use tonal or outline controls.

## Colour system

### Grouped surfaces

| Role | Light | Dark | Intended use |
|---|---:|---:|---|
| App canvas | `#F2F2F7` | `#000000` | Primary background and grouped-page canvas |
| Standard card | `#FFFFFF` | `#1C1C1E` | Cards, settings groups and dialogs |
| Secondary surface | `#EFEFF4` | `#2C2C2E` | Inputs, segmented-control tracks and low-priority panels |
| Separator / outline | `#C6C6C8` | `#38383A` | Subtle separation; never a dominant border |
| Primary action | `#007AFF` | `#0A84FF` | Main buttons, selected tab and actionable links |
| Tinted action | `#D9ECFF` | `#003F7D` | Secondary action panels and selected filters |

System blue is the only general-purpose action accent. Violet and cyan remain optional domain accents, not decoration. Device-derived dynamic colour remains disabled so it cannot alter financial or status semantics.

### Status semantics

| Meaning | Light token | Dark token | User-facing use |
|---|---:|---:|---|
| Neutral | `#8E8E93` | `#8E8E93` | Draft, scheduled, informational |
| Positive | `#34C759` | `#30D158` | Confirmed, completed, live |
| Attention | `#FF9500` | `#FF9F0A` | Due soon, pending, reconnecting |
| Negative | `#FF3B30` | `#FF453A` | Overdue, disputed, failed sync |
| Disabled | `#8E8E93` | `#636366` | Waived, cancelled, unavailable |

Every state remains understandable without colour through a visible label and relevant icon.

## Type, shape and spacing

The type scale uses the platform sans family and keeps tabular figures for money. A `40sp` display is reserved for the one decisive value on a screen. Headlines range from 20–28sp, with 14–16sp body copy using short, readable line heights. Do not assign display styling to several values at once.

Controls use an `8–14dp` radius scale. Standard cards use `18dp`, feature cards use `22dp`, and modal sheets use `22dp` rounded top corners. The spacing rhythm is based on 4, 8, 12, 16, 20, 24 and 28dp. A screen should favour a few deliberate groups over a large collection of individually outlined boxes.

## Core components

| Component | Cupertino behaviour |
|---|---|
| **Grouped background** | Neutral system canvas with no ambient gradient or decorative light pool. |
| **Feature card** | One optional, system-blue card per primary screen. It uses a solid surface, white type and restrained depth. |
| **Cards** | White or dark-grouped panels with a subtle separator. Dense lists should use grouped rows rather than nested cards. |
| **Primary action** | Solid system-blue control, 50dp high, 14dp radius, short press compression and an immediate opacity response; no gradient. |
| **Tonal action** | Blue-tinted panel with dark-blue text; reserved for a useful secondary route. |
| **Inputs** | Secondary grouped surface with concise label, focused outline and room for support/error text. |
| **Tab bar** | Screen-edge tab bar with a soft top contour, system-blue selected icon and label, and no floating island indicator. |
| **Icon tile** | Compact rounded-square tile with semantic tint and no decorative shine. |
| **Segments and filters** | Grouped track with a surface-selected state; no coloured gradient or exaggerated shadow. |

## Screen hierarchy

**Home** introduces the household and one balance/action priority, then groups fast actions and recent information. **Money** gives the personal outstanding balance clear prominence without making payment declaration appear as money movement. **Tasks** separates mine, overdue and requests according to risk. **Calendar** uses labels and familiar hierarchy for period/event meaning. **House** separates household identity, members and settings into calm groups. **Chat** preserves sender, timestamp, connection state and retry information ahead of decoration.

The welcome experience starts with a compact brand tile, a clear headline and two native-feeling actions. Primary registration remains system blue; signing in remains an outline or text route so the first decision is unambiguous.

## Motion and accessibility

Motion is limited to short opacity, colour and scale responses when reduced motion is not enabled. Interactive cards compress to `0.985`, buttons to `0.98` and icon controls to `0.94`; financial values never wait for animation. All custom overlays honour safe drawing insets and the software keyboard. Maintain semantic headings, live regions for status messages and accessible descriptions for icon-only controls.

## Implementation boundary

The redesign changes **presentation only**. Existing server states, permissions, financial audit behaviour, localisation keys, accessibility semantics and the product restriction against moving user money remain preserved. New features must extend reusable theme primitives before creating screen-local colour, card, button or navigation treatments.
