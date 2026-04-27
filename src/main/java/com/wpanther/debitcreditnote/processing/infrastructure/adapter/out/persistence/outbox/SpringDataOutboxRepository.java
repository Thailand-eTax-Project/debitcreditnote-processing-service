package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence.outbox;

import com.wpanther.saga.domain.outbox.OutboxEvent;
import com.wpanther.saga.domain.outbox.OutboxEventRepository;
import com.wpanther.saga.domain.outbox.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class SpringDataOutboxRepository implements OutboxEventRepository {

    private final JpaOutboxEventRepository jpaRepository;

    public SpringDataOutboxRepository(JpaOutboxEventRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        log.debug("Saving outbox event: {} for aggregate: {}/{}",
                event.getId(), event.getAggregateType(), event.getAggregateId());
        OutboxEventEntity entity = OutboxEventEntity.fromDomain(event);
        OutboxEventEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<OutboxEvent> findById(UUID id) {
        return jpaRepository.findById(id).map(OutboxEventEntity::toDomain);
    }

    @Override
    public List<OutboxEvent> findPendingEvents(int limit) {
        log.debug("Finding pending events with limit: {}", limit);
        return jpaRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, Pageable.ofSize(limit))
                .stream()
                .map(OutboxEventEntity::toDomain)
                .toList();
    }

    @Override
    public List<OutboxEvent> findFailedEvents(int limit) {
        log.debug("Finding failed events with limit: {}", limit);
        return jpaRepository.findFailedEventsOrderByCreatedAtAsc(
                Pageable.ofSize(limit))
                .stream()
                .map(OutboxEventEntity::toDomain)
                .toList();
    }

    @Override
    public int deletePublishedBefore(Instant before) {
        log.debug("Deleting published events before: {}", before);
        int deletedCount = jpaRepository.deletePublishedBefore(before);
        log.info("Deleted {} published events before: {}", deletedCount, before);
        return deletedCount;
    }

    @Override
    public List<OutboxEvent> findByAggregate(String aggregateType, String aggregateId) {
        log.debug("Finding events for aggregate: {}/{}", aggregateType, aggregateId);
        return jpaRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                aggregateType, aggregateId)
                .stream()
                .map(OutboxEventEntity::toDomain)
                .toList();
    }
}
