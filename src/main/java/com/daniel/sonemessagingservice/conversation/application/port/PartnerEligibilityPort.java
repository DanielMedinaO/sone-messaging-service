package com.daniel.sonemessagingservice.conversation.application.port;

import java.util.UUID;

public interface PartnerEligibilityPort {

    /**
     * @throws com.daniel.sonemessagingservice.conversation.application.exception.PartnerNotFoundException
     *         if the partner does not exist
     * @throws com.daniel.sonemessagingservice.conversation.application.exception.PartnerEligibilityUnavailableException
     *         if the authoritative source cannot be consulted
     */
    PartnerEligibility getEligibility(UUID partnerId);
}
