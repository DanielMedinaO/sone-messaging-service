package com.daniel.sonemessagingservice.message.application.event;

import java.util.UUID;

import com.daniel.sonemessagingservice.message.domain.model.Message;

/** In-process event published by SendTextMessage; delivered to listeners after the transaction commits. */
public record MessageSentEvent(Message message, UUID customerId, UUID partnerId) {
}
