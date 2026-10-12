package com.microshop.notification.service;

import com.microshop.common.exception.ResourceNotFoundException;
import com.microshop.notification.entity.EmailStatus;
import com.microshop.notification.entity.Notification;
import com.microshop.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository repo;

    private NotificationService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new NotificationService(repo);
    }

    private Notification owned(UUID owner) {
        return Notification.builder().id(id).userId(owner).type("T").title("t").message("m")
                .emailStatus(EmailStatus.SENT).createdAt(Instant.now()).build();
    }

    @Test
    void listAlwaysSortsNewestFirstWhateverTheClientAsked() {
        when(repo.findByUserId(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(owned(userId))));

        service.list(userId, false, PageRequest.of(2, 5, Sort.by("message")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).findByUserId(any(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(captor.getValue().getSort().getOrderFor("message")).isNull();
    }

    @Test
    void unreadOnlyUsesTheUnreadQuery() {
        when(repo.findByUserIdAndReadFalse(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.list(userId, true, PageRequest.of(0, 20));

        verify(repo).findByUserIdAndReadFalse(any(), any(Pageable.class));
    }

    @Test
    void markReadSucceedsForTheOwnerAndIsRepeatable() {
        when(repo.markRead(id, userId)).thenReturn(1);

        assertThatCode(() -> {
            service.markRead(id, userId);
            service.markRead(id, userId);
        }).doesNotThrowAnyException();
    }

    @Test
    void someoneElsesOrUnknownNotificationIsNotFound() {
        when(repo.markRead(id, userId)).thenReturn(0);

        assertThatThrownBy(() -> service.markRead(id, userId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAllReadReturnsHowManyWereUpdated() {
        when(repo.markAllRead(userId)).thenReturn(4);

        assertThat(service.markAllRead(userId)).isEqualTo(4);
    }
}
