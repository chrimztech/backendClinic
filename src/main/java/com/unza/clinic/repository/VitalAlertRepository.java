package com.unza.clinic.repository;

import com.unza.clinic.model.VitalAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VitalAlertRepository extends JpaRepository<VitalAlert, Long> {
    Optional<VitalAlert> findByAlertId(String alertId);
    List<VitalAlert> findByPatientId(String patientId);
    List<VitalAlert> findByAcknowledged(boolean acknowledged);
    List<VitalAlert> findBySeverity(String severity);
    List<VitalAlert> findByPatientIdOrderByCreatedAtDesc(String patientId);
}
