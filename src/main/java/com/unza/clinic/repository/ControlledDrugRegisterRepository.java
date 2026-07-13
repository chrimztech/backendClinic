package com.unza.clinic.repository;

import com.unza.clinic.model.ControlledDrugRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ControlledDrugRegisterRepository extends JpaRepository<ControlledDrugRegister, Long> {
    Optional<ControlledDrugRegister> findByEntryId(String entryId);
    List<ControlledDrugRegister> findByDrugNameIgnoreCase(String drugName);
    List<ControlledDrugRegister> findByRxId(String rxId);
    List<ControlledDrugRegister> findByPatientId(String patientId);
    List<ControlledDrugRegister> findByDateDispensed(String dateDispensed);
    List<ControlledDrugRegister> findAllByOrderByCreatedAtDesc();
}
