package com.daniel.sonemessagingservice.message.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Message {

    public static final int MAX_TEXT_LENGTH = 2000;
    public static final int MIN_ATTACHMENTS = 1;
    public static final int MAX_ATTACHMENTS = 3;

    private final UUID id;
    private final UUID conversationId;
    private final UUID senderId;
    private final MessageType type;
    private final String content;
    private final List<MessageAttachment> attachments;
    private final Instant createdAt;

    private Message(UUID id, UUID conversationId, UUID senderId, MessageType type,
                    String content, List<MessageAttachment> attachments, Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.type = type;
        this.content = content;
        this.attachments = attachments == null ? List.of() : List.copyOf(attachments);
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
        return new Message(UUID.randomUUID(), conversationId, senderId, MessageType.TEXT, content, List.of(),
                createdAt);
    }

    /** Creates a new IMAGE message (no text content) carrying 1 to 3 attachments. */
    public static Message createImage(UUID conversationId, UUID senderId,
                                      List<MessageAttachment> attachments, Instant createdAt) {
        Objects.requireNonNull(conversationId, "conversationId must not be null");
        Objects.requireNonNull(senderId, "senderId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(attachments, "attachments must not be null");
        if (attachments.size() < MIN_ATTACHMENTS || attachments.size() > MAX_ATTACHMENTS) {
            throw new IllegalArgumentException("an IMAGE message requires between " + MIN_ATTACHMENTS
                    + " and " + MAX_ATTACHMENTS + " attachments");
        }
        return new Message(UUID.randomUUID(), conversationId, senderId, MessageType.IMAGE, null,
                attachments, createdAt);
    }

    /** Rebuilds an existing message from persisted data, without creation rules. */
    public static Message reconstitute(UUID id, UUID conversationId, UUID senderId, MessageType type,
                                       String content, Instant createdAt) {
        return reconstitute(id, conversationId, senderId, type, content, List.of(), createdAt);
    }

    public static Message reconstitute(UUID id, UUID conversationId, UUID senderId, MessageType type,
                                       String content, List<MessageAttachment> attachments, Instant createdAt) {
        return new Message(id, conversationId, senderId, type, content, attachments, createdAt);
    }

    public UUID getId() { return id; }

    public UUID getConversationId() { return conversationId; }

    public UUID getSenderId() { return senderId; }

    public MessageType getType() { return type; }

    public String getContent() { return content; }

    public List<MessageAttachment> getAttachments() { return attachments; }

    public Instant getCreatedAt() { return createdAt; }
}
