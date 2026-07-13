package com.unza.clinic.dto;

public record LoginResponse(
        boolean success,
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String expiresAt,
        String refreshToken,
        UserSummary user
) {
    public record UserSummary(
            Long id,
            String userId,
            String name,
            String email,
            String role,
            String department,
            String staffId,
            String manNumber,
            String status,
            boolean forcePasswordChange,
            java.util.List<String> permissions
    ) {
    }
}
