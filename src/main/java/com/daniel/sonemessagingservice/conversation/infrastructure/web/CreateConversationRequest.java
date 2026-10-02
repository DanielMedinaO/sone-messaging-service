package com.daniel.sonemessagingservice.conversation.infrastructure.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateConversationRequest(@NotNull UUID partnerId) {
}
