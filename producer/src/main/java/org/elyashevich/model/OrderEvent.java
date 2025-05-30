package org.elyashevich.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderEvent {
    private String eventId;
    private EventType eventType;
    private OrderData order;
    private LocalDateTime timestamp;
}