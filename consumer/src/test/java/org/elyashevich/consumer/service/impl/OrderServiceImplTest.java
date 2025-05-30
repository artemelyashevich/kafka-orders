package org.elyashevich.consumer.service.impl;

import org.elyashevich.consumer.domain.entity.Category;
import org.elyashevich.consumer.domain.entity.Order;
import org.elyashevich.consumer.domain.entity.OrderStatus;
import org.elyashevich.consumer.exception.BusinessException;
import org.elyashevich.consumer.repository.OrderRepository;
import org.elyashevich.consumer.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .build();

        testOrder = Order.builder()
                .id(1L)
                .productName("Smartphone")
                .price(BigDecimal.valueOf(999.99))
                .quantity(1)
                .status(OrderStatus.PENDING)
                .category(testCategory)
                .build();
    }

    @Test
    void create_ValidOrder_ReturnsCreatedOrder() {
        when(categoryService.findByName(anyString())).thenReturn(testCategory);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        var result = orderService.create(testOrder);

        assertAll(
            () -> assertEquals(testOrder.getId(), result.getId()),
            () -> assertEquals(OrderStatus.PENDING, result.getStatus()),
            () -> assertEquals(testCategory, result.getCategory())
        );
        verify(categoryService).findByName(testCategory.getName());
        verify(orderRepository).save(testOrder);
    }

    @Test
    void create_NullOrder_ThrowsException() {
        assertThrows(NullPointerException.class, () -> orderService.create(null));
    }

    @Test
    void create_OrderWithNullCategory_ThrowsException() {
        testOrder.setCategory(null);
        assertThrows(NullPointerException.class, () -> orderService.create(testOrder));
    }

    @Test
    void update_ValidOrder_ReturnsUpdatedOrder() {
        var updatedOrder = Order.builder()
                .id(1L)
                .productName("Updated Smartphone")
                .price(BigDecimal.valueOf(899.99))
                .quantity(2)
                .category(testCategory)
                .build();

        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(testOrder));
        when(categoryService.findByName(anyString())).thenReturn(testCategory);
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        var result = orderService.update(updatedOrder);

        assertAll(
            () -> assertEquals(updatedOrder.getProductName(), result.getProductName()),
            () -> assertEquals(updatedOrder.getPrice(), result.getPrice()),
            () -> assertEquals(updatedOrder.getQuantity(), result.getQuantity()),
            () -> assertEquals(testCategory, result.getCategory())
        );
        verify(orderRepository).findById(testOrder.getId());
        verify(categoryService).findByName(testCategory.getName());
        verify(orderRepository).save(testOrder);
    }

    @Test
    void update_OrderWithNewCategory_UpdatesCategory() {
        var newCategory = Category.builder().name("New Category").build();
        var updatedOrder = Order.builder()
                .id(1L)
                .category(newCategory)
                .build();

        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(testOrder));
        when(categoryService.findByName(anyString())).thenReturn(newCategory);
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        var result = orderService.update(updatedOrder);

        assertEquals(newCategory, result.getCategory());
        verify(categoryService).findByName(newCategory.getName());
    }

    @Test
    void update_NonExistentOrder_ThrowsException() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        var exception = assertThrows(BusinessException.class, 
            () -> orderService.update(testOrder));

        assertEquals("Order with id: '1' not found", exception.getMessage());
        verify(orderRepository).findById(testOrder.getId());
    }

    @ParameterizedTest
    @ValueSource(longs = {1L})
    void findById_ExistingOrder_ReturnsOrder(Long id) {
        when(orderRepository.findById(id)).thenReturn(Optional.of(testOrder));

        var result = orderService.findById(id);

        assertEquals(id, result.getId());
        verify(orderRepository).findById(id);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1L, 0L, 999L})
    void findById_NonExistentOrder_ThrowsException(Long id) {
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        var exception = assertThrows(BusinessException.class, 
            () -> orderService.findById(id));

        assertEquals("Order with id: '%d' not found".formatted(id), exception.getMessage());
        verify(orderRepository).findById(id);
    }

    @Test
    void cancel_OrderNotCancelled_UpdatesStatus() {
        testOrder.setStatus(OrderStatus.PROCESSING);
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        orderService.cancel(testOrder);

        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        verify(orderRepository).findById(testOrder.getId());
        verify(orderRepository).save(testOrder);
    }

    @Test
    void cancel_AlreadyCancelledOrder_DoesNothing() {
        testOrder.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(testOrder));

        orderService.cancel(testOrder);

        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        verify(orderRepository).findById(testOrder.getId());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancel_NullOrder_ThrowsException() {
        assertThrows(NullPointerException.class, () -> orderService.cancel(null));
    }

    @Test
    void complete_ProcessingOrder_UpdatesStatusToDelivered() {
        testOrder.setStatus(OrderStatus.PROCESSING);
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        var result = orderService.complete(testOrder);

        assertEquals(OrderStatus.DELIVERED, result.getStatus());
        verify(orderRepository).findById(testOrder.getId());
        verify(orderRepository).save(testOrder);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "CANCELLED", "DELIVERED"})
    void complete_NonProcessingOrder_ThrowsException(OrderStatus status) {
        testOrder.setStatus(status);
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(testOrder));

        var exception = assertThrows(BusinessException.class, 
            () -> orderService.complete(testOrder));

        assertEquals("Only orders in PROCESSING status can be completed", exception.getMessage());
        verify(orderRepository).findById(testOrder.getId());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void complete_NullOrder_ThrowsException() {
        assertThrows(NullPointerException.class, () -> orderService.complete(null));
    }
}