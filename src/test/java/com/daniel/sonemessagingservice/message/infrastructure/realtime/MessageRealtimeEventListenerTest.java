package com.daniel.sonemessagingservice.message.infrastructure.realtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.mockito.ArgumentCaptor;

import com.daniel.sonemessagingservice.message.application.event.MessageSentEvent;
import com.daniel.sonemessagingservice.message.domain.model.Message;

class MessageRealtimeEventListenerTest {

    final SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
    final MessageRealtimeEventListener listener = new MessageRealtimeEventListener(template);
    final UUID customerId = UUID.randomUUID();
    final UUID partnerId = UUID.randomUUID();

    private Message message(UUID sender) {
        return Message.createText(UUID.randomUUID(), sender, "Hola", Instant.parse("2030-05-10T15:30:00Z"));
    }

    @Test
    void sendsSamePayloadToBothParticipantsIncludingSender() {
        Message m = message(customerId);
        listener.onMessageSent(new MessageSentEvent(m, customerId, partnerId));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSendToUser(eq(customerId.toString()), eq("/queue/messages"), payload.capture());
        ArgumentCaptor<Object> payload2 = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSendToUser(eq(partnerId.toString()), eq("/queue/messages"), payload2.capture());

        RealtimeMessageResponse r = (RealtimeMessageResponse) payload.getValue();
        assertEquals(r, payload2.getValue());
        assertEquals(m.getId(), r.id());
        assertEquals(m.getConversationId(), r.conversationId());
        assertEquals(customerId, r.senderId());
        assertEquals("Hola", r.content());
        assertEquals(m.getCreatedAt(), r.createdAt());
    }

    @Test
    void partnerSenderAlsoReceives() {
        listener.onMessageSent(new MessageSentEvent(message(partnerId), customerId, partnerId));
        verify(template).convertAndSendToUser(eq(customerId.toString()), eq("/queue/messages"), any());
        verify(template).convertAndSendToUser(eq(partnerId.toString()), eq("/queue/messages"), any());
    }

    @Test
    void deliveryFailureIsNotPropagatedAndOtherRecipientStillserved() {
        doThrow(new IllegalStateException("boom")).when(template)
                .convertAndSendToUser(eq(customerId.toString()), any(String.class), any(Object.class));
        assertDoesNotThrow(() -> listener.onMessageSent(new MessageSentEvent(message(customerId), customerId, partnerId)));
        verify(template).convertAndSendToUser(eq(partnerId.toString()), eq("/queue/messages"), any());
    }
}
