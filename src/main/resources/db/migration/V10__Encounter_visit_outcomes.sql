ALTER TABLE encounter_records
    ADD COLUMN IF NOT EXISTS visit_outcome VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS ended_at VARCHAR(50),
    ADD COLUMN IF NOT EXISTS ended_by VARCHAR(120);

UPDATE encounter_records
SET visit_outcome = CASE WHEN checked_out THEN 'COMPLETED' ELSE 'ACTIVE' END;

CREATE INDEX IF NOT EXISTS idx_encounters_visit_outcome
    ON encounter_records (visit_outcome, checked_out);
