package com.dspread.pos.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.dspread.pos.utils.TRACE;

import java.util.UUID;

/**
 * Manages device ID (UUID) for invoice synchronization
 * Device ID is persisted in SharedPreferences and used for all sync operations
 */
public class DeviceIdManager {
    private static final String TAG = "DeviceIdManager";
    private static final String PREFS_NAME = "device_prefs";
    private static final String KEY_DEVICE_ID = "device_id";
    
    private static DeviceIdManager instance;
    private String deviceId;
    private Context context;

    private DeviceIdManager(Context context) {
        this.context = context.getApplicationContext();
        loadOrGenerateDeviceId();
    }

    /**
     * Initialize DeviceIdManager
     */
    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new DeviceIdManager(context);
        }
    }

    /**
     * Get singleton instance
     */
    public static synchronized DeviceIdManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DeviceIdManager not initialized. Call initialize() first.");
        }
        return instance;
    }

    /**
     * Get the device ID (UUID)
     * Generates a new UUID if none exists
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Load device ID from SharedPreferences or generate a new one
     */
    private void loadOrGenerateDeviceId() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        deviceId = prefs.getString(KEY_DEVICE_ID, null);

        if (deviceId == null || deviceId.trim().isEmpty()) {
            // Generate new UUID
            deviceId = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
            TRACE.i(TAG + ": Generated new device ID: " + deviceId);
        } else {
            TRACE.i(TAG + ": Loaded existing device ID: " + deviceId);
        }
    }

    /**
     * Reset device ID (for testing purposes)
     * WARNING: This should only be used in development/testing
     */
    public void resetDeviceId() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_DEVICE_ID).apply();
        deviceId = UUID.randomUUID().toString();
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
        TRACE.i(TAG + ": Reset device ID to: " + deviceId);
    }
}

