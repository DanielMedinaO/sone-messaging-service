package com.daniel.sonemessagingservice.message.application.port;

import com.daniel.sonemessagingservice.message.application.exception.FileStorageUnavailableException;

/** Output port for storing message images. Adapters (mock, S3...) live in infrastructure. */
public interface FileStoragePort {

    /** @throws FileStorageUnavailableException if the storage backend cannot store the file */
    StoredFile store(ImageUpload upload);
}
