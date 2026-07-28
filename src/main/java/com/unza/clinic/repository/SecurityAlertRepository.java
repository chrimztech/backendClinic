package com.unza.clinic.repository;

import com.unza.clinic.model.SecurityAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {
    List<SecurityAlert> findAllByOrderByOccurredAtDesc();
}
