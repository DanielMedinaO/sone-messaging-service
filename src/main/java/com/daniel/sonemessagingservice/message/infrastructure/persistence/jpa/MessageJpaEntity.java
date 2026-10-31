package com.daniel.sonemessagingservice.message.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.UUID;

import com.daniel.sonemessagingservice.message.domain.model.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "messages")
public class MessageJpaEntity {

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Required by JPA
    protected MessageJpaEntity() {
    }

    public MessageJpaEntity(UUID id, UUID conversationId, UUID senderId, MessageType type,
                            String content, Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.type = type;
        this.content = content;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }

    public UUID getConversationId() { return conversationId; }

    public UUID getSenderId() { return senderId; }

    public MessageType getType() { return type; }

    public String getContent() { return content; }

    public Instant getCreatedAt() { return createdAt; }
}
