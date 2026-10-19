package com.daniel.sonemessagingservice.message.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;
import com.daniel.sonemessagingservice.conversation.domain.repository.ConversationRepository;
import com.daniel.sonemessagingservice.message.application.exception.ConversationNotFoundException;
import com.daniel.sonemessagingservice.message.application.exception.NotConversationParticipantException;
import com.daniel.sonemessagingservice.message.domain.model.Message;
import com.daniel.sonemessagingservice.message.domain.repository.MessagePage;
import com.daniel.sonemessagingservice.message.domain.repository.MessageRepository;
import com.daniel.sonemessagingservice.shared.security.model.AppType;
import com.daniel.sonemessagingservice.shared.security.model.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class GetConversationMessagesTest {

    @Mock ConversationRepository conversationRepository;
    @Mock MessageRepository messageRepository;

    GetConversationMessages useCase;
    final UUID customerId = UUID.randomUUID();
    final UUID partnerId = UUID.randomUUID();
    Conversation conversation;

    @BeforeEach
    void setUp() {
        useCase = new GetConversationMessages(new ConversationAccess(conversationRepository), messageRepository);
        conversation = Conversation.create(customerId, partnerId);
    }

    private void found() {
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
    }

    @Test
    void customerParticipantCanQuery() {
        found();
        MessagePage page = new MessagePage(List.of(), 0, 50, 0, 0);
        when(messageRepository.findByConversationId(conversation.getId(), 0, 50)).thenReturn(page);
        assertSame(page, useCase.execute(conversation.getId(),
                new AuthenticatedUser(customerId, AppType.CUSTOMER, "ACTIVE"), 0, 50));
    }

    @Test
    void partnerParticipantCanQuery() {
        found();
        MessagePage page = new MessagePage(List.of(), 0, 50, 0, 0);
        when(messageRepository.findByConversationId(conversation.getId(), 0, 50)).thenReturn(page);
        assertSame(page, useCase.execute(conversation.getId(),
                new AuthenticatedUser(partnerId, AppType.PARTNER, "ACTIVE"), 0, 50));
    }

    @Test
    void outsiderForbidden() {
        found();
        assertThrows(NotConversationParticipantException.class, () -> useCase.execute(conversation.getId(),
                new AuthenticatedUser(UUID.randomUUID(), AppType.CUSTOMER, "ACTIVE"), 0, 50));
        verify(messageRepository, never()).findByConversationId(conversation.getId(), 0, 50);
    }

    @Test
    void conversationNotFound() {
        UUID id = UUID.randomUUID();
        when(conversationRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ConversationNotFoundException.class, () -> useCase.execute(id,
                new AuthenticatedUser(customerId, AppType.CUSTOMER, "ACTIVE"), 0, 50));
    }

    @Test
    void paginationParametersArePassedAndOrderingIsPreserved() {
        found();
        Message newer = Message.createText(conversation.getId(), customerId, "2");
        Message older = Message.createText(conversation.getId(), customerId, "1");
        MessagePage page = new MessagePage(List.of(newer, older), 2, 2, 6, 3);
        when(messageRepository.findByConversationId(conversation.getId(), 2, 2)).thenReturn(page);

        MessagePage result = useCase.execute(conversation.getId(),
                new AuthenticatedUser(customerId, AppType.CUSTOMER, "ACTIVE"), 2, 2);

        assertEquals(List.of(newer, older), result.content());
        assertEquals(2, result.page());
    }
}
