package com.daniel.sonemessagingservice.shared.security.jwt;

import java.util.UUID;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import com.daniel.sonemessagingservice.shared.security.model.AppType;

/** Validates the SONE token contract. userStatus and onboardingStep are intentionally ignored. */
public class SoneAccessTokenValidator implements OAuth2TokenValidator<Jwt> {

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String type = jwt.getClaimAsString(SoneJwtClaims.TYPE);
        if (!SoneJwtClaims.ACCESS.equals(type)) {
            return failure("Token type must be ACCESS");
        }
        if (!isUuid(jwt.getSubject())) {
            return failure("Token subject must be a valid UUID");
        }
        if (!isAppType(jwt.getClaimAsString(SoneJwtClaims.APP_TYPE))) {
            return failure("Token appType must be CUSTOMER or PARTNER");
        }
        String status = jwt.getClaimAsString(SoneJwtClaims.STATUS);
        if (status == null || status.isBlank()) {
            return failure("Token status is required");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static boolean isUuid(String value) {
        if (value == null) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isAppType(String value) {
        if (value == null) {
            return false;
        }
        try {
            AppType.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }
}
