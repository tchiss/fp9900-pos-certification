package com.dspread.pos.utils;

import com.dspread.pos.models.InvoiceData;
import com.dspread.pos.models.InvoiceLine;
import com.dspread.pos.models.Issuer;
import com.dspread.pos.models.Customer;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to create test invoice data for API testing
 * Based on the example from API documentation
 */
public class InvoiceTestData {

    /**
     * Create test invoice data matching the API documentation example
     */
    public static InvoiceData createTestInvoice() {
        // Create issuer (seller)
        Issuer issuer = new Issuer("Sarl IT TECH", "00407222257304518236");
        
        // Create customer (buyer)
        Customer customer = new Customer("Bemba Thomas", "40842322909316563708");
        
        // Create invoice lines
        List<InvoiceLine> invoiceLines = new ArrayList<>();
        
        // Article 1: 5 units at 10000 cents each, 5% VAT
        InvoiceLine line1 = new InvoiceLine("Article1", 5, 10000, 5);
        invoiceLines.add(line1);
        
        // Article 2: 2 units at 3500 cents each, 20% VAT
        InvoiceLine line2 = new InvoiceLine("Article2", 2, 3500, 20);
        invoiceLines.add(line2);
        
        // Create invoice data
        InvoiceData invoiceData = new InvoiceData(
            "Facture20250930", // externalNum
            "2335626442", // machineNum
            issuer,
            customer,
            invoiceLines,
            0, // totalHt - will be calculated
            0, // totalVat - will be calculated
            0, // totalTtc - will be calculated
            Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")) // issueDate
        );
        
        // Calculate totals
        invoiceData.calculateTotals();
        
        return invoiceData;
    }

    /**
     * Create test invoice with custom data
     */
    public static InvoiceData createCustomTestInvoice(String externalNum, String machineNum, 
                                                     String issuerName, String issuerId,
                                                     String customerName, String customerId) {
        // Create issuer
        Issuer issuer = new Issuer(issuerName, issuerId);
        
        // Create customer
        Customer customer = new Customer(customerName, customerId);
        
        // Create sample invoice line
        List<InvoiceLine> invoiceLines = new ArrayList<>();
        InvoiceLine line = new InvoiceLine("Test Item", 1, 10000, 18); // 1 item at 100.00 with 18% VAT
        invoiceLines.add(line);
        
        // Create invoice data
        InvoiceData invoiceData = new InvoiceData(
            externalNum,
            machineNum,
            issuer,
            customer,
            invoiceLines,
            0, // totalHt - will be calculated
            0, // totalVat - will be calculated
            0, // totalTtc - will be calculated
            Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
        );
        
        // Calculate totals
        invoiceData.calculateTotals();
        
        return invoiceData;
    }

    /**
     * Print invoice details for debugging
     */
    public static void printInvoiceDetails(InvoiceData invoice) {
        TRACE.i("=== Invoice Details ===");
        TRACE.i("External Number: " + invoice.getExternalNum());
        TRACE.i("Machine Number: " + invoice.getMachineNum());
        TRACE.i("Issuer: " + invoice.getIssuer().getName() + " (" + invoice.getIssuer().getIdentityNumber() + ")");
        TRACE.i("Customer: " + invoice.getCustomer().getName() + " (" + invoice.getCustomer().getIdentityNumber() + ")");
        TRACE.i("Issue Date: " + invoice.getIssueDate());
        TRACE.i("Total HT: " + invoice.getTotalHt() + " cents");
        TRACE.i("Total VAT: " + invoice.getTotalVat() + " cents");
        TRACE.i("Total TTC: " + invoice.getTotalTtc() + " cents");
        
        TRACE.i("Invoice Lines:");
        for (int i = 0; i < invoice.getInvoiceLines().size(); i++) {
            InvoiceLine line = invoice.getInvoiceLines().get(i);
            TRACE.i("  " + (i + 1) + ". " + line.getDesignation() + 
                   " - Qty: " + line.getQuantity() + 
                   ", Unit Price: " + line.getUnitPrice() + " cents" +
                   ", VAT: " + line.getVatRate() + "%" +
                   ", Total: " + line.getTotalWithVat() + " cents");
        }
        TRACE.i("=== End Invoice Details ===");
    }
}
