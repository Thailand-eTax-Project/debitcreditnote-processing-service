package com.wpanther.debitcreditnote.processing.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence.outbox.SpringDataOutboxRepository;
import com.wpanther.saga.domain.outbox.OutboxEventRepository;
import com.wpanther.saga.infrastructure.outbox.OutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxConfigTest {

    @Mock
    private SpringDataOutboxRepository springDataRepository;

    @Test
    void outboxEventRepository_returnsSpringDataRepository() {
        OutboxConfig config = new OutboxConfig();
        OutboxEventRepository result = config.outboxEventRepository(springDataRepository);

        assertNotNull(result);
        assertEquals(springDataRepository, result);
    }

    @Test
    void outboxService_createsWithObjectMapper() {
        OutboxConfig config = new OutboxConfig();
        ObjectMapper objectMapper = new ObjectMapper();

        OutboxService result = config.outboxService(springDataRepository, objectMapper);

        assertNotNull(result);
    }
}