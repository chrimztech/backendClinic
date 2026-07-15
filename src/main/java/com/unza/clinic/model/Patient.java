package com.unza.clinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "patient_id")
    private String patientId;
    private String clinicNumber;
    private String patientType;
    private String name;
    private Integer age;
    private String gender;
    private String dob;
    private String phone;
    private String email;
    private String address;
    @Column(name = "blood_group")
    private String bloodGroup;
    @Column(name = "student_id")
    private String studentId;
    private String manNumber;
    private String program;
    private String school;
    private Integer year;
    private String hostel;
    private String department;
    private String role;
    @Column(name = "emergency_contact")
    private String emergencyContact;
    @Column(name = "emergency_phone")
    private String emergencyPhone;
    @Column(name = "emergency_relation")
    private String emergencyRelation;
    private String allergies;
    private String conditions;
    private String insurance;
    private String status;

    public Patient() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getClinicNumber() { return clinicNumber; }
    public void setClinicNumber(String clinicNumber) { this.clinicNumber = clinicNumber; }
    public String getPatientType() { return patientType; }
    public void setPatientType(String patientType) { this.patientType = patientType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getManNumber() { return manNumber; }
    public void setManNumber(String manNumber) { this.manNumber = manNumber; }
    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }
    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getHostel() { return hostel; }
    public void setHostel(String hostel) { this.hostel = hostel; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }
    public String getEmergencyRelation() { return emergencyRelation; }
    public void setEmergencyRelation(String emergencyRelation) { this.emergencyRelation = emergencyRelation; }
    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }
    public String getInsurance() { return insurance; }
    public void setInsurance(String insurance) { this.insurance = insurance; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
