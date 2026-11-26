package ru.shim.FileConversionService.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.minio")
public record MinioProperties(
        String endpoint,
        String accessKey,
        String secretKey
) {
}