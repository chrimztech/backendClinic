package com.unza.clinic.repository;

import com.unza.clinic.model.NotificationItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationItem, Long> {
}