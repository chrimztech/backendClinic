package com.unza.clinic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unza.clinic.model.AppUser;
import com.unza.clinic.model.AuthUserDetails;
import com.unza.clinic.security.RolePermissions;
import com.unza.clinic.service.ClinicDataStore;
import com.unza.clinic.service.EncounterWorkflow;
import com.unza.clinic.util.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Authenticates STOMP connections and authorizes individual topic subscriptions. */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final ClinicDataStore dataStore;
    private final ObjectMapper objectMapper;

    public WebSocketAuthChannelInterceptor(JwtUtil jwtUtil, ClinicDataStore dataStore, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.dataStore = dataStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            throw new MessagingException("Invalid STOMP message headers");
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = firstHeader(accessor, HttpHeaders.AUTHORIZATION, "authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new MessagingException("A valid Bearer token is required for real-time updates");
        }

        String token = authorization.substring(7).trim();
        if (!jwtUtil.validateToken(token)) {
            throw new MessagingException("Real-time connection token is invalid or expired");
        }

        AppUser user = dataStore.findUserByEmail(jwtUtil.extractEmail(token));
        if (user == null || !"active".equalsIgnoreCase(user.getStatus())) {
            throw new MessagingException("The real-time user account is unavailable or inactive");
        }

        List<String> permissions = readPermissions(user);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));

        AuthUserDetails principal = new AuthUserDetails(
                user.getId(), user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getDepartment(),
                user.getStaffId(), user.getManNumber(), user.getStatus(),
                Boolean.TRUE.equals(user.getForcePasswordChange()), permissions
        );
        accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication)
                || !(authentication.getPrincipal() instanceof AuthUserDetails user)) {
            throw new MessagingException("Authenticate before subscribing to real-time updates");
        }

        String destination = accessor.getDestination();
        String requiredPermission = requiredPermission(destination);
        if (requiredPermission != null && user.getPermissions().stream().noneMatch(requiredPermission::equalsIgnoreCase)) {
            throw new MessagingException("You do not have permission to subscribe to " + destination);
        }
    }

    private String requiredPermission(String destination) {
        if (destination == null) return "__deny__";
        if (destination.startsWith("/topic/queue/")) {
            try {
                return EncounterWorkflow.permissionForStage(destination.substring("/topic/queue/".length()));
            } catch (IllegalArgumentException ignored) {
                return "__deny__";
            }
        }
        return switch (destination) {
            case "/topic/queue" -> null;
            case "/topic/notifications" -> "notifications.view";
            case "/topic/ward-status" -> "wards.view";
            case "/topic/vital-alerts" -> "triage.view";
            case "/topic/lab-results" -> "laboratory.view";
            case "/topic/pharmacy-queue" -> "pharmacy.view";
            default -> "__deny__";
        };
    }

    private List<String> readPermissions(AppUser user) {
        if (user.getPermissionsJson() != null && !user.getPermissionsJson().isBlank()) {
            try {
                return objectMapper.readValue(user.getPermissionsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (Exception ignored) {
                // Fall through to the role baseline if custom data is malformed.
            }
        }
        return RolePermissions.forRole(user.getRole());
    }

    private String firstHeader(StompHeaderAccessor accessor, String... names) {
        for (String name : names) {
            String value = accessor.getFirstNativeHeader(name);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}
