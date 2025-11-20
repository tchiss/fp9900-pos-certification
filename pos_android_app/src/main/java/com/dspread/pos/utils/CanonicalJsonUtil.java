package com.dspread.pos.utils;

import com.dspread.pos.models.InvoiceData;
import com.dspread.pos.models.Issuer;
import com.dspread.pos.models.Customer;
import com.dspread.pos.models.InvoiceLine;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class for generating canonical JSON from InvoiceData
 * Uses TreeMap to ensure alphabetical key ordering for deterministic JSON
 */
public class CanonicalJsonUtil {
    private static final Gson gson = new Gson();

    /**
     * Generates canonical JSON string from InvoiceData
     * Keys are sorted alphabetically to ensure deterministic output
     * 
     * @param invoiceData The invoice data to convert
     * @return Canonical JSON string
     */
    public static String toCanonicalJson(InvoiceData invoiceData) {
        if (invoiceData == null) {
            throw new IllegalArgumentException("InvoiceData cannot be null");
        }

        TreeMap<String, Object> map = toCanonicalMap(invoiceData);
        return gson.toJson(map);
    }

    /**
     * Converts InvoiceData to TreeMap with alphabetical key ordering
     * 
     * @param invoiceData The invoice data to convert
     * @return TreeMap with sorted keys
     */
    private static TreeMap<String, Object> toCanonicalMap(InvoiceData invoiceData) {
        TreeMap<String, Object> map = new TreeMap<>();

        // Add fields in alphabetical order
        if (invoiceData.getCustomer() != null) {
            map.put("customer", toCustomerMap(invoiceData.getCustomer()));
        }
        
        if (invoiceData.getExternalNum() != null) {
            map.put("externalNum", invoiceData.getExternalNum());
        }
        
        if (invoiceData.getInvoiceLines() != null && !invoiceData.getInvoiceLines().isEmpty()) {
            map.put("invoiceLines", toInvoiceLinesList(invoiceData.getInvoiceLines()));
        }
        
        if (invoiceData.getIssueDate() != null) {
            map.put("issueDate", invoiceData.getIssueDate());
        }
        
        if (invoiceData.getIssuer() != null) {
            map.put("issuer", toIssuerMap(invoiceData.getIssuer()));
        }
        
        if (invoiceData.getMachineNum() != null) {
            map.put("machineNum", invoiceData.getMachineNum());
        }
        
        map.put("totalHt", invoiceData.getTotalHt());
        map.put("totalTtc", invoiceData.getTotalTtc());
        map.put("totalVat", invoiceData.getTotalVat());

        return map;
    }

    /**
     * Converts Issuer to TreeMap
     */
    private static TreeMap<String, Object> toIssuerMap(Issuer issuer) {
        TreeMap<String, Object> map = new TreeMap<>();
        
        if (issuer.getIdentityNumber() != null) {
            map.put("identityNumber", issuer.getIdentityNumber());
        }
        
        if (issuer.getName() != null) {
            map.put("name", issuer.getName());
        }
        
        if (issuer.getTel() != null) {
            map.put("tel", issuer.getTel());
        }
        
        return map;
    }

    /**
     * Converts Customer to TreeMap
     */
    private static TreeMap<String, Object> toCustomerMap(Customer customer) {
        TreeMap<String, Object> map = new TreeMap<>();
        
        if (customer.getIdentityNumber() != null) {
            map.put("identityNumber", customer.getIdentityNumber());
        }
        
        if (customer.getName() != null) {
            map.put("name", customer.getName());
        }
        
        if (customer.getTel() != null) {
            map.put("tel", customer.getTel());
        }
        
        return map;
    }

    /**
     * Converts List of InvoiceLine to List of TreeMaps
     */
    private static List<TreeMap<String, Object>> toInvoiceLinesList(List<InvoiceLine> invoiceLines) {
        List<TreeMap<String, Object>> list = new ArrayList<>();
        
        for (InvoiceLine line : invoiceLines) {
            TreeMap<String, Object> map = new TreeMap<>();
            
            if (line.getDesignation() != null) {
                map.put("designation", line.getDesignation());
            }
            
            map.put("quantity", line.getQuantity());
            map.put("unitPrice", line.getUnitPrice());
            map.put("vatRate", line.getVatRate());
            
            list.add(map);
        }
        
        return list;
    }
}

