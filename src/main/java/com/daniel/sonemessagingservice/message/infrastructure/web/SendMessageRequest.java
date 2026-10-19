package com.daniel.sonemessagingservice.message.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to send a text message")
public record SendMessageRequest(
        @Schema(description = "Message text (1-2000 characters, not blank)",
                example = "Hola, ¿tienes disponibilidad mañana?", maxLength = 2000)
        @NotBlank @Size(max = 2000) String text) {
}
