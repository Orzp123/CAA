package com.caa.storage;

import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;

    public MinioStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Value("${minio.bucket-name}")
    private String defaultBucket;

    /**
     * Upload a file to the default bucket.
     *
     * @param objectName destination path within the bucket (e.g. "agents/avatar.png")
     * @param file       multipart file from the HTTP request
     * @return the object name stored
     */
    public String upload(String objectName, MultipartFile file) {
        try {
            ensureBucket(defaultBucket);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Uploaded {} to bucket {}", objectName, defaultBucket);
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO: " + objectName, e);
        }
    }

    /**
     * Upload an InputStream directly (useful for generated content).
     */
    public String upload(String objectName, InputStream inputStream, long size, String contentType) {
        try {
            ensureBucket(defaultBucket);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("Uploaded stream {} to bucket {}", objectName, defaultBucket);
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload stream to MinIO: " + objectName, e);
        }
    }

    /**
     * Download an object as an InputStream.
     */
    public InputStream download(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from MinIO: " + objectName, e);
        }
    }

    /**
     * Generate a pre-signed URL for temporary direct access (default 1 hour).
     */
    public String getPresignedUrl(String objectName) {
        return getPresignedUrl(objectName, 60);
    }

    public String getPresignedUrl(String objectName, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(defaultBucket)
                            .object(objectName)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL for: " + objectName, e);
        }
    }

    /**
     * 生成 PUT 预签名 URL，用于客户端直传（如 Logo 上传）。
     *
     * @param objectName    对象路径（如 "schools/{id}/logo.jpg"）
     * @param expirySeconds 有效期（秒）
     * @return PUT 预签名 URL
     */
    public String getPresignedPutUrl(String objectName, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(defaultBucket)
                            .object(objectName)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned PUT URL for: " + objectName, e);
        }
    }

    /**
     * Delete an object.
     */
    public void delete(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(objectName)
                            .build()
            );
            log.info("Deleted {} from bucket {}", objectName, defaultBucket);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from MinIO: " + objectName, e);
        }
    }

    /**
     * Check if an object exists.
     */
    public boolean exists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void ensureBucket(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Created bucket: {}", bucketName);
        }
    }
}
