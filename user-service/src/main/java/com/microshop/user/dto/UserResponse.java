package com.microshop.user.dto;

import com.microshop.user.entity.Role;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(UUID id, String email, String firstName, String lastName, String phone,
                           boolean enabled, Set<Role> roles, Instant createdAt) {
}
