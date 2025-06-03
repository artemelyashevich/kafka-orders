package org.elyashevich.consumer.repository;

import org.elyashevich.consumer.domain.entity.ProducerStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ProducerStatsRepository extends JpaRepository<ProducerStats, Long> {

    Optional<ProducerStats> findByProducerIdAndTopicName(String producerId, String topicName);
    
    @Modifying
    @Query("UPDATE ProducerStats ps SET ps.callCount = ps.callCount + 1, ps.lastCallTime = :now WHERE ps.producerId = :producerId AND ps.topicName = :topicName")
    void incrementCallCount(String producerId, String topicName, LocalDateTime now);
}