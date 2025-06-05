package org.elyashevich.consumer.api.kafka.consumer;

import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.elyashevich.consumer.api.dto.order.OrderEvent;
import org.elyashevich.consumer.api.mapper.OrderMapper;
import org.elyashevich.consumer.domain.entity.Category;
import org.elyashevich.consumer.exception.BusinessException;
import org.elyashevich.consumer.metrics.GrafanaKafkaConsumerMetrics;
import org.elyashevich.consumer.service.OrderService;
import org.elyashevich.consumer.service.ProducerStatsService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderKafkaConsumer {
    private static final OrderMapper orderMapper = OrderMapper.INSTANCE;
    private static final int PROCESSING_THREADS = 4;
    private static final int MAX_QUEUE_SIZE = 1000;

    private final GrafanaKafkaConsumerMetrics metrics;
    private final OrderService orderService;
    private final ProducerStatsService producerStatsService;

    private final ExecutorService processingExecutor = Executors.newFixedThreadPool(PROCESSING_THREADS);
    private final Map<Long, Queue<OrderEvent>> orderQueues = new ConcurrentHashMap<>();

    @KafkaListener(topics = "orders", concurrency = "4", groupId = "order-group")
    public void consumeOrder(ConsumerRecord<String, OrderEvent> orderRecord) {
        Timer.Sample timer = metrics.startTimer();
        try {

            producerStatsService.recordProducerCall(orderRecord.key(), orderRecord.topic());

            var event = orderRecord.value();
            var orderId = event.getOrder().getOrderId();

            if (!offerEventToQueue(orderId, event)) {
                throw new BusinessException("Order queue overflow for order: " + orderId);
            }

            processingExecutor.submit(() -> processOrderEvents(orderId));

            metrics.recordSuccess(timer, orderRecord.topic(), orderRecord.serializedValueSize());
            log.debug("Queued order event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to process order event", e);
        }
    }

    private boolean offerEventToQueue(long orderId, OrderEvent event) {
        var queue = orderQueues.computeIfAbsent(
                orderId,
                k -> new LinkedBlockingQueue<>(MAX_QUEUE_SIZE)
        );
        return queue.offer(event);
    }

    private void processOrderEvents(long orderId) {
        try {
            var queue = orderQueues.get(orderId);
            if (queue == null) return;

            OrderEvent event;
            while ((event = queue.poll()) != null) {
                processSingleEventWithRetry(event);
            }
        } finally {
            orderQueues.remove(orderId);
        }
    }

    private void processSingleEventWithRetry(OrderEvent event) {
        var attempt = 0;
        while (attempt < 3) {
            try {
                processSingleEvent(event);
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= 3) {
                    log.error("Failed to process order {} after {} attempts (event {})",
                            event.getOrder().getOrderId(), 3, event.getEventId());
                    throw e;
                }
                waitBeforeRetry(attempt);
            }
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

            log.debug("Processed order event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Error processing order event: {}", event.getEventId(), e);
            throw e;
        }
    }

    private void waitBeforeRetry(int attempt) {
        try {
            var delay = (long) Math.pow(2, attempt) * 100;
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void shutdown() {
        processingExecutor.shutdown();
        try {
            if (!processingExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                processingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            processingExecutor.shutdownNow();
        }
    }
}