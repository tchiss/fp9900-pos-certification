package com.dspread.pos.models;

import java.util.List;

/**
 * Modèle de données pour la réponse de certification DGI
 * Migré depuis InvoiceResponse de React Native
 */
public class CertificationResponse {
    private String status; // CERTIFIED, PENDING, REJECTED
    private String mecefCode;
    private String qrData;
    private String dgiInvoiceId;
    private List<String> warnings;
    private List<String> reasons;
    private String timestamp;

    public CertificationResponse() {
    }

    public CertificationResponse(String status, String mecefCode, String qrData, 
                               String dgiInvoiceId, String timestamp) {
        this.status = status;
        this.mecefCode = mecefCode;
        this.qrData = qrData;
        this.dgiInvoiceId = dgiInvoiceId;
        this.timestamp = timestamp;
    }

    // Getters et Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMecefCode() {
        return mecefCode;
    }

    public void setMecefCode(String mecefCode) {
        this.mecefCode = mecefCode;
    }

    public String getQrData() {
        return qrData;
    }

    public void setQrData(String qrData) {
        this.qrData = qrData;
    }

    public String getDgiInvoiceId() {
        return dgiInvoiceId;
    }

    public void setDgiInvoiceId(String dgiInvoiceId) {
        this.dgiInvoiceId = dgiInvoiceId;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Vérifie si la certification a réussi
     */
    public boolean isCertified() {
        return "CERTIFIED".equals(status);
    }

    /**
     * Vérifie si la certification est en attente
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    /**
     * Vérifie si la certification a été rejetée
     */
    public boolean isRejected() {
        return "REJECTED".equals(status);
    }

    @Override
    public String toString() {
        return "CertificationResponse{" +
                "status='" + status + '\'' +
                ", mecefCode='" + mecefCode + '\'' +
                ", qrData='" + qrData + '\'' +
                ", dgiInvoiceId='" + dgiInvoiceId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}