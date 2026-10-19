package com.daniel.sonemessagingservice.conversation.infrastructure.web;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to create or retrieve a conversation with a Partner")
public record CreateConversationRequest(
        @Schema(description = "Identifier of the Partner to talk to",
                example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull UUID partnerId) {
}
