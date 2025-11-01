package com.dspread.pos.models;

/**
 * Model for invoice issuer (seller)
 * Based on API documentation requirements
 */
public class Issuer {
    private String name;
    private String identityNumber;
    private String tel; // Phone number

    public Issuer() {
    }

    public Issuer(String name, String identityNumber, String tel) {
        this.name = name;
        this.identityNumber = identityNumber;
        this.tel = tel;
    }

    public Issuer(String name, String identityNumber) {
        this.name = name;
        this.identityNumber = identityNumber;
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
