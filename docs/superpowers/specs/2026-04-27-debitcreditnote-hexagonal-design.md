# Debit/Credit Note Processing Service — Hexagonal Architecture + DDD Refactor

**Date:** 2026-04-27  
**Reference:** `taxinvoice-processing-service` (full structural and behavioural parity)  
**Scope:** Approach A — full parity including metrics, REQUIRES_NEW transaction handling, pure domain event record, trimmed repository port, unit tests only.

---

## 1. Goal

Refactor `debitcreditnote-processing-service` to hexagonal architecture (ports and adapters) with DDD layering, matching `taxinvoice-processing-service` exactly in structure and behaviour. Directory structure and file naming conventions follow the reference.

---

## 2. Package Structure

Root package: `com.wpanther.debitcreditnote.processing`

```
application/
  dto/event/
    DebitCreditNoteProcessedEvent.java          ← Kafka DTO (outbound)
  port/in/
    ProcessDebitCreditNoteUseCase.java          ← driving port + ProcessingException
    CompensateDebitCreditNoteUseCase.java       ← compensation port + CompensationException
  port/out/
    SagaReplyPort.java                          ← publishSuccess / publishFailure / publishCompensated
    DebitCreditNoteEventPublishingPort.java     ← publish(DebitCreditNoteProcessedDomainEvent)
  service/
    DebitCreditNoteProcessingService.java       ← implements both use cases; owns metrics + REQUIRES_NEW

domain/
  event/
    DebitCreditNoteProcessedDomainEvent.java    ← pure Java record; no Jackson, no currency; sagaId included
  model/
    Address.java                                ← unchanged
    DebitCreditNoteId.java                      ← unchanged
    LineItem.java                               ← unchanged
    Money.java                                  ← unchanged
    Party.java                                  ← unchanged
    ProcessedDebitCreditNote.java               ← unchanged
    ProcessingStatus.java                       ← unchanged
    TaxIdentifier.java                          ← unchanged
  port/out/
    ProcessedDebitCreditNoteRepository.java     ← moved from domain/repository/ + trimmed
    DebitCreditNoteParserPort.java              ← renamed from domain/service/DebitCreditNoteParserService

infrastructure/
  adapter/
    in/
      messaging/
        SagaCommandHandler.java                 ← thin delegate; no @Transactional; no direct ports
        SagaRouteConfig.java                    ← moved from infrastructure/config/
        dto/
          ProcessDebitCreditNoteCommand.java    ← moved from domain/event/; sagaStep → SagaStep enum
          CompensateDebitCreditNoteCommand.java ← moved from domain/event/; sagaStep → SagaStep enum
    out/
      messaging/
        SagaReplyPublisher.java                 ← implements SagaReplyPort; publishFailure REQUIRES_NEW
        DebitCreditNoteEventPublisher.java      ← implements DebitCreditNoteEventPublishingPort
        HeaderSerializer.java                   ← header serialization utility
        dto/
          DebitCreditNoteReplyEvent.java        ← moved from domain/event/
      parsing/
        DebitCreditNoteParserServiceImpl.java   ← moved from infrastructure/service/; implements DebitCreditNoteParserPort
      persistence/
        ProcessedDebitCreditNoteEntity.java     ← moved
        DebitCreditNotePartyEntity.java         ← moved
        DebitCreditNoteLineItemEntity.java      ← moved
        ProcessedDebitCreditNoteMapper.java     ← moved
        ProcessedDebitCreditNoteRepositoryImpl.java ← moved + trimmed
        JpaProcessedDebitCreditNoteRepository.java  ← moved + trimmed
        outbox/
          OutboxEventEntity.java                ← moved
          JpaOutboxEventRepository.java         ← moved
          SpringDataOutboxRepository.java       ← moved
          OutboxCleanupScheduler.java           ← new; matches reference
  config/
    KafkaTopicsProperties.java                  ← new @ConfigurationProperties(prefix="app.kafka.topics")
    OutboxConfig.java                           ← moved from infrastructure/config/
```

### Files deleted (absorbed into new locations)

| Deleted file | Replaced by |
|---|---|
| `domain/event/ProcessDebitCreditNoteCommand.java` | `infrastructure/adapter/in/messaging/dto/` |
| `domain/event/CompensateDebitCreditNoteCommand.java` | `infrastructure/adapter/in/messaging/dto/` |
| `domain/event/DebitCreditNoteReplyEvent.java` | `infrastructure/adapter/out/messaging/dto/` |
| `domain/event/DebitCreditNoteProcessedEvent.java` | `application/dto/event/` (Kafka DTO) + `domain/event/` (pure record) |
| `domain/repository/ProcessedDebitCreditNoteRepository.java` | `domain/port/out/` |
| `domain/service/DebitCreditNoteParserService.java` | `domain/port/out/DebitCreditNoteParserPort.java` |
| `application/service/SagaCommandHandler.java` | `infrastructure/adapter/in/messaging/SagaCommandHandler.java` |
| `infrastructure/config/SagaRouteConfig.java` | `infrastructure/adapter/in/messaging/SagaRouteConfig.java` |
| `infrastructure/messaging/EventPublisher.java` | `infrastructure/adapter/out/messaging/DebitCreditNoteEventPublisher.java` |
| `infrastructure/messaging/SagaReplyPublisher.java` | `infrastructure/adapter/out/messaging/SagaReplyPublisher.java` |
| `infrastructure/service/DebitCreditNoteParserServiceImpl.java` | `infrastructure/adapter/out/parsing/` |

---

## 3. Port Interfaces

### 3.1 Inbound Ports (`application/port/in/`)

**`ProcessDebitCreditNoteUseCase`**
```java
void process(String documentId, String xmlContent,
             String sagaId, SagaStep sagaStep, String correlationId)
    throws ProcessingException;

class ProcessingException extends Exception { ... }
```

**`CompensateDebitCreditNoteUseCase`**
```java
void compensate(String documentId, String sagaId,
                SagaStep sagaStep, String correlationId);

class CompensationException extends RuntimeException { ... }  // unchecked — Camel DLC retry
```

### 3.2 Application Outbound Ports (`application/port/out/`)

**`SagaReplyPort`**
```java
void publishSuccess(String sagaId, SagaStep sagaStep, String correlationId);
void publishFailure(String sagaId, SagaStep sagaStep, String correlationId, String errorMessage);
void publishCompensated(String sagaId, SagaStep sagaStep, String correlationId);
```

**`DebitCreditNoteEventPublishingPort`**
```java
void publish(DebitCreditNoteProcessedDomainEvent event);  // MANDATORY propagation on adapter
```

### 3.3 Domain Outbound Ports (`domain/port/out/`)

**`ProcessedDebitCreditNoteRepository`** (trimmed)
```java
ProcessedDebitCreditNote save(ProcessedDebitCreditNote note);
Optional<ProcessedDebitCreditNote> findById(DebitCreditNoteId id);
Optional<ProcessedDebitCreditNote> findByNoteNumber(String noteNumber);
Optional<ProcessedDebitCreditNote> findBySourceNoteId(String sourceNoteId);
void deleteById(DebitCreditNoteId id);
// Removed: findByIdWithDetails, findByStatusWithDetails, existsBySourceNoteId
```

**`DebitCreditNoteParserPort`**
```java
ProcessedDebitCreditNote parse(String xmlContent, String sourceNoteId)
    throws ParsingException;

class ParsingException extends Exception {
    static ParsingException forEmpty() { ... }
    static ParsingException forOversized(int byteSize, int limitBytes) { ... }
    static ParsingException forTimeout(long timeoutMs) { ... }
    static ParsingException forInterrupted() { ... }
    static ParsingException forUnmarshal(Throwable cause) { ... }
    static ParsingException forUnexpectedRootElement(String className) { ... }
}
```

### 3.4 Domain Event

**`DebitCreditNoteProcessedDomainEvent`** — pure Java record, no Jackson, no framework deps
```java
record DebitCreditNoteProcessedDomainEvent(
    String documentId,
    String documentNumber,
    Money total,
    String sagaId,
    String correlationId,
    Instant occurredAt
) {
    static DebitCreditNoteProcessedDomainEvent of(
        String documentId, String documentNumber, Money total,
        String sagaId, String correlationId) { ... }  // stamps occurredAt = Instant.now()
}
```
No `currency` field — downstream consumers derive currency from context.

---

## 4. Application Service

`DebitCreditNoteProcessingService` implements `ProcessDebitCreditNoteUseCase` and `CompensateDebitCreditNoteUseCase`.

**Constructor dependencies:** `ProcessedDebitCreditNoteRepository`, `DebitCreditNoteParserPort`, `DebitCreditNoteEventPublishingPort`, `SagaReplyPort`, `MeterRegistry`, `PlatformTransactionManager`.

### 4.1 Metrics (initialized in constructor)

| Counter name | Purpose |
|---|---|
| `debitcreditnote.processing.success` | Successful processing |
| `debitcreditnote.processing.failure` | Failed processing |
| `debitcreditnote.processing.idempotent` | Duplicate commands (already COMPLETED) |
| `debitcreditnote.processing.race_condition_resolved` | Concurrent insert resolved via REQUIRES_NEW |
| `debitcreditnote.compensation.success` | Successful compensation |
| `debitcreditnote.compensation.idempotent` | Already-deleted document compensated |
| `debitcreditnote.compensation.failure` | Failed compensation |
| `debitcreditnote.processing.duration` (Timer) | Wall-clock time for processing |

### 4.2 `process()` — `@Transactional`

1. Start timer sample.
2. Call `processInternal(...)` — idempotency check on `findBySourceNoteId`:
   - **COMPLETED** → `publishSuccess`, return (idempotent counter).
   - **PROCESSING** → `markCompleted`, save, publish domain event, `publishSuccess` (resume).
   - **Other persisted status** → throw `IllegalStateException`.
   - **Not found** → parse → `startProcessing` → save → `markCompleted` → save → publish domain event → `publishSuccess`.
3. Catch `ParsingException` → `publishFailure`, throw `ProcessingException`.
4. Catch `DuplicateKeyException`:
   - If NOT `uq_processed_debit_credit_notes_source_note_id` violation → `publishFailure`, throw.
   - If IS source-note-id violation → `REQUIRES_NEW` re-check: found → `publishSuccess` (race resolved); not found → `publishFailure`.
   - Always throw `ProcessingException` to keep outer transaction ROLLBACK_ONLY.
5. Catch `DataIntegrityViolationException` → `publishFailure`, throw `ProcessingException`.
6. Catch `Exception` → `publishFailure` (REQUIRES_NEW inside port), throw `ProcessingException`.
7. Finally: stop timer sample.

### 4.3 `compensate()` — `@Transactional`

1. `findBySourceNoteId` — delete if present; log idempotent warning if absent.
2. `publishCompensated`.
3. Increment success counter.
4. On any exception: increment failure counter, `publishFailure`, throw `CompensationException` (propagates to Camel DLC for retry).

### 4.4 Transaction propagation on `SagaReplyPublisher`

| Method | Propagation | Reason |
|---|---|---|
| `publishSuccess` | `MANDATORY` | Must piggyback on caller's transaction |
| `publishCompensated` | `MANDATORY` | Must piggyback on caller's transaction |
| `publishFailure` | `REQUIRES_NEW` | Must commit even when outer transaction is ROLLBACK_ONLY |

---

## 5. Infrastructure Adapters

### 5.1 Inbound — `SagaCommandHandler`

- `@Component`, no `@Transactional`, no direct repository or publisher access.
- `handleProcessCommand`: calls `processUseCase.process(...)`, catches only `ProcessingException` (reply already committed to outbox), returns normally. All other exceptions propagate to Camel DLC.
- `handleCompensation`: calls `compensateUseCase.compensate(...)` with no try/catch. `CompensationException` propagates to Camel DLC for retry.

### 5.2 Inbound — Command DTOs

Both `ProcessDebitCreditNoteCommand` and `CompensateDebitCreditNoteCommand` move from `domain/event/` to `infrastructure/adapter/in/messaging/dto/`. The `sagaStep` field type changes from `String` to `SagaStep` enum.

### 5.3 Outbound — `DebitCreditNoteEventPublisher`

Implements `DebitCreditNoteEventPublishingPort`. Accepts pure `DebitCreditNoteProcessedDomainEvent`, translates internally to Kafka DTO `DebitCreditNoteProcessedEvent` (in `application/dto/event/`), then writes to outbox. `Propagation.MANDATORY`.

### 5.4 Outbound — Parser

`DebitCreditNoteParserServiceImpl` moves to `infrastructure/adapter/out/parsing/`. Method renamed `parseNote` → `parse`. Adds defensive guards matching the reference (size limit, timeout via `ExecutorService`, static factory exceptions on `DebitCreditNoteParserPort.ParsingException`). Uses `debitcreditnote.rsm.impl` and `debitcreditnote.ram.impl` JAXB packages.

### 5.5 Outbound — Persistence

`ProcessedDebitCreditNoteRepositoryImpl` and `JpaProcessedDebitCreditNoteRepository` trimmed to the 5-method port contract. `OutboxCleanupScheduler` added.

### 5.6 Config

`KafkaTopicsProperties` typed `@ConfigurationProperties` bean replaces raw `@Value` injections for topic names in `SagaRouteConfig`.

---

## 6. Test Suite (Unit Tests Only)

```
domain/model/
  DebitCreditNoteIdTest
  MoneyTest
  AddressTest
  TaxIdentifierTest
  PartyTest
  LineItemTest
  ProcessedDebitCreditNoteTest          ← state machine, invariants
  ProcessingStatusTest

domain/event/
  DebitCreditNoteProcessedDomainEventTest  ← record construction, of() factory

application/service/
  DebitCreditNoteProcessingServiceTest  ← all paths: happy, idempotent COMPLETED,
                                           resume PROCESSING, parse failure, DuplicateKey
                                           (source_note_id + other), DataIntegrity,
                                           generic exception, compensate success/idempotent/failure,
                                           metrics counters verified per path

infrastructure/adapter/in/messaging/
  SagaCommandHandlerTest                ← delegation, ProcessingException caught, others propagate
  SagaRouteConfigTest

infrastructure/adapter/out/messaging/
  SagaReplyPublisherTest                ← outbox write per status
  SagaReplyPublisherTransactionTest     ← publishFailure commits in REQUIRES_NEW while outer is ROLLBACK_ONLY
  DebitCreditNoteEventPublisherTest     ← domain event → Kafka DTO translation, outbox write
  HeaderSerializerTest

infrastructure/adapter/out/parsing/
  DebitCreditNoteParserServiceImplTest  ← valid XML, empty, oversized, unexpected root, malformed

infrastructure/adapter/out/persistence/
  ProcessedDebitCreditNoteEntityTest
  DebitCreditNotePartyEntityTest
  DebitCreditNoteLineItemEntityTest
  ProcessedDebitCreditNoteMapperTest    ← domain ↔ entity round-trip
  ProcessedDebitCreditNoteRepositoryImplTest
  outbox/
    OutboxEventEntityTest
    JpaOutboxEventRepositoryTest
    OutboxCleanupSchedulerTest

DebitCreditNoteProcessingServiceApplicationTest  ← @SpringBootTest smoke test (H2 + mock Camel)
```

---

## 7. Key Invariants & Constraints

- **Unique constraint name** used in `isSourceNoteIdViolation`: `uq_processed_debit_credit_notes_source_note_id` — must match Flyway V1 migration exactly.
- **`existsBySourceNoteId`** removed from repository port; all callers use `findBySourceNoteId(...).isPresent()`.
- **`currency`** removed from `DebitCreditNoteProcessedDomainEvent`; downstream consumers derive it from context.
- **`SagaStep`** enum replaces `String sagaStep` in all command DTOs and use case signatures.
- **`parseNote`** renamed to `parse` on `DebitCreditNoteParserPort` for consistency with the reference.
- **Annotation processor order** in `pom.xml`: Lombok before MapStruct (unchanged, already correct).
- **No integration tests** in this refactor — CDC and Kafka consumer integration tests deferred.
