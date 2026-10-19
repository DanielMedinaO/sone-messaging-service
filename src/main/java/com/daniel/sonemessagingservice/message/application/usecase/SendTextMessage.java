package com.daniel.sonemessagingservice.message.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;
import com.daniel.sonemessagingservice.conversation.domain.repository.ConversationRepository;
import com.daniel.sonemessagingservice.message.application.exception.DailyMessageLimitExceededException;
import com.daniel.sonemessagingservice.message.domain.model.Message;
import com.daniel.sonemessagingservice.message.domain.repository.MessageRepository;
import com.daniel.sonemessagingservice.shared.security.model.AuthenticatedUser;

@Service
public class SendTextMessage {

    public static final int DAILY_LIMIT = 200;

    private final ConversationAccess conversationAccess;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final Clock clock;

    public SendTextMessage(ConversationAccess conversationAccess,
                           ConversationRepository conversationRepository,
                           MessageRepository messageRepository,
                           Clock clock) {
        this.conversationAccess = conversationAccess;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.clock = clock;
    }

    @Transactional
    public Message execute(UUID conversationId, AuthenticatedUser user, String text) {
        Conversation conversation = conversationAccess.requireParticipant(conversationId, user);

        Instant now = clock.instant();
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        Instant dayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant nextDayStart = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        if (messageRepository.countByConversationIdCreatedBetween(conversationId, dayStart, nextDayStart)
                >= DAILY_LIMIT) {
            throw new DailyMessageLimitExceededException();
        }

        Message message = messageRepository.save(Message.createText(conversationId, user.id(), text, now));
        conversation.registerMessage(message.getCreatedAt());
        conversationRepository.save(conversation);
        return message;
    }
}
