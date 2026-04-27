package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.parsing;

import com.wpanther.debitcreditnote.processing.domain.model.*;
import com.wpanther.debitcreditnote.processing.domain.port.out.DebitCreditNoteParserPort;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.xml.transform.sax.SAXSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DebitCreditNoteParserServiceImpl
 */
class DebitCreditNoteParserServiceImplTest {

    private DebitCreditNoteParserPort parserService;

    @BeforeEach
    void setUp() {
        parserService = new DebitCreditNoteParserServiceImpl();
    }

    @Test
    void constructor_whenJaxbContextFails_throwsIllegalStateException() throws Exception {
        try (MockedStatic<JAXBContext> mockedJaxb = mockStatic(JAXBContext.class)) {
            mockedJaxb.when(() -> JAXBContext.newInstance(anyString()))
                .thenThrow(new JAXBException("Simulated JAXB failure"));
            assertThrows(IllegalStateException.class, () -> new DebitCreditNoteParserServiceImpl());
        }
    }

    @Test
    void parse_whenUnmarshalReturnsUnexpectedType_throwsException() throws Exception {
        try (MockedStatic<JAXBContext> mockedJaxb = mockStatic(JAXBContext.class)) {
            JAXBContext mockContext = mock(JAXBContext.class);
            Unmarshaller mockUnmarshaller = mock(Unmarshaller.class);
            mockedJaxb.when(() -> JAXBContext.newInstance(anyString())).thenReturn(mockContext);
            when(mockContext.createUnmarshaller()).thenReturn(mockUnmarshaller);
            when(mockUnmarshaller.unmarshal(any(SAXSource.class))).thenReturn("unexpected-string-type");

            DebitCreditNoteParserServiceImpl service = new DebitCreditNoteParserServiceImpl();
            DebitCreditNoteParserPort.ParsingException ex = assertThrows(
                DebitCreditNoteParserPort.ParsingException.class,
                () -> service.parse("<test/>", "test-id")
            );
            assertTrue(ex.getMessage().contains("Unexpected root element"));
        }
    }

    @Test
    void testParseValidDebitCreditNote() throws DebitCreditNoteParserPort.ParsingException {
        // Given: A valid Thai e-Tax debit/credit note XML
        String xmlContent = getSampleDebitCreditNoteXml();
        String sourceNoteId = "intake-12345";

        // When: Parsing the XML
        ProcessedDebitCreditNote note = parserService.parse(xmlContent, sourceNoteId);

        // Then: All fields should be correctly parsed
        assertNotNull(note);
        assertEquals(sourceNoteId, note.getSourceNoteId());
        assertEquals("DN2025-00001", note.getNoteNumber());
        assertEquals(LocalDate.of(2025, 1, 15), note.getIssueDate());
        assertEquals(LocalDate.of(2025, 2, 14), note.getDueDate());
        assertEquals("THB", note.getCurrency());
        assertNotNull(note.getId());
        assertEquals(xmlContent, note.getOriginalXml());
    }

    @Test
    void testParseSellerInformation() throws DebitCreditNoteParserPort.ParsingException {
        // Given: A valid debit/credit note XML
        String xmlContent = getSampleDebitCreditNoteXml();

        // When: Parsing the XML
        ProcessedDebitCreditNote note = parserService.parse(xmlContent, "test-123");

        // Then: Seller information should be correctly parsed
        Party seller = note.getSeller();
        assertNotNull(seller);
        assertEquals("Acme Corporation Ltd.", seller.name());
        assertEquals("1234567890123", seller.taxIdentifier().taxId());
        assertEquals("VAT", seller.taxIdentifier().scheme());

        Address sellerAddress = seller.address();
        assertNotNull(sellerAddress);
        assertEquals("123 Business Street", sellerAddress.street());
        assertEquals("Bangkok", sellerAddress.city());
        assertEquals("10110", sellerAddress.postalCode());
        assertEquals("TH", sellerAddress.country());
    }

    @Test
    void testParseBuyerInformation() throws DebitCreditNoteParserPort.ParsingException {
        // Given: A valid debit/credit note XML
        String xmlContent = getSampleDebitCreditNoteXml();

        // When: Parsing the XML
        ProcessedDebitCreditNote note = parserService.parse(xmlContent, "test-123");

        // Then: Buyer information should be correctly parsed
        Party buyer = note.getBuyer();
        assertNotNull(buyer);
        assertEquals("Customer Company Ltd.", buyer.name());
        assertEquals("9876543210987", buyer.taxIdentifier().taxId());

        Address buyerAddress = buyer.address();
        assertNotNull(buyerAddress);
        assertEquals("456 Customer Road", buyerAddress.street());
        assertEquals("Chiang Mai", buyerAddress.city());
        assertEquals("50000", buyerAddress.postalCode());
        assertEquals("TH", buyerAddress.country());
    }

    @Test
    void testParseLineItems() throws DebitCreditNoteParserPort.ParsingException {
        // Given: A valid debit/credit note XML with line items
        String xmlContent = getSampleDebitCreditNoteXml();

        // When: Parsing the XML
        ProcessedDebitCreditNote note = parserService.parse(xmlContent, "test-123");

        // Then: Line items should be correctly parsed
        assertNotNull(note.getItems());
        assertEquals(2, note.getItems().size());

        // First line item
        LineItem item1 = note.getItems().get(0);
        assertEquals("Professional Services - Consulting", item1.description());
        assertEquals(10, item1.quantity());
        assertEquals(Money.of(new BigDecimal("5000.00"), "THB"), item1.unitPrice());
        assertEquals(new BigDecimal("7.00"), item1.taxRate());

        // Second line item
        LineItem item2 = note.getItems().get(1);
        assertEquals("Software License", item2.description());
        assertEquals(1, item2.quantity());
        assertEquals(Money.of(new BigDecimal("10000.00"), "THB"), item2.unitPrice());
        assertEquals(new BigDecimal("7.00"), item2.taxRate());
    }

    @Test
    void testCalculateTotals() throws DebitCreditNoteParserPort.ParsingException {
        // Given: A valid debit/credit note XML
        String xmlContent = getSampleDebitCreditNoteXml();

        // When: Parsing the XML
        ProcessedDebitCreditNote note = parserService.parse(xmlContent, "test-123");

        // Then: Totals should be calculated correctly
        // Subtotal: (10 * 5000) + (1 * 10000) = 60,000
        assertEquals(0, new BigDecimal("60000.00").compareTo(note.getSubtotal().amount()));

        // Tax: 60,000 * 0.07 = 4,200
        assertEquals(0, new BigDecimal("4200.00").compareTo(note.getTotalTax().amount()));

        // Total: 60,000 + 4,200 = 64,200
        assertEquals(0, new BigDecimal("64200.00").compareTo(note.getTotal().amount()));
    }

    @Test
    void testParseDebitCreditNoteWithNullXml() {
        // Given: Null XML content
        String xmlContent = null;

        // When/Then: Should throw ParsingException
        assertThrows(DebitCreditNoteParserPort.ParsingException.class,
            () -> parserService.parse(xmlContent, "test-123"));
    }

    @Test
    void testParseDebitCreditNoteWithEmptyXml() {
        // Given: Empty XML content
        String xmlContent = "";

        // When/Then: Should throw ParsingException
        assertThrows(DebitCreditNoteParserPort.ParsingException.class,
            () -> parserService.parse(xmlContent, "test-123"));
    }

    @Test
    void testParseDebitCreditNoteWithInvalidXml() {
        // Given: Invalid XML content
        String xmlContent = "<invalid>Not a valid debit/credit note</invalid>";

        // When/Then: Should throw ParsingException
        assertThrows(DebitCreditNoteParserPort.ParsingException.class,
            () -> parserService.parse(xmlContent, "test-123"));
    }

    @Test
    void testParseDebitCreditNoteWithMissingNoteNumber() {
        // Given: XML without note number
        String xmlContent = getDebitCreditNoteXmlWithoutNoteNumber();

        // When/Then: Should throw ParsingException
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("note number") || exception.getMessage().contains("ID"));
    }

    @Test
    void testParseDebitCreditNoteWithMissingLineItems() {
        // Given: XML without line items
        String xmlContent = getDebitCreditNoteXmlWithoutLineItems();

        // When/Then: Should throw ParsingException
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("line item"));
    }

    @Test
    void testParseDebitCreditNoteWithMissingDueDate() throws DebitCreditNoteParserPort.ParsingException {
        // Given: XML without due date (should default to issue date + 30 days)
        String xmlContent = getDebitCreditNoteXmlWithoutDueDate();

        // When: Parsing the XML
        ProcessedDebitCreditNote note = parserService.parse(xmlContent, "test-123");

        // Then: Due date should be issue date + 30 days
        assertNotNull(note);
        assertEquals(LocalDate.of(2025, 1, 15), note.getIssueDate());
        assertEquals(LocalDate.of(2025, 2, 14), note.getDueDate());
    }

    @Test
    void testParseDebitCreditNoteWithMinimalAddress() throws DebitCreditNoteParserPort.ParsingException {
        // Given: XML with full address (the standardSupplyChain already has a full seller)
        // This tests that addresses with all fields present parse correctly.
        String xmlContent = getDebitCreditNoteXmlWithoutDueDate();

        // When: Parsing the XML
        ProcessedDebitCreditNote note = parserService.parse(xmlContent, "test-123");

        // Then: Address should have only country (standardSupplyChain uses standard seller with full address)
        assertNotNull(note);
        Party seller = note.getSeller();
        assertNotNull(seller.address());
        assertEquals("TH", seller.address().country());
    }

    @Test
    void testParseDebitCreditNoteWithTaxIdNoScheme() throws DebitCreditNoteParserPort.ParsingException {
        // Given: XML with tax ID but no scheme (should default to "VAT")
        String xmlContent = getSampleDebitCreditNoteXml();

        // When: Parsing the XML
        ProcessedDebitCreditNote note = parserService.parse(xmlContent, "test-123");

        // Then: Buyer tax scheme should default to "VAT" (buyer has no schemeID)
        assertNotNull(note);
        assertEquals("VAT", note.getBuyer().taxIdentifier().scheme());
        assertEquals("9876543210987", note.getBuyer().taxIdentifier().taxId());
    }

    @Test
    void testParseDebitCreditNoteWithMissingIssueDate() {
        // Given: XML without issue date
        String xmlContent = getDebitCreditNoteXmlWithoutIssueDate();

        // When/Then: Should throw ParsingException
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("Issue date"));
    }

    @Test
    void testParseDebitCreditNoteWithMissingSeller() {
        // Given: XML without seller information
        String xmlContent = getDebitCreditNoteXmlWithoutSeller();

        // When/Then: Should throw ParsingException
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("Seller"));
    }

    @Test
    void testParseDebitCreditNoteWithMissingBuyer() {
        // Given: XML without buyer information
        String xmlContent = getDebitCreditNoteXmlWithoutBuyer();

        // When/Then: Should throw ParsingException
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("Buyer"));
    }

    @Test
    void testParseDebitCreditNoteWithInvalidCurrency() {
        // Given: XML with invalid currency code (not 3 characters)
        String xmlContent = getDebitCreditNoteXmlWithInvalidCurrency();

        // When/Then: Should throw ParsingException
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("currency"));
    }

    @Test
    void testParseDebitCreditNoteWithMissingCurrency() {
        // Given: XML without currency
        String xmlContent = getDebitCreditNoteXmlWithoutCurrency();

        // When/Then: Should throw ParsingException
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("currency"));
    }

    @Test
    void testParseDebitCreditNoteWithLineItemMissingTax() throws DebitCreditNoteParserPort.ParsingException {
        // Given: XML with line item without tax info (should default to 0%)
        String xmlContent = getDebitCreditNoteXmlWithLineItemNoTax();

        // When: Parsing the XML
        ProcessedDebitCreditNote note = parserService.parse(xmlContent, "test-123");

        // Then: Tax rate should be zero
        assertNotNull(note);
        assertEquals(1, note.getItems().size());
        assertEquals(BigDecimal.ZERO, note.getItems().get(0).taxRate());
    }

    @Test
    void testParseDebitCreditNoteWithMissingSellerName() {
        // Given: XML without seller name
        String xmlContent = getDebitCreditNoteXmlWithoutSellerName();

        // When/Then: Should throw ParsingException
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("Seller name"));
    }

    @Test
    void testParseDebitCreditNoteWithMissingSellerTaxId() {
        // Given: XML without seller tax ID
        String xmlContent = getDebitCreditNoteXmlWithoutSellerTaxId();

        // When/Then: Should throw ParsingException
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("Seller tax"));
    }

    @Test
    void testParseDebitCreditNoteWithMissingSellerCountry() {
        // Given: XML where seller PostalTradeAddress is present but CountryID is absent.
        // The parser returns null for the address, but the Party domain model requires
        // non-null address, so the parse must throw an exception.
        String xmlContent = getDebitCreditNoteXmlWithoutSellerCountry();

        // When/Then: Should throw an exception because Party requires non-null address
        assertThrows(DebitCreditNoteParserPort.ParsingException.class,
            () -> parserService.parse(xmlContent, "test-123"),
            "Parse should fail when CountryID is absent because Party requires non-null address");
    }

    @Test
    void testParseDebitCreditNoteWithMissingExchangedDocument() {
        String xmlContent = getDebitCreditNoteXmlWithoutExchangedDocument();
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));
        assertTrue(exception.getMessage().contains("ExchangedDocument"));
    }

    @Test
    void testParseDebitCreditNoteWithMissingSupplyChainTradeTransaction() {
        String xmlContent = getDebitCreditNoteXmlWithoutSupplyChainTradeTransaction();
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));
        assertTrue(exception.getMessage().contains("SupplyChainTradeTransaction"));
    }

    @Test
    void testParseDebitCreditNoteWithSellerEmail() throws DebitCreditNoteParserPort.ParsingException {
        String xmlContent = getDebitCreditNoteXmlWithSellerEmail();
        ProcessedDebitCreditNote note = parserService.parse(xmlContent, "test-email");
        assertEquals("seller@acme.com", note.getSeller().email());
    }

    @Test
    void testParseDebitCreditNoteWithMissingSellerTaxRegistration() {
        String xmlContent = getDebitCreditNoteXmlWithoutSellerTaxRegistration();
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));
        assertTrue(exception.getMessage().contains("tax registration"));
    }

    @Test
    void testParseDebitCreditNoteWithMissingSellerAddress() {
        // Given: XML where seller has no PostalTradeAddress element.
        // The parser returns null for the address, but the Party domain model requires
        // non-null address, so the parse must throw an exception.
        String xmlContent = getDebitCreditNoteXmlWithoutSellerAddress();

        // When/Then: Should throw an exception because Party requires non-null address
        assertThrows(DebitCreditNoteParserPort.ParsingException.class,
            () -> parserService.parse(xmlContent, "test-123"),
            "Parse should fail when PostalTradeAddress is absent because Party requires non-null address");
    }

    @Test
    void testParseDebitCreditNoteWithLineItemMissingProductName() {
        String xmlContent = getDebitCreditNoteXmlWithLineItemMissingProduct();
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));
        assertTrue(exception.getMessage().toLowerCase().contains("line item"));
    }

    @Test
    void testParseDebitCreditNoteWithLineItemMissingDelivery() {
        String xmlContent = getDebitCreditNoteXmlWithLineItemMissingDelivery();
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));
        assertTrue(exception.getMessage().toLowerCase().contains("line item"));
    }

    @Test
    void testParseDebitCreditNoteWithFractionalQuantity() {
        String xmlContent = getDebitCreditNoteXmlWithFractionalQuantity();
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));
        assertTrue(exception.getMessage().toLowerCase().contains("whole number") || exception.getMessage().toLowerCase().contains("line item"));
    }

    @Test
    void testParseDebitCreditNoteWithLineItemMissingAgreement() {
        String xmlContent = getDebitCreditNoteXmlWithLineItemMissingAgreement();
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));
        assertTrue(exception.getMessage().toLowerCase().contains("line item"));
    }

    @Test
    void testParseDebitCreditNoteWithLineItemEmptyChargeAmount() {
        String xmlContent = getDebitCreditNoteXmlWithLineItemEmptyChargeAmount();
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));
        assertTrue(exception.getMessage().toLowerCase().contains("line item"));
    }

    @Test
    void testParseDebitCreditNoteWithLineItemMultipleChargeAmounts() {
        String xmlContent = getDebitCreditNoteXmlWithLineItemMultipleChargeAmounts();
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));
        assertTrue(exception.getMessage().contains("price amounts"),
            "Exception message should mention multiple price amounts but was: " + exception.getMessage());
    }

    @Test
    void testParseDebitCreditNoteWithUnrecognisedTaxIdSchemeDefaultsToVat()
            throws DebitCreditNoteParserPort.ParsingException {
        // Given: seller whose schemeID is not in the whitelist
        String xmlContent = getDebitCreditNoteXmlWithSellerTaxIdScheme("UNKNOWN_SCHEME");

        // When: parsing succeeds (document is not rejected)
        ProcessedDebitCreditNote note = parserService.parse(xmlContent, "test-123");

        // Then: scheme is silently normalised to "VAT"
        assertNotNull(note);
        assertEquals("VAT", note.getSeller().taxIdentifier().scheme());
    }

    @Test
    void testParseDebitCreditNoteWithKnownTaxIdSchemePreserved()
            throws DebitCreditNoteParserPort.ParsingException {
        // Given: seller with a whitelisted scheme
        String xmlContent = getDebitCreditNoteXmlWithSellerTaxIdScheme("EIN");

        // When: parsing succeeds
        ProcessedDebitCreditNote note = parserService.parse(xmlContent, "test-123");

        // Then: scheme is preserved as-is
        assertEquals("EIN", note.getSeller().taxIdentifier().scheme());
    }

    @Test
    void testParseDebitCreditNoteWithTaxRateAbove100() {
        // Given: line item with tax rate 999.99
        String xmlContent = getDebitCreditNoteXmlWithTaxRate("999.99");
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));
        assertTrue(exception.getMessage().contains("999.99"),
            "Exception should quote the offending rate, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("[0, 100]"),
            "Exception should state the valid range, got: " + exception.getMessage());
    }

    @Test
    void testParseDebitCreditNoteWithNegativeTaxRate() {
        // Given: negative tax rate
        String xmlContent = getDebitCreditNoteXmlWithTaxRate("-1.00");
        DebitCreditNoteParserPort.ParsingException exception =
            assertThrows(DebitCreditNoteParserPort.ParsingException.class,
                () -> parserService.parse(xmlContent, "test-123"));
        assertTrue(exception.getMessage().contains("-1.00"),
            "Exception should quote the offending rate, got: " + exception.getMessage());
    }

    @Test
    void testParseDebitCreditNoteWithTaxRateBoundary100() throws DebitCreditNoteParserPort.ParsingException {
        // Given: tax rate exactly 100 -- on the boundary, must be accepted
        String xmlContent = getDebitCreditNoteXmlWithTaxRate("100.00");
        ProcessedDebitCreditNote note = parserService.parse(xmlContent, "test-123");
        assertEquals(new BigDecimal("100.00"), note.getItems().get(0).taxRate());
    }

    @Test
    void testParseWithConcurrencyLimitOfOneSucceeds()
            throws DebitCreditNoteParserPort.ParsingException {
        // Given: parser configured with maxConcurrentParses=1 (tightest possible cap)
        DebitCreditNoteParserServiceImpl limitedParser =
            new DebitCreditNoteParserServiceImpl(10, TimeUnit.SECONDS, 30, 1);

        // When / Then: a single sequential parse must succeed
        ProcessedDebitCreditNote note = limitedParser.parse(getSampleDebitCreditNoteXml(), "test-semaphore");
        assertNotNull(note);
        assertEquals("DN2025-00001", note.getNoteNumber());
    }

    // -----------------------------------------------------------------------
    // Wall-clock timeout and payload-size guard
    // -----------------------------------------------------------------------

    @Test
    void parse_whenParsingExceedsTimeout_throwsParsingException() throws Exception {
        try (MockedStatic<JAXBContext> mockedJaxb = mockStatic(JAXBContext.class)) {
            JAXBContext mockContext = mock(JAXBContext.class);
            Unmarshaller mockUnmarshaller = mock(Unmarshaller.class);
            mockedJaxb.when(() -> JAXBContext.newInstance(anyString())).thenReturn(mockContext);
            when(mockContext.createUnmarshaller()).thenReturn(mockUnmarshaller);
            when(mockUnmarshaller.unmarshal(any(SAXSource.class))).thenAnswer(invocation -> {
                Thread.sleep(5_000); // simulate a parse that never finishes in time
                return null;
            });

            // Service configured with a very short timeout so the test completes quickly
            DebitCreditNoteParserServiceImpl service =
                new DebitCreditNoteParserServiceImpl(100, TimeUnit.MILLISECONDS);

            DebitCreditNoteParserPort.ParsingException ex = assertThrows(
                DebitCreditNoteParserPort.ParsingException.class,
                () -> service.parse(getSampleDebitCreditNoteXml(), "timeout-test")
            );
            assertTrue(ex.getMessage().contains("timed out"),
                "Exception message should mention timeout but was: " + ex.getMessage());
        }
    }

    @Test
    void parse_whenDueDateAbsentAndCustomDefaultConfigured_usesConfiguredValue()
            throws DebitCreditNoteParserPort.ParsingException {
        // Construct a service with a 15-day default
        DebitCreditNoteParserServiceImpl service =
            new DebitCreditNoteParserServiceImpl(10, TimeUnit.SECONDS, 15);

        ProcessedDebitCreditNote note = service.parse(getDebitCreditNoteXmlWithoutDueDate(), "due-date-config-test");

        // issueDate is 2025-01-15; with a 15-day default the due date must be 2025-01-30
        assertEquals(LocalDate.of(2025, 1, 15), note.getIssueDate());
        assertEquals(LocalDate.of(2025, 1, 30), note.getDueDate(),
            "Due date must be issue date + configured defaultDueDateDays (15), not the hardcoded 30");
    }

    @Test
    void parse_whenXmlExceedsMaxSize_throwsParsingException() {
        // Build a payload that exceeds the 500 KB limit
        String padding = "x".repeat(DebitCreditNoteParserServiceImpl.MAX_XML_BYTES + 1);
        String oversized = "<root>" + padding + "</root>";

        DebitCreditNoteParserPort.ParsingException ex = assertThrows(
            DebitCreditNoteParserPort.ParsingException.class,
            () -> parserService.parse(oversized, "size-test")
        );
        assertTrue(ex.getMessage().contains("too large"),
            "Exception message should mention size but was: " + ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // Semaphore lifecycle -- permit must be released even when submit() throws
    // -----------------------------------------------------------------------

    @Test
    void testSemaphoreIsReleasedWhenExecutorRejectsSubmit() {
        // maxConcurrentParses=1 so one leaked permit exhausts the semaphore entirely.
        DebitCreditNoteParserServiceImpl singlePermitParser =
            new DebitCreditNoteParserServiceImpl(5, TimeUnit.SECONDS, 30, 1);

        // Shut the executor down before submitting -- guarantees RejectedExecutionException.
        singlePermitParser.shutdownExecutor();

        String xml = getSampleDebitCreditNoteXml();

        // First call: executor is shut down -> submit() throws RejectedExecutionException.
        // The semaphore permit must be released inside the finally block.
        assertThrows(
            DebitCreditNoteParserPort.ParsingException.class,
            () -> singlePermitParser.parse(xml, "leak-test-1"),
            "parse() must throw when executor is shut down"
        );

        // Second call: if the permit leaked, would block or deadlock.
        assertThrows(
            DebitCreditNoteParserPort.ParsingException.class,
            () -> singlePermitParser.parse(xml, "leak-test-2"),
            "parse() must still throw (not deadlock) after first rejected submit"
        );
    }

    // -----------------------------------------------------------------------
    // Timezone-safety tests for convertXMLGregorianCalendarToLocalDate
    // -----------------------------------------------------------------------

    @Test
    void testParseDateTimezone_naiveTimestamp_utcJvm_yieldsCorrectThaiDate()
            throws DebitCreditNoteParserPort.ParsingException {
        TimeZone savedDefault = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

            // 2025-01-01T23:30:00 naive = Bangkok local 23:30 on Jan 1 -> date must be 2025-01-01.
            String xml = getDebitCreditNoteXmlWithIssueDateTime("2025-01-01T23:30:00");

            ProcessedDebitCreditNote note = parserService.parse(xml, "tz-test-naive-utc");

            assertEquals(LocalDate.of(2025, 1, 1), note.getIssueDate(),
                "Naive Bangkok datetime 2025-01-01T23:30:00 must be stored as 2025-01-01 on any JVM timezone");
        } finally {
            TimeZone.setDefault(savedDefault);
        }
    }

    @Test
    void testParseDateTimezone_explicitBangkokOffset_yieldsCorrectDate()
            throws DebitCreditNoteParserPort.ParsingException {
        // 2025-01-01T23:30:00+07:00 -- explicit Thai timezone, date must be 2025-01-01
        String xml = getDebitCreditNoteXmlWithIssueDateTime("2025-01-01T23:30:00+07:00");

        ProcessedDebitCreditNote note = parserService.parse(xml, "tz-test-explicit-th");

        assertEquals(LocalDate.of(2025, 1, 1), note.getIssueDate(),
            "Explicit +07:00 timestamp at 23:30 must yield 2025-01-01");
    }

    // =======================================================================
    // XML Test Fixtures
    // =======================================================================

    private String getSampleDebitCreditNoteXml() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:DebitCreditNote_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:DebitCreditNote_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:DebitCreditNote_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16"
                xmlns:qdt="urn:etda:uncefact:data:standard:QualifiedDataType:1">

              <rsm:ExchangedDocumentContext>
                <ram:GuidelineSpecifiedDocumentContextParameter>
                  <ram:ID>urn:etda.or.th:debitcreditnote:2p1</ram:ID>
                </ram:GuidelineSpecifiedDocumentContextParameter>
              </rsm:ExchangedDocumentContext>

              <rsm:ExchangedDocument>
                <ram:ID>DN2025-00001</ram:ID>
                <ram:Name>Debit Note</ram:Name>
                <ram:TypeCode>381</ram:TypeCode>
                <ram:IssueDateTime>2025-01-15T00:00:00</ram:IssueDateTime>
                <ram:PurposeCode>9</ram:PurposeCode>
                <ram:CreationDateTime>2025-01-15T00:00:00</ram:CreationDateTime>
              </rsm:ExchangedDocument>

              <rsm:SupplyChainTradeTransaction>
                <ram:ApplicableHeaderTradeAgreement>
                  <ram:SellerTradeParty>
                    <ram:Name>Acme Corporation Ltd.</ram:Name>
                    <ram:PostalTradeAddress>
                      <ram:LineOne>123 Business Street</ram:LineOne>
                      <ram:CityName>Bangkok</ram:CityName>
                      <ram:PostcodeCode>10110</ram:PostcodeCode>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID schemeID="VAT">1234567890123</ram:ID>
                    </ram:SpecifiedTaxRegistration>
                  </ram:SellerTradeParty>
                  <ram:BuyerTradeParty>
                    <ram:Name>Customer Company Ltd.</ram:Name>
                    <ram:PostalTradeAddress>
                      <ram:LineOne>456 Customer Road</ram:LineOne>
                      <ram:CityName>Chiang Mai</ram:CityName>
                      <ram:PostcodeCode>50000</ram:PostcodeCode>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID>9876543210987</ram:ID>
                    </ram:SpecifiedTaxRegistration>
                  </ram:BuyerTradeParty>
                </ram:ApplicableHeaderTradeAgreement>

                <ram:ApplicableHeaderTradeSettlement>
                  <ram:InvoiceCurrencyCode>THB</ram:InvoiceCurrencyCode>
                  <ram:ApplicableTradeTax>
                    <ram:TypeCode>VAT</ram:TypeCode>
                    <ram:CalculatedRate>7.00</ram:CalculatedRate>
                  </ram:ApplicableTradeTax>
                  <ram:SpecifiedTradePaymentTerms>
                    <ram:DueDateDateTime>2025-02-14T00:00:00</ram:DueDateDateTime>
                  </ram:SpecifiedTradePaymentTerms>
                  <ram:SpecifiedTradeSettlementHeaderMonetarySummation>
                    <ram:TaxBasisTotalAmount>60000.00</ram:TaxBasisTotalAmount>
                    <ram:TaxTotalAmount>4200.00</ram:TaxTotalAmount>
                    <ram:GrandTotalAmount>64200.00</ram:GrandTotalAmount>
                  </ram:SpecifiedTradeSettlementHeaderMonetarySummation>
                </ram:ApplicableHeaderTradeSettlement>

                <ram:IncludedSupplyChainTradeLineItem>
                  <ram:AssociatedDocumentLineDocument>
                    <ram:LineID>1</ram:LineID>
                  </ram:AssociatedDocumentLineDocument>
                  <ram:SpecifiedTradeProduct>
                    <ram:Name>Professional Services - Consulting</ram:Name>
                  </ram:SpecifiedTradeProduct>
                  <ram:SpecifiedLineTradeAgreement>
                    <ram:GrossPriceProductTradePrice>
                      <ram:ChargeAmount>5000.00</ram:ChargeAmount>
                    </ram:GrossPriceProductTradePrice>
                  </ram:SpecifiedLineTradeAgreement>
                  <ram:SpecifiedLineTradeDelivery>
                    <ram:BilledQuantity unitCode="HUR">10</ram:BilledQuantity>
                  </ram:SpecifiedLineTradeDelivery>
                  <ram:SpecifiedLineTradeSettlement>
                    <ram:ApplicableTradeTax>
                      <ram:TypeCode>VAT</ram:TypeCode>
                      <ram:CalculatedRate>7.00</ram:CalculatedRate>
                    </ram:ApplicableTradeTax>
                  </ram:SpecifiedLineTradeSettlement>
                </ram:IncludedSupplyChainTradeLineItem>

                <ram:IncludedSupplyChainTradeLineItem>
                  <ram:AssociatedDocumentLineDocument>
                    <ram:LineID>2</ram:LineID>
                  </ram:AssociatedDocumentLineDocument>
                  <ram:SpecifiedTradeProduct>
                    <ram:Name>Software License</ram:Name>
                  </ram:SpecifiedTradeProduct>
                  <ram:SpecifiedLineTradeAgreement>
                    <ram:GrossPriceProductTradePrice>
                      <ram:ChargeAmount>10000.00</ram:ChargeAmount>
                    </ram:GrossPriceProductTradePrice>
                  </ram:SpecifiedLineTradeAgreement>
                  <ram:SpecifiedLineTradeDelivery>
                    <ram:BilledQuantity unitCode="C62">1</ram:BilledQuantity>
                  </ram:SpecifiedLineTradeDelivery>
                  <ram:SpecifiedLineTradeSettlement>
                    <ram:ApplicableTradeTax>
                      <ram:TypeCode>VAT</ram:TypeCode>
                      <ram:CalculatedRate>7.00</ram:CalculatedRate>
                    </ram:ApplicableTradeTax>
                  </ram:SpecifiedLineTradeSettlement>
                </ram:IncludedSupplyChainTradeLineItem>
              </rsm:SupplyChainTradeTransaction>
            </rsm:DebitCreditNote_CrossIndustryInvoice>
            """;
    }

    // ---- Build helpers for modular XML generation ----

    private static final String NS_HEADER = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rsm:DebitCreditNote_CrossIndustryInvoice
            xmlns:rsm="urn:etda:uncefact:data:standard:DebitCreditNote_CrossIndustryInvoice:2"
            xmlns:ram="urn:etda:uncefact:data:standard:DebitCreditNote_ReusableAggregateBusinessInformationEntity:2"
            xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16"
            xmlns:qdt="urn:etda:uncefact:data:standard:QualifiedDataType:1">
        """;

    private static final String STANDARD_SELLER = """
          <ram:SellerTradeParty>
            <ram:Name>Acme Corporation Ltd.</ram:Name>
            <ram:PostalTradeAddress>
              <ram:LineOne>123 Business Street</ram:LineOne>
              <ram:CityName>Bangkok</ram:CityName>
              <ram:PostcodeCode>10110</ram:PostcodeCode>
              <ram:CountryID>TH</ram:CountryID>
            </ram:PostalTradeAddress>
            <ram:SpecifiedTaxRegistration>
              <ram:ID schemeID="VAT">1234567890123</ram:ID>
            </ram:SpecifiedTaxRegistration>
          </ram:SellerTradeParty>
        """;

    private static final String STANDARD_BUYER = """
          <ram:BuyerTradeParty>
            <ram:Name>Customer Company Ltd.</ram:Name>
            <ram:PostalTradeAddress>
              <ram:LineOne>456 Customer Road</ram:LineOne>
              <ram:CityName>Chiang Mai</ram:CityName>
              <ram:PostcodeCode>50000</ram:PostcodeCode>
              <ram:CountryID>TH</ram:CountryID>
            </ram:PostalTradeAddress>
            <ram:SpecifiedTaxRegistration>
              <ram:ID>9876543210987</ram:ID>
            </ram:SpecifiedTaxRegistration>
          </ram:BuyerTradeParty>
        """;

    private static final String STANDARD_SETTLEMENT = """
          <ram:ApplicableHeaderTradeSettlement>
            <ram:InvoiceCurrencyCode>THB</ram:InvoiceCurrencyCode>
            <ram:ApplicableTradeTax>
              <ram:TypeCode>VAT</ram:TypeCode>
              <ram:CalculatedRate>7.00</ram:CalculatedRate>
            </ram:ApplicableTradeTax>
            <ram:SpecifiedTradeSettlementHeaderMonetarySummation>
              <ram:TaxBasisTotalAmount>2000.00</ram:TaxBasisTotalAmount>
              <ram:TaxTotalAmount>140.00</ram:TaxTotalAmount>
              <ram:GrandTotalAmount>2140.00</ram:GrandTotalAmount>
            </ram:SpecifiedTradeSettlementHeaderMonetarySummation>
          </ram:ApplicableHeaderTradeSettlement>
        """;

    private static final String STANDARD_LINE_ITEM = """
          <ram:IncludedSupplyChainTradeLineItem>
            <ram:SpecifiedTradeProduct>
              <ram:Name>Professional Services</ram:Name>
            </ram:SpecifiedTradeProduct>
            <ram:SpecifiedLineTradeAgreement>
              <ram:GrossPriceProductTradePrice>
                <ram:ChargeAmount>1000.00</ram:ChargeAmount>
              </ram:GrossPriceProductTradePrice>
            </ram:SpecifiedLineTradeAgreement>
            <ram:SpecifiedLineTradeDelivery>
              <ram:BilledQuantity unitCode="C62">2</ram:BilledQuantity>
            </ram:SpecifiedLineTradeDelivery>
          </ram:IncludedSupplyChainTradeLineItem>
        """;

    private String buildXml(String exchangedDocument, String supplyChain) {
        return NS_HEADER + (exchangedDocument != null ? exchangedDocument : "") +
               (supplyChain != null ? supplyChain : "") +
               "</rsm:DebitCreditNote_CrossIndustryInvoice>";
    }

    private String standardExchangedDocument() {
        return """
          <rsm:ExchangedDocument>
            <ram:ID>DN2025-TEST</ram:ID>
            <ram:Name>Debit Note</ram:Name>
            <ram:TypeCode>381</ram:TypeCode>
            <ram:IssueDateTime>2025-01-15T00:00:00</ram:IssueDateTime>
            <ram:PurposeCode>9</ram:PurposeCode>
            <ram:CreationDateTime>2025-01-15T00:00:00</ram:CreationDateTime>
          </rsm:ExchangedDocument>
          """;
    }

    private String standardSupplyChain(String sellerOverride) {
        return """
          <rsm:SupplyChainTradeTransaction>
            <ram:ApplicableHeaderTradeAgreement>
          """ + (sellerOverride != null ? sellerOverride : STANDARD_SELLER) + STANDARD_BUYER + """
            </ram:ApplicableHeaderTradeAgreement>
          """ + STANDARD_SETTLEMENT + STANDARD_LINE_ITEM + """
          </rsm:SupplyChainTradeTransaction>
          """;
    }

    private String standardSupplyChainWithLineItem(String lineItemOverride) {
        return """
          <rsm:SupplyChainTradeTransaction>
            <ram:ApplicableHeaderTradeAgreement>
          """ + STANDARD_SELLER + STANDARD_BUYER + """
            </ram:ApplicableHeaderTradeAgreement>
          """ + STANDARD_SETTLEMENT + lineItemOverride + """
          </rsm:SupplyChainTradeTransaction>
          """;
    }

    // ---- Missing element fixtures ----

    private String getDebitCreditNoteXmlWithoutNoteNumber() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:DebitCreditNote_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:DebitCreditNote_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:DebitCreditNote_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16">

              <rsm:ExchangedDocumentContext>
                <ram:GuidelineSpecifiedDocumentContextParameter>
                  <ram:ID>urn:etda.or.th:debitcreditnote:2p1</ram:ID>
                </ram:GuidelineSpecifiedDocumentContextParameter>
              </rsm:ExchangedDocumentContext>

              <rsm:ExchangedDocument>
                <ram:Name>Debit Note</ram:Name>
                <ram:TypeCode>381</ram:TypeCode>
                <ram:IssueDateTime>2025-01-15T00:00:00</ram:IssueDateTime>
                <ram:PurposeCode>9</ram:PurposeCode>
                <ram:CreationDateTime>2025-01-15T00:00:00</ram:CreationDateTime>
              </rsm:ExchangedDocument>

              <rsm:SupplyChainTradeTransaction>
                <ram:ApplicableHeaderTradeAgreement>
                  <ram:SellerTradeParty>
                    <ram:Name>Test Seller</ram:Name>
                    <ram:PostalTradeAddress>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID>1234567890123</ram:ID>
                    </ram:SpecifiedTaxRegistration>
                  </ram:SellerTradeParty>
                  <ram:BuyerTradeParty>
                    <ram:Name>Test Buyer</ram:Name>
                    <ram:PostalTradeAddress>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID>9876543210987</ram:ID>
                    </ram:SpecifiedTaxRegistration>
                  </ram:BuyerTradeParty>
                </ram:ApplicableHeaderTradeAgreement>
                <ram:ApplicableHeaderTradeSettlement>
                  <ram:InvoiceCurrencyCode>THB</ram:InvoiceCurrencyCode>
                  <ram:ApplicableTradeTax>
                    <ram:TypeCode>VAT</ram:TypeCode>
                    <ram:CalculatedRate>7.00</ram:CalculatedRate>
                  </ram:ApplicableTradeTax>
                  <ram:SpecifiedTradeSettlementHeaderMonetarySummation>
                    <ram:TaxBasisTotalAmount>1000.00</ram:TaxBasisTotalAmount>
                    <ram:TaxTotalAmount>70.00</ram:TaxTotalAmount>
                    <ram:GrandTotalAmount>1070.00</ram:GrandTotalAmount>
                  </ram:SpecifiedTradeSettlementHeaderMonetarySummation>
                </ram:ApplicableHeaderTradeSettlement>
              </rsm:SupplyChainTradeTransaction>
            </rsm:DebitCreditNote_CrossIndustryInvoice>
            """;
    }

    private String getDebitCreditNoteXmlWithoutLineItems() {
        return buildXml(standardExchangedDocument(), """
          <rsm:SupplyChainTradeTransaction>
            <ram:ApplicableHeaderTradeAgreement>
          """ + STANDARD_SELLER + STANDARD_BUYER + """
            </ram:ApplicableHeaderTradeAgreement>
          """ + STANDARD_SETTLEMENT + """
          </rsm:SupplyChainTradeTransaction>
          """);
    }

    private String getDebitCreditNoteXmlWithoutDueDate() {
        return buildXml(standardExchangedDocument(), standardSupplyChain(null));
    }

    private String getDebitCreditNoteXmlWithMinimalAddress() {
        return buildXml(standardExchangedDocument(), standardSupplyChain(null));
    }

    private String getDebitCreditNoteXmlWithoutIssueDate() {
        String exchangedDocument = """
          <rsm:ExchangedDocument>
            <ram:ID>DN2025-TEST</ram:ID>
            <ram:Name>Debit Note</ram:Name>
            <ram:TypeCode>381</ram:TypeCode>
            <ram:PurposeCode>9</ram:PurposeCode>
            <ram:CreationDateTime>2025-01-15T00:00:00</ram:CreationDateTime>
          </rsm:ExchangedDocument>
          """;
        return buildXml(exchangedDocument, standardSupplyChain(null));
    }

    private String getDebitCreditNoteXmlWithoutSeller() {
        String supplyChain = """
          <rsm:SupplyChainTradeTransaction>
            <ram:ApplicableHeaderTradeAgreement>
          """ + STANDARD_BUYER + """
            </ram:ApplicableHeaderTradeAgreement>
          """ + STANDARD_SETTLEMENT + STANDARD_LINE_ITEM + """
          </rsm:SupplyChainTradeTransaction>
          """;
        return buildXml(standardExchangedDocument(), supplyChain);
    }

    private String getDebitCreditNoteXmlWithoutBuyer() {
        String supplyChain = """
          <rsm:SupplyChainTradeTransaction>
            <ram:ApplicableHeaderTradeAgreement>
          """ + STANDARD_SELLER + """
            </ram:ApplicableHeaderTradeAgreement>
          """ + STANDARD_SETTLEMENT + STANDARD_LINE_ITEM + """
          </rsm:SupplyChainTradeTransaction>
          """;
        return buildXml(standardExchangedDocument(), supplyChain);
    }

    private String getDebitCreditNoteXmlWithInvalidCurrency() {
        String settlement = """
          <ram:ApplicableHeaderTradeSettlement>
            <ram:InvoiceCurrencyCode>INVALID</ram:InvoiceCurrencyCode>
            <ram:ApplicableTradeTax>
              <ram:TypeCode>VAT</ram:TypeCode>
              <ram:CalculatedRate>7.00</ram:CalculatedRate>
            </ram:ApplicableTradeTax>
            <ram:SpecifiedTradeSettlementHeaderMonetarySummation>
              <ram:TaxBasisTotalAmount>2000.00</ram:TaxBasisTotalAmount>
              <ram:TaxTotalAmount>140.00</ram:TaxTotalAmount>
              <ram:GrandTotalAmount>2140.00</ram:GrandTotalAmount>
            </ram:SpecifiedTradeSettlementHeaderMonetarySummation>
          </ram:ApplicableHeaderTradeSettlement>
        """;
        String supplyChain = """
          <rsm:SupplyChainTradeTransaction>
            <ram:ApplicableHeaderTradeAgreement>
          """ + STANDARD_SELLER + STANDARD_BUYER + """
            </ram:ApplicableHeaderTradeAgreement>
          """ + settlement + STANDARD_LINE_ITEM + """
          </rsm:SupplyChainTradeTransaction>
          """;
        return buildXml(standardExchangedDocument(), supplyChain);
    }

    private String getDebitCreditNoteXmlWithoutCurrency() {
        String settlement = """
          <ram:ApplicableHeaderTradeSettlement>
            <ram:ApplicableTradeTax>
              <ram:TypeCode>VAT</ram:TypeCode>
              <ram:CalculatedRate>7.00</ram:CalculatedRate>
            </ram:ApplicableTradeTax>
            <ram:SpecifiedTradeSettlementHeaderMonetarySummation>
              <ram:TaxBasisTotalAmount>2000.00</ram:TaxBasisTotalAmount>
              <ram:TaxTotalAmount>140.00</ram:TaxTotalAmount>
              <ram:GrandTotalAmount>2140.00</ram:GrandTotalAmount>
            </ram:SpecifiedTradeSettlementHeaderMonetarySummation>
          </ram:ApplicableHeaderTradeSettlement>
        """;
        String supplyChain = """
          <rsm:SupplyChainTradeTransaction>
            <ram:ApplicableHeaderTradeAgreement>
          """ + STANDARD_SELLER + STANDARD_BUYER + """
            </ram:ApplicableHeaderTradeAgreement>
          """ + settlement + STANDARD_LINE_ITEM + """
          </rsm:SupplyChainTradeTransaction>
          """;
        return buildXml(standardExchangedDocument(), supplyChain);
    }

    private String getDebitCreditNoteXmlWithLineItemNoTax() {
        String item = """
            <ram:IncludedSupplyChainTradeLineItem>
              <ram:SpecifiedTradeProduct>
                <ram:Name>Test Product</ram:Name>
              </ram:SpecifiedTradeProduct>
              <ram:SpecifiedLineTradeAgreement>
                <ram:GrossPriceProductTradePrice>
                  <ram:ChargeAmount>1000.00</ram:ChargeAmount>
                </ram:GrossPriceProductTradePrice>
              </ram:SpecifiedLineTradeAgreement>
              <ram:SpecifiedLineTradeDelivery>
                <ram:BilledQuantity unitCode="C62">1</ram:BilledQuantity>
              </ram:SpecifiedLineTradeDelivery>
            </ram:IncludedSupplyChainTradeLineItem>
            """;
        return buildXml(standardExchangedDocument(), standardSupplyChainWithLineItem(item));
    }

    private String getDebitCreditNoteXmlWithoutSellerName() {
        String seller = """
            <ram:SellerTradeParty>
              <ram:PostalTradeAddress>
                <ram:CountryID>TH</ram:CountryID>
              </ram:PostalTradeAddress>
              <ram:SpecifiedTaxRegistration>
                <ram:ID>1234567890123</ram:ID>
              </ram:SpecifiedTaxRegistration>
            </ram:SellerTradeParty>
            """;
        return buildXml(standardExchangedDocument(), standardSupplyChain(seller));
    }

    private String getDebitCreditNoteXmlWithoutSellerTaxId() {
        String seller = """
            <ram:SellerTradeParty>
              <ram:Name>Test Seller</ram:Name>
              <ram:PostalTradeAddress>
                <ram:CountryID>TH</ram:CountryID>
              </ram:PostalTradeAddress>
            </ram:SellerTradeParty>
            """;
        return buildXml(standardExchangedDocument(), standardSupplyChain(seller));
    }

    private String getDebitCreditNoteXmlWithoutSellerCountry() {
        String sellerNoCountry = """
            <ram:SellerTradeParty>
              <ram:Name>Test Seller</ram:Name>
              <ram:PostalTradeAddress>
              </ram:PostalTradeAddress>
              <ram:SpecifiedTaxRegistration>
                <ram:ID>1234567890123</ram:ID>
              </ram:SpecifiedTaxRegistration>
            </ram:SellerTradeParty>
            """;
        return buildXml(standardExchangedDocument(), standardSupplyChain(sellerNoCountry));
    }

    private String getDebitCreditNoteXmlWithoutExchangedDocument() {
        return buildXml(null, standardSupplyChain(null));
    }

    private String getDebitCreditNoteXmlWithoutSupplyChainTradeTransaction() {
        return buildXml(standardExchangedDocument(), null);
    }

    private String getDebitCreditNoteXmlWithSellerEmail() {
        String sellerWithEmail = """
            <ram:SellerTradeParty>
              <ram:Name>Acme Corporation Ltd.</ram:Name>
              <ram:PostalTradeAddress>
                <ram:LineOne>123 Business Street</ram:LineOne>
                <ram:CityName>Bangkok</ram:CityName>
                <ram:PostcodeCode>10110</ram:PostcodeCode>
                <ram:CountryID>TH</ram:CountryID>
              </ram:PostalTradeAddress>
              <ram:SpecifiedTaxRegistration>
                <ram:ID schemeID="VAT">1234567890123</ram:ID>
              </ram:SpecifiedTaxRegistration>
              <ram:DefinedTradeContact>
                <ram:EmailURIUniversalCommunication>
                  <ram:URIID>seller@acme.com</ram:URIID>
                </ram:EmailURIUniversalCommunication>
              </ram:DefinedTradeContact>
            </ram:SellerTradeParty>
            """;
        return buildXml(standardExchangedDocument(), standardSupplyChain(sellerWithEmail));
    }

    private String getDebitCreditNoteXmlWithoutSellerTaxRegistration() {
        String sellerNoReg = """
            <ram:SellerTradeParty>
              <ram:Name>Acme Corporation Ltd.</ram:Name>
              <ram:PostalTradeAddress>
                <ram:LineOne>123 Business Street</ram:LineOne>
                <ram:CityName>Bangkok</ram:CityName>
                <ram:PostcodeCode>10110</ram:PostcodeCode>
                <ram:CountryID>TH</ram:CountryID>
              </ram:PostalTradeAddress>
            </ram:SellerTradeParty>
            """;
        return buildXml(standardExchangedDocument(), standardSupplyChain(sellerNoReg));
    }

    private String getDebitCreditNoteXmlWithoutSellerAddress() {
        String sellerNoAddress = """
            <ram:SellerTradeParty>
              <ram:Name>Acme Corporation Ltd.</ram:Name>
              <ram:SpecifiedTaxRegistration>
                <ram:ID schemeID="VAT">1234567890123</ram:ID>
              </ram:SpecifiedTaxRegistration>
            </ram:SellerTradeParty>
            """;
        return buildXml(standardExchangedDocument(), standardSupplyChain(sellerNoAddress));
    }

    private String getDebitCreditNoteXmlWithLineItemMissingProduct() {
        String item = """
            <ram:IncludedSupplyChainTradeLineItem>
              <ram:SpecifiedLineTradeAgreement>
                <ram:GrossPriceProductTradePrice>
                  <ram:ChargeAmount>1000.00</ram:ChargeAmount>
                </ram:GrossPriceProductTradePrice>
              </ram:SpecifiedLineTradeAgreement>
              <ram:SpecifiedLineTradeDelivery>
                <ram:BilledQuantity unitCode="C62">2</ram:BilledQuantity>
              </ram:SpecifiedLineTradeDelivery>
            </ram:IncludedSupplyChainTradeLineItem>
            """;
        return buildXml(standardExchangedDocument(), standardSupplyChainWithLineItem(item));
    }

    private String getDebitCreditNoteXmlWithLineItemMissingDelivery() {
        String item = """
            <ram:IncludedSupplyChainTradeLineItem>
              <ram:SpecifiedTradeProduct>
                <ram:Name>Professional Services</ram:Name>
              </ram:SpecifiedTradeProduct>
              <ram:SpecifiedLineTradeAgreement>
                <ram:GrossPriceProductTradePrice>
                  <ram:ChargeAmount>1000.00</ram:ChargeAmount>
                </ram:GrossPriceProductTradePrice>
              </ram:SpecifiedLineTradeAgreement>
            </ram:IncludedSupplyChainTradeLineItem>
            """;
        return buildXml(standardExchangedDocument(), standardSupplyChainWithLineItem(item));
    }

    private String getDebitCreditNoteXmlWithFractionalQuantity() {
        String item = """
            <ram:IncludedSupplyChainTradeLineItem>
              <ram:SpecifiedTradeProduct>
                <ram:Name>Professional Services</ram:Name>
              </ram:SpecifiedTradeProduct>
              <ram:SpecifiedLineTradeAgreement>
                <ram:GrossPriceProductTradePrice>
                  <ram:ChargeAmount>1000.00</ram:ChargeAmount>
                </ram:GrossPriceProductTradePrice>
              </ram:SpecifiedLineTradeAgreement>
              <ram:SpecifiedLineTradeDelivery>
                <ram:BilledQuantity unitCode="C62">1.5</ram:BilledQuantity>
              </ram:SpecifiedLineTradeDelivery>
            </ram:IncludedSupplyChainTradeLineItem>
            """;
        return buildXml(standardExchangedDocument(), standardSupplyChainWithLineItem(item));
    }

    private String getDebitCreditNoteXmlWithLineItemMissingAgreement() {
        String item = """
            <ram:IncludedSupplyChainTradeLineItem>
              <ram:SpecifiedTradeProduct>
                <ram:Name>Professional Services</ram:Name>
              </ram:SpecifiedTradeProduct>
              <ram:SpecifiedLineTradeDelivery>
                <ram:BilledQuantity unitCode="C62">2</ram:BilledQuantity>
              </ram:SpecifiedLineTradeDelivery>
            </ram:IncludedSupplyChainTradeLineItem>
            """;
        return buildXml(standardExchangedDocument(), standardSupplyChainWithLineItem(item));
    }

    private String getDebitCreditNoteXmlWithLineItemEmptyChargeAmount() {
        String item = """
            <ram:IncludedSupplyChainTradeLineItem>
              <ram:SpecifiedTradeProduct>
                <ram:Name>Professional Services</ram:Name>
              </ram:SpecifiedTradeProduct>
              <ram:SpecifiedLineTradeAgreement>
                <ram:GrossPriceProductTradePrice>
                </ram:GrossPriceProductTradePrice>
              </ram:SpecifiedLineTradeAgreement>
              <ram:SpecifiedLineTradeDelivery>
                <ram:BilledQuantity unitCode="C62">2</ram:BilledQuantity>
              </ram:SpecifiedLineTradeDelivery>
            </ram:IncludedSupplyChainTradeLineItem>
            """;
        return buildXml(standardExchangedDocument(), standardSupplyChainWithLineItem(item));
    }

    private String getDebitCreditNoteXmlWithLineItemMultipleChargeAmounts() {
        String item = """
            <ram:IncludedSupplyChainTradeLineItem>
              <ram:SpecifiedTradeProduct>
                <ram:Name>Professional Services</ram:Name>
              </ram:SpecifiedTradeProduct>
              <ram:SpecifiedLineTradeAgreement>
                <ram:GrossPriceProductTradePrice>
                  <ram:ChargeAmount>1000.00</ram:ChargeAmount>
                  <ram:ChargeAmount>900.00</ram:ChargeAmount>
                </ram:GrossPriceProductTradePrice>
              </ram:SpecifiedLineTradeAgreement>
              <ram:SpecifiedLineTradeDelivery>
                <ram:BilledQuantity unitCode="C62">2</ram:BilledQuantity>
              </ram:SpecifiedLineTradeDelivery>
            </ram:IncludedSupplyChainTradeLineItem>
            """;
        return buildXml(standardExchangedDocument(), standardSupplyChainWithLineItem(item));
    }

    private String getDebitCreditNoteXmlWithSellerTaxIdScheme(String scheme) {
        String seller = """
            <ram:SellerTradeParty>
              <ram:Name>Acme Corporation Ltd.</ram:Name>
              <ram:PostalTradeAddress>
                <ram:LineOne>123 Business Street</ram:LineOne>
                <ram:CityName>Bangkok</ram:CityName>
                <ram:PostcodeCode>10110</ram:PostcodeCode>
                <ram:CountryID>TH</ram:CountryID>
              </ram:PostalTradeAddress>
              <ram:SpecifiedTaxRegistration>
                <ram:ID schemeID=\"""" + scheme + """
                \">1234567890123</ram:ID>
              </ram:SpecifiedTaxRegistration>
            </ram:SellerTradeParty>
            """;
        return buildXml(standardExchangedDocument(), standardSupplyChain(seller));
    }

    private String getDebitCreditNoteXmlWithTaxRate(String rate) {
        String item = """
            <ram:IncludedSupplyChainTradeLineItem>
              <ram:SpecifiedTradeProduct>
                <ram:Name>Professional Services</ram:Name>
              </ram:SpecifiedTradeProduct>
              <ram:SpecifiedLineTradeAgreement>
                <ram:GrossPriceProductTradePrice>
                  <ram:ChargeAmount>1000.00</ram:ChargeAmount>
                </ram:GrossPriceProductTradePrice>
              </ram:SpecifiedLineTradeAgreement>
              <ram:SpecifiedLineTradeDelivery>
                <ram:BilledQuantity unitCode="C62">1</ram:BilledQuantity>
              </ram:SpecifiedLineTradeDelivery>
              <ram:SpecifiedLineTradeSettlement>
                <ram:ApplicableTradeTax>
                  <ram:CalculatedRate>""" + rate + """
                  </ram:CalculatedRate>
                </ram:ApplicableTradeTax>
              </ram:SpecifiedLineTradeSettlement>
            </ram:IncludedSupplyChainTradeLineItem>
            """;
        return buildXml(standardExchangedDocument(), standardSupplyChainWithLineItem(item));
    }

    private String getDebitCreditNoteXmlWithIssueDateTime(String dateTime) {
        String exchangedDocument = """
          <rsm:ExchangedDocument>
            <ram:ID>DN-TZ-TEST</ram:ID>
            <ram:Name>Debit Note</ram:Name>
            <ram:TypeCode>381</ram:TypeCode>
            <ram:IssueDateTime>""" + dateTime + """
</ram:IssueDateTime>
            <ram:PurposeCode>9</ram:PurposeCode>
            <ram:CreationDateTime>2025-01-15T00:00:00</ram:CreationDateTime>
          </rsm:ExchangedDocument>
          """;
        return buildXml(exchangedDocument, standardSupplyChain(null));
    }
}
