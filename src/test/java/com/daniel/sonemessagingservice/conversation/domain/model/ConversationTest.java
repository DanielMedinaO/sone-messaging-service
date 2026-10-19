package com.daniel.sonemessagingservice.conversation.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConversationTest {

    private final UUID customerId = UUID.randomUUID();
    private final UUID partnerId = UUID.randomUUID();

    @Test
    void createsValidConversation() {
        Conversation conversation = Conversation.create(customerId, partnerId);

        assertNotNull(conversation.getId());
        assertEquals(customerId, conversation.getCustomerId());
        assertEquals(partnerId, conversation.getPartnerId());
        assertNotNull(conversation.getCreatedAt());
        assertEquals(conversation.getCreatedAt(), conversation.getUpdatedAt());
    }

    @Test
    void rejectsNullCustomerId() {
        assertThrows(NullPointerException.class, () -> Conversation.create(null, partnerId));
    }

    @Test
    void rejectsNullPartnerId() {
        assertThrows(NullPointerException.class, () -> Conversation.create(customerId, null));
    }

    @Test
    void rejectsSameCustomerAndPartner() {
        assertThrows(IllegalArgumentException.class, () -> Conversation.create(customerId, customerId));
    }

    @Test
    void startsAsActive() {
        assertEquals(ConversationStatus.ACTIVE, Conversation.create(customerId, partnerId).getStatus());
    }

    @Test
    void startsWithoutLastMessageAt() {
        assertNull(Conversation.create(customerId, partnerId).getLastMessageAt());
    }

    @Test
    void registerMessage_updatesLastMessageAtAndUpdatedAt() {
        Conversation c = Conversation.create(UUID.randomUUID(), UUID.randomUUID());
        assertNull(c.getLastMessageAt());
        java.time.Instant at = java.time.Instant.parse("2030-01-01T10:00:00Z");

        c.registerMessage(at);

        assertEquals(at, c.getLastMessageAt());
        assertEquals(at, c.getUpdatedAt());
    }

    @Test
    void registerMessage_nullRejected() {
        Conversation c = Conversation.create(UUID.randomUUID(), UUID.randomUUID());
        assertThrows(NullPointerException.class, () -> c.registerMessage(null));
    }
}
