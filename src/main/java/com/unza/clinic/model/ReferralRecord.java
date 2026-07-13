package com.unza.clinic.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "referral_records")
public class ReferralRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String referralId;
    private String patientId;
    private String patientName;
    private String fromDept;
    private String toDept;
    private String referredBy;
    private String reason;
    private String urgency;
    private String date;
    private String status;
    private String notes;

    public ReferralRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReferralId() { return referralId; }
    public void setReferralId(String referralId) { this.referralId = referralId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getFromDept() { return fromDept; }
    public void setFromDept(String fromDept) { this.fromDept = fromDept; }
    public String getToDept() { return toDept; }
    public void setToDept(String toDept) { this.toDept = toDept; }
    public String getReferredBy() { return referredBy; }
    public void setReferredBy(String referredBy) { this.referredBy = referredBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}