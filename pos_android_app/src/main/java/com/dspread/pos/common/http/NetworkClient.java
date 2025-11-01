package com.dspread.pos.common.http;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class NetworkClient {
    private static final String TAG = "NetworkClient";
    private static final int TIMEOUT_SECONDS = 30;

    private static volatile NetworkClient instance;
    private final OkHttpClient client;
    private final Gson gson;

    private NetworkClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
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
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) {
                cb.onResult(new ApiResult.Error<T>("Connection error - Check your network"));
            }
            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    if (body == null) {
                        cb.onResult(new ApiResult.Error<T>("Empty response from server"));
                        return;
                    }
                    String payload = body.string();
                    if (response.isSuccessful()) {
                        try {
                            T data = gson.fromJson(payload, type);
                            cb.onResult(new ApiResult.Success<T>(data));
                        } catch (JsonSyntaxException ex) {
                            cb.onResult(new ApiResult.Error<T>("Invalid response format"));
                        }
                    } else {
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
}