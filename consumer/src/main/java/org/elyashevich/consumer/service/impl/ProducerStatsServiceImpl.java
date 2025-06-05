package org.elyashevich.consumer.service.impl;

import lombok.RequiredArgsConstructor;
import org.elyashevich.consumer.domain.entity.ProducerStats;
import org.elyashevich.consumer.repository.ProducerStatsRepository;
import org.elyashevich.consumer.service.ProducerStatsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ProducerStatsServiceImpl implements ProducerStatsService {
    private final ProducerStatsRepository statsRepository;

    private final ConcurrentMap<String, Object> keyLocks = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void recordProducerCall(String producerId, String topicName) {
        String lockKey = producerId + "|" + topicName;

        Object lock = keyLocks.compute(lockKey, (k, v) -> v == null ? new Object() : v);

        synchronized (lock) {
            var now = LocalDateTime.now();
            statsRepository.findByProducerIdAndTopicName(producerId, topicName)
                    .ifPresentOrElse(
                            stats -> statsRepository.incrementCallCount(producerId, topicName, now),
                            () -> createNewStats(producerId, topicName, now)
                    );
        }

        if (ThreadLocalRandom.current().nextInt(100) == 0) {
            keyLocks.entrySet().removeIf(entry -> ThreadLocalRandom.current().nextInt(10) == 0);
        }
    }

    @Override
    public List<ProducerStats> findAll() {
        return statsRepository.findAll();
    }

    private void createNewStats(String producerId, String topicName, LocalDateTime now) {
        ProducerStats stats = new ProducerStats();
        stats.setProducerId(producerId);
        stats.setTopicName(topicName);
        stats.setCallCount(1);
        stats.setLastCallTime(now);
        statsRepository.save(stats);
    }
}