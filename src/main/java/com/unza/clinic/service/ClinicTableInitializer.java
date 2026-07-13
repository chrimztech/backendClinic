package com.unza.clinic.service;

import com.unza.clinic.repository.DrugInteractionRepository;
import com.unza.clinic.repository.LabReferenceRangeRepository;
import com.unza.clinic.model.DrugInteraction;
import com.unza.clinic.model.LabReferenceRange;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Creates specialty clinic tables that have no JPA entity (accessed via raw JDBC).
 * Uses CREATE TABLE IF NOT EXISTS so it is safe to run on every startup.
 * Also seeds reference data (drug interactions, lab reference ranges) if empty.
 */
@Component
public class ClinicTableInitializer {

    private final JdbcTemplate jdbc;
    private final DrugInteractionRepository drugInteractionRepo;
    private final LabReferenceRangeRepository labReferenceRangeRepo;

    public ClinicTableInitializer(JdbcTemplate jdbc,
                                  DrugInteractionRepository drugInteractionRepo,
                                  LabReferenceRangeRepository labReferenceRangeRepo) {
        this.jdbc = jdbc;
        this.drugInteractionRepo = drugInteractionRepo;
        this.labReferenceRangeRepo = labReferenceRangeRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initTables() {
        createMchTables();
        createArtTables();
        createDentalTable();
        createEyeTable();
        createStiTable();
        createPhysioTables();
        createPharmacyDispensingLogTable();
        seedDrugInteractions();
        seedLabReferenceRanges();
    }

    // ─── MCH ────────────────────────────────────────────────────────────────

    private void createMchTables() {
        run("""
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
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        run("""
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
            )
            """);

        run("""
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
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);
    }

    // ─── ART ────────────────────────────────────────────────────────────────

    private void createArtTables() {
        run("""
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
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        run("""
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
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);
    }

    // ─── Dental ─────────────────────────────────────────────────────────────

    private void createDentalTable() {
        run("""
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
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);
    }

    // ─── Eye Clinic ──────────────────────────────────────────────────────────

    private void createEyeTable() {
        run("""
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
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);
    }

    // ─── STI ─────────────────────────────────────────────────────────────────

    private void createStiTable() {
        run("""
            CREATE TABLE IF NOT EXISTS sti_records (
                id BIGSERIAL PRIMARY KEY,
                visit_id VARCHAR(50) UNIQUE NOT NULL,
                patient_id VARCHAR(50) NOT NULL,
                patient_name VARCHAR(200) NOT NULL,
                visit_date VARCHAR(20),
                clinician_name VARCHAR(100),
                chief_complaint TEXT,
                syndrome_classification VARCHAR(100),
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
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);
    }

    // ─── Physiotherapy ───────────────────────────────────────────────────────

    private void createPhysioTables() {
        run("""
            CREATE TABLE IF NOT EXISTS physio_referrals (
                id BIGSERIAL PRIMARY KEY,
                referral_id VARCHAR(50) UNIQUE NOT NULL,
                patient_id VARCHAR(50) NOT NULL,
                patient_name VARCHAR(200) NOT NULL,
                referred_by VARCHAR(100),
                referral_date VARCHAR(20),
                diagnosis TEXT,
                referral_reason TEXT,
                urgency VARCHAR(20),
                assigned_therapist VARCHAR(100),
                first_appointment VARCHAR(20),
                status VARCHAR(20) NOT NULL DEFAULT 'pending',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        run("""
            CREATE TABLE IF NOT EXISTS physio_sessions (
                id BIGSERIAL PRIMARY KEY,
                session_id VARCHAR(50) UNIQUE NOT NULL,
                referral_id VARCHAR(50),
                patient_id VARCHAR(50) NOT NULL,
                patient_name VARCHAR(200) NOT NULL,
                session_date VARCHAR(20),
                therapist_name VARCHAR(100),
                session_number INTEGER,
                diagnosis TEXT,
                treatment_given TEXT,
                pain_score_before INTEGER,
                pain_score_after INTEGER,
                mobility_before VARCHAR(50),
                mobility_after VARCHAR(50),
                exercises_done TEXT,
                home_program TEXT,
                progress_notes TEXT,
                next_session VARCHAR(20),
                status VARCHAR(20) NOT NULL DEFAULT 'completed',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);
    }

    // ─── Pharmacy Dispensing Log ──────────────────────────────────────────────

    private void createPharmacyDispensingLogTable() {
        run("""
            CREATE TABLE IF NOT EXISTS pharmacy_dispensing_log (
                id BIGSERIAL PRIMARY KEY,
                log_id VARCHAR(50) UNIQUE NOT NULL,
                rx_id VARCHAR(50),
                patient_id VARCHAR(50),
                patient_name VARCHAR(200),
                patient_type VARCHAR(30),
                drug_name VARCHAR(200),
                quantity_requested INTEGER,
                quantity_dispensed INTEGER,
                unit VARCHAR(50),
                dispensing_instructions TEXT,
                pharmacist_name VARCHAR(100),
                pharmacist_notes TEXT,
                is_controlled BOOLEAN DEFAULT FALSE,
                substituted BOOLEAN DEFAULT FALSE,
                substitute_reason TEXT,
                dispensed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);
    }

    // ─── Seed reference data ──────────────────────────────────────────────────

    private void seedDrugInteractions() {
        if (drugInteractionRepo.count() > 0) return;
        String[][] interactions = {
            {"DI001","Warfarin","Aspirin","SEVERE","Anticoagulant + antiplatelet combination","Increased risk of bleeding","Avoid combination; use alternative analgesic"},
            {"DI002","Metronidazole","Alcohol","SEVERE","Disulfiram-like reaction","Flushing, vomiting, tachycardia","Instruct patient to avoid alcohol during treatment and 48h after"},
            {"DI003","Cotrimoxazole","Warfarin","SEVERE","Sulfonamide inhibits warfarin metabolism","Enhanced anticoagulant effect, bleeding risk","Monitor INR closely; reduce warfarin dose if needed"},
            {"DI004","Fluconazole","Metformin","MODERATE","CYP2C9 interaction","Increased metformin levels","Monitor blood glucose; adjust metformin dose"},
            {"DI005","Rifampicin","Oral Contraceptives","SEVERE","Enzyme induction reduces contraceptive efficacy","Contraceptive failure, unintended pregnancy","Use additional contraception; inform patient"},
            {"DI006","Tetracycline","Iron Supplements","MODERATE","Chelation reduces tetracycline absorption","Reduced antibiotic efficacy","Separate administration by at least 2 hours"},
            {"DI007","Ibuprofen","ACE Inhibitors","MODERATE","NSAIDs antagonise antihypertensive effect","Reduced blood pressure control; renal impairment","Use paracetamol as alternative; monitor BP and renal function"},
            {"DI008","Amoxicillin","Methotrexate","SEVERE","Reduced renal excretion of methotrexate","Methotrexate toxicity","Avoid; use alternative antibiotic"},
            {"DI009","Chloroquine","Antacids","MODERATE","Antacids reduce chloroquine absorption","Reduced antimalarial efficacy","Separate by at least 4 hours"},
            {"DI010","Efavirenz","Rifampicin","MODERATE","Rifampicin induces efavirenz metabolism","Reduced efavirenz levels; ART failure risk","Increase efavirenz dose to 800mg daily"},
            {"DI011","Nevirapine","Fluconazole","MODERATE","Fluconazole inhibits nevirapine metabolism","Increased nevirapine levels, hepatotoxicity","Monitor liver function tests"},
            {"DI012","Digoxin","Erythromycin","SEVERE","Gut flora alteration increases digoxin absorption","Digoxin toxicity","Monitor digoxin levels and ECG"},
            {"DI013","Paracetamol","Alcohol","SEVERE","Hepatotoxic combination","Liver failure risk with chronic alcohol use","Limit paracetamol dose; counsel on alcohol avoidance"},
            {"DI014","Ciprofloxacin","Calcium/Dairy","MODERATE","Chelation reduces ciprofloxacin absorption","Reduced antibiotic efficacy","Take on empty stomach; avoid dairy products within 2h"},
            {"DI015","Isoniazid","Phenytoin","MODERATE","INH inhibits phenytoin metabolism","Phenytoin toxicity (ataxia, nystagmus)","Monitor phenytoin levels; adjust dose"},
        };
        for (String[] row : interactions) {
            DrugInteraction di = new DrugInteraction();
            di.setInteractionId(row[0]);
            di.setDrugA(row[1]);
            di.setDrugB(row[2]);
            di.setSeverity(row[3]);
            di.setDescription(row[4]);
            di.setClinicalEffect(row[5]);
            di.setManagement(row[6]);
            di.setUpdatedAt(LocalDateTime.now());
            drugInteractionRepo.save(di);
        }
    }

    private void seedLabReferenceRanges() {
        if (labReferenceRangeRepo.count() > 0) return;
        addRange("LR001","Haemoglobin","Haematology","MALE",18,999,13.0,17.5,7.0,20.0,"g/dL","Anaemia","Polycythaemia");
        addRange("LR002","Haemoglobin","Haematology","FEMALE",18,999,11.5,15.5,7.0,20.0,"g/dL","Anaemia","Polycythaemia");
        addRange("LR003","Haemoglobin","Haematology","ALL",0,17,10.5,15.5,7.0,20.0,"g/dL","Anaemia (paediatric)","Polycythaemia");
        addRange("LR004","White Blood Cell Count","Haematology","ALL",0,999,4.0,11.0,2.0,30.0,"×10⁹/L","Leucopoenia — infection/drug/autoimmune risk","Leucocytosis — infection or malignancy");
        addRange("LR005","Platelet Count","Haematology","ALL",0,999,150.0,400.0,50.0,1000.0,"×10⁹/L","Thrombocytopoenia — bleeding risk","Thrombocytosis");
        addRange("LR006","Random Blood Glucose","Biochemistry","ALL",0,999,3.9,7.8,2.5,20.0,"mmol/L","Hypoglycaemia","Hyperglycaemia / possible diabetes");
        addRange("LR007","Fasting Blood Glucose","Biochemistry","ALL",0,999,3.9,5.5,2.5,20.0,"mmol/L","Hypoglycaemia","Impaired fasting glucose / diabetes");
        addRange("LR008","Serum Creatinine","Biochemistry","MALE",18,999,62.0,115.0,30.0,400.0,"μmol/L","Low muscle mass","Renal impairment");
        addRange("LR009","Serum Creatinine","Biochemistry","FEMALE",18,999,44.0,97.0,30.0,400.0,"μmol/L","Low muscle mass","Renal impairment");
        addRange("LR010","ALT (Alanine Aminotransferase)","Biochemistry","MALE",18,999,7.0,56.0,null,200.0,"U/L",null,"Hepatocellular damage");
        addRange("LR011","ALT (Alanine Aminotransferase)","Biochemistry","FEMALE",18,999,7.0,45.0,null,200.0,"U/L",null,"Hepatocellular damage");
        addRange("LR012","CD4 Count","Immunology","ALL",0,999,500.0,1500.0,50.0,null,"cells/μL","Immunosuppression — ART review",null);
        addRange("LR013","Urine Protein (Dipstick)","Urinalysis","ALL",0,999,0.0,0.0,null,null,"g/L",null,"Proteinuria — renal/pre-eclampsia");
        addRange("LR014","Serum Sodium","Biochemistry","ALL",0,999,136.0,145.0,120.0,155.0,"mmol/L","Hyponatraemia","Hypernatraemia");
        addRange("LR015","Serum Potassium","Biochemistry","ALL",0,999,3.5,5.0,2.5,6.5,"mmol/L","Hypokalaemia — arrhythmia risk","Hyperkalaemia — cardiac risk");
        addRange("LR016","ESR","Haematology","MALE",18,999,0.0,15.0,null,100.0,"mm/hr",null,"Elevated — infection/inflammation/malignancy");
        addRange("LR017","ESR","Haematology","FEMALE",18,999,0.0,20.0,null,100.0,"mm/hr",null,"Elevated — infection/inflammation/malignancy");
        addRange("LR018","Total Bilirubin","Biochemistry","ALL",0,999,0.0,20.5,null,50.0,"μmol/L",null,"Jaundice — hepatic/haemolytic");
        addRange("LR019","Sputum AFB","Microbiology","ALL",0,999,null,null,null,null,"qualitative","Negative — AFB not seen","Positive — TB likely; grade 1+/2+/3+");
        addRange("LR020","Malaria RDT","Parasitology","ALL",0,999,null,null,null,null,"qualitative","Negative","Positive — P. falciparum / P. vivax");
    }

    private void addRange(String id, String test, String cat, String gender,
                          int minAge, int maxAge, Double minVal, Double maxVal,
                          Double critLow, Double critHigh, String unit,
                          String intLow, String intHigh) {
        LabReferenceRange r = new LabReferenceRange();
        r.setRangeId(id);
        r.setTestName(test);
        r.setCategory(cat);
        r.setGender(gender);
        r.setMinAge(minAge);
        r.setMaxAge(maxAge);
        r.setMinValue(minVal != null ? BigDecimal.valueOf(minVal) : null);
        r.setMaxValue(maxVal != null ? BigDecimal.valueOf(maxVal) : null);
        r.setCriticalLow(critLow != null ? BigDecimal.valueOf(critLow) : null);
        r.setCriticalHigh(critHigh != null ? BigDecimal.valueOf(critHigh) : null);
        r.setUnit(unit);
        r.setInterpretationLow(intLow);
        r.setInterpretationHigh(intHigh);
        labReferenceRangeRepo.save(r);
    }

    private void run(String sql) {
        try {
            jdbc.execute(sql);
        } catch (Exception ignored) {
            // Table already exists or unsupported — silently skip
        }
    }
}
