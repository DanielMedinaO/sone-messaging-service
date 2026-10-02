package com.daniel.sonemessagingservice.conversation.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Conversation {

    private final UUID id;
    private final UUID customerId;
    private final UUID partnerId;
    private final ConversationStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant lastMessageAt;

    private Conversation(UUID id, UUID customerId, UUID partnerId, ConversationStatus status,
                         Instant createdAt, Instant updatedAt, Instant lastMessageAt) {
        this.id = id;
        this.customerId = customerId;
        this.partnerId = partnerId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastMessageAt = lastMessageAt;
    }

    /** Creates a brand conversation, applying all creation rules. */
    public static Conversation create(UUID customerId, UUID partnerId) {
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(partnerId, "partnerId must not be null");
        if (customerId.equals(partnerId)) {
            throw new IllegalArgumentException("customerId and partnerId must be different");
        }
        Instant now = Instant.now();
        return new Conversation(UUID.randomUUID(), customerId, partnerId,
                ConversationStatus.ACTIVE, now, now, null);
    }

    /** Rebuilds an existing conversation from persisted data, without creation rules. */
    public static Conversation reconstitute(UUID id, UUID customerId, UUID partnerId,
                                            ConversationStatus status, Instant createdAt,
                                            Instant updatedAt, Instant lastMessageAt) {
        return new Conversation(id, customerId, partnerId, status, createdAt, updatedAt, lastMessageAt);
    }

    public UUID getId() { return id; }

    public UUID getCustomerId() { return customerId; }

    public UUID getPartnerId() { return partnerId; }

    public ConversationStatus getStatus() { return status; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    public Instant getLastMessageAt() { return lastMessageAt; }
}
