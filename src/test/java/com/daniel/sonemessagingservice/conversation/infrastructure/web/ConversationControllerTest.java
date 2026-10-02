package com.daniel.sonemessagingservice.conversation.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.daniel.sonemessagingservice.conversation.application.exception.PartnerEligibilityUnavailableException;
import com.daniel.sonemessagingservice.conversation.application.exception.PartnerNotActiveException;
import com.daniel.sonemessagingservice.conversation.application.exception.PartnerNotFoundException;
import com.daniel.sonemessagingservice.conversation.application.usecase.StartConversation;
import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@SpringBootTest
@AutoConfigureMockMvc
class ConversationControllerTest {

    private static final String URL = "/api/v1/conversations";
    private static final KeyPair KEY = generate();

    @Autowired
    MockMvc mvc;

    @MockitoBean
    StartConversation startConversation;

    final UUID userId = UUID.randomUUID();
    final UUID partnerId = UUID.randomUUID();

    @DynamicPropertySource
    static void keys(DynamicPropertyRegistry registry) throws Exception {
        Path file = Files.createTempFile("conv-test-public", ".pem");
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

    private String token(String appType, String status) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .claim("type", "ACCESS")
                .claim("appType", appType)
                .claim("status", status)
                .claim("userStatus", "BLOCKED")
                .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) KEY.getPrivate()));
        return jwt.serialize();
    }

    private ResultActions send(String token, String json) throws Exception {
        var req = post(URL).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(req);
    }

    private String body() {
        return "{\"partnerId\":\"" + partnerId + "\"}";
    }

    @Test
    void withoutJwt_401() throws Exception {
        send(null, body()).andExpect(status().isUnauthorized());
        verify(startConversation, never()).execute(any(), any());
    }

    @Test
    void customerActive_ok_usesJwtSubAndBodyPartner() throws Exception {
        Conversation c = Conversation.create(userId, partnerId);
        when(startConversation.execute(userId, partnerId)).thenReturn(c);

        send(token("CUSTOMER", "ACTIVE"), body())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c.getId().toString()))
                .andExpect(jsonPath("$.customerId").value(userId.toString()))
                .andExpect(jsonPath("$.partnerId").value(partnerId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        verify(startConversation).execute(userId, partnerId);
    }

    @Test
    void bodyCustomerId_isIgnored() throws Exception {
        UUID spoofed = UUID.randomUUID();
        when(startConversation.execute(userId, partnerId)).thenReturn(Conversation.create(userId, partnerId));

        send(token("CUSTOMER", "ACTIVE"),
                "{\"partnerId\":\"" + partnerId + "\",\"customerId\":\"" + spoofed + "\"}")
                .andExpect(status().isOk());
        verify(startConversation).execute(userId, partnerId);
        verify(startConversation, never()).execute(spoofed, partnerId);
    }

    @Test
    void partnerActive_403() throws Exception {
        send(token("PARTNER", "ACTIVE"), body()).andExpect(status().isForbidden());
        verify(startConversation, never()).execute(any(), any());
    }

    @Test
    void customerNotActive_403() throws Exception {
        send(token("CUSTOMER", "SUSPENDED"), body()).andExpect(status().isForbidden());
        verify(startConversation, never()).execute(any(), any());
    }

    @Test
    void missingPartnerId_400() throws Exception {
        send(token("CUSTOMER", "ACTIVE"), "{}").andExpect(status().isBadRequest());
    }

    @Test
    void partnerNotActive_403() throws Exception {
        when(startConversation.execute(userId, partnerId)).thenThrow(new PartnerNotActiveException());
        send(token("CUSTOMER", "ACTIVE"), body()).andExpect(status().isForbidden());
    }

    @Test
    void partnerNotFound_404() throws Exception {
        when(startConversation.execute(userId, partnerId)).thenThrow(new PartnerNotFoundException());
        send(token("CUSTOMER", "ACTIVE"), body()).andExpect(status().isNotFound());
    }

    @Test
    void marketplaceUnavailable_503() throws Exception {
        when(startConversation.execute(userId, partnerId))
                .thenThrow(new PartnerEligibilityUnavailableException("down", null));
        send(token("CUSTOMER", "ACTIVE"), body()).andExpect(status().isServiceUnavailable());
    }
}
