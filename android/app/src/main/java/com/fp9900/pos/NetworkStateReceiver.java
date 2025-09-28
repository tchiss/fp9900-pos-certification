package com.fp9900.pos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.facebook.react.HeadlessJsTaskService;

/**
 * Broadcast receiver for network state changes
 * Triggers sync when network becomes available
 */
public class NetworkStateReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkStateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Network state changed");
        
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = networkInfo != null && networkInfo.isConnected();
            
            Log.d(TAG, "Network connected: " + isConnected);
            
            if (isConnected) {
                // Network is available, trigger sync
                triggerSync(context);
            }
        }
    }

    private void triggerSync(Context context) {
        try {
            // Start the sync service
            Intent syncIntent = new Intent(context, SyncService.class);
            context.startService(syncIntent);
            
            Log.d(TAG, "Sync triggered due to network availability");
        } catch (Exception e) {
            Log.e(TAG, "Failed to trigger sync", e);
        }
    }
}
