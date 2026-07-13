package com.unza.clinic.repository;

import com.unza.clinic.model.QueueTicket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueRepository extends JpaRepository<QueueTicket, Long> {
}