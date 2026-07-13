-- ===================================================================
-- UNZA Clinic Database Schema - Version 2
-- Created: 2026-05-11
-- Description: Specialized department tables — MCH, ART, Dental, Eye,
--              STI, Physiotherapy, and Pharmacy Dispensing Log
-- ===================================================================

-- ===================================================================
-- MCH (Mother & Child Health) Tables
-- ===================================================================

CREATE TABLE IF NOT EXISTS mch_antenatal_visits (
    id BIGSERIAL PRIMARY KEY,
    visit_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    visit_date VARCHAR(20),
    gestational_age VARCHAR(20),
    gravida INTEGER,
    para INTEGER,
    lmp VARCHAR(20),
    edd VARCHAR(20),
    blood_pressure VARCHAR(20),
    weight VARCHAR(20),
    fetal_heart_rate VARCHAR(20),
    presentation VARCHAR(50),
    urinalysis VARCHAR(100),
    hiv_status VARCHAR(50),
    syphilis_status VARCHAR(50),
    ferrous BOOLEAN DEFAULT FALSE,
    folic_acid BOOLEAN DEFAULT FALSE,
    itn BOOLEAN DEFAULT FALSE,
    tetanus_vaccine VARCHAR(20),
    next_visit VARCHAR(20),
    nurse_notes TEXT,
    risk_factors TEXT,
    nurse_name VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_anc_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_anc_patient_id ON mch_antenatal_visits(patient_id);
CREATE INDEX idx_anc_visit_date ON mch_antenatal_visits(visit_date);
CREATE INDEX idx_anc_status ON mch_antenatal_visits(status);

CREATE TABLE IF NOT EXISTS mch_immunization_visits (
    id BIGSERIAL PRIMARY KEY,
    child_id VARCHAR(50) UNIQUE NOT NULL,
    child_name VARCHAR(200) NOT NULL,
    mother_name VARCHAR(200),
    dob VARCHAR(20),
    vaccines_given TEXT,
    visit_date VARCHAR(20),
    next_due VARCHAR(20),
    weight VARCHAR(20),
    height VARCHAR(20),
    muac VARCHAR(20),
    nutrition_status VARCHAR(50),
    nurse_notes TEXT,
    nurse_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_immu_child_id ON mch_immunization_visits(child_id);
CREATE INDEX idx_immu_visit_date ON mch_immunization_visits(visit_date);

CREATE TABLE IF NOT EXISTS mch_family_planning (
    id BIGSERIAL PRIMARY KEY,
    visit_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    method VARCHAR(200),
    start_date VARCHAR(20),
    next_visit VARCHAR(20),
    side_effects TEXT,
    counseling_given BOOLEAN DEFAULT TRUE,
    notes TEXT,
    nurse_name VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fp_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_fp_patient_id ON mch_family_planning(patient_id);
CREATE INDEX idx_fp_status ON mch_family_planning(status);

-- ===================================================================
-- ART / HIV Clinic Tables
-- ===================================================================

CREATE TABLE IF NOT EXISTS art_patients (
    id BIGSERIAL PRIMARY KEY,
    art_number VARCHAR(100) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    enrollment_date VARCHAR(20),
    current_regimen TEXT,
    regimen_line VARCHAR(50),
    cd4_baseline INTEGER,
    cd4_latest INTEGER,
    vl_latest BIGINT,
    vl_date VARCHAR(20),
    disclosure_status VARCHAR(100),
    pregnancy_status VARCHAR(50),
    tb_status VARCHAR(100),
    tb_treatment BOOLEAN DEFAULT FALSE,
    adherence_score VARCHAR(20),
    next_pickup VARCHAR(20),
    next_clinic_date VARCHAR(20),
    transfer_in BOOLEAN DEFAULT FALSE,
    transfer_from VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_art_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_art_number ON art_patients(art_number);
CREATE INDEX idx_art_patient_id ON art_patients(patient_id);
CREATE INDEX idx_art_status ON art_patients(status);
CREATE INDEX idx_art_next_pickup ON art_patients(next_pickup);

CREATE TABLE IF NOT EXISTS art_clinic_visits (
    id BIGSERIAL PRIMARY KEY,
    visit_id VARCHAR(50) UNIQUE NOT NULL,
    art_number VARCHAR(100) NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    visit_date VARCHAR(20),
    clinician_name VARCHAR(100),
    weight VARCHAR(20),
    blood_pressure VARCHAR(20),
    cd4_count INTEGER,
    viral_load BIGINT,
    vl_date VARCHAR(20),
    viral_load_status VARCHAR(50),
    regimen TEXT,
    regimen_changed BOOLEAN DEFAULT FALSE,
    change_reason TEXT,
    ois_present TEXT,
    cotrimoxazole BOOLEAN DEFAULT FALSE,
    inh BOOLEAN DEFAULT FALSE,
    days_dispensed INTEGER,
    pills_returned INTEGER,
    adherence_score VARCHAR(20),
    side_effects TEXT,
    counseling_given BOOLEAN DEFAULT TRUE,
    next_visit VARCHAR(20),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_art_visit_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_art_visit_art_number ON art_clinic_visits(art_number);
CREATE INDEX idx_art_visit_patient_id ON art_clinic_visits(patient_id);
CREATE INDEX idx_art_visit_date ON art_clinic_visits(visit_date);

-- ===================================================================
-- Dental Clinic Table
-- ===================================================================

CREATE TABLE IF NOT EXISTS dental_records (
    id BIGSERIAL PRIMARY KEY,
    visit_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    visit_date VARCHAR(20),
    dentist_name VARCHAR(100),
    chief_complaint TEXT,
    teeth_affected VARCHAR(200),
    diagnosis VARCHAR(200),
    treatment_performed TEXT,
    local_anesthetic BOOLEAN DEFAULT FALSE,
    anesthetic_type VARCHAR(100),
    extractions_done TEXT,
    fillings_placed TEXT,
    scaling_done BOOLEAN DEFAULT FALSE,
    xray_taken BOOLEAN DEFAULT FALSE,
    xray_findings TEXT,
    medications TEXT,
    referral_needed BOOLEAN DEFAULT FALSE,
    referral_reason TEXT,
    next_appointment VARCHAR(20),
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'completed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dental_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_dental_patient_id ON dental_records(patient_id);
CREATE INDEX idx_dental_visit_date ON dental_records(visit_date);
CREATE INDEX idx_dental_status ON dental_records(status);

-- ===================================================================
-- Eye Clinic Table
-- ===================================================================

CREATE TABLE IF NOT EXISTS eye_clinic_records (
    id BIGSERIAL PRIMARY KEY,
    visit_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    visit_date VARCHAR(20),
    optometrist_name VARCHAR(100),
    chief_complaint TEXT,
    va_right_unaided VARCHAR(10),
    va_left_unaided VARCHAR(10),
    va_right_corrected VARCHAR(10),
    va_left_corrected VARCHAR(10),
    refraction_right_sph VARCHAR(10),
    refraction_right_cyl VARCHAR(10),
    refraction_right_axis VARCHAR(10),
    refraction_left_sph VARCHAR(10),
    refraction_left_cyl VARCHAR(10),
    refraction_left_axis VARCHAR(10),
    iop_right VARCHAR(10),
    iop_left VARCHAR(10),
    colour_vision VARCHAR(50),
    slit_lamp_findings TEXT,
    fundoscopy TEXT,
    diagnosis VARCHAR(200),
    spectacles_prescribed BOOLEAN DEFAULT FALSE,
    spectacles_type VARCHAR(100),
    treatment TEXT,
    referral_needed BOOLEAN DEFAULT FALSE,
    referral_reason TEXT,
    next_review VARCHAR(20),
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'completed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eye_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_eye_patient_id ON eye_clinic_records(patient_id);
CREATE INDEX idx_eye_visit_date ON eye_clinic_records(visit_date);
CREATE INDEX idx_eye_status ON eye_clinic_records(status);

-- ===================================================================
-- STI Clinic Table
-- ===================================================================

CREATE TABLE IF NOT EXISTS sti_records (
    id BIGSERIAL PRIMARY KEY,
    visit_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    visit_date VARCHAR(20),
    clinician_name VARCHAR(100),
    chief_complaint TEXT,
    syndrome_classification VARCHAR(200),
    lab_tests_done TEXT,
    hiv_test VARCHAR(50),
    syphilis_test VARCHAR(50),
    hepatitis_b VARCHAR(50),
    diagnosis VARCHAR(200),
    treatment_protocol TEXT,
    medications_given TEXT,
    partner_notification BOOLEAN DEFAULT FALSE,
    partner_treated BOOLEAN DEFAULT FALSE,
    condoms_provided INTEGER DEFAULT 0,
    hiv_counseling_given BOOLEAN DEFAULT TRUE,
    adherence_counseling_given BOOLEAN DEFAULT TRUE,
    follow_up_date VARCHAR(20),
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'completed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sti_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_sti_patient_id ON sti_records(patient_id);
CREATE INDEX idx_sti_visit_date ON sti_records(visit_date);
CREATE INDEX idx_sti_syndrome ON sti_records(syndrome_classification);

-- ===================================================================
-- Physiotherapy Tables
-- ===================================================================

CREATE TABLE IF NOT EXISTS physio_referrals (
    id BIGSERIAL PRIMARY KEY,
    referral_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    referred_by VARCHAR(100),
    referral_date VARCHAR(20),
    diagnosis VARCHAR(200),
    referral_reason TEXT,
    urgency VARCHAR(20),
    assigned_therapist VARCHAR(100),
    first_appointment VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_physio_ref_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_physio_ref_patient_id ON physio_referrals(patient_id);
CREATE INDEX idx_physio_ref_status ON physio_referrals(status);

CREATE TABLE IF NOT EXISTS physio_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    session_date VARCHAR(20),
    therapist_name VARCHAR(100),
    session_number INTEGER,
    diagnosis VARCHAR(200),
    treatment_given TEXT,
    pain_score_before INTEGER,
    pain_score_after INTEGER,
    mobility_before VARCHAR(100),
    mobility_after VARCHAR(100),
    exercises_done TEXT,
    home_program TEXT,
    progress_notes TEXT,
    next_session VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'completed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_physio_session_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_physio_session_patient_id ON physio_sessions(patient_id);
CREATE INDEX idx_physio_session_date ON physio_sessions(session_date);

-- ===================================================================
-- Pharmacy Dispensing Log
-- ===================================================================

CREATE TABLE IF NOT EXISTS pharmacy_dispensing_log (
    id BIGSERIAL PRIMARY KEY,
    log_id VARCHAR(50) UNIQUE NOT NULL,
    rx_id VARCHAR(50) NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    patient_type VARCHAR(20),
    drug_name TEXT,
    quantity_requested INTEGER,
    quantity_dispensed INTEGER,
    unit VARCHAR(20),
    dispensing_instructions TEXT,
    pharmacist_name VARCHAR(100),
    pharmacist_notes TEXT,
    is_controlled BOOLEAN DEFAULT FALSE,
    substituted BOOLEAN DEFAULT FALSE,
    substitute_reason TEXT,
    dispensed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dispense_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_dispense_log_rx_id ON pharmacy_dispensing_log(rx_id);
CREATE INDEX idx_dispense_log_patient_id ON pharmacy_dispensing_log(patient_id);
CREATE INDEX idx_dispense_log_is_controlled ON pharmacy_dispensing_log(is_controlled);
CREATE INDEX idx_dispense_log_dispensed_at ON pharmacy_dispensing_log(dispensed_at);

-- ===================================================================
-- Controlled Drugs Register
-- ===================================================================

CREATE TABLE IF NOT EXISTS controlled_drugs_register (
    id BIGSERIAL PRIMARY KEY,
    entry_id VARCHAR(50) UNIQUE NOT NULL,
    drug_name VARCHAR(200) NOT NULL,
    schedule VARCHAR(20),
    rx_id VARCHAR(50),
    patient_id VARCHAR(50),
    patient_name VARCHAR(200),
    quantity_dispensed INTEGER,
    unit VARCHAR(20),
    balance_before INTEGER,
    balance_after INTEGER,
    dispensed_by VARCHAR(100),
    authorized_by VARCHAR(100),
    date_dispensed VARCHAR(20),
    witness VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cdr_drug_name ON controlled_drugs_register(drug_name);
CREATE INDEX idx_cdr_date ON controlled_drugs_register(date_dispensed);
CREATE INDEX idx_cdr_rx_id ON controlled_drugs_register(rx_id);

-- ===================================================================
-- Update prescriptions table with extra pharmacy fields
-- ===================================================================

ALTER TABLE prescriptions
    ADD COLUMN IF NOT EXISTS drug_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS quantity INTEGER DEFAULT 1,
    ADD COLUMN IF NOT EXISTS dosage VARCHAR(100),
    ADD COLUMN IF NOT EXISTS duration VARCHAR(50),
    ADD COLUMN IF NOT EXISTS instructions TEXT,
    ADD COLUMN IF NOT EXISTS medication_class VARCHAR(100),
    ADD COLUMN IF NOT EXISTS program VARCHAR(100),
    ADD COLUMN IF NOT EXISTS dispensed_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS dispensed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS pharmacist_notes TEXT;

-- ===================================================================
-- Seed department records for specialized clinics
-- ===================================================================

INSERT INTO departments (code, name, head, doctors, nurses, beds, location, phone, status)
VALUES
  ('MCH', 'Mother & Child Health', '', 1, 3, 0, 'Block C', '', 'Active'),
  ('ART', 'ART / HIV Clinic', '', 1, 2, 0, 'Block D', '', 'Active'),
  ('DENTAL', 'Dental Clinic', '', 1, 1, 0, 'Block A', '', 'Active'),
  ('EYE', 'Eye Clinic', '', 1, 1, 0, 'Block A', '', 'Active'),
  ('STI', 'STI Clinic', '', 1, 1, 0, 'Block D', '', 'Active'),
  ('PHYSIO', 'Physiotherapy', '', 0, 0, 0, 'Block B', '', 'Active')
ON CONFLICT (code) DO NOTHING;

COMMIT;
