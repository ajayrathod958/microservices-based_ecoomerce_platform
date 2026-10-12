package com.microshop.notification.dto;

import com.microshop.notification.entity.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, String type, String title, String message, UUID orderId,
                                   boolean read, Instant createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getOrderId(),
                n.isRead(), n.getCreatedAt());
    }
}
