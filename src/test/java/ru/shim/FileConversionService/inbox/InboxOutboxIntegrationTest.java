package ru.shim.FileConversionService.inbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.shim.FileConversionService.storage.StorageLocation;
import tools.jackson.databind.json.JsonMapper;
import ru.shim.FileConversionService.event.ConversionStatus;
import ru.shim.FileConversionService.event.RequestConversionEvent;
import ru.shim.FileConversionService.event.ResponseConversionEvent;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@ActiveProfiles("test")
public class InboxOutboxIntegrationTest {

    @Autowired
    private InboxRepository inboxRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private ConversionCompletionService service;

    @Autowired
    private JsonMapper jsonMapper;

    private String createRequestJson(UUID eventId, UUID sagaId) {
        RequestConversionEvent request = new RequestConversionEvent(
                eventId,
                sagaId,
                new StorageLocation("uploads", "tests/inbox-fixture.txt")
        );
        return jsonMapper.writeValueAsString(request);
    }

    @Test
    @Transactional
    public void savesInboxEventOnlyOnce() {
        UUID eventId = UUID.randomUUID();
        String payloadJson = createRequestJson(eventId, UUID.randomUUID());
        assertTrue(inboxRepository.saveIfAbsent(eventId, payloadJson));
        assertFalse(inboxRepository.saveIfAbsent(eventId, payloadJson));
    }

    @Test
    @Transactional
    public void claimsNextInboxMessage() {
        jdbcTemplate.getJdbcOperations().execute("""
                CREATE TEMP TABLE inbox_messages
                (LIKE public.inbox_messages INCLUDING ALL)
                ON COMMIT DROP
                """);
        UUID eventId = UUID.randomUUID();
        String payloadJson = createRequestJson(eventId, UUID.randomUUID());
        assertTrue(inboxRepository.saveIfAbsent(eventId, payloadJson));

        var claimed = inboxRepository.claimNext().orElseThrow();
        assertEquals(eventId, claimed.eventId());
        assertNotNull(claimed.leaseUntil());
        assertTrue(inboxRepository.claimNext().isEmpty());
    }

    @Test
    void rollsBackInboxWhenOutboxInsertFails() {
        UUID requestId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        Map<String, UUID> requestParameters = Map.of("requestId", requestId);

        String payloadJson = createRequestJson(requestId, sagaId);

        try {
            OffsetDateTime leaseUntil = jdbcTemplate.queryForObject("""
                            INSERT INTO inbox_messages (event_id, payload, status, started_at, lease_until)
                            VALUES (:requestId, CAST(:payload AS jsonb), 'PROCESSING',
                                    CURRENT_TIMESTAMP, clock_timestamp() + INTERVAL '5 minutes')
                            RETURNING lease_until
                            """, Map.of("requestId", requestId, "payload", payloadJson),
                    (rs, rowNum) -> rs.getObject("lease_until", OffsetDateTime.class));

            jdbcTemplate.update("""
                    INSERT INTO outbox_messages
                        (event_id, request_event_id, saga_id, payload, status, published_at)
                    VALUES (:eventId, :requestId, :sagaId, CAST('{}' AS jsonb),
                            'PUBLISHED', CURRENT_TIMESTAMP)
                    """, Map.of(
                    "eventId", UUID.randomUUID(),
                    "requestId", requestId,
                    "sagaId", sagaId
            ));

            var claimed = new ClaimedInboxMessage(requestId, payloadJson, leaseUntil);
            var resultEvent = new ResponseConversionEvent(
                    UUID.randomUUID(),
                    requestId,
                    sagaId,
                    new StorageLocation("uploads", "tests/rollback.pdf"),
                    null,
                    ConversionStatus.SUCCESS
            );

            assertThrows(DuplicateKeyException.class, () -> service.finish(claimed, resultEvent));

            var inboxRow = jdbcTemplate.queryForMap("""
                    SELECT status, processed_at, lease_until = :leaseUntil AS same_lease
                    FROM inbox_messages
                    WHERE event_id = :requestId
                    """, Map.of("requestId", requestId, "leaseUntil", leaseUntil));

            assertEquals("PROCESSING", inboxRow.get("status"));
            assertNull(inboxRow.get("processed_at"));
            assertEquals(Boolean.TRUE, inboxRow.get("same_lease"));
        } finally {
            jdbcTemplate.update("""
                    DELETE FROM outbox_messages WHERE request_event_id = :requestId
                    """, requestParameters);
            jdbcTemplate.update("""
                    DELETE FROM inbox_messages WHERE event_id = :requestId
                    """, requestParameters);
        }
    }
}
