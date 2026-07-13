package com.unza.clinic.repository;

import com.unza.clinic.model.BloodUnit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloodUnitRepository extends JpaRepository<BloodUnit, Long> {
}