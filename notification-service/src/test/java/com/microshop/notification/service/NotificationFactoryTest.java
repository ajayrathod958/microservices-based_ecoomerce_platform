package com.microshop.notification.service;

import com.microshop.notification.entity.EmailStatus;
import com.microshop.notification.entity.Notification;
import com.microshop.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationFactoryTest {

    @Mock NotificationRepository repo;

    private Notification create(String email, String title, String message) {
        when(repo.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        return new NotificationFactory(repo).create(UUID.randomUUID(), email, "T", title, message, null);
    }

    @Test
    void withAnAddressTheEmailIsQueuedAsPending() {
        Notification n = create(" a@b.com ", "Hi", "Body");

        assertThat(n.getEmailStatus()).isEqualTo(EmailStatus.PENDING);
        assertThat(n.getEmail()).isEqualTo("a@b.com");
        assertThat(n.isRead()).isFalse();
    }

    @Test
    void withoutAnAddressTheEmailIsSkippedButTheInAppNotificationStillExists() {
        assertThat(create(null, "Hi", "Body").getEmailStatus()).isEqualTo(EmailStatus.SKIPPED);
        assertThat(create("  ", "Hi", "Body").getEmailStatus()).isEqualTo(EmailStatus.SKIPPED);
        assertThat(create("null", "Hi", "Body").getEmailStatus()).isEqualTo(EmailStatus.SKIPPED);
    }

    @Test
    void titleHasNoLineBreaksAndOverlongTextIsTruncatedToTheColumnSize() {
        Notification n = create("a@b.com", "Line1\r\nBcc: evil@x.com", "x".repeat(900));

        assertThat(n.getTitle()).doesNotContain("\n").doesNotContain("\r");
        assertThat(n.getMessage()).hasSize(500).endsWith("…");
    }
}
