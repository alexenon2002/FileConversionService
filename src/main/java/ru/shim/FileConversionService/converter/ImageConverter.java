package ru.shim.FileConversionService.converter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static ru.shim.FileConversionService.converter.FileType.JPG;
import static ru.shim.FileConversionService.converter.FileType.PNG;

import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

@Component
public class ImageConverter implements Converter {

    @Override
    public boolean supports(FileType fileType) {
        return fileType == JPG || fileType == PNG;
    }

    @Override
    public byte[] convert(byte[] source) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDImageXObject image;
            try {
                image = PDImageXObject.createFromByteArray(document, source, "image");
            } catch (IOException | IllegalArgumentException cause) {
                throw new InvalidSourceFileException("Не удалось прочитать исходное изображение", cause);
            }
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 50f;

            float availableWidth = pageWidth - 2 * margin;
            float availableHeight = pageHeight - 2 * margin;

            float scaleByWidth = availableWidth / imageWidth;
            float scaleByHeight = availableHeight / imageHeight;

            float scale = Math.min(scaleByWidth, scaleByHeight);
            float drawWidth = imageWidth * scale;
            float drawHeight = imageHeight * scale;

            float x = (pageWidth - drawWidth) / 2;
            float y = (pageHeight - drawHeight) / 2;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(image, x, y, drawWidth, drawHeight);
            }

            document.save(output);
        }
        return output.toByteArray();
    }
}