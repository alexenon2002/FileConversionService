package ru.shim.FileConversionService.kafka;

import tools.jackson.databind.json.JsonMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.shim.FileConversionService.event.RequestConversionEvent;
import ru.shim.FileConversionService.inbox.InboxRepository;

@Component
public class ConversionRequestListener {
    private final JsonMapper jsonMapper;
    private final InboxRepository inboxRepository;

    public ConversionRequestListener(JsonMapper jsonMapper, InboxRepository inboxRepository) {
        this.jsonMapper = jsonMapper;
        this.inboxRepository = inboxRepository;
    }

    @KafkaListener(topics = "${app.kafka.request-topic}")
    public void receive(String payloadJson) {
        RequestConversionEvent event =
                jsonMapper.readValue(payloadJson, RequestConversionEvent.class);
        inboxRepository.saveIfAbsent(event.eventId(), payloadJson);
    }

}
