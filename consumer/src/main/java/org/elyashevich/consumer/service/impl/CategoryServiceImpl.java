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
    public static final String CATEGORY_WITH_ID_WAS_NOT_FOUND_TEMPLATE = "Category with id: '%d' was not found";
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

        this.checkIfCategoryExistsByName(category.getName());

        var newCategory = this.categoryRepository.save(category);

        log.info("Saved category with name {}", newCategory.getName());
        return newCategory;
    }

    @Override
    @Transactional
    public Category update(Long id, Category category) {
        log.debug("Attempting update category with id: {}", id);

        this.checkIfCategoryExistsByName(category.getName());

        var oldCategory = this.findById(id);

        oldCategory.setDescription(category.getDescription());
        oldCategory.setName(category.getName());

        var updatedCategory = this.categoryRepository.save(oldCategory);

        log.info("Category updated: {}", updatedCategory);
        return updatedCategory;
    }

    @Override
    public Category findById(Long id) {
        log.debug("Attempting find category with id: {}", id);

        var category = this.categoryRepository.findById(id).orElseThrow(
                () -> {
                    var message = CATEGORY_WITH_ID_WAS_NOT_FOUND_TEMPLATE.formatted(id);
                    log.info(message);
                    return new ResourceNotFoundException(message);
                }
        );

        log.info("Category found: {}", category);
        return category;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.debug("Attempting delete category with id: {}", id);

        var category = this.findById(id);

        this.categoryRepository.delete(category);

        log.info("Category with id: {} deleted", id);
    }

    private void checkIfCategoryExistsByName(String name) {
        if (this.categoryRepository.existsByName(name)) {
            var message = CATEGORY_WITH_NAME_ALREADY_EXISTS_TEMPLATE.formatted(name);
            log.info(message);
            throw new ResourceAlreadyExistException(message);
        }
    }
}
