package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.parsing;

import com.wpanther.debitcreditnote.processing.domain.model.Address;
import com.wpanther.debitcreditnote.processing.domain.model.DebitCreditNoteId;
import com.wpanther.debitcreditnote.processing.domain.model.LineItem;
import com.wpanther.debitcreditnote.processing.domain.model.Money;
import com.wpanther.debitcreditnote.processing.domain.model.Party;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessedDebitCreditNote;
import com.wpanther.debitcreditnote.processing.domain.model.TaxIdentifier;
import com.wpanther.debitcreditnote.processing.domain.port.out.DebitCreditNoteParserPort;
import com.wpanther.etax.generated.debitcreditnote.ram.EmailUniversalCommunicationType;
import com.wpanther.etax.generated.debitcreditnote.ram.ExchangedDocumentType;
import com.wpanther.etax.generated.debitcreditnote.ram.HeaderTradeAgreementType;
import com.wpanther.etax.generated.debitcreditnote.ram.HeaderTradeSettlementType;
import com.wpanther.etax.generated.debitcreditnote.ram.LineTradeAgreementType;
import com.wpanther.etax.generated.debitcreditnote.ram.LineTradeDeliveryType;
import com.wpanther.etax.generated.debitcreditnote.ram.LineTradeSettlementType;
import com.wpanther.etax.generated.debitcreditnote.ram.SpecifiedTaxRegistrationType;
import com.wpanther.etax.generated.debitcreditnote.ram.SupplyChainTradeLineItemType;
import com.wpanther.etax.generated.debitcreditnote.ram.SupplyChainTradeTransactionType;
import com.wpanther.etax.generated.debitcreditnote.ram.TradeAddressType;
import com.wpanther.etax.generated.debitcreditnote.ram.TradeContactType;
import com.wpanther.etax.generated.debitcreditnote.ram.TradePartyType;
import com.wpanther.etax.generated.debitcreditnote.ram.TradePaymentTermsType;
import com.wpanther.etax.generated.debitcreditnote.ram.TradePriceType;
import com.wpanther.etax.generated.debitcreditnote.ram.TradeProductType;
import com.wpanther.etax.generated.debitcreditnote.ram.TradeTaxType;
import com.wpanther.etax.generated.debitcreditnote.rsm.DebitCreditNote_CrossIndustryInvoiceType;
import jakarta.annotation.PreDestroy;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of DebitCreditNoteParserPort that uses teda library's JAXB classes
 * to parse Thai e-Tax debit/credit note XML.
 *
 * <p>Two safety guards are applied before JAXB touches the bytes:
 * <ol>
 *   <li><b>Size check</b> -- rejects payloads larger than {@value #MAX_XML_BYTES} bytes
 *       (UTF-8) to prevent memory exhaustion from deliberately oversized inputs.</li>
 *   <li><b>Wall-clock timeout</b> -- the JAXB unmarshal runs in a dedicated virtual-thread
 *       executor; if parsing has not finished within {@code app.parsing.timeout-seconds}
 *       (default 10 s) the task is cancelled and a {@link ParsingException}
 *       is thrown.  This prevents a pathological XML structure from blocking a Camel
 *       consumer thread indefinitely even when entity-expansion caps are in place.</li>
 * </ol>
 */
@Slf4j
@Service
public class DebitCreditNoteParserServiceImpl implements DebitCreditNoteParserPort {

    private static final ZoneId TH_ZONE = ZoneId.of("Asia/Bangkok");

    /** Maximum accepted XML payload size (UTF-8 bytes). */
    static final int MAX_XML_BYTES = 500 * 1024; // 500 KB

    /**
     * Recognised tax identifier scheme codes per Thai e-Tax specification.
     * An unrecognised schemeID is logged and silently replaced with "VAT" rather
     * than rejecting the document, because the scheme is metadata that does not
     * affect tax calculations.
     */
    private static final Set<String> VALID_TAX_ID_SCHEMES = Set.of("VAT", "EIN", "TAX");

    private final JAXBContext jaxbContext;
    private final SAXParserFactory saxParserFactory;
    private final long parseTimeoutMs;

    /**
     * Number of days added to the issue date when the XML omits a due date.
     * Configured via {@code app.debitcreditnote.default-due-date-days} (default 30).
     */
    private final int defaultDueDateDays;

    /**
     * Dedicated virtual-thread executor for timed JAXB unmarshal operations.
     */
    private final ExecutorService parseExecutor;

    /**
     * Caps the number of concurrent JAXB unmarshal tasks.
     * Configured via {@code app.parsing.max-concurrent} (default: 300).
     */
    private final Semaphore parseSemaphore;

    // ---- Constructors -------------------------------------------------------

    /**
     * Production constructor -- Spring resolves all configurable values and injects them.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public DebitCreditNoteParserServiceImpl(
            @Value("${app.parsing.timeout-seconds:10}") int parseTimeoutSeconds,
            @Value("${app.parsing.max-concurrent:300}") int maxConcurrentParses,
            @Value("${app.debitcreditnote.default-due-date-days:30}") int defaultDueDateDays) {
        this(TimeUnit.SECONDS.toMillis(parseTimeoutSeconds), defaultDueDateDays, maxConcurrentParses);
    }

    /**
     * Package-private constructor for unit tests that need fine-grained timeout control
     * (e.g. 100 ms instead of 10 s) without a Spring context.
     */
    DebitCreditNoteParserServiceImpl(long timeout, TimeUnit unit) {
        this(unit.toMillis(timeout), 30, Integer.MAX_VALUE);
    }

    /**
     * Package-private constructor for unit tests that need both a custom timeout
     * and a custom due-date default.
     */
    DebitCreditNoteParserServiceImpl(long timeout, TimeUnit unit, int defaultDueDateDays) {
        this(unit.toMillis(timeout), defaultDueDateDays, Integer.MAX_VALUE);
    }

    /**
     * Package-private constructor for tests that need to exercise the concurrency cap.
     */
    DebitCreditNoteParserServiceImpl(long timeout, TimeUnit unit, int defaultDueDateDays, int maxConcurrentParses) {
        this(unit.toMillis(timeout), defaultDueDateDays, maxConcurrentParses);
    }

    /**
     * No-arg constructor kept for existing test classes that call
     * {@code new DebitCreditNoteParserServiceImpl()} directly.
     */
    DebitCreditNoteParserServiceImpl() {
        this(TimeUnit.SECONDS.toMillis(10), 30, Integer.MAX_VALUE);
    }

    private DebitCreditNoteParserServiceImpl(long parseTimeoutMs, int defaultDueDateDays, int maxConcurrentParses) {
        this.parseTimeoutMs = parseTimeoutMs;
        this.defaultDueDateDays = defaultDueDateDays;
        this.parseSemaphore = new Semaphore(maxConcurrentParses);
        this.parseExecutor = Executors.newVirtualThreadPerTaskExecutor();

        try {
            // CRITICAL: must use debitcreditnote packages, NOT taxinvoice packages.
            String contextPath = "com.wpanther.etax.generated.debitcreditnote.rsm.impl" +
                               ":com.wpanther.etax.generated.debitcreditnote.ram.impl" +
                               ":com.wpanther.etax.generated.common.qdt.impl" +
                               ":com.wpanther.etax.generated.common.udt.impl";
            this.jaxbContext = JAXBContext.newInstance(contextPath);
            log.info("JAXB context initialized successfully for Thai e-Tax debit/credit note parsing");
        } catch (JAXBException e) {
            log.error("Failed to initialize JAXB context", e);
            throw new IllegalStateException("Failed to initialize XML parser", e);
        }

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            this.saxParserFactory = factory;
        } catch (ParserConfigurationException | org.xml.sax.SAXException e) {
            throw new IllegalStateException("Failed to initialize secure SAX parser factory", e);
        }
    }

    @PreDestroy
    public void shutdownExecutor() {
        parseExecutor.shutdown();
    }

    // ---- Public API ---------------------------------------------------------

    @Override
    public ProcessedDebitCreditNote parse(String xmlContent, String sourceNoteId)
            throws ParsingException {

        log.debug("Starting XML parsing for source note ID: {}", sourceNoteId);

        try {
            // Step 1: Unmarshal XML to JAXB object (size-checked and time-bounded)
            DebitCreditNote_CrossIndustryInvoiceType jaxbNote = unmarshalXml(xmlContent);

            // Step 2: Extract note components
            ExchangedDocumentType document = jaxbNote.getExchangedDocument();
            if (document == null) {
                throw new ParsingException(
                    "Debit/Credit note XML missing required ExchangedDocument element");
            }

            SupplyChainTradeTransactionType transaction = jaxbNote.getSupplyChainTradeTransaction();
            if (transaction == null) {
                throw new ParsingException(
                    "Debit/Credit note XML missing required SupplyChainTradeTransaction element");
            }

            // Step 3: Map to domain model
            LocalDate issueDate = extractIssueDate(document);
            String currency = extractCurrency(transaction);

            ProcessedDebitCreditNote note = ProcessedDebitCreditNote.builder()
                .id(DebitCreditNoteId.generate())
                .sourceNoteId(sourceNoteId)
                .noteNumber(extractNoteNumber(document))
                .noteType(extractNoteType(document))
                .issueDate(issueDate)
                .dueDate(extractDueDate(transaction, issueDate))
                .seller(extractSeller(transaction))
                .buyer(extractBuyer(transaction))
                .items(extractLineItems(transaction, currency))
                .currency(currency)
                .originalXml(xmlContent)
                .build();

            log.info("Successfully parsed debit/credit note {} with {} line items",
                note.getNoteNumber(), note.getItems().size());

            return note;

        } catch (ParsingException e) {
            log.error("Failed to parse debit/credit note XML for source ID {}: {}",
                sourceNoteId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error parsing debit/credit note XML for source ID {}", sourceNoteId, e);
            throw new ParsingException("Unexpected error during debit/credit note parsing: " + e.getMessage(), e);
        }
    }

    // ---- Unmarshal (size-check + timeout) -----------------------------------

    /**
     * Validates the XML payload size and delegates the actual JAXB unmarshal to
     * {@link #doUnmarshal(String)}, which runs inside a time-bounded executor task.
     */
    private DebitCreditNote_CrossIndustryInvoiceType unmarshalXml(String xmlContent)
            throws ParsingException {

        if (xmlContent == null || xmlContent.isBlank()) {
            throw ParsingException.forEmpty();
        }

        int byteSize = xmlContent.getBytes(StandardCharsets.UTF_8).length;
        if (byteSize > MAX_XML_BYTES) {
            throw ParsingException.forOversized(byteSize, MAX_XML_BYTES);
        }

        try {
            parseSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ParsingException.forInterrupted();
        }

        try {
            Future<DebitCreditNote_CrossIndustryInvoiceType> future;
            try {
                future = parseExecutor.submit(() -> doUnmarshal(xmlContent));
            } catch (java.util.concurrent.RejectedExecutionException e) {
                throw ParsingException.forUnmarshal(e);
            }
            try {
                return future.get(parseTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw ParsingException.forTimeout(parseTimeoutMs);
            } catch (ExecutionException e) {
                future.cancel(true);
                Throwable cause = e.getCause();
                if (cause instanceof ParsingException ex) {
                    throw ex;
                }
                throw ParsingException.forUnmarshal(cause);
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw ParsingException.forInterrupted();
            }
        } finally {
            parseSemaphore.release();
        }
    }

    /**
     * Performs the actual JAXB unmarshal.  Runs inside an executor task submitted
     * by {@link #unmarshalXml(String)} so that it can be cancelled if it takes too long.
     */
    private DebitCreditNote_CrossIndustryInvoiceType doUnmarshal(String xmlContent)
            throws ParsingException {

        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            Object result;
            try (StringReader reader = new StringReader(xmlContent)) {
                org.xml.sax.XMLReader xmlReader = saxParserFactory.newSAXParser().getXMLReader();
                xmlReader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                SAXSource saxSource = new SAXSource(xmlReader, new InputSource(reader));
                result = unmarshaller.unmarshal(saxSource);
            }

            if (result instanceof jakarta.xml.bind.JAXBElement) {
                result = ((jakarta.xml.bind.JAXBElement<?>) result).getValue();
            }

            if (!(result instanceof DebitCreditNote_CrossIndustryInvoiceType)) {
                throw ParsingException.forUnexpectedRootElement(result.getClass().getName());
            }

            return (DebitCreditNote_CrossIndustryInvoiceType) result;

        } catch (JAXBException | org.xml.sax.SAXException | ParserConfigurationException e) {
            log.error("JAXB unmarshalling failed", e);
            throw ParsingException.forUnmarshal(e);
        }
    }

    // ---- Domain extraction helpers ------------------------------------------

    private String extractNoteNumber(ExchangedDocumentType document)
            throws ParsingException {

        if (document.getID() == null || document.getID().getValue() == null) {
            throw new ParsingException("Debit/Credit note number (ID) is missing");
        }
        return document.getID().getValue();
    }

    private String extractNoteType(ExchangedDocumentType document) {

        if (document.getTypeCode() == null) {
            return "UNKNOWN";
        }
        return document.getTypeCode().getValue();
    }

    private LocalDate extractIssueDate(ExchangedDocumentType document)
            throws ParsingException {

        XMLGregorianCalendar issueDateTime = document.getIssueDateTime();
        if (issueDateTime == null) {
            throw new ParsingException("Issue date/time is missing");
        }
        return convertXMLGregorianCalendarToLocalDate(issueDateTime);
    }

    private LocalDate extractDueDate(SupplyChainTradeTransactionType transaction, LocalDate issueDate)
            throws ParsingException {

        HeaderTradeSettlementType settlement = transaction.getApplicableHeaderTradeSettlement();
        if (settlement == null) {
            throw new ParsingException("Trade settlement information is missing");
        }

        List<TradePaymentTermsType> paymentTerms = settlement.getSpecifiedTradePaymentTerms();
        if (paymentTerms != null && !paymentTerms.isEmpty()) {
            XMLGregorianCalendar dueDateTime = paymentTerms.get(0).getDueDateDateTime();
            if (dueDateTime != null) {
                return convertXMLGregorianCalendarToLocalDate(dueDateTime);
            }
        }

        log.warn("Due date not found in XML, defaulting to issue date + {} days", defaultDueDateDays);
        return issueDate.plusDays(defaultDueDateDays);
    }

    private Party extractSeller(SupplyChainTradeTransactionType transaction)
            throws ParsingException {

        HeaderTradeAgreementType agreement = transaction.getApplicableHeaderTradeAgreement();
        if (agreement == null || agreement.getSellerTradeParty() == null) {
            throw new ParsingException("Seller information is missing");
        }
        return mapParty(agreement.getSellerTradeParty(), "Seller");
    }

    private Party extractBuyer(SupplyChainTradeTransactionType transaction)
            throws ParsingException {

        HeaderTradeAgreementType agreement = transaction.getApplicableHeaderTradeAgreement();
        if (agreement == null || agreement.getBuyerTradeParty() == null) {
            throw new ParsingException("Buyer information is missing");
        }
        return mapParty(agreement.getBuyerTradeParty(), "Buyer");
    }

    private Party mapParty(TradePartyType jaxbParty, String partyType)
            throws ParsingException {

        String name = Optional.ofNullable(jaxbParty.getName())
            .orElseThrow(() -> new ParsingException(partyType + " name is missing"));

        TaxIdentifier taxIdentifier = extractTaxIdentifier(jaxbParty, partyType);
        Address address = extractAddress(jaxbParty, partyType);

        String email = null;
        List<TradeContactType> contacts = jaxbParty.getDefinedTradeContact();
        if (contacts != null && !contacts.isEmpty()) {
            TradeContactType contact = contacts.get(0);
            EmailUniversalCommunicationType emailComm = contact.getEmailURIUniversalCommunication();
            if (emailComm != null && emailComm.getURIID() != null) {
                email = emailComm.getURIID().getValue();
            }
        }

        return Party.of(name, taxIdentifier, address, email);
    }

    private TaxIdentifier extractTaxIdentifier(TradePartyType jaxbParty, String partyType)
            throws ParsingException {

        SpecifiedTaxRegistrationType taxReg = jaxbParty.getSpecifiedTaxRegistration();
        if (taxReg == null) {
            throw new ParsingException(partyType + " tax registration is missing");
        }
        if (taxReg.getID() == null || taxReg.getID().getValue() == null) {
            throw new ParsingException(partyType + " tax ID is missing");
        }

        String taxId = taxReg.getID().getValue();
        String scheme = Optional.ofNullable(taxReg.getID().getSchemeID()).orElse("VAT");
        if (!VALID_TAX_ID_SCHEMES.contains(scheme)) {
            log.warn("{} tax identifier scheme '{}' is not recognised -- defaulting to VAT",
                    partyType, scheme);
            scheme = "VAT";
        }
        return TaxIdentifier.of(taxId, scheme);
    }

    /**
     * Extract address from party.
     *
     * <p>Returns {@code null} when {@code PostalTradeAddress} is absent or its
     * {@code CountryID} is missing -- both are permitted by the Thai e-Tax XSD.
     */
    private Address extractAddress(TradePartyType jaxbParty, String partyType) {

        TradeAddressType jaxbAddress = jaxbParty.getPostalTradeAddress();
        if (jaxbAddress == null) {
            log.warn("{} PostalTradeAddress element is absent -- party address will be null "
                + "(optional per Thai e-Tax XSD)", partyType);
            return null;
        }

        String streetAddress = Optional.ofNullable(jaxbAddress.getLineOne())
            .map(line -> line.getValue()).orElse(null);
        String city = Optional.ofNullable(jaxbAddress.getCityName())
            .map(name -> name.getValue()).orElse(null);
        String postalCode = Optional.ofNullable(jaxbAddress.getPostcodeCode())
            .map(code -> code.getValue()).orElse(null);

        String country = null;
        if (jaxbAddress.getCountryID() != null && jaxbAddress.getCountryID().getValue() != null) {
            country = jaxbAddress.getCountryID().getValue().value();
        }
        if (country == null) {
            log.warn("{} PostalTradeAddress present but CountryID is absent -- party address will be null "
                + "(optional per Thai e-Tax XSD). Discarded: street='{}', city='{}', postalCode='{}'",
                partyType, streetAddress, city, postalCode);
            return null;
        }

        return Address.of(streetAddress, city, postalCode, country);
    }

    private List<LineItem> extractLineItems(SupplyChainTradeTransactionType transaction, String currency)
            throws ParsingException {

        List<SupplyChainTradeLineItemType> jaxbItems =
            transaction.getIncludedSupplyChainTradeLineItem();

        if (jaxbItems == null || jaxbItems.isEmpty()) {
            throw new ParsingException("Debit/Credit note must have at least one line item");
        }

        List<LineItem> items = new ArrayList<>();
        for (SupplyChainTradeLineItemType jaxbItem : jaxbItems) {
            items.add(mapLineItem(jaxbItem, currency));
        }
        return items;
    }

    private LineItem mapLineItem(SupplyChainTradeLineItemType jaxbItem, String currency)
            throws ParsingException {

        // Product description
        TradeProductType product = jaxbItem.getSpecifiedTradeProduct();
        if (product == null || product.getName() == null || product.getName().isEmpty()) {
            throw new ParsingException("Line item product name is missing");
        }
        String description = product.getName().get(0).getValue();

        // Quantity
        LineTradeDeliveryType delivery = jaxbItem.getSpecifiedLineTradeDelivery();
        if (delivery == null || delivery.getBilledQuantity() == null) {
            throw new ParsingException("Line item quantity is missing");
        }
        BigDecimal quantityDecimal = delivery.getBilledQuantity().getValue();
        if (quantityDecimal.stripTrailingZeros().scale() > 0) {
            throw new ParsingException(
                "Line item quantity must be a whole number, got: " + quantityDecimal);
        }
        int quantity;
        try {
            quantity = quantityDecimal.intValueExact();
        } catch (ArithmeticException e) {
            throw new ParsingException(
                "Line item quantity out of integer range: " + quantityDecimal, e);
        }

        // Unit price
        LineTradeAgreementType agreement = jaxbItem.getSpecifiedLineTradeAgreement();
        if (agreement == null || agreement.getGrossPriceProductTradePrice() == null) {
            throw new ParsingException("Line item unit price is missing");
        }
        TradePriceType priceType = agreement.getGrossPriceProductTradePrice();
        if (priceType.getChargeAmount() == null || priceType.getChargeAmount().isEmpty()) {
            throw new ParsingException("Line item price amount is missing");
        }
        if (priceType.getChargeAmount().size() > 1) {
            throw new ParsingException(
                "Line item has " + priceType.getChargeAmount().size() + " price amounts; "
                + "exactly one ChargeAmount is expected per GrossPriceProductTradePrice");
        }
        Money unitPrice = Money.of(priceType.getChargeAmount().get(0).getValue(), currency);

        // Tax rate (optional -- defaults to 0%)
        LineTradeSettlementType settlement = jaxbItem.getSpecifiedLineTradeSettlement();
        BigDecimal taxRate = BigDecimal.ZERO;
        if (settlement != null && settlement.getApplicableTradeTax() != null
                && !settlement.getApplicableTradeTax().isEmpty()) {
            TradeTaxType tax = settlement.getApplicableTradeTax().get(0);
            if (tax.getCalculatedRate() != null) {
                taxRate = tax.getCalculatedRate();
                if (taxRate.compareTo(BigDecimal.ZERO) < 0
                        || taxRate.compareTo(new BigDecimal("100")) > 0) {
                    throw new ParsingException(
                        "Tax rate " + taxRate + " is outside valid range [0, 100]");
                }
            }
        }

        return new LineItem(description, quantity, unitPrice, taxRate);
    }

    private String extractCurrency(SupplyChainTradeTransactionType transaction)
            throws ParsingException {

        HeaderTradeSettlementType settlement = transaction.getApplicableHeaderTradeSettlement();
        if (settlement == null || settlement.getInvoiceCurrencyCode() == null) {
            throw new ParsingException("Debit/Credit note currency is missing");
        }

        String currency = null;
        if (settlement.getInvoiceCurrencyCode().getValue() != null) {
            currency = settlement.getInvoiceCurrencyCode().getValue().value();
        }
        if (currency == null || currency.length() != 3) {
            throw new ParsingException("Invalid currency code: " + currency);
        }
        return currency;
    }

    /**
     * Convert XMLGregorianCalendar to LocalDate using Thai timezone (UTC+7).
     *
     * <p>Two cases:
     * <ol>
     *   <li><b>Timezone undefined</b> ({@link DatatypeConstants#FIELD_UNDEFINED}) -- the XML
     *       contains a bare datetime with no offset, e.g. {@code 2025-01-01T23:30:00}.
     *       Per the ETDA Thai e-Tax standard, bare datetimes represent Bangkok local time.
     *       The calendar fields are therefore extracted directly.</li>
     *   <li><b>Timezone present</b> -- normalise the absolute instant to Bangkok time and
     *       extract the local date.</li>
     * </ol>
     */
    private LocalDate convertXMLGregorianCalendarToLocalDate(XMLGregorianCalendar calendar) {
        if (calendar.getTimezone() == DatatypeConstants.FIELD_UNDEFINED) {
            return LocalDate.of(calendar.getYear(), calendar.getMonth(), calendar.getDay());
        }
        return calendar.toGregorianCalendar().toZonedDateTime()
            .withZoneSameInstant(TH_ZONE)
            .toLocalDate();
    }
}
