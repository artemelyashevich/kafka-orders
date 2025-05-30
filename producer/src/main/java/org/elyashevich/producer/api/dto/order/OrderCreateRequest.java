package org.elyashevich.producer.api.dto.order;

import java.math.BigDecimal;

public record OrderCreateRequest(
        Long orderId,
        String productName,
        String categoryName,
        BigDecimal price,
        Integer quantity,
        String status,
        Long customerId,
        String eventType
) {}