package com.daniel.sonemessagingservice.conversation.domain.repository;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository {

    Conversation save(Conversation conversation);

    Optional<Conversation> findById(UUID conversationId);

    Optional<Conversation> findByCustomerIdAndPartnerId(UUID customerId, UUID partnerId);
}
