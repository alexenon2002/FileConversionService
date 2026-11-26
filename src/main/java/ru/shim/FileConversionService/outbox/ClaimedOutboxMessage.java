package ru.shim.FileConversionService.outbox;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClaimedOutboxMessage(
        UUID eventId,
        String payloadJson,
        OffsetDateTime leaseUntil
) {
}