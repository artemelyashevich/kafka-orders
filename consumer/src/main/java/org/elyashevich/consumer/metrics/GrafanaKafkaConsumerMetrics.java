package org.elyashevich.consumer.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GrafanaKafkaConsumerMetrics {
    private final MeterRegistry registry;
    private final Map<String, Timer> successTimers = new ConcurrentHashMap<>();

    private final Timer globalProcessingTimer;
    private final Counter totalMessagesCounter;
    private final DistributionSummary messageSizeSummary;

    public GrafanaKafkaConsumerMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.globalProcessingTimer = Timer.builder("kafka.consumer.processing.time.global")
                .description("Total message processing time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        this.totalMessagesCounter = Counter.builder("kafka.consumer.messages.total")
                .description("Total consumed messages count")
                .register(registry);

        this.messageSizeSummary = DistributionSummary.builder("kafka.consumer.message.size")
                .description("Message size distribution")
                .baseUnit("bytes")
                .register(registry);
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void recordSuccess(Timer.Sample sample, String topic, long messageSize) {
        if (sample == null) {
            return;
        }

        sample.stop(getSuccessTimer(topic));
        sample.stop(globalProcessingTimer);

        totalMessagesCounter.increment();
        messageSizeSummary.record(messageSize);
    }

    private Timer getSuccessTimer(String topic) {
        return successTimers.computeIfAbsent(topic, t ->
                Timer.builder("kafka.consumer.processing.time.success")
                        .tags("topic", t, "status", "success")
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .register(registry));
    }
}