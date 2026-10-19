package com.daniel.sonemessagingservice.message.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
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
}
