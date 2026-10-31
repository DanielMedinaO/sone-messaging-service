package com.daniel.sonemessagingservice.message.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;
import com.daniel.sonemessagingservice.conversation.domain.repository.ConversationRepository;
import com.daniel.sonemessagingservice.message.application.event.MessageSentEvent;
import com.daniel.sonemessagingservice.message.domain.model.Message;
import com.daniel.sonemessagingservice.message.domain.repository.MessageRepository;
import com.daniel.sonemessagingservice.shared.security.model.AuthenticatedUser;

@Service
public class SendTextMessage {

    private final ConversationAccess conversationAccess;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public SendTextMessage(ConversationAccess conversationAccess,
                           ConversationRepository conversationRepository,
                           MessageRepository messageRepository,
                           Clock clock,
                           ApplicationEventPublisher eventPublisher) {
        this.conversationAccess = conversationAccess;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Message execute(UUID conversationId, AuthenticatedUser user, String text) {
        Conversation conversation = conversationAccess.requireParticipant(conversationId, user);

        Instant now = clock.instant();
        DailyMessageLimit.ensureNotReached(messageRepository, conversationId, now);

        Message message = messageRepository.save(Message.createText(conversationId, user.id(), text, now));
        conversation.registerMessage(message.getCreatedAt());
        conversationRepository.save(conversation);
        eventPublisher.publishEvent(
                new MessageSentEvent(message, conversation.getCustomerId(), conversation.getPartnerId()));
        return message;
    }
}
