package com.unza.clinic.dto;

public record SettingsUpdateRequest(
        String hospitalName,
        String contactPhone,
        String address,
        Boolean emailNotifications,
        Boolean smsNotifications,
        Boolean lowStockAlerts,
        Boolean twoFactorAuth,
        Boolean auditLogging,
        Boolean autoLogout,
        Boolean backupEnabled,
        String backupFrequency,
        String backupLocation,
        String lastBackupAt
) {
}
