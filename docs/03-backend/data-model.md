# Data Model

## 1. Modelling principles

- Use PostgreSQL constraints in addition to application validation.
- Store money as signed 64-bit integer minor units and a three-letter currency.
- Keep historical financial and assignment records append-oriented.
- Use explicit lifecycle states, timestamps and actor IDs.
- Separate identity, household membership and platform administration.
- Store only data required for a documented purpose.

## 2. Identity and profile

### `users`

`id`, `email_normalised`, `email_verified_at`, `status`, `preferred_locale`, `preferred_timezone`, `created_at`, `updated_at`, `deletion_requested_at`, `deleted_at`.

### `user_profiles`

`user_id`, `display_name`, `pronunciation_optional`, `avatar_media_id`, `colour_token`, `accessibility_preferences`, `appearance_preference`.

### `user_sessions` and `user_devices`

Sessions contain hashed refresh-token identifiers, expiry, revocation and security metadata. Devices contain platform, app version, last seen, notification capability and user-defined device name. Do not store unnecessary device fingerprints.

## 3. Households and membership

### `households`

`id`, `name`, `timezone`, `default_currency`, `cycle_type`, `cycle_anchor`, `monthly_rule`, `status`, `created_by`, `created_at`, `version`.

### `household_memberships`

`id`, `household_id`, `user_id`, `role`, `status`, `joined_at`, `left_at`, `display_order`, `share_defaults`, `notification_overrides`.

Unique active membership per `(household_id, user_id)`. Historical memberships remain available for authorised ledger interpretation.

### `household_invitations`

`id`, `household_id`, `token_hash`, `email_hint_optional`, `intended_role`, `created_by`, `expires_at`, `max_uses`, `use_count`, `status`, `accepted_by`, `accepted_at`.

## 4. Billing cycles and recurrence

### `billing_cycles`

`id`, `household_id`, `start_local_date`, `end_local_date`, `due_local_date`, `timezone`, `currency`, `state`, `generated_from_rule_id`, `closed_at`.

### `recurrence_rules`

`id`, `household_id`, `resource_type`, `frequency`, `interval`, `anchor_local_date`, `day_of_month`, `weekdays`, `due_offset`, `timezone`, `start_date`, `end_date`, `enabled`, `version`.

Generated occurrences carry a deterministic occurrence key: `(rule_id, occurrence_local_date, rule_revision)`.

## 5. Ledger

### `expenses`

`id`, `household_id`, `cycle_id`, `title`, `category`, `supplier_label`, `amount_minor`, `currency`, `service_period_start`, `service_period_end`, `due_at`, `payer_membership_id`, `split_method`, `status`, `notes`, `recurrence_rule_id`, `revision`, `supersedes_expense_id`, `created_by`, `created_at`.

### `expense_allocations`

`id`, `expense_id`, `membership_id`, `amount_minor`, `percentage_basis_points`, `weight_units`, `status`, `rounding_adjustment_minor`.

Allocation amounts must sum exactly to the expense amount unless an explicit platform-funded adjustment type is introduced and legally/accountingly reviewed.

### `payment_declarations`

`id`, `household_id`, `allocation_id`, `declared_by`, `amount_minor`, `currency`, `paid_at`, `method_label`, `reference_redacted`, `evidence_media_id`, `status`, `confirmed_by`, `confirmed_at`, `reversal_of_id`, `reason`, `created_at`.

### `ledger_adjustments`

`id`, `household_id`, `allocation_id`, `type`, `amount_minor`, `reason`, `approved_by`, `created_at`, `reversal_of_id`.

## 6. Utilities and meter readings

### `utility_accounts`

Display-only household configuration: `supplier_label`, `utility_type`, `meter_label`, `unit`, `tariff_note`, and optional account-reference suffix. Do not store a full provider password or payment credential.

### `meter_readings`

`id`, `household_id`, `utility_account_id`, `reading_decimal`, `unit`, `read_at`, `submitted_by`, `photo_media_id`, `validation_status`, `notes`.

Decimal precision is utility-specific and defined by a value-object rule.

## 7. Shopping

### `shopping_lists`, `shopping_items`, `purchases`, `purchase_items`

Shopping items separate expected price from actual purchase price. A purchase links to a ledger expense only after the applicable approval rule is satisfied. Substitutions retain the original item reference.

## 8. Chores

### `chore_templates`

`id`, `household_id`, `title`, `zone`, `instructions`, `difficulty_weight`, `expected_minutes`, `assignment_strategy`, `proof_policy`, `postpone_policy`, `recurrence_rule_id`, `active`, `version`.

### `chore_assignments`

`id`, `household_id`, `template_id`, `occurrence_key`, `due_start`, `due_end`, `status`, `assigned_by`, `completed_at`, `completion_note`, `proof_media_id`, `version`.

### `chore_assignees`

Join table containing membership, role in the assignment (`responsible` or `helper`) and response state.

### `task_requests`

`id`, `assignment_id`, `type`, `requested_by`, `target_membership_id`, `proposed_due_at`, `reason`, `status`, `resolved_by`, `resolved_at`, `expires_at`.

## 9. Notifications and inbox

- `notification_preferences`
- `push_tokens`
- `notification_jobs`
- `notification_deliveries`
- `inbox_messages`

Delivery records retain provider response category and timestamps, not full private notification content indefinitely.

## 10. Store billing

### `store_accounts`

Links internal user to provider app-account token or obfuscated account ID where supported.

### `store_transactions`

Provider, environment, product ID, transaction/purchase token identifier, original transaction identifier, signed-payload hash, purchase time, expiry, revocation, acknowledgement state and raw payload retention pointer subject to minimisation.

### `entitlements`

`id`, `subject_type`, `subject_id`, `entitlement_key`, `source`, `status`, `starts_at`, `ends_at`, `grace_until`, `last_verified_at`.

Entitlements are derived from verified provider state and cannot be granted solely from a mobile-client success callback.

## 11. Privacy, audit and support

- `consent_records` record policy version, purpose and timestamp.
- `privacy_requests` track export/deletion workflow without storing exported content in the row.
- `audit_events` capture actor, action, target, reason, result, IP-risk category and correlation ID.
- `support_cases` store user-submitted issue data separately from privileged access events.
- `legal_holds` require a documented authority, narrow scope and expiry/review.

## 12. Indexing and partitioning

Every high-volume table begins with indexes supporting household scope and time ordering. Examples: `(household_id, created_at desc)`, `(household_id, status, due_at)`, `(user_id, status)`, provider transaction unique keys and sync cursor indexes. Partition audit/delivery tables only after measured volume justifies the operational complexity.
