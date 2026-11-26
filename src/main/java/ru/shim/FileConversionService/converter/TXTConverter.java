package ru.shim.FileConversionService.converter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static ru.shim.FileConversionService.converter.FileType.TXT;


@Component
public class TXTConverter implements Converter {
    @Override
    public boolean supports(FileType fileType) {
        return fileType == TXT;
    }

    @Override
    public byte[] convert(byte[] source) throws IOException {
        String text = new String(source, StandardCharsets.UTF_8);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String[] lines = text.split("\\R", -1);
        float pageWidth = PDRectangle.A4.getWidth();
        float margin = 50;
        float lineHeight = 16;
        float widthOfPage = pageWidth - margin - margin;
        float fontSize = 12f;

        try (PDDocument document = new PDDocument()) {
            PDType0Font font;
            try (InputStream input = TXTConverter.class.getResourceAsStream("/fonts/ttf/DejaVuSans.ttf")) {
                if (input == null) {
                    throw new IOException("Шрифт DejaVuSans.ttf не найден в ресурсах");
                }
                font = PDType0Font.load(document, input, true);
            }

            List<String> wrappedLines = wrapLines(lines, font, fontSize, widthOfPage);
            renderPages(document, wrappedLines, font, fontSize, margin, lineHeight);

            document.save(output);
        }

        return output.toByteArray();
    }

    private void renderPages(
            PDDocument document,
            @NonNull List<String> wrappedLines,
            PDType0Font font,
            float fontSize,
            float margin,
            float lineHeight
    ) throws IOException {
        float pageHeight = PDRectangle.A4.getHeight();
        int linesPerPage = (int) ((pageHeight - margin - margin) / lineHeight + 1);
        for (int start = 0; start < wrappedLines.size(); start += linesPerPage) {
            int end = Math.min(wrappedLines.size(), start + linesPerPage);
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);


            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(font, fontSize);
                content.setLeading(lineHeight);
                content.newLineAtOffset(margin, page.getMediaBox().getHeight() - margin);

                for (int i = start; i < end; i++) {
                    if (i > start) {
                        content.newLine();
                    }

                    String line = wrappedLines.get(i);
                    if (!line.isEmpty()) {
                        content.showText(line);
                    }
                }
                content.endText();
            }
        }
    }

    private @NonNull List<String> wrapLines(String @NonNull [] lines, PDType0Font font, float fontSize, float widthOfPage) throws IOException {
        List<String> wrappedLines = new ArrayList<>();

        for (String originalLine : lines) {

            if (originalLine.isBlank()) {
                wrappedLines.add("");
                continue;
            }

            String[] words = originalLine.split(" ");
            String currentLine = "";

            for (String word : words) {
                String candidate = currentLine.isEmpty()
                        ? word
                        : currentLine + " " + word;

                float candidateWidth = font.getStringWidth(candidate) / 1000f * fontSize;

                if (candidateWidth <= widthOfPage) {
                    currentLine = candidate;
                } else {
                    if (!currentLine.isEmpty()) {
                        wrappedLines.add(currentLine);
                    }

                    float wordWidth = font.getStringWidth(word) / 1000f * fontSize;

                    if (wordWidth <= widthOfPage) {
                        currentLine = word;
                    } else {
                        List<String> parts = splitLongWord(word, font, fontSize, widthOfPage);
                        int lastIndex = parts.size() - 1;

                        wrappedLines.addAll(parts.subList(0, lastIndex));
                        currentLine = parts.get(lastIndex);
                    }
                }
            }

            if (!currentLine.isEmpty()) {
                wrappedLines.add(currentLine);
            }
        }
        return wrappedLines;
    }

    private @NonNull List<String> splitLongWord(
            @NonNull String word, PDType0Font font, float fontSize, float maxWidth) throws IOException {
        List<String> parts = new ArrayList<>();
        String currentPart = "";

        for (int codePoint : word.codePoints().toArray()) {
            String symbol = Character.toString(codePoint);
            String candidate = currentPart + symbol;
            float rawWidth = font.getStringWidth(candidate);
            float widthInPoints = (rawWidth / 1000f) * fontSize;

            if (widthInPoints <= maxWidth) {
                currentPart = candidate;
            } else {
                parts.add(currentPart);
                currentPart = symbol;
            }
        }
        if (!currentPart.isEmpty()) {
            parts.add(currentPart);
        }
        return parts;
    }
}
