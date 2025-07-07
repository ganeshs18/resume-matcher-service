package com.example.resumeMatcherService.dto.request;

import lombok.Data;

@Data
public class UserSignupRequest {
    private String name;
    private String email;
    private String phone;
    private String password;
}