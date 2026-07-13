package com.unza.clinic.repository;

import com.unza.clinic.model.InventoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryRecord, Long> {
}