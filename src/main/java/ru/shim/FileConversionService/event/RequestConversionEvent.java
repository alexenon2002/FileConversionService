package ru.shim.FileConversionService.event;

import ru.shim.FileConversionService.storage.StorageLocation;

import java.util.UUID;

public record RequestConversionEvent(UUID eventId,
                                     UUID sagaId,
                                     StorageLocation source) {
}
