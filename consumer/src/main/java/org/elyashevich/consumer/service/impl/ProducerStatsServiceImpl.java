package org.elyashevich.consumer.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProducerStatsServiceImpl implements ProducerStatsService {
    private final ProducerStatsRepository statsRepository;

    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void recordProducerCall(String producerId, String topicName) {
        var lockKey = producerId + "|" + topicName;

        var lock = locks.computeIfAbsent(lockKey, k -> new ReentrantLock());

        try {
            if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    processStatsUpdate(producerId, topicName);
                } finally {
                    lock.unlock();
                }
            } else {
                handleLockTimeout(producerId, topicName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    @Override
    public List<ProducerStats> findAll() {
        return statsRepository.findAll();
    }

    private void processStatsUpdate(String producerId, String topicName) {
        var now = LocalDateTime.now();
        statsRepository.findByProducerIdAndTopicName(producerId, topicName)
                .ifPresentOrElse(
                        stats -> statsRepository.incrementCallCount(producerId, topicName, now),
                        () -> createNewStats(producerId, topicName, now)
                );

        cleanUpOldLocks();
    }

    private void handleLockTimeout(String producerId, String topicName) {
        log.warn("Could not acquire lock for producerId: {}, topic: {}", producerId, topicName);
    }

    private void cleanUpOldLocks() {
        if (ThreadLocalRandom.current().nextInt(100) == 0) {
            locks.entrySet().removeIf(entry ->
                    ThreadLocalRandom.current().nextInt(10) == 0 &&
                            !entry.getValue().isLocked()
            );
        }
    }

    private void createNewStats(String producerId, String topicName, LocalDateTime now) {
        var stats = new ProducerStats();
        stats.setProducerId(producerId);
        stats.setTopicName(topicName);
        stats.setCallCount(1);
        stats.setLastCallTime(now);
        statsRepository.save(stats);
    }
}