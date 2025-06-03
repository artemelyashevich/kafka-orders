package org.elyashevich.consumer.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elyashevich.consumer.domain.entity.Order;
import org.elyashevich.consumer.domain.entity.OrderStatus;
import org.elyashevich.consumer.exception.BusinessException;
import org.elyashevich.consumer.repository.OrderRepository;
import org.elyashevich.consumer.service.CategoryService;
import org.elyashevich.consumer.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    public static final String ORDER_WITH_ID_NOT_FOUND_TEMPLATE = "Order with id: '%d' not found";
    private final OrderRepository orderRepository;
    private final CategoryService categoryService;

    @Override
    @Transactional
    public Order create(Order order) {
        log.debug("Attempting to create order {}", order);

        order.setId(null);
        var category = categoryService.findByName(order.getCategory().getName());

        order.setCategory(category);
        order.setStatus(OrderStatus.PENDING);

        var createdOrder = orderRepository.save(order);

        log.info("Successfully created order {}", createdOrder.getId());
        return createdOrder;
    }

    @Override
    @Transactional
    public Order update(Order orderUpdate) {
        log.debug("Attempting to update order {}", orderUpdate.getId());

        Order existingOrder = this.findById(orderUpdate.getId());

        existingOrder.setProductName(orderUpdate.getProductName());
        existingOrder.setPrice(orderUpdate.getPrice());
        existingOrder.setQuantity(orderUpdate.getQuantity());

        if (orderUpdate.getCategory() != null) {
            var category = categoryService.findByName(orderUpdate.getCategory().getName());
            existingOrder.setCategory(category);
        }

        Order updatedOrder = orderRepository.save(existingOrder);

        log.info("Successfully updated order {}", updatedOrder.getId());
        return updatedOrder;
    }

    @Override
    public Order findById(Long id) {
        log.debug("Attempting to find order with id {}", id);

        var order = orderRepository.findById(id).orElseThrow(
                () -> {
                    var message = ORDER_WITH_ID_NOT_FOUND_TEMPLATE.formatted(id);
                    log.info(message);
                    return new BusinessException(message);
                }
        );

        log.info("Successfully found order {}", order.getId());
        return order;
    }

    @Override
    @Transactional
    public void cancel(Order candidate) {
        log.debug("Attempting to cancel order {}", candidate.getId());

        Order order = this.findById(candidate.getId());

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("Order {} is already cancelled", order.getId());
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("Successfully cancelled order {}", order.getId());
    }

    @Override
    @Transactional
    public Order complete(Order candidate) {
        log.debug("Attempting to complete order {}", candidate);

        Order order = this.findById(candidate.getId());

        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new BusinessException(
                    "Only orders in PROCESSING status can be completed");
        }

        order.setStatus(OrderStatus.DELIVERED);
        Order completedOrder = orderRepository.save(order);

        log.info("Successfully completed order {}", completedOrder.getId());
        return completedOrder;
    }
}