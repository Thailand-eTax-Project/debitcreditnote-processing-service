package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence.outbox;

import com.wpanther.saga.domain.outbox.OutboxEvent;
import com.wpanther.saga.domain.outbox.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OutboxEventEntity Tests")
class OutboxEventEntityTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build entity with all fields")
        void testBuilderWithAllFields() {
            // Arrange
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();

            // Act
            OutboxEventEntity entity = OutboxEventEntity.builder()
                .id(id)
                .aggregateType("ProcessedDebitCreditNote")
                .aggregateId("dcn-123")
                .eventType("DebitCreditNoteProcessedEvent")
                .payload("{\"key\":\"value\"}")
                .createdAt(now)
                .publishedAt(now.plusSeconds(5))
                .status(OutboxStatus.PUBLISHED)
                .retryCount(0)
                .errorMessage(null)
                .topic("debitcreditnote.processed")
                .partitionKey("dcn-123")
                .headers("{\"correlationId\":\"corr-123\"}")
                .build();

            // Assert
            assertNotNull(entity);
            assertEquals(id, entity.getId());
            assertEquals("ProcessedDebitCreditNote", entity.getAggregateType());
            assertEquals("dcn-123", entity.getAggregateId());
            assertEquals("DebitCreditNoteProcessedEvent", entity.getEventType());
            assertEquals("{\"key\":\"value\"}", entity.getPayload());
            assertEquals(now, entity.getCreatedAt());
            assertEquals(now.plusSeconds(5), entity.getPublishedAt());
            assertEquals(OutboxStatus.PUBLISHED, entity.getStatus());
            assertEquals(0, entity.getRetryCount());
            assertNull(entity.getErrorMessage());
            assertEquals("debitcreditnote.processed", entity.getTopic());
            assertEquals("dcn-123", entity.getPartitionKey());
            assertEquals("{\"correlationId\":\"corr-123\"}", entity.getHeaders());
        }

        @Test
        @DisplayName("Should build entity with minimal fields")
        void testBuilderWithMinimalFields() {
            // Act
            OutboxEventEntity entity = OutboxEventEntity.builder()
                .aggregateType("ProcessedDebitCreditNote")
                .aggregateId("dcn-456")
                .eventType("DebitCreditNoteProcessedEvent")
                .payload("{}")
                .build();

            // Assert
            assertNotNull(entity);
            assertEquals("ProcessedDebitCreditNote", entity.getAggregateType());
            assertEquals("dcn-456", entity.getAggregateId());
            assertEquals("DebitCreditNoteProcessedEvent", entity.getEventType());
            assertEquals("{}", entity.getPayload());
        }
    }

    @Nested
    @DisplayName("@PrePersist Tests")
    class PrePersistTests {

        @Test
        @DisplayName("Should generate default values on create")
        void testPrePersistDefaults() {
            // Arrange
            OutboxEventEntity entity = new OutboxEventEntity();
            entity.setAggregateType("ProcessedDebitCreditNote");
            entity.setAggregateId("dcn-789");
            entity.setEventType("DebitCreditNoteProcessedEvent");
            entity.setPayload("{\"test\":true}");

            assertNull(entity.getId(), "ID should be null before onCreate");
            assertNull(entity.getStatus(), "Status should be null before onCreate");
            assertNull(entity.getCreatedAt(), "CreatedAt should be null before onCreate");
            assertNull(entity.getRetryCount(), "RetryCount should be null before onCreate");

            // Act
            entity.onCreate();

            // Assert
            assertNotNull(entity.getId(), "ID should be generated on create");
            assertEquals(OutboxStatus.PENDING, entity.getStatus(), "Status should default to PENDING");
            assertNotNull(entity.getCreatedAt(), "CreatedAt should be set on create");
            assertEquals(0, entity.getRetryCount(), "RetryCount should default to 0");
        }

        @Test
        @DisplayName("Should not override existing values in onCreate")
        void testPrePersistDoesNotOverride() {
            // Arrange
            UUID existingId = UUID.randomUUID();
            Instant existingCreatedAt = Instant.now().minusSeconds(60);
            OutboxStatus existingStatus = OutboxStatus.PUBLISHED;
            Integer existingRetryCount = 3;

            OutboxEventEntity entity = new OutboxEventEntity();
            entity.setId(existingId);
            entity.setAggregateType("ProcessedDebitCreditNote");
            entity.setAggregateId("dcn-999");
            entity.setEventType("DebitCreditNoteProcessedEvent");
            entity.setPayload("{\"test\":true}");
            entity.setCreatedAt(existingCreatedAt);
            entity.setStatus(existingStatus);
            entity.setRetryCount(existingRetryCount);

            // Act
            entity.onCreate();

            // Assert
            assertEquals(existingId, entity.getId(), "Existing ID should not be overridden");
            assertEquals(existingCreatedAt, entity.getCreatedAt(), "Existing createdAt should not be overridden");
            assertEquals(existingStatus, entity.getStatus(), "Existing status should not be overridden");
            assertEquals(existingRetryCount, entity.getRetryCount(), "Existing retryCount should not be overridden");
        }
    }

    @Nested
    @DisplayName("Domain Conversion Tests")
    class DomainConversionTests {

        @Test
        @DisplayName("Should convert from domain OutboxEvent")
        void testFromDomain() {
            // Arrange
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();

            OutboxEvent domainEvent = OutboxEvent.builder()
                .id(id)
                .aggregateType("ProcessedDebitCreditNote")
                .aggregateId("dcn-from-domain")
                .eventType("DebitCreditNoteProcessedEvent")
                .payload("{\"processed\":true}")
                .createdAt(now)
                .publishedAt(now.plusSeconds(10))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .errorMessage(null)
                .topic("debitcreditnote.processed")
                .partitionKey("dcn-from-domain")
                .headers("{\"traceId\":\"trace-456\"}")
                .build();

            // Act
            OutboxEventEntity entity = OutboxEventEntity.fromDomain(domainEvent);

            // Assert
            assertNotNull(entity);
            assertEquals(id, entity.getId());
            assertEquals("ProcessedDebitCreditNote", entity.getAggregateType());
            assertEquals("dcn-from-domain", entity.getAggregateId());
            assertEquals("DebitCreditNoteProcessedEvent", entity.getEventType());
            assertEquals("{\"processed\":true}", entity.getPayload());
            assertEquals(now, entity.getCreatedAt());
            assertEquals(now.plusSeconds(10), entity.getPublishedAt());
            assertEquals(OutboxStatus.PENDING, entity.getStatus());
            assertEquals(0, entity.getRetryCount());
            assertNull(entity.getErrorMessage());
            assertEquals("debitcreditnote.processed", entity.getTopic());
            assertEquals("dcn-from-domain", entity.getPartitionKey());
            assertEquals("{\"traceId\":\"trace-456\"}", entity.getHeaders());
        }

        @Test
        @DisplayName("Should convert to domain OutboxEvent")
        void testToDomain() {
            // Arrange
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();

            OutboxEventEntity entity = OutboxEventEntity.builder()
                .id(id)
                .aggregateType("ProcessedDebitCreditNote")
                .aggregateId("dcn-to-domain")
                .eventType("DebitCreditNoteProcessedEvent")
                .payload("{\"signing\":true}")
                .createdAt(now)
                .status(OutboxStatus.PUBLISHED)
                .retryCount(1)
                .errorMessage("Temporary error")
                .topic("debitcreditnote.processed")
                .partitionKey("dcn-to-domain")
                .headers("{\"key\":\"value\"}")
                .build();

            // Act
            OutboxEvent domainEvent = entity.toDomain();

            // Assert
            assertNotNull(domainEvent);
            assertEquals(id, domainEvent.getId());
            assertEquals("ProcessedDebitCreditNote", domainEvent.getAggregateType());
            assertEquals("dcn-to-domain", domainEvent.getAggregateId());
            assertEquals("DebitCreditNoteProcessedEvent", domainEvent.getEventType());
            assertEquals("{\"signing\":true}", domainEvent.getPayload());
            assertEquals(now, domainEvent.getCreatedAt());
            assertEquals(OutboxStatus.PUBLISHED, domainEvent.getStatus());
            assertEquals(1, domainEvent.getRetryCount());
            assertEquals("Temporary error", domainEvent.getErrorMessage());
            assertEquals("debitcreditnote.processed", domainEvent.getTopic());
            assertEquals("dcn-to-domain", domainEvent.getPartitionKey());
            assertEquals("{\"key\":\"value\"}", domainEvent.getHeaders());
        }

        @Test
        @DisplayName("Should support round-trip conversion")
        void testRoundTripConversion() {
            // Arrange
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();

            OutboxEvent originalDomain = OutboxEvent.builder()
                .id(id)
                .aggregateType("ProcessedDebitCreditNote")
                .aggregateId("dcn-roundtrip")
                .eventType("DebitCreditNoteProcessedEvent")
                .payload("{\"roundtrip\":true}")
                .createdAt(now)
                .publishedAt(now.plusSeconds(15))
                .status(OutboxStatus.PUBLISHED)
                .retryCount(0)
                .errorMessage(null)
                .topic("debitcreditnote.processed")
                .partitionKey("dcn-roundtrip")
                .headers("{\"traceId\":\"trace-789\"}")
                .build();

            // Act
            OutboxEventEntity entity = OutboxEventEntity.fromDomain(originalDomain);
            OutboxEvent restoredDomain = entity.toDomain();

            // Assert
            assertEquals(originalDomain, restoredDomain, "Round-trip conversion should preserve all fields");
        }

        @Test
        @DisplayName("Should convert from domain with null optional fields")
        void testFromDomainWithNullOptionalFields() {
            // Arrange
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();

            OutboxEvent domainEvent = OutboxEvent.builder()
                .id(id)
                .aggregateType("ProcessedDebitCreditNote")
                .aggregateId("dcn-minimal")
                .eventType("DebitCreditNoteProcessedEvent")
                .payload("{\"minimal\":true}")
                .createdAt(now)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .publishedAt(null)
                .errorMessage(null)
                .topic(null)
                .partitionKey(null)
                .headers(null)
                .build();

            // Act
            OutboxEventEntity entity = OutboxEventEntity.fromDomain(domainEvent);

            // Assert
            assertNotNull(entity);
            assertEquals(id, entity.getId());
            assertEquals("ProcessedDebitCreditNote", entity.getAggregateType());
            assertEquals("dcn-minimal", entity.getAggregateId());
            assertEquals("DebitCreditNoteProcessedEvent", entity.getEventType());
            assertEquals("{\"minimal\":true}", entity.getPayload());
            assertEquals(now, entity.getCreatedAt());
            assertEquals(OutboxStatus.PENDING, entity.getStatus());
            assertEquals(0, entity.getRetryCount());
            assertNull(entity.getPublishedAt());
            assertNull(entity.getErrorMessage());
            assertNull(entity.getTopic());
            assertNull(entity.getPartitionKey());
            assertNull(entity.getHeaders());
        }
    }

    @Nested
    @DisplayName("Getters and Setters Tests")
    class GettersSettersTests {

        @Test
        @DisplayName("Should set and get all fields")
        void testSettersAndGetters() {
            // Arrange
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            OutboxEventEntity entity = new OutboxEventEntity();

            // Act
            entity.setId(id);
            entity.setAggregateType("ProcessedDebitCreditNote");
            entity.setAggregateId("dcn-setters");
            entity.setEventType("DebitCreditNoteProcessedEvent");
            entity.setPayload("{\"amount\":1000}");
            entity.setCreatedAt(now);
            entity.setPublishedAt(now.plusSeconds(5));
            entity.setStatus(OutboxStatus.PUBLISHED);
            entity.setRetryCount(0);
            entity.setErrorMessage(null);
            entity.setTopic("debitcreditnote.processed");
            entity.setPartitionKey("dcn-setters");
            entity.setHeaders("{\"key\":\"value\"}");

            // Assert
            assertEquals(id, entity.getId());
            assertEquals("ProcessedDebitCreditNote", entity.getAggregateType());
            assertEquals("dcn-setters", entity.getAggregateId());
            assertEquals("DebitCreditNoteProcessedEvent", entity.getEventType());
            assertEquals("{\"amount\":1000}", entity.getPayload());
            assertEquals(now, entity.getCreatedAt());
            assertEquals(now.plusSeconds(5), entity.getPublishedAt());
            assertEquals(OutboxStatus.PUBLISHED, entity.getStatus());
            assertEquals(0, entity.getRetryCount());
            assertNull(entity.getErrorMessage());
            assertEquals("debitcreditnote.processed", entity.getTopic());
            assertEquals("dcn-setters", entity.getPartitionKey());
            assertEquals("{\"key\":\"value\"}", entity.getHeaders());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create entity with no-args constructor")
        void testNoArgsConstructor() {
            // Act
            OutboxEventEntity entity = new OutboxEventEntity();

            // Assert
            assertNotNull(entity);
            assertNull(entity.getId());
            assertNull(entity.getAggregateType());
            assertNull(entity.getAggregateId());
            assertNull(entity.getEventType());
            assertNull(entity.getPayload());
        }

        @Test
        @DisplayName("Should create entity with all-args constructor")
        void testAllArgsConstructor() {
            // Arrange
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();

            // Act
            OutboxEventEntity entity = new OutboxEventEntity(
                id,
                "ProcessedDebitCreditNote",
                "dcn-allargs",
                "DebitCreditNoteProcessedEvent",
                "{\"allargs\":true}",
                now,
                now.plusSeconds(5),
                OutboxStatus.PUBLISHED,
                0,
                null,
                "debitcreditnote.processed",
                "dcn-allargs",
                "{\"headers\":true}"
            );

            // Assert
            assertNotNull(entity);
            assertEquals(id, entity.getId());
            assertEquals("ProcessedDebitCreditNote", entity.getAggregateType());
            assertEquals("dcn-allargs", entity.getAggregateId());
            assertEquals("DebitCreditNoteProcessedEvent", entity.getEventType());
            assertEquals("{\"allargs\":true}", entity.getPayload());
            assertEquals(now, entity.getCreatedAt());
            assertEquals(now.plusSeconds(5), entity.getPublishedAt());
            assertEquals(OutboxStatus.PUBLISHED, entity.getStatus());
            assertEquals(0, entity.getRetryCount());
            assertNull(entity.getErrorMessage());
            assertEquals("debitcreditnote.processed", entity.getTopic());
            assertEquals("dcn-allargs", entity.getPartitionKey());
            assertEquals("{\"headers\":true}", entity.getHeaders());
        }
    }
}
