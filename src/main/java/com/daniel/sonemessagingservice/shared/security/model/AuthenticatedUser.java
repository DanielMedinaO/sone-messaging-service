package com.daniel.sonemessagingservice.shared.security.model;

import java.util.UUID;

public record AuthenticatedUser(UUID id, AppType appType, String status) {
}
