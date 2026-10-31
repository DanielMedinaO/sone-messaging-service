package com.daniel.sonemessagingservice.message.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.daniel.sonemessagingservice.message.application.exception.ConversationNotFoundException;
import com.daniel.sonemessagingservice.message.application.exception.DailyMessageLimitExceededException;
import com.daniel.sonemessagingservice.message.application.exception.NotConversationParticipantException;
import com.daniel.sonemessagingservice.message.application.exception.FileStorageUnavailableException;
import com.daniel.sonemessagingservice.message.application.exception.ImageTooLargeException;
import com.daniel.sonemessagingservice.message.application.exception.InvalidImageUploadException;
import com.daniel.sonemessagingservice.message.application.exception.UnsupportedImageTypeException;
import com.daniel.sonemessagingservice.message.application.usecase.GetConversationMessages;
import com.daniel.sonemessagingservice.message.application.usecase.SendImageMessage;
import com.daniel.sonemessagingservice.message.domain.model.MessageAttachment;
import com.daniel.sonemessagingservice.message.application.usecase.SendTextMessage;
import com.daniel.sonemessagingservice.message.domain.model.Message;
import com.daniel.sonemessagingservice.message.domain.repository.MessagePage;
import com.daniel.sonemessagingservice.shared.security.model.AppType;
import com.daniel.sonemessagingservice.shared.security.model.AuthenticatedUser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@SpringBootTest
@AutoConfigureMockMvc
class MessageControllerTest {

    private static final KeyPair KEY = generate();

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SendTextMessage sendTextMessage;

    @MockitoBean
    SendImageMessage sendImageMessage;

    @MockitoBean
    GetConversationMessages getConversationMessages;

    final UUID userId = UUID.randomUUID();
    final UUID conversationId = UUID.randomUUID();
    final String url = "/api/v1/conversations/" + conversationId + "/messages";

    @DynamicPropertySource
    static void keys(DynamicPropertyRegistry registry) throws Exception {
        Path file = Files.createTempFile("msg-test-public", ".pem");
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
                .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) KEY.getPrivate()));
        return "Bearer " + jwt.serialize();
    }

    private static final String BODY = "{\"text\":\"Hola\"}";

    @Test
    void postWithoutJwt_401() throws Exception {
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postCustomer_201_usesJwtSender() throws Exception {
        Message m = Message.createText(conversationId, userId, "Hola");
        var user = new AuthenticatedUser(userId, AppType.CUSTOMER, "ACTIVE");
        when(sendTextMessage.execute(conversationId, user, "Hola")).thenReturn(m);

        mvc.perform(post(url).header("Authorization", token("CUSTOMER", "ACTIVE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Hola\",\"senderId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(m.getId().toString()))
                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.senderId").value(userId.toString()))
                .andExpect(jsonPath("$.type").value("TEXT"))
                .andExpect(jsonPath("$.content").value("Hola"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void postPartner_201() throws Exception {
        Message m = Message.createText(conversationId, userId, "Hola");
        when(sendTextMessage.execute(conversationId, new AuthenticatedUser(userId, AppType.PARTNER, "ACTIVE"), "Hola"))
                .thenReturn(m);
        mvc.perform(post(url).header("Authorization", token("PARTNER", "ACTIVE"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void postNonParticipant_403() throws Exception {
        when(sendTextMessage.execute(any(), any(), any())).thenThrow(new NotConversationParticipantException());
        mvc.perform(post(url).header("Authorization", token("CUSTOMER", "ACTIVE"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void postInactiveUser_403() throws Exception {
        mvc.perform(post(url).header("Authorization", token("CUSTOMER", "SUSPENDED"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verify(sendTextMessage, never()).execute(any(), any(), any());
    }

    @Test
    void postBlankText_400() throws Exception {
        mvc.perform(post(url).header("Authorization", token("CUSTOMER", "ACTIVE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest());
        verify(sendTextMessage, never()).execute(any(), any(), any());
    }

    @Test
    void postTooLongText_400() throws Exception {
        mvc.perform(post(url).header("Authorization", token("CUSTOMER", "ACTIVE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + "a".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postMissingBody_400() throws Exception {
        mvc.perform(post(url).header("Authorization", token("CUSTOMER", "ACTIVE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postConversationNotFound_404() throws Exception {
        when(sendTextMessage.execute(any(), any(), any())).thenThrow(new ConversationNotFoundException());
        mvc.perform(post(url).header("Authorization", token("CUSTOMER", "ACTIVE"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void postLimitReached_429() throws Exception {
        when(sendTextMessage.execute(any(), any(), any())).thenThrow(new DailyMessageLimitExceededException());
        mvc.perform(post(url).header("Authorization", token("CUSTOMER", "ACTIVE"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void getWithoutJwt_401() throws Exception {
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
    }

    @Test
    void getParticipant_200_defaultsPaging() throws Exception {
        Message m = Message.createText(conversationId, userId, "Hola");
        when(getConversationMessages.execute(conversationId,
                new AuthenticatedUser(userId, AppType.PARTNER, "ACTIVE"), 0, 50))
                .thenReturn(new MessagePage(List.of(m), 0, 50, 1, 1));

        mvc.perform(get(url).header("Authorization", token("PARTNER", "ACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(m.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void getNonParticipant_403() throws Exception {
        when(getConversationMessages.execute(any(), any(), anyInt(), anyInt()))
                .thenThrow(new NotConversationParticipantException());
        mvc.perform(get(url).header("Authorization", token("CUSTOMER", "ACTIVE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getNotFound_404() throws Exception {
        when(getConversationMessages.execute(any(), any(), anyInt(), anyInt()))
                .thenThrow(new ConversationNotFoundException());
        mvc.perform(get(url).header("Authorization", token("CUSTOMER", "ACTIVE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSizeAboveMax_400() throws Exception {
        mvc.perform(get(url).param("size", "101").header("Authorization", token("CUSTOMER", "ACTIVE")))
                .andExpect(status().isBadRequest());
        verify(getConversationMessages, never()).execute(any(), any(), anyInt(), anyInt());
    }

    // ---- IMAGE messages ----

    private String imagesUrl() { return url + "/images"; }

    private MockMultipartFile file(String name, String type) {
        return new MockMultipartFile("files", name, type, new byte[] {1, 2, 3});
    }

    @Test
    void postImagesWithoutJwt_401() throws Exception {
        mvc.perform(multipart(imagesUrl()).file(file("a.jpg", "image/jpeg")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postImages_201_returnsAttachments() throws Exception {
        MessageAttachment a = MessageAttachment.create("key", "a.jpg", "image/jpeg", 3,
                "mock://sone-messaging/key", Instant.now());
        Message m = Message.createImage(conversationId, userId, List.of(a), Instant.now());
        when(sendImageMessage.execute(any(), any(), any())).thenReturn(m);

        mvc.perform(multipart(imagesUrl()).file(file("a.jpg", "image/jpeg")).file(file("b.png", "image/png"))
                        .header("Authorization", token("CUSTOMER", "ACTIVE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("IMAGE"))
                .andExpect(jsonPath("$.content").doesNotExist())
                .andExpect(jsonPath("$.senderId").value(userId.toString()))
                .andExpect(jsonPath("$.attachments[0].url").value("mock://sone-messaging/key"))
                .andExpect(jsonPath("$.attachments[0].mimeType").value("image/jpeg"))
                .andExpect(jsonPath("$.attachments[0].sizeBytes").value(3))
                .andExpect(jsonPath("$.attachments[0].originalFileName").value("a.jpg"))
                .andExpect(jsonPath("$.attachments[0].storageKey").doesNotExist());
        org.mockito.ArgumentCaptor<List<com.daniel.sonemessagingservice.message.application.port.ImageUpload>> c =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(sendImageMessage).execute(org.mockito.ArgumentMatchers.eq(conversationId),
                org.mockito.ArgumentMatchers.eq(new AuthenticatedUser(userId, AppType.CUSTOMER, "ACTIVE")), c.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, c.getValue().size());
    }

    @Test
    void postImagesPartner_201() throws Exception {
        Message m = Message.createImage(conversationId, userId, List.of(MessageAttachment.create("k", "a.jpg",
                "image/jpeg", 3, "mock://x", Instant.now())), Instant.now());
        when(sendImageMessage.execute(any(), any(), any())).thenReturn(m);
        mvc.perform(multipart(imagesUrl()).file(file("a.jpg", "image/jpeg"))
                        .header("Authorization", token("PARTNER", "ACTIVE")))
                .andExpect(status().isCreated());
    }

    @Test
    void postImagesNonParticipant_403() throws Exception {
        when(sendImageMessage.execute(any(), any(), any())).thenThrow(new NotConversationParticipantException());
        mvc.perform(multipart(imagesUrl()).file(file("a.jpg", "image/jpeg"))
                        .header("Authorization", token("CUSTOMER", "ACTIVE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void postImagesNotFound_404() throws Exception {
        when(sendImageMessage.execute(any(), any(), any())).thenThrow(new ConversationNotFoundException());
        mvc.perform(multipart(imagesUrl()).file(file("a.jpg", "image/jpeg"))
                        .header("Authorization", token("CUSTOMER", "ACTIVE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void postImagesTooLarge_413() throws Exception {
        when(sendImageMessage.execute(any(), any(), any())).thenThrow(new ImageTooLargeException("too big"));
        mvc.perform(multipart(imagesUrl()).file(file("a.jpg", "image/jpeg"))
                        .header("Authorization", token("CUSTOMER", "ACTIVE")))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void postImagesInvalidType_415() throws Exception {
        when(sendImageMessage.execute(any(), any(), any())).thenThrow(new UnsupportedImageTypeException("bad"));
        mvc.perform(multipart(imagesUrl()).file(file("a.gif", "image/gif"))
                        .header("Authorization", token("CUSTOMER", "ACTIVE")))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void postImagesTooMany_400() throws Exception {
        when(sendImageMessage.execute(any(), any(), any())).thenThrow(new InvalidImageUploadException("max 3"));
        mvc.perform(multipart(imagesUrl()).file(file("1.jpg", "image/jpeg")).file(file("2.jpg", "image/jpeg"))
                        .file(file("3.jpg", "image/jpeg")).file(file("4.jpg", "image/jpeg"))
                        .header("Authorization", token("CUSTOMER", "ACTIVE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postImagesNoFiles_400() throws Exception {
        when(sendImageMessage.execute(any(), any(), any())).thenThrow(new InvalidImageUploadException("none"));
        mvc.perform(multipart(imagesUrl()).header("Authorization", token("CUSTOMER", "ACTIVE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postImagesLimitReached_429() throws Exception {
        when(sendImageMessage.execute(any(), any(), any())).thenThrow(new DailyMessageLimitExceededException());
        mvc.perform(multipart(imagesUrl()).file(file("a.jpg", "image/jpeg"))
                        .header("Authorization", token("CUSTOMER", "ACTIVE")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void postImagesStorageDown_503() throws Exception {
        when(sendImageMessage.execute(any(), any(), any()))
                .thenThrow(new FileStorageUnavailableException("down", new RuntimeException()));
        mvc.perform(multipart(imagesUrl()).file(file("a.jpg", "image/jpeg"))
                        .header("Authorization", token("CUSTOMER", "ACTIVE")))
                .andExpect(status().isServiceUnavailable());
    }
}
