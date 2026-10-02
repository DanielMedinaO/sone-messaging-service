package com.daniel.sonemessagingservice.conversation.application.usecase;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.daniel.sonemessagingservice.conversation.application.exception.PartnerEligibilityUnavailableException;
import com.daniel.sonemessagingservice.conversation.application.exception.PartnerNotActiveException;
import com.daniel.sonemessagingservice.conversation.application.exception.PartnerNotFoundException;
import com.daniel.sonemessagingservice.conversation.application.port.PartnerEligibility;
import com.daniel.sonemessagingservice.conversation.application.port.PartnerEligibilityPort;
import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;

@ExtendWith(MockitoExtension.class)
class StartConversationTest {

    @Mock
    PartnerEligibilityPort port;
    @Mock
    GetOrCreateConversation getOrCreate;
    @InjectMocks
    StartConversation startConversation;

    final UUID customerId = UUID.randomUUID();
    final UUID partnerId = UUID.randomUUID();

    @Test
    void activePartner_getsOrCreatesConversation() {
        Conversation conversation = Conversation.create(customerId, partnerId);
        when(port.getEligibility(partnerId)).thenReturn(new PartnerEligibility(partnerId, "ACTIVE"));
        when(getOrCreate.execute(customerId, partnerId)).thenReturn(conversation);

        assertSame(conversation, startConversation.execute(customerId, partnerId));
        verify(port).getEligibility(partnerId);
    }

    @Test
    void nonActivePartner_isRejected() {
        when(port.getEligibility(partnerId)).thenReturn(new PartnerEligibility(partnerId, "SUSPENDED"));

        assertThrows(PartnerNotActiveException.class, () -> startConversation.execute(customerId, partnerId));
        verifyNoInteractions(getOrCreate);
    }

    @Test
    void unknownPartner_propagatesNotFound() {
        when(port.getEligibility(partnerId)).thenThrow(new PartnerNotFoundException());

        assertThrows(PartnerNotFoundException.class, () -> startConversation.execute(customerId, partnerId));
        verify(getOrCreate, never()).execute(customerId, partnerId);
    }

    @Test
    void marketplaceUnavailable_doesNotCreate() {
        when(port.getEligibility(partnerId))
                .thenThrow(new PartnerEligibilityUnavailableException("down", null));

        assertThrows(PartnerEligibilityUnavailableException.class,
                () -> startConversation.execute(customerId, partnerId));
        verifyNoInteractions(getOrCreate);
    }
}
