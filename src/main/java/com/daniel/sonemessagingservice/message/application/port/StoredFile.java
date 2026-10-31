package com.daniel.sonemessagingservice.message.application.port;

/** Result of storing a file: where it lives and its metadata. */
public record StoredFile(String storageKey, String url, String originalFileName, String mimeType,
                         long sizeBytes) {
}
