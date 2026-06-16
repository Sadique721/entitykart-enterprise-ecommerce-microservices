package com.entitykart.userservice.controller;

import com.entitykart.userservice.dto.AddressDTO;
import com.entitykart.userservice.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public AddressDTO addAddress(@RequestHeader("X-Customer-Id") Long userId,
                                 @RequestBody AddressDTO dto) {
        return addressService.addAddress(userId, dto);
    }

    @GetMapping
    public List<AddressDTO> getAddresses(@RequestHeader("X-Customer-Id") Long userId) {
        return addressService.getUserAddresses(userId);
    }

    @DeleteMapping("/{addressId}")
    public void deleteAddress(@RequestHeader("X-Customer-Id") Long userId,
                              @PathVariable Long addressId) {
        addressService.deleteAddress(userId, addressId);
    }

    @PutMapping("/{addressId}")
    public AddressDTO updateAddress(@RequestHeader("X-Customer-Id") Long userId,
                                    @PathVariable Long addressId,
                                    @RequestBody AddressDTO dto) {
        return addressService.updateAddress(userId, addressId, dto);
    }
}