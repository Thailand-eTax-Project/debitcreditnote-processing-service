package com.wpanther.debitcreditnote.processing.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ProcessedDebitCreditNote {

    private final DebitCreditNoteId id;
    private final String sourceNoteId;

    private final String noteNumber;
    private final String noteType;
    private final LocalDate issueDate;
    private final LocalDate dueDate;

    private final Party seller;
    private final Party buyer;

    private final List<LineItem> items;

    private final String currency;

    private final String originalXml;

    private ProcessingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;

    private transient Money cachedSubtotal;
    private transient Money cachedTotalTax;
    private transient Money cachedTotal;

    private ProcessedDebitCreditNote(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Debit/Credit Note ID is required");
        this.sourceNoteId = Objects.requireNonNull(builder.sourceNoteId, "Source note ID is required");
        this.noteNumber = Objects.requireNonNull(builder.noteNumber, "Note number is required");
        this.noteType = Objects.requireNonNull(builder.noteType, "Note type is required");
        this.issueDate = Objects.requireNonNull(builder.issueDate, "Issue date is required");
        this.dueDate = Objects.requireNonNull(builder.dueDate, "Due date is required");
        this.seller = Objects.requireNonNull(builder.seller, "Seller is required");
        this.buyer = Objects.requireNonNull(builder.buyer, "Buyer is required");
        this.items = new ArrayList<>(Objects.requireNonNull(builder.items, "Items are required"));
        this.currency = Objects.requireNonNull(builder.currency, "Currency is required");
        this.originalXml = Objects.requireNonNull(builder.originalXml, "Original XML is required");
        this.status = builder.status != null ? builder.status : ProcessingStatus.PENDING;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.completedAt = builder.completedAt;
        this.errorMessage = builder.errorMessage;

        validateInvariant();
    }

    private void validateInvariant() {
        if (items.isEmpty()) {
            throw new IllegalStateException("Debit/Credit note must have at least one line item");
        }

        if (dueDate.isBefore(issueDate)) {
            throw new IllegalStateException("Due date cannot be before issue date");
        }

        if (currency.length() != 3) {
            throw new IllegalStateException("Currency must be a 3-letter ISO code");
        }

        items.forEach(item -> {
            if (!item.unitPrice().currency().equals(currency)) {
                throw new IllegalStateException(
                    String.format("Line item currency %s does not match note currency %s",
                        item.unitPrice().currency(), currency)
                );
            }
        });
    }

    public Money getSubtotal() {
        if (cachedSubtotal == null) {
            cachedSubtotal = items.stream()
                .map(LineItem::getLineTotal)
                .reduce(Money.zero(currency), Money::add);
        }
        return cachedSubtotal;
    }

    public Money getTotalTax() {
        if (cachedTotalTax == null) {
            cachedTotalTax = items.stream()
                .map(LineItem::getTaxAmount)
                .reduce(Money.zero(currency), Money::add);
        }
        return cachedTotalTax;
    }

    public Money getTotal() {
        if (cachedTotal == null) {
            cachedTotal = getSubtotal().add(getTotalTax());
        }
        return cachedTotal;
    }

    public void startProcessing() {
        if (status != ProcessingStatus.PENDING) {
            throw new IllegalStateException("Can only start processing from PENDING status");
        }
        this.status = ProcessingStatus.PROCESSING;
    }

    public void markCompleted() {
        if (status != ProcessingStatus.PROCESSING) {
            throw new IllegalStateException("Can only complete from PROCESSING status");
        }
        this.status = ProcessingStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public DebitCreditNoteId getId() {
        return id;
    }

    public String getSourceNoteId() {
        return sourceNoteId;
    }

    public String getNoteNumber() {
        return noteNumber;
    }

    public String getNoteType() {
        return noteType;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Party getSeller() {
        return seller;
    }

    public Party getBuyer() {
        return buyer;
    }

    public List<LineItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public String getCurrency() {
        return currency;
    }

    public String getOriginalXml() {
        return originalXml;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static class Builder {
        private DebitCreditNoteId id;
        private String sourceNoteId;
        private String noteNumber;
        private String noteType;
        private LocalDate issueDate;
        private LocalDate dueDate;
        private Party seller;
        private Party buyer;
        private List<LineItem> items = new ArrayList<>();
        private String currency;
        private String originalXml;
        private ProcessingStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        private String errorMessage;

        public Builder id(DebitCreditNoteId id) {
            this.id = id;
            return this;
        }

        public Builder sourceNoteId(String sourceNoteId) {
            this.sourceNoteId = sourceNoteId;
            return this;
        }

        public Builder noteNumber(String noteNumber) {
            this.noteNumber = noteNumber;
            return this;
        }

        public Builder noteType(String noteType) {
            this.noteType = noteType;
            return this;
        }

        public Builder issueDate(LocalDate issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        public Builder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder seller(Party seller) {
            this.seller = seller;
            return this;
        }

        public Builder buyer(Party buyer) {
            this.buyer = buyer;
            return this;
        }

        public Builder items(List<LineItem> items) {
            this.items = items;
            return this;
        }

        public Builder addItem(LineItem item) {
            this.items.add(item);
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder originalXml(String originalXml) {
            this.originalXml = originalXml;
            return this;
        }

        public Builder status(ProcessingStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder completedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ProcessedDebitCreditNote build() {
            return new ProcessedDebitCreditNote(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
