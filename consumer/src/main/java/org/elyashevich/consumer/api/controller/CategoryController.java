package org.elyashevich.consumer.api.controller;

import lombok.RequiredArgsConstructor;
import org.elyashevich.consumer.api.dto.category.CategoryCreateDto;
import org.elyashevich.consumer.api.dto.category.CategoryResponseDto;
import org.elyashevich.consumer.api.mapper.CategoryMapper;
import org.elyashevich.consumer.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private static final CategoryMapper categoryMapper = CategoryMapper.INSTANCE;

    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> findAll() {
        var categories = this.categoryService.findAll();
        return ResponseEntity.ok(
                categoryMapper.toDtoList(categories)
        );
    }

    @GetMapping("/{name}")
    public ResponseEntity<CategoryResponseDto> findByName(@PathVariable String name) {
        var category = this.categoryService.findByName(name);
        return ResponseEntity.ok(
                categoryMapper.toDto(category)
        );
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDto> create(
            @Validated @RequestBody CategoryCreateDto dto,
            UriComponentsBuilder uriBuilder
    ) {
        var category = this.categoryService.save(categoryMapper.toEntity(dto));
        return ResponseEntity.created(
                uriBuilder.replacePath("/api/v1/categories/{name}").build(Map.of("name", category.getName()))
        ).body(categoryMapper.toDto(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> update(
            @PathVariable("id") Long id,
            @Validated @RequestBody CategoryCreateDto dto,
            UriComponentsBuilder uriBuilder
    ) {
        var category = this.categoryService.update(id, categoryMapper.toEntity(dto));
        return ResponseEntity.created(
                uriBuilder.replacePath("/api/v1/categories/{name}").build(Map.of("name", category.getName()))
        ).body(categoryMapper.toDto(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        this.categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
