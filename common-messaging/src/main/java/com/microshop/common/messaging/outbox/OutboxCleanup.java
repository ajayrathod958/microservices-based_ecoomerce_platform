package com.microshop.common.messaging.outbox;

import com.microshop.common.messaging.idempotency.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/** Keeps the two bookkeeping tables from growing forever. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanup {

    private final OutboxRepository outbox;
    private final ProcessedEventRepository processed;

    @Scheduled(cron = "0 15 * * * *")
    @Transactional
    public void purge() {
        int sent = outbox.purgePublishedBefore(Instant.now().minus(Duration.ofDays(1)));
        int seen = processed.purgeBefore(Instant.now().minus(Duration.ofDays(7)));
        if (sent + seen > 0) {
            log.info("Purged {} published outbox rows and {} processed-event markers", sent, seen);
        }
    }
}
