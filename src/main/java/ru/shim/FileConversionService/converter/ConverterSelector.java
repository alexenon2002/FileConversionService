package ru.shim.FileConversionService.converter;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConverterSelector {

    private final List<Converter> converters;

    public ConverterSelector(List<Converter> converters) {
        this.converters = converters;
    }

    public Converter select(FileType fileType) {
        for (Converter converter : converters) {
            if (converter.supports(fileType)) {
                return converter;
            }
        }
        throw new IllegalArgumentException("Нет конвертера для формата " + fileType);
    }
}
