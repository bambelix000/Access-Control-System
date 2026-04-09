package com.bombel.AccessControl.user;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String name;
    private String identifier;
    private UserRole role;
}
