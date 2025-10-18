package com.dspread.pos.models;

/**
 * Response model for invoice creation
 * Based on API documentation
 */
public class InvoiceCreationResponse {
    private String invoiceId; // The unique invoice ID returned by the API
    private String status; // Status of the creation (e.g., "CREATED", "ERROR")
    private String message; // Optional message
    private String errorCode; // Error code if creation failed (e.g., REJ001, REJ002, etc.)

    public InvoiceCreationResponse() {
    }

    public InvoiceCreationResponse(String invoiceId, String status) {
        this.invoiceId = invoiceId;
        this.status = status;
    }

    public InvoiceCreationResponse(String status, String message, String errorCode) {
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
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

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Check if invoice creation was successful
     */
    public boolean isSuccess() {
        return "CREATED".equals(status) && invoiceId != null && !invoiceId.trim().isEmpty();
    }

    /**
     * Check if there was an error
     */
    public boolean hasError() {
        return errorCode != null || "ERROR".equals(status);
    }

    @Override
    public String toString() {
        return "InvoiceCreationResponse{" +
                "invoiceId='" + invoiceId + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", errorCode='" + errorCode + '\'' +
                '}';
    }
}
