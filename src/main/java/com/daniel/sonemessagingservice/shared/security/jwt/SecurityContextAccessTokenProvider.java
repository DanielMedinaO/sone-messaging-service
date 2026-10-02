package com.daniel.sonemessagingservice.shared.security.jwt;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextAccessTokenProvider implements AccessTokenProvider {

    @Override
    public String currentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof SoneAuthenticationToken token) {
            return (String) token.getCredentials();
        }
        throw new IllegalStateException("No authenticated ACCESS token in the current security context");
    }
}
