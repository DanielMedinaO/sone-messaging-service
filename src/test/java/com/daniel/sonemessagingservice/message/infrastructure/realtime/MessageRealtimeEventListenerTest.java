package com.daniel.sonemessagingservice.message.infrastructure.realtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.mockito.ArgumentCaptor;

import com.daniel.sonemessagingservice.message.application.event.MessageSentEvent;
import com.daniel.sonemessagingservice.message.domain.model.Message;
import com.daniel.sonemessagingservice.message.domain.model.MessageAttachment;
import com.daniel.sonemessagingservice.message.domain.model.MessageType;

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

    @Test
    void imageMessageDeliveredToBothWithAttachmentsAndNoBinary() {
        MessageAttachment a = MessageAttachment.create("key-1", "photo.jpg", "image/jpeg", 1234,
                "mock://sone-messaging/key-1", Instant.parse("2030-05-10T15:30:00Z"));
        Message m = Message.createImage(UUID.randomUUID(), customerId, List.of(a), Instant.parse("2030-05-10T15:30:00Z"));
        listener.onMessageSent(new MessageSentEvent(m, customerId, partnerId));

        ArgumentCaptor<Object> c1 = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> c2 = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSendToUser(eq(customerId.toString()), eq("/queue/messages"), c1.capture());
        verify(template).convertAndSendToUser(eq(partnerId.toString()), eq("/queue/messages"), c2.capture());

        RealtimeMessageResponse r = (RealtimeMessageResponse) c1.getValue();
        assertEquals(r, c2.getValue());
        assertEquals(MessageType.IMAGE, r.type());
        assertNull(r.content());
        assertEquals(1, r.attachments().size());
        assertEquals("mock://sone-messaging/key-1", r.attachments().get(0).url());
        assertEquals("image/jpeg", r.attachments().get(0).mimeType());
        assertEquals(1234, r.attachments().get(0).sizeBytes());
        assertEquals("photo.jpg", r.attachments().get(0).originalFileName());
        for (var f : RealtimeMessageResponse.Attachment.class.getRecordComponents()) {
            assertNotEquals(byte[].class, f.getType());
        }
    }
}
