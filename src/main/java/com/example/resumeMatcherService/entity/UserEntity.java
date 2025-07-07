package com.example.resumeMatcherService.entity;


import com.example.resumeMatcherService.dto.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;
    private String pictureUrl;
    private String password; // For local auth, null for OAuth users

    @Enumerated(EnumType.STRING)
    private Role role; // USER, EDITOR, ADMIN

    @Column(nullable = false)
    private String provider; // e.g., GOOGLE

    private String providerId;     // Google sub ID
    private String profilePictureUrl; // Optional, can be null

    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;


}
