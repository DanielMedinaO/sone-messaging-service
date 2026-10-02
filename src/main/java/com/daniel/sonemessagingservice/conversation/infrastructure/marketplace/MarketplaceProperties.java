package com.daniel.sonemessagingservice.conversation.infrastructure.marketplace;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sone.marketplace")
public record MarketplaceProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}
