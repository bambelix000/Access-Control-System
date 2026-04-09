package com.bombel.AccessControl.user.dto;

import com.bombel.AccessControl.user.UserRole;
import lombok.Data;

@Data
public class CreateUserRequest {
    private String name;
    private String identifier;
    private UserRole role;
}
