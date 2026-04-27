package com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging;

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
import org.junit.jupiter.api.extension.ExtendWith;
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
@ExtendWith(com.wpanther.debitcreditnote.processing.util.KafkaAvailableCondition.class)
class SagaRouteConfigTest {

    @Autowired
    private CamelContext camelContext;

    @MockBean
    private SagaCommandHandler sagaCommandHandler;

    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

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
            "doc-1", "<xml>test</xml>", "DCN-001"
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
            "process-debit-credit-note", "doc-1", "debit-credit-note"
        );
        String json = objectMapper.writeValueAsString(command);

        try (ProducerTemplate producer = camelContext.createProducerTemplate()) {
            producer.sendBody("direct:saga-compensation", json);
        }

        verify(sagaCommandHandler).handleCompensation(any(CompensateDebitCreditNoteCommand.class));
    }
}