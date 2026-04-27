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

<!-- Tasks 10–22 will be added in subsequent batches -->
