package org.elyashevich.producer.producer;

import org.elyashevich.producer.metrics.KafkaMetrics;
import io.micrometer.core.instrument.Timer;
import org.elyashevich.producer.model.OrderEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;


@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaOrderProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final KafkaMetrics metrics;

    public void sendOrderToKafka(OrderEvent order) {
        Timer.Sample sample = Timer.start();
        try {
            kafkaTemplate.send(
                    "orders",
                    String.valueOf(ThreadLocalRandom.current().nextInt(1, 11)),
                    order
            );
            log.info("Sent order event: {}", order.getEventId());
            metrics.incrementMessageCount();
        } finally {
            sample.stop(metrics.getProcessingTimer());
        }
    }
}