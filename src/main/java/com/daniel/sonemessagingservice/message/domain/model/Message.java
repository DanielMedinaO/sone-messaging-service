package com.daniel.sonemessagingservice.message.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Message {

    public static final int MAX_TEXT_LENGTH = 2000;

    private final UUID id;
    private final UUID conversationId;
    private final UUID senderId;
    private final MessageType type;
    private final String content;
    private final Instant createdAt;

    private Message(UUID id, UUID conversationId, UUID senderId, MessageType type,
                    String content, Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.type = type;
        this.content = content;
        this.createdAt = createdAt;
    }

    /** Creates a new TEXT message stamped with the current instant. */
    public static Message createText(UUID conversationId, UUID senderId, String content) {
        return createText(conversationId, senderId, content, Instant.now());
    }

    /** Creates a new TEXT message applying all creation rules. */
    public static Message createText(UUID conversationId, UUID senderId, String content, Instant createdAt) {
        Objects.requireNonNull(conversationId, "conversationId must not be null");
        Objects.requireNonNull(senderId, "senderId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        if (content.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("content must not exceed " + MAX_TEXT_LENGTH + " characters");
        }
        return new Message(UUID.randomUUID(), conversationId, senderId, MessageType.TEXT, content, createdAt);
    }

    /** Rebuilds an existing message from persisted data, without creation rules. */
    public static Message reconstitute(UUID id, UUID conversationId, UUID senderId, MessageType type,
                                       String content, Instant createdAt) {
        return new Message(id, conversationId, senderId, type, content, createdAt);
    }

    public UUID getId() { return id; }

    public UUID getConversationId() { return conversationId; }

    public UUID getSenderId() { return senderId; }

    public MessageType getType() { return type; }

    public String getContent() { return content; }

    public Instant getCreatedAt() { return createdAt; }
}
