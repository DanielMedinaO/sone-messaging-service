package com.daniel.sonemessagingservice.message.infrastructure.web;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

import com.daniel.sonemessagingservice.message.domain.model.MessageAttachment;

@Schema(description = "Image attached to an IMAGE message")
public record MessageAttachmentResponse(
        @Schema(description = "Attachment identifier") UUID id,
        @Schema(description = "Image URL", example = "mock://sone-messaging/6b0f1c1e-3c1a-4c55-9f43-1f2d7b7d9a10") String url,
        @Schema(description = "Image MIME type", example = "image/jpeg") String mimeType,
        @Schema(description = "Image size in bytes", example = "120000") long sizeBytes,
        @Schema(description = "Original file name as sent by the client", example = "photo.jpg") String originalFileName) {

    static MessageAttachmentResponse from(MessageAttachment a) {
        return new MessageAttachmentResponse(a.getId(), a.getUrl(), a.getMimeType(), a.getSizeBytes(),
                a.getOriginalFileName());
    }
}
