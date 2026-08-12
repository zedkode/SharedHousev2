# SharedHouse atmospheric design system

## Product context

SharedHouse is a privacy-first coordination app for adult shared homes. It combines household
finances, recurring bills, chores, calendar events, member requests, notifications and live chat.
It records payment declarations and confirmations but never pretends to move or hold money.

## Experience principles

- Calm, intimate and highly legible: a quiet household control room, not a banking dashboard.
- Dark-first and atmospheric, inspired by the supplied reference without copying its brand.
- One-handed actions, large touch targets and progressive disclosure for dense household data.
- Clear responsibility without blame, public shaming or alarming visuals.
- Accessible with large text, screen readers, high contrast and reduced motion.
- Every empty, loading, offline, denied, conflict and failed-sync state is honest and actionable.

## Core palette

The default experience is dark. Do not use dynamic Material colors or Material 3 components.

- Night background: `#111820`
- Deep atmospheric blue: `#1B2A39`
- Low horizon blue: `#31475A`
- Glass panel: `#252724` at 88% opacity
- Raised glass: `#3A3A37` at 82% opacity
- Circular action: `#484845` at 86% opacity
- Hairline border: `#FFFFFF` at 10% opacity
- Primary text: `#F5F1EA`
- Secondary text: `#C7C5BF`
- Muted text: `#969B9E`
- Warm focus: `#D9CDBD`
- Positive: `#A9D7B5`
- Attention: `#E2C78C`
- Negative: `#F0A7A2`
- Information: `#A8C7DF`

High-contrast mode deepens the background to `#080C11`, makes primary text white and increases
hairlines to 28% white. Status colors are always paired with an icon and text.

## Atmospheric background

Use a full-screen layered gradient: `#111820` at the top, `#26394A` around 34%, then `#161A1D`
at the bottom. Add only soft, abstract blue-gray light blooms; never use personal photographs or
literal house imagery. Content remains readable when transparency is disabled.

## Typography

Use the native platform system typeface: Roboto/system on Android and San Francisco/system on iOS.
Use relaxed line height and slightly reduced letter spacing for large headings. Monetary figures use
tabular figures. Never truncate amounts, due dates or primary actions.

- Hero / amount: 36-44sp, regular or medium.
- Screen title: 28-32sp, medium.
- Section title: 20-22sp, medium.
- Card title: 17-18sp, medium.
- Body: 16sp, regular.
- Supporting label: 13-14sp, medium.
- Navigation label: 11-12sp, medium.

## Shape, spacing and depth

- Spacing rhythm: 4, 8, 12, 16, 20, 24, 32 and 40 logical pixels.
- Mobile horizontal inset: 20 logical pixels.
- Main glass panel radius: 32 logical pixels.
- Standard glass card radius: 24 logical pixels.
- Input and action pill radius: full capsule.
- Circular quick action diameter: 72 logical pixels, minimum touch target 56.
- Use translucent fills, a one-pixel light hairline and layered dark shadows (8-24dp equivalent);
  raised panels also receive a faint top-edge highlight to create restrained three-dimensional depth.
- Selected controls use the solid warm-focus fill with dark `#171717` content, a stronger border and
  a shape/state cue. Never render white text or icons on the warm beige focus color.
- Use background blur only when supported; preserve the tonal hierarchy without blur.

## Global mobile shell

1. Atmospheric full-screen background behind every authenticated surface.
2. Minimal top header with household name, sync state and profile/menu controls.
3. Optional resume card for household chat or the next urgent household action.
4. Large rounded glass content panel that can visually echo a bottom sheet but remains stable.
5. Compact five-destination custom bottom dock: Home, Calendar, Money, Tasks and House.
6. Chat is reachable from the header/resume card and has an unread badge with a text alternative.
7. All screens stay within the platform safe drawing area. Headers must clear the status bar and the
   bottom dock must clear gesture/navigation bars; no tappable or readable content may be masked.

## Dashboard pattern

- A calm greeting and household context instead of a large colored hero tile.
- Three to four compact status or metric circles for due amount, next task, calendar and requests.
- A prominent glass chat-resume card with last sender/message and live/offline state.
- A large rounded quick-action panel with a 3-column grid of circular actions.
- SharedHouse quick actions: Add expense, Record payment, Add bill, Add task, Add event, Invite
  member, Open chat and Household settings. Role-restricted actions are hidden or clearly disabled.
- Below the action panel, concise upcoming items and unresolved requests in glass cards.

## Finance pattern

- Never use wallet-balance, transfer or custody metaphors.
- Lead with "Your amount due" and household cycle, then show owed-to/owed-by detail and status text.
- Separate recorded payment declarations from confirmed payments.
- Recurring costs show cadence, next occurrence, end condition and active/paused state.
- Filters use custom glass pills. Detail and edit flows use the same rounded panel language.
- Destructive or history-changing actions require consequence text and use revision/reversal records.

## Settings hierarchy

User Settings and Household Creator Settings are separate destinations and never share authority.

### User Settings

Profile, language, appearance/accessibility, personal notification preferences, privacy, security,
data export/deletion and sign out. These settings apply to the signed-in account.

### Household Creator Settings

Visible only with the verified household role capability. Includes household identity/timezone,
member roles and invitations, recurring bill rules, finance defaults, chore rotations, calendar
automation, household notification policy and lifecycle controls. Every command is scope-checked,
audited and guarded with consequence dialogs.

## Household chat

- Live chronological messages in a rounded glass conversation panel.
- Current user messages align right; other members align left with name and timestamp.
- Composer is a large translucent pill with a circular send action.
- Show connecting, live, reconnecting and offline states using icon plus text.
- Preserve messages append-only. Failed sends remain visibly unsent and retryable.
- Do not show message contents in system notifications when the user disables previews.

## Components

- AtmosphericScreen
- GlassPanel and GlassCard
- GlassPill and StatusPill
- CircularAction
- SharedHouseTextField based on a basic platform text input
- Primary, secondary and destructive custom buttons
- Custom bottom dock and adaptive side dock
- MetricCircle
- ChatResumeCard, MessageBubble and ChatComposer
- FinanceSummary, DueRow and RecurrenceRow
- ConfirmationDialog and GlassSheet
- Empty, loading, offline, denied, conflict and error states

## Motion and feedback

Use 140-220ms fades, small scale changes and panel transitions. Respect reduced motion. Do not
animate monetary totals or delay their final value. Haptics supplement but never replace visible and
spoken feedback. Live data refresh must not cause the whole screen to flash or jump.

## Localisation

English is the source language and Romanian is required at feature completion. Do not concatenate
sentences. Dates, time, currency, first day of week and plurals are locale-aware. Household timezone
and settlement currency are independent from UI language.

## Hard constraints

- Do not use Material 3 UI components, MaterialTheme, Material color schemes or dynamic colors.
- Do not copy Oura wordmarks, health metrics, proprietary icons or branded content.
- Do not use color alone to communicate status.
- Do not imitate public-warning visuals.
- Use "Record payment" when the action records a declaration; never imply a transfer occurred.
- Do not fabricate finance, sync, notification or chat success states.
- Use only the fonts, colors, spacing and component styles defined in this document.
