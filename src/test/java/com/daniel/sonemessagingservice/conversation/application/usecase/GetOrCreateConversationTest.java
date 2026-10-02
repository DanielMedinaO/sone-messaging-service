package com.daniel.sonemessagingservice.conversation.application.usecase;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;
import com.daniel.sonemessagingservice.conversation.domain.model.ConversationStatus;
import com.daniel.sonemessagingservice.conversation.domain.repository.ConversationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrCreateConversationTest {

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private GetOrCreateConversation getOrCreateConversation;

    private final UUID customerId = UUID.randomUUID();
    private final UUID partnerId = UUID.randomUUID();

    @Test
    void returnsExistingConversation() {
        Conversation existing = Conversation.create(customerId, partnerId);
        when(conversationRepository.findByCustomerIdAndPartnerId(customerId, partnerId))
                .thenReturn(Optional.of(existing));

        Conversation result = getOrCreateConversation.execute(customerId, partnerId);

        assertSame(existing, result);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void createsAndSavesConversationWhenNoneExists() {
        when(conversationRepository.findByCustomerIdAndPartnerId(customerId, partnerId))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Conversation result = getOrCreateConversation.execute(customerId, partnerId);

        assertEquals(customerId, result.getCustomerId());
        assertEquals(partnerId, result.getPartnerId());
        assertEquals(ConversationStatus.ACTIVE, result.getStatus());
        verify(conversationRepository).save(result);
    }
}
