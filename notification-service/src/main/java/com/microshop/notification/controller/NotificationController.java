package com.microshop.notification.controller;

import com.microshop.common.api.ApiResponse;
import com.microshop.common.api.PageResponse;
import com.microshop.common.security.AuthenticatedUser;
import com.microshop.notification.dto.NotificationResponse;
import com.microshop.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** Every endpoint is scoped to the authenticated user; there is no way to read or change another user's data. */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    @Operation(summary = "My notifications, newest first")
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(service.list(user.userId(), unreadOnly, pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Number of unread notifications (for the navbar badge)")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(Map.of("unread", service.unreadCount(user.userId())));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public ApiResponse<Void> markRead(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        service.markRead(id, user.userId());
        return ApiResponse.message("Marked as read");
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all my notifications as read")
    public ApiResponse<Map<String, Integer>> markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok("All marked as read", Map.of("updated", service.markAllRead(user.userId())));
    }
}
