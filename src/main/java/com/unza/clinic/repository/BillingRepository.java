package com.unza.clinic.repository;

import com.unza.clinic.model.BillingInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingRepository extends JpaRepository<BillingInvoice, Long> {
}