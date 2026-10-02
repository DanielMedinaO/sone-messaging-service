package com.daniel.sonemessagingservice.shared.security.jwt;

import java.util.List;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.daniel.sonemessagingservice.shared.security.model.AppType;
import com.daniel.sonemessagingservice.shared.security.model.AuthenticatedUser;

/** Maps an already validated JWT to an AuthenticatedUser principal and authorities. */
public class SoneJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String STATUS_ACTIVE = "STATUS_ACTIVE";
    public static final String ACTIVE = "ACTIVE";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AuthenticatedUser user = new AuthenticatedUser(
                UUID.fromString(jwt.getSubject()),
                AppType.valueOf(jwt.getClaimAsString(SoneJwtClaims.APP_TYPE)),
                jwt.getClaimAsString(SoneJwtClaims.STATUS));

        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("APP_" + user.appType().name()));
        if (ACTIVE.equals(user.status())) {
            authorities.add(new SimpleGrantedAuthority(STATUS_ACTIVE));
        }
        return new SoneAuthenticationToken(user, jwt, authorities);
    }
}
