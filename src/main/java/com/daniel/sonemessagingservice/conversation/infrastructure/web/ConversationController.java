package com.daniel.sonemessagingservice.conversation.infrastructure.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daniel.sonemessagingservice.conversation.application.usecase.StartConversation;
import com.daniel.sonemessagingservice.shared.security.model.AuthenticatedUser;

import com.daniel.sonemessagingservice.shared.documentation.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Conversations", description = "Conversations between Customers and Partners")
public class ConversationController {

    private final StartConversation startConversation;

    public ConversationController(StartConversation startConversation) {
        this.startConversation = startConversation;
    }

    @PostMapping
    @Operation(summary = "Create or retrieve a conversation",
            description = "Creates a conversation between the authenticated Customer and the requested "
                    + "Partner if one does not already exist. If the conversation already exists, the "
                    + "existing conversation is returned. The Customer is taken from the authenticated JWT; "
                    + "only partnerId is sent in the body and customerId must not be sent by the client. "
                    + "Only Customers with status ACTIVE can start conversations. The target Partner must "
                    + "exist and have status ACTIVE. Only one persistent conversation exists per "
                    + "Customer + Partner combination.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponse(responseCode = "200", description = "Conversation created or already existing")
    @ApiResponse(responseCode = "400", description = "Invalid body or UUID",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "JWT missing, expired, invalid or not accepted",
            content = @Content)
    @ApiResponse(responseCode = "403",
            description = "Authenticated user is not an ACTIVE Customer, or the Partner is not ACTIVE",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Partner does not exist",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "Marketplace unavailable",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public ConversationResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                       @Valid @RequestBody CreateConversationRequest request) {
        return ConversationResponse.from(startConversation.execute(user.id(), request.partnerId()));
    }
}
