package com.microshop.common.messaging;

import com.microshop.common.messaging.outbox.OutboxEvent;
import com.microshop.common.messaging.outbox.OutboxRelay;
import com.microshop.common.messaging.outbox.OutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock OutboxRepository outbox;
    @Mock KafkaTemplate<String, String> kafka;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(outbox, kafka);
    }

    private static OutboxEvent event(String topic) {
        return OutboxEvent.builder().id(UUID.randomUUID()).topic(topic).messageKey("order-1")
                .eventType("TestEvent").payload("{}").createdAt(Instant.now()).build();
    }

    private static CompletableFuture<SendResult<String, String>> ok() {
        return CompletableFuture.completedFuture(null);
    }

    private static CompletableFuture<SendResult<String, String>> failed() {
        return CompletableFuture.failedFuture(new RuntimeException("broker down"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsInOrderWithHeadersAndMarksPublished() {
        OutboxEvent e1 = event("order-created");
        OutboxEvent e2 = event("order-confirmed");
        when(outbox.findTop100ByPublishedAtIsNullOrderBySeqAsc()).thenReturn(List.of(e1, e2));
        when(kafka.send(any(ProducerRecord.class))).thenReturn(ok());

        relay.relay();

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafka, times(2)).send(captor.capture());
        assertThat(captor.getAllValues()).extracting(ProducerRecord::topic)
                .containsExactly("order-created", "order-confirmed");
        ProducerRecord<String, String> first = captor.getAllValues().get(0);
        assertThat(first.key()).isEqualTo("order-1");
        assertThat(new String(first.headers().lastHeader("eventId").value(), StandardCharsets.UTF_8))
                .isEqualTo(e1.getId().toString());
        assertThat(e1.getPublishedAt()).isNotNull();
        assertThat(e2.getPublishedAt()).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void stopsAtFirstFailureSoLaterEventsNeverOvertakeEarlierOnes() {
        OutboxEvent e1 = event("t1");
        OutboxEvent e2 = event("t2");
        OutboxEvent e3 = event("t3");
        when(outbox.findTop100ByPublishedAtIsNullOrderBySeqAsc()).thenReturn(List.of(e1, e2, e3));
        when(kafka.send(any(ProducerRecord.class))).thenReturn(ok()).thenReturn(failed());

        relay.relay();

        assertThat(e1.getPublishedAt()).isNotNull();
        assertThat(e2.getPublishedAt()).isNull();     // stays pending, retried on the next poll
        assertThat(e3.getPublishedAt()).isNull();     // never attempted
        verify(kafka, times(2)).send(any(ProducerRecord.class));
    }
}
