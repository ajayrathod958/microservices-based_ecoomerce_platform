package com.microshop.common.messaging;

import com.microshop.common.messaging.idempotency.IdempotencyGuard;
import com.microshop.common.messaging.idempotency.ProcessedEvent;
import com.microshop.common.messaging.idempotency.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyGuardTest {

    @Mock ProcessedEventRepository processed;

    @Test
    void firstDeliveryIsRecordedPerHandler() {
        IdempotencyGuard guard = new IdempotencyGuard(processed);
        UUID eventId = UUID.randomUUID();
        when(processed.existsById("h1:" + eventId)).thenReturn(false);

        assertThat(guard.firstTime(eventId, "h1")).isTrue();

        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processed).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("h1:" + eventId);
    }

    @Test
    void redeliveryIsReportedAsDuplicateAndNotRecordedAgain() {
        IdempotencyGuard guard = new IdempotencyGuard(processed);
        UUID eventId = UUID.randomUUID();
        when(processed.existsById("h1:" + eventId)).thenReturn(true);

        assertThat(guard.firstTime(eventId, "h1")).isFalse();
        verify(processed, never()).saveAndFlush(any());
    }
}
