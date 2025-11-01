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
        String json = gson.toJson(invoice);
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/invoices")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .addHeader("Accept", "*/*")
                .build();

        net.execute(req, InvoiceCreationResponse.class, res -> {
            if (res instanceof ApiResult.Success) {
                ApiResult.Success<InvoiceCreationResponse> s = (ApiResult.Success<InvoiceCreationResponse>) res;
                cb.onSuccess(s.data());
            } else if (res instanceof ApiResult.Error) {
                ApiResult.Error<InvoiceCreationResponse> e = (ApiResult.Error<InvoiceCreationResponse>) res;
                cb.onError(e.message());
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
    // Orchestration
    // ----------------------------
    public void certifyInvoice(InvoiceData invoice, ApiCallback<InvoiceVerificationResponse> cb) {
        createInvoice(invoice, new ApiCallback<InvoiceCreationResponse>() {
            @Override public void onSuccess(InvoiceCreationResponse creation) {
                String id = creation.getInvoiceId();
                fiscalizeInvoice(id, new ApiCallback<FiscalizationResponse>() {
                    @Override public void onSuccess(FiscalizationResponse fiscalized) {
                        verifyInvoice(id, new ApiCallback<InvoiceVerificationResponse>() {
                            @Override public void onSuccess(InvoiceVerificationResponse result) {
                                cb.onSuccess(result);
                            }
                            @Override public void onError(String error) {
                                cb.onError("Verification failed: " + error);
                            }
                        });
                    }
                    @Override public void onError(String error) {
                        cb.onError("Fiscalization failed: " + error);
                    }
                });
            }
            @Override public void onError(String error) {
                cb.onError("Creation failed: " + error);
            }
        });
    }
}