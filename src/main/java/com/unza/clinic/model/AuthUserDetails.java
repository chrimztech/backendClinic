package com.unza.clinic.model;

import java.util.List;

public class AuthUserDetails {
    private Long id;
    private String userId;
    private String name;
    private String email;
    private String role;
    private String department;
    private String staffId;
    private String manNumber;
    private String status;
    private Boolean forcePasswordChange;
    private List<String> permissions;

    public AuthUserDetails(Long id, String userId, String name, String email, String role, String department,
                          String staffId, String manNumber, String status, Boolean forcePasswordChange,
                          List<String> permissions) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.department = department;
        this.staffId = staffId;
        this.manNumber = manNumber;
        this.status = status;
        this.forcePasswordChange = forcePasswordChange;
        this.permissions = permissions;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getDepartment() { return department; }
    public String getStaffId() { return staffId; }
    public String getManNumber() { return manNumber; }
    public String getStatus() { return status; }
    public Boolean getForcePasswordChange() { return forcePasswordChange; }
    public List<String> getPermissions() { return permissions; }
}
