package org.elyashevich.consumer.service.impl;

import lombok.RequiredArgsConstructor;
import org.elyashevich.consumer.domain.entity.ProducerStats;
import org.elyashevich.consumer.repository.ProducerStatsRepository;
import org.elyashevich.consumer.service.ProducerStatsService;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProducerStatsServiceImpl implements ProducerStatsService {
    private final ProducerStatsRepository statsRepository;

    @Override
    @Transactional
    public void recordProducerCall(String producerId, String topicName) {
        var now = LocalDateTime.now();

        boolean updated = false;
        int retryCount = 0;

        while (!updated && retryCount < 3) {
            try {
                statsRepository.findByProducerIdAndTopicName(producerId, topicName)
                        .ifPresentOrElse(
                                stats -> statsRepository.incrementCallCount(producerId, topicName, now),
                                () -> createNewStats(producerId, topicName, now)
                        );
                updated = true;
            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount >= 3) throw e;
            }
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