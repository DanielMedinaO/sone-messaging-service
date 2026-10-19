package com.daniel.sonemessagingservice.message.application.exception;

public class DailyMessageLimitExceededException extends RuntimeException {

    public DailyMessageLimitExceededException() {
        super("Daily message limit for this conversation has been reached");
    }
}
