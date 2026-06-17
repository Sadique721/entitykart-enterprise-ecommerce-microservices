package com.entitykart.productservice.service;

import com.entitykart.productservice.dto.CategoryDTO;
import com.entitykart.productservice.entity.CategoryEntity;
import com.entitykart.productservice.entity.SubCategoryEntity;
import com.entitykart.productservice.event.CategoryUpdatedEvent;
import com.entitykart.productservice.repository.CategoryRepository;
import com.entitykart.productservice.repository.SubCategoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final String CATEGORY_EVENTS_TOPIC = "category-events";

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional(readOnly = true)
    public List<CategoryEntity> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public CategoryEntity createCategory(CategoryEntity category) {
        if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required");
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public CategoryEntity updateCategory(Long id, CategoryDTO dto) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));

        if (dto.getCategoryName() != null) {
            category.setCategoryName(dto.getCategoryName());
        }
        if (dto.getActive() != null) {
            category.setActive(dto.getActive());
        }

        CategoryEntity saved = categoryRepository.save(category);
        CategoryUpdatedEvent event = new CategoryUpdatedEvent(
                saved.getCategoryId(),
                saved.getCategoryName(),
                saved.getActive(),
                LocalDateTime.now());
        kafkaTemplate.send(CATEGORY_EVENTS_TOPIC, event);

        return saved;
    }

    @Transactional
    public SubCategoryEntity createSubCategory(Long categoryId, SubCategoryEntity subCategory) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException("Category not found: " + categoryId);
        }

        subCategory.setCategoryId(categoryId);
        if (subCategory.getActive() == null) {
            subCategory.setActive(true);
        }
        return subCategoryRepository.save(subCategory);
    }

    @Transactional(readOnly = true)
    public List<SubCategoryEntity> getSubCategories(Long categoryId) {
        return subCategoryRepository.findByCategoryId(categoryId);
    }

    @Transactional
    public void deleteCategory(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        categoryRepository.delete(category);
    }

    @Transactional
    public void deleteSubCategory(Long categoryId, Long subId) {
        SubCategoryEntity subCategory = subCategoryRepository.findById(subId)
                .orElseThrow(() -> new RuntimeException("Sub-category not found: " + subId));
        if (!subCategory.getCategoryId().equals(categoryId)) {
            throw new RuntimeException("Sub-category does not belong to category: " + categoryId);
        }
        subCategoryRepository.delete(subCategory);
    }
}
