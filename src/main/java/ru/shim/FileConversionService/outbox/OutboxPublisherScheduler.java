package ru.shim.FileConversionService.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.outbox",
        name = "publishing-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxPublisherScheduler {

    private final OutboxPublisher outboxPublisher;

    public OutboxPublisherScheduler(OutboxPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay:1s}")
    public void publish() throws Exception {
        outboxPublisher.publishNext();
    }
}
