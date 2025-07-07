package com.example.resumeMatcherService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "resumes")
public class ResumeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String oldS3Url;
    private String modifiedS3Url;

    private Double score;

    @Lob
    private String aiResponse; // Full JSON or feedback

    @ManyToOne
    private UserEntity owner;

    @ManyToOne
    private JobDescriptionEntity job;

    private LocalDateTime uploadedAt;
    private LocalDateTime matchedAt;
}
