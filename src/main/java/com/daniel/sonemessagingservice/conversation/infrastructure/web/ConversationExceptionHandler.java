package com.daniel.sonemessagingservice.conversation.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.daniel.sonemessagingservice.conversation.application.exception.PartnerEligibilityUnavailableException;
import com.daniel.sonemessagingservice.conversation.application.exception.PartnerNotActiveException;
import com.daniel.sonemessagingservice.conversation.application.exception.PartnerNotFoundException;

@RestControllerAdvice
class ConversationExceptionHandler {

    @ExceptionHandler(PartnerNotFoundException.class)
    ProblemDetail partnerNotFound(PartnerNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(PartnerNotActiveException.class)
    ProblemDetail partnerNotActive(PartnerNotActiveException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(PartnerEligibilityUnavailableException.class)
    ProblemDetail unavailable(PartnerEligibilityUnavailableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Partner validation is temporarily unavailable");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
