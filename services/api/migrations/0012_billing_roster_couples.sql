CREATE TABLE household_billing_rosters (
    household_id uuid PRIMARY KEY REFERENCES households(id) ON DELETE RESTRICT,
    version integer NOT NULL DEFAULT 1 CHECK (version > 0),
    updated_by_membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

INSERT INTO household_billing_rosters (
    household_id, version, updated_by_membership_id, created_at, updated_at
)
SELECT h.id, 1, owner.id, h.created_at, h.updated_at
FROM households h
JOIN household_memberships owner
  ON owner.household_id = h.id AND owner.role = 'owner' AND owner.status = 'active'
ON CONFLICT (household_id) DO NOTHING;

CREATE TABLE household_billing_couples (
    id uuid PRIMARY KEY,
    household_id uuid NOT NULL REFERENCES households(id) ON DELETE RESTRICT,
    primary_membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    partner_membership_id uuid REFERENCES household_memberships(id) ON DELETE RESTRICT,
    partner_display_name varchar(80),
    status varchar(16) NOT NULL CHECK (status IN ('active', 'archived')),
    version integer NOT NULL DEFAULT 1 CHECK (version > 0),
    created_by_membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    archived_by_membership_id uuid REFERENCES household_memberships(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    archived_at timestamptz,
    CONSTRAINT household_billing_couples_partner_check CHECK (
        (partner_membership_id IS NOT NULL AND partner_display_name IS NULL)
        OR (
            partner_membership_id IS NULL
            AND partner_display_name IS NOT NULL
            AND char_length(trim(partner_display_name)) BETWEEN 1 AND 80
        )
    ),
    CONSTRAINT household_billing_couples_distinct_members_check CHECK (
        partner_membership_id IS NULL OR partner_membership_id <> primary_membership_id
    ),
    CONSTRAINT household_billing_couples_archive_check CHECK (
        (status = 'active' AND archived_by_membership_id IS NULL AND archived_at IS NULL)
        OR (status = 'archived' AND archived_at IS NOT NULL)
    )
);

CREATE INDEX household_billing_couples_household_status_idx
    ON household_billing_couples (household_id, status, created_at, id);

CREATE UNIQUE INDEX household_billing_couples_active_primary_unique
    ON household_billing_couples (household_id, primary_membership_id)
    WHERE status = 'active';

CREATE UNIQUE INDEX household_billing_couples_active_partner_unique
    ON household_billing_couples (household_id, partner_membership_id)
    WHERE status = 'active' AND partner_membership_id IS NOT NULL;

CREATE FUNCTION enforce_active_billing_couple_memberships() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.status <> 'active' THEN
        RETURN NEW;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM household_memberships membership
        WHERE membership.id = NEW.primary_membership_id
          AND membership.household_id = NEW.household_id
          AND membership.status = 'active'
    ) THEN
        RAISE EXCEPTION 'Billing couple primary must be an active member of the household.';
    END IF;

    IF NEW.partner_membership_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM household_memberships membership
        WHERE membership.id = NEW.partner_membership_id
          AND membership.household_id = NEW.household_id
          AND membership.status = 'active'
    ) THEN
        RAISE EXCEPTION 'Billing couple partner must be an active member of the household.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM household_billing_couples existing
        WHERE existing.household_id = NEW.household_id
          AND existing.status = 'active'
          AND existing.id <> NEW.id
          AND (
              existing.primary_membership_id = NEW.primary_membership_id
              OR existing.partner_membership_id = NEW.primary_membership_id
              OR (
                  NEW.partner_membership_id IS NOT NULL
                  AND (
                      existing.primary_membership_id = NEW.partner_membership_id
                      OR existing.partner_membership_id = NEW.partner_membership_id
                  )
              )
          )
    ) THEN
        RAISE EXCEPTION 'A member can belong to only one active billing couple.';
    END IF;

    RETURN NEW;
END;
$$;

CREATE CONSTRAINT TRIGGER household_billing_couples_membership_guard
AFTER INSERT OR UPDATE ON household_billing_couples
DEFERRABLE INITIALLY IMMEDIATE
FOR EACH ROW EXECUTE FUNCTION enforce_active_billing_couple_memberships();

CREATE FUNCTION maintain_billing_roster_after_membership_change() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.status = 'active' AND NEW.status <> 'active' THEN
        UPDATE household_billing_couples
        SET status = 'archived', version = version + 1, archived_by_membership_id = NULL,
            archived_at = now(), updated_at = now()
        WHERE household_id = NEW.household_id
          AND status = 'active'
          AND (primary_membership_id = NEW.id OR partner_membership_id = NEW.id);
    END IF;

    IF TG_OP = 'INSERT' THEN
        UPDATE household_billing_rosters
        SET version = version + 1, updated_by_membership_id = NEW.id, updated_at = now()
        WHERE household_id = NEW.household_id;
    ELSIF OLD.status IS DISTINCT FROM NEW.status THEN
        UPDATE household_billing_rosters
        SET version = version + 1, updated_by_membership_id = NEW.id, updated_at = now()
        WHERE household_id = NEW.household_id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER household_memberships_billing_roster_insert_maintenance
AFTER INSERT ON household_memberships
FOR EACH ROW EXECUTE FUNCTION maintain_billing_roster_after_membership_change();

CREATE TRIGGER household_memberships_billing_roster_status_maintenance
AFTER UPDATE OF status ON household_memberships
FOR EACH ROW EXECUTE FUNCTION maintain_billing_roster_after_membership_change();

ALTER TABLE expense_allocations
    DROP CONSTRAINT expense_allocations_rounding_adjustment_minor_check,
    ADD COLUMN billing_unit_type varchar(16) NOT NULL DEFAULT 'individual'
        CHECK (billing_unit_type IN ('individual', 'couple')),
    ADD COLUMN billing_unit_label varchar(180) NOT NULL DEFAULT 'Household member',
    ADD COLUMN participant_count smallint NOT NULL DEFAULT 1 CHECK (participant_count BETWEEN 1 AND 2),
    ADD COLUMN billing_couple_id uuid REFERENCES household_billing_couples(id) ON DELETE RESTRICT;

ALTER TABLE expense_allocations
    ADD CONSTRAINT expense_allocations_rounding_adjustment_minor_check CHECK (
        rounding_adjustment_minor BETWEEN 0 AND participant_count
    ),
    ADD CONSTRAINT expense_allocations_billing_unit_check CHECK (
        (billing_unit_type = 'individual' AND participant_count = 1 AND billing_couple_id IS NULL)
        OR (billing_unit_type = 'couple' AND participant_count = 2 AND billing_couple_id IS NOT NULL)
    );

-- Keep this data update after every ALTER on expense_allocations. Existing allocation
-- integrity uses deferred constraint triggers, so another ALTER in the same transaction
-- after touching historical rows would fail on populated production databases.
UPDATE expense_allocations allocation
SET billing_unit_label = profile.display_name
FROM household_memberships membership
JOIN user_profiles profile ON profile.user_id = membership.user_id
WHERE allocation.membership_id = membership.id;

CREATE TABLE expense_allocation_members (
    allocation_id uuid NOT NULL REFERENCES expense_allocations(id) ON DELETE RESTRICT,
    membership_id uuid NOT NULL REFERENCES household_memberships(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL,
    PRIMARY KEY (allocation_id, membership_id)
);

INSERT INTO expense_allocation_members (allocation_id, membership_id, created_at)
SELECT id, membership_id, created_at FROM expense_allocations;

CREATE INDEX expense_allocation_members_membership_idx
    ON expense_allocation_members (membership_id, allocation_id);
