package com.unza.clinic.repository;

import com.unza.clinic.model.EncounterRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EncounterRepository extends JpaRepository<EncounterRecord, Long> {
    Optional<EncounterRecord> findByEncounterId(String encounterId);
}
