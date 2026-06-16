package com.entitykart.userservice.dto;

import lombok.Data;

@Data
public class AddressDTO {
    private Long id;
    private Long userId;
    private String fullName;
    private String phone;
    private String streetAddress;
    private String city;
    private String state;
    private String zipCode;
    private Boolean isDefault;
}