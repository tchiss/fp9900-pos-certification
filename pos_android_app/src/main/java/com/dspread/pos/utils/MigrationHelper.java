package com.dspread.pos.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.dspread.pos.db.AppDatabase;
import com.dspread.pos.db.InvoiceDao;
import com.dspread.pos.db.InvoiceEntity;
import com.dspread.pos.managers.StorageManager;
import com.dspread.pos.models.InvoiceData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

/**
 * Helper class for migrating invoices from SharedPreferences to Room database
 * Reconstructs hash, prevHash, and seqNo to maintain chain integrity
 */
public class MigrationHelper {
    private static final String TAG = "MigrationHelper";
    private static final String PREFS_NAME = "pos_storage";
    private static final String KEY_MIGRATION_COMPLETE = "migration_to_room_complete";
    private static final String KEY_PENDING_INVOICES = "pending_invoices";

    /**
     * Migrate all invoices from SharedPreferences to Room database
     * This should be called once at app startup
     * 
     * @param context Application context
     * @return Number of invoices migrated
     */
    public static int migrateSharedPreferencesToRoom(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Check if migration already completed
        if (prefs.getBoolean(KEY_MIGRATION_COMPLETE, false)) {
            TRACE.i(TAG + ": Migration already completed, skipping");
            return 0;
        }

        TRACE.i(TAG + ": Starting migration from SharedPreferences to Room");

        try {
            AppDatabase database = AppDatabase.getInstance(context);
            InvoiceDao invoiceDao = database.invoiceDao();

            // Get pending invoices from SharedPreferences
            Gson gson = new Gson();
            String json = prefs.getString(KEY_PENDING_INVOICES, "[]");
            Type listType = new TypeToken<List<InvoiceData>>(){}.getType();
            List<InvoiceData> pendingInvoices = gson.fromJson(json, listType);

            if (pendingInvoices == null || pendingInvoices.isEmpty()) {
                TRACE.i(TAG + ": No invoices to migrate");
                markMigrationComplete(prefs);
                return 0;
            }

            TRACE.i(TAG + ": Found " + pendingInvoices.size() + " invoice(s) to migrate");

            // Get last invoice from Room to continue the chain
            InvoiceEntity lastInvoice = invoiceDao.getLastInvoice();
            long nextSeqNo = (lastInvoice != null) ? lastInvoice.seqNo + 1 : 1;
            String prevHash = (lastInvoice != null) ? lastInvoice.hash : "GENESIS";

            int migratedCount = 0;

            // Migrate each invoice
            for (InvoiceData invoiceData : pendingInvoices) {
                try {
                    // Generate canonical JSON
                    String canonicalJson = CanonicalJsonUtil.toCanonicalJson(invoiceData);

                    // Calculate SHA-256 hash
                    String hash = calculateSHA256(canonicalJson);

                    // Sign hash with KeyManager
                    String signature = "";
                    try {
                        com.dspread.pos.security.KeyManager keyManager = com.dspread.pos.security.KeyManager.getInstance();
                        byte[] hashBytes = hexStringToByteArray(hash);
                        signature = keyManager.sign(hashBytes);
                    } catch (IllegalStateException e) {
                        // KeyManager not initialized yet - this should not happen as it's initialized before migration
                        TRACE.e(TAG + ": KeyManager not initialized during migration: " + e.getMessage());
                        throw new RuntimeException("KeyManager must be initialized before migration", e);
                    } catch (Exception e) {
                        TRACE.e(TAG + ": Error signing migrated invoice: " + e.getMessage());
                        throw new RuntimeException("Failed to sign migrated invoice", e);
                    }

                    // Create InvoiceEntity
                    String id = UUID.randomUUID().toString();
                    InvoiceEntity entity = new InvoiceEntity(
                        id,
                        canonicalJson,
                        hash,
                        signature,
                        prevHash,
                        nextSeqNo,
                        InvoiceEntity.STATUS_PENDING_DGI,
                        System.currentTimeMillis()
                    );

                    // Insert into Room
                    invoiceDao.insert(entity);

                    // Update for next iteration
                    prevHash = hash;
                    nextSeqNo++;
                    migratedCount++;

                    TRACE.i(TAG + ": Migrated invoice " + invoiceData.getExternalNum() + " (seqNo: " + entity.seqNo + ")");

                } catch (Exception e) {
                    TRACE.e(TAG + ": Error migrating invoice " + invoiceData.getExternalNum() + ": " + e.getMessage());
                    // Continue with next invoice
                }
            }

            // Mark migration as complete
            markMigrationComplete(prefs);

            TRACE.i(TAG + ": Migration completed - " + migratedCount + " invoice(s) migrated");

            return migratedCount;

        } catch (Exception e) {
            TRACE.e(TAG + ": Fatal error during migration: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Mark migration as complete in SharedPreferences
     */
    private static void markMigrationComplete(SharedPreferences prefs) {
        prefs.edit().putBoolean(KEY_MIGRATION_COMPLETE, true).apply();
        TRACE.i(TAG + ": Migration marked as complete");
    }

    /**
     * Calculate SHA-256 hash of a string
     */
    private static String calculateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes("UTF-8"));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating SHA-256", e);
        }
    }

    /**
     * Convert byte array to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Convert hex string to byte array
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}

