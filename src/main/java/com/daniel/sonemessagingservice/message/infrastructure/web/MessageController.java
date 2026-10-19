package com.daniel.sonemessagingservice.message.infrastructure.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.daniel.sonemessagingservice.message.application.usecase.GetConversationMessages;
import com.daniel.sonemessagingservice.message.application.usecase.SendTextMessage;
import com.daniel.sonemessagingservice.shared.documentation.config.OpenApiConfig;
import com.daniel.sonemessagingservice.shared.security.model.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/messages")
@Tag(name = "Messages", description = "Text messages inside a conversation")
public class MessageController {

    private final SendTextMessage sendTextMessage;
    private final GetConversationMessages getConversationMessages;

    public MessageController(SendTextMessage sendTextMessage, GetConversationMessages getConversationMessages) {
        this.sendTextMessage = sendTextMessage;
        this.getConversationMessages = getConversationMessages;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a text message",
            description = "Sends a TEXT message in the conversation. The sender is always the authenticated "
                    + "user (JWT); only 'text' is accepted in the body. The user must be an ACTIVE Customer "
                    + "or Partner belonging to the conversation. A conversation accepts at most 200 messages "
                    + "per UTC day (00:00:00 UTC to 00:00:00 UTC of the next day).",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponse(responseCode = "201", description = "Message persisted")
    @ApiResponse(responseCode = "400", description = "Invalid or blank text, text longer than 2000 characters, or invalid body/UUID",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "JWT missing, expired, invalid or not accepted",
            content = @Content)
    @ApiResponse(responseCode = "403", description = "User is not ACTIVE or does not belong to the conversation",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Conversation does not exist",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "429", description = "Daily limit of 200 messages (UTC) reached for the conversation",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public MessageResponse send(@AuthenticationPrincipal AuthenticatedUser user,
                                @PathVariable UUID conversationId,
                                @Valid @RequestBody SendMessageRequest request) {
        return MessageResponse.from(sendTextMessage.execute(conversationId, user, request.text()));
    }

    @GetMapping
    @Operation(summary = "List conversation messages",
            description = "Returns the conversation history paginated, most recent messages first "
                    + "(createdAt DESC). Only the Customer or Partner belonging to the conversation can "
                    + "query it. Default page=0, size=50, maximum size=100.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponse(responseCode = "200", description = "Page of messages")
    @ApiResponse(responseCode = "400", description = "Invalid page, size or UUID",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "JWT missing, expired, invalid or not accepted",
            content = @Content)
    @ApiResponse(responseCode = "403", description = "User is not ACTIVE or does not belong to the conversation",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Conversation does not exist",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public MessagePageResponse list(@AuthenticationPrincipal AuthenticatedUser user,
                                    @PathVariable UUID conversationId,
                                    @Parameter(description = "Zero-based page index")
                                    @RequestParam(defaultValue = "0") @Min(0) int page,
                                    @Parameter(description = "Page size (max 100)")
                                    @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return MessagePageResponse.from(getConversationMessages.execute(conversationId, user, page, size));
    }
}
