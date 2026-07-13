package com.unza.clinic.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "insurance_claims")
public class InsuranceClaim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String claimId;
    private String patient;
    private String insurer;
    private String service;
    private Double amount;
    private String submitted;
    private String status;
    private String notes;
    private String approvedAmount;

    public InsuranceClaim() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClaimId() { return claimId; }
    public void setClaimId(String claimId) { this.claimId = claimId; }
    public String getPatient() { return patient; }
    public void setPatient(String patient) { this.patient = patient; }
    public String getInsurer() { return insurer; }
    public void setInsurer(String insurer) { this.insurer = insurer; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getSubmitted() { return submitted; }
    public void setSubmitted(String submitted) { this.submitted = submitted; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(String approvedAmount) { this.approvedAmount = approvedAmount; }
}