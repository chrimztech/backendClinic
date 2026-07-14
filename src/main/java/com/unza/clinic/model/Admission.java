package com.unza.clinic.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admissions")
public class Admission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String admissionId;
    private String patientId;
    private String patientName;
    private String ward;
    private String bed;
    private String doctor;
    private String admittedOn;
    private String diagnosis;
    private String status;
    private String dischargeType;
    private String dischargeSummary;
    private String dischargedOn;
    private String dischargedBy;

    public Admission() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAdmissionId() { return admissionId; }
    public void setAdmissionId(String admissionId) { this.admissionId = admissionId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }
    public String getBed() { return bed; }
    public void setBed(String bed) { this.bed = bed; }
    public String getDoctor() { return doctor; }
    public void setDoctor(String doctor) { this.doctor = doctor; }
    public String getAdmittedOn() { return admittedOn; }
    public void setAdmittedOn(String admittedOn) { this.admittedOn = admittedOn; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDischargeType() { return dischargeType; }
    public void setDischargeType(String dischargeType) { this.dischargeType = dischargeType; }
    public String getDischargeSummary() { return dischargeSummary; }
    public void setDischargeSummary(String dischargeSummary) { this.dischargeSummary = dischargeSummary; }
    public String getDischargedOn() { return dischargedOn; }
    public void setDischargedOn(String dischargedOn) { this.dischargedOn = dischargedOn; }
    public String getDischargedBy() { return dischargedBy; }
    public void setDischargedBy(String dischargedBy) { this.dischargedBy = dischargedBy; }
}