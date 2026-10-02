package com.daniel.sonemessagingservice.shared.security.jwt;

/** Gives access to the raw ACCESS token of the current request, for synchronous propagation only. */
public interface AccessTokenProvider {

    String currentToken();
}
