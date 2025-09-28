package com.fp9900.pos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

/**
 * Background service for syncing pending invoices
 * Runs when the app is in background or device is idle
 */
public class SyncService extends Service {
    private static final String TAG = "SyncService";
    private static final String CHANNEL_ID = "sync_service_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.d(TAG, "SyncService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SyncService started");
        
        // Create foreground notification
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
        
        // Start the sync task
        startSyncTask();
        
        // Return START_STICKY to restart service if killed
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SyncService destroyed");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Invoice Sync Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background invoice synchronization service");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Invoice Sync")
            .setContentText("Synchronizing pending invoices...")
            .setSmallIcon(R.drawable.ic_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void startSyncTask() {
        try {
            // Create a HeadlessJsTaskService to run JavaScript code
            Intent taskIntent = new Intent(this, SyncTaskService.class);
            taskIntent.putExtra("taskName", "syncPendingInvoices");
            
            // Pass any necessary data
            WritableMap data = Arguments.createMap();
            data.putString("reason", "background_sync");
            taskIntent.putExtra("data", data.toString());
            
            startService(taskIntent);
            
            Log.d(TAG, "Sync task started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start sync task", e);
        }
    }
}
