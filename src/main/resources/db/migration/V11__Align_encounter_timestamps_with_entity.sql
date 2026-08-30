-- Encounter timestamps are stored as ISO-8601 strings by the application.
-- Older installations created these two columns as PostgreSQL timestamps.
ALTER TABLE encounter_records
    ALTER COLUMN created_at TYPE VARCHAR(50) USING created_at::text,
    ALTER COLUMN updated_at TYPE VARCHAR(50) USING updated_at::text;
