package com.daniel.sonemessagingservice.message.infrastructure.realtime;

import java.time.Instant;
import java.util.UUID;

import com.daniel.sonemessagingservice.message.domain.model.Message;
import com.daniel.sonemessagingservice.message.domain.model.MessageType;

public record RealtimeMessageResponse(UUID id, UUID conversationId, UUID senderId, MessageType type,
                                      String content, Instant createdAt) {

    public static RealtimeMessageResponse from(Message m) {
        return new RealtimeMessageResponse(m.getId(), m.getConversationId(), m.getSenderId(), m.getType(),
                m.getContent(), m.getCreatedAt());
    }
}
