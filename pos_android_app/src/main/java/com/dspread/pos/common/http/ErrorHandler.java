package com.dspread.pos.common.http;

public final class ErrorHandler {
    private ErrorHandler() {}

    public static String map(int statusCode, String body) {
        if (body == null) body = "";
        // Rejection codes (tailor as needed)
        if (body.contains("REJ001")) return "REJ001: Invalid parameters";
        if (body.contains("REJ002")) return "REJ002: Missing issuer information";
        if (body.contains("REJ003")) return "REJ003: Missing customer information";
        if (body.contains("REJ004")) return "REJ004: Missing invoice line data";
        if (body.contains("REJ005")) return "REJ005: Line totals don't match invoice totals";
        if (body.contains("REJ006")) return "REJ006: Unknown issuer identity number";
        if (body.contains("REJ007")) return "REJ007: Unknown issuer identity number in system";
        if (body.contains("REJ008")) return "REJ008: Unknown client identity number in system";
        if (body.contains("REJ009")) return "REJ009: Unknown/duplicate machine ID or external number";
        if (body.contains("REJ010")) return "REJ010: General error occurred";
        if (body.contains("REJ020")) return "REJ020: Invoice number not provided";
        if (body.contains("REJ021")) return "REJ021: Unknown invoice number";
        if (body.contains("REJ022")) return "REJ022: Error occurred during normalization";
        if (body.contains("REJ030")) return "REJ030: Unknown invoice number";
        if (body.contains("REJ040")) return "REJ040: Read data is null or empty";
        if (body.contains("REJ041")) return "REJ041: Invalid issuer identification number";
        if (body.contains("REJ042")) return "REJ042: Invalid client identification number";
        if (body.contains("REJ043")) return "REJ043: Invalid machine identification number";
        if (body.contains("REJ044")) return "REJ044: Data already synchronized";
        if (body.contains("REJ049")) return "REJ049: Error occurred during synchronization";

        // Java 11 compatible switch statement
        switch (statusCode) {
            case 400: return "Invalid data - Check entered information";
            case 401: return "Unauthorized - Check your credentials";
            case 403: return "Access denied - Insufficient permissions";
            case 404: return "Service not found";
            case 422: return "Invalid invoice data - Check format";
            case 500: return "Server error - Try again later";
            case 503: return "Service temporarily unavailable";
            default: return "HTTP " + statusCode + ": " + body;
        }
    }
}