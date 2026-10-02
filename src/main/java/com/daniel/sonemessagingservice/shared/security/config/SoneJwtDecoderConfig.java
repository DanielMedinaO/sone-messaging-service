package com.daniel.sonemessagingservice.shared.security.config;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.converter.RsaKeyConverters;

import com.daniel.sonemessagingservice.shared.security.jwt.SoneAccessTokenValidator;

@Configuration
public class SoneJwtDecoderConfig {

    @Bean
    RSAPublicKey sonePublicKey(@Value("${sone.security.jwt.public-key-location}") Resource location) throws IOException {
        try (InputStream in = location.getInputStream()) {
            return RsaKeyConverters.x509().convert(in);
        }
    }

    @Bean
    JwtDecoder jwtDecoder(RSAPublicKey sonePublicKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(sonePublicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new SoneAccessTokenValidator());
        decoder.setJwtValidator(validator);
        return decoder;
    }
}
