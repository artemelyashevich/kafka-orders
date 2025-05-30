package org.elyashevich.consumer.api.kafka.consumer;

import org.elyashevich.consumer.api.dto.order.OrderEvent;
import org.elyashevich.consumer.api.mapper.OrderMapper;
import org.elyashevich.consumer.domain.entity.Category;
import org.elyashevich.consumer.metrics.GrafanaKafkaConsumerMetrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.elyashevich.consumer.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
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
    private final OrderMapper orderMapper;

    @KafkaListener(topics = "orders", concurrency = "4", groupId = "order-group")
    public void consumeOrder(
            @Payload OrderEvent orderEvent,
            ConsumerRecord<String, OrderEvent> record,
            Acknowledgment acknowledgment
    ) throws InterruptedException {
        Timer.Sample timer = metrics.startTimer();
        simulateProcessingDelay();

        log.info("Received Order Event: key={}, partition={}, offset={}, event={}",
                record.key(), record.partition(), record.offset(), orderEvent);

        var order = this.orderMapper.toEntity(orderEvent.getOrder());
        order.setCategory(Category.builder().name(orderEvent.getOrder().getCategoryName()).build());

        switch (orderEvent.getEventType()) {
            case ORDER_CREATED -> this.orderService.create(order);
            case ORDER_UPDATED -> {
                order.setId(orderEvent.getOrder().getOrderId());
                this.orderService.update(order);
            }
            case ORDER_CANCELLED -> {
                order.setId(orderEvent.getOrder().getOrderId());
                this.orderService.cancel(order);
            }
            default -> log.warn("Unknown event type: {}", orderEvent.getEventType());
        }

        acknowledgment.acknowledge();
        log.info("Successfully processed order event: {}", orderEvent.getEventId());

        metrics.recordSuccess(timer, record.topic(), record.serializedValueSize());
    }

    private void simulateProcessingDelay() throws InterruptedException {
        if (simulateProcessingDelay) {
            int delay = RANDOM.nextInt(maxProcessingDelayMs + 1) * 5;
            Thread.sleep(delay);
        }
    }
}