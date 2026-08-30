ALTER TABLE encounter_records
    ADD COLUMN IF NOT EXISTS queue_status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'ROUTINE',
    ADD COLUMN IF NOT EXISTS assigned_to VARCHAR(120),
    ADD COLUMN IF NOT EXISTS department_entered_at VARCHAR(50),
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

UPDATE encounter_records
SET department_entered_at = COALESCE(updated_at::text, created_at::text)
WHERE department_entered_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_encounters_department_queue
    ON encounter_records (current_stage, queue_status, checked_out);
