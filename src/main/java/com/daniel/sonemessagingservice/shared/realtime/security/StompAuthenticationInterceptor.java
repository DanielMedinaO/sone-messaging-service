package com.daniel.sonemessagingservice.shared.realtime.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import com.daniel.sonemessagingservice.shared.security.jwt.SoneJwtAuthenticationConverter;

/**
 * Authenticates the STOMP CONNECT frame reusing the existing JwtDecoder (signature, expiry, ACCESS type,
 * claims) and SoneJwtAuthenticationConverter, and restricts the frames clients may send.
 */
@Component
public class StompAuthenticationInterceptor implements ChannelInterceptor {

    public static final String USER_MESSAGES_DESTINATION = "/user/queue/messages";
    private static final String BEARER = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final SoneJwtAuthenticationConverter converter = new SoneJwtAuthenticationConverter();

    public StompAuthenticationInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message; // heartbeat
        }
        StompCommand command = accessor.getCommand();
        switch (command) {
            case CONNECT, STOMP -> accessor.setUser(authenticate(accessor));
            case SUBSCRIBE -> {
                requireAuthenticated(accessor);
                if (!USER_MESSAGES_DESTINATION.equals(accessor.getDestination())) {
                    throw new MessagingException("Subscription destination not allowed");
                }
            }
            case UNSUBSCRIBE, DISCONNECT -> { }
            default -> throw new MessagingException("STOMP frame not allowed: " + command);
        }
        return message;
    }

    private Authentication authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith(BEARER) || header.length() == BEARER.length()) {
            throw new MessagingException("Missing or malformed Authorization header");
        }
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(header.substring(BEARER.length()).trim());
        } catch (JwtException | IllegalArgumentException e) {
            throw new MessagingException("Invalid access token");
        }
        Authentication authentication = converter.convert(jwt);
        boolean active = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> SoneJwtAuthenticationConverter.STATUS_ACTIVE.equals(a.getAuthority()));
        if (!active) {
            throw new MessagingException("User is not active");
        }
        return authentication;
    }

    private void requireAuthenticated(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new MessagingException("Not authenticated");
        }
    }
}
