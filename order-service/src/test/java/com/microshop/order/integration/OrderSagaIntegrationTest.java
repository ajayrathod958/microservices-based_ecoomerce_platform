package com.microshop.order.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microshop.events.InventoryFailedEvent;
import com.microshop.events.InventoryReservedEvent;
import com.microshop.events.PaymentCompletedEvent;
import com.microshop.events.StockShortage;
import com.microshop.events.Topics;
import com.microshop.order.client.CartClient;
import com.microshop.order.client.CartItemSnapshot;
import com.microshop.order.client.CartSnapshot;
import com.microshop.order.client.ProductClient;
import com.microshop.order.client.ProductInfo;
import com.microshop.order.entity.OrderStatus;
import com.microshop.order.repository.CustomerOrderRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs order-service against a REAL PostgreSQL and a REAL Kafka (Testcontainers). The other services are played by
 * the test itself: it publishes the events inventory-service / payment-service would publish.
 * Skipped automatically when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "security.internal-token=test-internal-token",
        "app.outbox.poll-interval-ms=200",
        "app.kafka.retry.initial-interval-ms=100",
        "app.kafka.retry.max-attempts=2"
})
@AutoConfigureMockMvc
class OrderSagaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    /** Records everything published to the topics we assert on. */
    @TestConfiguration
    static class ObserverConfig {
        @Bean
        EventObserver eventObserver() {
            return new EventObserver();
        }
    }

    static class EventObserver {
        private final Map<String, List<String>> byTopic = new ConcurrentHashMap<>();

        @KafkaListener(groupId = "it-observer", topics = {
                Topics.ORDER_CREATED, Topics.ORDER_CONFIRMED, Topics.ORDER_CANCELLED,
                Topics.INVENTORY_RESERVED + ".DLT"})
        void on(ConsumerRecord<String, String> record) {
            byTopic.computeIfAbsent(record.topic(), t -> new CopyOnWriteArrayList<>()).add(record.value());
        }

        long countFor(String topic, String needle) {
            return byTopic.getOrDefault(topic, List.of()).stream().filter(v -> v.contains(needle)).count();
        }
    }

    @MockBean CartClient cartClient;
    @MockBean ProductClient productClient;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @Autowired CustomerOrderRepository orders;
    @Autowired EventObserver observer;

    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder) {
        return builder.header("X-User-Id", userId.toString())
                .header("X-User-Email", "jane@example.com")
                .header("X-User-Roles", "USER")
                .header("X-Internal-Token", "test-internal-token");
    }

    private UUID placeOrder() throws Exception {
        when(cartClient.fetchCart()).thenReturn(new CartSnapshot(List.of(new CartItemSnapshot(productId, 2))));
        when(productClient.fetch(productId)).thenReturn(new ProductInfo(productId, "Phone", new BigDecimal("10.00")));

        String body = mvc.perform(asUser(post("/api/orders"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress":{"fullName":"Jane Doe","line1":"1 Main St","city":"Pune",
                                 "postalCode":"411001","country":"IN"},"paymentToken":"tok_success"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(20.00))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(body);
        return UUID.fromString(json.at("/data/id").asText());
    }

    private void send(String topic, UUID orderId, Object event) throws Exception {
        kafkaTemplate.send(topic, orderId.toString(), mapper.writeValueAsString(event)).get(10, TimeUnit.SECONDS);
    }

    private InventoryReservedEvent reserved(UUID orderId) {
        return new InventoryReservedEvent(UUID.randomUUID(), Instant.now(), orderId, userId, "jane@example.com",
                new BigDecimal("20.00"), "USD", "tok_success");
    }

    private PaymentCompletedEvent paid(UUID orderId) {
        return new PaymentCompletedEvent(UUID.randomUUID(), Instant.now(), orderId, userId, "jane@example.com",
                UUID.randomUUID(), new BigDecimal("20.00"), "USD", "mock_123");
    }

    private OrderStatus statusOf(UUID orderId) {
        return orders.findById(orderId).orElseThrow().getStatus();
    }

    @Test
    void checkoutSavesPendingOrderAndPublishesOrderCreatedViaTheOutbox() throws Exception {
        UUID orderId = placeOrder();

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(observer.countFor(Topics.ORDER_CREATED, orderId.toString())).isEqualTo(1));
        assertThat(statusOf(orderId)).isEqualTo(OrderStatus.PENDING);
        verify(cartClient).clear();
    }

    @Test
    void happyPathConfirmsTheOrderAndAnnouncesIt() throws Exception {
        UUID orderId = placeOrder();

        send(Topics.INVENTORY_RESERVED, orderId, reserved(orderId));
        send(Topics.PAYMENT_COMPLETED, orderId, paid(orderId));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(statusOf(orderId)).isEqualTo(OrderStatus.CONFIRMED));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(observer.countFor(Topics.ORDER_CONFIRMED, orderId.toString())).isEqualTo(1));

        mvc.perform(asUser(get("/api/orders/" + orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.history.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void redeliveredPaymentEventIsAppliedExactlyOnce() throws Exception {
        UUID orderId = placeOrder();
        PaymentCompletedEvent event = paid(orderId);

        send(Topics.PAYMENT_COMPLETED, orderId, event);
        send(Topics.PAYMENT_COMPLETED, orderId, event);   // same eventId: a redelivery

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(statusOf(orderId)).isEqualTo(OrderStatus.CONFIRMED));
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(observer.countFor(Topics.ORDER_CONFIRMED, orderId.toString())).isEqualTo(1));
    }

    @Test
    void inventoryFailureCancelsTheOrderAndPublishesOrderCancelled() throws Exception {
        UUID orderId = placeOrder();

        send(Topics.INVENTORY_FAILED, orderId, new InventoryFailedEvent(UUID.randomUUID(), Instant.now(), orderId,
                userId, "jane@example.com", List.of(new StockShortage(productId, 2, 0)), "Insufficient stock for 1 item(s)"));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(statusOf(orderId)).isEqualTo(OrderStatus.CANCELLED));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(observer.countFor(Topics.ORDER_CANCELLED, orderId.toString())).isEqualTo(1));
        mvc.perform(asUser(get("/api/orders/" + orderId)))
                .andExpect(jsonPath("$.data.failureReason").value("Insufficient stock for 1 item(s)"));
    }

    @Test
    void anotherUsersOrderIsNotFound() throws Exception {
        UUID orderId = placeOrder();

        mvc.perform(get("/api/orders/" + orderId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Email", "eve@example.com")
                        .header("X-User-Roles", "USER")
                        .header("X-Internal-Token", "test-internal-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void malformedMessageIsDeadLetteredInsteadOfBlockingTheTopic() throws Exception {
        String poison = "this-is-not-json-" + UUID.randomUUID();

        kafkaTemplate.send(Topics.INVENTORY_RESERVED, "poison-key", poison).get(10, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(observer.countFor(Topics.INVENTORY_RESERVED + ".DLT", poison)).isEqualTo(1));
    }
}
