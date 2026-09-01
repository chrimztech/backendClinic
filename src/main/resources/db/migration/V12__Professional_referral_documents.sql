-- Align the original Flyway table name with the JPA entity on fresh databases.
DO $$
BEGIN
    IF to_regclass('referral_records') IS NULL AND to_regclass('referrals') IS NOT NULL THEN
        ALTER TABLE referrals RENAME TO referral_records;
    END IF;
END $$;

ALTER TABLE IF EXISTS referral_records
    ADD COLUMN IF NOT EXISTS destination_facility VARCHAR(200),
    ADD COLUMN IF NOT EXISTS provisional_diagnosis VARCHAR(255),
    ADD COLUMN IF NOT EXISTS clinical_summary TEXT,
    ADD COLUMN IF NOT EXISTS investigations TEXT,
    ADD COLUMN IF NOT EXISTS treatment_given TEXT,
    ADD COLUMN IF NOT EXISTS vital_signs VARCHAR(255),
    ADD COLUMN IF NOT EXISTS referring_clinician_contact VARCHAR(120);
