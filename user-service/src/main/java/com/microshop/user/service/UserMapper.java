package com.microshop.user.service;

import com.microshop.user.dto.UserResponse;
import com.microshop.user.entity.User;

import java.util.TreeSet;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getPhone(),
                u.isEnabled(), new TreeSet<>(u.getRoles()), u.getCreatedAt());
    }
}
