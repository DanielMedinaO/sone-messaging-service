package com.daniel.sonemessagingservice.message.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

import com.daniel.sonemessagingservice.message.domain.model.Message;

@Schema(description = "Chat message")
public record MessageResponse(
        @Schema(description = "Message identifier") UUID id,
        @Schema(description = "Conversation identifier") UUID conversationId,
        @Schema(description = "Sender identifier (the authenticated user who sent it)") UUID senderId,
        @Schema(description = "Message type", example = "TEXT", allowableValues = {"TEXT", "IMAGE"}) String type,
        @Schema(description = "Text content; null for IMAGE messages", nullable = true) String content,
        @Schema(description = "Image attachments (1-3 for IMAGE, empty for TEXT)") List<MessageAttachmentResponse> attachments,
        @Schema(description = "Creation timestamp (UTC)") Instant createdAt) {

    static MessageResponse from(Message m) {
        return new MessageResponse(m.getId(), m.getConversationId(), m.getSenderId(),
                m.getType().name(), m.getContent(),
                m.getAttachments().stream().map(MessageAttachmentResponse::from).toList(), m.getCreatedAt());
    }
}
