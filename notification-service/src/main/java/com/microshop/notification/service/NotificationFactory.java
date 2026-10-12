package com.microshop.notification.service;

import com.microshop.notification.entity.EmailStatus;
import com.microshop.notification.entity.Notification;
import com.microshop.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/** Creates the in-app record; the email (if there is an address) is queued as PENDING for the dispatcher. */
@Component
@RequiredArgsConstructor
public class NotificationFactory {

    private final NotificationRepository notifications;

    public Notification create(UUID userId, String email, String type, String title, String message, UUID orderId) {
        boolean hasEmail = email != null && !email.isBlank() && !"null".equalsIgnoreCase(email);
        return notifications.save(Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .email(hasEmail ? email.trim() : null)
                .type(abbreviate(type, 40))
                .title(abbreviate(title, 150))
                .message(abbreviate(message, 500))
                .orderId(orderId)
                .emailStatus(hasEmail ? EmailStatus.PENDING : EmailStatus.SKIPPED)
                .createdAt(Instant.now())
                .build());
    }

    static String abbreviate(String s, int max) {
        if (s == null || s.isBlank()) {
            return "-";
        }
        String clean = s.replaceAll("[\\r\\n]+", " ").trim();   // no header injection via the subject line
        return clean.length() <= max ? clean : clean.substring(0, max - 1) + "…";
    }
}
