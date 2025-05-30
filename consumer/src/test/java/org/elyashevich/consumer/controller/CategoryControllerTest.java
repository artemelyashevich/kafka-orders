package org.elyashevich.consumer.controller;

import org.elyashevich.consumer.api.controller.CategoryController;
import org.elyashevich.consumer.api.dto.category.CategoryCreateDto;
import org.elyashevich.consumer.api.dto.category.CategoryResponseDto;
import org.elyashevich.consumer.api.mapper.CategoryMapper;
import org.elyashevich.consumer.domain.entity.Category;
import org.elyashevich.consumer.exception.ResourceNotFoundException;
import org.elyashevich.consumer.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@DisplayName("Category Controller Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private CategoryMapper categoryMapper;

    private Category testCategory;
    private CategoryCreateDto validCreateDto;
    private CategoryResponseDto responseDto;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices")
                .build();

        validCreateDto = new CategoryCreateDto(
                "Electronics",
                "Electronic devices"
        );

        responseDto = new CategoryResponseDto(
                1L,
                "Electronics",
                "Electronic devices"
        );
    }

    @Test
    @Order(1)
    @DisplayName("GET / - Success")
    void findAll_ReturnsListOfCategories() throws Exception {
        // Given
        given(categoryService.findAll()).willReturn(List.of(testCategory));
        given(categoryMapper.toDtoList(List.of(testCategory))).willReturn(List.of(responseDto));

        // When/Then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Electronics"))
                .andExpect(jsonPath("$[0].description").value("Electronic devices"));

        verify(categoryService).findAll();
        verify(categoryMapper).toDtoList(List.of(testCategory));
    }

    @Test
    @Order(2)
    @DisplayName("GET / - Empty List")
    void findAll_ReturnsEmptyList() throws Exception {
        // Given
        given(categoryService.findAll()).willReturn(List.of());

        // When/Then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(categoryService).findAll();
    }

    @Test
    @Order(3)
    @DisplayName("GET /{name} - Success")
    void findByName_WithValidName_ReturnsCategory() throws Exception {
        // Given
        given(categoryService.findByName("Electronics")).willReturn(testCategory);
        given(categoryMapper.toDto(testCategory)).willReturn(responseDto);

        // When/Then
        mockMvc.perform(get("/api/v1/categories/{name}", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.description").value("Electronic devices"));

        verify(categoryService).findByName("Electronics");
        verify(categoryMapper).toDto(testCategory);
    }

    @Test
    @Order(4)
    @DisplayName("GET /{name} - Not Found")
    void findByName_WithNonExistingName_ReturnsNotFound() throws Exception {
        // Given
        given(categoryService.findByName("Unknown"))
                .willThrow(new ResourceNotFoundException("Category not found"));

        // When/Then
        mockMvc.perform(get("/api/v1/categories/{name}", "Unknown"))
                .andExpect(status().isNotFound());

        verify(categoryService).findByName("Unknown");
    }

    @ParameterizedTest
    @Order(5)
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    @DisplayName("GET /{name} - Bad Request (Invalid Name)")
    void findByName_WithInvalidName_ReturnsBadRequest(String invalidName) throws Exception {
        mockMvc.perform(get("/api/v1/categories/{name}", invalidName))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).findByName(any());
    }

    @Test
    @Order(6)
    @DisplayName("POST / - Success")
    void create_WithValidDto_ReturnsCreated() throws Exception {
        // Given
        given(categoryMapper.toEntity(validCreateDto)).willReturn(testCategory);
        given(categoryService.save(any(Category.class))).willReturn(testCategory);
        given(categoryMapper.toDto(testCategory)).willReturn(responseDto);

        // When/Then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDto)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.description").value("Electronic devices"));

        verify(categoryMapper).toEntity(validCreateDto);
        verify(categoryService).save(any(Category.class));
        verify(categoryMapper).toDto(testCategory);
    }

    @Test
    @Order(7)
    @DisplayName("POST / - Bad Request (Empty Body)")
    void create_WithEmptyBody_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).save(any());
    }

    @ParameterizedTest
    @Order(8)
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    @DisplayName("POST / - Bad Request (Blank Name)")
    void create_WithBlankName_ReturnsBadRequest(String invalidName) throws Exception {
        CategoryCreateDto invalidDto = new CategoryCreateDto(
                invalidName,
                "Valid description"
        );

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).save(any());
    }

    @Test
    @Order(9)
    @DisplayName("POST / - Bad Request (Long Description)")
    void create_WithLongDescription_ReturnsBadRequest() throws Exception {
        CategoryCreateDto invalidDto = new CategoryCreateDto(
                "Valid name",
                "a".repeat(256) // Exceeds 255 character limit
        );

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).save(any());
    }

    @Test
    @Order(10)
    @DisplayName("PUT /{id} - Success")
    void update_WithValidDto_ReturnsCreated() throws Exception {
        // Given
        given(categoryMapper.toEntity(validCreateDto)).willReturn(testCategory);
        given(categoryService.update(anyLong(), any(Category.class))).willReturn(testCategory);
        given(categoryMapper.toDto(testCategory)).willReturn(responseDto);

        // When/Then
        mockMvc.perform(put("/api/v1/categories/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDto)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.description").value("Electronic devices"));

        verify(categoryMapper).toEntity(validCreateDto);
        verify(categoryService).update(eq(1L), any(Category.class));
        verify(categoryMapper).toDto(testCategory);
    }

    @Test
    @Order(11)
    @DisplayName("DELETE /{id} - Success")
    void delete_WithValidId_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(categoryService).delete(1L);
    }

    @Test
    @Order(12)
    @DisplayName("DELETE /{id} - Not Found")
    void delete_WithNonExistingId_ReturnsNotFound() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("Category not found"))
                .when(categoryService).delete(999L);

        // When/Then
        mockMvc.perform(delete("/api/v1/categories/{id}", 999L))
                .andExpect(status().isNotFound());

        verify(categoryService).delete(999L);
    }
}