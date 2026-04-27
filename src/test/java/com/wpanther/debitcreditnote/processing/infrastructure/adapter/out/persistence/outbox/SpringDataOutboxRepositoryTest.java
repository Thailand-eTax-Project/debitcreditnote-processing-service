package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence.outbox;

import com.wpanther.saga.domain.outbox.OutboxEvent;
import com.wpanther.saga.domain.outbox.OutboxStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpringDataOutboxRepositoryTest {

    @Mock
    private JpaOutboxEventRepository jpaRepository;

    private SpringDataOutboxRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SpringDataOutboxRepository(jpaRepository);
    }

    @Test
    void save_convertsDomainEventToEntity() {
        OutboxEvent domainEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("ProcessedDebitCreditNote")
                .aggregateId("doc-1")
                .eventType("DEBITCREDITNOTE_REPLY")
                .payload("{\"status\":\"SUCCESS\"}")
                .createdAt(Instant.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        OutboxEventEntity savedEntity = OutboxEventEntity.builder()
                .id(domainEvent.getId())
                .aggregateType(domainEvent.getAggregateType())
                .aggregateId(domainEvent.getAggregateId())
                .eventType(domainEvent.getEventType())
                .payload(domainEvent.getPayload())
                .createdAt(domainEvent.getCreatedAt())
                .status(domainEvent.getStatus())
                .retryCount(domainEvent.getRetryCount())
                .build();

        when(jpaRepository.save(any(OutboxEventEntity.class))).thenReturn(savedEntity);

        OutboxEvent result = repository.save(domainEvent);

        assertNotNull(result);
        assertEquals(domainEvent.getId(), result.getId());
        assertEquals(domainEvent.getAggregateType(), result.getAggregateType());
        verify(jpaRepository).save(any(OutboxEventEntity.class));
    }

    @Test
    void findById_returnsDomainEvent() {
        UUID id = UUID.randomUUID();
        OutboxEventEntity entity = OutboxEventEntity.builder()
                .id(id)
                .aggregateType("Test")
                .aggregateId("test-1")
                .eventType("TEST")
                .payload("{}")
                .createdAt(Instant.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        when(jpaRepository.findById(id)).thenReturn(java.util.Optional.of(entity));

        var result = repository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(java.util.Optional.empty());

        var result = repository.findById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByAggregate_returnsDomainEvents() {
        OutboxEventEntity entity = OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateType("ProcessedDebitCreditNote")
                .aggregateId("doc-1")
                .eventType("TEST")
                .payload("{}")
                .createdAt(Instant.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        when(jpaRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("ProcessedDebitCreditNote", "doc-1"))
                .thenReturn(java.util.List.of(entity));

        var result = repository.findByAggregate("ProcessedDebitCreditNote", "doc-1");

        assertFalse(result.isEmpty());
        assertEquals("ProcessedDebitCreditNote", result.get(0).getAggregateType());
    }

    @Test
    void findByAggregate_returnsEmptyList() {
        when(jpaRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("Test", "none"))
                .thenReturn(java.util.List.of());

        var result = repository.findByAggregate("Test", "none");

        assertTrue(result.isEmpty());
    }
}