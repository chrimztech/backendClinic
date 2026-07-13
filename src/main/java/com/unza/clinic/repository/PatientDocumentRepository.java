package com.unza.clinic.repository;

import com.unza.clinic.model.PatientDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PatientDocumentRepository extends JpaRepository<PatientDocument, Long> {
    List<PatientDocument> findByPatientId(String patientId);
    List<PatientDocument> findByPatientIdAndStatus(String patientId, String status);
    Optional<PatientDocument> findByDocumentId(String documentId);
    List<PatientDocument> findByEncounterId(String encounterId);
}
