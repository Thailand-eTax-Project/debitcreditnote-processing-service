# Debit/Credit Note Processing Service — Hexagonal Architecture Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor `debitcreditnote-processing-service` from a loosely layered structure to full hexagonal architecture + DDD, matching `taxinvoice-processing-service` exactly in structure and behaviour.

**Architecture:** Ports-and-adapters: domain ports in `domain/port/out/`, application ports in `application/port/in/` and `application/port/out/`, inbound adapters in `infrastructure/adapter/in/messaging/`, outbound adapters in `infrastructure/adapter/out/{messaging,parsing,persistence}/`. The application service (`DebitCreditNoteProcessingService`) implements both use cases, owns all saga reply logic, Micrometer metrics, and REQUIRES_NEW transaction handling. Old files are deleted after all new files compile and tests pass.

**Tech Stack:** Java 21, Spring Boot 3.2.5, Apache Camel 4.14.4, Spring Data JPA, PostgreSQL (H2 for tests), Kafka (via outbox), Micrometer, teda library (JAXB), saga-commons library.

**Reference:** All patterns, propagation annotations, and metric names mirror `taxinvoice-processing-service`.

**Spec:** `docs/superpowers/specs/2026-04-27-debitcreditnote-hexagonal-design.md`

**Build command (run after each commit to verify):**
```bash
cd /home/wpanther/projects/etax/invoice-microservices/services/debitcreditnote-processing-service
mvn clean verify -Dspring.profiles.active=test 2>&1 | tail -20
```

---

## File Map

### NEW files created by this plan

| File | Task |
|---|---|
| `domain/port/out/DebitCreditNoteParserPort.java` | 1 |
| `domain/port/out/ProcessedDebitCreditNoteRepository.java` | 2 |
| `domain/event/DebitCreditNoteProcessedDomainEvent.java` | 3 |
| `application/port/in/ProcessDebitCreditNoteUseCase.java` | 4 |
| `application/port/in/CompensateDebitCreditNoteUseCase.java` | 4 |
| `application/port/out/SagaReplyPort.java` | 5 |
| `application/port/out/DebitCreditNoteEventPublishingPort.java` | 5 |
| `application/dto/event/DebitCreditNoteProcessedEvent.java` | 6 |
| `infrastructure/config/KafkaTopicsProperties.java` | 7 |
| `infrastructure/adapter/in/messaging/dto/ProcessDebitCreditNoteCommand.java` | 8 |
| `infrastructure/adapter/in/messaging/dto/CompensateDebitCreditNoteCommand.java` | 8 |
| `infrastructure/adapter/out/messaging/dto/DebitCreditNoteReplyEvent.java` | 9 |
| `infrastructure/adapter/out/messaging/HeaderSerializer.java` | 10 |
| `infrastructure/adapter/out/messaging/SagaReplyPublisher.java` | 11 |
| `infrastructure/adapter/out/messaging/DebitCreditNoteEventPublisher.java` | 12 |
| `infrastructure/adapter/out/parsing/DebitCreditNoteParserServiceImpl.java` | 13 |
| `infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteEntity.java` | 14 |
| `infrastructure/adapter/out/persistence/DebitCreditNotePartyEntity.java` | 14 |
| `infrastructure/adapter/out/persistence/DebitCreditNoteLineItemEntity.java` | 14 |
| `infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteMapper.java` | 14 |
| `infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteRepositoryImpl.java` | 15 |
| `infrastructure/adapter/out/persistence/JpaProcessedDebitCreditNoteRepository.java` | 15 |
| `infrastructure/adapter/out/persistence/outbox/OutboxEventEntity.java` | 16 |
| `infrastructure/adapter/out/persistence/outbox/JpaOutboxEventRepository.java` | 16 |
| `infrastructure/adapter/out/persistence/outbox/SpringDataOutboxRepository.java` | 16 |
| `infrastructure/adapter/out/persistence/outbox/OutboxCleanupScheduler.java` | 16 |
| `infrastructure/config/OutboxConfig.java` | 17 |
| `infrastructure/adapter/in/messaging/SagaCommandHandler.java` | 18 |
| `infrastructure/adapter/in/messaging/SagaRouteConfig.java` | 18 |

### REWRITTEN files

| File | Task |
|---|---|
| `application/service/DebitCreditNoteProcessingService.java` | 19 |

### TEST files created by this plan

| File | Task |
|---|---|
| `test/.../domain/event/DebitCreditNoteProcessedDomainEventTest.java` | 3 |
| `test/.../domain/model/DebitCreditNoteIdTest.java` | 20 |
| `test/.../domain/model/MoneyTest.java` | 20 |
| `test/.../domain/model/AddressTest.java` | 20 |
| `test/.../domain/model/TaxIdentifierTest.java` | 20 |
| `test/.../domain/model/PartyTest.java` | 20 |
| `test/.../domain/model/LineItemTest.java` | 20 |
| `test/.../domain/model/ProcessedDebitCreditNoteTest.java` | 20 |
| `test/.../domain/model/ProcessingStatusTest.java` | 20 |
| `test/.../infrastructure/adapter/out/messaging/HeaderSerializerTest.java` | 10 |
| `test/.../infrastructure/adapter/out/messaging/SagaReplyPublisherTest.java` | 11 |
| `test/.../infrastructure/adapter/out/messaging/SagaReplyPublisherTransactionTest.java` | 11 |
| `test/.../infrastructure/adapter/out/messaging/DebitCreditNoteEventPublisherTest.java` | 12 |
| `test/.../infrastructure/adapter/out/parsing/DebitCreditNoteParserServiceImplTest.java` | 13 |
| `test/.../infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteEntityTest.java` | 14 |
| `test/.../infrastructure/adapter/out/persistence/DebitCreditNotePartyEntityTest.java` | 14 |
| `test/.../infrastructure/adapter/out/persistence/DebitCreditNoteLineItemEntityTest.java` | 14 |
| `test/.../infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteMapperTest.java` | 14 |
| `test/.../infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteRepositoryImplTest.java` | 15 |
| `test/.../infrastructure/adapter/out/persistence/outbox/OutboxEventEntityTest.java` | 16 |
| `test/.../infrastructure/adapter/out/persistence/outbox/JpaOutboxEventRepositoryTest.java` | 16 |
| `test/.../infrastructure/adapter/out/persistence/outbox/OutboxCleanupSchedulerTest.java` | 16 |
| `test/.../infrastructure/adapter/in/messaging/SagaCommandHandlerTest.java` | 18 |
| `test/.../infrastructure/adapter/in/messaging/SagaRouteConfigTest.java` | 18 |
| `test/.../application/service/DebitCreditNoteProcessingServiceTest.java` | 19 |
| `test/.../DebitCreditNoteProcessingServiceApplicationTest.java` | 21 |

### DELETED files (Task 22)

`domain/event/ProcessDebitCreditNoteCommand.java`, `domain/event/CompensateDebitCreditNoteCommand.java`, `domain/event/DebitCreditNoteReplyEvent.java`, `domain/event/DebitCreditNoteProcessedEvent.java`, `domain/repository/ProcessedDebitCreditNoteRepository.java`, `domain/service/DebitCreditNoteParserService.java`, `application/service/SagaCommandHandler.java`, `infrastructure/config/SagaRouteConfig.java`, `infrastructure/messaging/EventPublisher.java`, `infrastructure/messaging/SagaReplyPublisher.java`, `infrastructure/service/DebitCreditNoteParserServiceImpl.java`, `infrastructure/persistence/` (all files), `infrastructure/config/OutboxConfig.java` (moved to infrastructure/config/ — same path, no delete needed)

### UNCHANGED files (do not touch)

`domain/model/` (all 8 files), `src/main/resources/application.yml` (until Task 17), `src/main/resources/db/migration/` (all 4 SQL files), `pom.xml`, `DebitCreditNoteProcessingServiceApplication.java`

---

All paths below are relative to:
`src/main/java/com/wpanther/debitcreditnote/processing/` (main)
`src/test/java/com/wpanther/debitcreditnote/processing/` (test)

---

## Task 1: Domain Parser Port

**What:** Create `domain/port/out/DebitCreditNoteParserPort` — the domain-level outbound port for XML parsing. Replaces `domain/service/DebitCreditNoteParserService`. Rename method `parseNote` → `parse`. Add static factory methods to `ParsingException` matching the reference.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/domain/port/out/DebitCreditNoteParserPort.java`

- [ ] **Step 1: Create the port interface**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/domain/port/out/DebitCreditNoteParserPort.java
package com.wpanther.debitcreditnote.processing.domain.port.out;

import com.wpanther.debitcreditnote.processing.domain.model.ProcessedDebitCreditNote;

public interface DebitCreditNoteParserPort {

    ProcessedDebitCreditNote parse(String xmlContent, String sourceNoteId)
            throws ParsingException;

    class ParsingException extends Exception {
        public ParsingException(String message) {
            super(message);
        }

        public ParsingException(String message, Throwable cause) {
            super(message, cause);
        }

        public static ParsingException forEmpty() {
            return new ParsingException("XML content is null or empty");
        }

        public static ParsingException forOversized(int byteSize, int limitBytes) {
            return new ParsingException(
                "XML payload too large: " + byteSize + " bytes (limit " + limitBytes + " bytes / 500 KB)");
        }

        public static ParsingException forTimeout(long timeoutMs) {
            return new ParsingException(
                "XML parsing timed out after " + timeoutMs + " ms — possible malformed input");
        }

        public static ParsingException forInterrupted() {
            return new ParsingException("XML parsing was interrupted");
        }

        public static ParsingException forUnmarshal(Throwable cause) {
            return new ParsingException("XML parsing failed: " + cause.getMessage(), cause);
        }

        public static ParsingException forUnexpectedRootElement(String className) {
            return new ParsingException("Unexpected root element: " + className);
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/domain/port/out/DebitCreditNoteParserPort.java
git commit -m "Add domain parser port DebitCreditNoteParserPort"
```

---

## Task 2: Domain Repository Port

**What:** Create `domain/port/out/ProcessedDebitCreditNoteRepository` — trimmed to 5 methods. Replaces `domain/repository/ProcessedDebitCreditNoteRepository` (which had 7 methods). Removed: `findByIdWithDetails`, `findByStatusWithDetails`, `existsBySourceNoteId`.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/domain/port/out/ProcessedDebitCreditNoteRepository.java`

- [ ] **Step 1: Create the port interface**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/domain/port/out/ProcessedDebitCreditNoteRepository.java
package com.wpanther.debitcreditnote.processing.domain.port.out;

import com.wpanther.debitcreditnote.processing.domain.model.DebitCreditNoteId;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessedDebitCreditNote;

import java.util.Optional;

public interface ProcessedDebitCreditNoteRepository {

    ProcessedDebitCreditNote save(ProcessedDebitCreditNote note);

    Optional<ProcessedDebitCreditNote> findById(DebitCreditNoteId id);

    Optional<ProcessedDebitCreditNote> findByNoteNumber(String noteNumber);

    Optional<ProcessedDebitCreditNote> findBySourceNoteId(String sourceNoteId);

    void deleteById(DebitCreditNoteId id);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/domain/port/out/ProcessedDebitCreditNoteRepository.java
git commit -m "Add trimmed domain repository port ProcessedDebitCreditNoteRepository"
```

---

## Task 3: Domain Event Record

**What:** Create `domain/event/DebitCreditNoteProcessedDomainEvent` — pure Java record, no Jackson, no framework dependencies. Includes `sagaId`. No `currency` field (currency is available via `total.currency()`). Write test first.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/domain/event/DebitCreditNoteProcessedDomainEvent.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/domain/event/DebitCreditNoteProcessedDomainEventTest.java`

- [ ] **Step 1: Write the failing test**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/domain/event/DebitCreditNoteProcessedDomainEventTest.java
package com.wpanther.debitcreditnote.processing.domain.event;

import com.wpanther.debitcreditnote.processing.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DebitCreditNoteProcessedDomainEventTest {

    @Test
    void of_stampsOccurredAt() {
        Instant before = Instant.now();
        DebitCreditNoteProcessedDomainEvent event = DebitCreditNoteProcessedDomainEvent.of(
            "doc-1", "CN-001", Money.of(BigDecimal.valueOf(100), "THB"), "saga-1", "corr-1"
        );
        Instant after = Instant.now();

        assertThat(event.occurredAt()).isBetween(before, after);
    }

    @Test
    void of_setsAllFields() {
        Money total = Money.of(BigDecimal.valueOf(500), "THB");
        DebitCreditNoteProcessedDomainEvent event = DebitCreditNoteProcessedDomainEvent.of(
            "doc-2", "DN-002", total, "saga-2", "corr-2"
        );

        assertThat(event.documentId()).isEqualTo("doc-2");
        assertThat(event.documentNumber()).isEqualTo("DN-002");
        assertThat(event.total()).isEqualTo(total);
        assertThat(event.sagaId()).isEqualTo("saga-2");
        assertThat(event.correlationId()).isEqualTo("corr-2");
    }

    @Test
    void canonicalConstructor_allowsFixedTimestamp() {
        Instant fixed = Instant.parse("2026-01-01T00:00:00Z");
        Money total = Money.of(BigDecimal.valueOf(200), "THB");
        DebitCreditNoteProcessedDomainEvent event = new DebitCreditNoteProcessedDomainEvent(
            "doc-3", "DN-003", total, "saga-3", "corr-3", fixed
        );

        assertThat(event.occurredAt()).isEqualTo(fixed);
    }
}
```

- [ ] **Step 2: Run test — verify it fails (class not found)**

```bash
mvn test -Dtest=DebitCreditNoteProcessedDomainEventTest -pl . 2>&1 | tail -10
```
Expected: compilation error — `DebitCreditNoteProcessedDomainEvent` does not exist.

- [ ] **Step 3: Create the domain event record**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/domain/event/DebitCreditNoteProcessedDomainEvent.java
package com.wpanther.debitcreditnote.processing.domain.event;

import com.wpanther.debitcreditnote.processing.domain.model.Money;

import java.time.Instant;

public record DebitCreditNoteProcessedDomainEvent(
        String documentId,
        String documentNumber,
        Money total,
        String sagaId,
        String correlationId,
        Instant occurredAt
) {
    public static DebitCreditNoteProcessedDomainEvent of(
            String documentId,
            String documentNumber,
            Money total,
            String sagaId,
            String correlationId) {
        return new DebitCreditNoteProcessedDomainEvent(
            documentId, documentNumber, total, sagaId, correlationId, Instant.now());
    }
}
```

- [ ] **Step 4: Run test — verify it passes**

```bash
mvn test -Dtest=DebitCreditNoteProcessedDomainEventTest -pl . 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`, all 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/domain/event/DebitCreditNoteProcessedDomainEvent.java \
        src/test/java/com/wpanther/debitcreditnote/processing/domain/event/DebitCreditNoteProcessedDomainEventTest.java
git commit -m "Add pure domain event record DebitCreditNoteProcessedDomainEvent"
```

---

## Task 4: Application Inbound Ports

**What:** Create `application/port/in/ProcessDebitCreditNoteUseCase` and `application/port/in/CompensateDebitCreditNoteUseCase`. These are pure interfaces — no logic to TDD.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/application/port/in/ProcessDebitCreditNoteUseCase.java`
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/application/port/in/CompensateDebitCreditNoteUseCase.java`

- [ ] **Step 1: Create ProcessDebitCreditNoteUseCase**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/application/port/in/ProcessDebitCreditNoteUseCase.java
package com.wpanther.debitcreditnote.processing.application.port.in;

import com.wpanther.saga.domain.enums.SagaStep;

public interface ProcessDebitCreditNoteUseCase {

    void process(String documentId, String xmlContent,
                 String sagaId, SagaStep sagaStep, String correlationId)
            throws ProcessingException;

    class ProcessingException extends Exception {
        public ProcessingException(String message) {
            super(message);
        }

        public ProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

- [ ] **Step 2: Create CompensateDebitCreditNoteUseCase**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/application/port/in/CompensateDebitCreditNoteUseCase.java
package com.wpanther.debitcreditnote.processing.application.port.in;

import com.wpanther.saga.domain.enums.SagaStep;

public interface CompensateDebitCreditNoteUseCase {

    void compensate(String documentId, String sagaId,
                    SagaStep sagaStep, String correlationId);

    class CompensationException extends RuntimeException {
        public CompensationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/application/port/in/
git commit -m "Add application inbound ports for process and compensate use cases"
```

---

## Task 5: Application Outbound Ports

**What:** Create `application/port/out/SagaReplyPort` and `application/port/out/DebitCreditNoteEventPublishingPort`. Pure interfaces — no logic to TDD. `SagaReplyPort` uses `SagaStep` enum (replacing current `String sagaStep`).

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/application/port/out/SagaReplyPort.java`
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/application/port/out/DebitCreditNoteEventPublishingPort.java`

- [ ] **Step 1: Create SagaReplyPort**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/application/port/out/SagaReplyPort.java
package com.wpanther.debitcreditnote.processing.application.port.out;

import com.wpanther.saga.domain.enums.SagaStep;

public interface SagaReplyPort {

    void publishSuccess(String sagaId, SagaStep sagaStep, String correlationId);

    void publishFailure(String sagaId, SagaStep sagaStep, String correlationId, String errorMessage);

    void publishCompensated(String sagaId, SagaStep sagaStep, String correlationId);
}
```

- [ ] **Step 2: Create DebitCreditNoteEventPublishingPort**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/application/port/out/DebitCreditNoteEventPublishingPort.java
package com.wpanther.debitcreditnote.processing.application.port.out;

import com.wpanther.debitcreditnote.processing.domain.event.DebitCreditNoteProcessedDomainEvent;

public interface DebitCreditNoteEventPublishingPort {

    void publish(DebitCreditNoteProcessedDomainEvent event);
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/application/port/out/
git commit -m "Add application outbound ports SagaReplyPort and DebitCreditNoteEventPublishingPort"
```

---

## Task 6: Application Kafka DTO

**What:** Create `application/dto/event/DebitCreditNoteProcessedEvent` — the Kafka-serializable DTO published to `debitcreditnote.processed`. Extends `TraceEvent` (matching the taxinvoice reference). Has `documentId`, `documentNumber`, `total` (BigDecimal), `currency` (from `Money.currency()`). Does NOT have a `noteId` UUID field (the old version did; we align with reference). Write a test to verify serialization.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/application/dto/event/DebitCreditNoteProcessedEvent.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/application/dto/event/DebitCreditNoteProcessedEventTest.java`

- [ ] **Step 1: Write the failing test**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/application/dto/event/DebitCreditNoteProcessedEventTest.java
package com.wpanther.debitcreditnote.processing.application.dto.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DebitCreditNoteProcessedEventTest {

    @Test
    void constructor_setsAllFields() {
        var event = new DebitCreditNoteProcessedEvent(
            "doc-1", "CN-001", BigDecimal.valueOf(1500), "THB", "saga-1", "corr-1"
        );

        assertThat(event.getDocumentId()).isEqualTo("doc-1");
        assertThat(event.getDocumentNumber()).isEqualTo("CN-001");
        assertThat(event.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(event.getCurrency()).isEqualTo("THB");
        assertThat(event.getSagaId()).isEqualTo("saga-1");
        assertThat(event.getCorrelationId()).isEqualTo("corr-1");
    }

    @Test
    void getEventType_returnsCorrectType() {
        var event = new DebitCreditNoteProcessedEvent(
            "doc-1", "CN-001", BigDecimal.TEN, "THB", "saga-1", "corr-1"
        );
        assertThat(event.getEventType()).isEqualTo("debitcreditnote.processed");
    }
}
```

- [ ] **Step 2: Run test — verify it fails**

```bash
mvn test -Dtest=DebitCreditNoteProcessedEventTest -pl . 2>&1 | tail -10
```
Expected: compilation error — class does not exist.

- [ ] **Step 3: Create the Kafka DTO**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/application/dto/event/DebitCreditNoteProcessedEvent.java
package com.wpanther.debitcreditnote.processing.application.dto.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wpanther.saga.domain.model.TraceEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class DebitCreditNoteProcessedEvent extends TraceEvent {

    private static final String EVENT_TYPE = "debitcreditnote.processed";
    private static final String SOURCE = "debitcreditnote-processing-service";
    private static final String TRACE_TYPE = "DEBIT_CREDIT_NOTE_PROCESSED";

    @JsonProperty("documentId")
    private final String documentId;

    @JsonProperty("documentNumber")
    private final String documentNumber;

    @JsonProperty("total")
    private final BigDecimal total;

    @JsonProperty("currency")
    private final String currency;

    public DebitCreditNoteProcessedEvent(String documentId, String documentNumber,
                                          BigDecimal total, String currency,
                                          String sagaId, String correlationId) {
        super(sagaId, correlationId, SOURCE, TRACE_TYPE, null);
        this.documentId = documentId;
        this.documentNumber = documentNumber;
        this.total = total;
        this.currency = currency;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @JsonCreator
    public DebitCreditNoteProcessedEvent(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("version") int version,
            @JsonProperty("sagaId") String sagaId,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("source") String source,
            @JsonProperty("traceType") String traceType,
            @JsonProperty("context") String context,
            @JsonProperty("documentId") String documentId,
            @JsonProperty("documentNumber") String documentNumber,
            @JsonProperty("total") BigDecimal total,
            @JsonProperty("currency") String currency) {
        super(eventId, occurredAt, eventType, version, sagaId, correlationId, source, traceType, context);
        this.documentId = documentId;
        this.documentNumber = documentNumber;
        this.total = total;
        this.currency = currency;
    }
}
```

- [ ] **Step 4: Run test — verify it passes**

```bash
mvn test -Dtest=DebitCreditNoteProcessedEventTest -pl . 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`, both tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/application/dto/event/DebitCreditNoteProcessedEvent.java \
        src/test/java/com/wpanther/debitcreditnote/processing/application/dto/event/DebitCreditNoteProcessedEventTest.java
git commit -m "Add application Kafka DTO DebitCreditNoteProcessedEvent"
```

---

## Task 7: KafkaTopicsProperties Config

**What:** Create `infrastructure/config/KafkaTopicsProperties` — typed `@ConfigurationProperties` record binding `app.kafka.topics.*`. Replaces raw `@Value` injections currently in `SagaRouteConfig`. Fail-fast at startup on misconfiguration.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/config/KafkaTopicsProperties.java`

- [ ] **Step 1: Create the config record**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/config/KafkaTopicsProperties.java
package com.wpanther.debitcreditnote.processing.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for all {@code app.kafka.topics.*} configuration properties.
 *
 * YAML key mapping (Spring relaxed binding):
 *   app.kafka.topics.debitcreditnote-processed       → debitcreditnoteProcessed
 *   app.kafka.topics.dlq                             → dlq
 *   app.kafka.topics.saga-command-debitcreditnote    → sagaCommandDebitcreditnote
 *   app.kafka.topics.saga-compensation-debitcreditnote → sagaCompensationDebitcreditnote
 *   app.kafka.topics.saga-reply-debitcreditnote      → sagaReplyDebitcreditnote
 */
@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicsProperties(
        String debitcreditnoteProcessed,
        String dlq,
        String sagaCommandDebitcreditnote,
        String sagaCompensationDebitcreditnote,
        String sagaReplyDebitcreditnote) {
}
```

- [ ] **Step 2: Enable ConfigurationProperties in application main class**

Open `src/main/java/com/wpanther/debitcreditnote/processing/DebitCreditNoteProcessingServiceApplication.java` and add `@EnableConfigurationProperties(KafkaTopicsProperties.class)` to the class:

```java
package com.wpanther.debitcreditnote.processing;

import com.wpanther.debitcreditnote.processing.infrastructure.config.KafkaTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class DebitCreditNoteProcessingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DebitCreditNoteProcessingServiceApplication.class, args);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/config/KafkaTopicsProperties.java \
        src/main/java/com/wpanther/debitcreditnote/processing/DebitCreditNoteProcessingServiceApplication.java
git commit -m "Add typed KafkaTopicsProperties config and enable scheduling"
```

---

## Task 8: Inbound Command DTOs

**What:** Create the two command DTO classes in `infrastructure/adapter/in/messaging/dto/`. These are moved from `domain/event/` and updated: `sagaStep` field type changes from `String` to `SagaStep` enum. Old files in `domain/event/` are NOT deleted yet (they are used by existing code still in place).

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/dto/ProcessDebitCreditNoteCommand.java`
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/dto/CompensateDebitCreditNoteCommand.java`

- [ ] **Step 1: Create ProcessDebitCreditNoteCommand**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/dto/ProcessDebitCreditNoteCommand.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.saga.domain.model.IntegrationEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ProcessDebitCreditNoteCommand extends IntegrationEvent {

    private static final long serialVersionUID = 1L;

    @JsonProperty("sagaId")
    private final String sagaId;

    @JsonProperty("sagaStep")
    private final SagaStep sagaStep;

    @JsonProperty("correlationId")
    private final String correlationId;

    @JsonProperty("documentId")
    private final String documentId;

    @JsonProperty("xmlContent")
    private final String xmlContent;

    @JsonProperty("noteNumber")
    private final String noteNumber;

    @JsonCreator
    public ProcessDebitCreditNoteCommand(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("version") int version,
            @JsonProperty("sagaId") String sagaId,
            @JsonProperty("sagaStep") SagaStep sagaStep,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("documentId") String documentId,
            @JsonProperty("xmlContent") String xmlContent,
            @JsonProperty("noteNumber") String noteNumber) {
        super(eventId, occurredAt, eventType, version);
        this.sagaId = sagaId;
        this.sagaStep = sagaStep;
        this.correlationId = correlationId;
        this.documentId = documentId;
        this.xmlContent = xmlContent;
        this.noteNumber = noteNumber;
    }

    public ProcessDebitCreditNoteCommand(String sagaId, SagaStep sagaStep, String correlationId,
                                          String documentId, String xmlContent, String noteNumber) {
        super();
        this.sagaId = sagaId;
        this.sagaStep = sagaStep;
        this.correlationId = correlationId;
        this.documentId = documentId;
        this.xmlContent = xmlContent;
        this.noteNumber = noteNumber;
    }
}
```

- [ ] **Step 2: Create CompensateDebitCreditNoteCommand**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/dto/CompensateDebitCreditNoteCommand.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.saga.domain.model.IntegrationEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class CompensateDebitCreditNoteCommand extends IntegrationEvent {

    private static final long serialVersionUID = 1L;

    @JsonProperty("sagaId")
    private final String sagaId;

    @JsonProperty("sagaStep")
    private final SagaStep sagaStep;

    @JsonProperty("correlationId")
    private final String correlationId;

    @JsonProperty("documentId")
    private final String documentId;

    @JsonCreator
    public CompensateDebitCreditNoteCommand(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("version") int version,
            @JsonProperty("sagaId") String sagaId,
            @JsonProperty("sagaStep") SagaStep sagaStep,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("documentId") String documentId) {
        super(eventId, occurredAt, eventType, version);
        this.sagaId = sagaId;
        this.sagaStep = sagaStep;
        this.correlationId = correlationId;
        this.documentId = documentId;
    }

    public CompensateDebitCreditNoteCommand(String sagaId, SagaStep sagaStep,
                                             String correlationId, String documentId) {
        super();
        this.sagaId = sagaId;
        this.sagaStep = sagaStep;
        this.correlationId = correlationId;
        this.documentId = documentId;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/dto/
git commit -m "Add inbound command DTOs with SagaStep enum (moved from domain/event/)"
```

---

## Task 9: Outbound Reply DTO

**What:** Create `infrastructure/adapter/out/messaging/dto/DebitCreditNoteReplyEvent` — moved from `domain/event/`. Add static factory methods (`success`, `failure`, `compensated`) matching the taxinvoice reference. Update to accept `SagaStep` enum in constructors.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/dto/DebitCreditNoteReplyEvent.java`

- [ ] **Step 1: Create the reply event DTO**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/dto/DebitCreditNoteReplyEvent.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.messaging.dto;

import com.wpanther.saga.domain.enums.ReplyStatus;
import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.saga.domain.model.SagaReply;

public class DebitCreditNoteReplyEvent extends SagaReply {

    private static final long serialVersionUID = 1L;

    public static DebitCreditNoteReplyEvent success(String sagaId, SagaStep sagaStep, String correlationId) {
        return new DebitCreditNoteReplyEvent(sagaId, sagaStep, correlationId, ReplyStatus.SUCCESS);
    }

    public static DebitCreditNoteReplyEvent failure(String sagaId, SagaStep sagaStep,
                                                     String correlationId, String errorMessage) {
        return new DebitCreditNoteReplyEvent(sagaId, sagaStep, correlationId, errorMessage);
    }

    public static DebitCreditNoteReplyEvent compensated(String sagaId, SagaStep sagaStep, String correlationId) {
        return new DebitCreditNoteReplyEvent(sagaId, sagaStep, correlationId, ReplyStatus.COMPENSATED);
    }

    private DebitCreditNoteReplyEvent(String sagaId, SagaStep sagaStep,
                                       String correlationId, ReplyStatus status) {
        super(sagaId, sagaStep, correlationId, status);
    }

    private DebitCreditNoteReplyEvent(String sagaId, SagaStep sagaStep,
                                       String correlationId, String errorMessage) {
        super(sagaId, sagaStep, correlationId, errorMessage);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/dto/
git commit -m "Add outbound reply DTO DebitCreditNoteReplyEvent with static factory methods"
```

---

## Task 10: HeaderSerializer

**What:** Create `infrastructure/adapter/out/messaging/HeaderSerializer` — utility component for serializing outbox event headers to JSON. Throws `IllegalStateException` on failure (no silent fallback). Write test first.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/HeaderSerializer.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/HeaderSerializerTest.java`

- [ ] **Step 1: Write the failing test**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/HeaderSerializerTest.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeaderSerializerTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private HeaderSerializer headerSerializer;

    @Test
    void toJson_successfulSerialization() throws JsonProcessingException {
        Map<String, String> headers = Map.of("key", "value");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"key\":\"value\"}");

        String result = headerSerializer.toJson(headers);

        assertEquals("{\"key\":\"value\"}", result);
    }

    @Test
    void toJson_whenJsonProcessingException_throwsIllegalStateException() throws JsonProcessingException {
        Map<String, String> headers = Map.of("key", "value");
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("error") {});

        assertThrows(IllegalStateException.class, () -> headerSerializer.toJson(headers));
    }
}
```

- [ ] **Step 2: Run test — verify it fails**

```bash
mvn test -Dtest=HeaderSerializerTest -pl . 2>&1 | tail -10
```
Expected: compilation error — `HeaderSerializer` does not exist.

- [ ] **Step 3: Create HeaderSerializer**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/HeaderSerializer.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeaderSerializer {

    private final ObjectMapper objectMapper;

    public String toJson(Map<String, String> headers) {
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox headers to JSON: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: Run test — verify it passes**

```bash
mvn test -Dtest=HeaderSerializerTest -pl . 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`, both tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/HeaderSerializer.java \
        src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/HeaderSerializerTest.java
git commit -m "Add HeaderSerializer for outbox event header serialization"
```

---

## Task 11: SagaReplyPublisher (outbound adapter, fix REQUIRES_NEW)

**What:** Create `infrastructure/adapter/out/messaging/SagaReplyPublisher` implementing `SagaReplyPort`. Fixes the bug in the old `infrastructure/messaging/SagaReplyPublisher` where `publishFailure` used `MANDATORY` instead of `REQUIRES_NEW`. Delete the old file atomically after the new one is in place. Write Mockito unit test and `@SpringBootTest` transaction test.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/SagaReplyPublisher.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/SagaReplyPublisherTest.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/SagaReplyPublisherTransactionTest.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/messaging/SagaReplyPublisher.java`

**Important:** `infrastructure/messaging/SagaReplyPublisher` and `infrastructure/adapter/out/messaging/SagaReplyPublisher` are in different packages, so Spring will try to register BOTH beans at the same time, causing a conflict. The old file **must be deleted** in the same commit as creating the new file.

- [ ] **Step 1: Write the Mockito unit test**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/SagaReplyPublisherTest.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.messaging;

import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.saga.infrastructure.outbox.OutboxService;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.messaging.dto.DebitCreditNoteReplyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaReplyPublisherTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private HeaderSerializer headerSerializer;

    private SagaReplyPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SagaReplyPublisher(outboxService, headerSerializer, "saga.reply.debitcreditnote");
    }

    @Test
    void publishSuccess_callsOutboxWithCorrectParameters() {
        when(headerSerializer.toJson(any())).thenReturn("{\"sagaId\":\"saga-1\",\"correlationId\":\"corr-1\",\"status\":\"SUCCESS\"}");

        publisher.publishSuccess("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");

        verify(outboxService).saveWithRouting(
            any(DebitCreditNoteReplyEvent.class),
            eq("ProcessedDebitCreditNote"),
            eq("saga-1"),
            eq("saga.reply.debitcreditnote"),
            eq("saga-1"),
            contains("SUCCESS")
        );
    }

    @Test
    void publishSuccess_usesSagaIdAsPartitionKey() {
        when(headerSerializer.toJson(any())).thenReturn("{}");

        publisher.publishSuccess("my-saga-id", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");

        ArgumentCaptor<String> partitionKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService).saveWithRouting(
            any(), any(), any(), any(),
            partitionKeyCaptor.capture(),
            any()
        );

        assertEquals("my-saga-id", partitionKeyCaptor.getValue());
    }

    @Test
    void publishFailure_callsOutboxWithCorrectParameters() {
        when(headerSerializer.toJson(any())).thenReturn("{\"sagaId\":\"saga-1\",\"correlationId\":\"corr-1\",\"status\":\"FAILURE\"}");

        publisher.publishFailure("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1", "Parse error");

        verify(outboxService).saveWithRouting(
            any(DebitCreditNoteReplyEvent.class),
            eq("ProcessedDebitCreditNote"),
            eq("saga-1"),
            eq("saga.reply.debitcreditnote"),
            eq("saga-1"),
            contains("FAILURE")
        );
    }

    @Test
    void publishCompensated_callsOutboxWithCorrectParameters() {
        when(headerSerializer.toJson(any())).thenReturn("{\"sagaId\":\"saga-1\",\"correlationId\":\"corr-1\",\"status\":\"COMPENSATED\"}");

        publisher.publishCompensated("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");

        verify(outboxService).saveWithRouting(
            any(DebitCreditNoteReplyEvent.class),
            eq("ProcessedDebitCreditNote"),
            eq("saga-1"),
            eq("saga.reply.debitcreditnote"),
            eq("saga-1"),
            contains("COMPENSATED")
        );
    }

    @Test
    void publishSuccess_headersContainCorrectFields() {
        when(headerSerializer.toJson(any())).thenReturn("{\"sagaId\":\"saga-1\",\"correlationId\":\"corr-1\",\"status\":\"SUCCESS\"}");

        publisher.publishSuccess("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");

        ArgumentCaptor<String> headersCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService).saveWithRouting(any(), any(), any(), any(), any(), headersCaptor.capture());

        String headers = headersCaptor.getValue();
        assertTrue(headers.contains("saga-1"));
        assertTrue(headers.contains("corr-1"));
        assertTrue(headers.contains("SUCCESS"));
    }

    @Test
    void publishReplyEvent_hasCorrectTopic() {
        when(headerSerializer.toJson(any())).thenReturn("{}");

        publisher.publishSuccess("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService).saveWithRouting(any(), any(), any(), topicCaptor.capture(), any(), any());

        assertEquals("saga.reply.debitcreditnote", topicCaptor.getValue());
    }

    @Test
    void publishReplyEvent_hasCorrectAggregateType() {
        when(headerSerializer.toJson(any())).thenReturn("{}");

        publisher.publishFailure("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1", "error");

        ArgumentCaptor<String> aggregateTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService).saveWithRouting(any(), aggregateTypeCaptor.capture(), any(), any(), any(), any());

        assertEquals("ProcessedDebitCreditNote", aggregateTypeCaptor.getValue());
    }
}
```

- [ ] **Step 2: Write the transaction propagation test**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/SagaReplyPublisherTransactionTest.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.messaging;

import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence.outbox.OutboxEventEntity;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence.outbox.SpringDataOutboxRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SagaReplyPublisherTransactionTest {

    @Autowired
    private SagaReplyPublisher sagaReplyPublisher;

    @Autowired
    private SpringDataOutboxRepository outboxRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanup() {
        outboxRepository.deleteAll();
    }

    @Test
    void publishFailure_commitsOutboxEntry_evenWhenOuterTransactionIsRollbackOnly() {
        String sagaId = UUID.randomUUID().toString();
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        try {
            txTemplate.execute(status -> {
                status.setRollbackOnly();
                sagaReplyPublisher.publishFailure(
                        sagaId, SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1", "duplicate key");
                return null;
            });
        } catch (UnexpectedRollbackException | TransactionSystemException ignored) {
            // expected — outer transaction was rolled back
        }

        List<OutboxEventEntity> entries = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(sagaId))
                .toList();

        assertFalse(entries.isEmpty(),
                "publishFailure() must commit its outbox entry in its own transaction " +
                "so the orchestrator receives a FAILURE reply even when the outer " +
                "transaction is rolled back");
    }

    @Test
    void publishFailure_outboxEntry_containsFailureStatus() {
        String sagaId = UUID.randomUUID().toString();
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        try {
            txTemplate.execute(status -> {
                status.setRollbackOnly();
                sagaReplyPublisher.publishFailure(
                        sagaId, SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1", "some error");
                return null;
            });
        } catch (UnexpectedRollbackException | TransactionSystemException ignored) {
            // expected
        }

        List<OutboxEventEntity> entries = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(sagaId))
                .toList();

        assertFalse(entries.isEmpty(), "outbox entry must exist");
        assertTrue(entries.get(0).getPayload().contains("FAILURE"),
                "outbox payload must indicate FAILURE status");
    }

    @Test
    void publishCompensated_commitsOutboxEntry_togetherWithOuterTransaction() {
        String sagaId = UUID.randomUUID().toString();
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        txTemplate.execute(status -> {
            sagaReplyPublisher.publishCompensated(
                    sagaId, SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");
            return null;
        });

        List<OutboxEventEntity> entries = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(sagaId))
                .toList();

        assertFalse(entries.isEmpty(),
                "publishCompensated() outbox entry must be committed with the outer transaction");
    }

    @Test
    void publishCompensated_rollsBackOutboxEntry_whenOuterTransactionRollsBack() {
        String sagaId = UUID.randomUUID().toString();
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        try {
            txTemplate.execute(status -> {
                sagaReplyPublisher.publishCompensated(
                        sagaId, SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");
                status.setRollbackOnly();
                return null;
            });
        } catch (UnexpectedRollbackException | TransactionSystemException ignored) {
            // expected
        }

        List<OutboxEventEntity> entries = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(sagaId))
                .toList();

        assertTrue(entries.isEmpty(),
                "publishCompensated() outbox entry must be rolled back together with the outer " +
                "transaction — a premature COMPENSATED reply must never be delivered");
    }

    @Test
    void publishSuccess_commitsOutboxEntry_togetherWithOuterTransaction() {
        String sagaId = UUID.randomUUID().toString();
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        txTemplate.execute(status -> {
            sagaReplyPublisher.publishSuccess(
                    sagaId, SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");
            return null;
        });

        List<OutboxEventEntity> entries = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(sagaId))
                .toList();

        assertFalse(entries.isEmpty(),
                "publishSuccess() outbox entry must be committed with the outer transaction");
    }

    @Test
    void publishSuccess_rollsBackOutboxEntry_whenOuterTransactionRollsBack() {
        String sagaId = UUID.randomUUID().toString();
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        try {
            txTemplate.execute(status -> {
                sagaReplyPublisher.publishSuccess(
                        sagaId, SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");
                status.setRollbackOnly();
                return null;
            });
        } catch (UnexpectedRollbackException | TransactionSystemException ignored) {
            // expected
        }

        List<OutboxEventEntity> entries = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(sagaId))
                .toList();

        assertTrue(entries.isEmpty(),
                "publishSuccess() outbox entry must be rolled back together with the outer " +
                "transaction — a premature success reply must never be delivered");
    }
}
```

- [ ] **Step 3: Run both tests — verify they fail**

```bash
mvn test -Dtest="SagaReplyPublisherTest,SagaReplyPublisherTransactionTest" -pl . 2>&1 | tail -15
```
Expected: compilation errors — the new `SagaReplyPublisher` and `SpringDataOutboxRepository` in the new package do not exist yet.

- [ ] **Step 4: Create the new SagaReplyPublisher AND delete the old one in one atomic step**

Create `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/SagaReplyPublisher.java`:

```java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.messaging;

import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.saga.infrastructure.outbox.OutboxService;
import com.wpanther.debitcreditnote.processing.application.port.out.SagaReplyPort;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.messaging.dto.DebitCreditNoteReplyEvent;
import com.wpanther.debitcreditnote.processing.infrastructure.config.KafkaTopicsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@Slf4j
public class SagaReplyPublisher implements SagaReplyPort {

    private static final String AGGREGATE_TYPE = "ProcessedDebitCreditNote";

    private final OutboxService outboxService;
    private final HeaderSerializer headerSerializer;
    private final String replyTopic;

    @Autowired
    public SagaReplyPublisher(
            OutboxService outboxService,
            HeaderSerializer headerSerializer,
            KafkaTopicsProperties topics) {
        this(outboxService, headerSerializer, topics.sagaReplyDebitcreditnote());
    }

    SagaReplyPublisher(OutboxService outboxService, HeaderSerializer headerSerializer, String replyTopic) {
        this.outboxService = outboxService;
        this.headerSerializer = headerSerializer;
        this.replyTopic = replyTopic;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishSuccess(String sagaId, SagaStep sagaStep, String correlationId) {
        DebitCreditNoteReplyEvent reply = DebitCreditNoteReplyEvent.success(sagaId, sagaStep, correlationId);

        Map<String, String> headers = Map.of(
            "sagaId", sagaId,
            "correlationId", correlationId,
            "status", "SUCCESS"
        );

        outboxService.saveWithRouting(
            reply,
            AGGREGATE_TYPE,
            sagaId,
            replyTopic,
            sagaId,
            headerSerializer.toJson(headers)
        );

        log.info("Published SUCCESS saga reply for saga {} step {}", sagaId, sagaStep);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishFailure(String sagaId, SagaStep sagaStep, String correlationId, String errorMessage) {
        DebitCreditNoteReplyEvent reply = DebitCreditNoteReplyEvent.failure(sagaId, sagaStep, correlationId, errorMessage);

        Map<String, String> headers = Map.of(
            "sagaId", sagaId,
            "correlationId", correlationId,
            "status", "FAILURE"
        );

        outboxService.saveWithRouting(
            reply,
            AGGREGATE_TYPE,
            sagaId,
            replyTopic,
            sagaId,
            headerSerializer.toJson(headers)
        );

        log.info("Published FAILURE saga reply for saga {} step {}: {}", sagaId, sagaStep, errorMessage);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishCompensated(String sagaId, SagaStep sagaStep, String correlationId) {
        DebitCreditNoteReplyEvent reply = DebitCreditNoteReplyEvent.compensated(sagaId, sagaStep, correlationId);

        Map<String, String> headers = Map.of(
            "sagaId", sagaId,
            "correlationId", correlationId,
            "status", "COMPENSATED"
        );

        outboxService.saveWithRouting(
            reply,
            AGGREGATE_TYPE,
            sagaId,
            replyTopic,
            sagaId,
            headerSerializer.toJson(headers)
        );

        log.info("Published COMPENSATED saga reply for saga {} step {}", sagaId, sagaStep);
    }
}
```

Then delete the old file:
```bash
rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/messaging/SagaReplyPublisher.java
```

- [ ] **Step 5: Run the Mockito unit tests — verify they pass**

```bash
mvn test -Dtest=SagaReplyPublisherTest -pl . 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`, all 7 tests pass.

- [ ] **Step 6: Commit (new + deleted file in one commit)**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/SagaReplyPublisher.java \
        src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/SagaReplyPublisherTest.java \
        src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/SagaReplyPublisherTransactionTest.java
git rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/messaging/SagaReplyPublisher.java
git commit -m "Add SagaReplyPublisher adapter (fix publishFailure REQUIRES_NEW, delete old impl)"
```

---

## Task 12: DebitCreditNoteEventPublisher (outbound adapter)

**What:** Create `infrastructure/adapter/out/messaging/DebitCreditNoteEventPublisher` implementing `DebitCreditNoteEventPublishingPort`. Accepts the pure domain event, translates it to the Kafka DTO, and writes to outbox with `MANDATORY` propagation. Delete the old `infrastructure/messaging/EventPublisher`. Write test first.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/DebitCreditNoteEventPublisher.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/DebitCreditNoteEventPublisherTest.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/messaging/EventPublisher.java`

- [ ] **Step 1: Write the failing test**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/DebitCreditNoteEventPublisherTest.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.messaging;

import com.wpanther.saga.infrastructure.outbox.OutboxService;
import com.wpanther.debitcreditnote.processing.application.dto.event.DebitCreditNoteProcessedEvent;
import com.wpanther.debitcreditnote.processing.domain.event.DebitCreditNoteProcessedDomainEvent;
import com.wpanther.debitcreditnote.processing.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebitCreditNoteEventPublisherTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private HeaderSerializer headerSerializer;

    private DebitCreditNoteEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        eventPublisher = new DebitCreditNoteEventPublisher(outboxService, headerSerializer, "debitcreditnote.processed");
    }

    @Test
    void publish_callsOutboxWithCorrectParameters() {
        DebitCreditNoteProcessedDomainEvent domainEvent = DebitCreditNoteProcessedDomainEvent.of(
            "doc-1", "CN-001", Money.of(BigDecimal.valueOf(1500), "THB"), "saga-1", "corr-1"
        );
        when(headerSerializer.toJson(any())).thenReturn("{\"correlationId\":\"corr-1\",\"documentNumber\":\"CN-001\"}");

        eventPublisher.publish(domainEvent);

        verify(outboxService).saveWithRouting(
            any(DebitCreditNoteProcessedEvent.class),
            eq("ProcessedDebitCreditNote"),
            eq("doc-1"),
            eq("debitcreditnote.processed"),
            eq("doc-1"),
            eq("{\"correlationId\":\"corr-1\",\"documentNumber\":\"CN-001\"}")
        );
    }

    @Test
    void publish_usesDocumentIdAsPartitionKey() {
        DebitCreditNoteProcessedDomainEvent domainEvent = DebitCreditNoteProcessedDomainEvent.of(
            "doc-42", "DN-007", Money.of(BigDecimal.valueOf(500), "THB"), "saga-2", "corr-2"
        );
        when(headerSerializer.toJson(any())).thenReturn("{}");

        eventPublisher.publish(domainEvent);

        ArgumentCaptor<String> partitionKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService).saveWithRouting(any(), any(), any(), any(), partitionKeyCaptor.capture(), any());

        assertEquals("doc-42", partitionKeyCaptor.getValue());
    }

    @Test
    void publish_usesCorrectTopic() {
        DebitCreditNoteProcessedDomainEvent domainEvent = DebitCreditNoteProcessedDomainEvent.of(
            "doc-1", "CN-001", Money.of(BigDecimal.TEN, "THB"), "saga-1", "corr-1"
        );
        when(headerSerializer.toJson(any())).thenReturn("{}");

        eventPublisher.publish(domainEvent);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService).saveWithRouting(any(), any(), any(), topicCaptor.capture(), any(), any());

        assertEquals("debitcreditnote.processed", topicCaptor.getValue());
    }

    @Test
    void publish_translatesMoneyFieldsToKafkaDto() {
        DebitCreditNoteProcessedDomainEvent domainEvent = DebitCreditNoteProcessedDomainEvent.of(
            "doc-1", "CN-001", Money.of(BigDecimal.valueOf(9999), "THB"), "saga-1", "corr-1"
        );
        when(headerSerializer.toJson(any())).thenReturn("{}");

        eventPublisher.publish(domainEvent);

        ArgumentCaptor<DebitCreditNoteProcessedEvent> eventCaptor =
            ArgumentCaptor.forClass(DebitCreditNoteProcessedEvent.class);
        verify(outboxService).saveWithRouting(eventCaptor.capture(), any(), any(), any(), any(), any());

        DebitCreditNoteProcessedEvent kafkaEvent = eventCaptor.getValue();
        assertEquals("doc-1", kafkaEvent.getDocumentId());
        assertEquals("CN-001", kafkaEvent.getDocumentNumber());
        assertEquals(0, BigDecimal.valueOf(9999).compareTo(kafkaEvent.getTotal()));
        assertEquals("THB", kafkaEvent.getCurrency());
    }
}
```

- [ ] **Step 2: Run test — verify it fails**

```bash
mvn test -Dtest=DebitCreditNoteEventPublisherTest -pl . 2>&1 | tail -10
```
Expected: compilation error — `DebitCreditNoteEventPublisher` does not exist.

- [ ] **Step 3: Create DebitCreditNoteEventPublisher AND delete old EventPublisher**

Create `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/DebitCreditNoteEventPublisher.java`:

```java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.messaging;

import com.wpanther.saga.infrastructure.outbox.OutboxService;
import com.wpanther.debitcreditnote.processing.application.dto.event.DebitCreditNoteProcessedEvent;
import com.wpanther.debitcreditnote.processing.application.port.out.DebitCreditNoteEventPublishingPort;
import com.wpanther.debitcreditnote.processing.domain.event.DebitCreditNoteProcessedDomainEvent;
import com.wpanther.debitcreditnote.processing.infrastructure.config.KafkaTopicsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@Slf4j
public class DebitCreditNoteEventPublisher implements DebitCreditNoteEventPublishingPort {

    private final OutboxService outboxService;
    private final HeaderSerializer headerSerializer;
    private final String debitcreditnoteProcessedTopic;

    @Autowired
    public DebitCreditNoteEventPublisher(
            OutboxService outboxService,
            HeaderSerializer headerSerializer,
            KafkaTopicsProperties topics) {
        this(outboxService, headerSerializer, topics.debitcreditnoteProcessed());
    }

    DebitCreditNoteEventPublisher(OutboxService outboxService, HeaderSerializer headerSerializer,
                                   String debitcreditnoteProcessedTopic) {
        this.outboxService = outboxService;
        this.headerSerializer = headerSerializer;
        this.debitcreditnoteProcessedTopic = debitcreditnoteProcessedTopic;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(DebitCreditNoteProcessedDomainEvent domainEvent) {
        DebitCreditNoteProcessedEvent kafkaEvent = new DebitCreditNoteProcessedEvent(
            domainEvent.documentId(),
            domainEvent.documentNumber(),
            domainEvent.total().amount(),
            domainEvent.total().currency(),
            domainEvent.sagaId(),
            domainEvent.correlationId()
        );

        Map<String, String> headers = Map.of(
            "correlationId", domainEvent.correlationId(),
            "documentNumber", domainEvent.documentNumber()
        );

        outboxService.saveWithRouting(
            kafkaEvent,
            "ProcessedDebitCreditNote",
            domainEvent.documentId(),
            debitcreditnoteProcessedTopic,
            domainEvent.documentId(),
            headerSerializer.toJson(headers)
        );

        log.info("Published DebitCreditNoteProcessedEvent to outbox: {}", domainEvent.documentNumber());
    }
}
```

Then delete the old file:
```bash
rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/messaging/EventPublisher.java
```

- [ ] **Step 4: Run test — verify it passes**

```bash
mvn test -Dtest=DebitCreditNoteEventPublisherTest -pl . 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`, all 4 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/DebitCreditNoteEventPublisher.java \
        src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/messaging/DebitCreditNoteEventPublisherTest.java
git rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/messaging/EventPublisher.java
git commit -m "Add DebitCreditNoteEventPublisher adapter (delete old EventPublisher)"
```

---

## Task 13: DebitCreditNoteParserServiceImpl (parsing adapter with guards)

**What:** Create `infrastructure/adapter/out/parsing/DebitCreditNoteParserServiceImpl` implementing `DebitCreditNoteParserPort`. Adds size check (500 KB), wall-clock timeout via virtual-thread executor, concurrency cap via `Semaphore`, and XXE-safe SAXParserFactory. Method renamed from `parseNote` to `parse`. Delete the old `infrastructure/service/DebitCreditNoteParserServiceImpl`. Write tests first.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/parsing/DebitCreditNoteParserServiceImpl.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/parsing/DebitCreditNoteParserServiceImplTest.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/service/DebitCreditNoteParserServiceImpl.java`

- [ ] **Step 1: Write the failing tests**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/parsing/DebitCreditNoteParserServiceImplTest.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.parsing;

import com.wpanther.debitcreditnote.processing.domain.port.out.DebitCreditNoteParserPort;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessedDebitCreditNote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebitCreditNoteParserServiceImplTest {

    private DebitCreditNoteParserServiceImpl parser;

    @BeforeEach
    void setUp() {
        // Package-private constructor: 10-second timeout, single-threaded (no concurrency cap needed)
        parser = new DebitCreditNoteParserServiceImpl(10, TimeUnit.SECONDS);
    }

    @Test
    void parse_nullContent_throwsParsingExceptionForEmpty() {
        assertThatThrownBy(() -> parser.parse(null, "source-1"))
            .isInstanceOf(DebitCreditNoteParserPort.ParsingException.class)
            .hasMessageContaining("null or empty");
    }

    @Test
    void parse_blankContent_throwsParsingExceptionForEmpty() {
        assertThatThrownBy(() -> parser.parse("   ", "source-1"))
            .isInstanceOf(DebitCreditNoteParserPort.ParsingException.class)
            .hasMessageContaining("null or empty");
    }

    @Test
    void parse_oversizedContent_throwsParsingExceptionForOversized() {
        // Create a string > 500 KB
        String oversized = "A".repeat(501 * 1024);

        assertThatThrownBy(() -> parser.parse(oversized, "source-1"))
            .isInstanceOf(DebitCreditNoteParserPort.ParsingException.class)
            .hasMessageContaining("too large");
    }

    @Test
    void parse_malformedXml_throwsParsingExceptionForUnmarshal() {
        String malformed = "<not-valid-xml";

        assertThatThrownBy(() -> parser.parse(malformed, "source-1"))
            .isInstanceOf(DebitCreditNoteParserPort.ParsingException.class);
    }

    @Test
    void parse_wrongRootElement_throwsParsingExceptionForUnexpectedRootElement() {
        // Valid XML but wrong root element (not DebitCreditNote_CrossIndustryInvoice)
        String wrongRoot = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Invoice></Invoice>";

        assertThatThrownBy(() -> parser.parse(wrongRoot, "source-1"))
            .isInstanceOf(DebitCreditNoteParserPort.ParsingException.class);
    }

    @Test
    void parse_timeout_throwsParsingExceptionForTimeout() {
        // Very short timeout to force a timeout on a large-enough (but valid-sized) input
        DebitCreditNoteParserServiceImpl shortTimeoutParser =
            new DebitCreditNoteParserServiceImpl(1, TimeUnit.MILLISECONDS);

        // Use content near the size limit to make parsing slow enough to timeout
        String content = "<?xml version=\"1.0\"?>" + "<x>".repeat(10000) + "</x>".repeat(10000);

        assertThatThrownBy(() -> shortTimeoutParser.parse(content, "source-1"))
            .isInstanceOf(DebitCreditNoteParserPort.ParsingException.class)
            .hasMessageContaining("timed out");
    }
}
```

- [ ] **Step 2: Run tests — verify they fail**

```bash
mvn test -Dtest=DebitCreditNoteParserServiceImplTest -pl . 2>&1 | tail -10
```
Expected: compilation error — `DebitCreditNoteParserServiceImpl` in the new package does not exist.

- [ ] **Step 3: Create the new parser impl AND delete the old one**

Create `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/parsing/DebitCreditNoteParserServiceImpl.java`:

```java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.parsing;

import com.wpanther.debitcreditnote.processing.domain.model.*;
import com.wpanther.debitcreditnote.processing.domain.port.out.DebitCreditNoteParserPort;
import com.wpanther.etax.generated.debitcreditnote.ram.*;
import com.wpanther.etax.generated.debitcreditnote.rsm.DebitCreditNote_CrossIndustryInvoiceType;
import jakarta.annotation.PreDestroy;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class DebitCreditNoteParserServiceImpl implements DebitCreditNoteParserPort {

    static final int MAX_XML_BYTES = 500 * 1024; // 500 KB

    private final JAXBContext jaxbContext;
    private final SAXParserFactory saxParserFactory;
    private final long parseTimeoutMs;
    private final int defaultDueDateDays;
    private final ExecutorService parseExecutor;
    private final Semaphore parseSemaphore;

    @org.springframework.beans.factory.annotation.Autowired
    public DebitCreditNoteParserServiceImpl(
            @Value("${app.parsing.timeout-seconds:10}") int parseTimeoutSeconds,
            @Value("${app.parsing.max-concurrent:300}") int maxConcurrentParses,
            @Value("${app.debitcreditnote.default-due-date-days:30}") int defaultDueDateDays) {
        this(TimeUnit.SECONDS.toMillis(parseTimeoutSeconds), defaultDueDateDays, maxConcurrentParses);
    }

    DebitCreditNoteParserServiceImpl(long timeout, TimeUnit unit) {
        this(unit.toMillis(timeout), 30, Integer.MAX_VALUE);
    }

    DebitCreditNoteParserServiceImpl(long timeout, TimeUnit unit, int defaultDueDateDays) {
        this(unit.toMillis(timeout), defaultDueDateDays, Integer.MAX_VALUE);
    }

    DebitCreditNoteParserServiceImpl(long timeout, TimeUnit unit, int defaultDueDateDays, int maxConcurrentParses) {
        this(unit.toMillis(timeout), defaultDueDateDays, maxConcurrentParses);
    }

    DebitCreditNoteParserServiceImpl() {
        this(TimeUnit.SECONDS.toMillis(10), 30, Integer.MAX_VALUE);
    }

    private DebitCreditNoteParserServiceImpl(long parseTimeoutMs, int defaultDueDateDays, int maxConcurrentParses) {
        this.parseTimeoutMs = parseTimeoutMs;
        this.defaultDueDateDays = defaultDueDateDays;
        this.parseSemaphore = new Semaphore(maxConcurrentParses);
        this.parseExecutor = Executors.newVirtualThreadPerTaskExecutor();

        try {
            String contextPath = "com.wpanther.etax.generated.debitcreditnote.rsm.impl" +
                               ":com.wpanther.etax.generated.debitcreditnote.ram.impl" +
                               ":com.wpanther.etax.generated.common.qdt.impl" +
                               ":com.wpanther.etax.generated.common.udt.impl";
            this.jaxbContext = JAXBContext.newInstance(contextPath);
            log.info("JAXB context initialized successfully for Thai e-Tax debit/credit note parsing");
        } catch (JAXBException e) {
            log.error("Failed to initialize JAXB context", e);
            throw new IllegalStateException("Failed to initialize XML parser", e);
        }

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            this.saxParserFactory = factory;
        } catch (ParserConfigurationException | org.xml.sax.SAXException e) {
            throw new IllegalStateException("Failed to initialize secure SAX parser factory", e);
        }
    }

    @PreDestroy
    public void shutdownExecutor() {
        parseExecutor.shutdown();
    }

    @Override
    public ProcessedDebitCreditNote parse(String xmlContent, String sourceNoteId)
            throws DebitCreditNoteParserPort.ParsingException {

        log.debug("Starting XML parsing for source note ID: {}", sourceNoteId);

        try {
            DebitCreditNote_CrossIndustryInvoiceType jaxbNote = unmarshalXml(xmlContent);

            ExchangedDocumentType document = jaxbNote.getExchangedDocument();
            if (document == null) {
                throw new ParsingException("DebitCreditNote XML missing required ExchangedDocument element");
            }

            SupplyChainTradeTransactionType transaction = jaxbNote.getSupplyChainTradeTransaction();
            if (transaction == null) {
                throw new ParsingException("DebitCreditNote XML missing required SupplyChainTradeTransaction element");
            }

            LocalDate issueDate = extractIssueDate(document);
            String currency = extractCurrency(transaction);

            ProcessedDebitCreditNote note = ProcessedDebitCreditNote.builder()
                .id(DebitCreditNoteId.generate())
                .sourceNoteId(sourceNoteId)
                .noteNumber(extractNoteNumber(document))
                .noteType(extractNoteType(document))
                .issueDate(issueDate)
                .dueDate(extractDueDate(transaction, issueDate))
                .seller(extractSeller(transaction))
                .buyer(extractBuyer(transaction))
                .items(extractLineItems(transaction, currency))
                .currency(currency)
                .originalXml(xmlContent)
                .build();

            log.info("Successfully parsed debit/credit note {} with {} line items",
                note.getNoteNumber(), note.getItems().size());

            return note;

        } catch (ParsingException e) {
            log.error("Failed to parse debit/credit note XML for source ID {}: {}", sourceNoteId, e.getMessage());
            throw e;
        }
    }

    private DebitCreditNote_CrossIndustryInvoiceType unmarshalXml(String xmlContent)
            throws ParsingException {

        if (xmlContent == null || xmlContent.isBlank()) {
            throw ParsingException.forEmpty();
        }

        int byteSize = xmlContent.getBytes(StandardCharsets.UTF_8).length;
        if (byteSize > MAX_XML_BYTES) {
            throw ParsingException.forOversized(byteSize, MAX_XML_BYTES);
        }

        try {
            parseSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ParsingException.forInterrupted();
        }

        try {
            Future<DebitCreditNote_CrossIndustryInvoiceType> future = parseExecutor.submit(() -> doUnmarshal(xmlContent));
            return future.get(parseTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw ParsingException.forTimeout(parseTimeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ParsingException.forInterrupted();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ParsingException pe) throw pe;
            throw ParsingException.forUnmarshal(cause != null ? cause : e);
        } finally {
            parseSemaphore.release();
        }
    }

    private DebitCreditNote_CrossIndustryInvoiceType doUnmarshal(String xmlContent) throws ParsingException {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            InputSource inputSource = new InputSource(new StringReader(xmlContent));
            SAXSource saxSource = new SAXSource(saxParserFactory.newSAXParser().getXMLReader(), inputSource);
            Object result = unmarshaller.unmarshal(saxSource);

            if (result instanceof DebitCreditNote_CrossIndustryInvoiceType note) {
                return note;
            }
            throw ParsingException.forUnexpectedRootElement(result == null ? "null" : result.getClass().getName());
        } catch (ParsingException e) {
            throw e;
        } catch (Exception e) {
            throw ParsingException.forUnmarshal(e);
        }
    }

    // ---- Extraction helpers (adapted from old DebitCreditNoteParserServiceImpl) ----

    private String extractNoteNumber(ExchangedDocumentType document) {
        return Optional.ofNullable(document.getID())
            .map(id -> id.getValue())
            .orElse("UNKNOWN");
    }

    private String extractNoteType(ExchangedDocumentType document) {
        return Optional.ofNullable(document.getTypeCode())
            .map(code -> code.getValue())
            .orElse("UNKNOWN");
    }

    private LocalDate extractIssueDate(ExchangedDocumentType document) {
        return Optional.ofNullable(document.getIssueDateTime())
            .map(dt -> dt.getDateTimeString())
            .map(dts -> dts.getValue())
            .map(this::parseDate)
            .orElse(LocalDate.now());
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr.substring(0, 8),
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private String extractCurrency(SupplyChainTradeTransactionType transaction) {
        return Optional.ofNullable(transaction.getApplicableHeaderTradeSettlement())
            .map(HeaderTradeSettlementType::getInvoiceCurrencyCode)
            .map(code -> code.getValue())
            .orElse("THB");
    }

    private LocalDate extractDueDate(SupplyChainTradeTransactionType transaction, LocalDate issueDate) {
        return Optional.ofNullable(transaction.getApplicableHeaderTradeSettlement())
            .map(HeaderTradeSettlementType::getSpecifiedTradePaymentTerms)
            .filter(terms -> !terms.isEmpty())
            .map(terms -> terms.get(0))
            .map(TradePaymentTermsType::getDueDateDateTime)
            .map(dt -> dt.getDateTimeString())
            .map(dts -> dts.getValue())
            .map(this::parseDate)
            .orElse(issueDate.plusDays(defaultDueDateDays));
    }

    private Party extractSeller(SupplyChainTradeTransactionType transaction) {
        return Optional.ofNullable(transaction.getApplicableHeaderTradeAgreement())
            .map(HeaderTradeAgreementType::getSellerTradeParty)
            .map(this::toParty)
            .orElseThrow(() -> new IllegalStateException("Seller not found in XML"));
    }

    private Party extractBuyer(SupplyChainTradeTransactionType transaction) {
        return Optional.ofNullable(transaction.getApplicableHeaderTradeAgreement())
            .map(HeaderTradeAgreementType::getBuyerTradeParty)
            .map(this::toParty)
            .orElseThrow(() -> new IllegalStateException("Buyer not found in XML"));
    }

    private Party toParty(TradePartyType tradeParty) {
        String name = Optional.ofNullable(tradeParty.getName())
            .map(n -> n.getValue()).orElse("");

        String taxId = Optional.ofNullable(tradeParty.getSpecifiedTaxRegistration())
            .filter(list -> !list.isEmpty())
            .map(list -> list.get(0))
            .map(TaxRegistrationType::getID)
            .map(id -> id.getValue()).orElse("");

        String taxScheme = Optional.ofNullable(tradeParty.getSpecifiedTaxRegistration())
            .filter(list -> !list.isEmpty())
            .map(list -> list.get(0))
            .map(TaxRegistrationType::getID)
            .map(id -> id.getSchemeID()).orElse("VAT");

        String email = Optional.ofNullable(tradeParty.getDefinedTradeContact())
            .filter(list -> !list.isEmpty())
            .map(list -> list.get(0))
            .map(TradeContactType::getEmailURIUniversalCommunication)
            .map(comm -> comm.getURIID())
            .map(id -> id.getValue()).orElse(null);

        TradeAddressType address = Optional.ofNullable(tradeParty.getPostalTradeAddress()).orElse(null);
        String street = address != null && address.getLineOne() != null ? address.getLineOne().getValue() : "";
        String city = address != null && address.getCityName() != null ? address.getCityName().getValue() : "";
        String postalCode = address != null && address.getPostcodeCode() != null ? address.getPostcodeCode().getValue() : "";
        String country = address != null && address.getCountryID() != null ? address.getCountryID().getValue() : "TH";

        return Party.of(name, TaxIdentifier.of(taxId, taxScheme),
            Address.of(street, city, postalCode, country), email);
    }

    private List<LineItem> extractLineItems(SupplyChainTradeTransactionType transaction, String currency) {
        List<LineItem> items = new ArrayList<>();
        List<SupplyChainTradeLineItemType> lineItems = transaction.getIncludedSupplyChainTradeLineItem();
        if (lineItems == null) return items;

        for (SupplyChainTradeLineItemType lineItem : lineItems) {
            try {
                String description = Optional.ofNullable(lineItem.getSpecifiedTradeProduct())
                    .map(TradeProductType::getName)
                    .filter(list -> !list.isEmpty())
                    .map(list -> list.get(0))
                    .map(n -> n.getValue()).orElse("Unknown");

                int quantity = Optional.ofNullable(lineItem.getSpecifiedLineTradeDelivery())
                    .map(LineTradeDeliveryType::getBilledQuantity)
                    .map(q -> q.getValue())
                    .map(BigDecimal::intValue).orElse(1);

                BigDecimal unitPrice = Optional.ofNullable(lineItem.getSpecifiedLineTradeAgreement())
                    .map(LineTradeAgreementType::getNetPriceProductTradePrice)
                    .map(TradePriceType::getChargeAmount)
                    .filter(list -> !list.isEmpty())
                    .map(list -> list.get(0))
                    .map(amt -> amt.getValue()).orElse(BigDecimal.ZERO);

                BigDecimal taxRate = Optional.ofNullable(lineItem.getSpecifiedLineTradeSettlement())
                    .map(LineTradeSettlementType::getApplicableTradeTax)
                    .filter(list -> !list.isEmpty())
                    .map(list -> list.get(0))
                    .map(TradeTaxType::getRateApplicablePercent)
                    .map(pct -> pct.getValue()).orElse(BigDecimal.valueOf(7));

                items.add(new LineItem(description, quantity, Money.of(unitPrice, currency), taxRate));
            } catch (Exception e) {
                log.warn("Failed to parse line item: {}", e.getMessage());
            }
        }
        return items;
    }
}
```

Then delete the old file:
```bash
rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/service/DebitCreditNoteParserServiceImpl.java
```

- [ ] **Step 4: Run tests — verify they pass**

```bash
mvn test -Dtest=DebitCreditNoteParserServiceImplTest -pl . 2>&1 | tail -15
```
Expected: `BUILD SUCCESS`. The `parse_timeout` test may be flaky if the JVM is under load — if so, increase the delay or skip it and add `@Disabled("flaky on slow CI")`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/parsing/DebitCreditNoteParserServiceImpl.java \
        src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/parsing/DebitCreditNoteParserServiceImplTest.java
git rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/service/DebitCreditNoteParserServiceImpl.java
git commit -m "Add DebitCreditNoteParserServiceImpl adapter with size/timeout/concurrency guards (delete old impl)"
```

---

## Task 14: Persistence entities and mapper (move to adapter/out/persistence/)

**What:** Copy the three JPA entity classes and the mapper to `infrastructure/adapter/out/persistence/`, updating package declarations. Delete the old files at `infrastructure/persistence/` in the same commit to avoid dual `@Entity` registration on the same table. Write entity tests.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteEntity.java`
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/DebitCreditNotePartyEntity.java`
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/DebitCreditNoteLineItemEntity.java`
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteMapper.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteEntityTest.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/DebitCreditNotePartyEntityTest.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/DebitCreditNoteLineItemEntityTest.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteMapperTest.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/ProcessedDebitCreditNoteEntity.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/DebitCreditNotePartyEntity.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/DebitCreditNoteLineItemEntity.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/ProcessedDebitCreditNoteMapper.java`

**Note:** The old `ProcessedDebitCreditNoteRepositoryImpl` and `JpaProcessedDebitCreditNoteRepository` still reference the old package. They will fail to compile once the old entity classes are deleted. This is expected — they will be replaced in Task 15. Run `mvn test -Dtest=ProcessedDebitCreditNoteEntityTest,...` (specific tests only, not full build) until Task 15 is done.

- [ ] **Step 1: Write the entity tests**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteEntityTest.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import com.wpanther.debitcreditnote.processing.domain.model.ProcessingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedDebitCreditNoteEntityTest {

    @Test
    void prePersist_generatesIdWhenNull() {
        ProcessedDebitCreditNoteEntity entity = new ProcessedDebitCreditNoteEntity();
        entity.onCreate();
        assertThat(entity.getId()).isNotNull();
    }

    @Test
    void prePersist_doesNotOverwriteExistingId() {
        UUID existingId = UUID.randomUUID();
        ProcessedDebitCreditNoteEntity entity = ProcessedDebitCreditNoteEntity.builder()
            .id(existingId)
            .build();
        entity.onCreate();
        assertThat(entity.getId()).isEqualTo(existingId);
    }

    @Test
    void addParty_linksBidirectionally() {
        ProcessedDebitCreditNoteEntity note = ProcessedDebitCreditNoteEntity.builder()
            .id(UUID.randomUUID())
            .sourceNoteId("src-1")
            .noteNumber("CN-001")
            .noteType("380")
            .issueDate(LocalDate.now())
            .dueDate(LocalDate.now().plusDays(30))
            .currency("THB")
            .subtotal(BigDecimal.valueOf(1000))
            .totalTax(BigDecimal.valueOf(70))
            .total(BigDecimal.valueOf(1070))
            .originalXml("<xml/>")
            .status(ProcessingStatus.PENDING)
            .build();

        DebitCreditNotePartyEntity party = new DebitCreditNotePartyEntity();
        note.addParty(party);

        assertThat(note.getParties()).contains(party);
        assertThat(party.getNote()).isEqualTo(note);
    }
}
```

```java
// src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/DebitCreditNotePartyEntityTest.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DebitCreditNotePartyEntityTest {

    @Test
    void prePersist_generatesIdWhenNull() {
        DebitCreditNotePartyEntity entity = new DebitCreditNotePartyEntity();
        entity.onCreate();
        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
    }

    @Test
    void prePersist_doesNotOverwriteExistingId() {
        UUID existingId = UUID.randomUUID();
        DebitCreditNotePartyEntity entity = DebitCreditNotePartyEntity.builder().id(existingId).build();
        entity.onCreate();
        assertThat(entity.getId()).isEqualTo(existingId);
    }
}
```

```java
// src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/DebitCreditNoteLineItemEntityTest.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DebitCreditNoteLineItemEntityTest {

    @Test
    void prePersist_generatesIdWhenNull() {
        DebitCreditNoteLineItemEntity entity = new DebitCreditNoteLineItemEntity();
        entity.onCreate();
        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
    }

    @Test
    void prePersist_doesNotOverwriteExistingId() {
        UUID existingId = UUID.randomUUID();
        DebitCreditNoteLineItemEntity entity = DebitCreditNoteLineItemEntity.builder().id(existingId).build();
        entity.onCreate();
        assertThat(entity.getId()).isEqualTo(existingId);
    }
}
```

```java
// src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteMapperTest.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import com.wpanther.debitcreditnote.processing.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedDebitCreditNoteMapperTest {

    private ProcessedDebitCreditNoteMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ProcessedDebitCreditNoteMapper();
    }

    @Test
    void toEntity_andToDomain_roundTrip_preservesNoteNumber() {
        Party seller = Party.of("Seller Co", TaxIdentifier.of("1234567890123", "VAT"),
            Address.of("123 Main St", "Bangkok", "10110", "TH"), "seller@example.com");
        Party buyer = Party.of("Buyer Co", TaxIdentifier.of("9876543210987", "VAT"),
            Address.of("456 Side St", "Bangkok", "10120", "TH"), "buyer@example.com");

        ProcessedDebitCreditNote original = ProcessedDebitCreditNote.builder()
            .id(DebitCreditNoteId.generate())
            .sourceNoteId("src-1")
            .noteNumber("CN-001")
            .noteType("380")
            .issueDate(LocalDate.of(2026, 4, 27))
            .dueDate(LocalDate.of(2026, 5, 27))
            .seller(seller)
            .buyer(buyer)
            .items(List.of())
            .currency("THB")
            .originalXml("<xml/>")
            .status(ProcessingStatus.COMPLETED)
            .build();

        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(original);
        ProcessedDebitCreditNote restored = mapper.toDomain(entity);

        assertThat(restored.getNoteNumber()).isEqualTo("CN-001");
        assertThat(restored.getSourceNoteId()).isEqualTo("src-1");
        assertThat(restored.getStatus()).isEqualTo(ProcessingStatus.COMPLETED);
    }
}
```

- [ ] **Step 2: Run the new-location tests — verify they fail**

```bash
mvn test -Dtest="ProcessedDebitCreditNoteEntityTest,DebitCreditNotePartyEntityTest,DebitCreditNoteLineItemEntityTest,ProcessedDebitCreditNoteMapperTest" -pl . 2>&1 | tail -10
```
Expected: compilation error — classes do not exist in new package yet.

- [ ] **Step 3: Create entities and mapper in new location, delete old files**

Create `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteEntity.java` — same content as the old file but change the package declaration to `infrastructure.adapter.out.persistence` and update cross-references within the same file:

```java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import com.wpanther.debitcreditnote.processing.domain.model.ProcessingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "processed_debit_credit_notes", indexes = {
    @Index(name = "idx_note_number", columnList = "note_number"),
    @Index(name = "idx_source_note_id", columnList = "source_note_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_issue_date", columnList = "issue_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedDebitCreditNoteEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "source_note_id", nullable = false, length = 100)
    private String sourceNoteId;

    @Column(name = "note_number", nullable = false, length = 50)
    private String noteNumber;

    @Column(name = "note_type", nullable = false, length = 20)
    private String noteType;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalTax;

    @Column(name = "total", nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    @Column(name = "original_xml", nullable = false, columnDefinition = "TEXT")
    private String originalXml;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcessingStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<DebitCreditNotePartyEntity> parties = new HashSet<>();

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<DebitCreditNoteLineItemEntity> lineItems = new ArrayList<>();

    public void addParty(DebitCreditNotePartyEntity party) {
        parties.add(party);
        party.setNote(this);
    }

    public void addLineItem(DebitCreditNoteLineItemEntity lineItem) {
        lineItems.add(lineItem);
        lineItem.setNote(this);
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
```

Create `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/DebitCreditNotePartyEntity.java`:

```java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "debit_credit_note_parties", indexes = {
    @Index(name = "idx_party_note_id", columnList = "note_id"),
    @Index(name = "idx_party_type", columnList = "party_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebitCreditNotePartyEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false, foreignKey = @ForeignKey(name = "fk_party_note"))
    private ProcessedDebitCreditNoteEntity note;

    @Column(name = "party_type", nullable = false, length = 20)
    private String partyType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "tax_id", nullable = false, length = 50)
    private String taxId;

    @Column(name = "tax_scheme", nullable = false, length = 20)
    private String taxScheme;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "street_address", length = 500)
    private String streetAddress;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 3)
    private String country;

    @Column(name = "created_at", nullable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = java.time.LocalDateTime.now();
        }
    }
}
```

Create `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/DebitCreditNoteLineItemEntity.java`:

```java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "debit_credit_note_line_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"note_id", "line_number"}),
       indexes = {
           @Index(name = "idx_line_note_id", columnList = "note_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebitCreditNoteLineItemEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lineitem_note"))
    private ProcessedDebitCreditNoteEntity note;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_with_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalWithTax;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
```

Create `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteMapper.java` — same content as old file, new package, updated imports:

```java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import com.wpanther.debitcreditnote.processing.domain.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProcessedDebitCreditNoteMapper {

    public ProcessedDebitCreditNoteEntity toEntity(ProcessedDebitCreditNote domain) {
        ProcessedDebitCreditNoteEntity entity = ProcessedDebitCreditNoteEntity.builder()
                .id(domain.getId().getValue())
                .sourceNoteId(domain.getSourceNoteId())
                .noteNumber(domain.getNoteNumber())
                .noteType(domain.getNoteType())
                .issueDate(domain.getIssueDate())
                .dueDate(domain.getDueDate())
                .currency(domain.getCurrency())
                .subtotal(domain.getSubtotal().amount())
                .totalTax(domain.getTotalTax().amount())
                .total(domain.getTotal().amount())
                .originalXml(domain.getOriginalXml())
                .status(domain.getStatus())
                .errorMessage(domain.getErrorMessage())
                .createdAt(domain.getCreatedAt())
                .completedAt(domain.getCompletedAt())
                .build();

        entity.addParty(toPartyEntity(domain.getSeller(), "SELLER", entity));
        entity.addParty(toPartyEntity(domain.getBuyer(), "BUYER", entity));

        int lineNumber = 1;
        for (LineItem item : domain.getItems()) {
            entity.addLineItem(toLineItemEntity(item, lineNumber++, entity));
        }

        return entity;
    }

    public ProcessedDebitCreditNote toDomain(ProcessedDebitCreditNoteEntity entity) {
        Set<Party> parties = entity.getParties().stream()
                .map(this::toDomainParty)
                .collect(Collectors.toSet());

        Party seller = parties.stream()
                .filter(p -> "SELLER".equals(getPartyType(entity, p)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seller not found"));

        Party buyer = parties.stream()
                .filter(p -> "BUYER".equals(getPartyType(entity, p)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Buyer not found"));

        return ProcessedDebitCreditNote.builder()
                .id(DebitCreditNoteId.of(entity.getId()))
                .sourceNoteId(entity.getSourceNoteId())
                .noteNumber(entity.getNoteNumber())
                .noteType(entity.getNoteType())
                .issueDate(entity.getIssueDate())
                .dueDate(entity.getDueDate())
                .seller(seller)
                .buyer(buyer)
                .items(entity.getLineItems().stream()
                        .map(this::toDomainLineItem)
                        .collect(Collectors.toList()))
                .currency(entity.getCurrency())
                .originalXml(entity.getOriginalXml())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .completedAt(entity.getCompletedAt())
                .errorMessage(entity.getErrorMessage())
                .build();
    }

    private DebitCreditNotePartyEntity toPartyEntity(Party domain, String partyType,
                                                      ProcessedDebitCreditNoteEntity noteEntity) {
        return DebitCreditNotePartyEntity.builder()
                .partyType(partyType)
                .name(domain.name())
                .taxId(domain.taxIdentifier().taxId())
                .taxScheme(domain.taxIdentifier().scheme())
                .email(domain.email())
                .streetAddress(domain.address().street())
                .city(domain.address().city())
                .postalCode(domain.address().postalCode())
                .country(domain.address().country())
                .note(noteEntity)
                .build();
    }

    private DebitCreditNoteLineItemEntity toLineItemEntity(LineItem domain, int lineNumber,
                                                            ProcessedDebitCreditNoteEntity noteEntity) {
        return DebitCreditNoteLineItemEntity.builder()
                .lineNumber(lineNumber)
                .description(domain.description())
                .quantity(domain.quantity())
                .unitPrice(domain.unitPrice().amount())
                .taxRate(domain.taxRate())
                .lineTotal(domain.getLineTotal().amount())
                .taxAmount(domain.getTaxAmount().amount())
                .totalWithTax(domain.getTotalWithTax().amount())
                .note(noteEntity)
                .build();
    }

    private Party toDomainParty(DebitCreditNotePartyEntity entity) {
        return Party.of(
                entity.getName(),
                TaxIdentifier.of(entity.getTaxId(), entity.getTaxScheme()),
                Address.of(entity.getStreetAddress(), entity.getCity(),
                           entity.getPostalCode(), entity.getCountry()),
                entity.getEmail()
        );
    }

    private LineItem toDomainLineItem(DebitCreditNoteLineItemEntity entity) {
        return new LineItem(
                entity.getDescription(),
                entity.getQuantity(),
                Money.of(entity.getUnitPrice(), "THB"),
                entity.getTaxRate()
        );
    }

    private String getPartyType(ProcessedDebitCreditNoteEntity noteEntity, Party party) {
        return noteEntity.getParties().stream()
                .filter(pe -> pe.getName().equals(party.name()))
                .findFirst()
                .map(DebitCreditNotePartyEntity::getPartyType)
                .orElse("UNKNOWN");
    }
}
```

Delete old files:
```bash
rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/ProcessedDebitCreditNoteEntity.java
rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/DebitCreditNotePartyEntity.java
rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/DebitCreditNoteLineItemEntity.java
rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/ProcessedDebitCreditNoteMapper.java
```

- [ ] **Step 4: Run entity/mapper tests only — verify they pass**

```bash
mvn test -Dtest="ProcessedDebitCreditNoteEntityTest,DebitCreditNotePartyEntityTest,DebitCreditNoteLineItemEntityTest,ProcessedDebitCreditNoteMapperTest" -pl . 2>&1 | tail -10
```
Expected: `BUILD SUCCESS` for these specific tests. Full `mvn verify` will still fail because `ProcessedDebitCreditNoteRepositoryImpl` still imports from the old package — that is expected and fixed in Task 15.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteEntity.java \
        src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/DebitCreditNotePartyEntity.java \
        src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/DebitCreditNoteLineItemEntity.java \
        src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteMapper.java \
        src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteEntityTest.java \
        src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/DebitCreditNotePartyEntityTest.java \
        src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/DebitCreditNoteLineItemEntityTest.java \
        src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteMapperTest.java
git rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/ProcessedDebitCreditNoteEntity.java \
       src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/DebitCreditNotePartyEntity.java \
       src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/DebitCreditNoteLineItemEntity.java \
       src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/ProcessedDebitCreditNoteMapper.java
git commit -m "Move JPA entities and mapper to adapter/out/persistence/ (delete old persistence/ files)"
```

---

## Task 15: Repository impl and JPA interface (move and trim)

**What:** Create the trimmed `ProcessedDebitCreditNoteRepositoryImpl` and `JpaProcessedDebitCreditNoteRepository` in `infrastructure/adapter/out/persistence/`. Remove the 3 extra methods (`findByIdWithDetails`, `findByStatusWithDetails`, `existsBySourceNoteId`) to match the 5-method port contract. Delete the old files. Write a repository impl test.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteRepositoryImpl.java`
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/JpaProcessedDebitCreditNoteRepository.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteRepositoryImplTest.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/ProcessedDebitCreditNoteRepositoryImpl.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/JpaProcessedDebitCreditNoteRepository.java`

- [ ] **Step 1: Write the failing test**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteRepositoryImplTest.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import com.wpanther.debitcreditnote.processing.domain.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessedDebitCreditNoteRepositoryImplTest {

    @Mock
    private JpaProcessedDebitCreditNoteRepository jpaRepository;

    @Mock
    private ProcessedDebitCreditNoteMapper mapper;

    @InjectMocks
    private ProcessedDebitCreditNoteRepositoryImpl repository;

    @Test
    void save_mapsAndDelegatesAndReturnsResult() {
        ProcessedDebitCreditNote domain = buildMinimalDomainNote();
        ProcessedDebitCreditNoteEntity entity = new ProcessedDebitCreditNoteEntity();
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        ProcessedDebitCreditNote result = repository.save(domain);

        assertThat(result).isSameAs(domain);
        verify(jpaRepository).save(entity);
    }

    @Test
    void findBySourceNoteId_returnsEmptyWhenNotFound() {
        when(jpaRepository.findBySourceNoteId("missing")).thenReturn(Optional.empty());

        Optional<ProcessedDebitCreditNote> result = repository.findBySourceNoteId("missing");

        assertThat(result).isEmpty();
    }

    @Test
    void findBySourceNoteId_returnsMappedDomainWhenFound() {
        ProcessedDebitCreditNoteEntity entity = new ProcessedDebitCreditNoteEntity();
        ProcessedDebitCreditNote domain = buildMinimalDomainNote();
        when(jpaRepository.findBySourceNoteId("src-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<ProcessedDebitCreditNote> result = repository.findBySourceNoteId("src-1");

        assertThat(result).contains(domain);
    }

    @Test
    void deleteById_delegatesToJpaRepository() {
        UUID id = UUID.randomUUID();
        DebitCreditNoteId noteId = DebitCreditNoteId.of(id);

        repository.deleteById(noteId);

        verify(jpaRepository).deleteById(id);
    }

    private ProcessedDebitCreditNote buildMinimalDomainNote() {
        return ProcessedDebitCreditNote.builder()
            .id(DebitCreditNoteId.generate())
            .sourceNoteId("src-1")
            .noteNumber("CN-001")
            .noteType("380")
            .issueDate(LocalDate.now())
            .dueDate(LocalDate.now().plusDays(30))
            .seller(Party.of("Seller", TaxIdentifier.of("1234567890123", "VAT"),
                Address.of("St", "City", "10000", "TH"), null))
            .buyer(Party.of("Buyer", TaxIdentifier.of("9876543210987", "VAT"),
                Address.of("St", "City", "10000", "TH"), null))
            .items(java.util.List.of())
            .currency("THB")
            .originalXml("<xml/>")
            .status(ProcessingStatus.PENDING)
            .build();
    }
}
```

- [ ] **Step 2: Run test — verify it fails**

```bash
mvn test -Dtest=ProcessedDebitCreditNoteRepositoryImplTest -pl . 2>&1 | tail -10
```
Expected: compilation error — new `ProcessedDebitCreditNoteRepositoryImpl` does not exist.

- [ ] **Step 3: Create trimmed repository impl and JPA interface, delete old files**

Create `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/JpaProcessedDebitCreditNoteRepository.java`:

```java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaProcessedDebitCreditNoteRepository
        extends JpaRepository<ProcessedDebitCreditNoteEntity, UUID> {

    @Query("SELECT n FROM ProcessedDebitCreditNoteEntity n WHERE n.noteNumber = :noteNumber")
    Optional<ProcessedDebitCreditNoteEntity> findByNoteNumber(@Param("noteNumber") String noteNumber);

    @Query("SELECT n FROM ProcessedDebitCreditNoteEntity n WHERE n.sourceNoteId = :sourceNoteId")
    Optional<ProcessedDebitCreditNoteEntity> findBySourceNoteId(@Param("sourceNoteId") String sourceNoteId);

    void deleteById(UUID id);
}
```

Create `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteRepositoryImpl.java`:

```java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import com.wpanther.debitcreditnote.processing.domain.model.DebitCreditNoteId;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessedDebitCreditNote;
import com.wpanther.debitcreditnote.processing.domain.port.out.ProcessedDebitCreditNoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ProcessedDebitCreditNoteRepositoryImpl implements ProcessedDebitCreditNoteRepository {

    private final JpaProcessedDebitCreditNoteRepository jpaRepository;
    private final ProcessedDebitCreditNoteMapper mapper;

    @Override
    public ProcessedDebitCreditNote save(ProcessedDebitCreditNote note) {
        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(note);
        ProcessedDebitCreditNoteEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ProcessedDebitCreditNote> findById(DebitCreditNoteId id) {
        return jpaRepository.findById(id.getValue()).map(mapper::toDomain);
    }

    @Override
    public Optional<ProcessedDebitCreditNote> findByNoteNumber(String noteNumber) {
        return jpaRepository.findByNoteNumber(noteNumber).map(mapper::toDomain);
    }

    @Override
    public Optional<ProcessedDebitCreditNote> findBySourceNoteId(String sourceNoteId) {
        return jpaRepository.findBySourceNoteId(sourceNoteId).map(mapper::toDomain);
    }

    @Override
    public void deleteById(DebitCreditNoteId id) {
        jpaRepository.deleteById(id.getValue());
    }
}
```

Delete old files:
```bash
rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/ProcessedDebitCreditNoteRepositoryImpl.java
rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/JpaProcessedDebitCreditNoteRepository.java
```

- [ ] **Step 4: Run repository tests — verify they pass**

```bash
mvn test -Dtest=ProcessedDebitCreditNoteRepositoryImplTest -pl . 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`, all 4 tests pass. At this point the `infrastructure/persistence/` directory should be empty (all files deleted across Tasks 14–15). Run `mvn verify` — it should now pass for the persistence adapter package.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/JpaProcessedDebitCreditNoteRepository.java \
        src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteRepositoryImpl.java \
        src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/ProcessedDebitCreditNoteRepositoryImplTest.java
git rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/ProcessedDebitCreditNoteRepositoryImpl.java \
       src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/JpaProcessedDebitCreditNoteRepository.java
git commit -m "Add trimmed repository impl/JPA interface in adapter/out/persistence/ (delete old persistence/ files)"
```

---

## Task 16: Outbox files (move and add OutboxCleanupScheduler)

**What:** Move `OutboxEventEntity`, `JpaOutboxEventRepository`, and `SpringDataOutboxRepository` from `infrastructure/persistence/outbox/` to `infrastructure/adapter/out/persistence/outbox/` (package update only). Add new `OutboxCleanupScheduler` matching the reference. Delete old files. Write tests for the entity and the scheduler.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/OutboxEventEntity.java`
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/JpaOutboxEventRepository.java`
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/SpringDataOutboxRepository.java`
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/OutboxCleanupScheduler.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/OutboxEventEntityTest.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/OutboxCleanupSchedulerTest.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/JpaOutboxEventRepositoryTest.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/outbox/OutboxEventEntity.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/outbox/JpaOutboxEventRepository.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/outbox/SpringDataOutboxRepository.java`

**Note:** `SagaReplyPublisherTransactionTest` imports `SpringDataOutboxRepository` from the new package. That import was already written correctly in Task 11's test code. Ensure the class exists in the new location before running the transaction test.

- [ ] **Step 1: Write the tests**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/OutboxEventEntityTest.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence.outbox;

import com.wpanther.saga.domain.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventEntityTest {

    @Test
    void prePersist_setsDefaultsWhenNull() {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.onCreate();

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getRetryCount()).isEqualTo(0);
    }

    @Test
    void prePersist_doesNotOverwriteExistingId() {
        UUID existingId = UUID.randomUUID();
        OutboxEventEntity entity = OutboxEventEntity.builder().id(existingId).build();
        entity.onCreate();

        assertThat(entity.getId()).isEqualTo(existingId);
    }
}
```

```java
// src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/JpaOutboxEventRepositoryTest.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence.outbox;

import com.wpanther.saga.domain.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class JpaOutboxEventRepositoryTest {

    @Autowired
    private JpaOutboxEventRepository repository;

    @Test
    void deletePublishedBefore_deletesOnlyPublishedEvents() {
        OutboxEventEntity published = OutboxEventEntity.builder()
            .aggregateType("ProcessedDebitCreditNote")
            .aggregateId("agg-1")
            .eventType("debitcreditnote.processed")
            .payload("{}")
            .status(OutboxStatus.PUBLISHED)
            .publishedAt(Instant.now().minusSeconds(3600))
            .build();

        OutboxEventEntity pending = OutboxEventEntity.builder()
            .aggregateType("ProcessedDebitCreditNote")
            .aggregateId("agg-2")
            .eventType("debitcreditnote.processed")
            .payload("{}")
            .status(OutboxStatus.PENDING)
            .build();

        repository.save(published);
        repository.save(pending);

        int deleted = repository.deletePublishedBefore(Instant.now());

        assertThat(deleted).isEqualTo(1);
        List<OutboxEventEntity> remaining = repository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getStatus()).isEqualTo(OutboxStatus.PENDING);
    }
}
```

```java
// src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/OutboxCleanupSchedulerTest.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence.outbox;

import com.wpanther.saga.domain.outbox.OutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxCleanupSchedulerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OutboxCleanupScheduler(outboxEventRepository, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(scheduler, "retentionDays", 7);
        ReflectionTestUtils.setField(scheduler, "cleanupCron", "0 0 2 * * *");
    }

    @Test
    void cleanPublishedEvents_callsDeleteWithCutoffDate() {
        when(outboxEventRepository.deletePublishedBefore(any())).thenReturn(5);

        scheduler.cleanPublishedEvents();

        verify(outboxEventRepository).deletePublishedBefore(any());
    }

    @Test
    void cleanPublishedEvents_onException_doesNotPropagate() {
        when(outboxEventRepository.deletePublishedBefore(any()))
            .thenThrow(new RuntimeException("DB down"));

        // must not throw — scheduler catches and increments counter
        scheduler.cleanPublishedEvents();
    }

    @Test
    void logConfiguration_throwsOnInvalidRetentionDays() {
        ReflectionTestUtils.setField(scheduler, "retentionDays", 0);

        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> scheduler.logConfiguration()
        );
    }
}
```

- [ ] **Step 2: Run tests — verify they fail**

```bash
mvn test -Dtest="OutboxEventEntityTest,OutboxCleanupSchedulerTest,JpaOutboxEventRepositoryTest" -pl . 2>&1 | tail -10
```
Expected: compilation errors — new-package classes do not exist.

- [ ] **Step 3: Create new outbox files AND delete old files**

Create `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/OutboxEventEntity.java` — same content as old file, updated package:

```java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence.outbox;

import com.wpanther.saga.domain.outbox.OutboxEvent;
import com.wpanther.saga.domain.outbox.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_status", columnList = "status"),
    @Index(name = "idx_outbox_created", columnList = "created_at"),
    @Index(name = "idx_outbox_aggregate", columnList = "aggregate_id, aggregate_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "topic", length = 255)
    private String topic;

    @Column(name = "partition_key", length = 255)
    private String partitionKey;

    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = OutboxStatus.PENDING;
        if (createdAt == null) createdAt = Instant.now();
        if (retryCount == null) retryCount = 0;
    }

    public static OutboxEventEntity fromDomain(OutboxEvent event) {
        return OutboxEventEntity.builder()
                .id(event.getId())
                .aggregateType(event.getAggregateType())
                .aggregateId(event.getAggregateId())
                .eventType(event.getEventType())
                .payload(event.getPayload())
                .createdAt(event.getCreatedAt())
                .publishedAt(event.getPublishedAt())
                .status(event.getStatus())
                .retryCount(event.getRetryCount())
                .errorMessage(event.getErrorMessage())
                .topic(event.getTopic())
                .partitionKey(event.getPartitionKey())
                .headers(event.getHeaders())
                .build();
    }

    public OutboxEvent toDomain() {
        return OutboxEvent.builder()
                .id(this.id)
                .aggregateType(this.aggregateType)
                .aggregateId(this.aggregateId)
                .eventType(this.eventType)
                .payload(this.payload)
                .createdAt(this.createdAt)
                .publishedAt(this.publishedAt)
                .status(this.status)
                .retryCount(this.retryCount)
                .errorMessage(this.errorMessage)
                .topic(this.topic)
                .partitionKey(this.partitionKey)
                .headers(this.headers)
                .build();
    }
}
```

Create `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/JpaOutboxEventRepository.java`:

```java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence.outbox;

import com.wpanther.saga.domain.outbox.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JpaOutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    @Query("SELECT e FROM OutboxEventEntity e WHERE e.status = 'FAILED' ORDER BY e.createdAt ASC")
    List<OutboxEventEntity> findFailedEventsOrderByCreatedAtAsc(Pageable pageable);

    List<OutboxEventEntity> findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
            String aggregateType, String aggregateId);

    @Modifying
    @Query("DELETE FROM OutboxEventEntity e WHERE e.status = 'PUBLISHED' AND e.publishedAt < :before")
    int deletePublishedBefore(@Param("before") Instant before);
}
```

Create `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/SpringDataOutboxRepository.java`:

```java
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
```

Create `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/OutboxCleanupScheduler.java`:

```java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence.outbox;

import com.wpanther.saga.domain.outbox.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
public class OutboxCleanupScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final Counter cleanupFailureCounter;

    @Value("${app.outbox.cleanup.retention-days:7}")
    private int retentionDays;

    @Value("${app.outbox.cleanup.cron:0 0 2 * * *}")
    private String cleanupCron;

    public OutboxCleanupScheduler(OutboxEventRepository outboxEventRepository,
                                  MeterRegistry meterRegistry) {
        this.outboxEventRepository = outboxEventRepository;
        this.cleanupFailureCounter = Counter.builder("outbox.cleanup.failure")
            .description("Number of times the outbox cleanup job failed")
            .register(meterRegistry);
    }

    @PostConstruct
    void logConfiguration() {
        if (retentionDays < 1) {
            throw new IllegalStateException(
                "app.outbox.cleanup.retention-days must be >= 1, got: " + retentionDays);
        }
        log.info("OutboxCleanupScheduler initialized: retention={} days, cron='{}'",
            retentionDays, cleanupCron);
    }

    @Scheduled(cron = "${app.outbox.cleanup.cron:0 0 2 * * *}")
    public void cleanPublishedEvents() {
        try {
            Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
            int deleted = outboxEventRepository.deletePublishedBefore(cutoff);
            log.info("Outbox cleanup: deleted {} published events older than {} days", deleted, retentionDays);
        } catch (Exception e) {
            cleanupFailureCounter.increment();
            log.error("Outbox cleanup failed: {}", e.toString());
        }
    }
}
```

Delete old files:
```bash
rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/outbox/OutboxEventEntity.java
rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/outbox/JpaOutboxEventRepository.java
rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/outbox/SpringDataOutboxRepository.java
```

- [ ] **Step 4: Run outbox tests — verify they pass**

```bash
mvn test -Dtest="OutboxEventEntityTest,OutboxCleanupSchedulerTest,JpaOutboxEventRepositoryTest" -pl . 2>&1 | tail -15
```
Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 5: Also run the SagaReplyPublisher transaction test now that SpringDataOutboxRepository is in the correct package**

```bash
mvn test -Dtest=SagaReplyPublisherTransactionTest -pl . 2>&1 | tail -15
```
Expected: `BUILD SUCCESS`, all 6 transaction tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/ \
        src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/out/persistence/outbox/
git rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/outbox/OutboxEventEntity.java \
       src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/outbox/JpaOutboxEventRepository.java \
       src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/persistence/outbox/SpringDataOutboxRepository.java
git commit -m "Move outbox files to adapter/out/persistence/outbox/ and add OutboxCleanupScheduler"
```

---

## Task 17: OutboxConfig + application.yml + application-test.yml

**What:** Create `infrastructure/config/OutboxConfig.java` (same location — not moved to adapter package, matching the reference). Update `application.yml` with new `app.parsing.*`, `app.outbox.cleanup.*`, `app.camel.retry.*`, `app.kafka.consumers.*` sections. Update `application-test.yml` to match the expanded config.

**Critical naming difference from taxinvoice:** In this service, `JpaOutboxEventRepository` is the Spring Data JPA **interface** and `SpringDataOutboxRepository` is the `@Component` concrete adapter. So `OutboxConfig.outboxEventRepository()` must `return springRepository` — NOT `return new JpaOutboxEventRepository(springRepository)`.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/config/OutboxConfig.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/test/resources/application-test.yml`

- [ ] **Step 1: Create OutboxConfig**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/config/OutboxConfig.java
package com.wpanther.debitcreditnote.processing.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence.outbox.SpringDataOutboxRepository;
import com.wpanther.saga.domain.outbox.OutboxEventRepository;
import com.wpanther.saga.infrastructure.outbox.OutboxService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutboxConfig {

    @Bean
    @ConditionalOnMissingBean(OutboxEventRepository.class)
    public OutboxEventRepository outboxEventRepository(SpringDataOutboxRepository springRepository) {
        return springRepository;
    }

    @Bean
    @ConditionalOnMissingBean(OutboxService.class)
    public OutboxService outboxService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        return new OutboxService(repository, objectMapper);
    }
}
```

- [ ] **Step 2: Update application.yml**

Add the following sections after the existing `app:` block (after `saga-reply-debitcreditnote`). Keep all existing content intact.

```yaml
# Replace the existing app: block entirely with:
app:
  parsing:
    timeout-seconds: 10
    max-concurrent: 300
  debitcreditnote:
    default-due-date-days: 30
  outbox:
    cleanup:
      retention-days: 7
      cron: "0 0 2 * * *"
  camel:
    retry:
      max-redeliveries: 3
      redelivery-delay-ms: 1000
      backoff-multiplier: 2.0
      max-redelivery-delay-ms: 10000
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS:localhost:9092}
    consumers:
      max-poll-records: 100
      count: 3
    topics:
      debitcreditnote-processed: debitcreditnote.processed
      dlq: debitcreditnote.processing.dlq
      saga-command-debitcreditnote: saga.command.debitcreditnote
      saga-compensation-debitcreditnote: saga.compensation.debitcreditnote
      saga-reply-debitcreditnote: saga.reply.debitcreditnote
```

Also add `spring.transaction.default-timeout: 30` after the `flyway:` block, matching the reference.

- [ ] **Step 3: Update application-test.yml**

Replace the full content with expanded test configuration:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true

  flyway:
    enabled: false

  transaction:
    default-timeout: 30

camel:
  springboot:
    name: debitcreditnote-processing-camel-test
  dataformat:
    jackson:
      auto-discover-object-mapper: true

app:
  parsing:
    timeout-seconds: 5
    max-concurrent: 10
  debitcreditnote:
    default-due-date-days: 30
  outbox:
    cleanup:
      retention-days: 7
      cron: "0 0 2 * * *"
  camel:
    retry:
      max-redeliveries: 3
      redelivery-delay-ms: 500
      backoff-multiplier: 2.0
      max-redelivery-delay-ms: 5000
  kafka:
    bootstrap-servers: localhost:9093
    consumers:
      max-poll-records: 10
      count: 1
    topics:
      debitcreditnote-processed: debitcreditnote.processed.test
      dlq: debitcreditnote.processing.dlq.test
      saga-command-debitcreditnote: saga.command.debitcreditnote.test
      saga-compensation-debitcreditnote: saga.compensation.debitcreditnote.test
      saga-reply-debitcreditnote: saga.reply.debitcreditnote.test

logging:
  level:
    root: WARN
    com.wpanther.debitcreditnote.processing: INFO
    org.apache.camel: WARN
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/config/OutboxConfig.java \
       src/main/resources/application.yml \
       src/test/resources/application-test.yml
git commit -m "Add OutboxConfig and expand application.yml with parsing, outbox, retry config"
```

---

## Task 18: SagaCommandHandler + SagaRouteConfig (inbound adapters)

**What:** Create the new inbound adapter versions. `SagaCommandHandler` moves from `application/service/` to `infrastructure/adapter/in/messaging/` — becomes a thin delegate with no `@Transactional`, no direct repository or publisher access. `SagaRouteConfig` moves from `infrastructure/config/` to `infrastructure/adapter/in/messaging/` — uses `KafkaTopicsProperties` for topic names, configurable retry params via `@Value`, `kafkaConsumerParams()` helper. Write unit tests for both. Delete old files in the same commit.

**Files:**
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/SagaCommandHandler.java`
- Create: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/SagaRouteConfig.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/SagaCommandHandlerTest.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/SagaRouteConfigTest.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/application/service/SagaCommandHandler.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/config/SagaRouteConfig.java`

- [ ] **Step 1: Create SagaCommandHandler (thin delegate)**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/SagaCommandHandler.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging;

import com.wpanther.debitcreditnote.processing.application.port.in.CompensateDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.application.port.in.ProcessDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.application.port.in.ProcessDebitCreditNoteUseCase.ProcessingException;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto.CompensateDebitCreditNoteCommand;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto.ProcessDebitCreditNoteCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaCommandHandler {

    private final ProcessDebitCreditNoteUseCase processUseCase;
    private final CompensateDebitCreditNoteUseCase compensateUseCase;

    public void handleProcessCommand(ProcessDebitCreditNoteCommand command) {
        log.info("Handling ProcessDebitCreditNoteCommand for saga {} document {}",
            command.getSagaId(), command.getDocumentId());

        try {
            processUseCase.process(
                command.getDocumentId(),
                command.getXmlContent(),
                command.getSagaId(),
                command.getSagaStep(),
                command.getCorrelationId()
            );
        } catch (ProcessingException e) {
            log.error("ProcessingException for saga {} document {}: {}",
                command.getSagaId(), command.getDocumentId(), e.getMessage(), e);
        }
    }

    public void handleCompensation(CompensateDebitCreditNoteCommand command) {
        log.info("Handling compensation for saga {} document {}",
            command.getSagaId(), command.getDocumentId());

        compensateUseCase.compensate(
            command.getDocumentId(),
            command.getSagaId(),
            command.getSagaStep(),
            command.getCorrelationId()
        );
    }
}
```

- [ ] **Step 2: Create SagaRouteConfig (with KafkaTopicsProperties)**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/SagaRouteConfig.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging;

import com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto.CompensateDebitCreditNoteCommand;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto.ProcessDebitCreditNoteCommand;
import com.wpanther.debitcreditnote.processing.infrastructure.config.KafkaTopicsProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SagaRouteConfig extends RouteBuilder {

    private static final String GROUP_ID = "debitcreditnote-processing-service";

    private final SagaCommandHandler sagaCommandHandler;
    private final KafkaTopicsProperties topics;

    @Value("${app.kafka.bootstrap-servers}")
    private String kafkaBrokers;

    @Value("${app.camel.retry.max-redeliveries:3}")
    private int maxRedeliveries;

    @Value("${app.camel.retry.redelivery-delay-ms:1000}")
    private long redeliveryDelayMs;

    @Value("${app.camel.retry.backoff-multiplier:2.0}")
    private double backoffMultiplier;

    @Value("${app.camel.retry.max-redelivery-delay-ms:10000}")
    private long maxRedeliveryDelayMs;

    @Value("${app.kafka.consumers.max-poll-records:100}")
    private int maxPollRecords;

    @Value("${app.kafka.consumers.count:3}")
    private int consumersCount;

    public SagaRouteConfig(SagaCommandHandler sagaCommandHandler, KafkaTopicsProperties topics) {
        this.sagaCommandHandler = sagaCommandHandler;
        this.topics = topics;
    }

    private String kafkaConsumerParams() {
        return "?brokers=RAW(" + kafkaBrokers + ")"
            + "&groupId=" + GROUP_ID
            + "&autoOffsetReset=earliest"
            + "&autoCommitEnable=false"
            + "&breakOnFirstError=true"
            + "&maxPollRecords=" + maxPollRecords
            + "&consumersCount=" + consumersCount;
    }

    @Override
    public void configure() throws Exception {

        errorHandler(deadLetterChannel("kafka:" + topics.dlq() + "?brokers=RAW(" + kafkaBrokers + ")")
            .maximumRedeliveries(maxRedeliveries)
            .redeliveryDelay(redeliveryDelayMs)
            .useExponentialBackOff()
            .backOffMultiplier(backoffMultiplier)
            .maximumRedeliveryDelay(maxRedeliveryDelayMs)
            .logExhausted(true)
            .logStackTrace(true));

        from("kafka:" + topics.sagaCommandDebitcreditnote() + kafkaConsumerParams())
            .routeId("saga-command-consumer")
            .log("Received saga command from Kafka: partition=${header[kafka.PARTITION]}, offset=${header[kafka.OFFSET]}")
            .unmarshal().json(JsonLibrary.Jackson, ProcessDebitCreditNoteCommand.class)
            .process(exchange -> {
                ProcessDebitCreditNoteCommand cmd = exchange.getIn().getBody(ProcessDebitCreditNoteCommand.class);
                log.info("Processing saga command for saga: {}, note: {}",
                    cmd.getSagaId(), cmd.getNoteNumber());
                sagaCommandHandler.handleProcessCommand(cmd);
            })
            .log("Successfully processed saga command");

        from("kafka:" + topics.sagaCompensationDebitcreditnote() + kafkaConsumerParams())
            .routeId("saga-compensation-consumer")
            .log("Received compensation command from Kafka: partition=${header[kafka.PARTITION]}, offset=${header[kafka.OFFSET]}")
            .unmarshal().json(JsonLibrary.Jackson, CompensateDebitCreditNoteCommand.class)
            .process(exchange -> {
                CompensateDebitCreditNoteCommand cmd = exchange.getIn().getBody(CompensateDebitCreditNoteCommand.class);
                log.info("Processing compensation for saga: {}, document: {}",
                    cmd.getSagaId(), cmd.getDocumentId());
                sagaCommandHandler.handleCompensation(cmd);
            })
            .log("Successfully processed compensation command");
    }
}
```

- [ ] **Step 3: Write SagaCommandHandlerTest**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/SagaCommandHandlerTest.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging;

import com.wpanther.debitcreditnote.processing.application.port.in.CompensateDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.application.port.in.ProcessDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.application.port.in.ProcessDebitCreditNoteUseCase.ProcessingException;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto.CompensateDebitCreditNoteCommand;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto.ProcessDebitCreditNoteCommand;
import com.wpanther.saga.domain.enums.SagaStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaCommandHandlerTest {

    @Mock
    private ProcessDebitCreditNoteUseCase processUseCase;

    @Mock
    private CompensateDebitCreditNoteUseCase compensateUseCase;

    @InjectMocks
    private SagaCommandHandler sagaCommandHandler;

    private ProcessDebitCreditNoteCommand validCommand;
    private CompensateDebitCreditNoteCommand compensateCommand;

    @BeforeEach
    void setUp() {
        validCommand = new ProcessDebitCreditNoteCommand(
            "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1", "doc-001", "<xml/>", "CN-001"
        );

        compensateCommand = new CompensateDebitCreditNoteCommand(
            "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1",
            "process-debit-credit-note", "doc-001", "debitcreditnote"
        );
    }

    @Test
    void shouldDelegateToProcessUseCase() throws Exception {
        doNothing().when(processUseCase).process(any(), any(), any(), any(), any());

        sagaCommandHandler.handleProcessCommand(validCommand);

        verify(processUseCase).process(
            eq("doc-001"),
            eq("<xml/>"),
            eq("saga-1"),
            eq(SagaStep.PROCESS_DEBIT_CREDIT_NOTE),
            eq("corr-1")
        );
        verify(compensateUseCase, never()).compensate(any(), any(), any(), any());
    }

    @Test
    void shouldDelegateToCompensateUseCase() {
        doNothing().when(compensateUseCase).compensate(any(), any(), any(), any());

        sagaCommandHandler.handleCompensation(compensateCommand);

        verify(compensateUseCase).compensate(
            eq("doc-001"),
            eq("saga-1"),
            eq(SagaStep.PROCESS_DEBIT_CREDIT_NOTE),
            eq("corr-1")
        );
        verify(processUseCase, never()).process(any(), any(), any(), any(), any());
    }

    @Test
    void shouldCatchProcessingException() throws Exception {
        doThrow(new ProcessingException("Processing error"))
            .when(processUseCase).process(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> sagaCommandHandler.handleProcessCommand(validCommand));

        verify(processUseCase).process(any(), any(), any(), any(), any());
    }

    @Test
    void shouldPropagateCompensationException() {
        doThrow(new CompensateDebitCreditNoteUseCase.CompensationException(
                "Compensation failed", new RuntimeException("DB error")))
            .when(compensateUseCase).compensate(any(), any(), any(), any());

        assertThrows(CompensateDebitCreditNoteUseCase.CompensationException.class,
            () -> sagaCommandHandler.handleCompensation(compensateCommand));

        verify(compensateUseCase).compensate(any(), any(), any(), any());
    }
}
```

- [ ] **Step 4: Write SagaRouteConfigTest**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/SagaRouteConfigTest.java
package com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto.CompensateDebitCreditNoteCommand;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto.ProcessDebitCreditNoteCommand;
import com.wpanther.saga.domain.enums.SagaStep;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@CamelSpringBootTest
@SpringBootTest
@ActiveProfiles("test")
@UseAdviceWith
class SagaRouteConfigTest {

    @Autowired
    private CamelContext camelContext;

    @MockBean
    private SagaCommandHandler sagaCommandHandler;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        AdviceWith.adviceWith(camelContext, "saga-command-consumer", a -> {
            a.replaceFromWith("direct:saga-command");
        });
        AdviceWith.adviceWith(camelContext, "saga-compensation-consumer", a -> {
            a.replaceFromWith("direct:saga-compensation");
        });

        camelContext.start();
    }

    @Test
    void shouldHaveAllRoutesConfigured() {
        List<Route> routes = camelContext.getRoutes();
        assertFalse(routes.isEmpty(), "No Camel routes found");

        List<String> routeIds = routes.stream()
            .map(Route::getRouteId)
            .toList();

        assertTrue(routeIds.contains("saga-command-consumer"),
            "Missing saga-command-consumer route. Found: " + routeIds);
        assertTrue(routeIds.contains("saga-compensation-consumer"),
            "Missing saga-compensation-consumer route. Found: " + routeIds);
    }

    @Test
    void shouldHaveExactlyTwoRoutes() {
        List<Route> routes = camelContext.getRoutes();
        assertEquals(2, routes.size(),
            "Expected exactly 2 routes but found: " + routes.stream()
                .map(Route::getRouteId).toList());
    }

    @Test
    void shouldProcessSagaCommand() throws Exception {
        ProcessDebitCreditNoteCommand command = new ProcessDebitCreditNoteCommand(
            "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1",
            "doc-1", "<xml>test</xml>", "CN-001"
        );
        String json = objectMapper.writeValueAsString(command);

        try (ProducerTemplate producer = camelContext.createProducerTemplate()) {
            producer.sendBody("direct:saga-command", json);
        }

        verify(sagaCommandHandler).handleProcessCommand(any(ProcessDebitCreditNoteCommand.class));
    }

    @Test
    void shouldProcessCompensationCommand() throws Exception {
        CompensateDebitCreditNoteCommand command = new CompensateDebitCreditNoteCommand(
            "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1",
            "process-debit-credit-note", "doc-1", "debitcreditnote"
        );
        String json = objectMapper.writeValueAsString(command);

        try (ProducerTemplate producer = camelContext.createProducerTemplate()) {
            producer.sendBody("direct:saga-compensation", json);
        }

        verify(sagaCommandHandler).handleCompensation(any(CompensateDebitCreditNoteCommand.class));
    }
}
```

- [ ] **Step 5: Delete old files and commit (atomic)**

Delete the old `SagaCommandHandler` from `application/service/` and old `SagaRouteConfig` from `infrastructure/config/`. Both must be deleted in the same commit as the new adapter files to avoid bean registration conflicts.

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/SagaCommandHandler.java \
       src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/SagaRouteConfig.java \
       src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/SagaCommandHandlerTest.java \
       src/test/java/com/wpanther/debitcreditnote/processing/infrastructure/adapter/in/messaging/SagaRouteConfigTest.java
git rm src/main/java/com/wpanther/debitcreditnote/processing/application/service/SagaCommandHandler.java
git rm src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/config/SagaRouteConfig.java
git commit -m "Move SagaCommandHandler and SagaRouteConfig to adapter/in/messaging with unit tests"
```

---

## Task 19: DebitCreditNoteProcessingService (full rewrite) + test

**What:** Rewrite `application/service/DebitCreditNoteProcessingService.java` to implement both `ProcessDebitCreditNoteUseCase` and `CompensateDebitCreditNoteUseCase`. Add Micrometer metrics (7 counters + 1 timer), `REQUIRES_NEW` template, all idempotency paths, race-condition handling, and full compensation support. Write comprehensive unit test. Delete old `domain/service/DebitCreditNoteParserService.java` and `domain/repository/ProcessedDebitCreditNoteRepository.java` after confirming all references are updated.

**Files:**
- Rewrite: `src/main/java/com/wpanther/debitcreditnote/processing/application/service/DebitCreditNoteProcessingService.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/application/service/DebitCreditNoteProcessingServiceTest.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/domain/service/DebitCreditNoteParserService.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/domain/repository/ProcessedDebitCreditNoteRepository.java`

- [ ] **Step 1: Rewrite DebitCreditNoteProcessingService**

```java
// src/main/java/com/wpanther/debitcreditnote/processing/application/service/DebitCreditNoteProcessingService.java
package com.wpanther.debitcreditnote.processing.application.service;

import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.debitcreditnote.processing.application.port.in.CompensateDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.application.port.in.ProcessDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.application.port.out.SagaReplyPort;
import com.wpanther.debitcreditnote.processing.application.port.out.DebitCreditNoteEventPublishingPort;
import com.wpanther.debitcreditnote.processing.domain.event.DebitCreditNoteProcessedDomainEvent;
import com.wpanther.debitcreditnote.processing.domain.model.DebitCreditNoteId;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessedDebitCreditNote;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessingStatus;
import com.wpanther.debitcreditnote.processing.domain.port.out.ProcessedDebitCreditNoteRepository;
import com.wpanther.debitcreditnote.processing.domain.port.out.DebitCreditNoteParserPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class DebitCreditNoteProcessingService implements ProcessDebitCreditNoteUseCase, CompensateDebitCreditNoteUseCase {

    private final ProcessedDebitCreditNoteRepository noteRepository;
    private final DebitCreditNoteParserPort parserService;
    private final DebitCreditNoteEventPublishingPort eventPublisher;
    private final SagaReplyPort sagaReplyPort;
    private final MeterRegistry meterRegistry;

    private final TransactionTemplate requiresNewTemplate;

    private final Counter processSuccessCounter;
    private final Counter processFailureCounter;
    private final Counter processIdempotentCounter;
    private final Counter processRaceConditionResolvedCounter;
    private final Counter compensateSuccessCounter;
    private final Counter compensateIdempotentCounter;
    private final Counter compensateFailureCounter;
    private final Timer processingTimer;

    public DebitCreditNoteProcessingService(
            ProcessedDebitCreditNoteRepository noteRepository,
            DebitCreditNoteParserPort parserService,
            DebitCreditNoteEventPublishingPort eventPublisher,
            SagaReplyPort sagaReplyPort,
            MeterRegistry meterRegistry,
            PlatformTransactionManager transactionManager) {
        this.noteRepository = noteRepository;
        this.parserService = parserService;
        this.eventPublisher = eventPublisher;
        this.sagaReplyPort = sagaReplyPort;
        this.meterRegistry = meterRegistry;

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.requiresNewTemplate = template;

        this.processSuccessCounter = Counter.builder("debitcreditnote.processing.success")
            .description("Number of successfully processed debit/credit notes")
            .register(meterRegistry);
        this.processFailureCounter = Counter.builder("debitcreditnote.processing.failure")
            .description("Number of failed debit/credit note processing attempts")
            .register(meterRegistry);
        this.processIdempotentCounter = Counter.builder("debitcreditnote.processing.idempotent")
            .description("Number of duplicate processing requests handled idempotently")
            .register(meterRegistry);
        this.processRaceConditionResolvedCounter = Counter.builder("debitcreditnote.processing.race_condition_resolved")
            .description("Number of DuplicateKeyExceptions on source_note_id resolved as concurrent inserts")
            .register(meterRegistry);
        this.compensateSuccessCounter = Counter.builder("debitcreditnote.compensation.success")
            .description("Number of successful compensations")
            .register(meterRegistry);
        this.compensateIdempotentCounter = Counter.builder("debitcreditnote.compensation.idempotent")
            .description("Number of duplicate compensation commands for already-deleted notes")
            .register(meterRegistry);
        this.compensateFailureCounter = Counter.builder("debitcreditnote.compensation.failure")
            .description("Number of failed compensation attempts")
            .register(meterRegistry);
        this.processingTimer = Timer.builder("debitcreditnote.processing.duration")
            .description("Time taken to process debit/credit notes")
            .register(meterRegistry);
    }

    @Override
    @Transactional
    public void process(String documentId, String xmlContent,
                         String sagaId, SagaStep sagaStep, String correlationId) throws ProcessingException {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            processNoteInternal(documentId, xmlContent, sagaId, sagaStep, correlationId);
        } catch (DebitCreditNoteParserPort.ParsingException e) {
            processFailureCounter.increment();
            sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId, "Parse error: " + e.toString());
            throw new ProcessingException("Failed to parse debit/credit note: " + e.toString(), e);
        } catch (DuplicateKeyException e) {
            if (!isSourceNoteIdViolation(e)) {
                processFailureCounter.increment();
                log.error("Duplicate key violation on non-idempotent constraint for document {}, saga {}: {}",
                        documentId, sagaId, e.toString());
                sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId,
                        "Constraint violation for document " + documentId + ": " + e.toString());
                throw new ProcessingException(
                        "Constraint violation for document " + documentId, e);
            }

            log.warn("DuplicateKeyException on source_note_id for document {}, saga {} — re-checking for concurrent insert",
                    documentId, sagaId);
            requiresNewTemplate.execute(txStatus -> {
                Optional<ProcessedDebitCreditNote> existing = noteRepository.findBySourceNoteId(documentId);
                if (existing.isPresent()) {
                    log.warn("Race condition resolved: document {} already committed by concurrent thread — replying SUCCESS",
                            documentId);
                    processRaceConditionResolvedCounter.increment();
                    sagaReplyPort.publishSuccess(sagaId, sagaStep, correlationId);
                } else {
                    log.error("DuplicateKeyException on source_note_id for document {} but no record found — replying FAILURE",
                            documentId);
                    processFailureCounter.increment();
                    sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId,
                            "Duplicate key violation for document " + documentId + ": " + e.toString());
                }
                return null;
            });
            throw new ProcessingException("Concurrent insert for document: " + documentId, e);
        } catch (DataIntegrityViolationException e) {
            processFailureCounter.increment();
            log.error("Constraint violation (non-duplicate-key) for document {}, saga {}: {}",
                    documentId, sagaId, e.toString());
            sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId,
                    "Constraint violation for document " + documentId + ": " + e.toString());
            throw new ProcessingException(
                    "Constraint violation for document " + documentId, e);
        } catch (Exception e) {
            processFailureCounter.increment();
            sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId,
                    "Processing error for document " + documentId + ": " + e.toString());
            throw new ProcessingException(
                    "Failed to process debit/credit note " + documentId + ": " + e.toString(), e);
        } finally {
            sample.stop(processingTimer);
        }
    }

    private ProcessedDebitCreditNote processNoteInternal(String documentId, String xmlContent,
                                                          String sagaId, SagaStep sagaStep, String correlationId)
            throws DebitCreditNoteParserPort.ParsingException {
        log.info("Processing debit/credit note for saga, document: {}", documentId);

        Optional<ProcessedDebitCreditNote> existing = noteRepository.findBySourceNoteId(documentId);
        if (existing.isPresent()) {
            ProcessedDebitCreditNote existingNote = existing.get();

            if (existingNote.getStatus() == ProcessingStatus.COMPLETED) {
                log.warn("Debit/credit note already completed for document {}, returning idempotent success", documentId);
                processIdempotentCounter.increment();
                sagaReplyPort.publishSuccess(sagaId, sagaStep, correlationId);
                return existingNote;
            }

            if (existingNote.getStatus() == ProcessingStatus.PROCESSING) {
                log.warn("Debit/credit note for document {} found in PROCESSING state — previous attempt "
                        + "failed mid-flight; resuming completion", documentId);
                existingNote.markCompleted();
                noteRepository.save(existingNote);
                DebitCreditNoteProcessedDomainEvent domainEvent = DebitCreditNoteProcessedDomainEvent.of(
                    existingNote.getSourceNoteId(),
                    existingNote.getNoteNumber(),
                    existingNote.getTotal(),
                    sagaId,
                    correlationId
                );
                eventPublisher.publish(domainEvent);
                sagaReplyPort.publishSuccess(sagaId, sagaStep, correlationId);
                processSuccessCounter.increment();
                log.info("Resumed and completed debit/credit note: {}", existingNote.getNoteNumber());
                return existingNote;
            }

            throw new IllegalStateException(
                "Debit/credit note for document " + documentId + " has unexpected persisted status: "
                    + existingNote.getStatus());
        }

        ProcessedDebitCreditNote note = parserService.parse(xmlContent, documentId);

        note.startProcessing();
        ProcessedDebitCreditNote saved = noteRepository.save(note);

        log.info("Saved processed debit/credit note: {}", saved.getNoteNumber());

        saved.markCompleted();
        noteRepository.save(saved);

        DebitCreditNoteProcessedDomainEvent domainEvent = DebitCreditNoteProcessedDomainEvent.of(
            saved.getSourceNoteId(),
            saved.getNoteNumber(),
            saved.getTotal(),
            sagaId,
            correlationId
        );
        eventPublisher.publish(domainEvent);

        sagaReplyPort.publishSuccess(sagaId, sagaStep, correlationId);

        processSuccessCounter.increment();
        log.info("Successfully processed debit/credit note: {}", saved.getNoteNumber());
        return saved;
    }

    @Override
    @Transactional
    public void compensate(String documentId, String sagaId, SagaStep sagaStep, String correlationId) {
        log.info("Compensating debit/credit note for document: {}", documentId);

        try {
            Optional<ProcessedDebitCreditNote> existing = noteRepository.findBySourceNoteId(documentId);
            if (existing.isPresent()) {
                noteRepository.deleteById(existing.get().getId());
                log.info("Deleted debit/credit note for document: {}", documentId);
            } else {
                compensateIdempotentCounter.increment();
                log.warn("Debit/credit note not found for compensation of document {} saga {} — "
                    + "treating as idempotent duplicate (already compensated or never processed)",
                    documentId, sagaId);
            }

            sagaReplyPort.publishCompensated(sagaId, sagaStep, correlationId);
            compensateSuccessCounter.increment();
        } catch (Exception e) {
            compensateFailureCounter.increment();
            log.error("Failed to compensate debit/credit note for saga {}: {}", sagaId, e.toString(), e);
            sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId, "Compensation failed: " + e.toString());
            throw new CompensationException(
                    "Compensation failed for document " + documentId, e);
        }
    }

    private static boolean isSourceNoteIdViolation(DuplicateKeyException e) {
        Throwable cause = e.getMostSpecificCause();
        String msg = cause.getMessage();
        if (msg == null || !msg.contains("uq_processed_debit_credit_notes_source_note_id")) {
            return false;
        }
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sqlEx && "23505".equals(sqlEx.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
```

- [ ] **Step 2: Write DebitCreditNoteProcessingServiceTest**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/application/service/DebitCreditNoteProcessingServiceTest.java
package com.wpanther.debitcreditnote.processing.application.service;

import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.debitcreditnote.processing.application.port.in.CompensateDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.application.port.in.ProcessDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.application.port.out.SagaReplyPort;
import com.wpanther.debitcreditnote.processing.application.port.out.DebitCreditNoteEventPublishingPort;
import com.wpanther.debitcreditnote.processing.domain.event.DebitCreditNoteProcessedDomainEvent;
import com.wpanther.debitcreditnote.processing.domain.model.*;
import com.wpanther.debitcreditnote.processing.domain.port.out.ProcessedDebitCreditNoteRepository;
import com.wpanther.debitcreditnote.processing.domain.port.out.DebitCreditNoteParserPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebitCreditNoteProcessingServiceTest {

    @Mock
    private ProcessedDebitCreditNoteRepository noteRepository;

    @Mock
    private DebitCreditNoteParserPort parserService;

    @Mock
    private DebitCreditNoteEventPublishingPort eventPublisher;

    @Mock
    private SagaReplyPort sagaReplyPort;

    @Mock
    private PlatformTransactionManager transactionManager;

    private DebitCreditNoteProcessingService service;

    private ProcessedDebitCreditNote validNote;

    @BeforeEach
    void setUp() {
        service = new DebitCreditNoteProcessingService(
            noteRepository,
            parserService,
            eventPublisher,
            sagaReplyPort,
            new SimpleMeterRegistry(),
            transactionManager
        );

        Party seller = Party.of(
            "Seller Company",
            TaxIdentifier.of("1234567890", "VAT"),
            new Address("123 Street", "Bangkok", "10110", "TH"),
            null
        );

        Party buyer = Party.of(
            "Buyer Company",
            TaxIdentifier.of("9876543210", "VAT"),
            new Address("456 Road", "Chiang Mai", "50000", "TH"),
            null
        );

        LineItem item = new LineItem(
            "Service 1",
            10,
            Money.of(new BigDecimal("1000.00"), "THB"),
            new BigDecimal("7.00")
        );

        validNote = ProcessedDebitCreditNote.builder()
            .id(DebitCreditNoteId.generate())
            .sourceNoteId("intake-123")
            .noteNumber("CN-001")
            .noteType("DEBIT")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .seller(seller)
            .buyer(buyer)
            .addItem(item)
            .currency("THB")
            .originalXml("<xml>test</xml>")
            .build();
    }

    @Test
    void testProcessNoteSuccess() throws Exception {
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class))).thenReturn(validNote);

        service.process("intake-123", "<xml>test</xml>", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");

        verify(noteRepository).findBySourceNoteId("intake-123");
        verify(parserService).parse("<xml>test</xml>", "intake-123");
        verify(noteRepository, times(2)).save(any(ProcessedDebitCreditNote.class));
        verify(eventPublisher).publish(any(DebitCreditNoteProcessedDomainEvent.class));
        verify(sagaReplyPort).publishSuccess("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");
    }

    @Test
    void testProcessNoteAlreadyCompleted() throws Exception {
        ProcessedDebitCreditNote completedNote = ProcessedDebitCreditNote.builder()
            .id(DebitCreditNoteId.generate())
            .sourceNoteId("intake-123")
            .noteNumber("CN-001")
            .noteType("DEBIT")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .seller(Party.of("Seller", TaxIdentifier.of("1234567890", "VAT"),
                new Address("123 St", "Bangkok", "10110", "TH"), null))
            .buyer(Party.of("Buyer", TaxIdentifier.of("9876543210", "VAT"),
                new Address("456 Rd", "Chiang Mai", "50000", "TH"), null))
            .addItem(new LineItem("S1", 10, Money.of(new BigDecimal("1000.00"), "THB"), new BigDecimal("7.00")))
            .currency("THB").originalXml("<xml/>")
            .status(ProcessingStatus.COMPLETED)
            .build();
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.of(completedNote));

        service.process("intake-123", "<xml>test</xml>", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");

        verify(parserService, never()).parse(anyString(), anyString());
        verify(noteRepository, never()).save(any(ProcessedDebitCreditNote.class));
        verify(eventPublisher, never()).publish(any());
        verify(sagaReplyPort).publishSuccess("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");
    }

    @Test
    void testProcessNoteResumesFromProcessingState() throws Exception {
        ProcessedDebitCreditNote processingNote = ProcessedDebitCreditNote.builder()
            .id(DebitCreditNoteId.generate())
            .sourceNoteId("intake-123")
            .noteNumber("CN-001")
            .noteType("DEBIT")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .seller(Party.of("Seller", TaxIdentifier.of("1234567890", "VAT"),
                new Address("123 St", "Bangkok", "10110", "TH"), null))
            .buyer(Party.of("Buyer", TaxIdentifier.of("9876543210", "VAT"),
                new Address("456 Rd", "Chiang Mai", "50000", "TH"), null))
            .addItem(new LineItem("S1", 10, Money.of(new BigDecimal("1000.00"), "THB"), new BigDecimal("7.00")))
            .currency("THB").originalXml("<xml/>")
            .status(ProcessingStatus.PROCESSING)
            .build();
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.of(processingNote));

        service.process("intake-123", "<xml>test</xml>", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");

        verify(parserService, never()).parse(anyString(), anyString());
        verify(noteRepository, times(1)).save(any(ProcessedDebitCreditNote.class));
        verify(eventPublisher).publish(any(DebitCreditNoteProcessedDomainEvent.class));
        verify(sagaReplyPort).publishSuccess("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");
        assertEquals(ProcessingStatus.COMPLETED, processingNote.getStatus());
    }

    @Test
    void testProcessNoteParsingError() throws Exception {
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString()))
            .thenThrow(new DebitCreditNoteParserPort.ParsingException("Parse error"));

        assertThrows(ProcessDebitCreditNoteUseCase.ProcessingException.class,
            () -> service.process("intake-123", "<xml>test</xml>", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123"));

        verify(noteRepository, never()).save(any(ProcessedDebitCreditNote.class));
        verify(eventPublisher, never()).publish(any());
        verify(sagaReplyPort).publishFailure(eq("saga-1"), eq(SagaStep.PROCESS_DEBIT_CREDIT_NOTE), eq("correlation-123"), contains("Parse error"));
    }

    @Test
    void testProcessNotePublishesCorrectEvent() throws Exception {
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class))).thenReturn(validNote);

        service.process("intake-123", "<xml>test</xml>", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");

        ArgumentCaptor<DebitCreditNoteProcessedDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(DebitCreditNoteProcessedDomainEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        DebitCreditNoteProcessedDomainEvent event = eventCaptor.getValue();
        assertEquals("CN-001", event.documentNumber());
        assertEquals("THB", event.total().currency());
        assertEquals("correlation-123", event.correlationId());
    }

    @Test
    void testProcessNoteSavesTwice() throws Exception {
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class))).thenReturn(validNote);

        service.process("intake-123", "<xml>test</xml>", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");

        verify(noteRepository, times(2)).save(any(ProcessedDebitCreditNote.class));
    }

    @Test
    void testProcessNoteDatabaseError() throws Exception {
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class)))
            .thenThrow(new RuntimeException("Database error"));

        assertThrows(ProcessDebitCreditNoteUseCase.ProcessingException.class,
            () -> service.process("intake-123", "<xml>test</xml>", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123"));

        verify(sagaReplyPort).publishFailure(eq("saga-1"), eq(SagaStep.PROCESS_DEBIT_CREDIT_NOTE), eq("correlation-123"), contains("Processing error"));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void testCompensateDeletesExistingNote() {
        when(noteRepository.findBySourceNoteId("intake-123")).thenReturn(Optional.of(validNote));

        service.compensate("intake-123", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");

        verify(noteRepository).findBySourceNoteId("intake-123");
        verify(noteRepository).deleteById(validNote.getId());
        verify(sagaReplyPort).publishCompensated("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");
    }

    @Test
    void testCompensateNotFound() {
        when(noteRepository.findBySourceNoteId("intake-notfound")).thenReturn(Optional.empty());

        service.compensate("intake-notfound", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");

        verify(noteRepository).findBySourceNoteId("intake-notfound");
        verify(noteRepository, never()).deleteById(any());
        verify(sagaReplyPort).publishCompensated("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");
    }

    @Test
    void testCompensateHandlesException() {
        when(noteRepository.findBySourceNoteId("intake-123")).thenReturn(Optional.of(validNote));
        doThrow(new RuntimeException("DB error")).when(noteRepository).deleteById(any());

        assertThrows(CompensateDebitCreditNoteUseCase.CompensationException.class,
            () -> service.compensate("intake-123", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1"));

        verify(sagaReplyPort).publishFailure(eq("saga-1"), eq(SagaStep.PROCESS_DEBIT_CREDIT_NOTE), eq("corr-1"), contains("Compensation failed"));
    }

    @Test
    void testProcessNoteRaceConditionResolvesAsSuccess() throws Exception {
        SQLException sqlCause = new SQLException(
            "ERROR: duplicate key value violates unique constraint" +
            " \"uq_processed_debit_credit_notes_source_note_id\"", "23505");
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(noteRepository.findBySourceNoteId(anyString()))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(validNote));
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class)))
            .thenThrow(new DuplicateKeyException("duplicate key", sqlCause));

        ProcessDebitCreditNoteUseCase.ProcessingException ex =
            assertThrows(ProcessDebitCreditNoteUseCase.ProcessingException.class,
                () -> service.process("intake-123", "<xml>test</xml>",
                                      "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123"));
        assertInstanceOf(DataIntegrityViolationException.class, ex.getCause());

        verify(sagaReplyPort).publishSuccess("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");
        verify(sagaReplyPort, never()).publishFailure(any(), any(), any(), any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void testProcessNoteDataIntegrityViolation() throws Exception {
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class)))
            .thenThrow(new DataIntegrityViolationException(
                "value too long for type character varying(500)"));

        ProcessDebitCreditNoteUseCase.ProcessingException ex =
            assertThrows(ProcessDebitCreditNoteUseCase.ProcessingException.class,
                () -> service.process("intake-123", "<xml>test</xml>",
                                      "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123"));
        assertInstanceOf(DataIntegrityViolationException.class, ex.getCause());
        assertTrue(ex.getMessage().contains("Constraint violation"));

        verify(sagaReplyPort).publishFailure(
            eq("saga-1"), eq(SagaStep.PROCESS_DEBIT_CREDIT_NOTE), eq("correlation-123"),
            contains("Constraint violation"));
        verify(transactionManager, never()).getTransaction(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void testProcessNoteDuplicateKeyOnNonIdempotentConstraint() throws Exception {
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class)))
            .thenThrow(new DuplicateKeyException(
                "duplicate key value violates unique constraint \"idx_note_number_unique\""));

        ProcessDebitCreditNoteUseCase.ProcessingException ex =
            assertThrows(ProcessDebitCreditNoteUseCase.ProcessingException.class,
                () -> service.process("intake-123", "<xml>test</xml>",
                                      "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123"));
        assertInstanceOf(DuplicateKeyException.class, ex.getCause());
        assertTrue(ex.getMessage().contains("Constraint violation"));

        verify(sagaReplyPort).publishFailure(
            eq("saga-1"), eq(SagaStep.PROCESS_DEBIT_CREDIT_NOTE), eq("correlation-123"),
            contains("Constraint violation"));
        verify(sagaReplyPort, never()).publishSuccess(any(), any(), any());
        verify(noteRepository, times(1)).findBySourceNoteId(anyString());
        verify(eventPublisher, never()).publish(any());
    }
}
```

- [ ] **Step 3: Delete old domain service and repository, then commit**

```bash
git add src/main/java/com/wpanther/debitcreditnote/processing/application/service/DebitCreditNoteProcessingService.java \
       src/test/java/com/wpanther/debitcreditnote/processing/application/service/DebitCreditNoteProcessingServiceTest.java
git rm src/main/java/com/wpanther/debitcreditnote/processing/domain/service/DebitCreditNoteParserService.java
git rm src/main/java/com/wpanther/debitcreditnote/processing/domain/repository/ProcessedDebitCreditNoteRepository.java
git commit -m "Rewrite DebitCreditNoteProcessingService with metrics, REQUIRES_NEW, and full test suite"
```

---

## Task 20: Domain model unit tests

**What:** Write unit tests for all 9 domain model classes: `DebitCreditNoteId`, `Money`, `Address`, `TaxIdentifier`, `Party`, `LineItem`, `ProcessedDebitCreditNote`, `ProcessingStatus`. These tests cover construction, validation, state transitions, equality, and edge cases.

**Files:**
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/domain/model/DebitCreditNoteIdTest.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/domain/model/MoneyTest.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/domain/model/AddressTest.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/domain/model/TaxIdentifierTest.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/domain/model/PartyTest.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/domain/model/LineItemTest.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/domain/model/ProcessedDebitCreditNoteTest.java`
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/domain/model/ProcessingStatusTest.java`

- [ ] **Step 1: Write DebitCreditNoteIdTest**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/domain/model/DebitCreditNoteIdTest.java
package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DebitCreditNoteIdTest {

    @Test
    void generate_createsNonNullId() {
        DebitCreditNoteId id = DebitCreditNoteId.generate();
        assertNotNull(id);
        assertNotNull(id.getValue());
    }

    @Test
    void generate_createsUniqueIds() {
        DebitCreditNoteId id1 = DebitCreditNoteId.generate();
        DebitCreditNoteId id2 = DebitCreditNoteId.generate();
        assertNotEquals(id1, id2);
    }

    @Test
    void ofUuid_createsIdWithValue() {
        UUID uuid = UUID.randomUUID();
        DebitCreditNoteId id = DebitCreditNoteId.of(uuid);
        assertEquals(uuid, id.getValue());
    }

    @Test
    void ofString_createsIdFromUuidString() {
        UUID uuid = UUID.randomUUID();
        DebitCreditNoteId id = DebitCreditNoteId.of(uuid.toString());
        assertEquals(uuid, id.getValue());
    }

    @Test
    void ofNull_throwsNPE() {
        assertThrows(NullPointerException.class, () -> DebitCreditNoteId.of((UUID) null));
    }

    @Test
    void equals_sameValue() {
        UUID uuid = UUID.randomUUID();
        DebitCreditNoteId id1 = DebitCreditNoteId.of(uuid);
        DebitCreditNoteId id2 = DebitCreditNoteId.of(uuid);
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void toString_returnsUuidString() {
        UUID uuid = UUID.randomUUID();
        DebitCreditNoteId id = DebitCreditNoteId.of(uuid);
        assertEquals(uuid.toString(), id.toString());
    }
}
```

- [ ] **Step 2: Write MoneyTest**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/domain/model/MoneyTest.java
package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void of_createsMoney() {
        Money money = Money.of(new BigDecimal("100.00"), "THB");
        assertEquals(new BigDecimal("100.00"), money.amount());
        assertEquals("THB", money.currency());
    }

    @Test
    void of_invalidCurrency_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> Money.of(BigDecimal.ONE, "INVALID"));
    }

    @Test
    void of_nullAmount_throws() {
        assertThrows(NullPointerException.class,
            () -> Money.of(null, "THB"));
    }

    @Test
    void add_sameCurrency() {
        Money a = Money.of(new BigDecimal("100.00"), "THB");
        Money b = Money.of(new BigDecimal("50.00"), "THB");
        Money result = a.add(b);
        assertEquals(new BigDecimal("150.00"), result.amount());
        assertEquals("THB", result.currency());
    }

    @Test
    void add_differentCurrency_throws() {
        Money thb = Money.of(BigDecimal.ONE, "THB");
        Money usd = Money.of(BigDecimal.ONE, "USD");
        assertThrows(IllegalArgumentException.class, () -> thb.add(usd));
    }

    @Test
    void subtract_sameCurrency() {
        Money a = Money.of(new BigDecimal("100.00"), "THB");
        Money b = Money.of(new BigDecimal("30.00"), "THB");
        Money result = a.subtract(b);
        assertEquals(new BigDecimal("70.00"), result.amount());
    }

    @Test
    void multiply_returnsScaledResult() {
        Money money = Money.of(new BigDecimal("100.00"), "THB");
        Money result = money.multiply(BigDecimal.valueOf(7));
        assertEquals(new BigDecimal("700.00"), result.amount());
    }

    @Test
    void zero_createsZeroAmount() {
        Money zero = Money.zero("THB");
        assertEquals(BigDecimal.ZERO, zero.amount());
        assertEquals("THB", zero.currency());
    }
}
```

- [ ] **Step 3: Write AddressTest**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/domain/model/AddressTest.java
package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddressTest {

    @Test
    void of_createsAddress() {
        Address addr = Address.of("123 Street", "Bangkok", "10110", "TH");
        assertEquals("123 Street", addr.street());
        assertEquals("Bangkok", addr.city());
        assertEquals("10110", addr.postalCode());
        assertEquals("TH", addr.country());
    }

    @Test
    void of_nullCountry_throws() {
        assertThrows(NullPointerException.class,
            () -> Address.of("Street", "City", "12345", null));
    }

    @Test
    void record_equality() {
        Address a1 = Address.of("123 St", "Bangkok", "10110", "TH");
        Address a2 = Address.of("123 St", "Bangkok", "10110", "TH");
        assertEquals(a1, a2);
    }
}
```

- [ ] **Step 4: Write TaxIdentifierTest**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/domain/model/TaxIdentifierTest.java
package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaxIdentifierTest {

    @Test
    void of_createsTaxId() {
        TaxIdentifier taxId = TaxIdentifier.of("1234567890", "VAT");
        assertEquals("1234567890", taxId.taxId());
        assertEquals("VAT", taxId.scheme());
    }

    @Test
    void of_nullTaxId_throws() {
        assertThrows(NullPointerException.class,
            () -> TaxIdentifier.of(null, "VAT"));
    }

    @Test
    void of_nullScheme_throws() {
        assertThrows(NullPointerException.class,
            () -> TaxIdentifier.of("1234567890", null));
    }

    @Test
    void record_equality() {
        TaxIdentifier t1 = TaxIdentifier.of("1234567890", "VAT");
        TaxIdentifier t2 = TaxIdentifier.of("1234567890", "VAT");
        assertEquals(t1, t2);
    }
}
```

- [ ] **Step 5: Write PartyTest**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/domain/model/PartyTest.java
package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PartyTest {

    private final Address address = Address.of("123 St", "Bangkok", "10110", "TH");
    private final TaxIdentifier taxId = TaxIdentifier.of("1234567890", "VAT");

    @Test
    void of_createsParty() {
        Party party = Party.of("Company", taxId, address, "test@example.com");
        assertEquals("Company", party.name());
        assertEquals(taxId, party.taxIdentifier());
        assertEquals(address, party.address());
        assertEquals("test@example.com", party.email());
    }

    @Test
    void of_nullEmail_isAllowed() {
        Party party = Party.of("Company", taxId, address, null);
        assertNull(party.email());
    }

    @Test
    void of_nullName_throws() {
        assertThrows(NullPointerException.class,
            () -> Party.of(null, taxId, address, null));
    }

    @Test
    void of_nullTaxId_throws() {
        assertThrows(NullPointerException.class,
            () -> Party.of("Company", null, address, null));
    }

    @Test
    void of_nullAddress_throws() {
        assertThrows(NullPointerException.class,
            () -> Party.of("Company", taxId, null, null));
    }
}
```

- [ ] **Step 6: Write LineItemTest**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/domain/model/LineItemTest.java
package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LineItemTest {

    private final Money unitPrice = Money.of(new BigDecimal("100.00"), "THB");

    @Test
    void construction_setsFields() {
        LineItem item = new LineItem("Service", 10, unitPrice, new BigDecimal("7.00"));
        assertEquals("Service", item.description());
        assertEquals(10, item.quantity());
        assertEquals(unitPrice, item.unitPrice());
        assertEquals(new BigDecimal("7.00"), item.taxRate());
    }

    @Test
    void construction_zeroQuantity_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> new LineItem("Service", 0, unitPrice, new BigDecimal("7.00")));
    }

    @Test
    void construction_negativeQuantity_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> new LineItem("Service", -1, unitPrice, new BigDecimal("7.00")));
    }

    @Test
    void construction_nullDescription_throws() {
        assertThrows(NullPointerException.class,
            () -> new LineItem(null, 10, unitPrice, new BigDecimal("7.00")));
    }

    @Test
    void getLineTotal_returnsQuantityTimesPrice() {
        LineItem item = new LineItem("Service", 10, unitPrice, new BigDecimal("7.00"));
        Money lineTotal = item.getLineTotal();
        assertEquals(new BigDecimal("1000.00"), lineTotal.amount());
    }

    @Test
    void getTaxAmount_returnsCorrectTax() {
        LineItem item = new LineItem("Service", 10, unitPrice, new BigDecimal("7.00"));
        Money tax = item.getTaxAmount();
        assertEquals(new BigDecimal("70.0000"), tax.amount());
    }

    @Test
    void getTotalWithTax_returnsLineTotalPlusTax() {
        LineItem item = new LineItem("Service", 10, unitPrice, new BigDecimal("7.00"));
        Money total = item.getTotalWithTax();
        assertEquals(new BigDecimal("1070.0000"), total.amount());
    }
}
```

- [ ] **Step 7: Write ProcessedDebitCreditNoteTest**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/domain/model/ProcessedDebitCreditNoteTest.java
package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ProcessedDebitCreditNoteTest {

    private ProcessedDebitCreditNote.Builder validBuilder() {
        Party seller = Party.of("Seller", TaxIdentifier.of("1234567890", "VAT"),
            new Address("123 St", "Bangkok", "10110", "TH"), null);
        Party buyer = Party.of("Buyer", TaxIdentifier.of("9876543210", "VAT"),
            new Address("456 Rd", "Chiang Mai", "50000", "TH"), null);
        return ProcessedDebitCreditNote.builder()
            .id(DebitCreditNoteId.generate())
            .sourceNoteId("src-1")
            .noteNumber("CN-001")
            .noteType("DEBIT")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .seller(seller)
            .buyer(buyer)
            .addItem(new LineItem("S1", 10, Money.of(new BigDecimal("100.00"), "THB"), new BigDecimal("7.00")))
            .currency("THB")
            .originalXml("<xml/>");
    }

    @Test
    void build_createsNoteInPendingStatus() {
        ProcessedDebitCreditNote note = validBuilder().build();
        assertEquals(ProcessingStatus.PENDING, note.getStatus());
    }

    @Test
    void startProcessing_transitionsToProcessing() {
        ProcessedDebitCreditNote note = validBuilder().build();
        note.startProcessing();
        assertEquals(ProcessingStatus.PROCESSING, note.getStatus());
    }

    @Test
    void startProcessing_fromNonPending_throws() {
        ProcessedDebitCreditNote note = validBuilder().build();
        note.startProcessing();
        assertThrows(IllegalStateException.class, note::startProcessing);
    }

    @Test
    void markCompleted_transitionsToCompleted() {
        ProcessedDebitCreditNote note = validBuilder().build();
        note.startProcessing();
        note.markCompleted();
        assertEquals(ProcessingStatus.COMPLETED, note.getStatus());
        assertNotNull(note.getCompletedAt());
    }

    @Test
    void markCompleted_fromPending_throws() {
        ProcessedDebitCreditNote note = validBuilder().build();
        assertThrows(IllegalStateException.class, note::markCompleted);
    }

    @Test
    void markFailed_setsError() {
        ProcessedDebitCreditNote note = validBuilder().build();
        note.markFailed("some error");
        assertEquals(ProcessingStatus.FAILED, note.getStatus());
        assertEquals("some error", note.getErrorMessage());
    }

    @Test
    void getSubtotal_sumsLineTotals() {
        ProcessedDebitCreditNote note = validBuilder()
            .addItem(new LineItem("S2", 5, Money.of(new BigDecimal("200.00"), "THB"), new BigDecimal("7.00")))
            .build();
        assertEquals(new BigDecimal("2000.00"), note.getSubtotal().amount());
    }

    @Test
    void getTotalTax_sumsTaxAmounts() {
        ProcessedDebitCreditNote note = validBuilder()
            .addItem(new LineItem("S2", 5, Money.of(new BigDecimal("200.00"), "THB"), new BigDecimal("7.00")))
            .build();
        assertEquals(new BigDecimal("140.0000"), note.getTotalTax().amount());
    }

    @Test
    void getTotal_returnsSubtotalPlusTax() {
        ProcessedDebitCreditNote note = validBuilder().build();
        assertEquals(note.getSubtotal().add(note.getTotalTax()), note.getTotal());
    }

    @Test
    void build_emptyItems_throws() {
        assertThrows(IllegalStateException.class, () -> validBuilder().items(java.util.List.of()).build());
    }

    @Test
    void build_dueDateBeforeIssueDate_throws() {
        assertThrows(IllegalStateException.class, () -> validBuilder()
            .issueDate(LocalDate.of(2025, 6, 1))
            .dueDate(LocalDate.of(2025, 5, 1))
            .build());
    }

    @Test
    void build_currencyMismatch_throws() {
        assertThrows(IllegalStateException.class, () -> validBuilder()
            .addItem(new LineItem("S2", 1, Money.of(BigDecimal.ONE, "USD"), BigDecimal.ZERO))
            .build());
    }

    @Test
    void getItems_returnsUnmodifiableList() {
        ProcessedDebitCreditNote note = validBuilder().build();
        assertThrows(UnsupportedOperationException.class,
            () -> note.getItems().add(new LineItem("X", 1, Money.of(BigDecimal.ONE, "THB"), BigDecimal.ZERO)));
    }

    @Test
    void build_statusOverride() {
        ProcessedDebitCreditNote note = validBuilder().status(ProcessingStatus.COMPLETED).build();
        assertEquals(ProcessingStatus.COMPLETED, note.getStatus());
    }
}
```

- [ ] **Step 8: Write ProcessingStatusTest**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/domain/model/ProcessingStatusTest.java
package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessingStatusTest {

    @Test
    void hasAllExpectedValues() {
        ProcessingStatus[] values = ProcessingStatus.values();
        assertEquals(4, values.length);
        assertNotNull(ProcessingStatus.valueOf("PENDING"));
        assertNotNull(ProcessingStatus.valueOf("PROCESSING"));
        assertNotNull(ProcessingStatus.valueOf("COMPLETED"));
        assertNotNull(ProcessingStatus.valueOf("FAILED"));
    }
}
```

- [ ] **Step 9: Commit**

```bash
git add src/test/java/com/wpanther/debitcreditnote/processing/domain/model/
git commit -m "Add unit tests for all domain model classes"
```

---

## Task 21: Full `mvn verify` + smoke test

**What:** Run the full Maven verify to confirm compilation, all tests pass, and JaCoCo coverage thresholds are met. Add the `@SpringBootTest` smoke test if not already present. This task validates that all pieces integrate correctly.

**Files:**
- Create: `src/test/java/com/wpanther/debitcreditnote/processing/DebitCreditNoteProcessingServiceApplicationTest.java`

- [ ] **Step 1: Write smoke test**

```java
// src/test/java/com/wpanther/debitcreditnote/processing/DebitCreditNoteProcessingServiceApplicationTest.java
package com.wpanther.debitcreditnote.processing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DebitCreditNoteProcessingServiceApplicationTest {

    @Test
    void contextLoads() {
        // Smoke test — verifies Spring context starts with H2 + mock Camel
    }
}
```

- [ ] **Step 2: Run full Maven verify**

```bash
cd /home/wpanther/projects/etax/invoice-microservices/services/debitcreditnote-processing-service
mvn clean verify -Dspring.profiles.active=test 2>&1 | tail -40
```

Expected: `BUILD SUCCESS`. All tests pass. JaCoCo coverage meets threshold.

If tests fail, fix the failing test and re-run before committing.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/wpanther/debitcreditnote/processing/DebitCreditNoteProcessingServiceApplicationTest.java
git commit -m "Add SpringBoot smoke test and verify full build"
```

---

## Task 22: Delete old domain event files

**What:** Delete the old `domain/event/` files that have been replaced by new locations. After this task, only `DebitCreditNoteProcessedDomainEvent.java` should remain in `domain/event/`. Also delete `infrastructure/messaging/EventPublisher.java` and `infrastructure/messaging/SagaReplyPublisher.java` if they still exist (they were replaced in Tasks 11-12).

**Files:**
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/domain/event/ProcessDebitCreditNoteCommand.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/domain/event/CompensateDebitCreditNoteCommand.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/domain/event/DebitCreditNoteReplyEvent.java`
- Delete: `src/main/java/com/wpanther/debitcreditnote/processing/domain/event/DebitCreditNoteProcessedEvent.java`
- Delete (if still present): `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/messaging/EventPublisher.java`
- Delete (if still present): `src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/messaging/SagaReplyPublisher.java`

- [ ] **Step 1: Delete old files**

```bash
git rm src/main/java/com/wpanther/debitcreditnote/processing/domain/event/ProcessDebitCreditNoteCommand.java \
       src/main/java/com/wpanther/debitcreditnote/processing/domain/event/CompensateDebitCreditNoteCommand.java \
       src/main/java/com/wpanther/debitcreditnote/processing/domain/event/DebitCreditNoteReplyEvent.java \
       src/main/java/com/wpanther/debitcreditnote/processing/domain/event/DebitCreditNoteProcessedEvent.java

# Delete old infrastructure/messaging files if they still exist
git rm -f src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/messaging/EventPublisher.java 2>/dev/null || true
git rm -f src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/messaging/SagaReplyPublisher.java 2>/dev/null || true
git rm -f src/main/java/com/wpanther/debitcreditnote/processing/infrastructure/service/DebitCreditNoteParserServiceImpl.java 2>/dev/null || true
```

- [ ] **Step 2: Run mvn verify to confirm nothing is broken**

```bash
cd /home/wpanther/projects/etax/invoice-microservices/services/debitcreditnote-processing-service
mvn clean verify -Dspring.profiles.active=test 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git commit -m "Remove old domain/event DTOs and infrastructure files replaced by hexagonal adapters"
```
