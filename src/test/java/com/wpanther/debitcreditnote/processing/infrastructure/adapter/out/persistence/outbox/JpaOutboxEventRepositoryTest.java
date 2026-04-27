package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence.outbox;

import com.wpanther.saga.domain.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JpaOutboxEventRepository
 */
@DataJpaTest
@ActiveProfiles("test")
class JpaOutboxEventRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaOutboxEventRepository jpaOutboxEventRepository;

    @Test
    void testSaveOutboxEvent() {
        // Given
        OutboxEventEntity event = OutboxEventEntity.builder()
                .aggregateType("ProcessedDebitCreditNote")
                .aggregateId(UUID.randomUUID().toString())
                .eventType("debitcreditnote.processed")
                .payload("{\"test\": \"data\"}")
                .status(OutboxStatus.PENDING)
                .topic("debitcreditnote.processed")
                .partitionKey("dcn-123")
                .headers("{\"correlationId\": \"test-123\"}")
                .build();

        // When
        OutboxEventEntity saved = jpaOutboxEventRepository.save(event);

        // Then
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("ProcessedDebitCreditNote", saved.getAggregateType());
        assertEquals("debitcreditnote.processed", saved.getEventType());
        assertEquals(OutboxStatus.PENDING, saved.getStatus());
        assertEquals("debitcreditnote.processed", saved.getTopic());
        assertEquals("dcn-123", saved.getPartitionKey());
    }

    @Test
    void testFindById() {
        // Given
        OutboxEventEntity event = OutboxEventEntity.builder()
                .aggregateType("ProcessedDebitCreditNote")
                .aggregateId(UUID.randomUUID().toString())
                .eventType("debitcreditnote.processed")
                .payload("{\"test\": \"data\"}")
                .status(OutboxStatus.PENDING)
                .topic("debitcreditnote.processed")
                .partitionKey("dcn-123")
                .headers("{\"correlationId\": \"test-123\"}")
                .build();
        entityManager.persist(event);
        entityManager.flush();

        // When
        Optional<OutboxEventEntity> found = jpaOutboxEventRepository.findById(event.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals(event.getId(), found.get().getId());
        assertEquals("ProcessedDebitCreditNote", found.get().getAggregateType());
    }

    @Test
    void testUpdateStatus() {
        // Given
        OutboxEventEntity event = OutboxEventEntity.builder()
                .aggregateType("ProcessedDebitCreditNote")
                .aggregateId(UUID.randomUUID().toString())
                .eventType("debitcreditnote.processed")
                .payload("{\"test\": \"data\"}")
                .status(OutboxStatus.PENDING)
                .topic("debitcreditnote.processed")
                .partitionKey("dcn-123")
                .headers("{\"correlationId\": \"test-123\"}")
                .build();
        entityManager.persist(event);
        entityManager.flush();

        // When
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        jpaOutboxEventRepository.save(event);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<OutboxEventEntity> updated = jpaOutboxEventRepository.findById(event.getId());
        assertTrue(updated.isPresent());
        assertEquals(OutboxStatus.PUBLISHED, updated.get().getStatus());
        assertNotNull(updated.get().getPublishedAt());
    }

    @Test
    void testFindByStatusOrderByCreatedAtAsc() {
        // Given
        Instant baseTime = Instant.now();
        OutboxEventEntity event1 = createOutboxEvent(OutboxStatus.PENDING, baseTime.minusSeconds(100));
        OutboxEventEntity event2 = createOutboxEvent(OutboxStatus.PUBLISHED, baseTime.minusSeconds(90));
        OutboxEventEntity event3 = createOutboxEvent(OutboxStatus.PENDING, baseTime.minusSeconds(80));
        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);
        entityManager.flush();

        // When
        List<OutboxEventEntity> pendingEvents = jpaOutboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, 10)
        );

        // Then
        assertEquals(2, pendingEvents.size());
        assertEquals(event1.getId(), pendingEvents.get(0).getId());
        assertEquals(event3.getId(), pendingEvents.get(1).getId());
    }

    @Test
    void testDeleteById() {
        // Given
        OutboxEventEntity event = OutboxEventEntity.builder()
                .aggregateType("ProcessedDebitCreditNote")
                .aggregateId(UUID.randomUUID().toString())
                .eventType("debitcreditnote.processed")
                .payload("{\"test\": \"data\"}")
                .status(OutboxStatus.PENDING)
                .topic("debitcreditnote.processed")
                .partitionKey("dcn-123")
                .headers("{\"correlationId\": \"test-123\"}")
                .build();
        entityManager.persist(event);
        entityManager.flush();

        // When
        jpaOutboxEventRepository.deleteById(event.getId());
        entityManager.flush();

        // Then
        Optional<OutboxEventEntity> deleted = jpaOutboxEventRepository.findById(event.getId());
        assertFalse(deleted.isPresent());
    }

    @Test
    void testFindByAggregateTypeAndAggregateIdOrderByCreatedAtAsc() {
        // Given
        String aggregateId = UUID.randomUUID().toString();
        String aggregateType = "ProcessedDebitCreditNote";
        OutboxEventEntity event1 = OutboxEventEntity.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType("debitcreditnote.processed")
                .payload("{\"test\": \"data\"}")
                .status(OutboxStatus.PENDING)
                .topic("debitcreditnote.processed")
                .partitionKey("dcn-123")
                .headers("{\"correlationId\": \"test-123\"}")
                .build();
        OutboxEventEntity event2 = OutboxEventEntity.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType("debitcreditnote.saga.reply")
                .payload("{\"test\": \"data2\"}")
                .status(OutboxStatus.PUBLISHED)
                .topic("debitcreditnote.saga.reply")
                .partitionKey("dcn-123")
                .headers("{\"correlationId\": \"test-123\"}")
                .build();
        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.flush();

        // When
        List<OutboxEventEntity> events = jpaOutboxEventRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                aggregateType, aggregateId
        );

        // Then
        assertEquals(2, events.size());
    }

    @Test
    void testDeletePublishedBefore() {
        // Given
        Instant baseTime = Instant.now();
        OutboxEventEntity oldEvent = OutboxEventEntity.builder()
                .aggregateType("ProcessedDebitCreditNote")
                .aggregateId(UUID.randomUUID().toString())
                .eventType("debitcreditnote.processed")
                .payload("{\"test\": \"data\"}")
                .status(OutboxStatus.PUBLISHED)
                .topic("debitcreditnote.processed")
                .partitionKey("dcn-123")
                .headers("{\"correlationId\": \"test-123\"}")
                .publishedAt(baseTime.minusSeconds(3600))
                .createdAt(baseTime.minusSeconds(3700))
                .build();
        OutboxEventEntity recentEvent = OutboxEventEntity.builder()
                .aggregateType("ProcessedDebitCreditNote")
                .aggregateId(UUID.randomUUID().toString())
                .eventType("debitcreditnote.processed")
                .payload("{\"test\": \"data2\"}")
                .status(OutboxStatus.PUBLISHED)
                .topic("debitcreditnote.processed")
                .partitionKey("dcn-456")
                .headers("{\"correlationId\": \"test-456\"}")
                .publishedAt(baseTime.minusSeconds(100))
                .createdAt(baseTime.minusSeconds(200))
                .build();
        entityManager.persist(oldEvent);
        entityManager.persist(recentEvent);
        entityManager.flush();

        // When
        int deleted = jpaOutboxEventRepository.deletePublishedBefore(baseTime.minusSeconds(300));

        // Then
        assertEquals(1, deleted);
        assertFalse(jpaOutboxEventRepository.existsById(oldEvent.getId()));
        assertTrue(jpaOutboxEventRepository.existsById(recentEvent.getId()));
    }

    private OutboxEventEntity createOutboxEvent(OutboxStatus status, Instant createdAt) {
        return OutboxEventEntity.builder()
                .aggregateType("ProcessedDebitCreditNote")
                .aggregateId(UUID.randomUUID().toString())
                .eventType("debitcreditnote.processed")
                .payload("{\"test\": \"data\"}")
                .status(status)
                .topic("debitcreditnote.processed")
                .partitionKey("dcn-123")
                .headers("{\"correlationId\": \"test-123\"}")
                .createdAt(createdAt)
                .build();
    }
}
