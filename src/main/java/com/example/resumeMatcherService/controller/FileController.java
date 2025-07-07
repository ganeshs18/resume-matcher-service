package com.example.resumeMatcherService.controller;


import com.example.resumeMatcherService.dto.CommonResponse;
import com.example.resumeMatcherService.service.DocumentReaderService;
import com.example.resumeMatcherService.service.S3StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class FileController {

    @Autowired
    private S3StorageService s3StorageService;

    @Autowired
    private DocumentReaderService documentReaderService;

    // Endpoint to read a document from S3 and return its content

    @PostMapping(value = "/read-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<String>> readDocument(
        @Parameter(description = "File to upload", required = true, schema = @Schema(type = "string", format = "binary"))
        @RequestPart("file") MultipartFile file) throws IOException {
        String content = documentReaderService.extractContentFromMultipart(file);
        CommonResponse<String> response = new CommonResponse<>("Document read successfully", content, HttpStatus.OK);
        return ResponseEntity.ok(response);
    }

    // This controller will handle file upload and download requests

    @PostMapping(value ="/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileKey = s3StorageService.uploadFile(file);
        CommonResponse<String> response = new CommonResponse<>("File uploaded successfully", fileKey, HttpStatus.OK);
        return ResponseEntity.ok(response);
    }



}
