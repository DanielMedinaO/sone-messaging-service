package com.daniel.sonemessagingservice.shared.realtime.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Production must restrict allowed-origin-patterns to the real client origins; "*" is for local development. */
@ConfigurationProperties(prefix = "sone.websocket")
public record WebSocketProperties(List<String> allowedOriginPatterns) {

    public WebSocketProperties {
        allowedOriginPatterns = allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()
                ? List.of("*") : allowedOriginPatterns;
    }
}
