package com.daniel.sonemessagingservice.shared.security.jwt;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import com.daniel.sonemessagingservice.shared.security.model.AuthenticatedUser;

public class SoneAuthenticationToken extends AbstractAuthenticationToken {

    private final transient AuthenticatedUser user;
    private final transient Jwt jwt;

    public SoneAuthenticationToken(AuthenticatedUser user, Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.user = user;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return jwt.getTokenValue();
    }

    @Override
    public AuthenticatedUser getPrincipal() {
        return user;
    }
}
