package com.daniel.sonemessagingservice.conversation.application.usecase;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;
import com.daniel.sonemessagingservice.conversation.domain.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetOrCreateConversation {

    private final ConversationRepository conversationRepository;

    public GetOrCreateConversation(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    public Conversation execute(UUID customerId, UUID partnerId) {
        return conversationRepository.findByCustomerIdAndPartnerId(customerId, partnerId)
                .orElseGet(() -> conversationRepository.save(Conversation.create(customerId, partnerId)));
    }
}
