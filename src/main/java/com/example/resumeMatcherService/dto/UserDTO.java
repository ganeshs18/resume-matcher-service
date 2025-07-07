package com.example.resumeMatcherService.dto;


import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String profilePictureUrl;
    private Role role;


}

