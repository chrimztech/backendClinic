ALTER TABLE IF EXISTS service_tariffs
    ADD COLUMN IF NOT EXISTS always_billable BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE IF EXISTS billing_invoices
    ADD COLUMN IF NOT EXISTS encounter_id BIGINT;

ALTER TABLE IF EXISTS lab_tests
    ADD COLUMN IF NOT EXISTS tariff_code VARCHAR(255);

ALTER TABLE IF EXISTS imaging_requests
    ADD COLUMN IF NOT EXISTS tariff_code VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_billing_invoices_encounter
    ON billing_invoices (encounter_id);
