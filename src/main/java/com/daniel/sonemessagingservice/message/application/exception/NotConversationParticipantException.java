package com.daniel.sonemessagingservice.message.application.exception;

public class NotConversationParticipantException extends RuntimeException {

    public NotConversationParticipantException() {
        super("User does not belong to this conversation");
    }
}
