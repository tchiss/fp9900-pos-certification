package com.dspread.pos.models;

import java.util.List;

/**
 * Model for invoice data according to API documentation
 * Based on the JSON structure from the API docs
 */
public class InvoiceData {
    private String externalNum; // External invoice number
    private String machineNum; // Machine identification number
    private Issuer issuer; // Invoice issuer (seller)
    private Customer customer; // Invoice customer (buyer)
    private List<InvoiceLine> invoiceLines; // Invoice line items
    private int totalHt; // Total excluding tax (in cents)
    private int totalVat; // Total VAT amount (in cents)
    private int totalTtc; // Total including tax (in cents)
    private String issueDate; // Invoice issue date in ISO format

    public InvoiceData() {
    }

    public InvoiceData(String externalNum, String machineNum, Issuer issuer, Customer customer,
                      List<InvoiceLine> invoiceLines, int totalHt, int totalVat, int totalTtc, String issueDate) {
        this.externalNum = externalNum;
        this.machineNum = machineNum;
        this.issuer = issuer;
        this.customer = customer;
        this.invoiceLines = invoiceLines;
        this.totalHt = totalHt;
        this.totalVat = totalVat;
        this.totalTtc = totalTtc;
        this.issueDate = issueDate;
    }

    // Getters and Setters
    public String getExternalNum() {
        return externalNum;
    }

    public void setExternalNum(String externalNum) {
        this.externalNum = externalNum;
    }

    public String getMachineNum() {
        return machineNum;
    }

    public void setMachineNum(String machineNum) {
        this.machineNum = machineNum;
    }

    public Issuer getIssuer() {
        return issuer;
    }

    public void setIssuer(Issuer issuer) {
        this.issuer = issuer;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<InvoiceLine> getInvoiceLines() {
        return invoiceLines;
    }

    public void setInvoiceLines(List<InvoiceLine> invoiceLines) {
        this.invoiceLines = invoiceLines;
    }

    public int getTotalHt() {
        return totalHt;
    }

    public void setTotalHt(int totalHt) {
        this.totalHt = totalHt;
    }

    public int getTotalVat() {
        return totalVat;
    }

    public void setTotalVat(int totalVat) {
        this.totalVat = totalVat;
    }

    public int getTotalTtc() {
        return totalTtc;
    }

    public void setTotalTtc(int totalTtc) {
        this.totalTtc = totalTtc;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(String issueDate) {
        this.issueDate = issueDate;
    }

    /**
     * Calculate totals from invoice lines
     */
    public void calculateTotals() {
        if (invoiceLines == null || invoiceLines.isEmpty()) {
            this.totalHt = 0;
            this.totalVat = 0;
            this.totalTtc = 0;
            return;
        }

        int calculatedHt = 0;
        int calculatedVat = 0;

        for (InvoiceLine line : invoiceLines) {
            calculatedHt += line.getTotalPrice();
            calculatedVat += line.getVatAmount();
        }

        this.totalHt = calculatedHt;
        this.totalVat = calculatedVat;
        this.totalTtc = calculatedHt + calculatedVat;
    }

    /**
     * Validate invoice data according to API requirements
     */
    public boolean isValid() {
        // Check required fields
        if (externalNum == null || externalNum.trim().isEmpty()) {
            return false;
        }
        if (machineNum == null || machineNum.trim().isEmpty()) {
            return false;
        }
        if (issuer == null || issuer.getName() == null || issuer.getName().trim().isEmpty()) {
            return false;
        }
        if (issuer.getIdentityNumber() == null || issuer.getIdentityNumber().trim().isEmpty()) {
            return false;
        }
        if (issuer.getTel() == null || issuer.getTel().trim().isEmpty()) {
            return false;
        }
        if (customer == null || customer.getName() == null || customer.getName().trim().isEmpty()) {
            return false;
        }
        if (customer.getIdentityNumber() == null || customer.getIdentityNumber().trim().isEmpty()) {
            return false;
        }
        if (invoiceLines == null || invoiceLines.isEmpty()) {
            return false;
        }

        // Validate invoice lines
        for (InvoiceLine line : invoiceLines) {
            if (line.getDesignation() == null || line.getDesignation().trim().isEmpty()) {
                return false;
            }
            if (line.getQuantity() <= 0) {
                return false;
            }
            if (line.getUnitPrice() <= 0) {
                return false;
            }
            if (line.getVatRate() < 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "InvoiceData{" +
                "externalNum='" + externalNum + '\'' +
                ", machineNum='" + machineNum + '\'' +
                ", issuer=" + issuer +
                ", customer=" + customer +
                ", invoiceLines=" + invoiceLines +
                ", totalHt=" + totalHt +
                ", totalVat=" + totalVat +
                ", totalTtc=" + totalTtc +
                ", issueDate='" + issueDate + '\'' +
                '}';
    }
}