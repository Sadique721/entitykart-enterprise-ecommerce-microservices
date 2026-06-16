package com.entitykart.userservice.service;

import com.entitykart.userservice.dto.AddressDTO;
import com.entitykart.userservice.entity.AddressEntity;
import com.entitykart.userservice.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    @Transactional
    public AddressDTO addAddress(Long userId, AddressDTO dto) {
        AddressEntity entity = new AddressEntity();
        entity.setUserId(userId);
        entity.setFullName(dto.getFullName());
        entity.setPhone(dto.getPhone());
        entity.setStreetAddress(dto.getStreetAddress());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setZipCode(dto.getZipCode());
        entity.setIsDefault(dto.getIsDefault());

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            // remove default from other addresses
            addressRepository.findByUserId(userId).forEach(addr -> {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            });
        }
        AddressEntity saved = addressRepository.save(entity);
        return convertToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<AddressDTO> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        addressRepository.deleteByUserIdAndId(userId, addressId);
    }

    @Transactional
    public AddressDTO updateAddress(Long userId, Long addressId, AddressDTO dto) {
        AddressEntity entity = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        if (!entity.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        entity.setFullName(dto.getFullName());
        entity.setPhone(dto.getPhone());
        entity.setStreetAddress(dto.getStreetAddress());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setZipCode(dto.getZipCode());
        if (Boolean.TRUE.equals(dto.getIsDefault()) && !Boolean.TRUE.equals(entity.getIsDefault())) {
            addressRepository.findByUserId(userId).forEach(addr -> addr.setIsDefault(false));
            entity.setIsDefault(true);
        } else {
            entity.setIsDefault(dto.getIsDefault());
        }
        return convertToDTO(addressRepository.save(entity));
    }

    private AddressDTO convertToDTO(AddressEntity entity) {
        AddressDTO dto = new AddressDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setFullName(entity.getFullName());
        dto.setPhone(entity.getPhone());
        dto.setStreetAddress(entity.getStreetAddress());
        dto.setCity(entity.getCity());
        dto.setState(entity.getState());
        dto.setZipCode(entity.getZipCode());
        dto.setIsDefault(entity.getIsDefault());
        return dto;
    }
}