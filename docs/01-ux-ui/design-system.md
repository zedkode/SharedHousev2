# UI Design System

## Visual direction

SharedHouse should feel calm, trustworthy and practical rather than financial-corporate or childish. The design uses spacious cards, clear totals, recognisable household icons, member avatars and restrained motion.

## Theme modes

- Light.
- Dark.
- Follow system.
- Android dynamic colour when supported, with a product fallback palette.
- User-selectable accent from an accessible predefined set.

Themes must preserve semantic status contrast and may not allow an accent to make warnings or paid/overdue states ambiguous.

## Core components

- household switcher;
- cycle selector;
- money summary card;
- due-item row;
- avatar and avatar stack;
- task assignment card;
- calendar event chip;
- status badge with icon and text;
- split visualiser;
- payment declaration sheet;
- help/swap/postpone action sheet;
- shopping item row;
- empty, loading, offline and error states;
- permission education sheet;
- destructive action confirmation;
- audit/history timeline.

## Status semantics

- Neutral: scheduled, draft, informational.
- Positive: recorded, confirmed, completed.
- Attention: due soon, pending request, estimated.
- Negative: overdue, rejected, failed sync, disputed.
- Disabled: waived, cancelled, expired.

Every state uses icon, label and accessible description. Never rely on red/green alone.

## Typography and density

Use platform system typography. Totals use tabular figures where available. Body copy must scale without clipping. Dense administrative tables belong in the web portal; mobile uses grouped cards and drill-down.

## Motion

Use short, purposeful transitions for state changes, calendar navigation and confirmations. Respect reduced-motion settings. Do not animate money totals in a way that obscures the final value.

## Empty and error states

Every empty state explains why it is empty and provides the next permitted action. Errors distinguish offline, permission, validation, conflict, subscription entitlement and server failure. Never display raw backend error details.
