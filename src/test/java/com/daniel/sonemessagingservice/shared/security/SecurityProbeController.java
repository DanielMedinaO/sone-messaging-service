package com.daniel.sonemessagingservice.shared.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daniel.sonemessagingservice.shared.security.model.AuthenticatedUser;

@RestController
class SecurityProbeController {

    @GetMapping("/api/v1/test/me")
    AuthenticatedUser me(@AuthenticationPrincipal AuthenticatedUser user) {
        return user;
    }
}
