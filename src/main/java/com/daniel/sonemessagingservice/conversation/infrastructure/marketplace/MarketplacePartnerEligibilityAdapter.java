package com.daniel.sonemessagingservice.conversation.infrastructure.marketplace;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.daniel.sonemessagingservice.conversation.application.exception.PartnerEligibilityUnavailableException;
import com.daniel.sonemessagingservice.conversation.application.exception.PartnerNotFoundException;
import com.daniel.sonemessagingservice.conversation.application.port.PartnerEligibility;
import com.daniel.sonemessagingservice.conversation.application.port.PartnerEligibilityPort;
import com.daniel.sonemessagingservice.shared.security.jwt.AccessTokenProvider;

@Component
public class MarketplacePartnerEligibilityAdapter implements PartnerEligibilityPort {

    private final RestClient marketplaceRestClient;
    private final AccessTokenProvider accessTokenProvider;

    public MarketplacePartnerEligibilityAdapter(RestClient marketplaceRestClient,
                                                AccessTokenProvider accessTokenProvider) {
        this.marketplaceRestClient = marketplaceRestClient;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public PartnerEligibility getEligibility(UUID partnerId) {
        try {
            EligibilityResponse body = marketplaceRestClient.get()
                    .uri("/api/partners/{partnerId}/messaging-eligibility", partnerId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenProvider.currentToken())
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {
                        throw new PartnerNotFoundException();
                    })
                    .onStatus(status -> status.isError(), (req, res) -> {
                        throw new PartnerEligibilityUnavailableException(
                                "Marketplace responded with status " + res.getStatusCode().value(), null);
                    })
                    .body(EligibilityResponse.class);
            if (body == null || body.status() == null) {
                throw new PartnerEligibilityUnavailableException("Marketplace returned an empty response", null);
            }
            return new PartnerEligibility(partnerId, body.status());
        } catch (RestClientException e) {
            throw new PartnerEligibilityUnavailableException("Marketplace is not reachable", e);
        }
    }

    record EligibilityResponse(UUID partnerId, String status) {
    }
}
