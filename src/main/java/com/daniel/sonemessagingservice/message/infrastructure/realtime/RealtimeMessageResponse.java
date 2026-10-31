package com.daniel.sonemessagingservice.message.infrastructure.realtime;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.daniel.sonemessagingservice.message.domain.model.Message;
import com.daniel.sonemessagingservice.message.domain.model.MessageAttachment;
import com.daniel.sonemessagingservice.message.domain.model.MessageType;

/** Realtime payload: metadata and URLs only, never binary content. */
public record RealtimeMessageResponse(UUID id, UUID conversationId, UUID senderId, MessageType type,
                                      String content, List<Attachment> attachments, Instant createdAt) {

    public record Attachment(UUID id, String url, String mimeType, long sizeBytes, String originalFileName) {

        static Attachment from(MessageAttachment a) {
            return new Attachment(a.getId(), a.getUrl(), a.getMimeType(), a.getSizeBytes(),
                    a.getOriginalFileName());
        }
    }

    public static RealtimeMessageResponse from(Message m) {
        return new RealtimeMessageResponse(m.getId(), m.getConversationId(), m.getSenderId(), m.getType(),
                m.getContent(), m.getAttachments().stream().map(Attachment::from).toList(), m.getCreatedAt());
    }
}
