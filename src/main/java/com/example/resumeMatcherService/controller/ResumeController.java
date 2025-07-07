package com.example.resumeMatcherService.controller;


import com.example.resumeMatcherService.dto.CommonResponse;
import com.example.resumeMatcherService.dto.ResumeBlockDto;
import com.example.resumeMatcherService.dto.ResumeEvent;
import com.example.resumeMatcherService.entity.ResumeBlockEntity;
import com.example.resumeMatcherService.entity.ResumeEntity;
import com.example.resumeMatcherService.repository.ResumeBlockRepository;
import com.example.resumeMatcherService.repository.ResumeRepository;
import com.example.resumeMatcherService.service.DocumentReaderService;
import com.example.resumeMatcherService.service.JwtService;
import com.example.resumeMatcherService.service.S3StorageService;
import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/resume")
public class ResumeController {



    @Autowired
    private S3StorageService s3StorageService;

    @Autowired
    private DocumentReaderService documentReaderService;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private JwtService jwtService;

     @PostMapping(value = "/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
     public ResponseEntity<CommonResponse<String>> uploadResume(
            @RequestPart("file") MultipartFile file,
            @RequestPart("userId") Long userId) {
        // You may want to use userId for associating the resume with a user
        String fileKey = s3StorageService.uploadFile(file);
        CommonResponse<String> response = new CommonResponse<>("Resume uploaded successfully", fileKey, HttpStatus.OK);
        return ResponseEntity.ok(response);
     }

     @CrossOrigin(origins = "*")
    @GetMapping(
            value = "/enhance",
//            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<ResumeEvent>> enhanceResumeWithSSE(
            @Parameter(description = "File Id", required = true)
            @RequestParam(name = "s3Key") String s3Key,

            @Parameter(description = "Job title for enhancement", required = true)
            @RequestParam(name = "jobTitle") String jobTitle,

            @Parameter(description = "Company name", required = true)
            @RequestParam(name = "company") String company,

            @Parameter(description = "User ID", required = true)
            @RequestParam(name = "userId") Long userId,

            @Parameter(description = "Raw Job Description", required = true)
            @RequestParam(name = "rawJd") String rawJobDescription,
            @Parameter(description = "Token", required = true)
            @RequestParam(name = "token") String jwtToken
    ) {
        // Validate JWT token
        if (!jwtService.isTokenValid(jwtToken)) {
            return Flux.just(ServerSentEvent.<ResumeEvent>builder()
                .event("ERROR")
                .data(new ResumeEvent(ResumeEvent.EventType.ERROR, "Invalid or expired token", null, 0))
                .build());
        }
        return documentReaderService.enhanceResumeStream(
                        s3Key,
                        jobTitle,
                        company,
                        userId,
                        rawJobDescription
                )
                .map(event -> {
                    String eventType = event.getType() != null ? event.getType().name() : "ERROR";
                    return ServerSentEvent.<ResumeEvent>builder()
                            .event(eventType)
                            .id(UUID.randomUUID().toString())
                            .data(event)
                            .build();
                });
    }

    @Autowired
    private ResumeBlockRepository resumeBlockRepository;

    @GetMapping("/{resumeId}")
    public ResponseEntity<List<ResumeBlockDto>> getBlocksByResumeId(@PathVariable Long resumeId) {
        List<ResumeBlockEntity> entities = resumeBlockRepository.findByResumeIdOrderByBlockIndexAsc(resumeId);

        List<ResumeBlockDto> dtos = entities.stream()
            .map(ResumeBlockDto::fromEntity)
            .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/resumes/{userId}")
    public ResponseEntity<List<ResumeEntity>> getResumesByUserId(@PathVariable Long userId) {
        List<ResumeEntity> resumes = resumeRepository.findByOwnerId(userId);
        return ResponseEntity.ok(resumes);
    }

    @PostMapping("/blocks/enhance")
    public ResponseEntity<List<ResumeBlockDto>> saveEnhancedBlocks(@RequestBody List<ResumeBlockDto> blocks) {
        List<ResumeBlockEntity> entities = blocks.stream()
                .map(dto -> {
                    ResumeBlockEntity entity = resumeBlockRepository.findById(String.valueOf(dto.getId())).orElse(null);
                    if (entity != null) {
                        entity.setEnhancedText(dto.getEnhancedText());
                        entity.setEnhancedAt(java.time.LocalDateTime.now());
                    }
                    return entity;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        List<ResumeBlockEntity> saved = resumeBlockRepository.saveAll(entities);
        List<ResumeBlockDto> result = saved.stream().map(ResumeBlockDto::fromEntity).toList();
        return ResponseEntity.ok(result);
    }


}
