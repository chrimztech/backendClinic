package com.unza.clinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private String name;
    private String email;
    private String role;
    private String department;
    private String staffId;
    private String manNumber;
    private String password;
    @Column(columnDefinition = "TEXT")
    private String permissionsJson;
    private Boolean forcePasswordChange;
    private String status;
    private String lastLogin;
    private String passwordChangedAt;
    private Integer passwordVersion;

    public AppUser() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }
    public String getManNumber() { return manNumber; }
    public void setManNumber(String manNumber) { this.manNumber = manNumber; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPermissionsJson() { return permissionsJson; }
    public void setPermissionsJson(String permissionsJson) { this.permissionsJson = permissionsJson; }
    public Boolean getForcePasswordChange() { return forcePasswordChange; }
    public void setForcePasswordChange(Boolean forcePasswordChange) { this.forcePasswordChange = forcePasswordChange; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }
    public String getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(String passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }
    public Integer getPasswordVersion() { return passwordVersion; }
    public void setPasswordVersion(Integer passwordVersion) { this.passwordVersion = passwordVersion; }
}
