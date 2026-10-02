package com.daniel.sonemessagingservice.conversation.infrastructure.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daniel.sonemessagingservice.conversation.application.usecase.StartConversation;
import com.daniel.sonemessagingservice.shared.security.model.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final StartConversation startConversation;

    public ConversationController(StartConversation startConversation) {
        this.startConversation = startConversation;
    }

    @PostMapping
    public ConversationResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                       @Valid @RequestBody CreateConversationRequest request) {
        return ConversationResponse.from(startConversation.execute(user.id(), request.partnerId()));
    }
}
