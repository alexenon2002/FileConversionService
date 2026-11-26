package ru.shim.FileConversionService.converter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ZIPConverter implements Converter {

    private final FileTypeResolver fileTypeResolver;
    private final ConverterSelector converterSelector;

    public ZIPConverter(
            FileTypeResolver fileTypeResolver,
            @Lazy ConverterSelector converterSelector
    ) {
        this.fileTypeResolver = fileTypeResolver;
        this.converterSelector = converterSelector;
    }

    List<byte[]> convertEntries(byte[] source) throws IOException {
        List<byte[]> pdfFiles = new ArrayList<>();

        for (ArchiveEntry entry : readEntries(source)) {
            FileType fileType = fileTypeResolver.resolve(entry.name());
            Converter converter = converterSelector.select(fileType);
            pdfFiles.add(converter.convert(entry.bytes()));
        }

        return pdfFiles;
    }

    private byte @NonNull [] mergePdfs(@NonNull List<byte[]> pdfFiles) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PDFMergerUtility merger = new PDFMergerUtility();

        try (PDDocument destination = new PDDocument()) {
            for (byte[] pdfBytes : pdfFiles) {
                try (PDDocument part = Loader.loadPDF(pdfBytes)) {
                    merger.appendDocument(destination, part);
                }
            }

            destination.save(output);
        }

        return output.toByteArray();
    }

    @Override
    public boolean supports(FileType fileType) {
        return fileType == FileType.ZIP;
    }

    @Override
    public byte[] convert(byte[] source) throws IOException {
        List<byte[]> pdfFiles = convertEntries(source);
        return mergePdfs(pdfFiles);
    }

    record ArchiveEntry(String name, byte[] bytes) {
    }

    List<ArchiveEntry> readEntries(byte[] source) throws IOException {
        List<ArchiveEntry> entries = new ArrayList<>();

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(source))) {
            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                byte[] bytes = zip.readAllBytes();
                ArchiveEntry archiveEntry = new ArchiveEntry(name, bytes);
                entries.add(archiveEntry);
            }
        } catch (IOException | IllegalArgumentException cause) {
            throw new InvalidSourceFileException("Не удалось прочитать ZIP-архив", cause);
        }
        if (entries.isEmpty()) {
            throw new InvalidSourceFileException("В ZIP-архиве нет файлов для конвертации");
        }

        return entries;
    }

}
