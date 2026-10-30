package com.daniel.sonemessagingservice.message.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import com.daniel.sonemessagingservice.message.application.event.MessageSentEvent;
import org.mockito.junit.jupiter.MockitoExtension;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;
import com.daniel.sonemessagingservice.conversation.domain.repository.ConversationRepository;
import com.daniel.sonemessagingservice.message.application.exception.ConversationNotFoundException;
import com.daniel.sonemessagingservice.message.application.exception.DailyMessageLimitExceededException;
import com.daniel.sonemessagingservice.message.application.exception.NotConversationParticipantException;
import com.daniel.sonemessagingservice.message.domain.model.Message;
import com.daniel.sonemessagingservice.message.domain.model.MessageType;
import com.daniel.sonemessagingservice.message.domain.repository.MessageRepository;
import com.daniel.sonemessagingservice.shared.security.model.AppType;
import com.daniel.sonemessagingservice.shared.security.model.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class SendTextMessageTest {

    private static final Instant NOW = Instant.parse("2030-05-10T15:30:00Z");
    private static final Instant DAY_START = Instant.parse("2030-05-10T00:00:00Z");
    private static final Instant NEXT_DAY = Instant.parse("2030-05-11T00:00:00Z");

    @Mock ConversationRepository conversationRepository;
    @Mock MessageRepository messageRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    SendTextMessage useCase;
    final UUID customerId = UUID.randomUUID();
    final UUID partnerId = UUID.randomUUID();
    Conversation conversation;

    @BeforeEach
    void setUp() {
        useCase = new SendTextMessage(new ConversationAccess(conversationRepository),
                conversationRepository, messageRepository, Clock.fixed(NOW, ZoneOffset.UTC), eventPublisher);
        conversation = Conversation.create(customerId, partnerId);
    }

    private void found() {
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
    }

    private void stubPersistence(long count) {
        found();
        when(messageRepository.countByConversationIdCreatedBetween(conversation.getId(), DAY_START, NEXT_DAY))
                .thenReturn(count);
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));
    }

    private AuthenticatedUser customer(UUID id) { return new AuthenticatedUser(id, AppType.CUSTOMER, "ACTIVE"); }

    private AuthenticatedUser partner(UUID id) { return new AuthenticatedUser(id, AppType.PARTNER, "ACTIVE"); }

    @Test
    void customerParticipantCanSend() {
        stubPersistence(0);
        Message m = useCase.execute(conversation.getId(), customer(customerId), "Hola");
        assertEquals(customerId, m.getSenderId());
        assertEquals(MessageType.TEXT, m.getType());
        assertEquals(NOW, m.getCreatedAt());
    }

    @Test
    void partnerParticipantCanSend() {
        stubPersistence(0);
        Message m = useCase.execute(conversation.getId(), partner(partnerId), "Hola");
        assertEquals(partnerId, m.getSenderId());
    }

    @Test
    void publishesMessageSentEventWithParticipants() {
        stubPersistence(0);
        Message m = useCase.execute(conversation.getId(), partner(partnerId), "Hola");
        ArgumentCaptor<MessageSentEvent> captor = ArgumentCaptor.forClass(MessageSentEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertSame(m, captor.getValue().message());
        assertEquals(customerId, captor.getValue().customerId());
        assertEquals(partnerId, captor.getValue().partnerId());
    }

    @Test
    void foreignCustomerForbidden() {
        found();
        assertThrows(NotConversationParticipantException.class,
                () -> useCase.execute(conversation.getId(), customer(UUID.randomUUID()), "Hola"));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void foreignPartnerForbidden() {
        found();
        assertThrows(NotConversationParticipantException.class,
                () -> useCase.execute(conversation.getId(), partner(UUID.randomUUID()), "Hola"));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void partnerCannotActAsCustomerAndViceVersa() {
        found();
        assertThrows(NotConversationParticipantException.class,
                () -> useCase.execute(conversation.getId(), partner(customerId), "Hola"));
        assertThrows(NotConversationParticipantException.class,
                () -> useCase.execute(conversation.getId(), customer(partnerId), "Hola"));
    }

    @Test
    void conversationNotFound() {
        UUID id = UUID.randomUUID();
        when(conversationRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ConversationNotFoundException.class, () -> useCase.execute(id, customer(customerId), "Hola"));
    }

    @Test
    void allows199() {
        stubPersistence(199);
        assertDoesNotThrow(() -> useCase.execute(conversation.getId(), customer(customerId), "Hola"));
    }

    @Test
    void rejects200() {
        found();
        when(messageRepository.countByConversationIdCreatedBetween(conversation.getId(), DAY_START, NEXT_DAY))
                .thenReturn(200L);
        assertThrows(DailyMessageLimitExceededException.class,
                () -> useCase.execute(conversation.getId(), customer(customerId), "Hola"));
        verify(messageRepository, never()).save(any());
        verify(conversationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void countsOnlyTheCurrentUtcDayInterval() {
        stubPersistence(0);
        useCase.execute(conversation.getId(), customer(customerId), "Hola");
        verify(messageRepository).countByConversationIdCreatedBetween(eq(conversation.getId()), eq(DAY_START), eq(NEXT_DAY));
    }

    @Test
    void persistsMessageAndUpdatesConversation() {
        stubPersistence(0);
        Message m = useCase.execute(conversation.getId(), customer(customerId), "Hola");
        verify(messageRepository).save(m);
        verify(conversationRepository).save(conversation);
        assertEquals(m.getCreatedAt(), conversation.getLastMessageAt());
        assertEquals(m.getCreatedAt(), conversation.getUpdatedAt());
    }

    @Test
    void invalidTextRejectedBeforePersisting() {
        stubPersistenceNoSave();
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(conversation.getId(), customer(customerId), "   "));
        verify(messageRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    private void stubPersistenceNoSave() {
        found();
        when(messageRepository.countByConversationIdCreatedBetween(any(), any(), any())).thenReturn(0L);
    }
}
