package com.unza.clinic.service;

import com.lowagie.text.pdf.PdfReader;
import com.unza.clinic.model.Patient;
import com.unza.clinic.model.ReferralRecord;
import com.unza.clinic.repository.AttendanceRepository;
import com.unza.clinic.repository.BillingRepository;
import com.unza.clinic.repository.DrugRepository;
import com.unza.clinic.repository.LabTestRepository;
import com.unza.clinic.repository.PatientRepository;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ExportServiceReferralTests {

    @Test
    void createsBrandedPrintableReferralPdf() throws Exception {
        ExportService service = new ExportService(
                mock(PatientRepository.class),
                mock(BillingRepository.class),
                mock(AttendanceRepository.class),
                mock(DrugRepository.class),
                mock(LabTestRepository.class));

        Patient patient = samplePatient();
        ReferralRecord referral = sampleReferral();
        byte[] pdf = service.exportReferralPdf(referral, patient);

        assertTrue(pdf.length > 8_000);
        assertEquals("%PDF", new String(pdf, 0, 4));
        PdfReader reader = new PdfReader(pdf);
        assertTrue(reader.getNumberOfPages() >= 1);
        reader.close();

        String previewPath = System.getProperty("referral.pdf.preview");
        if (previewPath != null && !previewPath.isBlank()) {
            Path output = Path.of(previewPath).toAbsolutePath();
            Files.createDirectories(output.getParent());
            Files.write(output, pdf);
        }
    }

    private Patient samplePatient() {
        Patient patient = new Patient();
        patient.setPatientId("PAT-2026-0142");
        patient.setClinicNumber("UNZAC-0142");
        patient.setPatientType("STUDENT");
        patient.setName("Naledi Mwewa");
        patient.setDob("2003-08-14");
        patient.setAge(23);
        patient.setGender("Female");
        patient.setPhone("+260 97 000 0142");
        patient.setAllergies("No known drug allergies");
        patient.setConditions("Asthma - well controlled");
        return patient;
    }

    private ReferralRecord sampleReferral() {
        ReferralRecord referral = new ReferralRecord();
        referral.setReferralId("REF-2026-0142");
        referral.setPatientId("PAT-2026-0142");
        referral.setPatientName("Naledi Mwewa");
        referral.setFromDept("General Outpatient Department, UNZA Clinic");
        referral.setToDept("Cardiology Clinic");
        referral.setDestinationFacility("University Teaching Hospital");
        referral.setReferredBy("Dr. Chanda Mwansa");
        referral.setReferringClinicianContact("+260 21 100 0000");
        referral.setReason("Specialist assessment of recurrent exertional palpitations and episodic chest discomfort.");
        referral.setProvisionalDiagnosis("Suspected paroxysmal supraventricular tachycardia");
        referral.setClinicalSummary("Three-week history of intermittent palpitations precipitated by exercise. Episodes last five to ten minutes and settle with rest. No syncope reported.");
        referral.setVitalSigns("BP 118/72 mmHg; pulse 84 bpm regular; SpO2 98% on room air; temperature 36.7 C");
        referral.setInvestigations("Resting ECG: sinus rhythm. Full blood count and thyroid function tests within reference range.");
        referral.setTreatmentGiven("Advised to avoid stimulants, maintain hydration, and return immediately if symptoms worsen.");
        referral.setUrgency("urgent");
        referral.setDate("2026-08-30");
        referral.setStatus("pending");
        referral.setNotes("Kindly assess and advise on ambulatory ECG monitoring and further management.");
        return referral;
    }
}
