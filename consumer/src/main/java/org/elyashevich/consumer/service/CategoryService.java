package org.elyashevich.consumer.service;

import org.elyashevich.consumer.domain.entity.Category;

import java.util.List;

public interface CategoryService {

    List<Category> findAll();

    Category findByName(String name);

    Category save(Category category);
}
