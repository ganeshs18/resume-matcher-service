package com.example.resumeMatcherService.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_descriptions")
public class JobDescriptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String company;
    private String jobUrl;

    @Lob
    private String description; // Raw JD text pasted

    @ManyToOne
    private UserEntity postedBy;

    private LocalDateTime createdAt;
}