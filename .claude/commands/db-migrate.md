# /project:db-migrate

Generate or apply a database migration.

## Instructions
1. Use Flyway for all DB migrations
2. Naming: `V{timestamp}__{description}.sql` — e.g., `V20260419_001__add_user_email_index.sql`
3. Migrations must be backward compatible (no breaking changes in a single deploy)
4. Never modify existing migration files — always create a new one
5. Test migration locally before submitting
6. Include rollback notes in comments if the migration is risky

## Safety Rules
- No `DROP TABLE` or `DROP COLUMN` without a deprecation period
- Large table alterations should use `ALTER TABLE ... ADD COLUMN` with a default, not rewrite
- Always include `-- Rollback:` comment with reversal SQL