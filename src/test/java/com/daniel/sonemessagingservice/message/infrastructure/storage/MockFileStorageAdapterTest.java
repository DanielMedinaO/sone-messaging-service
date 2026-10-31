package com.daniel.sonemessagingservice.message.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.daniel.sonemessagingservice.message.application.port.ImageUpload;
import com.daniel.sonemessagingservice.message.application.port.StoredFile;

class MockFileStorageAdapterTest {

    final MockFileStorageAdapter adapter = new MockFileStorageAdapter();

    private ImageUpload upload(String name) {
        return new ImageUpload(name, "image/png", 3, new byte[] {1, 2, 3});
    }

    @Test
    void generatesKeyAndUrlAndKeepsMetadata() {
        StoredFile f = adapter.store(upload("photo.png"));
        assertFalse(f.storageKey().isBlank());
        assertEquals("mock://sone-messaging/" + f.storageKey(), f.url());
        assertEquals("photo.png", f.originalFileName());
        assertEquals("image/png", f.mimeType());
        assertEquals(3, f.sizeBytes());
    }

    @Test
    void keyIsUniqueAndNotDerivedFromFileName() {
        StoredFile a = adapter.store(upload("../../etc/passwd.png"));
        StoredFile b = adapter.store(upload("../../etc/passwd.png"));
        assertNotEquals(a.storageKey(), b.storageKey());
        assertFalse(a.storageKey().contains("passwd"));
        assertFalse(a.storageKey().contains("."));
        assertDoesNotThrow(() -> java.util.UUID.fromString(a.storageKey()));
    }
}
