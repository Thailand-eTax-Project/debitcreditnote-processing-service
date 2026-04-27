package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import com.wpanther.debitcreditnote.processing.domain.model.*;
import com.wpanther.debitcreditnote.processing.domain.port.out.ProcessedDebitCreditNoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProcessedDebitCreditNoteRepositoryImpl.
 * Mocks the JpaProcessedDebitCreditNoteRepository and verifies all 5 repository methods.
 */
@ExtendWith(MockitoExtension.class)
class ProcessedDebitCreditNoteRepositoryImplTest {

    @Mock
    private JpaProcessedDebitCreditNoteRepository entityRepository;

    @Mock
    private ProcessedDebitCreditNoteMapper mapper;

    @InjectMocks
    private ProcessedDebitCreditNoteRepositoryImpl repository;

    private ProcessedDebitCreditNote testNote;
    private ProcessedDebitCreditNoteEntity testEntity;

    @BeforeEach
    void setUp() {
        Party seller = Party.of(
            "Test Seller",
            TaxIdentifier.of("1234567890", "VAT"),
            new Address("123 Street", "Bangkok", "10110", "TH"),
            "seller@test.com"
        );

        Party buyer = Party.of(
            "Test Buyer",
            TaxIdentifier.of("9876543210", "VAT"),
            new Address("456 Road", "Chiang Mai", "50000", "TH"),
            "buyer@test.com"
        );

        LineItem item = new LineItem(
            "Test Service",
            10,
            Money.of(new BigDecimal("1000.00"), "THB"),
            new BigDecimal("7.00")
        );

        testNote = ProcessedDebitCreditNote.builder()
            .id(DebitCreditNoteId.generate())
            .sourceNoteId("intake-test-123")
            .noteNumber("DCN-TEST-001")
            .noteType("DEBIT")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .seller(seller)
            .buyer(buyer)
            .addItem(item)
            .currency("THB")
            .originalXml("<xml>test</xml>")
            .build();

        testEntity = ProcessedDebitCreditNoteEntity.builder()
            .id(testNote.getId().getValue())
            .sourceNoteId(testNote.getSourceNoteId())
            .noteNumber(testNote.getNoteNumber())
            .noteType(testNote.getNoteType())
            .issueDate(testNote.getIssueDate())
            .dueDate(testNote.getDueDate())
            .currency(testNote.getCurrency())
            .subtotal(new BigDecimal("10000.00"))
            .totalTax(new BigDecimal("700.00"))
            .total(new BigDecimal("10700.00"))
            .originalXml(testNote.getOriginalXml())
            .status(ProcessingStatus.PROCESSING)
            .build();
    }

    @Test
    void testSave() {
        // Given
        when(mapper.toEntity(testNote)).thenReturn(testEntity);
        when(entityRepository.save(testEntity)).thenReturn(testEntity);
        when(mapper.toDomain(testEntity)).thenReturn(testNote);

        // When
        ProcessedDebitCreditNote result = repository.save(testNote);

        // Then
        assertNotNull(result);
        verify(mapper).toEntity(testNote);
        verify(entityRepository).save(testEntity);
        verify(mapper).toDomain(testEntity);
    }

    @Test
    void testFindById() {
        // Given
        UUID id = testNote.getId().getValue();
        when(entityRepository.findById(id)).thenReturn(Optional.of(testEntity));
        when(mapper.toDomain(testEntity)).thenReturn(testNote);

        // When
        Optional<ProcessedDebitCreditNote> result = repository.findById(testNote.getId());

        // Then
        assertTrue(result.isPresent());
        assertEquals(testNote.getId(), result.get().getId());
        verify(entityRepository).findById(id);
        verify(mapper).toDomain(testEntity);
    }

    @Test
    void testFindByIdNotFound() {
        // Given
        UUID id = UUID.randomUUID();
        when(entityRepository.findById(id)).thenReturn(Optional.empty());

        // When
        Optional<ProcessedDebitCreditNote> result = repository.findById(DebitCreditNoteId.of(id));

        // Then
        assertFalse(result.isPresent());
        verify(entityRepository).findById(id);
        verify(mapper, never()).toDomain(any());
    }

    @Test
    void testFindByNoteNumber() {
        // Given
        when(entityRepository.findByNoteNumber("DCN-TEST-001")).thenReturn(Optional.of(testEntity));
        when(mapper.toDomain(testEntity)).thenReturn(testNote);

        // When
        Optional<ProcessedDebitCreditNote> result = repository.findByNoteNumber("DCN-TEST-001");

        // Then
        assertTrue(result.isPresent());
        assertEquals("DCN-TEST-001", result.get().getNoteNumber());
        verify(entityRepository).findByNoteNumber("DCN-TEST-001");
        verify(mapper).toDomain(testEntity);
    }

    @Test
    void testFindByNoteNumberNotFound() {
        // Given
        when(entityRepository.findByNoteNumber("NON-EXISTENT")).thenReturn(Optional.empty());

        // When
        Optional<ProcessedDebitCreditNote> result = repository.findByNoteNumber("NON-EXISTENT");

        // Then
        assertFalse(result.isPresent());
        verify(entityRepository).findByNoteNumber("NON-EXISTENT");
        verify(mapper, never()).toDomain(any());
    }

    @Test
    void testFindBySourceNoteId() {
        // Given
        when(entityRepository.findBySourceNoteId("intake-test-123")).thenReturn(Optional.of(testEntity));
        when(mapper.toDomain(testEntity)).thenReturn(testNote);

        // When
        Optional<ProcessedDebitCreditNote> result = repository.findBySourceNoteId("intake-test-123");

        // Then
        assertTrue(result.isPresent());
        assertEquals("intake-test-123", result.get().getSourceNoteId());
        verify(entityRepository).findBySourceNoteId("intake-test-123");
        verify(mapper).toDomain(testEntity);
    }

    @Test
    void testFindBySourceNoteIdNotFound() {
        // Given
        when(entityRepository.findBySourceNoteId("non-existent")).thenReturn(Optional.empty());

        // When
        Optional<ProcessedDebitCreditNote> result = repository.findBySourceNoteId("non-existent");

        // Then
        assertFalse(result.isPresent());
        verify(entityRepository).findBySourceNoteId("non-existent");
        verify(mapper, never()).toDomain(any());
    }

    @Test
    void testDeleteById() {
        // Given
        UUID id = testNote.getId().getValue();
        doNothing().when(entityRepository).deleteById(id);

        // When
        repository.deleteById(testNote.getId());

        // Then
        verify(entityRepository).deleteById(id);
    }
}
