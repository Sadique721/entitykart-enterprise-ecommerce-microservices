package com.entitykart.productservice.repository;

import com.entitykart.productservice.entity.SubCategoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubCategoryRepository extends JpaRepository<SubCategoryEntity, Long> {

    List<SubCategoryEntity> findByCategoryId(Long categoryId);
}
