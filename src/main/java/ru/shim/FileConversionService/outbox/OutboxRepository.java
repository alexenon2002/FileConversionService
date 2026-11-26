package ru.shim.FileConversionService.outbox;

import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.shim.FileConversionService.event.ResponseConversionEvent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OutboxRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OutboxRepository(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String INSERT_RESULT = """
            INSERT INTO outbox_messages (event_id, request_event_id, saga_id, payload)
            VALUES (:eventId, :requestEventId, :sagaId, CAST(:payload AS jsonb))
            """;

    public void insert(@NonNull ResponseConversionEvent resultEvent, String payloadJson) {
        Map<String, Object> parameters = Map.of(
                "eventId", resultEvent.eventId(),
                "requestEventId", resultEvent.requestEventId(),
                "sagaId", resultEvent.sagaId(),
                "payload", payloadJson
        );
        jdbcTemplate.update(INSERT_RESULT, parameters);
    }

    private static final String CLAIM_NEXT = """
            WITH candidate AS (
                SELECT event_id
                FROM outbox_messages
                WHERE status = 'PENDING'
                   OR (status = 'PUBLISHING' AND lease_until <= CURRENT_TIMESTAMP)
                ORDER BY created_at, event_id
                LIMIT 1
                FOR UPDATE SKIP LOCKED
            )
            UPDATE outbox_messages AS outbox
            SET status = 'PUBLISHING',
                publishing_started_at = CURRENT_TIMESTAMP,
                lease_until = CURRENT_TIMESTAMP + INTERVAL '2 minutes'
            FROM candidate
            WHERE outbox.event_id = candidate.event_id
            RETURNING outbox.event_id, outbox.payload, outbox.lease_until
            """;

    public Optional<ClaimedOutboxMessage> claimNext() {
        List<ClaimedOutboxMessage> messages = jdbcTemplate.query(CLAIM_NEXT, (rs, rowNum) ->
                new ClaimedOutboxMessage(
                        rs.getObject("event_id", UUID.class),
                        rs.getString("payload"),
                        rs.getObject("lease_until", OffsetDateTime.class)
                )
        );
        return messages.stream().findFirst();
    }

    private static final String MARK_PUBLISHED = """
            UPDATE outbox_messages
            SET status = 'PUBLISHED',
                published_at = CURRENT_TIMESTAMP,
                lease_until = NULL
            WHERE event_id = :eventId
              AND status = 'PUBLISHING'
              AND lease_until = :leaseUntil
              AND lease_until > clock_timestamp()
            """;

    public boolean markPublished(@NonNull ClaimedOutboxMessage claimed) {
        Map<String, Object> parameters = Map.of(
                "eventId", claimed.eventId(),
                "leaseUntil", claimed.leaseUntil()
        );

        int updatedRows = jdbcTemplate.update(MARK_PUBLISHED, parameters);
        return updatedRows == 1;
    }
}
