package com.unza.clinic.repository;

import com.unza.clinic.model.ServiceTariff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceTariffRepository extends JpaRepository<ServiceTariff, Long> {
}
