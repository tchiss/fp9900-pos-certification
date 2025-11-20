package com.dspread.pos;

import android.content.Context;
import android.os.Build;


import com.dspread.pos.common.manager.FragmentCacheManager;
import com.dspread.pos.posAPI.POSManager;
import com.dspread.pos.ui.main.MainActivity;
import com.dspread.pos.utils.DevUtils;
import com.dspread.pos.utils.TRACE;
import com.dspread.pos.db.AppDatabase;
import com.dspread.pos.managers.ApiManager;
import com.dspread.pos.managers.PrinterManager;
import com.dspread.pos.managers.StorageManager;
import com.dspread.pos.security.KeyManager;
import com.dspread.pos.sync.InvoiceSyncWorker;
import com.dspread.pos.utils.DeviceIdManager;
import com.dspread.pos.utils.MigrationHelper;
import com.dspread.pos_android_app.BuildConfig;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;
import com.dspread.pos_android_app.R;
import com.tencent.bugly.crashreport.BuglyLog;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.upgrade.bean.UpgradeConfig;
import com.tencent.upgrade.core.UpgradeManager;

import me.goldze.mvvmhabit.base.BaseApplication;
import me.goldze.mvvmhabit.crash.CaocConfig;


/**
 * @author user
 */
public class TerminalApplication extends BaseApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        
        // MINIMAL initialization - only what's absolutely necessary
        TRACE.setContext(this);
        
        // STEP 2: Test ApiManager + StorageManager initialization (SYNCHRONOUS)
        TRACE.i("TerminalApplication: Testing ApiManager + StorageManager initialization (SYNC)");
        
        try {
            // Initialize managers synchronously to avoid timing issues
            // ApiManager now uses singleton pattern with context
            TRACE.i("ApiManager will be initialized on first use");
            
            // Initialize DeviceIdManager (needed for sync)
            DeviceIdManager.initialize(TerminalApplication.this);
            TRACE.i("DeviceIdManager initialized successfully");
            
            // Initialize KeyManager for ECDSA signing
            KeyManager.initialize(TerminalApplication.this);
            TRACE.i("KeyManager initialized successfully");
            
            // Initialize AppDatabase with SQLCipher
            AppDatabase.getInstance(TerminalApplication.this);
            TRACE.i("AppDatabase initialized successfully");
            
            // Run migration from SharedPreferences to Room (if needed)
            new Thread(() -> {
                try {
                    int migratedCount = MigrationHelper.migrateSharedPreferencesToRoom(TerminalApplication.this);
                    if (migratedCount > 0) {
                        TRACE.i("TerminalApplication: Migrated " + migratedCount + " invoice(s) from SharedPreferences to Room");
                    }
                } catch (Exception e) {
                    TRACE.e("TerminalApplication: Error during migration: " + e.getMessage());
                }
            }).start();
            
            StorageManager.initialize(TerminalApplication.this);
            TRACE.i("StorageManager initialized successfully");
            
            // Initialize POSManager to prevent crash in MainActivity.onDestroy()
            POSManager.init(TerminalApplication.this);
            TRACE.i("POSManager initialized successfully");
            
            // Initialize PrinterManager
            PrinterManager.initialize(TerminalApplication.this);
            TRACE.i("PrinterManager initialized successfully");
            
            // Initialize WorkManager for periodic sync
            initializeWorkManager();
            TRACE.i("WorkManager initialized successfully");
            
            // Verify that instances are accessible
            ApiManager apiManager = ApiManager.getInstance(TerminalApplication.this);
            StorageManager storageManager = StorageManager.getInstance();
            
            TRACE.i("TerminalApplication: All managers initialized and verified successfully");
            TRACE.i("TerminalApplication: ApiManager instance: " + (apiManager != null ? "OK" : "NULL"));
            TRACE.i("TerminalApplication: StorageManager instance: " + (storageManager != null ? "OK" : "NULL"));
        } catch (Exception e) {
            TRACE.e("Error initializing managers: " + e.getMessage());
        }
        
        // TODO: Re-enable other managers after confirming ApiManager doesn't cause ANR
        /*
        initCrash();
        new Thread(() -> {
            try {
                initBugly();
                initShiply();
                FragmentCacheManager.getInstance();
                POSManager.init(TerminalApplication.this);
                // StorageManager.initialize(TerminalApplication.this);
                // PrinterManager.initialize(TerminalApplication.this);
            } catch (Exception e) {
                TRACE.e("Error in background initialization: " + e.getMessage());
            }
        }).start();
        */
    }

    private void initCrash() {
        CaocConfig.Builder.create()
                .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT) //Background mode, activate immersive mode
                .enabled(true) //Do you want to initiate global exception capture
                .showErrorDetails(true) //Whether to display detailed error information
                .showRestartButton(true) //Is the restart button displayed
                .trackActivities(true) //Whether to track Activity
                .minTimeBetweenCrashesMs(2000) //Interval between crashes (milliseconds)
                .errorDrawable(R.mipmap.szfp) //error icon
                .restartActivity(MainActivity.class) //Activity after restart
//                .errorActivity(YourCustomErrorActivity.class) //Error activity after crash
//                .eventListener(new YourCustomEventListener()) //Error listening after crash
                .apply();
    }

    private void initBugly() {
        Context context = getApplicationContext();
        // Get the current package name
        String packageName = context.getPackageName();
        // Get the current process name
        String processName = DevUtils.getProcessName(android.os.Process.myPid());
        // Set whether it is a reporting process
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(context);
        strategy.setUploadProcess(processName == null || processName.equals(packageName));
        strategy.setAppVersion(DevUtils.getPackageVersionName(this, packageName));
        strategy.setAppPackageName(packageName);

        // Initialize Bugly
        CrashReport.initCrashReport(context, "b2d80aa171", BuildConfig.DEBUG, strategy);
        BuglyLog.setCache(1024 * 10); // 设置本地缓存大小(10KB)

        // Set user data
        CrashReport.setUserId(DevUtils.getDeviceId(this));
        CrashReport.setDeviceModel(this,Build.MODEL);

        // Add custom logs
        CrashReport.setUserSceneTag(context, 9527); // Set label
        CrashReport.putUserData(context, "deviceModel", Build.MODEL);
        CrashReport.putUserData(context, "deviceManufacturer", Build.MANUFACTURER);
    }

    private void initShiply(){
        String appId = "6316d5169f"; // The appid of the Android product applied for on the front-end page of Shiply
        String appKey = "ffe00435-2389-4189-bd87-4b30ffcaff8e"; // The appkey for Android products applied for on the front-end page of Shiply
        UpgradeConfig.Builder builder = new UpgradeConfig.Builder();
        UpgradeConfig config = builder.appId(appId).appKey(appKey).build();
        UpgradeManager.getInstance().init(this, config);
//        Map<String, String> map = new HashMap<>();
//        map.put("UserGender", "Male");
//        builder.systemVersion(String.valueOf(Build.VERSION.SDK_INT))    // The user's mobile system version is used to match the system version distribution conditions set when creating tasks in the Shiply frontend
////                .customParams(map)                                      // Custom attribute key value pairs are used to match the custom distribution conditions set when creating tasks in the shiply frontend
//                .cacheExpireTime(1000 * 60 * 60 * 6)                    // The cache duration of the grayscale strategy (ms), if not set, defaults to 1 day
////                .internalInitMMKVForRDelivery(true)                     // Is mmkv initialized internally by the SDK (calling MMKV. initializes)? If the business has already initialized mmkv, it can be set to false
////                .userId("xxx")                                          // User ID, used to match the experience list in the tasks created by Shiply frontend and the user number package in the distribution conditions
//                .customLogger(new TRACE());// Log implementation interface, it is recommended to connect to the log interface of the business side for easy troubleshooting
        builder.cacheExpireTime(1000 * 60 * 60 * 6)
                .customLogger(new TRACE());
    }

    private void initializeManagers() {
        try {
            // ApiManager now uses singleton pattern with context
            TRACE.i("ApiManager will be initialized on first use");
            
            // Initialize Printer Manager for thermal printing
            PrinterManager.initialize(TerminalApplication.this);
            TRACE.i("PrinterManager initialized successfully");
            
            // Initialize Storage Manager for offline sync
            StorageManager.initialize(TerminalApplication.this);
            TRACE.i("StorageManager initialized successfully");
            
        } catch (Exception e) {
            TRACE.e("Error initializing managers: " + e.getMessage());
        }
    }

    /**
     * Initialize WorkManager for periodic invoice synchronization
     */
    private void initializeWorkManager() {
        try {
            // Create constraints: require network connection
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            // Create periodic work request (every 15-30 minutes)
            PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                    InvoiceSyncWorker.class,
                    15, // Repeat interval
                    TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build();

            // Enqueue unique periodic work to prevent duplicates
            WorkManager.getInstance(TerminalApplication.this).enqueueUniquePeriodicWork(
                    "invoice_sync_worker",
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    syncWorkRequest);
            
            TRACE.i("TerminalApplication: InvoiceSyncWorker scheduled (every 15 minutes)");
        } catch (Exception e) {
            TRACE.e("TerminalApplication: Error initializing WorkManager: " + e.getMessage());
        }
    }
}
