-- ===================================================================
-- UNZA Clinic Database Schema - Version 1
-- Created: 2026-05-05
-- Description: Complete schema for all clinic entities
-- ===================================================================

-- Patients table
CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    patient_id VARCHAR(50) UNIQUE NOT NULL,
    clinic_number VARCHAR(50) UNIQUE NOT NULL,
    patient_type VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    name VARCHAR(200) NOT NULL,
    age INTEGER,
    gender VARCHAR(20),
    dob VARCHAR(20),
    phone VARCHAR(30),
    email VARCHAR(100),
    address TEXT,
    blood_group VARCHAR(10),
    student_id VARCHAR(50),
    man_number VARCHAR(50),
    program VARCHAR(200),
    school VARCHAR(200),
    year INTEGER,
    hostel VARCHAR(100),
    emergency_contact VARCHAR(100),
    emergency_phone VARCHAR(30),
    emergency_relation VARCHAR(50),
    allergies TEXT,
    conditions TEXT,
    insurance VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_patients_patient_id ON patients(patient_id);
CREATE INDEX idx_patients_clinic_number ON patients(clinic_number);
CREATE INDEX idx_patients_status ON patients(status);

-- Staff Members table
CREATE TABLE IF NOT EXISTS staff_members (
    id BIGSERIAL PRIMARY KEY,
    staff_id VARCHAR(50) UNIQUE NOT NULL,
    man_number VARCHAR(50) UNIQUE,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    department VARCHAR(100),
    phone VARCHAR(30),
    email VARCHAR(100),
    specialization VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_staff_staff_id ON staff_members(staff_id);
CREATE INDEX idx_staff_man_number ON staff_members(man_number);
CREATE INDEX idx_staff_department ON staff_members(department);

-- Departments table
CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    head VARCHAR(100),
    doctors INTEGER DEFAULT 0,
    nurses INTEGER DEFAULT 0,
    beds INTEGER DEFAULT 0,
    location VARCHAR(200),
    phone VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_departments_code ON departments(code);
CREATE INDEX idx_departments_name ON departments(name);

-- App Users table (authentication)
CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    role VARCHAR(50) NOT NULL,
    department VARCHAR(100),
    staff_id VARCHAR(50),
    man_number VARCHAR(50),
    password VARCHAR(255) NOT NULL,
    permissions_json TEXT,
    force_password_change BOOLEAN DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff FOREIGN KEY (staff_id) REFERENCES staff_members(staff_id) ON DELETE SET NULL
);

CREATE INDEX idx_app_users_email ON app_users(email);
CREATE INDEX idx_app_users_user_id ON app_users(user_id);
CREATE INDEX idx_app_users_role ON app_users(role);

-- Appointments table
CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    appointment_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    doctor_id VARCHAR(50),
    doctor_name VARCHAR(100),
    department VARCHAR(100),
    date VARCHAR(20),
    time VARCHAR(20),
    type VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'scheduled',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_appointments_patient_id ON appointments(patient_id);
CREATE INDEX idx_appointments_date ON appointments(date);
CREATE INDEX idx_appointments_status ON appointments(status);

-- Prescriptions table
CREATE TABLE IF NOT EXISTS prescriptions (
    id BIGSERIAL PRIMARY KEY,
    rx_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    patient_id_num VARCHAR(50),
    doctor VARCHAR(100),
    date VARCHAR(20),
    items TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_prescriptions_patient_id ON prescriptions(patient_id);
CREATE INDEX idx_prescriptions_rx_id ON prescriptions(rx_id);
CREATE INDEX idx_prescriptions_status ON prescriptions(status);

-- Admissions table
CREATE TABLE IF NOT EXISTS admissions (
    id BIGSERIAL PRIMARY KEY,
    admission_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    ward VARCHAR(100),
    bed VARCHAR(20),
    doctor VARCHAR(100),
    admitted_on VARCHAR(20),
    diagnosis TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_admissions_patient_id ON admissions(patient_id);
CREATE INDEX idx_admissions_admission_id ON admissions(admission_id);
CREATE INDEX idx_admissions_status ON admissions(status);
CREATE INDEX idx_admissions_ward ON admissions(ward);

-- Laboratory Tests table
CREATE TABLE IF NOT EXISTS lab_tests (
    id BIGSERIAL PRIMARY KEY,
    test_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    test VARCHAR(200),
    requested_by VARCHAR(100),
    date VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    results TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_lab_tests_patient_id ON lab_tests(patient_id);
CREATE INDEX idx_lab_tests_test_id ON lab_tests(test_id);
CREATE INDEX idx_lab_tests_status ON lab_tests(status);

-- Billing Invoices table
CREATE TABLE IF NOT EXISTS billing_invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    items TEXT,
    line_items_json TEXT,
    subtotal DECIMAL(10,2) DEFAULT 0,
    tax DECIMAL(10,2) DEFAULT 0,
    total DECIMAL(10,2) DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    due_date VARCHAR(20),
    paid_date VARCHAR(20),
    payment_method VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_billing_invoices_patient_id ON billing_invoices(patient_id);
CREATE INDEX idx_billing_invoices_invoice_id ON billing_invoices(invoice_id);
CREATE INDEX idx_billing_invoices_status ON billing_invoices(status);

-- Service Tariffs table
CREATE TABLE IF NOT EXISTS service_tariffs (
    id BIGSERIAL PRIMARY KEY,
    tariff_code VARCHAR(50) UNIQUE NOT NULL,
    department VARCHAR(100) NOT NULL,
    category VARCHAR(100),
    service_name VARCHAR(200) NOT NULL,
    unit_label VARCHAR(50),
    price DECIMAL(10,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tariffs_tariff_code ON service_tariffs(tariff_code);
CREATE INDEX idx_tariffs_department ON service_tariffs(department);
CREATE INDEX idx_tariffs_category ON service_tariffs(category);

-- Inventory Records table
CREATE TABLE IF NOT EXISTS inventory_records (
    id BIGSERIAL PRIMARY KEY,
    item_code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(100),
    quantity INTEGER NOT NULL DEFAULT 0,
    unit VARCHAR(20),
    min_stock INTEGER NOT NULL DEFAULT 0,
    location VARCHAR(200),
    last_restocked VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'in-stock',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_inventory_item_code ON inventory_records(item_code);
CREATE INDEX idx_inventory_category ON inventory_records(category);
CREATE INDEX idx_inventory_status ON inventory_records(status);

-- Suppliers table
CREATE TABLE IF NOT EXISTS suppliers (
    id BIGSERIAL PRIMARY KEY,
    supplier_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    contact VARCHAR(100),
    phone VARCHAR(30),
    email VARCHAR(100),
    items INTEGER DEFAULT 0,
    last_order VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_suppliers_supplier_id ON suppliers(supplier_id);
CREATE INDEX idx_suppliers_name ON suppliers(name);

-- Drugs table
CREATE TABLE IF NOT EXISTS drugs (
    id BIGSERIAL PRIMARY KEY,
    drug_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(100),
    drug_type VARCHAR(100),
    batch_number VARCHAR(100),
    stock INTEGER NOT NULL DEFAULT 0,
    reorder_level INTEGER NOT NULL DEFAULT 50,
    unit VARCHAR(20),
    expiry VARCHAR(20),
    storage_location VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'available',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_drugs_drug_id ON drugs(drug_id);
CREATE INDEX idx_drugs_name ON drugs(name);
CREATE INDEX idx_drugs_category ON drugs(category);
CREATE INDEX idx_drugs_status ON drugs(status);

-- Imaging Requests table
CREATE TABLE IF NOT EXISTS imaging_requests (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    type VARCHAR(100),
    body_part VARCHAR(100),
    requested_by VARCHAR(100),
    request_date VARCHAR(20),
    radiologist VARCHAR(100),
    findings TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_imaging_patient_id ON imaging_requests(patient_id);
CREATE INDEX idx_imaging_request_id ON imaging_requests(request_id);
CREATE INDEX idx_imaging_status ON imaging_requests(status);

-- Insurance Claims table
CREATE TABLE IF NOT EXISTS insurance_claims (
    id BIGSERIAL PRIMARY KEY,
    claim_id VARCHAR(50) UNIQUE NOT NULL,
    patient VARCHAR(200),
    insurer VARCHAR(100),
    service VARCHAR(200),
    amount DECIMAL(10,2),
    submitted VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_claims_claim_id ON insurance_claims(claim_id);
CREATE INDEX idx_claims_status ON insurance_claims(status);

-- Referrals table
CREATE TABLE IF NOT EXISTS referrals (
    id BIGSERIAL PRIMARY KEY,
    referral_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    from_dept VARCHAR(100),
    to_dept VARCHAR(100),
    referred_by VARCHAR(100),
    reason TEXT,
    urgency VARCHAR(20),
    date VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_referrals_patient_id ON referrals(patient_id);
CREATE INDEX idx_referrals_referral_id ON referrals(referral_id);
CREATE INDEX idx_referrals_status ON referrals(status);

-- Triage Records table
CREATE TABLE IF NOT EXISTS triage_records (
    id BIGSERIAL PRIMARY KEY,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    level VARCHAR(20),
    chief_complaint TEXT,
    blood_pressure VARCHAR(20),
    temperature DECIMAL(4,1),
    pulse_rate INTEGER,
    respiratory_rate INTEGER,
    oxygen_saturation INTEGER,
    weight_kg DECIMAL(5,2),
    height_cm DECIMAL(5,2),
    bmi DECIMAL(4,2),
    random_blood_sugar DECIMAL(5,2),
    pain_score INTEGER,
    consciousness_level VARCHAR(50),
    notes TEXT,
    vital_signs TEXT,
    nurse_name VARCHAR(100),
    arrival_time VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'waiting',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_triage_patient_id ON triage_records(patient_id);
CREATE INDEX idx_triage_status ON triage_records(status);
CREATE INDEX idx_triage_level ON triage_records(level);

-- Queue Tickets table
CREATE TABLE IF NOT EXISTS queue_tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_no VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50),
    patient_name VARCHAR(200) NOT NULL,
    department VARCHAR(100) NOT NULL,
    priority VARCHAR(20) DEFAULT 'normal',
    check_in_time VARCHAR(20),
    wait_time VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'waiting',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_queue_ticket_no ON queue_tickets(ticket_no);
CREATE INDEX idx_queue_department ON queue_tickets(department);
CREATE INDEX idx_queue_status ON queue_tickets(status);

-- Emergency Cases table
CREATE TABLE IF NOT EXISTS emergency_cases (
    id BIGSERIAL PRIMARY KEY,
    case_id VARCHAR(50) UNIQUE NOT NULL,
    patient_name VARCHAR(200),
    age INTEGER,
    gender VARCHAR(20),
    severity VARCHAR(20),
    chief_complaint TEXT,
    arrival_mode VARCHAR(50),
    arrival_time VARCHAR(20),
    attending_doctor VARCHAR(100),
    nurse_on_duty VARCHAR(100),
    vitals TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_emergency_case_id ON emergency_cases(case_id);
CREATE INDEX idx_emergency_status ON emergency_cases(status);
CREATE INDEX idx_emergency_severity ON emergency_cases(severity);

-- Blood Units table
CREATE TABLE IF NOT EXISTS blood_units (
    id BIGSERIAL PRIMARY KEY,
    unit_id VARCHAR(50) UNIQUE NOT NULL,
    blood_type VARCHAR(10) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'available',
    expiry_date VARCHAR(20),
    donor_name VARCHAR(100),
    collection_date VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_blood_units_unit_id ON blood_units(unit_id);
CREATE INDEX idx_blood_units_blood_type ON blood_units(blood_type);
CREATE INDEX idx_blood_units_status ON blood_units(status);

-- Donations table
CREATE TABLE IF NOT EXISTS donations (
    id BIGSERIAL PRIMARY KEY,
    donor_name VARCHAR(100),
    blood_type VARCHAR(10) NOT NULL,
    units INTEGER DEFAULT 1,
    date VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'completed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_donations_blood_type ON donations(blood_type);
CREATE INDEX idx_donations_date ON donations(date);

-- Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50),
    title VARCHAR(200),
    message TEXT,
    time VARCHAR(20),
    read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_read ON notifications(read);
CREATE INDEX idx_notifications_type ON notifications(type);

-- Audit Logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp VARCHAR(50),
    user_name VARCHAR(100),
    role VARCHAR(50),
    action VARCHAR(50),
    description TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_user_name ON audit_logs(user_name);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);

-- Attendance Records table
CREATE TABLE IF NOT EXISTS attendance_records (
    id BIGSERIAL PRIMARY KEY,
    staff_id VARCHAR(50) NOT NULL,
    name VARCHAR(100),
    role VARCHAR(50),
    department VARCHAR(100),
    shift VARCHAR(50),
    check_in VARCHAR(20),
    check_out VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'on-duty',
    date VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff FOREIGN KEY (staff_id) REFERENCES staff_members(staff_id) ON DELETE CASCADE
);

CREATE INDEX idx_attendance_staff_id ON attendance_records(staff_id);
CREATE INDEX idx_attendance_date ON attendance_records(date);
CREATE INDEX idx_attendance_status ON attendance_records(status);

-- Staff Schedules table
CREATE TABLE IF NOT EXISTS staff_schedules (
    id BIGSERIAL PRIMARY KEY,
    schedule_id VARCHAR(50) UNIQUE NOT NULL,
    staff_id VARCHAR(50) NOT NULL,
    name VARCHAR(100),
    role VARCHAR(50),
    department VARCHAR(100),
    day_of_week VARCHAR(20),
    shift_name VARCHAR(100),
    start_time VARCHAR(20),
    end_time VARCHAR(20),
    location VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'Scheduled',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_sched FOREIGN KEY (staff_id) REFERENCES staff_members(staff_id) ON DELETE CASCADE
);

CREATE INDEX idx_schedules_schedule_id ON staff_schedules(schedule_id);
CREATE INDEX idx_schedules_staff_id ON staff_schedules(staff_id);
CREATE INDEX idx_schedules_day ON staff_schedules(day_of_week);

-- Clinical Forms table
CREATE TABLE IF NOT EXISTS clinical_forms (
    id BIGSERIAL PRIMARY KEY,
    form_id VARCHAR(50) UNIQUE NOT NULL,
    form_type VARCHAR(100) NOT NULL,
    title VARCHAR(200),
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    department VARCHAR(100),
    encounter_id VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payload_json TEXT,
    CONSTRAINT fk_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_clinical_forms_form_id ON clinical_forms(form_id);
CREATE INDEX idx_clinical_forms_patient_id ON clinical_forms(patient_id);
CREATE INDEX idx_clinical_forms_type ON clinical_forms(form_type);
CREATE INDEX idx_clinical_forms_status ON clinical_forms(status);

-- Wards table
CREATE TABLE IF NOT EXISTS wards (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    total_beds INTEGER NOT NULL DEFAULT 0,
    occupied INTEGER NOT NULL DEFAULT 0,
    available INTEGER NOT NULL DEFAULT 0,
    bed_board TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wards_name ON wards(name);

-- System Settings table
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGSERIAL PRIMARY KEY,
    hospital_name VARCHAR(200),
    contact_phone VARCHAR(30),
    address TEXT,
    email_notifications BOOLEAN DEFAULT TRUE,
    sms_notifications BOOLEAN DEFAULT TRUE,
    low_stock_alerts BOOLEAN DEFAULT TRUE,
    two_factor_auth BOOLEAN DEFAULT TRUE,
    audit_logging BOOLEAN DEFAULT TRUE,
    auto_logout BOOLEAN DEFAULT TRUE,
    backup_enabled BOOLEAN DEFAULT TRUE,
    backup_frequency VARCHAR(20) DEFAULT 'Daily',
    backup_location VARCHAR(200),
    last_backup_at VARCHAR(50),
    demo_data_cleared BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_settings_id ON system_settings(id);

-- Encounters table (patient workflow tracking)
CREATE TABLE IF NOT EXISTS encounter_records (
    id BIGSERIAL PRIMARY KEY,
    encounter_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    patient_type VARCHAR(20) DEFAULT 'GENERAL',
    current_stage VARCHAR(50) NOT NULL DEFAULT 'RECEPTION',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',
    checkout_eligible BOOLEAN DEFAULT FALSE,
    checked_out BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    checkout_time VARCHAR(50),
    stage_history TEXT,
    pending_actions TEXT,
    completed_actions TEXT,
    notes TEXT,
    CONSTRAINT fk_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX idx_encounters_encounter_id ON encounter_records(encounter_id);
CREATE INDEX idx_encounters_patient_id ON encounter_records(patient_id);
CREATE INDEX idx_encounters_current_stage ON encounter_records(current_stage);
CREATE INDEX idx_encounters_checked_out ON encounter_records(checked_out);

-- Billing Line Items table (for detailed invoice items)
CREATE TABLE IF NOT EXISTS billing_line_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id VARCHAR(50) NOT NULL,
    tariff_code VARCHAR(50),
    service_name VARCHAR(200),
    department VARCHAR(100),
    quantity INTEGER DEFAULT 1,
    unit_price DECIMAL(10,2) DEFAULT 0,
    line_total DECIMAL(10,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice FOREIGN KEY (invoice_id) REFERENCES billing_invoices(invoice_id) ON DELETE CASCADE
);

CREATE INDEX idx_line_items_invoice_id ON billing_line_items(invoice_id);

-- ===================================================================
-- Initial Data Seeding for Critical Tables
-- ===================================================================

-- Insert default system settings
INSERT INTO system_settings (id, hospital_name, contact_phone, address, email_notifications, sms_notifications, low_stock_alerts, two_factor_auth, audit_logging, auto_logout, backup_enabled, backup_frequency, backup_location, last_backup_at, demo_data_cleared)
VALUES (1, 'University of Zambia Clinic', '+260 211 290000', 'Great East Road, Lusaka, Zambia', true, true, true, true, true, true, true, 'Daily', 'Local secure server', CURRENT_TIMESTAMP::VARCHAR, false)
ON CONFLICT (id) DO NOTHING;

-- Create initial admin user (password: admin123 - BCrypt hash would be $2a$10$... in production)
INSERT INTO app_users (user_id, name, email, role, department, staff_id, man_number, password, permissions_json, force_password_change, status, last_login)
VALUES ('USR-001', 'Admin User', 'admin@unza.zm', 'Admin', 'IT', '', '', 'admin123', '[]', true, 'active', '')
ON CONFLICT (email) DO NOTHING;

COMMIT;
