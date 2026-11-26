package ru.shim.FileConversionService.converter;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class FileTypeResolver {

    public FileType resolve(@NonNull String fileName) {
        int lastSlash = fileName.lastIndexOf('/');
        int lastDot = fileName.lastIndexOf('.');

        if (lastDot == fileName.length() - 1 || lastDot <= lastSlash) {
            throw new UnsupportedFileTypeException("У файла нет расширения: " + fileName);
        }

        String extension = fileName.substring(lastDot + 1).toUpperCase(Locale.ROOT);

        try {
            return FileType.valueOf(extension);
        } catch (IllegalArgumentException cause) {
            throw new UnsupportedFileTypeException(
                    "Неподдерживаемый формат файла: " + fileName
            );
        }
    }
}