package org.elyashevich.consumer.api.dto.order;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderEvent {
    private String eventId;
    private EventType eventType;
    private OrderData order;
    private LocalDateTime timestamp;
}