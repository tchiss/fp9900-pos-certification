package com.fp9900.pos;

import android.content.Intent;
import android.util.Log;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

/**
 * Headless JS Task Service for running JavaScript sync tasks in background
 */
public class SyncTaskService extends HeadlessJsTaskService {
    private static final String TAG = "SyncTaskService";

    @Override
    protected String getTaskConfig(Intent intent) {
        return intent.getStringExtra("taskName");
    }

    @Override
    protected WritableMap getTaskConfig(Intent intent, String taskName) {
        WritableMap config = Arguments.createMap();
        
        // Set timeout for the task (in milliseconds)
        config.putInt("timeout", 60000); // 1 minute
        
        // Add any additional configuration
        String data = intent.getStringExtra("data");
        if (data != null) {
            config.putString("data", data);
        }
        
        Log.d(TAG, "Starting headless task: " + taskName);
        return config;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SyncTaskService created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SyncTaskService destroyed");
    }
}
