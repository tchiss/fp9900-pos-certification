package com.dspread.pos.models;

/**
 * Model for invoice issuer (seller)
 * Based on KPS Access - Normalisation Factures API
 * Format: { "name": "...", "identityNumber": "...", "tel": "..." }
 */
public class Issuer {
    private String name;
    private String identityNumber; // API expects "identityNumber" field
    private String tel; // Tel field is required by API

    public Issuer() {
    }

    public Issuer(String name, String identityNumber) {
        this.name = name;
        this.identityNumber = identityNumber;
        this.tel = "+237123456789"; // Default tel if not provided
    }
    
    public Issuer(String name, String identityNumber, String tel) {
        this.name = name;
        this.identityNumber = identityNumber;
        this.tel = tel;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentityNumber() {
        return identityNumber;
    }

    public void setIdentityNumber(String identityNumber) {
        this.identityNumber = identityNumber;
    }
    
    public String getTel() {
        return tel;
    }
    
    public void setTel(String tel) {
        this.tel = tel;
    }

    @Override
    public String toString() {
        return "Issuer{" +
                "name='" + name + '\'' +
                ", identityNumber='" + identityNumber + '\'' +
                ", tel='" + tel + '\'' +
                '}';
    }
}
