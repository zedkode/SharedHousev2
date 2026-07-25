# Accessibility and Localisation

## Language behaviour

At first launch, use the device’s preferred supported language. Supported initial languages are English (`en`) and Romanian (`ro`). If neither is present, use English. The user can override language without changing the device language.

Household currency, timezone and date-cycle rules do not change when UI language changes.

## Localisation implementation

- Stable message keys, not source text as identifiers.
- ICU-style plural and select messages where supported.
- No concatenated sentences.
- Locale-aware currency and number formatting.
- Localised category defaults with user-defined labels retained verbatim.
- Dates shown with explicit year where ambiguity exists.
- Translators receive context, screenshots and character constraints.
- CI rejects missing English or Romanian keys.

## Accessibility requirements

- All avatars have meaningful labels such as “Task assigned to Andrei”, not “image”.
- Money is announced with currency and state.
- Calendar cells expose date, event count, amount due and assigned task count.
- Swipe-only actions have visible alternatives.
- Dynamic Type/font scaling does not truncate critical totals or actions.
- Focus order follows visual order.
- Modal sheets trap focus correctly on web and restore focus after close.
- Charts have text/table equivalents.
- Reduced motion and reduced transparency preferences are honoured.
- Haptic feedback supplements but never replaces visual/audible state.

## Inclusive language

Use neutral household terminology. Avoid “debtor”, “offender”, “failed tenant” or public ranking. Fairness statistics should explain workload distribution without shaming individuals.
