package com.daniel.sonemessagingservice.conversation.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;

public record ConversationResponse(UUID id, UUID customerId, UUID partnerId, String status,
                                   Instant createdAt, Instant updatedAt, Instant lastMessageAt) {

    static ConversationResponse from(Conversation c) {
        return new ConversationResponse(c.getId(), c.getCustomerId(), c.getPartnerId(),
                c.getStatus().name(), c.getCreatedAt(), c.getUpdatedAt(), c.getLastMessageAt());
    }
}
