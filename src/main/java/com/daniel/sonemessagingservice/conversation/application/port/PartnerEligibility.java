package com.daniel.sonemessagingservice.conversation.application.port;

import java.util.UUID;

public record PartnerEligibility(UUID partnerId, String status) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
