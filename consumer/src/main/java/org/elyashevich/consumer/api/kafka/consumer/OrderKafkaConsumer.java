package org.elyashevich.consumer.api.kafka.consumer;

import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.elyashevich.consumer.api.dto.order.OrderEvent;
import org.elyashevich.consumer.api.mapper.OrderMapper;
import org.elyashevich.consumer.domain.entity.Category;
import org.elyashevich.consumer.metrics.GrafanaKafkaConsumerMetrics;
import org.elyashevich.consumer.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderKafkaConsumer {

    private final GrafanaKafkaConsumerMetrics metrics;
    private static final Random RANDOM = new Random();

    @Value("${kafka.consumer.simulate.processing.delay:true}")
    private boolean simulateProcessingDelay;

    @Value("${kafka.consumer.max.processing.delay.ms:100}")
    private int maxProcessingDelayMs;

    private final OrderService orderService;
    private static final OrderMapper orderMapper = OrderMapper.INSTANCE;
    private final ExecutorService processingExecutor = Executors.newFixedThreadPool(4);
    private final Map<String, BlockingQueue<OrderEvent>> orderQueues = new ConcurrentHashMap<>();

    @KafkaListener(topics = "orders", concurrency = "4", groupId = "order-group")
    public void consumeOrder(ConsumerRecord<String, OrderEvent> orderRecord) {
        Timer.Sample timer = metrics.startTimer();
        try {
            var event = orderRecord.value();

            simulateProcessingDelay();

            orderQueues.computeIfAbsent(
                    event.getOrder().getOrderId().toString(),
                    k -> new LinkedBlockingQueue<>()
            ).put(event);

            processingExecutor.submit(() -> processEventsForOrder(event.getOrder().getOrderId().toString()));

            metrics.recordSuccess(timer, orderRecord.topic(), orderRecord.serializedValueSize());
            log.info("Successfully queued order event: {}", event.getEventId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Processing interrupted for order event", e);
        } catch (Exception e) {
            log.error("Failed to process order event", e);
            throw e;
        }
    }

    private void processEventsForOrder(String orderId) {
        try {
            var queue = orderQueues.get(orderId);
            if (queue == null) return;

            while (!queue.isEmpty()) {
                var event = queue.poll();
                if (event != null) {
                    processSingleEvent(event);
                }
            }
        } finally {
            orderQueues.remove(orderId);
        }
    }

    private void processSingleEvent(OrderEvent event) {
        try {
            var order = orderMapper.toEntity(event.getOrder());
            order.setCategory(Category.builder().name(event.getOrder().getCategoryName()).build());

            switch (event.getEventType()) {
                case ORDER_CREATED -> orderService.create(order);
                case ORDER_UPDATED -> {
                    order.setId(event.getOrder().getOrderId());
                    orderService.update(order);
                }
                case ORDER_CANCELLED -> {
                    order.setId(event.getOrder().getOrderId());
                    orderService.cancel(order);
                }
                default -> log.warn("Unknown event type: {}", event.getEventType());
            }

            log.info("Successfully processed order event: {}", event.getEventId());
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock conflict for order {} (event {}). Retrying...",
                    event.getOrder().getOrderId(), event.getEventId());
            throw e;
        } catch (Exception e) {
            log.error("Failed to process order event: {}", event.getEventId(), e);
            throw e;
        }
    }

    private void simulateProcessingDelay() throws InterruptedException {
        if (simulateProcessingDelay) {
            int delay = RANDOM.nextInt(maxProcessingDelayMs + 1);
            Thread.sleep(delay);
        }
    }

    @PreDestroy
    public void shutdown() {
        processingExecutor.shutdownNow();
    }
}