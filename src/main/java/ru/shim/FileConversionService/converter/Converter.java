package ru.shim.FileConversionService.converter;

import ru.shim.FileConversionService.converter.FileType;

import java.io.IOException;

public interface Converter {
    boolean supports(FileType fileType);
    byte[] convert(byte[] source) throws IOException;
}
