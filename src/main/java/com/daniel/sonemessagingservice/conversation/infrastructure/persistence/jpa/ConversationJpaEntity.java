package com.daniel.sonemessagingservice.conversation.infrastructure.persistence.jpa;

import com.daniel.sonemessagingservice.conversation.domain.model.ConversationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class ConversationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    // Required by JPA
    protected ConversationJpaEntity() {
    }

    public ConversationJpaEntity(UUID id, UUID customerId, UUID partnerId, ConversationStatus status,
                                 Instant createdAt, Instant updatedAt, Instant lastMessageAt) {
        this.id = id;
        this.customerId = customerId;
        this.partnerId = partnerId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastMessageAt = lastMessageAt;
    }

    public UUID getId() { return id; }

    public UUID getCustomerId() { return customerId; }

    public UUID getPartnerId() { return partnerId; }

    public ConversationStatus getStatus() { return status; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    public Instant getLastMessageAt() { return lastMessageAt; }
}
