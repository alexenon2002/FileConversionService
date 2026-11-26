package ru.shim.FileConversionService.inbox;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClaimedInboxMessage(UUID eventId,
                                  String payloadJson,
                                  OffsetDateTime leaseUntil) {

}
