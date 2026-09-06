package com.unza.clinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "departments")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String name;
    private String head;
    @Column(name = "head_user_id")
    private String headUserId;
    private Integer clinicians;
    private Integer doctors;
    private Integer nurses;
    private Integer beds;
    private String location;
    private String phone;
    private String status;

    public Department() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHead() { return head; }
    public void setHead(String head) { this.head = head; }
    public String getHeadUserId() { return headUserId; }
    public void setHeadUserId(String headUserId) { this.headUserId = headUserId; }
    public Integer getClinicians() { return clinicians != null ? clinicians : doctors; }
    public void setClinicians(Integer clinicians) { this.clinicians = clinicians; }
    /** @deprecated retained only for databases and clients created before the clinician terminology change. */
    @Deprecated
    public Integer getDoctors() { return doctors; }
    /** @deprecated use {@link #setClinicians(Integer)}. */
    @Deprecated
    public void setDoctors(Integer doctors) { this.doctors = doctors; }
    public Integer getNurses() { return nurses; }
    public void setNurses(Integer nurses) { this.nurses = nurses; }
    public Integer getBeds() { return beds; }
    public void setBeds(Integer beds) { this.beds = beds; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
