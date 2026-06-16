package com.entitykart.productservice.controller;

import com.entitykart.productservice.dto.CategoryDTO;
import com.entitykart.productservice.entity.CategoryEntity;
import com.entitykart.productservice.entity.SubCategoryEntity;
import com.entitykart.productservice.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryEntity> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @PostMapping
    public CategoryEntity createCategory(@RequestBody CategoryEntity category) {
        return categoryService.createCategory(category);
    }

    @PutMapping("/{id}")
    public CategoryEntity updateCategory(@PathVariable Long id, @RequestBody CategoryDTO categoryDTO) {
        return categoryService.updateCategory(id, categoryDTO);
    }

    @GetMapping("/{categoryId}/sub-categories")
    public List<SubCategoryEntity> getSubCategories(@PathVariable Long categoryId) {
        return categoryService.getSubCategories(categoryId);
    }

    @PostMapping("/{categoryId}/sub-categories")
    public SubCategoryEntity createSubCategory(
            @PathVariable Long categoryId,
            @RequestBody SubCategoryEntity subCategory) {
        return categoryService.createSubCategory(categoryId, subCategory);
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
    }

    @DeleteMapping("/{categoryId}/sub-categories/{subId}")
    public void deleteSubCategory(@PathVariable Long categoryId, @PathVariable Long subId) {
        categoryService.deleteSubCategory(categoryId, subId);
    }
}
