package com.daniel.sonemessagingservice.message.infrastructure.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.daniel.sonemessagingservice.message.application.event.MessageSentEvent;

/** Best-effort realtime delivery after commit; REST history remains the source of truth. */
@Component
public class MessageRealtimeEventListener {

    static final String DESTINATION = "/queue/messages";
    private static final Logger log = LoggerFactory.getLogger(MessageRealtimeEventListener.class);

    private final SimpMessagingTemplate messagingTemplate;

    public MessageRealtimeEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentEvent event) {
        RealtimeMessageResponse payload = RealtimeMessageResponse.from(event.message());
        deliver(event.customerId().toString(), payload);
        deliver(event.partnerId().toString(), payload);
    }

    private void deliver(String userName, RealtimeMessageResponse payload) {
        try {
            messagingTemplate.convertAndSendToUser(userName, DESTINATION, payload);
        } catch (RuntimeException e) {
            log.warn("Realtime delivery failed for message {} to user {}", payload.id(), userName, e);
        }
    }
}
