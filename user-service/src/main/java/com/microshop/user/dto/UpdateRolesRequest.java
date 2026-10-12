package com.microshop.user.dto;

import com.microshop.user.entity.Role;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UpdateRolesRequest(@NotEmpty Set<Role> roles) {
}
