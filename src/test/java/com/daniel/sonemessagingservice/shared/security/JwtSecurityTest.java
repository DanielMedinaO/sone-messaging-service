package com.daniel.sonemessagingservice.shared.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@SpringBootTest
@AutoConfigureMockMvc
class JwtSecurityTest {

    private static final String URL = "/api/v1/test/me";
    private static final KeyPair TRUSTED = generate();
    private static final KeyPair OTHER = generate();

    @Autowired
    MockMvc mvc;

    @DynamicPropertySource
    static void keys(DynamicPropertyRegistry registry) throws Exception {
        Path file = Files.createTempFile("test-public", ".pem");
        file.toFile().deleteOnExit();
        String body = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(TRUSTED.getPublic().getEncoded());
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

    private static Map<String, Object> valid(String appType, String status) {
        Map<String, Object> c = new HashMap<>();
        c.put("sub", UUID.randomUUID().toString());
        c.put("type", "ACCESS");
        c.put("appType", appType);
        c.put("status", status);
        return c;
    }

    private static String token(Map<String, Object> claims, KeyPair key, Instant exp) throws Exception {
        JWTClaimsSet.Builder b = new JWTClaimsSet.Builder();
        claims.forEach(b::claim);
        b.issueTime(java.util.Date.from(exp.minusSeconds(900)));
        b.expirationTime(java.util.Date.from(exp));
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(), b.build());
        jwt.sign(new RSASSASigner((RSAPrivateKey) key.getPrivate()));
        return jwt.serialize();
    }

    private ResultActions call(Map<String, Object> claims) throws Exception {
        return mvc.perform(get(URL).header("Authorization",
                "Bearer " + token(claims, TRUSTED, Instant.now().plusSeconds(600))));
    }

    private ResultActions callWith(Map<String, Object> claims, KeyPair key, Instant exp) throws Exception {
        return mvc.perform(get(URL).header("Authorization", "Bearer " + token(claims, key, exp)));
    }

    @Test
    void withoutToken_401() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidSignature_401() throws Exception {
        callWith(valid("CUSTOMER", "ACTIVE"), OTHER, Instant.now().plusSeconds(600))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expired_401() throws Exception {
        callWith(valid("CUSTOMER", "ACTIVE"), TRUSTED, Instant.now().minusSeconds(600))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAccessTypes_401() throws Exception {
        for (String type : new String[] {"REFRESH", "CUSTOMER_PRE_AUTH", "PARTNER_PRE_AUTH"}) {
            Map<String, Object> c = valid("CUSTOMER", "ACTIVE");
            c.put("type", type);
            call(c).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void missingType_401() throws Exception {
        Map<String, Object> c = valid("CUSTOMER", "ACTIVE");
        c.remove("type");
        call(c).andExpect(status().isUnauthorized());
    }

    @Test
    void missingSub_401() throws Exception {
        Map<String, Object> c = valid("CUSTOMER", "ACTIVE");
        c.remove("sub");
        call(c).andExpect(status().isUnauthorized());
    }

    @Test
    void subNotUuid_401() throws Exception {
        Map<String, Object> c = valid("CUSTOMER", "ACTIVE");
        c.put("sub", "not-a-uuid");
        call(c).andExpect(status().isUnauthorized());
    }

    @Test
    void missingAppType_401() throws Exception {
        Map<String, Object> c = valid("CUSTOMER", "ACTIVE");
        c.remove("appType");
        call(c).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownAppType_401() throws Exception {
        call(valid("ADMIN", "ACTIVE")).andExpect(status().isUnauthorized());
    }

    @Test
    void missingStatus_401() throws Exception {
        Map<String, Object> c = valid("CUSTOMER", "ACTIVE");
        c.remove("status");
        call(c).andExpect(status().isUnauthorized());
    }

    @Test
    void customerActive_allowed_andPrincipalExtracted() throws Exception {
        Map<String, Object> c = valid("CUSTOMER", "ACTIVE");
        call(c).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c.get("sub")))
                .andExpect(jsonPath("$.appType").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void partnerActive_allowed() throws Exception {
        call(valid("PARTNER", "ACTIVE")).andExpect(status().isOk())
                .andExpect(jsonPath("$.appType").value("PARTNER"));
    }

    @Test
    void customerNotActive_403() throws Exception {
        call(valid("CUSTOMER", "SUSPENDED")).andExpect(status().isForbidden());
    }

    @Test
    void partnerNotActive_403() throws Exception {
        call(valid("PARTNER", "PENDING")).andExpect(status().isForbidden());
    }

    @Test
    void userStatusBlocked_isIgnored() throws Exception {
        Map<String, Object> c = valid("CUSTOMER", "ACTIVE");
        c.put("userStatus", "BLOCKED");
        call(c).andExpect(status().isOk());
    }

    @Test
    void withoutUserStatus_allowed() throws Exception {
        call(valid("CUSTOMER", "ACTIVE")).andExpect(status().isOk());
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(result ->
                org.junit.jupiter.api.Assertions.assertNotEquals(401, result.getResponse().getStatus()));
    }
}
