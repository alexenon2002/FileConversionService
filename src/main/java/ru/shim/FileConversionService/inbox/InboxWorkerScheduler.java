package ru.shim.FileConversionService.inbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.inbox",
        name = "polling-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class InboxWorkerScheduler {

    private final InboxWorker inboxWorker;

    public InboxWorkerScheduler(InboxWorker inboxWorker) {
        this.inboxWorker = inboxWorker;
    }

    @Scheduled(fixedDelayString = "${app.inbox.poll-delay:1s}")
    public void poll() throws Exception {
        inboxWorker.processNext();
    }
}
