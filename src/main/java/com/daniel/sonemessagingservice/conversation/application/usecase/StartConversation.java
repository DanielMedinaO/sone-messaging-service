package com.daniel.sonemessagingservice.conversation.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.daniel.sonemessagingservice.conversation.application.exception.PartnerNotActiveException;
import com.daniel.sonemessagingservice.conversation.application.port.PartnerEligibility;
import com.daniel.sonemessagingservice.conversation.application.port.PartnerEligibilityPort;
import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;

/** Customer status is verified by the JWT; partner status is verified against Marketplace. */
@Service
public class StartConversation {

    private final PartnerEligibilityPort partnerEligibilityPort;
    private final GetOrCreateConversation getOrCreateConversation;

    public StartConversation(PartnerEligibilityPort partnerEligibilityPort,
                             GetOrCreateConversation getOrCreateConversation) {
        this.partnerEligibilityPort = partnerEligibilityPort;
        this.getOrCreateConversation = getOrCreateConversation;
    }

    public Conversation execute(UUID customerId, UUID partnerId) {
        PartnerEligibility eligibility = partnerEligibilityPort.getEligibility(partnerId);
        if (eligibility == null || !eligibility.isActive()) {
            throw new PartnerNotActiveException();
        }
        return getOrCreateConversation.execute(customerId, partnerId);
    }
}
