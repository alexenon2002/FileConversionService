package ru.shim.FileConversionService.inbox;

import org.springframework.stereotype.Component;
import ru.shim.FileConversionService.conversion.ConversionService;
import ru.shim.FileConversionService.converter.InvalidSourceFileException;
import ru.shim.FileConversionService.converter.UnsupportedFileTypeException;
import ru.shim.FileConversionService.event.ConversionError;
import ru.shim.FileConversionService.event.ConversionStatus;
import ru.shim.FileConversionService.event.RequestConversionEvent;
import ru.shim.FileConversionService.event.ResponseConversionEvent;
import ru.shim.FileConversionService.storage.StorageLocation;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

import ru.shim.FileConversionService.storage.SourceFileNotFoundException;

@Component
public class InboxWorker {

    private final InboxRepository inboxRepository;
    private final JsonMapper jsonMapper;
    private final ConversionService conversionService;
    private final ConversionCompletionService completionService;

    public InboxWorker(
            InboxRepository inboxRepository,
            JsonMapper jsonMapper,
            ConversionService conversionService,
            ConversionCompletionService completionService
    ) {
        this.inboxRepository = inboxRepository;
        this.jsonMapper = jsonMapper;
        this.conversionService = conversionService;
        this.completionService = completionService;
    }

    public boolean processNext() throws Exception {
        Optional<ClaimedInboxMessage> next = inboxRepository.claimNext();
        if (next.isEmpty()) {
            return false;
        }

        ClaimedInboxMessage claimed = next.orElseThrow();
        RequestConversionEvent request =
                jsonMapper.readValue(claimed.payloadJson(), RequestConversionEvent.class);

        UUID resultEventId = UUID.randomUUID();
        StorageLocation destination = new StorageLocation(
                "uploads",
                "results/" + claimed.eventId() + "/" + resultEventId + ".pdf"
        );

        ConversionError error = null;
        try {
            conversionService.convert(request.source(), destination);
        } catch (UnsupportedFileTypeException
                 | InvalidSourceFileException
                 | SourceFileNotFoundException exception) {
            String errorCode;
            if (exception instanceof UnsupportedFileTypeException) {
                errorCode = "UNSUPPORTED_FILE_TYPE";
            } else if (exception instanceof SourceFileNotFoundException) {
                errorCode = "SOURCE_FILE_NOT_FOUND";
            } else {
                errorCode = "INVALID_SOURCE_FILE";
            }

            error = new ConversionError(errorCode, exception.getMessage());

        }

        boolean successful = error == null;
        StorageLocation resultLocation = successful ? destination : null;
        ConversionStatus status = successful ? ConversionStatus.SUCCESS : ConversionStatus.FAILED;

        ResponseConversionEvent resultEvent = new ResponseConversionEvent(
                resultEventId,
                claimed.eventId(),
                request.sagaId(),
                resultLocation,
                error,
                status
        );

        return completionService.finish(claimed, resultEvent);
    }
}