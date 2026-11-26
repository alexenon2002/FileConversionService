package ru.shim.FileConversionService.converter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ImageConverterTest {

    @Test
    void convertsPngToPdf() throws IOException {
        BufferedImage image = new BufferedImage(1000, 500, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean written = ImageIO.write(image, "png", output);
        assertTrue(written);
        byte[] pngBytes = output.toByteArray();
        ImageConverter converter = new ImageConverter();
        byte[] pdfBytes = converter.convert(pngBytes);

        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            assertEquals(1, pdf.getNumberOfPages());

            BufferedImage rendered = new PDFRenderer(pdf).renderImage(0);

            int cornerPixel = rendered.getRGB(10, 10);
            assertEquals(Color.WHITE.getRGB(), cornerPixel);

            int centerX = rendered.getWidth() / 2;
            int centerY = rendered.getHeight() / 2;
            int centerPixel = rendered.getRGB(centerX, centerY);

            assertEquals(Color.BLACK.getRGB(), centerPixel);
        }
    }

    @Test
    void convertsJpgToPdf() throws IOException {
        BufferedImage image = new BufferedImage(500, 1000, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean written = ImageIO.write(image, "jpg", output);
        assertTrue(written);
        byte[] jpgBytes = output.toByteArray();
        ImageConverter converter = new ImageConverter();
        byte[] pdfBytes = converter.convert(jpgBytes);

        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            assertEquals(1, pdf.getNumberOfPages());

            BufferedImage rendered = new PDFRenderer(pdf).renderImage(0);

            int cornerPixel = rendered.getRGB(10, 10);
            assertEquals(Color.WHITE.getRGB(), cornerPixel);

            int centerX = rendered.getWidth() / 2;
            int pixel = rendered.getRGB(centerX, 60);

            assertEquals(Color.BLACK.getRGB(), pixel);
        }
    }

    @Test
    public void rejectsInvalidImageBytes() {
        ImageConverter converter = new ImageConverter();
        byte[] invalidImage = {1, 2, 3, 4};
        var exception = assertThrows(
                InvalidSourceFileException.class,
                () -> converter.convert(invalidImage)
        );
        assertNotNull(exception.getCause());
    }
}
