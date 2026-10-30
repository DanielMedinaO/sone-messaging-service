package com.daniel.sonemessagingservice.shared.realtime;

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import com.daniel.sonemessagingservice.shared.realtime.security.StompAuthenticationInterceptor;
import com.daniel.sonemessagingservice.shared.security.jwt.SoneAccessTokenValidator;

class StompAuthenticationInterceptorTest {

    private static final KeyPair KEYS = generate();
    private static final KeyPair OTHER = generate();

    private final StompAuthenticationInterceptor interceptor;

    StompAuthenticationInterceptorTest() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) KEYS.getPublic())
                .signatureAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), new SoneAccessTokenValidator()));
        interceptor = new StompAuthenticationInterceptor(decoder);
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            return g.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String token(UUID sub, String appType, String status, String userStatus, KeyPair key) throws Exception {
        JWTClaimsSet.Builder b = new JWTClaimsSet.Builder().subject(sub.toString()).claim("type", "ACCESS")
                .claim("appType", appType).claim("status", status)
                .issueTime(Date.from(Instant.now())).expirationTime(Date.from(Instant.now().plusSeconds(600)));
        if (userStatus != null) {
            b.claim("userStatus", userStatus);
        }
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(), b.build());
        jwt.sign(new RSASSASigner((RSAPrivateKey) key.getPrivate()));
        return jwt.serialize();
    }

    private Message<?> frame(StompCommand command, String destination, String authorization, boolean authenticated) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        if (authenticated) {
            accessor.setUser(new org.springframework.security.authentication.TestingAuthenticationToken("u", "p"));
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Authentication connect(String authorization) {
        Message<?> out = interceptor.preSend(frame(StompCommand.CONNECT, null, authorization, false), null);
        return (Authentication) StompHeaderAccessor.wrap(out).getUser();
    }

    @Test
    void connectWithoutAuthorizationRejected() {
        assertThrows(MessagingException.class, () -> connect(null));
    }

    @Test
    void connectWithoutBearerRejected() throws Exception {
        String t = token(UUID.randomUUID(), "CUSTOMER", "ACTIVE", null, KEYS);
        assertThrows(MessagingException.class, () -> connect("Basic " + t));
        assertThrows(MessagingException.class, () -> connect(t));
    }

    @Test
    void connectWithInvalidJwtRejected() throws Exception {
        assertThrows(MessagingException.class, () -> connect("Bearer garbage"));
        String forged = token(UUID.randomUUID(), "CUSTOMER", "ACTIVE", null, OTHER);
        assertThrows(MessagingException.class, () -> connect("Bearer " + forged));
    }

    @Test
    void connectWithNonActiveStatusRejected() throws Exception {
        String t = token(UUID.randomUUID(), "CUSTOMER", "SUSPENDED", "ACTIVE", KEYS);
        assertThrows(MessagingException.class, () -> connect("Bearer " + t));
    }

    @Test
    void customerActiveAuthenticated() throws Exception {
        UUID id = UUID.randomUUID();
        Authentication a = connect("Bearer " + token(id, "CUSTOMER", "ACTIVE", null, KEYS));
        assertTrue(a.isAuthenticated());
        assertEquals(id.toString(), a.getName());
    }

    @Test
    void partnerActiveAuthenticated() throws Exception {
        UUID id = UUID.randomUUID();
        Authentication a = connect("Bearer " + token(id, "PARTNER", "ACTIVE", null, KEYS));
        assertEquals(id.toString(), a.getName());
    }

    @Test
    void userStatusIsIgnored() throws Exception {
        UUID id = UUID.randomUUID();
        assertEquals(id.toString(),
                connect("Bearer " + token(id, "CUSTOMER", "ACTIVE", "BLOCKED", KEYS)).getName());
        assertThrows(MessagingException.class,
                () -> connect("Bearer " + token(UUID.randomUUID(), "CUSTOMER", "INACTIVE", "ACTIVE", KEYS)));
    }

    @Test
    void subscribeToUserQueueAllowed() {
        Message<?> m = frame(StompCommand.SUBSCRIBE, "/user/queue/messages", null, true);
        assertSame(m, interceptor.preSend(m, null));
    }

    @Test
    void subscribeToOtherDestinationRejected() {
        for (String d : new String[] {"/topic/conversations/1", "/queue/messages", "/user/queue/other"}) {
            assertThrows(MessagingException.class,
                    () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, d, null, true), null));
        }
    }

    @Test
    void subscribeUnauthenticatedRejected() {
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/user/queue/messages", null, false), null));
    }

    @Test
    void clientSendRejected() {
        for (String d : new String[] {"/queue/messages", "/topic/x", "/app/x"}) {
            assertThrows(MessagingException.class,
                    () -> interceptor.preSend(frame(StompCommand.SEND, d, null, true), null));
        }
    }
}
