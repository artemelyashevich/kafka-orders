package org.elyashevich.producer.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderEvent {
    private String eventId;
    private EventType eventType;
    private OrderData order;
    private LocalDateTime timestamp;
}