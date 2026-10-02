package com.daniel.sonemessagingservice.conversation.infrastructure.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.daniel.sonemessagingservice.conversation.application.exception.PartnerEligibilityUnavailableException;
import com.daniel.sonemessagingservice.conversation.application.exception.PartnerNotFoundException;
import com.daniel.sonemessagingservice.conversation.application.port.PartnerEligibility;
import com.sun.net.httpserver.HttpServer;

class MarketplacePartnerEligibilityAdapterTest {

    private HttpServer server;
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private volatile int responseStatus = 200;
    private volatile String responseBody = "";
    private volatile long delayMillis = 0;

    private final UUID partnerId = UUID.randomUUID();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            try {
                exchange.sendResponseHeaders(responseStatus, bytes.length == 0 ? -1 : bytes.length);
                if (bytes.length > 0) {
                    exchange.getResponseBody().write(bytes);
                }
            } catch (IOException ignored) {
                // client gave up (timeout test)
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private MarketplacePartnerEligibilityAdapter adapter(Duration readTimeout) {
        MarketplaceProperties props = new MarketplaceProperties(
                "http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(1), readTimeout);
        return new MarketplacePartnerEligibilityAdapter(
                new MarketplaceClientConfig().marketplaceRestClient(props), () -> "abc.def.ghi");
    }

    @Test
    void ok_sendsExpectedRequestAndMapsResponse() {
        responseBody = "{\"partnerId\":\"" + partnerId + "\",\"status\":\"ACTIVE\"}";

        PartnerEligibility result = adapter(Duration.ofSeconds(2)).getEligibility(partnerId);

        assertEquals("GET", method.get());
        assertEquals("/api/partners/" + partnerId + "/messaging-eligibility", path.get());
        assertEquals("Bearer abc.def.ghi", authorization.get());
        assertEquals(partnerId, result.partnerId());
        assertEquals("ACTIVE", result.status());
        assertTrue(result.isActive());
    }

    @Test
    void nonActiveStatus_isMapped() {
        responseBody = "{\"partnerId\":\"" + partnerId + "\",\"status\":\"SUSPENDED\"}";

        PartnerEligibility result = adapter(Duration.ofSeconds(2)).getEligibility(partnerId);

        assertEquals("SUSPENDED", result.status());
    }

    @Test
    void notFound_mapsToPartnerNotFound() {
        responseStatus = 404;
        assertThrows(PartnerNotFoundException.class, () -> adapter(Duration.ofSeconds(2)).getEligibility(partnerId));
    }

    @Test
    void serverError_mapsToUnavailable() {
        responseStatus = 500;
        assertThrows(PartnerEligibilityUnavailableException.class,
                () -> adapter(Duration.ofSeconds(2)).getEligibility(partnerId));
    }

    @Test
    void timeout_mapsToUnavailable() {
        delayMillis = 1500;
        responseBody = "{\"partnerId\":\"" + partnerId + "\",\"status\":\"ACTIVE\"}";
        assertThrows(PartnerEligibilityUnavailableException.class,
                () -> adapter(Duration.ofMillis(300)).getEligibility(partnerId));
    }

    @Test
    void connectionRefused_mapsToUnavailable() {
        server.stop(0);
        assertThrows(PartnerEligibilityUnavailableException.class,
                () -> adapter(Duration.ofSeconds(1)).getEligibility(partnerId));
    }
}
