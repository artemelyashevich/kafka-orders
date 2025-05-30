package org.elyashevich.consumer.repository;

import org.elyashevich.consumer.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
