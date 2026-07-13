package com.unza.clinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String rxId;
    private String patientId;
    private String patientName;
    private String patientIdNum;
    private String doctor;
    private String date;
    private String items;
    private String drugName;
    private Integer quantity;
    private String dosage;
    private String duration;
    private String instructions;
    private String medicationClass;
    private String program;
    private String status;
    private String dispensedBy;
    private LocalDateTime dispensedAt;
    @Column(columnDefinition = "TEXT")
    private String pharmacistNotes;

    public Prescription() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRxId() { return rxId; }
    public void setRxId(String rxId) { this.rxId = rxId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getPatientIdNum() { return patientIdNum; }
    public void setPatientIdNum(String patientIdNum) { this.patientIdNum = patientIdNum; }
    public String getDoctor() { return doctor; }
    public void setDoctor(String doctor) { this.doctor = doctor; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getItems() { return items; }
    public void setItems(String items) { this.items = items; }
    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public String getMedicationClass() { return medicationClass; }
    public void setMedicationClass(String medicationClass) { this.medicationClass = medicationClass; }
    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDispensedBy() { return dispensedBy; }
    public void setDispensedBy(String dispensedBy) { this.dispensedBy = dispensedBy; }
    public LocalDateTime getDispensedAt() { return dispensedAt; }
    public void setDispensedAt(LocalDateTime dispensedAt) { this.dispensedAt = dispensedAt; }
    public String getPharmacistNotes() { return pharmacistNotes; }
    public void setPharmacistNotes(String pharmacistNotes) { this.pharmacistNotes = pharmacistNotes; }
}
