package com.daniel.sonemessagingservice.message.infrastructure.storage;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.daniel.sonemessagingservice.message.application.port.FileStoragePort;
import com.daniel.sonemessagingservice.message.application.port.ImageUpload;
import com.daniel.sonemessagingservice.message.application.port.StoredFile;

/** Development storage: does not persist bytes, it only produces consistent metadata and a mock URL. */
@Component
@ConditionalOnProperty(name = "sone.storage.provider", havingValue = "mock", matchIfMissing = true)
public class MockFileStorageAdapter implements FileStoragePort {

    static final String URL_PREFIX = "mock://sone-messaging/";

    @Override
    public StoredFile store(ImageUpload upload) {
        String storageKey = UUID.randomUUID().toString();
        return new StoredFile(storageKey, URL_PREFIX + storageKey, upload.originalFileName(),
                upload.mimeType(), upload.sizeBytes());
    }
}
