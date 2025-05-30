package org.elyashevich.producer.api.mapper;

import org.elyashevich.producer.api.dto.order.OrderCreateRequest;
import org.elyashevich.producer.model.EventType;
import org.elyashevich.producer.model.OrderEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, imports = {UUID.class, LocalDateTime.class})
public interface OrderEventMapper {

    OrderEventMapper INSTANCE = Mappers.getMapper(OrderEventMapper.class);

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "timestamp", expression = "java(LocalDateTime.now())")
    @Mapping(source = "eventType", target = "eventType", qualifiedByName = "stringToEventType")
    @Mapping(source = ".", target = "order")
    OrderEvent toOrderEvent(OrderCreateRequest request);

    @Named("stringToEventType")
    default EventType stringToEventType(String eventType) {
        return eventType != null ? EventType.valueOf(eventType) : null;
    }
}