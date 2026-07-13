package com.unza.clinic.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_settings")
public class SystemSettings {
    @Id
    private Long id;
    private String hospitalName;
    private String contactPhone;
    private String address;
    private boolean emailNotifications;
    private boolean smsNotifications;
    private boolean lowStockAlerts;
    private boolean twoFactorAuth;
    private boolean auditLogging;
    private boolean autoLogout;
    private Boolean backupEnabled;
    private String backupFrequency;
    private String backupLocation;
    private String lastBackupAt;
    private Boolean demoDataCleared;

    public SystemSettings() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    public boolean isSmsNotifications() { return smsNotifications; }
    public void setSmsNotifications(boolean smsNotifications) { this.smsNotifications = smsNotifications; }
    public boolean isLowStockAlerts() { return lowStockAlerts; }
    public void setLowStockAlerts(boolean lowStockAlerts) { this.lowStockAlerts = lowStockAlerts; }
    public boolean isTwoFactorAuth() { return twoFactorAuth; }
    public void setTwoFactorAuth(boolean twoFactorAuth) { this.twoFactorAuth = twoFactorAuth; }
    public boolean isAuditLogging() { return auditLogging; }
    public void setAuditLogging(boolean auditLogging) { this.auditLogging = auditLogging; }
    public boolean isAutoLogout() { return autoLogout; }
    public void setAutoLogout(boolean autoLogout) { this.autoLogout = autoLogout; }
    public Boolean getBackupEnabled() { return backupEnabled; }
    public void setBackupEnabled(Boolean backupEnabled) { this.backupEnabled = backupEnabled; }
    public String getBackupFrequency() { return backupFrequency; }
    public void setBackupFrequency(String backupFrequency) { this.backupFrequency = backupFrequency; }
    public String getBackupLocation() { return backupLocation; }
    public void setBackupLocation(String backupLocation) { this.backupLocation = backupLocation; }
    public String getLastBackupAt() { return lastBackupAt; }
    public void setLastBackupAt(String lastBackupAt) { this.lastBackupAt = lastBackupAt; }
    public Boolean getDemoDataCleared() { return demoDataCleared; }
    public void setDemoDataCleared(Boolean demoDataCleared) { this.demoDataCleared = demoDataCleared; }
}
