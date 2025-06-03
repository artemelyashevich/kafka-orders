package org.elyashevich.consumer.service;

import org.elyashevich.consumer.domain.entity.ProducerStats;

import java.util.List;

public interface ProducerStatsService {

    void recordProducerCall(String producerId, String topicName);

    List<ProducerStats> findAll();
}
