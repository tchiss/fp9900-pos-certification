package com.dspread.pos.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.dspread.pos.utils.TRACE;

import net.sqlcipher.database.SupportFactory;

import java.security.SecureRandom;

/**
 * Room database with SQLCipher encryption support
 * Singleton pattern for database access
 */
@Database(entities = {InvoiceEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    private static final String DATABASE_NAME = "dgi_invoices.db";
    private static final String PREFS_NAME = "db_prefs";
    private static final String KEY_DB_PASSWORD = "db_password";
    
    private static volatile AppDatabase instance;
    private static String databasePassword;

    public abstract InvoiceDao invoiceDao();

    /**
     * Get singleton instance
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = create(context);
        }
        return instance;
    }

    /**
     * Create database instance with SQLCipher encryption
     */
    private static AppDatabase create(Context context) {
        Context appContext = context.getApplicationContext();
        
        // Get or generate database password
        databasePassword = getOrGeneratePassword(appContext);
        
        // Create SQLCipher factory
        byte[] key = databasePassword.getBytes();
        SupportFactory factory = new SupportFactory(key);
        
        TRACE.i(TAG + ": Creating encrypted database: " + DATABASE_NAME);
        
        return Room.databaseBuilder(appContext, AppDatabase.class, DATABASE_NAME)
                .openHelperFactory(factory)
                .allowMainThreadQueries() // Allow main thread queries for synchronous operations
                .build();
    }

    /**
     * Get existing password or generate a new one
     * Password is stored in SharedPreferences (encrypted by Android system)
     */
    private static String getOrGeneratePassword(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String password = prefs.getString(KEY_DB_PASSWORD, null);

        if (password == null || password.isEmpty()) {
            // Generate a secure random password
            SecureRandom random = new SecureRandom();
            byte[] passwordBytes = new byte[32];
            random.nextBytes(passwordBytes);
            password = android.util.Base64.encodeToString(passwordBytes, android.util.Base64.NO_WRAP);
            
            prefs.edit().putString(KEY_DB_PASSWORD, password).apply();
            TRACE.i(TAG + ": Generated new database password");
        } else {
            TRACE.i(TAG + ": Using existing database password");
        }

        return password;
    }

    /**
     * Close database instance (for testing/cleanup)
     */
    public static synchronized void closeInstance() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}

