package com.daniel.sonemessagingservice.message.infrastructure.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.daniel.sonemessagingservice.message.domain.repository.MessagePage;

@Schema(description = "Page of messages, newest first")
public record MessagePageResponse(
        @Schema(description = "Messages in this page ordered by createdAt DESC") List<MessageResponse> content,
        @Schema(description = "Zero-based page index") int page,
        @Schema(description = "Requested page size") int size,
        @Schema(description = "Total messages in the conversation") long totalElements,
        @Schema(description = "Total number of pages") int totalPages) {

    static MessagePageResponse from(MessagePage p) {
        return new MessagePageResponse(p.content().stream().map(MessageResponse::from).toList(),
                p.page(), p.size(), p.totalElements(), p.totalPages());
    }
}
