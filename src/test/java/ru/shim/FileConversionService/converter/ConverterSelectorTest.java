package ru.shim.FileConversionService.converter;

import org.junit.jupiter.api.Test;


import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConverterSelectorTest {

    private final Converter textConverter = new TXTConverter();
    private final Converter imageConverter = new ImageConverter();
    private final ConverterSelector selector =
            new ConverterSelector(List.of(imageConverter, textConverter));

    @Test
    public void selectsTextConverterForTxt() {
        assertSame(textConverter, selector.select(FileType.TXT));
    }

    @Test
    public void selectsImageConverterForPngAndJpg() {
        assertSame(imageConverter, selector.select(FileType.PNG));
        assertSame(imageConverter, selector.select(FileType.JPG));
    }

    @Test
    public void throwsWhenNoConverterSupportsType() {
        assertThrows(IllegalArgumentException.class, () -> selector.select(FileType.ZIP));
    }
}