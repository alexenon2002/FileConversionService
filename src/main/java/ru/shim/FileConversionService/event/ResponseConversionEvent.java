package ru.shim.FileConversionService.event;

import ru.shim.FileConversionService.storage.StorageLocation;

import java.util.UUID;

public record ResponseConversionEvent(UUID eventId,
                                      UUID requestEventId,
                                      UUID sagaId,
                                      StorageLocation result,
                                      ConversionError error,
                                      ConversionStatus status
                           ) {
}
