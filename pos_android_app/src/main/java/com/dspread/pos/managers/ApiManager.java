package com.dspread.pos.managers;

import android.content.Context;
import android.util.Log;

import com.dspread.pos.models.InvoiceData;
import com.dspread.pos.models.InvoiceCreationResponse;
import com.dspread.pos.models.FiscalizationResponse;
import com.dspread.pos.models.InvoiceVerificationResponse;
import com.dspread.pos.utils.TRACE;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Manager for DGI API calls
 * Migrated from React Native api.ts
 */
public class ApiManager {
    private static final String TAG = "ApiManager";
    private static final String BASE_URL = "https://api.invoice.fisc.kpsaccess.com:9443";
    private static final int TIMEOUT_SECONDS = 30;
    
    private static ApiManager instance;
    private OkHttpClient httpClient;
    private Gson gson;
    private Context context;

    private ApiManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        initializeHttpClient();
    }

    public static synchronized ApiManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ApiManager not initialized. Call initialize() first.");
        }
        return instance;
    }

    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new ApiManager(context);
        }
    }

    private void initializeHttpClient() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Interface for invoice creation callbacks
     */
    public interface InvoiceCreationCallback {
        void onSuccess(InvoiceCreationResponse response);
        void onError(String error);
    }

    /**
     * Interface for fiscalization callbacks
     */
    public interface FiscalizationCallback {
        void onSuccess(FiscalizationResponse response);
        void onError(String error);
    }

    /**
     * Interface for invoice verification callbacks
     */
    public interface InvoiceVerificationCallback {
        void onSuccess(InvoiceVerificationResponse response);
        void onError(String error);
    }

    /**
     * Interface for API health callbacks
     */
    public interface HealthCallback {
        void onSuccess(boolean isHealthy);
        void onError(String error);
    }

    /**
     * Create an invoice
     */
    public void createInvoice(InvoiceData invoiceData, InvoiceCreationCallback callback) {
        TRACE.i(TAG + ": Starting invoice creation");
        
        try {
            // Validate invoice data
            if (!invoiceData.isValid()) {
                callback.onError("Invalid invoice data");
                return;
            }

            // Convert data to JSON
            String jsonData = gson.toJson(invoiceData);
            Log.d(TAG, "Invoice data: " + jsonData);

            // Create request
            RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), 
                jsonData
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/invoices")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build();

            // Execute request
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    TRACE.e(TAG + ": Network error during invoice creation: " + e.getMessage());
                    callback.onError("Connection error - Check your network");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "API Response: " + responseBody);

                        if (response.isSuccessful()) {
                            // Parse response
                            InvoiceCreationResponse creationResponse = gson.fromJson(
                                responseBody, 
                                InvoiceCreationResponse.class
                            );
                            
                            TRACE.i(TAG + ": Invoice creation successful: " + creationResponse.getStatus());
                            callback.onSuccess(creationResponse);
                        } else {
                            // Handle HTTP errors
                            String errorMessage = handleHttpError(response.code(), responseBody);
                            TRACE.e(TAG + ": HTTP error " + response.code() + ": " + errorMessage);
                            callback.onError(errorMessage);
                        }
                    } catch (JsonSyntaxException e) {
                        TRACE.e(TAG + ": JSON parsing error: " + e.getMessage());
                        callback.onError("Server response format error");
                    } catch (Exception e) {
                        TRACE.e(TAG + ": Unexpected error: " + e.getMessage());
                        callback.onError("Unexpected error during invoice creation");
                    } finally {
                        response.close();
                    }
                }
            });

        } catch (Exception e) {
            TRACE.e(TAG + ": Error preparing invoice creation request: " + e.getMessage());
            callback.onError("Error preparing request");
        }
    }

    /**
     * Fiscalize an invoice (certify it)
     */
    public void fiscalizeInvoice(String invoiceId, FiscalizationCallback callback) {
        TRACE.i(TAG + ": Starting invoice fiscalization for ID: " + invoiceId);
        
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/invoices/" + invoiceId + "/fiscalize")
                    .post(RequestBody.create(null, ""))
                    .addHeader("Accept", "*/*")
                    .build();

            // Execute request
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    TRACE.e(TAG + ": Network error during fiscalization: " + e.getMessage());
                    callback.onError("Connection error - Check your network");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Fiscalization Response: " + responseBody);

                        if (response.isSuccessful()) {
                            // Parse response
                            FiscalizationResponse fiscalizationResponse = gson.fromJson(
                                responseBody, 
                                FiscalizationResponse.class
                            );
                            
                            TRACE.i(TAG + ": Fiscalization successful: " + fiscalizationResponse.getStatus());
                            callback.onSuccess(fiscalizationResponse);
                        } else {
                            // Handle HTTP errors
                            String errorMessage = handleHttpError(response.code(), responseBody);
                            TRACE.e(TAG + ": HTTP error " + response.code() + ": " + errorMessage);
                            callback.onError(errorMessage);
                        }
                    } catch (JsonSyntaxException e) {
                        TRACE.e(TAG + ": JSON parsing error: " + e.getMessage());
                        callback.onError("Server response format error");
                    } catch (Exception e) {
                        TRACE.e(TAG + ": Unexpected error: " + e.getMessage());
                        callback.onError("Unexpected error during fiscalization");
                    } finally {
                        response.close();
                    }
                }
            });

        } catch (Exception e) {
            TRACE.e(TAG + ": Error preparing fiscalization request: " + e.getMessage());
            callback.onError("Error preparing request");
        }
    }

    /**
     * Verify/fetch an invoice
     */
    public void verifyInvoice(String invoiceId, InvoiceVerificationCallback callback) {
        TRACE.i(TAG + ": Starting invoice verification for staging: " + invoiceId);
        
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/invoices/" + invoiceId)
                    .get()
                    .addHeader("Accept", "*/*")
                    .build();

            // Execute request
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    TRACE.e(TAG + ": Network error during verification: " + e.getMessage());
                    callback.onError("Connection error - Check your network");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Verification Response: " + responseBody);

                        if (response.isSuccessful()) {
                            // Parse response
                            InvoiceVerificationResponse verificationResponse = gson.fromJson(
                                responseBody, 
                                InvoiceVerificationResponse.class
                            );
                            
                            TRACE.i(TAG + ": Verification successful: " + verificationResponse.getStatus());
                            callback.onSuccess(verificationResponse);
                        } else {
                            // Handle HTTP errors
                            String errorMessage = handleHttpError(response.code(), responseBody);
                            TRACE.e(TAG + ": HTTP error " + response.code() + ": " + errorMessage);
                            callback.onError(errorMessage);
                        }
                    } catch (JsonSyntaxException e) {
                        TRACE.e(TAG + ": JSON parsing error: " + e.getMessage());
                        callback.onError("Server response format error");
                    } catch (Exception e) {
                        TRACE.e(TAG + ": Unexpected error: " + e.getMessage());
                        callback.onError("Unexpected error during verification");
                    } finally {
                        response.close();
                    }
                }
            });

        } catch (Exception e) {
            TRACE.e(TAG + ": Error preparing verification request: " + e.getMessage());
            callback.onError("Error preparing request");
        }
    }

    /**
     * Check DGI API health
     */
    public void checkApiHealth(HealthCallback callback) {
        TRACE.i(TAG + ": Checking API health");
        
        Request request = new Request.Builder()
                .url(BASE_URL + "/actuator/health")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                TRACE.e(TAG + ": Health check failed: " + e.getMessage());
                callback.onError("Unable to contact DGI API");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    boolean isHealthy = response.isSuccessful();
                    TRACE.i(TAG + ": Health check result: " + isHealthy);
                    callback.onSuccess(isHealthy);
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * Handle HTTP errors and return appropriate error message
     */
    private String handleHttpError(int statusCode, String responseBody) {
        // Try to parse error response for specific error codes
        try {
            Gson gson = new Gson();
            // Assuming error response has errorCode field
            if (responseBody.contains("errorCode")) {
                // Parse error response to get specific error code
                // This would need to be adjusted based on actual API error response format
            }
        } catch (Exception e) {
            // Fall back to generic error handling
        }

        // Handle specific rejection codes from documentation
        if (responseBody.contains("REJ001")) {
            return "REJ001: No valid parameters provided";
        } else if (responseBody.contains("REJ002")) {
            return "REJ002: Missing issuer information (Identity Number, Name or Phone)";
        } else if (responseBody.contains("REJ003")) {
            return "REJ003: Missing customer information (Identity Number or Name)";
        } else if (responseBody.contains("REJ004")) {
            return "REJ004: Missing invoice line information (quantity, unit amount or VAT rate)";
        } else if (responseBody.contains("REJ005")) {
            return "REJ005: Line totals don't match invoice totals";
        } else if (responseBody.contains("REJ006")) {
            return "REJ006: Unknown issuer identity number";
        } else if (responseBody.contains("REJ007")) {
            return "REJ007: Unknown customer identity number";
        } else if (responseBody.contains("REJ008")) {
            return "REJ008: Unknown machine number or duplicate external number";
        } else if (responseBody.contains("REJ009")) {
            return "REJ009: Error occurred during invoice creation";
        } else if (responseBody.contains("REJ020")) {
            return "REJ020: Invoice number not provided";
        } else if (responseBody.contains("REJ021")) {
            return "REJ021: Unknown invoice number";
        } else if (responseBody.contains("REJ022")) {
            return "REJ022: Error occurred during invoice normalization";
        } else if (responseBody.contains("REJ030")) {
            return "REJ030: Unknown invoice number";
        }

        // Generic HTTP error handling
        switch (statusCode) {
            case 400:
                return "Invalid data - Check entered information";
            case 401:
                return "Unauthorized - Check your credentials";
            case 403:
                return "Access denied - Insufficient permissions";
            case 404:
                return "Service not found - Contact support";
            case 422:
                return "Invalid invoice data - Check format";
            case 500:
                return "Server error - Try again later";
            case 503:
                return "Service temporarily unavailable - DGI service maintenance";
            default:
                return "Error " + statusCode + " - " + responseBody;
        }
    }

    /**
     * Complete invoice certification flow: Create -> Fiscalize -> Verify
     */
    public void certifyInvoiceComplete(InvoiceData invoiceData, InvoiceCertificationCallback callback) {
        TRACE.i(TAG + ": Starting complete invoice certification flow");
        
        // Step 1: Create invoice
        createInvoice(invoiceData, new InvoiceCreationCallback() {
            @Override
            public void onSuccess(InvoiceCreationResponse creationResponse) {
                String invoiceId = creationResponse.getInvoiceId();
                TRACE.i(TAG + ": Invoice created successfully with ID: " + invoiceId);
                
                // Step 2: Fiscalize invoice
                fiscalizeInvoice(invoiceId, new FiscalizationCallback() {
                    @Override
                    public void onSuccess(FiscalizationResponse fiscalizationResponse) {
                        TRACE.i(TAG + ": Invoice fiscalized successfully");
                        
                        // Step 3: Verify invoice
                        verifyInvoice(invoiceId, new InvoiceVerificationCallback() {
                            @Override
                            public void onSuccess(InvoiceVerificationResponse verificationResponse) {
                                TRACE.i(TAG + ": Invoice verification successful");
                                callback.onSuccess(verificationResponse);
                            }

                            @Override
                            public void onError(String error) {
                                TRACE.e(TAG + ": Invoice verification failed: " + error);
                                callback.onError("Verification failed: " + error);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        TRACE.e(TAG + ": Invoice fiscalization failed: " + error);
                        callback.onError("Fiscalization failed: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                TRACE.e(TAG + ": Invoice creation failed: " + error);
                callback.onError("Creation failed: " + error);
            }
        });
    }

    /**
     * Interface for complete invoice certification callbacks
     */
    public interface InvoiceCertificationCallback {
        void onSuccess(InvoiceVerificationResponse response);
        void onError(String error);
    }

    /**
     * Retry with exponential backoff
     */
    public void certifyInvoiceWithRetry(InvoiceData invoiceData, int maxRetries, InvoiceCertificationCallback callback) {
        certifyInvoiceWithRetry(invoiceData, maxRetries, 0, callback);
    }

    private void certifyInvoiceWithRetry(InvoiceData invoiceData, int maxRetries, int currentRetry, InvoiceCertificationCallback callback) {
        certifyInvoiceComplete(invoiceData, new InvoiceCertificationCallback() {
            @Override
            public void onSuccess(InvoiceVerificationResponse response) {
                callback.onSuccess(response);
            }

            @Override
            public void onError(String error) {
                if (currentRetry < maxRetries) {
                    // Wait before retrying (exponential backoff)
                    long delay = (long) Math.pow(2, currentRetry) * 1000; // 1s, 2s, 4s, 8s...
                    TRACE.i(TAG + ": Retry " + (currentRetry + 1) + "/" + maxRetries + " in " + delay + "ms");
                    
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        certifyInvoiceWithRetry(invoiceData, maxRetries, currentRetry + 1, callback);
                    }, delay);
                } else {
                    callback.onError(error);
                }
            }
        });
    }
}