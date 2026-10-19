package com.daniel.sonemessagingservice.message.domain.repository;

import java.time.Instant;
import java.util.UUID;

import com.daniel.sonemessagingservice.message.domain.model.Message;

public interface MessageRepository {

    Message save(Message message);

    /** Messages of a conversation, newest first. */
    MessagePage findByConversationId(UUID conversationId, int page, int size);

    /** Counts messages with {@code from <= createdAt < to}. */
    long countByConversationIdCreatedBetween(UUID conversationId, Instant from, Instant to);
}
