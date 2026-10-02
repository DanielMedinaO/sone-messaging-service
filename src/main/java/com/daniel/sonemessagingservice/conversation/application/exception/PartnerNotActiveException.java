package com.daniel.sonemessagingservice.conversation.application.exception;

public class PartnerNotActiveException extends RuntimeException {

    public PartnerNotActiveException() {
        super("Partner is not available for messaging");
    }
}
