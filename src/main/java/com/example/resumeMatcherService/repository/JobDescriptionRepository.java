package com.example.resumeMatcherService.repository;

import com.example.resumeMatcherService.entity.JobDescriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobDescriptionRepository extends JpaRepository<JobDescriptionEntity, String> {
}

