package com.dspread.pos.models;

/**
 * Response model for invoice verification
 * Contains the complete fiscalized invoice data
 */
public class InvoiceVerificationResponse {
    private String invoiceId;
    private String externalNum;
    private String machineNum;
    private Issuer issuer;
    private Customer customer;
    private java.util.List<InvoiceLine> invoiceLines;
    private int totalHt;
    private int totalVat;
    private int totalTtc;
    private String issueDate;
    private String fiscalizationDate;
    private String status; // FISCALIZED, PENDING, etc.
    private String qrCode; // QR code data for fiscalized invoice
    private String mecefCode; // MECEF code if available

    public InvoiceVerificationResponse() {
    }

    // Getters and Setters
    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

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

    public java.util.List<InvoiceLine> getInvoiceLines() {
        return invoiceLines;
    }

    public void setInvoiceLines(java.util.List<InvoiceLine> invoiceLines) {
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

    public String getFiscalizationDate() {
        return fiscalizationDate;
    }

    public void setFiscalizationDate(String fiscalizationDate) {
        this.fiscalizationDate = fiscalizationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getMecefCode() {
        return mecefCode;
    }

    public void setMecefCode(String mecefCode) {
        this.mecefCode = mecefCode;
    }

    /**
     * Check if invoice is fiscalized
     */
    public boolean isFiscalized() {
        return "FISCALIZED".equals(status);
    }

    /**
     * Check if invoice is pending
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    @Override
    public String toString() {
        return "InvoiceVerificationResponse{" +
                "invoiceId='" + invoiceId + '\'' +
                ", externalNum='" + externalNum + '\'' +
                ", status='" + status + '\'' +
                ", fiscalizationDate='" + fiscalizationDate + '\'' +
                ", qrCode='" + qrCode + '\'' +
                ", mecefCode='" + mecefCode + '\'' +
                '}';
    }
}
