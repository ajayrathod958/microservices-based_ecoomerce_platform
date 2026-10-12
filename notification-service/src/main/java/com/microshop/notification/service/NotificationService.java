package com.microshop.notification.service;

import com.microshop.common.api.PageResponse;
import com.microshop.common.exception.ResourceNotFoundException;
import com.microshop.notification.dto.NotificationResponse;
import com.microshop.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifications;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, boolean unreadOnly, Pageable requested) {
        // newest first, always: the client's sort parameter is deliberately ignored
        Pageable pageable = PageRequest.of(requested.getPageNumber(), requested.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        var page = unreadOnly
                ? notifications.findByUserIdAndReadFalse(userId, pageable)
                : notifications.findByUserId(userId, pageable);
        return PageResponse.from(page.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notifications.countByUserIdAndReadFalse(userId);
    }

    /**
     * The UPDATE's WHERE clause includes the owner, and it matches even when the row is already read, so
     * 0 rows means "missing or not yours": a 404 either way (no existence leak). Repeating the call is harmless.
     */
    @Transactional
    public void markRead(UUID id, UUID userId) {
        if (notifications.markRead(id, userId) == 0) {
            throw new ResourceNotFoundException("Notification not found");
        }
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return notifications.markAllRead(userId);
    }
}
