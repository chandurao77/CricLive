-- BaseEntity audits updated_at on every table; venue was created without it.
ALTER TABLE venue ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
