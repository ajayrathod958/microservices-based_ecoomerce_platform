package com.microshop.user.service;

import com.microshop.common.api.PageResponse;
import com.microshop.common.exception.BadRequestException;
import com.microshop.common.exception.ResourceNotFoundException;
import com.microshop.common.exception.UnauthorizedException;
import com.microshop.user.dto.ChangePasswordRequest;
import com.microshop.user.dto.UpdateProfileRequest;
import com.microshop.user.dto.UserResponse;
import com.microshop.user.entity.Role;
import com.microshop.user.entity.User;
import com.microshop.user.repository.RefreshTokenRepository;
import com.microshop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return UserMapper.toResponse(find(id));
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateProfileRequest req) {
        User user = find(id);
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setPhone(req.phone());
        return UserMapper.toResponse(user);
    }

    /** Changing the password signs the user out everywhere by revoking all refresh tokens. */
    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest req) {
        User user = find(id);
        if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        user.setPasswordHash(encoder.encode(req.newPassword()));
        refreshTokens.revokeAllForUser(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.from(users.findAll(pageable).map(UserMapper::toResponse));
    }

    @Transactional
    public UserResponse updateRoles(UUID targetId, Set<Role> roles, UUID actingAdminId) {
        if (targetId.equals(actingAdminId) && !roles.contains(Role.ADMIN)) {
            throw new BadRequestException("You cannot remove your own ADMIN role");
        }
        User user = find(targetId);
        user.getRoles().clear();
        user.getRoles().addAll(roles);
        return UserMapper.toResponse(user);
    }

    private User find(UUID id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
