package org.elyashevich.consumer.api.dto.category;

public record CategoryResponseDto(
        Long id,
        String name,
        String description
) {
}
