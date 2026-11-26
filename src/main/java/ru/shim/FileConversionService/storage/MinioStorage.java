package ru.shim.FileConversionService.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class MinioStorage {
    private final MinioClient minioClient;

    public MinioStorage(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public byte[] download(@NonNull StorageLocation location) throws Exception {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(location.bucket())
                .object(location.key())
                .build();
        try (InputStream stream = minioClient.getObject(args)) {
            return stream.readAllBytes();
        } catch (ErrorResponseException exception) {
            if ("NoSuchKey".equals(exception.errorResponse().code())) {
                throw new SourceFileNotFoundException(
                        "Исходный файл не найден: " + location.key(),
                        exception
                );
            }
            throw exception;
        }
    }

    public void upload(@NonNull StorageLocation location, byte[] content, String contentType)
            throws MinioException {
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(location.bucket())
                .object(location.key())
                .data(content, content.length)
                .contentType(contentType)
                .build();

        minioClient.putObject(args);
    }
}
