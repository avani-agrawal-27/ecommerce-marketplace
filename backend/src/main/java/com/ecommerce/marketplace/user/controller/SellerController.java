package com.ecommerce.marketplace.user.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/seller")
public class SellerController {

    @GetMapping("/access")
    public Map<String, Object> sellerAccess(
            Authentication authentication
    ) {

        return Map.of(
                "message", "Seller access granted",
                "email", authentication.getName(),
                "role", "SELLER"
        );
    }
}