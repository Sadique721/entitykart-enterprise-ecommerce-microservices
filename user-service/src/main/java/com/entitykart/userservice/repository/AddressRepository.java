package com.entitykart.userservice.repository;

import com.entitykart.userservice.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AddressRepository extends JpaRepository<AddressEntity, Long> {
    List<AddressEntity> findByUserId(Long userId);
    void deleteByUserIdAndId(Long userId, Long addressId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT a.city) FROM AddressEntity a")
    long countDistinctCities();
}