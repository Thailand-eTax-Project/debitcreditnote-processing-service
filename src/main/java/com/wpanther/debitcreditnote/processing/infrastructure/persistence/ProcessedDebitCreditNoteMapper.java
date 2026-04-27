package com.wpanther.debitcreditnote.processing.infrastructure.persistence;

import com.wpanther.debitcreditnote.processing.domain.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

        domain.getSeller();
        entity.addParty(toPartyEntity(domain.getSeller(), "SELLER", entity));

        domain.getBuyer();
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

    private DebitCreditNotePartyEntity toPartyEntity(Party domain, String partyType, ProcessedDebitCreditNoteEntity noteEntity) {
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

    private DebitCreditNoteLineItemEntity toLineItemEntity(LineItem domain, int lineNumber, ProcessedDebitCreditNoteEntity noteEntity) {
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
                Address.of(
                        entity.getStreetAddress(),
                        entity.getCity(),
                        entity.getPostalCode(),
                        entity.getCountry()
                ),
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
