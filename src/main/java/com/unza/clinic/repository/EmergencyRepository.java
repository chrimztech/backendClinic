package com.unza.clinic.repository;

import com.unza.clinic.model.EmergencyCase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyRepository extends JpaRepository<EmergencyCase, Long> {
}