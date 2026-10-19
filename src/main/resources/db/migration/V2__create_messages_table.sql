CREATE TABLE main.messages (
    id              UUID        PRIMARY KEY,
    conversation_id UUID        NOT NULL,
    sender_id       UUID        NOT NULL,
    type            VARCHAR(20) NOT NULL,
    content         TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES main.conversations (id)
);

CREATE INDEX idx_messages_conversation_created_at
    ON main.messages (conversation_id, created_at DESC);
