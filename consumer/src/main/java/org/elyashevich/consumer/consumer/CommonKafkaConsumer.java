package org.elyashevich.consumer.consumer;

import org.elyashevich.consumer.metrics.GrafanaKafkaConsumerMetrics;
import org.elyashevich.consumer.model.Order;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommonKafkaConsumer {

    private final GrafanaKafkaConsumerMetrics metrics;
    private static final Random RANDOM = new Random();

    @Value("${kafka.consumer.simulate.processing.delay:true}")
    private boolean simulateProcessingDelay;

    @Value("${kafka.consumer.max.processing.delay.ms:100}")
    private int maxProcessingDelayMs;

    @KafkaListener(topics = "orders", concurrency = "4", groupId = "test-group")
    public void consumeOrder(ConsumerRecord<String, Order> record) throws InterruptedException {
        Timer.Sample timer = metrics.startTimer();
        simulateProcessingDelay();
        log.info(
                "Received order: order={}, key={}, partition={}",
                record.value(),
                record.key(),
                record.partition()
        );

        metrics.recordSuccess(timer, record.topic(), record.serializedValueSize());
    }

    @KafkaListener(topics = "messages", concurrency = "4", groupId = "test-group")
    public void consumeMessages(ConsumerRecord<String, String> record) throws InterruptedException {
        Timer.Sample timer = metrics.startTimer();
        simulateProcessingDelay();

        log.info(
                "Received message: message={}, key={}, partition={}",
                record.value(),
                record.key(),
                record.partition()
        );
        metrics.recordSuccess(timer, record.topic(), record.serializedValueSize());
    }

    private void simulateProcessingDelay() throws InterruptedException {
        if (simulateProcessingDelay) {
            int delay = RANDOM.nextInt(maxProcessingDelayMs + 1) * 5;
            Thread.sleep(delay);
        }
    }
}