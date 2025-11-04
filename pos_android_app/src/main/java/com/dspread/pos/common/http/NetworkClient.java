package com.dspread.pos.common.http;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import okhttp3.*;
import okio.Buffer;
import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public final class NetworkClient {
    private static final String TAG = "NetworkClient";
    private static final int TIMEOUT_SECONDS = 30;

    private static volatile NetworkClient instance;
    private final OkHttpClient client;
    private final Gson gson;

    private NetworkClient() {
        // Create a trust manager that accepts all certificates
        // WARNING: Only for development/testing. In production, use proper certificate validation.
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    // Trust all certificates - development only
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
        };

        // Create SSL context
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (Exception e) {
            Log.e(TAG, "Failed to create SSL context", e);
            sslContext = null;
        }

        // Build OkHttpClient with custom SSL socket factory
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        if (sslContext != null) {
            clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                        .hostnameVerifier((hostname, session) -> true); // Accept all hostnames
        }
        
        this.client = clientBuilder.build();
        this.gson = new Gson();
    }

    public static NetworkClient getInstance() {
        if (instance == null) {
            synchronized (NetworkClient.class) {
                if (instance == null) instance = new NetworkClient();
            }
        }
        return instance;
    }

    public <T> void execute(Request request, Class<T> type, Callback<T> cb) {
        String url = request.url().toString();
        String method = request.method();
        String requestBodyPreview = getRequestBodyPreview(request);
        Log.i(TAG, "Executing request: " + method + " " + url);
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) {
                String errorMsg = getDetailedErrorMessage(e, url);
                Log.e(TAG, "Request failed: " + method + " " + url + ": " + e.getMessage(), e);
                if (requestBodyPreview != null) {
                    Log.e(TAG, "Request body: " + requestBodyPreview);
                }
                cb.onResult(new ApiResult.Error<T>(errorMsg));
            }
            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    if (body == null) {
                        cb.onResult(new ApiResult.Error<T>("Empty response from server"));
                        return;
                    }
                    String payload = body.string();
                    if (response.isSuccessful()) {
                        // Log response for debugging
                        Log.i(TAG, "Response body: " + (payload != null && payload.length() > 500 
                            ? payload.substring(0, 500) + "..." : payload));
                        try {
                            T data = gson.fromJson(payload, type);
                            cb.onResult(new ApiResult.Success<T>(data));
                        } catch (JsonSyntaxException ex) {
                            Log.e(TAG, "JSON parsing error: " + ex.getMessage() + ", Response: " + payload);
                            cb.onResult(new ApiResult.Error<T>("Invalid response format: " + ex.getMessage()));
                        }
                    } else {
                        // Log error response for debugging - ALWAYS show body for 400 errors
                        String bodyPreview = payload != null && payload.length() > 0 
                            ? (payload.length() > 500 ? payload.substring(0, 500) + "..." : payload)
                            : "(empty)";
                        Log.e(TAG, "HTTP " + response.code() + " error for: " + method + " " + url);
                        if (requestBodyPreview != null) {
                            Log.e(TAG, "Request body: " + requestBodyPreview);
                        }
                        Log.e(TAG, "Response body: " + bodyPreview);
                        if (response.code() == 400) {
                            // For 400 errors, always log full body for debugging
                            Log.e(TAG, "HTTP 400 Full response body: " + (payload != null ? payload : "(null)"));
                        }
                        String msg = ErrorHandler.map(response.code(), payload);
                        cb.onResult(new ApiResult.Error<T>(msg, response.code()));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error: " + e.getMessage());
                    cb.onResult(new ApiResult.Error<T>("Unexpected error: " + e.getMessage()));
                }
            }
        });
    }

    public interface Callback<T> {
        void onResult(ApiResult<T> result);
    }
    
    /**
     * Génère un message d'erreur détaillé basé sur le type d'IOException
     */
    private String getDetailedErrorMessage(IOException e, String url) {
        String errorType = e.getClass().getSimpleName();
        String errorMessage = e.getMessage();
        
        if (errorMessage == null) {
            errorMessage = "Unknown error";
        }
        
        // Détecter différents types d'erreurs réseau
        if (errorMessage.contains("Unable to resolve host") || 
            errorMessage.contains("No address associated with hostname")) {
            return "DNS resolution failed - Cannot resolve host. Check your internet connection and DNS settings.";
        } else if (errorMessage.contains("timeout") || 
                   errorMessage.contains("Read timed out") ||
                   errorMessage.contains("Connect timed out")) {
            return "Connection timeout - Server took too long to respond. Check your internet connection.";
        } else if (errorMessage.contains("SSL") || 
                   errorMessage.contains("certificate") ||
                   errorMessage.contains("handshake") ||
                   errorMessage.contains("CertPathValidatorException") ||
                   errorMessage.contains("Trust anchor")) {
            return "SSL/TLS certificate error - The server certificate could not be verified. This may be due to an expired certificate, self-signed certificate, or network security configuration. URL: " + url;
        } else if (errorMessage.contains("Network is unreachable") ||
                   errorMessage.contains("No route to host")) {
            return "Network unreachable - Cannot reach the server. Check your internet connection.";
        } else if (errorMessage.contains("Connection refused")) {
            return "Connection refused - Server rejected the connection. URL: " + url;
        } else {
            // Message générique mais avec plus de détails
            return "Connection error - " + errorType + ": " + errorMessage + " (URL: " + url + ")";
        }
    }

    /**
     * Safely renders a preview of the request body for logging.
     */
    private String getRequestBodyPreview(Request request) {
        RequestBody body = request.body();
        if (body == null) {
            return null;
        }
        try {
            Buffer buffer = new Buffer();
            body.writeTo(buffer);
            String content = buffer.readUtf8();
            if (content == null || content.isEmpty()) {
                return "(empty)";
            }
            int limit = 2000;
            return content.length() > limit ? content.substring(0, limit) + "..." : content;
        } catch (Exception ex) {
            return "(unable to read body: " + ex.getMessage() + ")";
        }
    }
}