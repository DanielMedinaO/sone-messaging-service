package com.daniel.sonemessagingservice.message.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "message_attachments")
public class MessageAttachmentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(nullable = false)
    private short position;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, columnDefinition = "text")
    private String url;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Required by JPA
    protected MessageAttachmentJpaEntity() {
    }

    public MessageAttachmentJpaEntity(UUID id, UUID messageId, short position, String storageKey,
                                      String originalFileName, String mimeType, long sizeBytes, String url,
                                      Instant createdAt) {
        this.id = id;
        this.messageId = messageId;
        this.position = position;
        this.storageKey = storageKey;
        this.originalFileName = originalFileName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.url = url;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }

    public UUID getMessageId() { return messageId; }

    public short getPosition() { return position; }

    public String getStorageKey() { return storageKey; }

    public String getOriginalFileName() { return originalFileName; }

    public String getMimeType() { return mimeType; }

    public long getSizeBytes() { return sizeBytes; }

    public String getUrl() { return url; }

    public Instant getCreatedAt() { return createdAt; }
}
