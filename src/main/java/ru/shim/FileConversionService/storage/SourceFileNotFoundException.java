package ru.shim.FileConversionService.storage;

import java.io.IOException;

public class SourceFileNotFoundException extends IOException {
    public SourceFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
