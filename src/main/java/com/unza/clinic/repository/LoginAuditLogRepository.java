package com.unza.clinic.repository;

import com.unza.clinic.model.LoginAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface LoginAuditLogRepository extends JpaRepository<LoginAuditLog, Long> {
    List<LoginAuditLog> findByEmailIgnoreCase(String email);
    List<LoginAuditLog> findByIpAddress(String ipAddress);
    List<LoginAuditLog> findBySuccess(boolean success);

    @Query("SELECT l FROM LoginAuditLog l WHERE l.ipAddress = :ip AND l.success = false AND l.loggedAt >= :since")
    List<LoginAuditLog> findRecentFailuresByIp(@Param("ip") String ip, @Param("since") LocalDateTime since);

    List<LoginAuditLog> findTop100ByOrderByLoggedAtDesc();
}
