package ru.shim.FileConversionService.storage;

import io.minio.errors.MinioException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.shim.FileConversionService.converter.TXTConverter;
import ru.shim.FileConversionService.conversion.ConversionService;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest
@ActiveProfiles("test")
public class MinioIntegrationTest {

    @Autowired
    private MinioStorage minioStorage;

    @Autowired
    private ConversionService conversionService;

    private @NonNull StorageLocation uploadTestText(@NonNull String text) throws MinioException {
        StorageLocation location = new StorageLocation(
                "uploads", "tests/" + UUID.randomUUID() + ".txt"
        );

        byte[] content = text.getBytes(StandardCharsets.UTF_8);
        minioStorage.upload(location, content, "text/plain; charset=utf-8");

        return location;
    }

    @Test
    public void downloadTextFromMinio() throws Exception {
        String expectedText = "Текст для проверки скачивания.";
        StorageLocation location = uploadTestText(expectedText);
        byte[] bytes = minioStorage.download(location);
        String text = new String(bytes, StandardCharsets.UTF_8);

        assertEquals(expectedText,
                text.strip());
    }

    @Test
    public void uploadsAndDownloadsPdf() throws Exception {
        byte[] pdfBytes = new TXTConverter()
                .convert("MinIO upload test".getBytes(StandardCharsets.UTF_8));
        StorageLocation location =
                new StorageLocation("uploads", "tests/" + UUID.randomUUID() + ".pdf");
        minioStorage.upload(location, pdfBytes, "application/pdf");
        byte[] downloadedBytes = minioStorage.download(location);
        assertArrayEquals(pdfBytes, downloadedBytes);
    }

    @Test
    public void convertFileToPdf() throws Exception {
        String expectedText = "Тестовый текст";
        StorageLocation location = uploadTestText(expectedText);
        var destination = new StorageLocation(
                "uploads", "tests/conversion-" + UUID.randomUUID() + ".pdf");
        conversionService.convert(location, destination);
        byte[] pdfBytes = minioStorage.download(destination);

        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            String actualText = new PDFTextStripper().getText(pdf);
            assertEquals(expectedText,
                    actualText.strip());
        }
    }
}
