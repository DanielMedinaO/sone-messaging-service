package com.daniel.sonemessagingservice.conversation.application.exception;

public class PartnerNotFoundException extends RuntimeException {

    public PartnerNotFoundException() {
        super("Partner not found");
    }
}
