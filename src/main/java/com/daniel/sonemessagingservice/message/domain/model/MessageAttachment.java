package com.daniel.sonemessagingservice.message.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Image attached to a {@link Message}. Child of the Message aggregate, not an aggregate itself. */
public class MessageAttachment {

    private final UUID id;
    private final String storageKey;
    private final String originalFileName;
    private final String mimeType;
    private final long sizeBytes;
    private final String url;
    private final Instant createdAt;

    private MessageAttachment(UUID id, String storageKey, String originalFileName, String mimeType,
                              long sizeBytes, String url, Instant createdAt) {
        this.id = id;
        this.storageKey = storageKey;
        this.originalFileName = originalFileName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.url = url;
        this.createdAt = createdAt;
    }

    public static MessageAttachment create(String storageKey, String originalFileName, String mimeType,
                                           long sizeBytes, String url, Instant createdAt) {
        requireText(storageKey, "storageKey");
        requireText(originalFileName, "originalFileName");
        requireText(mimeType, "mimeType");
        requireText(url, "url");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("sizeBytes must be positive");
        }
        return new MessageAttachment(UUID.randomUUID(), storageKey, originalFileName, mimeType, sizeBytes,
                url, createdAt);
    }

    public static MessageAttachment reconstitute(UUID id, String storageKey, String originalFileName,
                                                 String mimeType, long sizeBytes, String url, Instant createdAt) {
        return new MessageAttachment(id, storageKey, originalFileName, mimeType, sizeBytes, url, createdAt);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    public UUID getId() { return id; }

    public String getStorageKey() { return storageKey; }

    public String getOriginalFileName() { return originalFileName; }

    public String getMimeType() { return mimeType; }

    public long getSizeBytes() { return sizeBytes; }

    public String getUrl() { return url; }

    public Instant getCreatedAt() { return createdAt; }
}
