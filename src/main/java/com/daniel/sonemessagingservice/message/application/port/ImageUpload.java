package com.daniel.sonemessagingservice.message.application.port;

/** Framework-independent image received from a client. */
public record ImageUpload(String originalFileName, String mimeType, long sizeBytes, byte[] content) {
}
