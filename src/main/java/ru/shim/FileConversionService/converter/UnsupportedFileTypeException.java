package ru.shim.FileConversionService.converter;

public class UnsupportedFileTypeException extends IllegalArgumentException {
    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}
