package com.unza.clinic.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A sensitive-case alert (self-harm, suicide, sexual assault, physical attack,
 * panic button, other) visible to university Security staff, regardless of
 * whether it was raised in the clinic or the counselling system.
 *
 * Mirrored 1:1 with the counselling system's own SecurityAlert so both sides
 * can synchronize creation and status updates over HTTP (see
 * ExternalCounselingSecurityAlertController and SecurityAlertSyncService).
 */
@Entity
@Table(name = "security_alerts")
public class SecurityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SecurityAlertCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SecurityAlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_system", nullable = false)
    private SecuritySystem originSystem;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SecurityAlertSourceType sourceType;

    @Column(name = "subject_student_id")
    private String subjectStudentId;

    @Column(name = "subject_name")
    private String subjectName;

    @Column(name = "reported_by_user_id")
    private String reportedByUserId;

    @Column(name = "reported_by_name")
    private String reportedByName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SecurityAlertStatus status;

    @Column(name = "acknowledged_by_name")
    private String acknowledgedByName;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_by_name")
    private String resolvedByName;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "external_alert_id")
    private String externalAlertId;

    @Enumerated(EnumType.STRING)
    @Column(name = "external_system")
    private SecuritySystem externalSystem;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SecurityAlert() {}

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.occurredAt == null) {
            this.occurredAt = now;
        }
        if (this.status == null) {
            this.status = SecurityAlertStatus.NEW;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SecurityAlertCategory getCategory() { return category; }
    public void setCategory(SecurityAlertCategory category) { this.category = category; }

    public SecurityAlertSeverity getSeverity() { return severity; }
    public void setSeverity(SecurityAlertSeverity severity) { this.severity = severity; }

    public SecuritySystem getOriginSystem() { return originSystem; }
    public void setOriginSystem(SecuritySystem originSystem) { this.originSystem = originSystem; }

    public SecurityAlertSourceType getSourceType() { return sourceType; }
    public void setSourceType(SecurityAlertSourceType sourceType) { this.sourceType = sourceType; }

    public String getSubjectStudentId() { return subjectStudentId; }
    public void setSubjectStudentId(String subjectStudentId) { this.subjectStudentId = subjectStudentId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getReportedByUserId() { return reportedByUserId; }
    public void setReportedByUserId(String reportedByUserId) { this.reportedByUserId = reportedByUserId; }

    public String getReportedByName() { return reportedByName; }
    public void setReportedByName(String reportedByName) { this.reportedByName = reportedByName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public SecurityAlertStatus getStatus() { return status; }
    public void setStatus(SecurityAlertStatus status) { this.status = status; }

    public String getAcknowledgedByName() { return acknowledgedByName; }
    public void setAcknowledgedByName(String acknowledgedByName) { this.acknowledgedByName = acknowledgedByName; }

    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public String getResolvedByName() { return resolvedByName; }
    public void setResolvedByName(String resolvedByName) { this.resolvedByName = resolvedByName; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public String getExternalAlertId() { return externalAlertId; }
    public void setExternalAlertId(String externalAlertId) { this.externalAlertId = externalAlertId; }

    public SecuritySystem getExternalSystem() { return externalSystem; }
    public void setExternalSystem(SecuritySystem externalSystem) { this.externalSystem = externalSystem; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
