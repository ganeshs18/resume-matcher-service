package com.example.resumeMatcherService.service;


import com.example.resumeMatcherService.dto.*;
import com.example.resumeMatcherService.dto.request.UserLoginRequest;
import com.example.resumeMatcherService.dto.request.UserSignupRequest;
import com.example.resumeMatcherService.dto.response.UserLoginResponseDTO;
import com.example.resumeMatcherService.entity.UserEntity;
import com.example.resumeMatcherService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final JwtService jwtService;


    private final PasswordEncoder passwordEncoder;

    public UserLoginResponseDTO signup(UserSignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CommonErrorException("Email already in use", "EMAIL_IN_USE");
        }

        UserEntity user = UserEntity.builder()
                .name(request.getName())
                .email(request.getEmail())
//                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.valueOf(Role.JOB_SEEKER.name()))
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return generateResponse(user);
    }

    public ResponseEntity<?> login(UserLoginRequest request) {
        UserEntity userEntity = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (userEntity == null || !passwordEncoder.matches(request.getPassword(), userEntity.getPassword())) {
            CommonErrorResponse error = new CommonErrorResponse("Invalid email or password", "INVALID_CREDENTIALS");
            return ResponseEntity.status(401).body(error);
        }
        UserLoginResponseDTO response = generateResponse(userEntity);
        return ResponseEntity.ok(new CommonResponse<>( "Login successful", response));
    }

    public UserLoginResponseDTO loginOrRegisterGoogleUser(String email, String name) {
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = UserEntity.builder()
                    .email(email)
                    .name(name)
                    .role(Role.JOB_SEEKER)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(user);
        }
        return generateResponse(user);
    }

    private UserLoginResponseDTO generateResponse(UserEntity user) {
        String token = jwtService.generateToken(user);
        UserDTO userDTO = UserDTO.builder()
                .id(String.valueOf(user.getId()))
                .email(user.getEmail())
                .name(user.getName())
//                .phone(user.getPhone())
                .role(Role.valueOf(String.valueOf(user.getRole())))
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();

        return new UserLoginResponseDTO(token, userDTO);
    }
}
