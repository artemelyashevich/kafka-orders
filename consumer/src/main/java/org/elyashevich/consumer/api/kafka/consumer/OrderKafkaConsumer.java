package org.elyashevich.consumer.api.kafka.consumer;

import io.micrometer.core.instrument.Timer;
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
import org.springframework.stereotype.Service;

import java.util.Random;

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

    @KafkaListener(topics = "orders", concurrency = "4", groupId = "order-group")
    public void consumeOrder(
            ConsumerRecord<String, OrderEvent> orderRecord
    ) throws InterruptedException {
        Timer.Sample timer = metrics.startTimer();
        simulateProcessingDelay();
        var event = orderRecord.value();
        var order = orderMapper.toEntity(event.getOrder());
        order.setCategory(Category.builder().name(event.getOrder().getCategoryName()).build());

        switch (event.getEventType()) {
            case ORDER_CREATED -> this.orderService.create(order);
            case ORDER_UPDATED -> {
                order.setId(event.getOrder().getOrderId());
                this.orderService.update(order);
            }
            case ORDER_CANCELLED -> {
                order.setId(event.getOrder().getOrderId());
                this.orderService.cancel(order);
            }
            default -> log.warn("Unknown event type: {}", event.getEventType());
        }

        log.info("Successfully processed order event: {}", event.getEventId());

        metrics.recordSuccess(timer, orderRecord.topic(), orderRecord.serializedValueSize());
    }

    private void simulateProcessingDelay() throws InterruptedException {
        if (simulateProcessingDelay) {
            int delay = RANDOM.nextInt(maxProcessingDelayMs + 1) * 5;
            Thread.sleep(delay);
        }
    }
}