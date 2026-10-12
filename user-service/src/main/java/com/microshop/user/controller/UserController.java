package com.microshop.user.controller;

import com.microshop.common.api.ApiResponse;
import com.microshop.common.api.PageResponse;
import com.microshop.common.security.AuthenticatedUser;
import com.microshop.user.dto.ChangePasswordRequest;
import com.microshop.user.dto.UpdateProfileRequest;
import com.microshop.user.dto.UpdateRolesRequest;
import com.microshop.user.dto.UserResponse;
import com.microshop.user.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(userService.getById(principal.userId()));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserResponse> updateMe(@AuthenticationPrincipal AuthenticatedUser principal,
                                              @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok("Profile updated", userService.updateProfile(principal.userId(), request));
    }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.userId(), request);
        return ApiResponse.message("Password changed. Please log in again on your other devices.");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(userService.list(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(userService.getById(id));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateRoles(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @PathVariable UUID id,
                                                 @Valid @RequestBody UpdateRolesRequest request) {
        return ApiResponse.ok("Roles updated", userService.updateRoles(id, request.roles(), principal.userId()));
    }
}
