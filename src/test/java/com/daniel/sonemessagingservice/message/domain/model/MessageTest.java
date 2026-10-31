package com.daniel.sonemessagingservice.message.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class MessageTest {

    private final UUID conversationId = UUID.randomUUID();
    private final UUID senderId = UUID.randomUUID();

    @Test
    void createText_valid() {
        Instant before = Instant.now();
        Message m = Message.createText(conversationId, senderId, "Hola");
        assertNotNull(m.getId());
        assertEquals(conversationId, m.getConversationId());
        assertEquals(senderId, m.getSenderId());
        assertEquals("Hola", m.getContent());
        assertEquals(MessageType.TEXT, m.getType());
        assertFalse(m.getCreatedAt().isBefore(before));
    }

    @Test
    void createText_acceptsMaxLength() {
        assertDoesNotThrow(() -> Message.createText(conversationId, senderId, "a".repeat(2000)));
    }

    @Test
    void nullConversationId() {
        assertThrows(NullPointerException.class, () -> Message.createText(null, senderId, "x"));
    }

    @Test
    void nullSenderId() {
        assertThrows(NullPointerException.class, () -> Message.createText(conversationId, null, "x"));
    }

    @Test
    void nullContent() {
        assertThrows(IllegalArgumentException.class, () -> Message.createText(conversationId, senderId, null));
    }

    @Test
    void emptyContent() {
        assertThrows(IllegalArgumentException.class, () -> Message.createText(conversationId, senderId, ""));
    }

    @Test
    void blankContent() {
        assertThrows(IllegalArgumentException.class, () -> Message.createText(conversationId, senderId, "  \n\t "));
    }

    @Test
    void tooLongContent() {
        assertThrows(IllegalArgumentException.class,
                () -> Message.createText(conversationId, senderId, "a".repeat(2001)));
    }

    private MessageAttachment attachment() {
        return MessageAttachment.create(UUID.randomUUID().toString(), "photo.jpg", "image/jpeg", 1000,
                "mock://sone-messaging/x", Instant.now());
    }

    @Test
    void createImage_valid() {
        Message m = Message.createImage(conversationId, senderId, List.of(attachment(), attachment()), Instant.now());
        assertNotNull(m.getId());
        assertEquals(MessageType.IMAGE, m.getType());
        assertNull(m.getContent());
        assertEquals(2, m.getAttachments().size());
        assertEquals(conversationId, m.getConversationId());
        assertEquals(senderId, m.getSenderId());
    }

    @Test
    void createImage_acceptsThree() {
        assertDoesNotThrow(() -> Message.createImage(conversationId, senderId,
                List.of(attachment(), attachment(), attachment()), Instant.now()));
    }

    @Test
    void createImage_zeroAttachmentsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Message.createImage(conversationId, senderId, List.of(), Instant.now()));
    }

    @Test
    void createImage_moreThanThreeRejected() {
        assertThrows(IllegalArgumentException.class, () -> Message.createImage(conversationId, senderId,
                List.of(attachment(), attachment(), attachment(), attachment()), Instant.now()));
    }

    @Test
    void createImage_requiresConversationAndSender() {
        assertThrows(NullPointerException.class,
                () -> Message.createImage(null, senderId, List.of(attachment()), Instant.now()));
        assertThrows(NullPointerException.class,
                () -> Message.createImage(conversationId, null, List.of(attachment()), Instant.now()));
    }

    @Test
    void text_hasNoAttachments() {
        assertTrue(Message.createText(conversationId, senderId, "Hola").getAttachments().isEmpty());
    }

    @Test
    void attachments_areImmutable() {
        Message m = Message.createImage(conversationId, senderId, List.of(attachment()), Instant.now());
        assertThrows(UnsupportedOperationException.class, () -> m.getAttachments().clear());
    }
}
