package org.elyashevich.producer.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

@Component
public class KafkaMetrics {
    private final Counter kafkaMessageCounter;
    private final Timer kafkaProcessingTimer;

    public KafkaMetrics(MeterRegistry registry) {
        this.kafkaMessageCounter = Counter.builder("kafka.messages.total")
            .description("Total received Kafka messages")
            .tag("type", "consumer")
            .register(registry);

        this.kafkaProcessingTimer = Timer.builder("kafka.processing.time")
            .description("Time spent processing Kafka messages")
            .publishPercentiles(0.5, 0.95)
            .register(registry);
    }

    public void incrementMessageCount() {
        kafkaMessageCounter.increment();
    }

    public Timer getProcessingTimer() {
        return kafkaProcessingTimer;
    }
}