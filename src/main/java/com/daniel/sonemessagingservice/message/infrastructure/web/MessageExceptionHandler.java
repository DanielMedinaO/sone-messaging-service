package com.daniel.sonemessagingservice.message.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.daniel.sonemessagingservice.message.application.exception.ConversationNotFoundException;
import com.daniel.sonemessagingservice.message.application.exception.DailyMessageLimitExceededException;
import com.daniel.sonemessagingservice.message.application.exception.FileStorageUnavailableException;
import com.daniel.sonemessagingservice.message.application.exception.ImageTooLargeException;
import com.daniel.sonemessagingservice.message.application.exception.InvalidImageUploadException;
import com.daniel.sonemessagingservice.message.application.exception.NotConversationParticipantException;
import com.daniel.sonemessagingservice.message.application.exception.UnsupportedImageTypeException;

@RestControllerAdvice
class MessageExceptionHandler {

    @ExceptionHandler(ConversationNotFoundException.class)
    ProblemDetail notFound(ConversationNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NotConversationParticipantException.class)
    ProblemDetail forbidden(NotConversationParticipantException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(DailyMessageLimitExceededException.class)
    ProblemDetail tooMany(DailyMessageLimitExceededException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(InvalidImageUploadException.class)
    ProblemDetail invalidImages(InvalidImageUploadException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ImageTooLargeException.class)
    ProblemDetail imageTooLarge(ImageTooLargeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail uploadTooLarge(MaxUploadSizeExceededException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "Upload exceeds the maximum allowed size (5 MB per image)");
    }

    @ExceptionHandler(UnsupportedImageTypeException.class)
    ProblemDetail unsupportedType(UnsupportedImageTypeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getMessage());
    }

    @ExceptionHandler(FileStorageUnavailableException.class)
    ProblemDetail storageUnavailable(FileStorageUnavailableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "File storage is unavailable");
    }
}
