package com.daniel.sonemessagingservice.shared.realtime;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.context.ApplicationEventPublisher;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import com.daniel.sonemessagingservice.message.application.event.MessageSentEvent;
import com.daniel.sonemessagingservice.message.domain.model.Message;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketRealtimeIntegrationTest {

    private static final KeyPair KEYS = generate();

    @LocalServerPort int port;
    @Autowired ApplicationEventPublisher publisher;
    @Autowired TransactionTemplate tx;

    @DynamicPropertySource
    static void keys(DynamicPropertyRegistry registry) throws Exception {
        Path file = Files.createTempFile("ws-public", ".pem");
        file.toFile().deleteOnExit();
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(KEYS.getPublic().getEncoded());
        Files.writeString(file, "-----BEGIN PUBLIC KEY-----\n" + body + "\n-----END PUBLIC KEY-----\n");
        registry.add("sone.security.jwt.public-key-location", () -> file.toUri().toString());
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

    private static String token(UUID sub, String appType) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject(sub.toString()).claim("type", "ACCESS")
                .claim("appType", appType).claim("status", "ACTIVE")
                .issueTime(Date.from(Instant.now())).expirationTime(Date.from(Instant.now().plusSeconds(600))).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) KEYS.getPrivate()));
        return jwt.serialize();
    }

    private WebSocketStompClient client() {
        WebSocketStompClient c = new WebSocketStompClient(new StandardWebSocketClient());
        c.setMessageConverter(new StringMessageConverter() {
            @Override
            protected boolean supportsMimeType(org.springframework.messaging.MessageHeaders headers) {
                return true;
            }
        });
        return c;
    }

    private StompSession connect(String bearer) throws Exception {
        StompHeaders headers = new StompHeaders();
        if (bearer != null) {
            headers.add("Authorization", bearer);
        }
        return client().connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), headers,
                new StompSessionHandlerAdapter() { }).get(5, TimeUnit.SECONDS);
    }

    private BlockingQueue<String> subscribe(StompSession session) {
        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/messages", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders h) { return String.class; }
            @Override public void handleFrame(StompHeaders h, Object payload) { received.add((String) payload); }
        });
        return received;
    }

    @Test
    void bothParticipantsReceiveMessageAfterCommit() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        StompSession customer = connect("Bearer " + token(customerId, "CUSTOMER"));
        StompSession partner = connect("Bearer " + token(partnerId, "PARTNER"));
        BlockingQueue<String> customerQ = subscribe(customer);
        BlockingQueue<String> partnerQ = subscribe(partner);
        Thread.sleep(500);

        Message m = Message.createText(UUID.randomUUID(), customerId, "Hola");
        tx.executeWithoutResult(s -> publisher.publishEvent(new MessageSentEvent(m, customerId, partnerId)));

        String a = customerQ.poll(5, TimeUnit.SECONDS);
        String b = partnerQ.poll(5, TimeUnit.SECONDS);
        assertNotNull(a);
        assertNotNull(b);
        assertTrue(a.contains(m.getId().toString()));
        assertTrue(b.contains(m.getId().toString()));
        assertTrue(a.contains("\"content\":\"Hola\""));
        customer.disconnect();
        partner.disconnect();
    }

    @Test
    void connectWithoutJwtIsRejectedAfterHandshake() {
        assertThrows(Exception.class, () -> connect(null));
    }

    @Test
    void rolledBackTransactionDeliversNothing() throws Exception {
        UUID customerId = UUID.randomUUID();
        StompSession customer = connect("Bearer " + token(customerId, "CUSTOMER"));
        BlockingQueue<String> q = subscribe(customer);
        Thread.sleep(500);
        Message m = Message.createText(UUID.randomUUID(), customerId, "Hola");
        tx.executeWithoutResult(s -> {
            publisher.publishEvent(new MessageSentEvent(m, customerId, UUID.randomUUID()));
            s.setRollbackOnly();
        });
        assertNull(q.poll(1, TimeUnit.SECONDS));
        customer.disconnect();
    }
}
