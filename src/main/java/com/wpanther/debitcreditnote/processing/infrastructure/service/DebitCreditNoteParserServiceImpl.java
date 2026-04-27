package com.wpanther.debitcreditnote.processing.infrastructure.service;

import com.wpanther.debitcreditnote.processing.domain.model.*;
import com.wpanther.debitcreditnote.processing.domain.service.DebitCreditNoteParserService;
import com.wpanther.etax.generated.common.qdt.ThaiInvoiceDocumentCodeType;
import com.wpanther.etax.generated.debitcreditnote.ram.*;
import com.wpanther.etax.generated.debitcreditnote.rsm.DebitCreditNote_CrossIndustryInvoiceType;
import com.wpanther.etax.generated.debitcreditnote.rsm.impl.DebitCreditNote_CrossIndustryInvoiceTypeImpl;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DebitCreditNoteParserServiceImpl implements DebitCreditNoteParserService {

    private final JAXBContext jaxbContext;

    public DebitCreditNoteParserServiceImpl() throws DebitCreditNoteParsingException {
        try {
            String contextPath = "com.wpanther.etax.generated.debitcreditnote.rsm.impl" +
                               ":com.wpanther.etax.generated.debitcreditnote.ram.impl" +
                               ":com.wpanther.etax.generated.common.qdt.impl" +
                               ":com.wpanther.etax.generated.common.udt.impl";
            this.jaxbContext = JAXBContext.newInstance(contextPath);
            log.info("JAXB context initialized successfully for Thai e-Tax debit/credit note parsing");
        } catch (JAXBException e) {
            log.error("Failed to initialize JAXB context", e);
            throw new DebitCreditNoteParsingException("Failed to initialize XML parser", e);
        }
    }

    @Override
    public ProcessedDebitCreditNote parseNote(String xmlContent, String sourceNoteId)
            throws DebitCreditNoteParsingException {

        log.debug("Starting XML parsing for source note ID: {}", sourceNoteId);

        try {
            DebitCreditNote_CrossIndustryInvoiceType jaxbNote = unmarshalXml(xmlContent);

            ExchangedDocumentType document = jaxbNote.getExchangedDocument();
            if (document == null) {
                throw new DebitCreditNoteParsingException("Debit/Credit note XML missing required ExchangedDocument element");
            }

            SupplyChainTradeTransactionType transaction = jaxbNote.getSupplyChainTradeTransaction();
            if (transaction == null) {
                throw new DebitCreditNoteParsingException("Debit/Credit note XML missing required SupplyChainTradeTransaction element");
            }

            LocalDate issueDate = extractIssueDate(document);

            ProcessedDebitCreditNote note = ProcessedDebitCreditNote.builder()
                .id(DebitCreditNoteId.generate())
                .sourceNoteId(sourceNoteId)
                .noteNumber(extractNoteNumber(document))
                .noteType(extractNoteType(document))
                .issueDate(issueDate)
                .dueDate(extractDueDate(transaction, issueDate))
                .seller(extractSeller(transaction))
                .buyer(extractBuyer(transaction))
                .items(extractLineItems(transaction))
                .currency(extractCurrency(transaction))
                .originalXml(xmlContent)
                .build();

            log.info("Successfully parsed debit/credit note {} with {} line items",
                note.getNoteNumber(), note.getItems().size());

            return note;

        } catch (DebitCreditNoteParsingException e) {
            log.error("Failed to parse debit/credit note XML for source ID {}: {}",
                sourceNoteId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error parsing debit/credit note XML for source ID " + sourceNoteId, e);
            throw new DebitCreditNoteParsingException("Unexpected error during debit/credit note parsing", e);
        }
    }

    private DebitCreditNote_CrossIndustryInvoiceType unmarshalXml(String xmlContent)
            throws DebitCreditNoteParsingException {

        if (xmlContent == null || xmlContent.isBlank()) {
            throw new DebitCreditNoteParsingException("XML content is null or empty");
        }

        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(xmlContent);

            Object result = unmarshaller.unmarshal(reader);

            if (result instanceof jakarta.xml.bind.JAXBElement) {
                jakarta.xml.bind.JAXBElement<?> jaxbElement = (jakarta.xml.bind.JAXBElement<?>) result;
                result = jaxbElement.getValue();
            }

            if (!(result instanceof DebitCreditNote_CrossIndustryInvoiceType)) {
                throw new DebitCreditNoteParsingException(
                    "Unexpected root element: " + result.getClass().getName()
                );
            }

            return (DebitCreditNote_CrossIndustryInvoiceType) result;

        } catch (JAXBException e) {
            log.error("JAXB unmarshalling failed", e);
            throw new DebitCreditNoteParsingException("Failed to parse XML: " + e.getMessage(), e);
        }
    }

    private String extractNoteNumber(ExchangedDocumentType document)
            throws DebitCreditNoteParsingException {

        if (document.getID() == null || document.getID().getValue() == null) {
            throw new DebitCreditNoteParsingException("Debit/Credit note number (ID) is missing");
        }

        return document.getID().getValue();
    }

    private String extractNoteType(ExchangedDocumentType document)
            throws DebitCreditNoteParsingException {

        if (document.getTypeCode() == null) {
            return "UNKNOWN";
        }

        return document.getTypeCode().getValue();
    }

    private LocalDate extractIssueDate(ExchangedDocumentType document)
            throws DebitCreditNoteParsingException {

        XMLGregorianCalendar issueDateTime = document.getIssueDateTime();
        if (issueDateTime == null) {
            throw new DebitCreditNoteParsingException("Issue date/time is missing");
        }

        return convertXMLGregorianCalendarToLocalDate(issueDateTime);
    }

    private LocalDate extractDueDate(SupplyChainTradeTransactionType transaction, LocalDate issueDate)
            throws DebitCreditNoteParsingException {

        HeaderTradeSettlementType settlement = transaction.getApplicableHeaderTradeSettlement();
        if (settlement == null) {
            throw new DebitCreditNoteParsingException("Trade settlement information is missing");
        }

        List<TradePaymentTermsType> paymentTerms = settlement.getSpecifiedTradePaymentTerms();
        if (paymentTerms != null && !paymentTerms.isEmpty()) {
            TradePaymentTermsType terms = paymentTerms.get(0);
            XMLGregorianCalendar dueDateTime = terms.getDueDateDateTime();
            if (dueDateTime != null) {
                return convertXMLGregorianCalendarToLocalDate(dueDateTime);
            }
        }

        log.warn("Due date not found in XML, defaulting to issue date + 30 days");
        return issueDate.plusDays(30);
    }

    private Party extractSeller(SupplyChainTradeTransactionType transaction)
            throws DebitCreditNoteParsingException {

        HeaderTradeAgreementType agreement = transaction.getApplicableHeaderTradeAgreement();
        if (agreement == null || agreement.getSellerTradeParty() == null) {
            throw new DebitCreditNoteParsingException("Seller information is missing");
        }

        return mapParty(agreement.getSellerTradeParty(), "Seller");
    }

    private Party extractBuyer(SupplyChainTradeTransactionType transaction)
            throws DebitCreditNoteParsingException {

        HeaderTradeAgreementType agreement = transaction.getApplicableHeaderTradeAgreement();
        if (agreement == null || agreement.getBuyerTradeParty() == null) {
            throw new DebitCreditNoteParsingException("Buyer information is missing");
        }

        return mapParty(agreement.getBuyerTradeParty(), "Buyer");
    }

    private Party mapParty(TradePartyType jaxbParty, String partyType)
            throws DebitCreditNoteParsingException {

        String name = Optional.ofNullable(jaxbParty.getName())
            .orElseThrow(() -> new DebitCreditNoteParsingException(partyType + " name is missing"));

        TaxIdentifier taxIdentifier = extractTaxIdentifier(jaxbParty, partyType);
        Address address = extractAddress(jaxbParty, partyType);

        String email = null;
        List<TradeContactType> contacts = jaxbParty.getDefinedTradeContact();
        if (contacts != null && !contacts.isEmpty()) {
            TradeContactType contact = contacts.get(0);
            if (contact.getEmailURIUniversalCommunication() != null &&
                contact.getEmailURIUniversalCommunication().getURIID() != null) {
                email = contact.getEmailURIUniversalCommunication().getURIID().getValue();
            }
        }

        return Party.of(name, taxIdentifier, address, email);
    }

    private TaxIdentifier extractTaxIdentifier(TradePartyType jaxbParty, String partyType)
            throws DebitCreditNoteParsingException {

        SpecifiedTaxRegistrationType taxReg = jaxbParty.getSpecifiedTaxRegistration();
        if (taxReg == null) {
            throw new DebitCreditNoteParsingException(partyType + " tax registration is missing");
        }

        if (taxReg.getID() == null || taxReg.getID().getValue() == null) {
            throw new DebitCreditNoteParsingException(partyType + " tax ID is missing");
        }

        String taxId = taxReg.getID().getValue();
        String scheme = Optional.ofNullable(taxReg.getID().getSchemeID())
            .orElse("VAT");

        return TaxIdentifier.of(taxId, scheme);
    }

    private Address extractAddress(TradePartyType jaxbParty, String partyType)
            throws DebitCreditNoteParsingException {

        TradeAddressType jaxbAddress = jaxbParty.getPostalTradeAddress();
        if (jaxbAddress == null) {
            throw new DebitCreditNoteParsingException(partyType + " address is missing");
        }

        String streetAddress = Optional.ofNullable(jaxbAddress.getLineOne())
            .map(line -> line.getValue())
            .orElse(null);

        String city = Optional.ofNullable(jaxbAddress.getCityName())
            .map(name -> name.getValue())
            .orElse(null);

        String postalCode = Optional.ofNullable(jaxbAddress.getPostcodeCode())
            .map(code -> code.getValue())
            .orElse(null);

        String country = null;
        if (jaxbAddress.getCountryID() != null && jaxbAddress.getCountryID().getValue() != null) {
            country = jaxbAddress.getCountryID().getValue().value();
        }
        if (country == null) {
            throw new DebitCreditNoteParsingException(partyType + " country is missing");
        }

        return Address.of(streetAddress, city, postalCode, country);
    }

    private List<LineItem> extractLineItems(SupplyChainTradeTransactionType transaction)
            throws DebitCreditNoteParsingException {

        List<SupplyChainTradeLineItemType> jaxbItems =
            transaction.getIncludedSupplyChainTradeLineItem();

        if (jaxbItems == null || jaxbItems.isEmpty()) {
            throw new DebitCreditNoteParsingException("Debit/Credit note must have at least one line item");
        }

        List<LineItem> items = new ArrayList<>();
        String currency = extractCurrency(transaction);

        for (int i = 0; i < jaxbItems.size(); i++) {
            try {
                LineItem item = mapLineItem(jaxbItems.get(i), currency);
                items.add(item);
            } catch (Exception e) {
                throw new DebitCreditNoteParsingException(
                    "Failed to parse line item " + (i + 1) + ": " + e.getMessage(), e
                );
            }
        }

        return items;
    }

    private LineItem mapLineItem(SupplyChainTradeLineItemType jaxbItem, String currency)
            throws DebitCreditNoteParsingException {

        TradeProductType product = jaxbItem.getSpecifiedTradeProduct();
        if (product == null || product.getName() == null || product.getName().isEmpty()) {
            throw new DebitCreditNoteParsingException("Line item product name is missing");
        }
        String description = product.getName().get(0).getValue();

        LineTradeDeliveryType delivery = jaxbItem.getSpecifiedLineTradeDelivery();
        if (delivery == null || delivery.getBilledQuantity() == null) {
            throw new DebitCreditNoteParsingException("Line item quantity is missing");
        }
        BigDecimal quantityDecimal = delivery.getBilledQuantity().getValue();
        int quantity = quantityDecimal.intValue();

        LineTradeAgreementType agreement = jaxbItem.getSpecifiedLineTradeAgreement();
        if (agreement == null || agreement.getGrossPriceProductTradePrice() == null) {
            throw new DebitCreditNoteParsingException("Line item unit price is missing");
        }
        TradePriceType priceType = agreement.getGrossPriceProductTradePrice();
        if (priceType.getChargeAmount() == null || priceType.getChargeAmount().size() == 0) {
            throw new DebitCreditNoteParsingException("Line item price amount is missing");
        }
        BigDecimal unitPriceAmount = priceType.getChargeAmount().get(0).getValue();
        Money unitPrice = Money.of(unitPriceAmount, currency);

        LineTradeSettlementType settlement = jaxbItem.getSpecifiedLineTradeSettlement();
        BigDecimal taxRate = BigDecimal.ZERO;

        if (settlement != null && settlement.getApplicableTradeTax() != null
            && !settlement.getApplicableTradeTax().isEmpty()) {

            TradeTaxType tax = settlement.getApplicableTradeTax().get(0);
            if (tax.getCalculatedRate() != null) {
                taxRate = tax.getCalculatedRate();
            }
        }

        return new LineItem(description, quantity, unitPrice, taxRate);
    }

    private String extractCurrency(SupplyChainTradeTransactionType transaction)
            throws DebitCreditNoteParsingException {

        HeaderTradeSettlementType settlement = transaction.getApplicableHeaderTradeSettlement();
        if (settlement == null || settlement.getInvoiceCurrencyCode() == null) {
            throw new DebitCreditNoteParsingException("Debit/Credit note currency is missing");
        }

        String currency = null;
        if (settlement.getInvoiceCurrencyCode().getValue() != null) {
            currency = settlement.getInvoiceCurrencyCode().getValue().value();
        }

        if (currency == null || currency.length() != 3) {
            throw new DebitCreditNoteParsingException("Invalid currency code: " + currency);
        }

        return currency;
    }

    private LocalDate convertXMLGregorianCalendarToLocalDate(XMLGregorianCalendar calendar) {
        if (calendar == null) {
            return null;
        }
        return LocalDate.of(
            calendar.getYear(),
            calendar.getMonth(),
            calendar.getDay()
        );
    }
}
