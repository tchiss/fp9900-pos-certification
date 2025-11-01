package com.dspread.pos.models;

/**
 * Response model for invoice fiscalization
 * Based on KPS Access API documentation V1 (Octobre 2025)
 * Response structure: { "token": "...", "qrBase64": "...", "certifiedAt": "..." }
 */
public class FiscalizationResponse {
    private String token; // Fiscal token for the certified invoice
    private String qrBase64; // QR code image in base64 format
    private String certifiedAt; // ISO timestamp when invoice was certified
    private String errorCode; // Error code if fiscalization failed (for error cases)
    private String message; // Error message if fiscalization failed (for error cases)

    public FiscalizationResponse() {
    }

    public FiscalizationResponse(String token, String qrBase64, String certifiedAt) {
        this.token = token;
        this.qrBase64 = qrBase64;
        this.certifiedAt = certifiedAt;
    }

    public FiscalizationResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getQrBase64() {
        return qrBase64;
    }

    public void setQrBase64(String qrBase64) {
        this.qrBase64 = qrBase64;
    }

    public String getCertifiedAt() {
        return certifiedAt;
    }

    public void setCertifiedAt(String certifiedAt) {
        this.certifiedAt = certifiedAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Check if fiscalization was successful
     * Success is indicated by presence of token and qrBase64
     */
    public boolean isSuccess() {
        return token != null && !token.isEmpty() && 
               qrBase64 != null && !qrBase64.isEmpty();
    }

    /**
     * Check if there was an error
     */
    public boolean hasError() {
        return errorCode != null || message != null;
    }

    /**
     * Get QR code as byte array (decoded from base64)
     */
    public byte[] getQrCodeBytes() {
        if (qrBase64 == null || qrBase64.isEmpty()) {
            return null;
        }
        try {
            return android.util.Base64.decode(qrBase64, android.util.Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "FiscalizationResponse{" +
                "token='" + token + '\'' +
                ", qrBase64='" + (qrBase64 != null ? qrBase64.substring(0, Math.min(50, qrBase64.length())) + "..." : "null") + '\'' +
                ", certifiedAt='" + certifiedAt + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
