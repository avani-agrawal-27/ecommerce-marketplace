package com.ecommerce.marketplace.user.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @GetMapping("/access")
    public Map<String, Object> adminAccess(
            Authentication authentication
    ) {

        return Map.of(
                "message", "Admin access granted",
                "email", authentication.getName(),
                "role", "ADMIN"
        );
    }
}