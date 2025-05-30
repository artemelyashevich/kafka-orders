package org.elyashevich.consumer.service;

import org.elyashevich.consumer.domain.entity.Order;

public interface OrderService {

    Order create(Order order);

    Order update(Order order);

    Order findById(Long id);

    void cancel(Order order);

    Order complete(Order order);
}
