package ru.shim.FileConversionService.converter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TXTConverterTest {

    @Test
    void convertsTextToSinglePagePdf() throws IOException {
        String expectedText = "Привет, PDF!\r\nВторая строка";
        byte[] source = expectedText.getBytes(StandardCharsets.UTF_8);
        byte[] pdfBytes = new TXTConverter().convert(source);

        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            assertEquals(1, pdf.getNumberOfPages());

            String actualText = new PDFTextStripper().getText(pdf);
            assertEquals(expectedText.lines().toList(), actualText.lines().toList());
        }
    }

    @Test
    void convertsTextForTwoPagePdf() throws IOException {
        List<String> expectedText = new ArrayList<>();
        for (int i = 1; i <= 49; i++) {
            expectedText.add("Intro , Conclusion" + i);
        }
        String newExpectedText = String.join("\n", expectedText);

        byte[] source = newExpectedText.getBytes(StandardCharsets.UTF_8);
        byte[] pdfBytes = new TXTConverter().convert(source);

        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            assertEquals(2, pdf.getNumberOfPages());

            String actualText = new PDFTextStripper().getText(pdf);
            assertEquals(expectedText, actualText.lines().toList());

            System.out.println(expectedText);
        }
    }

    @Test
    void convertsTextWithOneLongLinePdf() throws IOException {

        String sourceText = "Hello ".repeat(30).stripTrailing();
        byte[] source = sourceText.getBytes(StandardCharsets.UTF_8);
        byte[] pdfBytes = new TXTConverter().convert(source);

        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {

            String actualText = new PDFTextStripper().getText(pdf);
            List<String> actualList = actualText.lines().toList();
            assertTrue(actualList.size() > 1);

            String joinedText = String.join(" ", actualList);
            assertEquals(sourceText, joinedText);

        }
    }

    @Test
    void convertsTextIfWordIsWiderThanPagePdf() throws IOException {

        String sourceText = "W".repeat(60);
        byte[] source = sourceText.getBytes(StandardCharsets.UTF_8);
        byte[] pdfBytes = new TXTConverter().convert(source);

        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {

            String actualText = new PDFTextStripper().getText(pdf);
            List<String> actualList = actualText.lines().toList();
            assertTrue(actualList.size() > 1);

            String joinedText = String.join("", actualList);
            assertEquals(sourceText, joinedText);

        }
    }

    @Test
    void convertsWhitespaceToSingleBlankPage() throws IOException {
        byte[] source = "   ".getBytes(StandardCharsets.UTF_8);
        byte[] pdfBytes = new TXTConverter().convert(source);
        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            assertEquals(1, pdf.getNumberOfPages());
            String actualText = new PDFTextStripper().getText(pdf);
            assertTrue(actualText.isBlank());
        }
    }
}
