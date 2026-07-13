package com.unza.clinic.repository;

import com.unza.clinic.model.TriageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TriageRepository extends JpaRepository<TriageRecord, Long> {
}