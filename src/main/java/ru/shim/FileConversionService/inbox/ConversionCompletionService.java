package ru.shim.FileConversionService.inbox;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.shim.FileConversionService.event.ResponseConversionEvent;
import ru.shim.FileConversionService.outbox.OutboxRepository;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ConversionCompletionService {

    private final InboxRepository inboxRepository;
    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;

    public ConversionCompletionService(
            InboxRepository inboxRepository,
            OutboxRepository outboxRepository,
            JsonMapper jsonMapper
    ) {
        this.inboxRepository = inboxRepository;
        this.outboxRepository = outboxRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public boolean finish(@NonNull ClaimedInboxMessage claimed, @NonNull ResponseConversionEvent resultEvent) {
        if (!claimed.eventId().equals(resultEvent.requestEventId())) {
            throw new IllegalArgumentException("Результат относится к другому запросу");
        }

        if (!inboxRepository.markFinished(claimed, resultEvent.status())) {
            return false;
        }

        String payloadJson = jsonMapper.writeValueAsString(resultEvent);
        outboxRepository.insert(resultEvent, payloadJson);
        return true;
    }
}