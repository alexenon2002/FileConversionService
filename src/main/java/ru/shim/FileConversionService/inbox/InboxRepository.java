package ru.shim.FileConversionService.inbox;

import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.shim.FileConversionService.event.ConversionStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InboxRepository {

    private static final String INSERT_IF_ABSENT = """
            INSERT INTO inbox_messages (event_id, payload, status)
            VALUES (:eventId, CAST(:payload AS jsonb), 'RECEIVED')
            ON CONFLICT (event_id) DO NOTHING
            """;

    private static final String CLAIM_NEXT = """
            WITH candidate AS (
                SELECT event_id
                FROM inbox_messages
                WHERE status = 'RECEIVED'
                   OR (status = 'PROCESSING' AND lease_until <= CURRENT_TIMESTAMP)
                ORDER BY received_at, event_id
                LIMIT 1
                FOR UPDATE SKIP LOCKED
            )
            UPDATE inbox_messages AS inbox
            SET status = 'PROCESSING',
                started_at = CURRENT_TIMESTAMP,
                lease_until = CURRENT_TIMESTAMP + INTERVAL '2 minutes'
            FROM candidate
            WHERE inbox.event_id = candidate.event_id
            RETURNING inbox.event_id, inbox.payload, inbox.lease_until
            """;

    private final NamedParameterJdbcTemplate template;

    public InboxRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public boolean saveIfAbsent(UUID eventId, String payloadJson) {
        Map<String, Object> parameters = Map.of(
                "eventId", eventId,
                "payload", payloadJson
        );
        int insertedRows = template.update(INSERT_IF_ABSENT, parameters);

        return insertedRows == 1;
    }

    public Optional<ClaimedInboxMessage> claimNext() {
        List<ClaimedInboxMessage> messages = template.query(CLAIM_NEXT, (rs, rowNum) -> {
            UUID eventId = rs.getObject("event_id", UUID.class);
            String payloadJson = rs.getString("payload");
            OffsetDateTime leaseUntil = rs.getObject("lease_until", OffsetDateTime.class);
            return new ClaimedInboxMessage(eventId, payloadJson, leaseUntil);
        });
        return messages.stream().findFirst();
    }

    private static final String MARK_FINISHED = """
            UPDATE inbox_messages
            SET status = :status,
                processed_at = CURRENT_TIMESTAMP,
                lease_until = NULL
            WHERE event_id = :eventId
              AND status = 'PROCESSING'
              AND lease_until = :leaseUntil
              AND lease_until > clock_timestamp()
            """;

    public boolean markFinished(
            ClaimedInboxMessage claimed,
            @NonNull ConversionStatus conversionStatus
    ) {
        String inboxStatus = switch (conversionStatus) {
            case SUCCESS -> "COMPLETED";
            case FAILED -> "FAILED";
        };

        Map<String, Object> parameters = Map.of(
                "status", inboxStatus,
                "eventId", claimed.eventId(),
                "leaseUntil", claimed.leaseUntil()
        );

        int updatedRows = template.update(MARK_FINISHED, parameters);
        return updatedRows == 1;
    }
}
