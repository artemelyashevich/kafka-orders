package org.elyashevich.consumer.api.mapper;

import org.elyashevich.consumer.api.dto.category.CategoryCreateDto;
import org.elyashevich.consumer.api.dto.category.CategoryResponseDto;
import org.elyashevich.consumer.domain.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface CategoryMapper {

    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);

    Category toEntity(CategoryCreateDto categoryCreateDto);

    CategoryResponseDto toDto(Category category);

    List<CategoryResponseDto> toDtoList(List<Category> categories);
}
