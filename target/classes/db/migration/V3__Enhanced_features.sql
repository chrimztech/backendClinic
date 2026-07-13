-- ===================================================================
-- UNZA Clinic Database Schema - Version 3
-- Created: 2026-05-12
-- Description: Enhanced features — document storage, drug interactions,
--              lab reference ranges, refresh tokens, vital alerts
-- ===================================================================

-- ===================================================================
-- Patient Documents (Feature 2: File & Document Management)
-- ===================================================================

CREATE TABLE IF NOT EXISTS patient_documents (
    id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200),
    document_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(200) NOT NULL,
    file_size BIGINT,
    content_type VARCHAR(100),
    file_path TEXT NOT NULL,
    description TEXT,
    uploaded_by VARCHAR(100),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    encounter_id VARCHAR(50),
    lab_test_id VARCHAR(50),
    imaging_request_id VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pdoc_patient_id ON patient_documents(patient_id);
CREATE INDEX idx_pdoc_document_type ON patient_documents(document_type);
CREATE INDEX idx_pdoc_encounter_id ON patient_documents(encounter_id);
CREATE INDEX idx_pdoc_status ON patient_documents(status);

-- ===================================================================
-- Drug Interactions (Feature 4: Drug Safety Checking)
-- ===================================================================

CREATE TABLE IF NOT EXISTS drug_interactions (
    id BIGSERIAL PRIMARY KEY,
    interaction_id VARCHAR(50) UNIQUE NOT NULL,
    drug_a VARCHAR(200) NOT NULL,
    drug_b VARCHAR(200) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MODERATE',
    description TEXT,
    clinical_effect TEXT,
    management TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dint_drug_a ON drug_interactions(LOWER(drug_a));
CREATE INDEX idx_dint_drug_b ON drug_interactions(LOWER(drug_b));
CREATE INDEX idx_dint_severity ON drug_interactions(severity);

-- Seed common drug interactions relevant to a university clinic
INSERT INTO drug_interactions (interaction_id, drug_a, drug_b, severity, description, clinical_effect, management) VALUES
('DI001', 'Warfarin', 'Aspirin', 'SEVERE', 'Anticoagulant + antiplatelet combination', 'Increased risk of bleeding', 'Avoid combination; use alternative analgesic'),
('DI002', 'Metronidazole', 'Alcohol', 'SEVERE', 'Disulfiram-like reaction', 'Flushing, vomiting, tachycardia', 'Instruct patient to avoid alcohol during treatment and 48h after'),
('DI003', 'Cotrimoxazole', 'Warfarin', 'SEVERE', 'Sulfonamide inhibits warfarin metabolism', 'Enhanced anticoagulant effect, bleeding risk', 'Monitor INR closely; reduce warfarin dose if needed'),
('DI004', 'Fluconazole', 'Metformin', 'MODERATE', 'CYP2C9 interaction', 'Increased metformin levels', 'Monitor blood glucose; adjust metformin dose'),
('DI005', 'Rifampicin', 'Oral Contraceptives', 'SEVERE', 'Enzyme induction reduces contraceptive efficacy', 'Contraceptive failure, unintended pregnancy', 'Use additional contraception; inform patient'),
('DI006', 'Tetracycline', 'Iron Supplements', 'MODERATE', 'Chelation reduces tetracycline absorption', 'Reduced antibiotic efficacy', 'Separate administration by at least 2 hours'),
('DI007', 'Ibuprofen', 'ACE Inhibitors', 'MODERATE', 'NSAIDs antagonise antihypertensive effect', 'Reduced blood pressure control; renal impairment', 'Use paracetamol as alternative; monitor BP and renal function'),
('DI008', 'Amoxicillin', 'Methotrexate', 'SEVERE', 'Reduced renal excretion of methotrexate', 'Methotrexate toxicity', 'Avoid; use alternative antibiotic'),
('DI009', 'Chloroquine', 'Antacids', 'MODERATE', 'Antacids reduce chloroquine absorption', 'Reduced antimalarial efficacy', 'Separate by at least 4 hours'),
('DI010', 'Efavirenz', 'Rifampicin', 'MODERATE', 'Rifampicin induces efavirenz metabolism', 'Reduced efavirenz levels; ART failure risk', 'Increase efavirenz dose to 800mg daily'),
('DI011', 'Nevirapine', 'Fluconazole', 'MODERATE', 'Fluconazole inhibits nevirapine metabolism', 'Increased nevirapine levels, hepatotoxicity', 'Monitor liver function tests'),
('DI012', 'Digoxin', 'Erythromycin', 'SEVERE', 'Gut flora alteration increases digoxin absorption', 'Digoxin toxicity', 'Monitor digoxin levels and ECG'),
('DI013', 'Paracetamol', 'Alcohol', 'SEVERE', 'Hepatotoxic combination', 'Liver failure risk with chronic alcohol use', 'Limit paracetamol dose; counsel on alcohol avoidance'),
('DI014', 'Ciprofloxacin', 'Calcium/Dairy', 'MODERATE', 'Chelation reduces ciprofloxacin absorption', 'Reduced antibiotic efficacy', 'Take on empty stomach; avoid dairy products within 2h'),
('DI015', 'Isoniazid', 'Phenytoin', 'MODERATE', 'INH inhibits phenytoin metabolism', 'Phenytoin toxicity (ataxia, nystagmus)', 'Monitor phenytoin levels; adjust dose');

-- ===================================================================
-- Lab Reference Ranges (Feature 6: Lab Workflows)
-- ===================================================================

CREATE TABLE IF NOT EXISTS lab_reference_ranges (
    id BIGSERIAL PRIMARY KEY,
    range_id VARCHAR(50) UNIQUE NOT NULL,
    test_name VARCHAR(200) NOT NULL,
    category VARCHAR(100),
    gender VARCHAR(10) DEFAULT 'ALL',
    min_age INTEGER DEFAULT 0,
    max_age INTEGER DEFAULT 999,
    min_value DECIMAL(12,3),
    max_value DECIMAL(12,3),
    critical_low DECIMAL(12,3),
    critical_high DECIMAL(12,3),
    unit VARCHAR(50),
    interpretation_low TEXT,
    interpretation_high TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lrr_test_name ON lab_reference_ranges(LOWER(test_name));
CREATE INDEX idx_lrr_category ON lab_reference_ranges(category);
CREATE INDEX idx_lrr_gender ON lab_reference_ranges(gender);

-- Seed common reference ranges
INSERT INTO lab_reference_ranges (range_id, test_name, category, gender, min_age, max_age, min_value, max_value, critical_low, critical_high, unit, interpretation_low, interpretation_high) VALUES
('LR001', 'Haemoglobin', 'Haematology', 'MALE', 18, 999, 13.0, 17.5, 7.0, 20.0, 'g/dL', 'Anaemia', 'Polycythaemia'),
('LR002', 'Haemoglobin', 'Haematology', 'FEMALE', 18, 999, 11.5, 15.5, 7.0, 20.0, 'g/dL', 'Anaemia', 'Polycythaemia'),
('LR003', 'Haemoglobin', 'Haematology', 'ALL', 0, 17, 10.5, 15.5, 7.0, 20.0, 'g/dL', 'Anaemia (paediatric)', 'Polycythaemia'),
('LR004', 'White Blood Cell Count', 'Haematology', 'ALL', 0, 999, 4.0, 11.0, 2.0, 30.0, '×10⁹/L', 'Leucopoenia — infection/drug/autoimmune risk', 'Leucocytosis — infection or malignancy'),
('LR005', 'Platelet Count', 'Haematology', 'ALL', 0, 999, 150.0, 400.0, 50.0, 1000.0, '×10⁹/L', 'Thrombocytopoenia — bleeding risk', 'Thrombocytosis'),
('LR006', 'Random Blood Glucose', 'Biochemistry', 'ALL', 0, 999, 3.9, 7.8, 2.5, 20.0, 'mmol/L', 'Hypoglycaemia', 'Hyperglycaemia / possible diabetes'),
('LR007', 'Fasting Blood Glucose', 'Biochemistry', 'ALL', 0, 999, 3.9, 5.5, 2.5, 20.0, 'mmol/L', 'Hypoglycaemia', 'Impaired fasting glucose / diabetes'),
('LR008', 'Serum Creatinine', 'Biochemistry', 'MALE', 18, 999, 62.0, 115.0, 30.0, 400.0, 'μmol/L', 'Low muscle mass', 'Renal impairment'),
('LR009', 'Serum Creatinine', 'Biochemistry', 'FEMALE', 18, 999, 44.0, 97.0, 30.0, 400.0, 'μmol/L', 'Low muscle mass', 'Renal impairment'),
('LR010', 'ALT (Alanine Aminotransferase)', 'Biochemistry', 'MALE', 18, 999, 7.0, 56.0, NULL, 200.0, 'U/L', NULL, 'Hepatocellular damage'),
('LR011', 'ALT (Alanine Aminotransferase)', 'Biochemistry', 'FEMALE', 18, 999, 7.0, 45.0, NULL, 200.0, 'U/L', NULL, 'Hepatocellular damage'),
('LR012', 'CD4 Count', 'Immunology', 'ALL', 0, 999, 500.0, 1500.0, 50.0, NULL, 'cells/μL', 'Immunosuppression — ART review', NULL),
('LR013', 'Urine Protein (Dipstick)', 'Urinalysis', 'ALL', 0, 999, 0.0, 0.0, NULL, NULL, 'g/L', NULL, 'Proteinuria — renal/pre-eclampsia'),
('LR014', 'Serum Sodium', 'Biochemistry', 'ALL', 0, 999, 136.0, 145.0, 120.0, 155.0, 'mmol/L', 'Hyponatraemia', 'Hypernatraemia'),
('LR015', 'Serum Potassium', 'Biochemistry', 'ALL', 0, 999, 3.5, 5.0, 2.5, 6.5, 'mmol/L', 'Hypokalaemia — arrhythmia risk', 'Hyperkalaemia — cardiac risk'),
('LR016', 'ESR', 'Haematology', 'MALE', 18, 999, 0.0, 15.0, NULL, 100.0, 'mm/hr', NULL, 'Elevated — infection/inflammation/malignancy'),
('LR017', 'ESR', 'Haematology', 'FEMALE', 18, 999, 0.0, 20.0, NULL, 100.0, 'mm/hr', NULL, 'Elevated — infection/inflammation/malignancy'),
('LR018', 'Total Bilirubin', 'Biochemistry', 'ALL', 0, 999, 0.0, 20.5, NULL, 50.0, 'μmol/L', NULL, 'Jaundice — hepatic/haemolytic'),
('LR019', 'Sputum AFB', 'Microbiology', 'ALL', 0, 999, NULL, NULL, NULL, NULL, 'qualitative', 'Negative — AFB not seen', 'Positive — TB likely; grade 1+/2+/3+'),
('LR020', 'Malaria RDT', 'Parasitology', 'ALL', 0, 999, NULL, NULL, NULL, NULL, 'qualitative', 'Negative', 'Positive — P. falciparum / P. vivax');

-- ===================================================================
-- Refresh Tokens (Feature 8: Secure Session Management)
-- ===================================================================

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_id VARCHAR(100) UNIQUE NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    user_email VARCHAR(100) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    replaced_by_token_id VARCHAR(100)
);

CREATE INDEX idx_rt_token_id ON refresh_tokens(token_id);
CREATE INDEX idx_rt_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_rt_user_email ON refresh_tokens(user_email);
CREATE INDEX idx_rt_revoked ON refresh_tokens(revoked);

-- ===================================================================
-- Vital Alerts (Feature 5: Vital Signs Trends & Clinical Alerts)
-- ===================================================================

CREATE TABLE IF NOT EXISTS vital_alerts (
    id BIGSERIAL PRIMARY KEY,
    alert_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200),
    triage_id BIGINT,
    vital_type VARCHAR(50) NOT NULL,
    value_text VARCHAR(100) NOT NULL,
    threshold_text VARCHAR(100),
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    message TEXT,
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by VARCHAR(100),
    acknowledged_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_va_patient_id ON vital_alerts(patient_id);
CREATE INDEX idx_va_severity ON vital_alerts(severity);
CREATE INDEX idx_va_acknowledged ON vital_alerts(acknowledged);
CREATE INDEX idx_va_created_at ON vital_alerts(created_at);

-- ===================================================================
-- Add notes + approvedAmount to insurance_claims
-- ===================================================================

ALTER TABLE insurance_claims
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS approved_amount VARCHAR(50);

COMMIT;
