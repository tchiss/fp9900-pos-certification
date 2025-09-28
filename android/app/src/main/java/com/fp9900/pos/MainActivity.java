package com.fp9900.pos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint;
import com.facebook.react.defaults.DefaultReactActivityDelegate;

public class MainActivity extends ReactActivity {
  private static final String TAG = "MainActivity";

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  @Override
  protected String getMainComponentName() {
    return "FP9900POS";
  }

  /**
   * Returns the instance of the {@link ReactActivityDelegate}. Here we use a util class {@link
   * DefaultReactActivityDelegate} which allows you to easily enable Fabric and Concurrent React
   * (aka React 18) with two boolean flags.
   */
  @Override
  protected ReactActivityDelegate createReactActivityDelegate() {
    return new DefaultReactActivityDelegate(
        this,
        getMainComponentName(),
        // If you opted-in for the New Architecture, we enable the Fabric Renderer.
        DefaultNewArchitectureEntryPoint.getFabricEnabled());
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "MainActivity created");
    
    // Start sync service for background operations
    Intent syncIntent = new Intent(this, SyncService.class);
    startService(syncIntent);
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "MainActivity resumed");
    
    // Initialize printer when activity resumes
    // This ensures printer is ready when user returns to the app
    try {
      // TODO: Initialize printer connection
      // PrinterModule.getInstance().initialize();
    } catch (Exception e) {
      Log.e(TAG, "Failed to initialize printer on resume", e);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    Log.d(TAG, "MainActivity paused");
    
    // Cleanup printer resources when activity is paused
    try {
      // TODO: Cleanup printer connection
      // PrinterModule.getInstance().cleanup();
    } catch (Exception e) {
      Log.e(TAG, "Failed to cleanup printer on pause", e);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "MainActivity destroyed");
    
    // Stop sync service when activity is destroyed
    Intent syncIntent = new Intent(this, SyncService.class);
    stopService(syncIntent);
  }

  @Override
  public void onBackPressed() {
    // Handle back button press
    // In a POS system, we might want to prevent accidental exits
    super.onBackPressed();
  }
}
