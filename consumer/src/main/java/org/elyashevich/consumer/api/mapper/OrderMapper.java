package org.elyashevich.consumer.api.mapper;

import org.elyashevich.consumer.domain.entity.Order;
import org.elyashevich.consumer.api.dto.order.OrderData;
import org.elyashevich.consumer.domain.entity.OrderStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface OrderMapper {

    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    @Mapping(target = "id", source = "orderId")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(OrderData orderData);

    @Named("stringToStatus")
    default OrderStatus stringToStatus(String status) {
        return status != null ? OrderStatus.valueOf(status) : null;
    }
}
