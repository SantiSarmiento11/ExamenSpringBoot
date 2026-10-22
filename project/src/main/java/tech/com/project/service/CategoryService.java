package tech.com.project.service;

import tech.com.project.dto.response.CategoryResponseDto;

import java.util.List;

public interface CategoryService {
    List<CategoryResponseDto> getAllActiveCategories();
}
