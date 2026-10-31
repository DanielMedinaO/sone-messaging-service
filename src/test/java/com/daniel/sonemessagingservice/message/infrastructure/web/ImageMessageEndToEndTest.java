package com.daniel.sonemessagingservice.message.infrastructure.web;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;
import com.daniel.sonemessagingservice.conversation.domain.repository.ConversationRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/** Real HTTP stack: real multipart limits, real PostgreSQL, real mock storage. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ImageMessageEndToEndTest {

    private static final KeyPair KEY = generate();

    @LocalServerPort int port;
    @Autowired ConversationRepository conversationRepository;

    @DynamicPropertySource
    static void keys(DynamicPropertyRegistry registry) throws Exception {
        Path file = Files.createTempFile("img-e2e-public", ".pem");
        file.toFile().deleteOnExit();
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(KEY.getPublic().getEncoded());
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
                .expirationTime(Date.from(Instant.now().plusSeconds(600))).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) KEY.getPrivate()));
        return "Bearer " + jwt.serialize();
    }

    private final RestClient http = RestClient.builder()
            .requestFactory(new org.springframework.http.client.BufferingClientHttpRequestFactory(
                    new org.springframework.http.client.SimpleClientHttpRequestFactory()))
            .build();

    private ResponseEntity<String> post(String conversationId, String bearer, int[] sizes, String mime) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        for (int i = 0; i < sizes.length; i++) {
            final String name = "p" + i + ".jpg";
            parts.add("files", new HttpEntity<>(new ByteArrayResource(new byte[sizes[i]]) {
                @Override public String getFilename() { return name; }
            }, headers(mime)));
        }
        return http.post().uri("http://localhost:" + port + "/api/v1/conversations/" + conversationId + "/messages/images")
                .header("Authorization", bearer).contentType(MediaType.MULTIPART_FORM_DATA).body(parts)
                .retrieve().onStatus(s -> true, (rq, rs) -> { }).toEntity(String.class);
    }

    private HttpHeaders headers(String mime) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.parseMediaType(mime));
        return h;
    }

    @Test
    void sendsImagesAndHistoryReturnsThem() throws Exception {
        UUID customerId = UUID.randomUUID();
        Conversation c = conversationRepository.save(Conversation.create(customerId, UUID.randomUUID()));
        String bearer = token(customerId, "CUSTOMER");

        ResponseEntity<String> created = post(c.getId().toString(), bearer, new int[] {100, 200}, "image/jpeg");
        assertEquals(201, created.getStatusCode().value(), created.getBody());
        assertTrue(created.getBody().contains("\"type\":\"IMAGE\""));
        assertTrue(created.getBody().contains("mock://sone-messaging/"));

        ResponseEntity<String> history = http.get()
                .uri("http://localhost:" + port + "/api/v1/conversations/" + c.getId() + "/messages")
                .header("Authorization", bearer).retrieve().toEntity(String.class);
        assertTrue(history.getBody().contains("\"type\":\"IMAGE\""));
        assertTrue(history.getBody().contains("\"originalFileName\":\"p1.jpg\""));
    }

    @Test
    void realMultipartLimitsAndValidation() throws Exception {
        UUID customerId = UUID.randomUUID();
        Conversation c = conversationRepository.save(Conversation.create(customerId, UUID.randomUUID()));
        String bearer = token(customerId, "CUSTOMER");
        String id = c.getId().toString();

        assertEquals(413, post(id, bearer, new int[] {5 * 1024 * 1024 + 1}, "image/jpeg").getStatusCode().value());
        assertEquals(415, post(id, bearer, new int[] {10}, "image/gif").getStatusCode().value());
        assertEquals(400, post(id, bearer, new int[] {10, 10, 10, 10}, "image/png").getStatusCode().value());
        assertEquals(403, post(id, token(UUID.randomUUID(), "CUSTOMER"), new int[] {10}, "image/png")
                .getStatusCode().value());
        assertEquals(404, post(UUID.randomUUID().toString(), bearer, new int[] {10}, "image/png")
                .getStatusCode().value());
    }

    @Test
    void swaggerDocumentsImageEndpoint() {
        String docs = http.get().uri("http://localhost:" + port + "/v3/api-docs").retrieve().body(String.class);
        assertTrue(docs.contains("/messages/images"));
        assertTrue(docs.contains("multipart/form-data"));
        assertTrue(docs.contains("MessageAttachmentResponse"));
    }
}
