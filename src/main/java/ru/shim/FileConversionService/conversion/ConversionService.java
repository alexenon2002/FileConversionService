package ru.shim.FileConversionService.conversion;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import ru.shim.FileConversionService.converter.FileType;
import ru.shim.FileConversionService.storage.MinioStorage;
import ru.shim.FileConversionService.converter.Converter;
import ru.shim.FileConversionService.converter.ConverterSelector;
import ru.shim.FileConversionService.converter.FileTypeResolver;
import ru.shim.FileConversionService.storage.StorageLocation;

@Service
public class ConversionService {
    private final MinioStorage storage;
    private final FileTypeResolver fileTypeResolver;
    private final ConverterSelector converterSelector;

    public ConversionService(MinioStorage storage,
                             FileTypeResolver resolver,
                             ConverterSelector selector) {
        this.storage = storage;
        this.fileTypeResolver = resolver;
        this.converterSelector = selector;
    }

    public void convert(@NonNull StorageLocation source, StorageLocation destination)
            throws Exception {
        FileType fileType = fileTypeResolver.resolve(source.key());
        Converter converter = converterSelector.select(fileType);
        byte[] sourceBytes = storage.download(source);
        byte[] pdfBytes = converter.convert(sourceBytes);
        storage.upload(destination, pdfBytes, "application/pdf");
    }
}
