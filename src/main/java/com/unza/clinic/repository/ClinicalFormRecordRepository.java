package com.unza.clinic.repository;

import com.unza.clinic.model.ClinicalFormRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalFormRecordRepository extends JpaRepository<ClinicalFormRecord, Long> {
}
