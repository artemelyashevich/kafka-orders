package org.elyashevich.consumer.service.impl;

import org.elyashevich.consumer.domain.entity.Category;
import org.elyashevich.consumer.exception.ResourceAlreadyExistException;
import org.elyashevich.consumer.exception.ResourceNotFoundException;
import org.elyashevich.consumer.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Test Category")
                .description("Test Description")
                .build();
    }

    @Test
    void findAll_NoCategories_ReturnsEmptyList() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        var result = categoryService.findAll();

        assertTrue(result.isEmpty());
        verify(categoryRepository).findAll();
    }

    @Test
    void findAll_WithCategories_ReturnsCategoryList() {
        var categories = List.of(testCategory, 
            Category.builder().id(2L).name("Another Category").build());
        when(categoryRepository.findAll()).thenReturn(categories);

        var result = categoryService.findAll();

        assertEquals(2, result.size());
        verify(categoryRepository).findAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Existing Category", "Another Category"})
    void findByName_ExistingCategory_ReturnsCategory(String name) {
        var category = Category.builder().name(name).build();
        when(categoryRepository.findByName(name)).thenReturn(Optional.of(category));

        var result = categoryService.findByName(name);

        assertEquals(name, result.getName());
        verify(categoryRepository).findByName(name);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    void findByName_BlankOrNullName_ThrowsException(String name) {
        assertThrows(ResourceNotFoundException.class,
            () -> categoryService.findByName(name));
    }

    @Test
    void findByName_NonExistentCategory_ThrowsException() {
        var name = "Non-existent";
        when(categoryRepository.findByName(name)).thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class, 
            () -> categoryService.findByName(name));

        assertEquals("Category with name: 'Non-existent' not found", exception.getMessage());
        verify(categoryRepository).findByName(name);
    }

    @Test
    void save_ValidCategory_ReturnsSavedCategory() {
        when(categoryRepository.existsByName(testCategory.getName())).thenReturn(false);
        when(categoryRepository.save(testCategory)).thenReturn(testCategory);

        var result = categoryService.save(testCategory);

        assertEquals(testCategory, result);
        verify(categoryRepository).existsByName(testCategory.getName());
        verify(categoryRepository).save(testCategory);
    }

    @Test
    void save_DuplicateCategoryName_ThrowsException() {
        when(categoryRepository.existsByName(testCategory.getName())).thenReturn(true);

        var exception = assertThrows(ResourceAlreadyExistException.class,
            () -> categoryService.save(testCategory));

        assertEquals("Category with name: 'Test Category' already exists", exception.getMessage());
        verify(categoryRepository).existsByName(testCategory.getName());
        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    void update_ValidCategory_ReturnsUpdatedCategory() {
        var updatedCategory = Category.builder()
                .name("Updated Name")
                .description("Updated Description")
                .build();
        var categoryId = 1L;

        when(categoryRepository.existsByName(updatedCategory.getName())).thenReturn(false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(testCategory)).thenReturn(testCategory);

        var result = categoryService.update(categoryId, updatedCategory);

        assertAll(
            () -> assertEquals(updatedCategory.getName(), result.getName()),
            () -> assertEquals(updatedCategory.getDescription(), result.getDescription()),
            () -> assertEquals(categoryId, result.getId())
        );
        verify(categoryRepository).existsByName(updatedCategory.getName());
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).save(testCategory);
    }

    @Test
    void update_NonExistentCategory_ThrowsException() {
        var categoryId = 99L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class, 
            () -> categoryService.update(categoryId, testCategory));

        assertEquals("Category with id: '99' was not found", exception.getMessage());
        verify(categoryRepository).findById(categoryId);
    }

    @Test
    void update_DuplicateCategoryName_ThrowsException() {
        var updatedCategory = Category.builder()
                .name("Duplicate Name")
                .build();
        when(categoryRepository.existsByName(updatedCategory.getName())).thenReturn(true);

        var exception = assertThrows(ResourceAlreadyExistException.class, 
            () -> categoryService.update(1L, updatedCategory));

        assertEquals("Category with name: 'Duplicate Name' already exists", exception.getMessage());
        verify(categoryRepository).existsByName(updatedCategory.getName());
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L, 100L})
    void findById_ExistingCategory_ReturnsCategory(Long id) {
        var category = Category.builder().id(id).build();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));

        var result = categoryService.findById(id);

        assertEquals(id, result.getId());
        verify(categoryRepository).findById(id);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1L, 0L, 999L})
    void findById_NonExistentCategory_ThrowsException(Long id) {
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class, 
            () -> categoryService.findById(id));

        assertEquals("Category with id: '%d' was not found".formatted(id), exception.getMessage());
        verify(categoryRepository).findById(id);
    }

    @Test
    void delete_ExistingCategory_DeletesSuccessfully() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        doNothing().when(categoryRepository).delete(testCategory);

        assertDoesNotThrow(() -> categoryService.delete(1L));
        
        verify(categoryRepository).findById(1L);
        verify(categoryRepository).delete(testCategory);
    }

    @Test
    void delete_NonExistentCategory_ThrowsException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class, 
            () -> categoryService.delete(1L));

        assertEquals("Category with id: '1' was not found", exception.getMessage());
        verify(categoryRepository).findById(1L);
        verifyNoMoreInteractions(categoryRepository);
    }
}