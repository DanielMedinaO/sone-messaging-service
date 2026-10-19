package com.daniel.sonemessagingservice.message.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;
import com.daniel.sonemessagingservice.conversation.domain.repository.ConversationRepository;
import com.daniel.sonemessagingservice.message.application.exception.ConversationNotFoundException;
import com.daniel.sonemessagingservice.message.application.exception.NotConversationParticipantException;
import com.daniel.sonemessagingservice.shared.security.model.AuthenticatedUser;

/** Loads a conversation and verifies the authenticated user is one of its participants. */
@Component
public class ConversationAccess {

    private final ConversationRepository conversationRepository;

    public ConversationAccess(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public Conversation requireParticipant(UUID conversationId, AuthenticatedUser user) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(ConversationNotFoundException::new);
        UUID expected = switch (user.appType()) {
            case CUSTOMER -> conversation.getCustomerId();
            case PARTNER -> conversation.getPartnerId();
        };
        if (!user.id().equals(expected)) {
            throw new NotConversationParticipantException();
        }
        return conversation;
    }
}
