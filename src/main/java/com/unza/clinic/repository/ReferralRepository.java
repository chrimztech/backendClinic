package com.unza.clinic.repository;

import com.unza.clinic.model.ReferralRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralRepository extends JpaRepository<ReferralRecord, Long> {
}