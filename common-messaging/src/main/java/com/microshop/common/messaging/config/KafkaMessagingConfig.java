package com.microshop.common.messaging.config;

import com.microshop.common.messaging.UnparseableEventException;
import com.microshop.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableScheduling
public class KafkaMessagingConfig {

    /**
     * Failed records are retried with exponential backoff (default 1s, 2s, 4s), then published to "<topic>.DLT" and
     * skipped so one poison message cannot block the partition. Malformed messages go straight to the DLT.
     * Spring Boot wires any CommonErrorHandler bean into its default listener container factory.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, String> template,
            @Value("${app.kafka.retry.max-attempts:3}") int maxAttempts,
            @Value("${app.kafka.retry.initial-interval-ms:1000}") long initialIntervalMs,
            @Value("${app.kafka.retry.multiplier:2.0}") double multiplier) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(Topics.dlt(record.topic()), -1));   // -1: let Kafka pick the partition

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxAttempts);
        backOff.setInitialInterval(initialIntervalMs);
        backOff.setMultiplier(multiplier);
        backOff.setMaxInterval(30_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(UnparseableEventException.class);
        return handler;
    }

    /** Idempotent: existing topics are left alone. Every service declares the same set. */
    @Bean
    public KafkaAdmin.NewTopics microshopTopics(@Value("${app.kafka.partitions:3}") int partitions) {
        NewTopic[] topics = Topics.allWithDeadLetters().stream()
                .map(name -> TopicBuilder.name(name).partitions(partitions).replicas(1).build())
                .toArray(NewTopic[]::new);
        return new KafkaAdmin.NewTopics(topics);
    }
}
