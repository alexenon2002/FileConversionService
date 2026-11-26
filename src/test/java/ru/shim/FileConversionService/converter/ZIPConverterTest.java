package ru.shim.FileConversionService.converter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ZIPConverterTest {
    private final ZIPConverter converter = new ZIPConverter(
            new FileTypeResolver(),
            new ConverterSelector(List.of(new TXTConverter(), new ImageConverter()))
    );

    private byte @NonNull [] createZipWithNote() throws IOException {
        ByteArrayOutputStream archive = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(archive)) {
            zip.putNextEntry(new ZipEntry("docs/"));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("docs/note.txt"));
            zip.write("Hello ZIP".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        return archive.toByteArray();
    }

    private byte @NonNull [] createZipWithTwoNotes() throws IOException {
        ByteArrayOutputStream archive = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(archive)) {
            zip.putNextEntry(new ZipEntry("z-first.txt"));
            zip.write("First".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("a-second.txt"));
            zip.write("Second".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        return archive.toByteArray();
    }

    @Test
    void selectsRegisteredZipConverter() {
        try (var context = new AnnotationConfigApplicationContext(
                FileTypeResolver.class,
                ConverterSelector.class,
                TXTConverter.class,
                ImageConverter.class,
                ZIPConverter.class
        )) {
            ConverterSelector selector = context.getBean(ConverterSelector.class);
            ZIPConverter expected = context.getBean(ZIPConverter.class);
            Converter actual = selector.select(FileType.ZIP);

            assertSame(expected, actual);
        }
    }

    @Test
    void readsFileAndSkipsDirectory() throws IOException {

        var entries = converter.readEntries(createZipWithNote());

        assertEquals(1, entries.size());

        var entry = entries.getFirst();
        assertEquals("docs/note.txt", entry.name());

        byte[] bytes = entry.bytes();
        assertEquals("Hello ZIP", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    void convertsTxtEntryToPdf() throws IOException {
        List<byte[]> pdfFiles = converter.convertEntries(createZipWithNote());

        assertEquals(1, pdfFiles.size());

        try (PDDocument pdf = Loader.loadPDF(pdfFiles.getFirst())) {
            String actualText = new PDFTextStripper().getText(pdf).strip();

            assertEquals("Hello ZIP", actualText);
        }
    }

    @Test
    void mergesZipEntriesInArchiveOrder() throws IOException {
        byte[] source = createZipWithTwoNotes();

        byte[] pdfBytes = converter.convert(source);

        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {

            assertEquals(2, pdf.getNumberOfPages());
            assertEquals(List.of("First", "Second"), new PDFTextStripper().getText(pdf).lines().toList());
        }
    }

    @Test
    void convertsNestedZipToPdf() throws IOException {
        byte[] innerZip = createZipWithNote();
        var output = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("inner.zip"));
            zip.write(innerZip);
            zip.closeEntry();
        }
        byte[] outerZip = output.toByteArray();
        try (var context = new AnnotationConfigApplicationContext(
                FileTypeResolver.class,
                ConverterSelector.class,
                TXTConverter.class,
                ImageConverter.class,
                ZIPConverter.class
        )) {
            ConverterSelector selector = context.getBean(ConverterSelector.class);
            Converter converter = selector.select(FileType.ZIP);
            byte[] pdfBytes = converter.convert(outerZip);

            try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
                assertEquals(1, pdf.getNumberOfPages());
                assertEquals(List.of("Hello ZIP"), new PDFTextStripper().getText(pdf).lines().toList());
            }
        }
    }

    @Test
    public void rejectsInvalidZipBytes() {
        byte[] invalidZip = {1, 2, 3, 4};

        var exception = assertThrows(
                InvalidSourceFileException.class,
                () -> converter.convert(invalidZip)
        );
        assertEquals("В ZIP-архиве нет файлов для конвертации", exception.getMessage());
    }
}