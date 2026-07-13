package com.unza.clinic.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unza.clinic.model.AppUser;
import com.unza.clinic.model.AuthUserDetails;
import com.unza.clinic.service.ClinicDataStore;
import com.unza.clinic.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ClinicDataStore dataStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ClinicDataStore dataStore) {
        this.jwtUtil = jwtUtil;
        this.dataStore = dataStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip JWT validation for public endpoints
        if (isPublicEndpoint(path)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Check for legacy headers for backward compatibility (optional)
            String legacyUserId = request.getHeader("X-Clinic-User-Id");
            String legacyRole = request.getHeader("X-Clinic-User-Role");
            if (legacyUserId != null && legacyRole != null) {
                // Create a simplified authentication for backward compatibility
                chain.doFilter(request, response);
                return;
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Authorization header missing or invalid\"}");
                return;
            }
        }

        String token = authHeader.substring(7);

        try {
            if (jwtUtil.validateToken(token)) {
                String userId = jwtUtil.extractUserId(token);
                String role = jwtUtil.extractRole(token);
                String department = jwtUtil.extractDepartment(token);
                String name = jwtUtil.extractName(token);
                String email = jwtUtil.extractEmail(token);

                // Try to get full user details from database for permissions
                AppUser user = dataStore.findUserByEmail(email);
                List<String> permissions = new ArrayList<>();
                if (user != null && user.getPermissionsJson() != null) {
                    try {
                        permissions = new ArrayList<>(objectMapper.readValue(user.getPermissionsJson(),
                                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
                    } catch (Exception e) {
                        permissions = new ArrayList<>(getDefaultPermissionsForRole(role));
                    }
                } else {
                    permissions = new ArrayList<>(getDefaultPermissionsForRole(role));
                }
                // Merge with role defaults so new default permissions apply to existing users
                for (String def : getDefaultPermissionsForRole(role)) {
                    if (!permissions.contains(def)) {
                        permissions.add(def);
                    }
                }

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                for (String perm : permissions) {
                    authorities.add(new SimpleGrantedAuthority(perm));
                }

                String[] requiredPermissions = resolveRequiredPermissions(request);
                if (requiredPermissions.length > 0 && permissions.stream().noneMatch(permission -> Arrays.asList(requiredPermissions).contains(permission))) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\":\"You do not have permission to access this resource\"}");
                    return;
                }

                AuthUserDetails userDetails = new AuthUserDetails(
                        user != null ? user.getId() : null,
                        userId, name, email, role, department,
                        user != null ? user.getStaffId() : null,
                        user != null ? user.getManNumber() : null,
                        user != null ? user.getStatus() : "active",
                        user != null && user.getForcePasswordChange() != null ? user.getForcePasswordChange() : false,
                        permissions
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
                return;
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Token validation failed\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/login") ||
               path.startsWith("/api/health") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/ws") ||
               path.equals("/");
    }

    private String[] resolveRequiredPermissions(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (path.startsWith("/api/patients")) {
            if ("POST".equals(method)) return new String[] { "walkin.view", "patients.view" };
            if ("PUT".equals(method)) return new String[] { "walkin.view", "patients.manage" };
            return new String[] { "patients.view" };
        }
        if (path.startsWith("/api/staff")) {
            return "GET".equals(method) ? new String[] { "staff.view" } : new String[] { "staff.manage" };
        }
        if (path.startsWith("/api/departments")) {
            return "GET".equals(method) ? new String[] { "sections.view", "departments.manage" } : new String[] { "departments.manage" };
        }
        if (path.startsWith("/api/external/sis")) {
            return new String[] { "walkin.view", "patients.view" };
        }
        if (path.startsWith("/api/external/hr")) {
            return new String[] { "walkin.view", "staff.view" };
        }
        if (path.startsWith("/api/external/counseling")) {
            return new String[] { "referrals.view", "counseling.view" };
        }
        if (path.startsWith("/api/appointments")) {
            return new String[] { "schedules.view" };
        }
        if (path.startsWith("/api/prescriptions")) {
            return new String[] { "prescriptions.view" };
        }
        if (path.startsWith("/api/admissions")) {
            return new String[] { "admissions.view" };
        }
        if (path.startsWith("/api/lab-tests")) {
            return new String[] { "laboratory.view" };
        }
        if (path.startsWith("/api/billing/summary") || path.startsWith("/api/billing")) {
            if ("POST".equals(method)) return new String[] { "billing.create" };
            if ("PUT".equals(method)) return new String[] { "billing.payments" };
            return new String[] { "billing.view" };
        }
        if (path.startsWith("/api/tariffs")) {
            return "GET".equals(method) ? new String[] { "billing.view", "tariffs.manage" } : new String[] { "tariffs.manage" };
        }
        if (path.startsWith("/api/inventory")) {
            return new String[] { "inventory.view" };
        }
        if (path.startsWith("/api/suppliers")) {
            return new String[] { "suppliers.view" };
        }
        if (path.startsWith("/api/drugs")) {
            return new String[] { "pharmacy.view" };
        }
        if (path.startsWith("/api/imaging")) {
            return new String[] { "radiology.view" };
        }
        if (path.startsWith("/api/insurance-claims")) {
            return new String[] { "insurance.view" };
        }
        if (path.startsWith("/api/referrals")) {
            return new String[] { "referrals.view" };
        }
        if (path.startsWith("/api/triage")) {
            return new String[] { "triage.view" };
        }
        if (path.startsWith("/api/queue")) {
            return new String[] { "walkin.view" };
        }
        if (path.startsWith("/api/emergency")) {
            return new String[] { "emergency.view" };
        }
        if (path.startsWith("/api/blood-bank")) {
            return new String[] { "bloodbank.view" };
        }
        if (path.startsWith("/api/notifications")) {
            return new String[] { "notifications.view" };
        }
        if (path.startsWith("/api/attendance")) {
            return new String[] { "attendance.view" };
        }
        if (path.startsWith("/api/staff-schedules")) {
            return new String[] { "schedules.view" };
        }
        if (path.startsWith("/api/wards")) {
            return "GET".equals(method) ? new String[] { "wards.view" } : new String[] { "departments.manage" };
        }
        if (path.startsWith("/api/dashboard")) {
            return new String[] { "dashboard.view" };
        }
        if (path.startsWith("/api/reports")) {
            return new String[] { "reports.view" };
        }
        if (path.startsWith("/api/encounters")) {
            return new String[] { "walkin.view", "triage.view", "records.view", "forms.view", "prescriptions.view", "laboratory.view", "radiology.view", "pharmacy.view", "admissions.view", "billing.view" };
        }
        if (path.startsWith("/api/clinical-forms")) {
            return new String[] { "forms.view" };
        }
        return new String[0];
    }

    private List<String> getDefaultPermissionsForRole(String role) {
        return switch (role != null ? role.toLowerCase() : "") {
            case "admin" -> java.util.List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view",
                    "walkin.view", "triage.view", "emergency.view", "staff.view", "schedules.view",
                    "records.view", "forms.view", "prescriptions.view", "referrals.view",
                    "counseling.view", "laboratory.view", "radiology.view", "bloodbank.view", "pharmacy.view",
                    "suppliers.view", "inventory.view", "wards.view", "admissions.view",
                    "billing.view", "billing.create", "billing.payments", "insurance.view",
                    "departments.manage", "attendance.view", "users.manage", "users.reset_password",
                    "patients.manage", "staff.manage", "audit.view", "audit.export",
                    "reports.view", "settings.view", "settings.manage", "backup.export", "tariffs.manage"
            );
            case "doctor" -> java.util.List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view",
                    "walkin.view", "triage.view", "emergency.view", "staff.view", "schedules.view",
                    "records.view", "forms.view", "prescriptions.view", "referrals.view", "counseling.view",
                    "laboratory.view", "radiology.view", "bloodbank.view", "pharmacy.view", "wards.view",
                    "admissions.view", "reports.view"
            );
            case "nurse" -> java.util.List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view",
                    "walkin.view", "triage.view", "emergency.view", "staff.view",
                    "records.view", "forms.view", "counseling.view", "pharmacy.view", "wards.view", "admissions.view"
            );
            case "receptionist" -> java.util.List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view",
                    "walkin.view", "schedules.view", "forms.view", "billing.view",
                    "billing.create", "billing.payments", "insurance.view"
            );
            case "pharmacist" -> java.util.List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view",
                    "forms.view", "pharmacy.view", "suppliers.view", "inventory.view",
                    "billing.view", "billing.create", "billing.payments"
            );
            case "lab technician" -> java.util.List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view",
                    "forms.view", "laboratory.view", "radiology.view", "bloodbank.view", "reports.view"
            );
            default -> java.util.List.of("dashboard.view");
        };
    }
}
