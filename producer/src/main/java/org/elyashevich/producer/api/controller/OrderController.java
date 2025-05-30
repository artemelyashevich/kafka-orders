package org.elyashevich.producer.api.controller;

import lombok.RequiredArgsConstructor;
import org.elyashevich.producer.api.dto.order.OrderCreateRequest;
import org.elyashevich.producer.api.mapper.OrderEventMapper;
import org.elyashevich.producer.producer.KafkaOrderProducer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final KafkaOrderProducer producer;
    private static final OrderEventMapper orderEventMapper = OrderEventMapper.INSTANCE;

    @PostMapping
    public void handleOrder(@RequestBody OrderCreateRequest event) {
        producer.sendOrderToKafka(orderEventMapper.toOrderEvent(event));
    }
}
