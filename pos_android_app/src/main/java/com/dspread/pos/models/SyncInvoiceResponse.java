package com.dspread.pos.models;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for invoice synchronization
 */
public class SyncInvoiceResponse {
    @SerializedName("invoiceId")
    private String invoiceId;

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    public SyncInvoiceResponse() {
    }

    public SyncInvoiceResponse(String invoiceId, String status, String message) {
        this.invoiceId = invoiceId;
        this.status = status;
        this.message = message;
    }

    // Getters and Setters
    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

