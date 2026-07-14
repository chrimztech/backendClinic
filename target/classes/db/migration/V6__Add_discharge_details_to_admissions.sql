ALTER TABLE admissions
    ADD COLUMN IF NOT EXISTS discharge_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS discharge_summary TEXT,
    ADD COLUMN IF NOT EXISTS discharged_on VARCHAR(20),
    ADD COLUMN IF NOT EXISTS discharged_by VARCHAR(100);
