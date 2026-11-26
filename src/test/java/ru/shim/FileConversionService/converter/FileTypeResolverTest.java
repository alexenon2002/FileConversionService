package ru.shim.FileConversionService.converter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileTypeResolverTest {
    @Test
    void resolvesTxtFromPath() {
        FileTypeResolver resolver = new FileTypeResolver();
        String fileName = "docs/note.txt";

        FileType actual = resolver.resolve(fileName);

        assertEquals(FileType.TXT, actual);
    }

    @Test
    void resolvesJpgFromPath() {
        FileTypeResolver resolver = new FileTypeResolver();
        String fileName = "IMAGE.JPG";

        FileType actual = resolver.resolve(fileName);

        assertEquals(FileType.JPG, actual);
    }

    @Test
    void rejectsFileNameWithTrailingDot() {
        FileTypeResolver resolver = new FileTypeResolver();
        String fileName = "note.";

        var error = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolve(fileName);
        });
        assertEquals("У файла нет расширения: " + fileName, error.getMessage());
    }

    @Test
    void rejectsFileNameWithTrailingSlash() {
        FileTypeResolver resolver = new FileTypeResolver();
        String fileName = "docs.v1/note";

        var error = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolve(fileName);
        });
        assertEquals("У файла нет расширения: " + fileName, error.getMessage());
    }

    @Test
    void rejectsFileNameWithUnsupportedType() {
        FileTypeResolver resolver = new FileTypeResolver();
        String fileName = "photo.bmp";

        var error = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolve(fileName);
        });
        assertEquals("Неподдерживаемый формат файла: " + fileName, error.getMessage());
    }


}