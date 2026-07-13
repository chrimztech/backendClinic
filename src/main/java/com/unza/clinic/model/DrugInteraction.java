package com.unza.clinic.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "drug_interactions")
public class DrugInteraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String interactionId;
    private String drugA;
    private String drugB;
    private String severity;  // MILD, MODERATE, SEVERE, CONTRAINDICATED

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String clinicalEffect;

    @Column(columnDefinition = "TEXT")
    private String management;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DrugInteraction() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInteractionId() { return interactionId; }
    public void setInteractionId(String interactionId) { this.interactionId = interactionId; }
    public String getDrugA() { return drugA; }
    public void setDrugA(String drugA) { this.drugA = drugA; }
    public String getDrugB() { return drugB; }
    public void setDrugB(String drugB) { this.drugB = drugB; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getClinicalEffect() { return clinicalEffect; }
    public void setClinicalEffect(String clinicalEffect) { this.clinicalEffect = clinicalEffect; }
    public String getManagement() { return management; }
    public void setManagement(String management) { this.management = management; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
