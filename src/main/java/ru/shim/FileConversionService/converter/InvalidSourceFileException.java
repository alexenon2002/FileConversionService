package ru.shim.FileConversionService.converter;

import java.io.IOException;

public class InvalidSourceFileException extends IOException {
    public InvalidSourceFileException(String message,Throwable cause) {
        super(message,cause);
    }
    public InvalidSourceFileException(String message) {
        super(message);
    }
}
