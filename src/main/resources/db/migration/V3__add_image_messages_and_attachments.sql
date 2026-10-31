ALTER TABLE main.messages ALTER COLUMN content DROP NOT NULL;

CREATE TABLE main.message_attachments (
    id                 UUID         PRIMARY KEY,
    message_id         UUID         NOT NULL,
    position           SMALLINT     NOT NULL,
    storage_key        VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    mime_type          VARCHAR(100) NOT NULL,
    size_bytes         BIGINT       NOT NULL,
    url                TEXT         NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_message_attachments_message FOREIGN KEY (message_id)
        REFERENCES main.messages (id) ON DELETE CASCADE
);

CREATE INDEX idx_message_attachments_message_id ON main.message_attachments (message_id);
