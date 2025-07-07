package com.example.resumeMatcherService.repository;

import com.example.resumeMatcherService.entity.ResumeBlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeBlockRepository extends JpaRepository<ResumeBlockEntity, String> {
    List<ResumeBlockEntity> findByResumeIdOrderByBlockIndexAsc(Long resumeId);
}

