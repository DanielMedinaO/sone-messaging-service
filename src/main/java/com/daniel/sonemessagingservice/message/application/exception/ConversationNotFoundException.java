package com.daniel.sonemessagingservice.message.application.exception;

public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException() {
        super("Conversation not found");
    }
}
