package com.unza.clinic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unza.clinic.model.AppUser;
import com.unza.clinic.model.AuthUserDetails;
import com.unza.clinic.service.ClinicDataStore;
import com.unza.clinic.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketAuthChannelInterceptorTests {

    private JwtUtil jwtUtil;
    private ClinicDataStore dataStore;
    private WebSocketAuthChannelInterceptor interceptor;
    private MessageChannel channel;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        dataStore = mock(ClinicDataStore.class);
        channel = mock(MessageChannel.class);
        interceptor = new WebSocketAuthChannelInterceptor(jwtUtil, dataStore, new ObjectMapper());
    }

    @Test
    void connectRequiresBearerToken() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, null, null, null);
        assertThrows(MessagingException.class, () -> interceptor.preSend(message, channel));
    }

    @Test
    void validConnectCreatesAuthenticatedPrincipal() {
        AppUser user = activeUser("[\"walkin.view\"]");
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(jwtUtil.extractEmail("token")).thenReturn(user.getEmail());
        when(dataStore.findUserByEmail(user.getEmail())).thenReturn(user);

        Message<byte[]> message = stompMessage(StompCommand.CONNECT, null, "Bearer token", null);
        interceptor.preSend(message, channel);

        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        UsernamePasswordAuthenticationToken authentication =
                assertInstanceOf(UsernamePasswordAuthenticationToken.class, accessor.getUser());
        assertInstanceOf(AuthUserDetails.class, authentication.getPrincipal());
        assertTrue(authentication.isAuthenticated());
    }

    @Test
    void subscriptionIsScopedToDepartmentPermission() {
        AuthUserDetails principal = new AuthUserDetails(1L, "USR-1", "Reception", "r@example.test",
                "Receptionist", "Reception", null, null, "active", false, List.of("walkin.view"));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());

        Message<byte[]> reception = stompMessage(StompCommand.SUBSCRIBE, "/topic/queue/RECEPTION", null, authentication);
        interceptor.preSend(reception, channel);

        Message<byte[]> laboratory = stompMessage(StompCommand.SUBSCRIBE, "/topic/queue/LABORATORY", null, authentication);
        assertThrows(MessagingException.class, () -> interceptor.preSend(laboratory, channel));
    }

    private Message<byte[]> stompMessage(StompCommand command, String destination, String authorization,
                                          UsernamePasswordAuthenticationToken authentication) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        if (destination != null) accessor.setDestination(destination);
        if (authorization != null) accessor.setNativeHeader("Authorization", authorization);
        if (authentication != null) accessor.setUser(authentication);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private AppUser activeUser(String permissions) {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUserId("USR-1");
        user.setName("Reception User");
        user.setEmail("reception@example.test");
        user.setRole("Receptionist");
        user.setDepartment("Reception");
        user.setStatus("active");
        user.setPermissionsJson(permissions);
        return user;
    }
}
