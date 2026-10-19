package com.daniel.sonemessagingservice.message.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.daniel.sonemessagingservice.message.domain.repository.MessagePage;
import com.daniel.sonemessagingservice.message.domain.repository.MessageRepository;
import com.daniel.sonemessagingservice.shared.security.model.AuthenticatedUser;

@Service
public class GetConversationMessages {

    private final ConversationAccess conversationAccess;
    private final MessageRepository messageRepository;

    public GetConversationMessages(ConversationAccess conversationAccess, MessageRepository messageRepository) {
        this.conversationAccess = conversationAccess;
        this.messageRepository = messageRepository;
    }

    @Transactional(readOnly = true)
    public MessagePage execute(UUID conversationId, AuthenticatedUser user, int page, int size) {
        conversationAccess.requireParticipant(conversationId, user);
        return messageRepository.findByConversationId(conversationId, page, size);
    }
}
