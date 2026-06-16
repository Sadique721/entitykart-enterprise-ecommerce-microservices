package com.entitykart.userservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent {

    private Long id;
    private String name;
    private String email;
    private String role;
}
