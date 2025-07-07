package com.example.resumeMatcherService.dto.response;


import com.example.resumeMatcherService.dto.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserLoginResponseDTO {
    private String token;
    private UserDTO user;
}
