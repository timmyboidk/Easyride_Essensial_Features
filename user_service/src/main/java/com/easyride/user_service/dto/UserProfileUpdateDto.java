package com.easyride.user_service.dto;

import lombok.Data;

@Data
public class UserProfileUpdateDto {
    private String email;
    private String address;

    // Add other fields that can be updated
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}