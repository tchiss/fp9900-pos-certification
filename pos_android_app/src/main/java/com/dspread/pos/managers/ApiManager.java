package com.dspread.pos.managers;

import android.content.Context;
import android.util.Log;

import com.dspread.pos.models.*;
import com.dspread.pos.utils.TRACE;
import com.dspread.pos.common.http.NetworkClient;
import com.dspread.pos.common.http.ApiResult;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class ApiManager {
    private static final String TAG = "ApiManager";
    // URL selon KPS Access - Normalisation Factures Postman collection
    private static final String BASE_URL = "https://api.fiv.dgi.kpsaccess.com";

    private static volatile ApiManager instance;
    private final NetworkClient net;
    private final com.google.gson.Gson gson;

    private ApiManager(Context context) {
        this.net = NetworkClient.getInstance();
        this.gson = new com.google.gson.Gson();
    }

    public static ApiManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ApiManager.class) {
                if (instance == null) instance = new ApiManager(context.getApplicationContext());
            }
        }
        return instance;
    }

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    // ----------------------------
    // API Calls
    // ----------------------------
    public void createInvoice(InvoiceData invoice, ApiCallback<InvoiceCreationResponse> cb) {
        if (invoice == null || !invoice.isValid()) {
            cb.onError("Invalid invoice data");
            return;
        }
        
        // Recalculate totals to ensure they match invoice lines
        invoice.calculateTotals();
        
        String json = gson.toJson(invoice);
        String url = BASE_URL + "/api/invoices";
        
        // CRITICAL: Log the exact JSON being sent for debugging
        android.util.Log.i(TAG, "===== CREATING INVOICE =====");
        android.util.Log.i(TAG, "URL: " + url);
        android.util.Log.i(TAG, "JSON Body: " + json);
        TRACE.i(TAG + ": ===== CREATING INVOICE =====");
        TRACE.i(TAG + ": URL: " + url);
        TRACE.i(TAG + ": JSON Body: " + json);
        TRACE.i(TAG + ": ExternalNum: " + invoice.getExternalNum());
        TRACE.i(TAG + ": MachineNum: " + invoice.getMachineNum());
        TRACE.i(TAG + ": IssueDate: " + invoice.getIssueDate());
        if (invoice.getIssuer() != null) {
            TRACE.i(TAG + ": Issuer - name: " + invoice.getIssuer().getName() + ", identityNumber: " + invoice.getIssuer().getIdentityNumber() + ", tel: " + invoice.getIssuer().getTel());
        }
        if (invoice.getCustomer() != null) {
            TRACE.i(TAG + ": Customer - name: " + invoice.getCustomer().getName() + ", identityNumber: " + invoice.getCustomer().getIdentityNumber());
        }
        TRACE.i(TAG + ": Totals - HT: " + invoice.getTotalHt() + ", VAT: " + invoice.getTotalVat() + ", TTC: " + invoice.getTotalTtc());
        if (invoice.getInvoiceLines() != null) {
            TRACE.i(TAG + ": InvoiceLines count: " + invoice.getInvoiceLines().size());
            for (int i = 0; i < invoice.getInvoiceLines().size(); i++) {
                com.dspread.pos.models.InvoiceLine line = invoice.getInvoiceLines().get(i);
                TRACE.i(TAG + ":   Line " + i + ": " + line.getDesignation() + 
                       " - Qty: " + line.getQuantity() + 
                       ", Price: " + line.getUnitPrice() + 
                       ", VAT: " + line.getVatRate() + "%" +
                       ", Total: " + line.getTotalPrice() + 
                       ", VAT Amt: " + line.getVatAmount());
            }
        }
        TRACE.i(TAG + ": ============================");
        
        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .addHeader("Accept", "*/*")
                .addHeader("Content-Type", "application/json")
                .build();

        // Some environments return a raw numeric ID instead of JSON. Request as String and normalize.
        net.execute(req, String.class, res -> {
            if (res == null) {
                TRACE.e(TAG + ": createInvoice received null result");
                cb.onError("Null response from server");
                return;
            }
            
            if (res instanceof ApiResult.Success) {
                ApiResult.Success<String> s = (ApiResult.Success<String>) res;
                String payload = s.data();
                if (payload == null || payload.trim().isEmpty()) {
                    TRACE.e(TAG + ": createInvoice received empty payload in success response");
                    cb.onError("Server returned empty response");
                    return;
                }

                String trimmed = payload.trim();
                InvoiceCreationResponse data;
                try {
                    if (trimmed.startsWith("{")) {
                        // JSON object
                        data = gson.fromJson(trimmed, InvoiceCreationResponse.class);
                    } else if (trimmed.matches("\\d+")) {
                        // Plain numeric ID returned
                        data = new InvoiceCreationResponse(trimmed, "CREATED");
                    } else {
                        // Unexpected format
                        TRACE.e(TAG + ": Unexpected createInvoice response format: " + trimmed);
                        cb.onError("Invalid response format from server");
                        return;
                    }
                } catch (Exception ex) {
                    TRACE.e(TAG + ": Error parsing createInvoice response: " + ex.getMessage());
                    cb.onError("Invalid response format: " + ex.getMessage());
                    return;
                }

                if (data == null || data.getInvoiceId() == null || data.getInvoiceId().trim().isEmpty()) {
                    TRACE.e(TAG + ": createInvoice parsed but missing invoice ID");
                    cb.onError("Server returned invalid invoice ID");
                    return;
                }

                TRACE.i(TAG + ": Invoice created successfully - ID: " + data.getInvoiceId());
                cb.onSuccess(data);
            } else if (res instanceof ApiResult.Error) {
                ApiResult.Error<String> e = (ApiResult.Error<String>) res;
                String errorMsg = e.message();
                if (errorMsg == null || errorMsg.trim().isEmpty()) {
                    errorMsg = "Unknown error occurred";
                }
                TRACE.e(TAG + ": createInvoice error: " + errorMsg);
                cb.onError(errorMsg);
            } else {
                TRACE.e(TAG + ": createInvoice received unknown result type: " + res.getClass().getName());
                cb.onError("Unexpected response format");
            }
        });
    }

    public void fiscalizeInvoice(String invoiceId, ApiCallback<FiscalizationResponse> cb) {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/invoices/" + invoiceId + "/fiscalize")
                .post(RequestBody.create("", null))
                .addHeader("Accept", "*/*")
                .build();

        net.execute(req, FiscalizationResponse.class, res -> {
            if (res instanceof ApiResult.Success) {
                ApiResult.Success<FiscalizationResponse> s = (ApiResult.Success<FiscalizationResponse>) res;
                cb.onSuccess(s.data());
            } else if (res instanceof ApiResult.Error) {
                ApiResult.Error<FiscalizationResponse> e = (ApiResult.Error<FiscalizationResponse>) res;
                cb.onError(e.message());
            }
        });
    }

    public void verifyInvoice(String invoiceId, ApiCallback<InvoiceVerificationResponse> cb) {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/invoices/" + invoiceId)
                .get()
                .addHeader("Accept", "*/*")
                .build();

        net.execute(req, InvoiceVerificationResponse.class, res -> {
            if (res instanceof ApiResult.Success) {
                ApiResult.Success<InvoiceVerificationResponse> s = (ApiResult.Success<InvoiceVerificationResponse>) res;
                cb.onSuccess(s.data());
            } else if (res instanceof ApiResult.Error) {
                ApiResult.Error<InvoiceVerificationResponse> e = (ApiResult.Error<InvoiceVerificationResponse>) res;
                cb.onError(e.message());
            }
        });
    }

    public void getInvoicePdf(String invoiceId, ApiCallback<byte[]> cb) {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/invoices/" + invoiceId + "/pdf")
                .get()
                .addHeader("Accept", "*/*")
                .build();

        // Use raw execution for bytes: reuse NetworkClient at low level by duplicating pattern
        // For simplicity, we call OkHttp directly here through NetworkClient's client access (not exposed).
        // Instead, we temporarily implement a small in-place call:
        new okhttp3.OkHttpClient().newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                cb.onError("Connection error - Check your network");
            }
            @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                try (okhttp3.ResponseBody body = response.body()) {
                    if (body == null) { cb.onError("Empty response"); return; }
                    if (response.isSuccessful()) cb.onSuccess(body.bytes());
                    else cb.onError("HTTP " + response.code() + ": " + body.string());
                } catch (Exception ex) {
                    cb.onError("Error reading PDF: " + ex.getMessage());
                }
            }
        });
    }

    public void checkApiHealth(ApiCallback<Boolean> cb) {
        Request req = new Request.Builder()
                .url(BASE_URL + "/actuator/health")
                .get()
                .build();
        new okhttp3.OkHttpClient().newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                cb.onError("Unable to contact DGI API");
            }
            @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                try { cb.onSuccess(response.isSuccessful()); }
                finally { response.close(); }
            }
        });
    }

    // ----------------------------
    // Testing endpoints
    // ----------------------------

    public void getTestingIdentities(ApiCallback<String> cb) {
        Request req = new Request.Builder()
                .url(BASE_URL + "/testing/identities")
                .get()
                .addHeader("Accept", "*/*")
                .build();

        net.execute(req, String.class, res -> {
            if (res instanceof ApiResult.Success) {
                ApiResult.Success<String> s = (ApiResult.Success<String>) res;
                cb.onSuccess(s.data());
            } else if (res instanceof ApiResult.Error) {
                ApiResult.Error<String> e = (ApiResult.Error<String>) res;
                cb.onError(e.message());
            }
        });
    }

    public void getTestingMachines(ApiCallback<String> cb) {
        Request req = new Request.Builder()
                .url(BASE_URL + "/testing/machines")
                .get()
                .addHeader("Accept", "*/*")
                .build();

        net.execute(req, String.class, res -> {
            if (res instanceof ApiResult.Success) {
                ApiResult.Success<String> s = (ApiResult.Success<String>) res;
                cb.onSuccess(s.data());
            } else if (res instanceof ApiResult.Error) {
                ApiResult.Error<String> e = (ApiResult.Error<String>) res;
                cb.onError(e.message());
            }
        });
    }

    // ----------------------------
    // Invoice Synchronization (Offline Mode)
    // ----------------------------
    /**
     * Synchronize an offline-signed invoice to the DGI API
     * Sends enriched payload with hash, signature, chain hash, and sequence number
     * 
     * @param invoiceEntity The invoice entity with signature data
     * @param cb Callback for sync result
     */
    public void syncInvoice(com.dspread.pos.db.InvoiceEntity invoiceEntity, ApiCallback<SyncInvoiceResponse> cb) {
        if (invoiceEntity == null) {
            cb.onError("Invoice entity cannot be null");
            return;
        }

        try {
            // Get device ID
            com.dspread.pos.utils.DeviceIdManager deviceIdManager = com.dspread.pos.utils.DeviceIdManager.getInstance();
            String deviceId = deviceIdManager.getDeviceId();

            // Parse payload JSON to Object
            Object payloadObject = gson.fromJson(invoiceEntity.payload, Object.class);

            // Create sync request
            SyncInvoiceRequest request = new SyncInvoiceRequest(
                deviceId,
                payloadObject,
                invoiceEntity.hash,
                invoiceEntity.signature,
                invoiceEntity.prevHash,
                invoiceEntity.seqNo,
                invoiceEntity.createdAt
            );

            String json = gson.toJson(request);
            String url = BASE_URL + "/api/invoices/sync";

            TRACE.i(TAG + ": ===== SYNCING INVOICE =====");
            TRACE.i(TAG + ": URL: " + url);
            TRACE.i(TAG + ": Invoice ID: " + invoiceEntity.id);
            TRACE.i(TAG + ": Hash: " + invoiceEntity.hash);
            TRACE.i(TAG + ": SeqNo: " + invoiceEntity.seqNo);
            TRACE.i(TAG + ": PrevHash: " + invoiceEntity.prevHash);
            TRACE.i(TAG + ": JSON Body: " + json);

            Request req = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .addHeader("Accept", "*/*")
                    .addHeader("Content-Type", "application/json")
                    .build();

            net.execute(req, String.class, res -> {
                if (res == null) {
                    TRACE.e(TAG + ": syncInvoice received null result");
                    cb.onError("Null response from server");
                    return;
                }

                if (res instanceof ApiResult.Success) {
                    ApiResult.Success<String> s = (ApiResult.Success<String>) res;
                    String payload = s.data();
                    if (payload == null || payload.trim().isEmpty()) {
                        TRACE.e(TAG + ": syncInvoice received empty payload in success response");
                        cb.onError("Server returned empty response");
                        return;
                    }

                    String trimmed = payload.trim();
                    SyncInvoiceResponse data;
                    try {
                        if (trimmed.startsWith("{")) {
                            // JSON object
                            data = gson.fromJson(trimmed, SyncInvoiceResponse.class);
                        } else {
                            // Unexpected format - create a basic success response
                            TRACE.w(TAG + ": Unexpected syncInvoice response format, treating as success");
                            data = new SyncInvoiceResponse(invoiceEntity.id, "CERTIFIED", "Invoice synced successfully");
                        }
                    } catch (Exception ex) {
                        TRACE.e(TAG + ": Error parsing syncInvoice response: " + ex.getMessage());
                        // Treat as success if we can't parse (API might return simple success)
                        data = new SyncInvoiceResponse(invoiceEntity.id, "CERTIFIED", "Invoice synced successfully");
                    }

                    TRACE.i(TAG + ": Invoice synced successfully - ID: " + (data.getInvoiceId() != null ? data.getInvoiceId() : invoiceEntity.id));
                    cb.onSuccess(data);
                } else if (res instanceof ApiResult.Error) {
                    ApiResult.Error<String> e = (ApiResult.Error<String>) res;
                    String errorMsg = e.message();
                    if (errorMsg == null || errorMsg.trim().isEmpty()) {
                        errorMsg = "Unknown error occurred";
                    }
                    TRACE.e(TAG + ": syncInvoice error: " + errorMsg);
                    cb.onError(errorMsg);
                } else {
                    TRACE.e(TAG + ": syncInvoice received unknown result type: " + res.getClass().getName());
                    cb.onError("Unexpected response format");
                }
            });
        } catch (Exception e) {
            TRACE.e(TAG + ": Error preparing sync request: " + e.getMessage());
            cb.onError("Error preparing sync request: " + e.getMessage());
        }
    }

    // ----------------------------
    // Orchestration
    // ----------------------------
    /**
     * Certify invoice: Create → Fiscalize (qui renvoie directement QR code et token)
     * Pas besoin de verify - fiscalize renvoie directement l'objet complet
     */
    public void certifyInvoice(InvoiceData invoice, ApiCallback<InvoiceVerificationResponse> cb) {
        // Validate input
        if (invoice == null) {
            cb.onError("Invoice data cannot be null");
            return;
        }
        
        if (cb == null) {
            TRACE.e(TAG + ": certifyInvoice called with null callback");
            return;
        }
        
        createInvoice(invoice, new ApiCallback<InvoiceCreationResponse>() {
            @Override public void onSuccess(InvoiceCreationResponse creation) {
                if (creation == null) {
                    cb.onError("Creation succeeded but response is null");
                    return;
                }
                
                String id = creation.getInvoiceId();
                if (id == null || id.trim().isEmpty()) {
                    cb.onError("Invoice ID is missing in creation response");
                    return;
                }
                
                TRACE.i(TAG + ": Invoice created with ID: " + id);
                
                // Fiscalize renvoie directement l'objet avec QR code et token
                fiscalizeInvoice(id, new ApiCallback<FiscalizationResponse>() {
                    @Override public void onSuccess(FiscalizationResponse fiscalized) {
                        if (fiscalized == null) {
                            cb.onError("Fiscalization succeeded but response is null");
                            return;
                        }
                        
                        if (!fiscalized.isSuccess()) {
                            cb.onError("Fiscalization failed: " + (fiscalized.getMessage() != null ? fiscalized.getMessage() : "Unknown error"));
                            return;
                        }
                        
                        TRACE.i(TAG + ": Invoice fiscalized successfully - Token: " + 
                               (fiscalized.getToken() != null ? fiscalized.getToken().substring(0, Math.min(20, fiscalized.getToken().length())) + "..." : "null") +
                               ", QR Code: " + (fiscalized.getQrBase64() != null ? "present" : "null"));
                        
                        // Convertir FiscalizationResponse en InvoiceVerificationResponse
                        InvoiceVerificationResponse verificationResponse = new InvoiceVerificationResponse();
                        verificationResponse.setInvoiceId(id);
                        verificationResponse.setExternalNum(invoice.getExternalNum());
                        verificationResponse.setMachineNum(invoice.getMachineNum());
                        verificationResponse.setIssuer(invoice.getIssuer());
                        verificationResponse.setCustomer(invoice.getCustomer());
                        verificationResponse.setInvoiceLines(invoice.getInvoiceLines());
                        verificationResponse.setTotalHt(invoice.getTotalHt());
                        verificationResponse.setTotalVat(invoice.getTotalVat());
                        verificationResponse.setTotalTtc(invoice.getTotalTtc());
                        verificationResponse.setIssueDate(invoice.getIssueDate());
                        verificationResponse.setFiscalizationDate(fiscalized.getCertifiedAt());
                        verificationResponse.setStatus("FISCALIZED");
                        verificationResponse.setQrCode(fiscalized.getQrBase64()); // QR code en base64
                        verificationResponse.setMecefCode(fiscalized.getToken()); // Le token sert de code MECEF
                        
                        cb.onSuccess(verificationResponse);
                    }
                    @Override public void onError(String error) {
                        TRACE.e(TAG + ": Fiscalization failed: " + error);
                        cb.onError("Fiscalization failed: " + error);
                    }
                });
            }
            @Override public void onError(String error) {
                TRACE.e(TAG + ": Creation failed: " + error);
                cb.onError("Creation failed: " + error);
            }
        });
    }
}