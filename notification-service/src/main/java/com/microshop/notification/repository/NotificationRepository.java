package com.microshop.notification.repository;

import com.microshop.notification.entity.EmailStatus;
import com.microshop.notification.entity.Notification;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadFalse(UUID userId, Pageable pageable);

    long countByUserIdAndReadFalse(UUID userId);

    /** FOR UPDATE SKIP LOCKED: several instances can dispatch emails without sending the same one twice. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<Notification> findTop50ByEmailStatusOrderByCreatedAtAsc(EmailStatus status);

    /** Ownership is part of the WHERE clause, so a user can never touch someone else's notification. */
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.read = true where n.id = :id and n.userId = :userId")
    int markRead(@Param("id") UUID id, @Param("userId") UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.read = true where n.userId = :userId and n.read = false")
    int markAllRead(@Param("userId") UUID userId);
}
