package org.elyashevich.producer.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderData {
    private Long orderId;
    private String productName;
    private String categoryName;
    private BigDecimal price;
    private Integer quantity;
    private String status;
    private Long customerId;
}