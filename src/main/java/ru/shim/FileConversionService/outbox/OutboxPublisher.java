package ru.shim.FileConversionService.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String resultTopic;

    public OutboxPublisher(
            OutboxRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.result-topic}") String resultTopic
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.resultTopic = resultTopic;
    }

    public boolean publishNext()
            throws InterruptedException, ExecutionException, TimeoutException {
        Optional<ClaimedOutboxMessage> next = outboxRepository.claimNext();
        if (next.isEmpty()) {
            return false;
        }

        ClaimedOutboxMessage claimed = next.orElseThrow();
        kafkaTemplate.send(
                resultTopic,
                claimed.eventId().toString(),
                claimed.payloadJson()
        ).get(30, TimeUnit.SECONDS);

        return outboxRepository.markPublished(claimed);
    }
}
