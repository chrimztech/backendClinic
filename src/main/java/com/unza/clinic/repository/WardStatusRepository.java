package com.unza.clinic.repository;

import com.unza.clinic.model.WardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WardStatusRepository extends JpaRepository<WardStatus, Long> {
}