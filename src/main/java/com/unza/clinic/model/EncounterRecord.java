package com.unza.clinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "encounter_records")
public class EncounterRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String encounterId;
    private String patientId;
    private String patientName;
    private String patientType;
    private String currentStage;
    private String paymentStatus;
    private boolean checkoutEligible;
    private boolean checkedOut;
    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String checkoutTime;
    @Lob
    @Column(length = 8000)
    private String stageHistory;
    @Lob
    @Column(length = 4000)
    private String pendingActions;
    @Lob
    @Column(length = 4000)
    private String completedActions;
    @Lob
    @Column(length = 4000)
    private String notes;

    public EncounterRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getPatientType() { return patientType; }
    public void setPatientType(String patientType) { this.patientType = patientType; }
    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public boolean isCheckoutEligible() { return checkoutEligible; }
    public void setCheckoutEligible(boolean checkoutEligible) { this.checkoutEligible = checkoutEligible; }
    public boolean isCheckedOut() { return checkedOut; }
    public void setCheckedOut(boolean checkedOut) { this.checkedOut = checkedOut; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getCheckoutTime() { return checkoutTime; }
    public void setCheckoutTime(String checkoutTime) { this.checkoutTime = checkoutTime; }
    public String getStageHistory() { return stageHistory; }
    public void setStageHistory(String stageHistory) { this.stageHistory = stageHistory; }
    public String getPendingActions() { return pendingActions; }
    public void setPendingActions(String pendingActions) { this.pendingActions = pendingActions; }
    public String getCompletedActions() { return completedActions; }
    public void setCompletedActions(String completedActions) { this.completedActions = completedActions; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
