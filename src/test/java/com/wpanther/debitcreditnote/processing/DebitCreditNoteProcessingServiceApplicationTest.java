package com.wpanther.debitcreditnote.processing;

import com.wpanther.debitcreditnote.processing.util.KafkaAvailableCondition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(KafkaAvailableCondition.class)
class DebitCreditNoteProcessingServiceApplicationTest {

    @Test
    void contextLoads() {
        // Smoke test — verifies Spring context starts with H2 + mock Camel
    }
}
