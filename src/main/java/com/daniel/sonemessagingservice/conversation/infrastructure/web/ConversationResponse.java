package com.daniel.sonemessagingservice.conversation.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;

@Schema(description = "Conversation between a Customer and a Partner")
public record ConversationResponse(
        @Schema(description = "Conversation identifier") UUID id,
        @Schema(description = "Customer identifier") UUID customerId,
        @Schema(description = "Partner identifier") UUID partnerId,
        @Schema(description = "Conversation status", example = "ACTIVE") String status,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt,
        @Schema(description = "Timestamp of the last message, if any", nullable = true) Instant lastMessageAt) {

    static ConversationResponse from(Conversation c) {
        return new ConversationResponse(c.getId(), c.getCustomerId(), c.getPartnerId(),
                c.getStatus().name(), c.getCreatedAt(), c.getUpdatedAt(), c.getLastMessageAt());
    }
}
