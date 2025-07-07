package com.example.resumeMatcherService.repository;

import com.example.resumeMatcherService.entity.ResumeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeRepository extends JpaRepository<ResumeEntity, String> {
    List<ResumeEntity> findByOwnerId(Long userId);
}

