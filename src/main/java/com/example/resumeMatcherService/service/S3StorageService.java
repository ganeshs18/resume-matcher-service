package com.example.resumeMatcherService.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;

    private final S3Presigner s3Presigner;


    @Value("${aws.s3.bucket-name}")
    private String bucketName;



    // Upload a file to S3 and return the object key
    public String uploadFile(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                    : "";

            String key = "resume/" + UUID.randomUUID() + extension;

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return key;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }



    // Delete a file from S3
    public void deleteFile(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(request);
    }

    public MultipartFile getFile(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try {
            ResponseInputStream<GetObjectResponse> responseStream = s3Client.getObject(getObjectRequest);
            return new MultipartFile() {
                @Override
                public String getName() {
                    return key;
                }

                @Override
                public String getOriginalFilename() {
                    return key;
                }

                @Override
                public String getContentType() {
                    return responseStream.response().contentType();
                }

                @Override
                public boolean isEmpty() {
                    return responseStream.response().contentLength() == 0;
                }

                @Override
                public long getSize() {
                    return responseStream.response().contentLength();
                }

                @Override
                public byte[] getBytes() throws IOException {
                    return responseStream.readAllBytes();
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return responseStream;
                }

                @Override
                public void transferTo(File dest) throws IOException, IllegalStateException {
                    throw new UnsupportedOperationException("Not implemented");
                }
            };
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to retrieve file from S3", e);
        }
    }


    public String generatePresignedUrl(String s3Key, long expiryInSeconds) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expiryInSeconds))
                .getObjectRequest(getObjectRequest)
                .build();

        URL url = s3Presigner.presignGetObject(presignRequest).url();
        return url.toString();
    }


}
