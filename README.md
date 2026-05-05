# Debit/Credit Note Processing Service

A saga-orchestrated microservice that processes Thai e-Tax Debit/Credit Note XML documents.

## Overview

This service is part of the Thai e-Tax invoice microservices ecosystem. It receives debit/credit note XML documents via Kafka saga commands, parses and validates them using the `teda` library, persists processed data to PostgreSQL, and replies to the saga orchestrator.

**Key capabilities:**
- Parse Thai e-Tax Debit/Credit Note XML (Cross-Industry Invoice ubl export format) via JAXB
- Calculate line-item subtotals, tax amounts, and grand totals
- Idempotent processing with race-condition protection on concurrent inserts
- Saga orchestration coordination (process + compensate)
- Transactional outbox pattern for exactly-once delivery
- Micrometer metrics for Prometheus monitoring

## Architecture

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Kafka Consumer (Camel)                                                  │
│  ┌──────────────────────────────┐                                        │
│  │  saga.command.debit-credit-note ──→ SagaCommandHandler                │
│  └──────────────────────────────┘                                        │
│                                    │                                      │
│                                    ▼                                      │
│                    ┌───────────────────────────────┐                     │
│                    │  DebitCreditNoteProcessingService                  │
│                    │    (ProcessDebitCreditNoteUseCase)                 │
│                    └───────────────────────────────┘                     │
│                           │                    │                         │
│            ┌──────────────┘                    └──────────────┐         │
│            ▼                                                  ▼         │
│  DebitCreditNoteParserPort                  SagaReplyPort              │
│  (DebitCreditNoteParserServiceImpl)         (SagaReplyPublisher)      │
│            │                                                  │         │
│            ▼                                                  ▼         │
│  teda library JAXB classes                  OutboxService ──→ Kafka     │
│  (DebitCreditNote_CrossIndustryInvoiceType)        │                    │
│                                                 ▼                        │
│  ProcessedDebitCreditNoteRepository              Kafka                  │
│  (Jpa/PostgreSQL)                         saga.reply.debit-credit-note │
└──────────────────────────────────────────────────────────────────────────┘
```

## Domain Model

### Aggregate Root: `ProcessedDebitCreditNote`

```
ProcessedDebitCreditNote
├── id: DebitCreditNoteId         (UUID, internal identity)
├── sourceNoteId: String          (XML ID attribute, unique constraint)
├── noteNumber: String            (document ID from XML)
├── noteType: String              (e.g. "381" = Debit Note, "383" = Credit Note)
├── issueDate: LocalDate
├── dueDate: LocalDate
├── seller: Party
├── buyer: Party
├── items: List<LineItem>
├── currency: String             (3-letter ISO code, e.g. "THB")
├── originalXml: String
├── status: ProcessingStatus      (PENDING → PROCESSING → COMPLETED | FAILED)
├── createdAt: LocalDateTime
├── completedAt: LocalDateTime
└── errorMessage: String

Money (value object)
├── amount: BigDecimal
└── currency: String

LineItem (value object)
├── description: String
├── quantity: int
├── unitPrice: Money
└── taxRate: BigDecimal           (percentage, e.g. 7.0 for 7%)

Party (value object)
├── name: String
├── taxIdentifier: TaxIdentifier
├── address: Address
└── email: String

Address (value object)
├── streetAddress: String
├── city: String
├── postalCode: String
└── country: String               (ISO 3166-1 alpha-2, e.g. "TH")

TaxIdentifier (value object)
├── id: String                    (e.g. "01055630341123")
└── scheme: String                (e.g. "VAT", "EIN", "TAX")

ProcessingStatus (enum)
├── PENDING
├── PROCESSING
├── COMPLETED
└── FAILED
```

## Kafka Integration

### Consumer Topics

| Topic | Consumer | Description |
|-------|----------|-------------|
| `saga.command.debit-credit-note` | SagaCommandHandler | Process command from orchestrator |
| `saga.compensation.debit-credit-note` | SagaCommandHandler | Compensate/rollback command |

### Producer Topics

| Topic | Producer | Description |
|-------|----------|-------------|
| `saga.reply.debit-credit-note` | SagaReplyPublisher | SUCCESS / FAILURE / COMPENSATED reply to orchestrator |
| `debit.credit.note.processed` | DebitCreditNoteEventPublisher | Notification event for downstream consumers |

### Message Schemas

**Process Command** (`ProcessDebitCreditNoteCommand`):
```json
{
  "documentId": "string",
  "xmlContent": "string",
  "sagaId": "string",
  "sagaStep": "DEBIT_CREDIT_NOTE",
  "correlationId": "string"
}
```

**Reply Event** (`DebitCreditNoteReplyEvent`):
```json
{
  "sagaId": "string",
  "sagaStep": "DEBIT_CREDIT_NOTE",
  "correlationId": "string",
  "status": "SUCCESS | FAILURE | COMPENSATED",
  "errorMessage": "string (optional)"
}
```

## API Reference

This service has **no REST API**. All interaction is via Kafka:

### Process Flow

1. Receive `ProcessDebitCreditNoteCommand` from `saga.command.debit-credit-note`
2. Check idempotency by `sourceNoteId`:
   - If `COMPLETED` → reply `SUCCESS` (idempotent)
   - If `PROCESSING` → resume and complete (partial-failure recovery)
   - If not found → parse, save as `PROCESSING`, then `COMPLETED`
3. Publish `DebitCreditNoteProcessedDomainEvent` to `debit.credit.note.processed`
4. Reply `SUCCESS` via saga outbox to `saga.reply.debit-credit-note`

### Compensate Flow

1. Receive `CompensateDebitCreditNoteCommand` from `saga.compensation.debit-credit-note`
2. Delete `ProcessedDebitCreditNote` by `sourceNoteId` (idempotent if absent)
3. Reply `COMPENSATED` via saga outbox

### Race Condition Handling

When two concurrent threads attempt to insert the same `sourceNoteId`:

- PostgreSQL unique constraint violation (`SQLState 23505`)
- One thread wins; the other catches `DuplicateKeyException`
- In a `REQUIRES_NEW` transaction, re-check if the record was committed
- If committed → reply `SUCCESS` (the document is already processed)
- If not found → reply `FAILURE`

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `KAFKA_BROKERS` | `localhost:9092` | Kafka bootstrap servers |
| `EUREKA_URL` | `http://localhost:8761/eureka/` | Eureka server URL |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `DB_NAME` | `debitcreditnote_db` | Database name |

### Application Properties

| Property | Default | Description |
|----------|---------|-------------|
| `app.parsing.timeout-seconds` | `10` | JAXB parse timeout (seconds) |
| `app.parsing.max-concurrent` | `300` | Max concurrent parse operations |
| `app.debitcreditnote.default-due-date-days` | `30` | Default due date when XML omits it |

## Database Schema

PostgreSQL with Flyway migrations:

```
V1__create_processed_debit_credit_notes_table.sql
V2__create_debit_credit_note_parties_table.sql
V3__create_debit_credit_note_line_items_table.sql
V4__create_outbox_events_table.sql
```

### Key Tables

**processed_debit_credit_notes**
- `id` (UUID, PK)
- `source_note_id` (VARCHAR, UNIQUE INDEX `uq_processed_debit_credit_notes_source_note_id`)
- `note_number`, `note_type`, `issue_date`, `due_date`
- `currency`, `original_xml`
- `status`, `created_at`, `completed_at`, `error_message`

**processed_debit_credit_note_parties** (one row per party type)
- `id` (UUID, PK)
- `parent_id` (UUID, FK → processed_debit_credit_notes)
- `party_type` (SELLER / BUYER)
- `name`, `tax_id`, `tax_scheme`, `email`
- `street_address`, `city`, `postal_code`, `country`

**processed_debit_credit_note_line_items**
- `id` (UUID, PK)
- `parent_id` (UUID, FK → processed_debit_credit_notes)
- `description`, `quantity`, `unit_price_amount`, `unit_price_currency`
- `tax_rate`, `line_total_amount`, `line_total_currency`

**outbox_events** (transactional outbox)
- Standard saga-commons outbox schema

## Building & Running

### Prerequisites

- Java 21+
- Maven 3.6+
- PostgreSQL 16+ with database `debitcreditnote_db`
- Kafka on `localhost:9092`
- `teda` library installed (`cd ../../teda && mvn clean install`)
- `saga-commons` library installed (`cd ../../saga-commons && mvn clean install`)

### Build

```bash
mvn clean package
```

### Run Tests

```bash
mvn test                  # Unit tests only
mvn verify                # Unit + JaCoCo coverage check (90% per bundle)
mvn test -Dtest=DebitCreditNoteParserServiceImplTest  # Single test class
mvn test -Dtest=ProcessedDebitCreditNoteTest#testSubtotalCalculation  # Single method
```

### Run Service

```bash
mvn spring-boot:run
```

## Metrics

Prometheus-compatible metrics via Micrometer:

| Metric | Type | Description |
|--------|------|-------------|
| `debitcreditnote_processing_success_total` | Counter | Successfully processed notes |
| `debitcreditnote_processing_failure_total` | Counter | Failed processing attempts |
| `debitcreditnote_processing_idempotent_total` | Counter | Idempotent duplicates (already completed) |
| `debitcreditnote_processing_race_condition_resolved_total` | Counter | Race-condition duplicates resolved as success |
| `debitcreditnote_compensation_success_total` | Counter | Successful compensations |
| `debitcreditnote_compensation_idempotent_total` | Counter | Duplicate compensations (not found) |
| `debitcreditnote_compensation_failure_total` | Counter | Failed compensations |
| `debitcreditnote_processing_duration_seconds` | Timer | Processing time histogram |

## Project Structure

```
src/main/java/com/wpanther/debitcreditnote/processing/
├── DebitCreditNoteProcessingServiceApplication.java
├── domain/
│   ├── model/                          # Domain value objects & entities
│   │   ├── ProcessedDebitCreditNote.java
│   │   ├── Money.java
│   │   ├── Party.java
│   │   ├── LineItem.java
│   │   ├── Address.java
│   │   ├── TaxIdentifier.java
│   │   ├── DebitCreditNoteId.java
│   │   └── ProcessingStatus.java
│   ├── event/
│   │   └── DebitCreditNoteProcessedDomainEvent.java
│   └── port/
│       └── out/
│           ├── DebitCreditNoteParserPort.java
│           └── ProcessedDebitCreditNoteRepository.java
├── application/
│   ├── service/
│   │   └── DebitCreditNoteProcessingService.java
│   ├── port/
│   │   ├── in/
│   │   │   ├── ProcessDebitCreditNoteUseCase.java
│   │   │   └── CompensateDebitCreditNoteUseCase.java
│   │   └── out/
│   │       ├── DebitCreditNoteEventPublishingPort.java
│   │       └── SagaReplyPort.java
│   └── dto/
│       └── event/
│           └── DebitCreditNoteProcessedEvent.java
└── infrastructure/
    ├── config/
    │   ├── OutboxConfig.java
    │   └── KafkaTopicsProperties.java
    └── adapter/
        ├── in/
        │   └── messaging/
        │       ├── SagaCommandHandler.java
        │       ├── SagaRouteConfig.java
        │       └── dto/
        │           ├── ProcessDebitCreditNoteCommand.java
        │           └── CompensateDebitCreditNoteCommand.java
        └── out/
            ├── parsing/
            │   └── DebitCreditNoteParserServiceImpl.java
            ├── persistence/
            │   ├── ProcessedDebitCreditNoteEntity.java
            │   ├── DebitCreditNotePartyEntity.java
            │   ├── DebitCreditNoteLineItemEntity.java
            │   ├── JpaProcessedDebitCreditNoteRepository.java
            │   ├── ProcessedDebitCreditNoteMapper.java
            │   ├── ProcessedDebitCreditNoteRepositoryImpl.java
            │   └── outbox/
            │       ├── OutboxEventEntity.java
            │       ├── JpaOutboxEventRepository.java
            │       ├── SpringDataOutboxRepository.java
            │       └── OutboxCleanupScheduler.java
            └── messaging/
                ├── SagaReplyPublisher.java
                ├── HeaderSerializer.java
                ├── DebitCreditNoteEventPublisher.java
                └── dto/
                    └── DebitCreditNoteReplyEvent.java
```

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Spring Boot | 3.2.5 | Application framework |
| Apache Camel | 4.14.4 | Kafka consumer/routing |
| teda | 1.0.0 | Thai e-Tax JAXB classes |
| saga-commons | 1.0.0-SNAPSHOT | Outbox pattern, saga DTOs |
| PostgreSQL | driver | Persistent storage |
| Flyway | 10.10.0 | Database migrations |
| Lombok | 1.18.30 | Boilerplate reduction |
| MapStruct | 1.5.5.Final | Entity ↔ Domain mapping |
| Micrometer | 1.12+ | Metrics (Prometheus) |