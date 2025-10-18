package com.dspread.pos.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.dspread.pos.models.InvoiceData;
import com.dspread.pos.models.CertificationResponse;
import com.dspread.pos.models.InvoiceVerificationResponse;
import com.dspread.pos.utils.TRACE;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire pour le stockage local et la synchronisation hors ligne
 * Migré depuis offline.ts de React Native
 */
public class StorageManager {
    private static final String TAG = "StorageManager";
    private static final String PREFS_NAME = "pos_storage";
    private static final String KEY_PENDING_INVOICES = "pending_invoices";
    private static final String KEY_CERTIFIED_INVOICES = "certified_invoices";
    private static final String KEY_SYNC_STATUS = "sync_status";
    
    private static StorageManager instance;
    private SharedPreferences prefs;
    private Gson gson;
    private Context context;

    private StorageManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public static synchronized StorageManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("StorageManager not initialized. Call initialize() first.");
        }
        return instance;
    }

    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new StorageManager(context);
        }
    }

    /**
     * Interface pour les callbacks de synchronisation
     */
    public interface SyncCallback {
        void onSuccess(int syncedCount);
        void onError(String error);
    }

    /**
     * Interface pour les callbacks de sauvegarde
     */
    public interface SaveCallback {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Sauvegarde une facture en attente de synchronisation
     */
    public void savePendingInvoice(InvoiceData invoiceData) {
        try {
            List<InvoiceData> pending = getPendingInvoices();
            pending.add(invoiceData);
            
            String json = gson.toJson(pending);
            prefs.edit().putString(KEY_PENDING_INVOICES, json).apply();
            
            TRACE.i(TAG + ": Invoice saved to pending queue");
            Log.d(TAG, "Pending invoices count: " + pending.size());
            
        } catch (Exception e) {
            TRACE.e(TAG + ": Error saving pending invoice" + ": " + e.getMessage());
            throw new RuntimeException("Erreur lors de la sauvegarde de la facture en attente", e);
        }
    }

    /**
     * Récupère toutes les factures en attente
     */
    public List<InvoiceData> getPendingInvoices() {
        try {
            String json = prefs.getString(KEY_PENDING_INVOICES, "[]");
            Type listType = new TypeToken<List<InvoiceData>>(){}.getType();
            List<InvoiceData> pending = gson.fromJson(json, listType);
            
            if (pending == null) {
                pending = new ArrayList<>();
            }
            
            return pending;
            
        } catch (Exception e) {
            TRACE.e(TAG + ": Error loading pending invoices" + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Sauvegarde une facture certifiée
     */
    public void saveCertifiedInvoice(InvoiceData invoiceData, CertificationResponse response) {
        try {
            // Créer un objet combiné pour la sauvegarde
            CertifiedInvoiceRecord record = new CertifiedInvoiceRecord(invoiceData, response);
            
            List<CertifiedInvoiceRecord> certified = getCertifiedInvoices();
            certified.add(record);
            
            String json = gson.toJson(certified);
            prefs.edit().putString(KEY_CERTIFIED_INVOICES, json).apply();
            
            TRACE.i(TAG + ": Certified invoice saved");
            
        } catch (Exception e) {
            TRACE.e(TAG + ": Error saving certified invoice" + ": " + e.getMessage());
            throw new RuntimeException("Erreur lors de la sauvegarde de la facture certifiée", e);
        }
    }

    /**
     * Sauvegarde une facture certifiée avec InvoiceVerificationResponse
     */
    public void saveCertifiedInvoice(InvoiceData invoiceData, InvoiceVerificationResponse response) {
        try {
            // Convert InvoiceVerificationResponse to CertificationResponse
            CertificationResponse certResponse = new CertificationResponse(
                response.getStatus(),
                response.getMecefCode(),
                response.getQrCode(),
                response.getInvoiceId(),
                response.getFiscalizationDate()
            );
            
            // Use the existing method
            saveCertifiedInvoice(invoiceData, certResponse);
            
        } catch (Exception e) {
            TRACE.e(TAG + ": Error saving certified invoice from verification response" + ": " + e.getMessage());
            throw new RuntimeException("Error saving certified invoice from verification response", e);
        }
    }

    /**
     * Récupère toutes les factures certifiées
     */
    public List<CertifiedInvoiceRecord> getCertifiedInvoices() {
        try {
            String json = prefs.getString(KEY_CERTIFIED_INVOICES, "[]");
            Type listType = new TypeToken<List<CertifiedInvoiceRecord>>(){}.getType();
            List<CertifiedInvoiceRecord> certified = gson.fromJson(json, listType);
            
            if (certified == null) {
                certified = new ArrayList<>();
            }
            
            return certified;
            
        } catch (Exception e) {
            TRACE.e(TAG + ": Error loading certified invoices" + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Synchronise les factures en attente avec l'API
     */
    public void syncPendingInvoices(SyncCallback callback) {
        TRACE.i(TAG + ": Starting sync of pending invoices");
        
        List<InvoiceData> pending = getPendingInvoices();
        if (pending.isEmpty()) {
            callback.onSuccess(0);
            return;
        }

        ApiManager apiManager = ApiManager.getInstance();
        int syncedCount = 0;
        int totalCount = pending.size();
        
        // Synchroniser chaque facture
        syncInvoicesRecursively(pending, 0, apiManager, callback, syncedCount, totalCount);
    }

    private void syncInvoicesRecursively(List<InvoiceData> pending, int index, 
                                       ApiManager apiManager, SyncCallback callback, 
                                       int syncedCount, int totalCount) {
        if (index >= pending.size()) {
            // Toutes les factures ont été traitées
            callback.onSuccess(syncedCount);
            return;
        }

        InvoiceData invoice = pending.get(index);
        
        apiManager.certifyInvoiceComplete(invoice, new ApiManager.InvoiceCertificationCallback() {
            @Override
            public void onSuccess(InvoiceVerificationResponse response) {
                // Sauvegarder comme certifiée
                saveCertifiedInvoice(invoice, response);
                
                // Supprimer de la liste des en attente
                removePendingInvoice(invoice);
                
                TRACE.i(TAG + ": Invoice synced successfully: " + (index + 1) + "/" + totalCount);
                
                // Continuer avec la facture suivante
                syncInvoicesRecursively(pending, index + 1, apiManager, callback, 
                                      syncedCount + 1, totalCount);
            }

            @Override
            public void onError(String error) {
                TRACE.e(TAG + ": Failed to sync invoice " + (index + 1) + "/" + totalCount + ": " + error);
                
                // Continuer avec la facture suivante même en cas d'erreur
                syncInvoicesRecursively(pending, index + 1, apiManager, callback, 
                                      syncedCount, totalCount);
            }
        });
    }

    /**
     * Supprime une facture de la liste des en attente
     */
    private void removePendingInvoice(InvoiceData invoiceToRemove) {
        try {
            List<InvoiceData> pending = getPendingInvoices();
            pending.removeIf(invoice -> 
                invoice.getExternalNum().equals(invoiceToRemove.getExternalNum()) &&
                invoice.getMachineNum().equals(invoiceToRemove.getMachineNum()) &&
                invoice.getTotalTtc() == invoiceToRemove.getTotalTtc()
            );
            
            String json = gson.toJson(pending);
            prefs.edit().putString(KEY_PENDING_INVOICES, json).apply();
            
        } catch (Exception e) {
            TRACE.e(TAG + ": Error removing pending invoice" + ": " + e.getMessage());
        }
    }

    /**
     * Vide toutes les factures en attente
     */
    public void clearPendingInvoices() {
        prefs.edit().putString(KEY_PENDING_INVOICES, "[]").apply();
        TRACE.i(TAG + ": All pending invoices cleared");
    }

    /**
     * Vide toutes les factures certifiées
     */
    public void clearCertifiedInvoices() {
        prefs.edit().putString(KEY_CERTIFIED_INVOICES, "[]").apply();
        TRACE.i(TAG + ": All certified invoices cleared");
    }

    /**
     * Obtient le statut de synchronisation
     */
    public SyncStatus getSyncStatus() {
        List<InvoiceData> pending = getPendingInvoices();
        List<CertifiedInvoiceRecord> certified = getCertifiedInvoices();
        
        return new SyncStatus(
            pending.size(),
            certified.size(),
            System.currentTimeMillis()
        );
    }

    /**
     * Sauvegarde le statut de synchronisation
     */
    public void saveSyncStatus(SyncStatus status) {
        String json = gson.toJson(status);
        prefs.edit().putString(KEY_SYNC_STATUS, json).apply();
    }

    /**
     * Classe pour représenter une facture certifiée
     */
    public static class CertifiedInvoiceRecord {
        private InvoiceData invoiceData;
        private CertificationResponse response;
        private long timestamp;

        public CertifiedInvoiceRecord(InvoiceData invoiceData, CertificationResponse response) {
            this.invoiceData = invoiceData;
            this.response = response;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public InvoiceData getInvoiceData() { return invoiceData; }
        public CertificationResponse getResponse() { return response; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Classe pour le statut de synchronisation
     */
    public static class SyncStatus {
        private int pendingCount;
        private int certifiedCount;
        private long lastSyncTime;

        public SyncStatus(int pendingCount, int certifiedCount, long lastSyncTime) {
            this.pendingCount = pendingCount;
            this.certifiedCount = certifiedCount;
            this.lastSyncTime = lastSyncTime;
        }

        // Getters
        public int getPendingCount() { return pendingCount; }
        public int getCertifiedCount() { return certifiedCount; }
        public long getLastSyncTime() { return lastSyncTime; }
    }
}
