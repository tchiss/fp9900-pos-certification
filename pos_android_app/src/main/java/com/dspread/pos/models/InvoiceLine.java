package com.dspread.pos.models;

/**
 * Model for invoice line item
 * Based on API documentation requirements
 */
public class InvoiceLine {
    private String designation;
    private int quantity;
    private int unitPrice; // API expects integer (cents)
    private int vatRate; // VAT rate as integer (e.g., 5 for 5%, 20 for 20%)

    public InvoiceLine() {
    }

    public InvoiceLine(String designation, int quantity, int unitPrice, int vatRate) {
        this.designation = designation;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.vatRate = vatRate;
    }

    // Getters and Setters
    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(int unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getVatRate() {
        return vatRate;
    }

    public void setVatRate(int vatRate) {
        this.vatRate = vatRate;
    }

    /**
     * Calculate total price for this line (quantity * unitPrice)
     */
    public int getTotalPrice() {
        return quantity * unitPrice;
    }

    /**
     * Calculate VAT amount for this line
     */
    public int getVatAmount() {
        return (int) Math.round(getTotalPrice() * vatRate / 100.0);
    }

    /**
     * Calculate total including VAT
     */
    public int getTotalWithVat() {
        return getTotalPrice() + getVatAmount();
    }

    @Override
    public String toString() {
        return "InvoiceLine{" +
                "designation='" + designation + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", vatRate=" + vatRate +
                '}';
    }
}