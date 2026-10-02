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
}
