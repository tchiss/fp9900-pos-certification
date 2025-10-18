package com.dspread.pos.models;

/**
 * Model for invoice customer (buyer)
 * Based on API documentation requirements
 */
public class Customer {
    private String name;
    private String identityNumber;

    public Customer() {
    }

    public Customer(String name, String identityNumber) {
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

    @Override
    public String toString() {
        return "Customer{" +
                "name='" + name + '\'' +
                ", identityNumber='" + identityNumber + '\'' +
                '}';
    }
}
