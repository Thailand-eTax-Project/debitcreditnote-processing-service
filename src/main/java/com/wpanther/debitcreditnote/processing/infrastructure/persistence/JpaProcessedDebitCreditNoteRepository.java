package com.wpanther.debitcreditnote.processing.infrastructure.persistence;

import com.wpanther.debitcreditnote.processing.domain.model.DebitCreditNoteId;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessingStatus;
import com.wpanther.debitcreditnote.processing.infrastructure.persistence.DebitCreditNotePartyEntity;
import com.wpanther.debitcreditnote.processing.infrastructure.persistence.DebitCreditNoteLineItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaProcessedDebitCreditNoteRepository extends JpaRepository<ProcessedDebitCreditNoteEntity, UUID> {

    @Query("SELECT DISTINCT n FROM ProcessedDebitCreditNoteEntity n " +
            "LEFT JOIN FETCH n.parties p " +
            "LEFT JOIN FETCH n.lineItems li " +
            "WHERE n.id = :id")
    Optional<ProcessedDebitCreditNoteEntity> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT DISTINCT n FROM ProcessedDebitCreditNoteEntity n " +
            "LEFT JOIN FETCH n.parties p " +
            "LEFT JOIN FETCH n.lineItems li " +
            "WHERE n.status = :status")
    List<ProcessedDebitCreditNoteEntity> findByStatusWithDetails(@Param("status") String status);

    @Query("SELECT n FROM ProcessedDebitCreditNoteEntity n WHERE n.noteNumber = :noteNumber")
    Optional<ProcessedDebitCreditNoteEntity> findByNoteNumber(@Param("noteNumber") String noteNumber);

    @Query("SELECT n FROM ProcessedDebitCreditNoteEntity n WHERE n.sourceNoteId = :sourceNoteId")
    Optional<ProcessedDebitCreditNoteEntity> findBySourceNoteId(@Param("sourceNoteId") String sourceNoteId);

    void deleteById(UUID id);
}
