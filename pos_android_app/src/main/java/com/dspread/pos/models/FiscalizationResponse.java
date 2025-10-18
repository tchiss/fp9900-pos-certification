package com.dspread.pos.models;

/**
 * Response model for invoice fiscalization
 * Based on API documentation
 */
public class FiscalizationResponse {
    private String status; // FISCALIZED, ERROR, etc.
    private String message; // Optional message
    private String errorCode; // Error code if fiscalization failed

    public FiscalizationResponse() {
    }

    public FiscalizationResponse(String status) {
        this.status = status;
    }

    public FiscalizationResponse(String status, String message, String errorCode) {
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
    }

    // Getters and Setters
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
     * Check if fiscalization was successful
     */
    public boolean isSuccess() {
        return "FISCALIZED".equals(status);
    }

    /**
     * Check if there was an error
     */
    public boolean hasError() {
        return errorCode != null || "ERROR".equals(status);
    }

    @Override
    public String toString() {
        return "FiscalizationResponse{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", errorCode='" + errorCode + '\'' +
                '}';
    }
}
