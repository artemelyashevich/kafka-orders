package org.elyashevich.producer;

import org.elyashevich.metrics.KafkaMetrics;
import org.elyashevich.model.Order;
import io.micrometer.core.instrument.Timer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaOrderProducer {

    private final KafkaTemplate<String, Order> kafkaTemplate;
    private final KafkaMetrics metrics;

    public void sendOrderToKafka(Order order) {
        Timer.Sample sample = Timer.start();
        try {
            kafkaTemplate.send("orders", order.getId(), order);
            metrics.incrementMessageCount();
        } finally {
            sample.stop(metrics.getProcessingTimer());
        }
        log.info("Order sent to kafka: id={}, productId: {}", order.getId(), order.getProductId());
    }
}