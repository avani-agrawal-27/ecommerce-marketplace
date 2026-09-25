package com.ecommerce.marketplace.auth.dto;

import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Set<String> roles
) {
}
