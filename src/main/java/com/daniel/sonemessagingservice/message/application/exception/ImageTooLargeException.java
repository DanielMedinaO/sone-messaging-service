package com.daniel.sonemessagingservice.message.application.exception;

public class ImageTooLargeException extends RuntimeException {

    public ImageTooLargeException(String message) {
        super(message);
    }
}
