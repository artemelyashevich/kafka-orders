package org.elyashevich.consumer.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elyashevich.consumer.domain.entity.Category;
import org.elyashevich.consumer.exception.ResourceAlreadyExistException;
import org.elyashevich.consumer.exception.ResourceNotFoundException;
import org.elyashevich.consumer.repository.CategoryRepository;
import org.elyashevich.consumer.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    public static final String CATEGORY_WITH_NAME_NOT_FOUND_TEMPLATE = "Category with name: '%s' not found";
    public static final String CATEGORY_WITH_NAME_ALREADY_EXISTS_TEMPLATE = "Category with name: '%s' already exists";
    private final CategoryRepository categoryRepository;

    @Override
    public List<Category> findAll() {
        log.debug("Attempting to find all categories");

        var categories = this.categoryRepository.findAll();

        log.info("Found {} categories", categories.size());
        return categories;
    }

    @Override
    public Category findByName(String name) {
        log.debug("Attempting to find category with name {}", name);

        var category = this.categoryRepository.findByName(name).orElseThrow(
                () -> {
                    var message = CATEGORY_WITH_NAME_NOT_FOUND_TEMPLATE.formatted(name);
                    log.info(message);
                    return new ResourceNotFoundException(message);
                }
        );

        log.info("Found category with name {}", name);
        return category;
    }

    @Override
    @Transactional
    public Category save(Category category) {
        log.debug("Attempting to save category with name {}", category.getName());

        if (this.categoryRepository.existsByName(category.getName())) {
            var message = CATEGORY_WITH_NAME_ALREADY_EXISTS_TEMPLATE.formatted(category.getName());
            log.info(message);
            throw new ResourceAlreadyExistException(message);
        }

        var newCategory = this.categoryRepository.save(category);

        log.info("Saved category with name {}", newCategory.getName());
        return newCategory;
    }
}
