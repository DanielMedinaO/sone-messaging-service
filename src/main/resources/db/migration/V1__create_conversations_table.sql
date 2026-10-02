CREATE SCHEMA IF NOT EXISTS main;

CREATE TABLE main.conversations (
    id              UUID        PRIMARY KEY,
    customer_id     UUID        NOT NULL,
    partner_id      UUID        NOT NULL,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    last_message_at TIMESTAMPTZ NULL,
    CONSTRAINT uk_conversations_customer_partner UNIQUE (customer_id, partner_id),
    CONSTRAINT ck_conversations_different_users CHECK (customer_id <> partner_id)
);
