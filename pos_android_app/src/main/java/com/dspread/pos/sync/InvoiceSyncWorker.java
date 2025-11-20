package com.dspread.pos.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.dspread.pos.db.AppDatabase;
import com.dspread.pos.db.InvoiceDao;
import com.dspread.pos.db.InvoiceEntity;
import com.dspread.pos.managers.ApiManager;
import com.dspread.pos.utils.TRACE;

import java.util.List;

/**
 * WorkManager worker for synchronizing pending invoices with DGI API
 * Runs periodically when network is available
 */
public class InvoiceSyncWorker extends Worker {
    private static final String TAG = "InvoiceSyncWorker";

    public InvoiceSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        TRACE.i(TAG + ": Starting invoice synchronization");

        try {
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            InvoiceDao invoiceDao = database.invoiceDao();

            // Get all pending invoices
            List<InvoiceEntity> pendingInvoices = invoiceDao.getPendingInvoices();

            if (pendingInvoices.isEmpty()) {
                TRACE.i(TAG + ": No pending invoices to sync");
                return Result.success();
            }

            TRACE.i(TAG + ": Found " + pendingInvoices.size() + " pending invoice(s) to sync");

            ApiManager apiManager = ApiManager.getInstance(getApplicationContext());
            int syncedCount = 0;
            int failedCount = 0;

            // Sync each pending invoice
            for (InvoiceEntity invoice : pendingInvoices) {
                try {
                    syncInvoice(invoice, apiManager, invoiceDao);
                    syncedCount++;
                } catch (Exception e) {
                    TRACE.e(TAG + ": Error syncing invoice " + invoice.id + ": " + e.getMessage());
                    failedCount++;
                    // Continue with next invoice
                }
            }

            TRACE.i(TAG + ": Sync completed - Success: " + syncedCount + ", Failed: " + failedCount);

            // Return success if at least some invoices were synced
            // If all failed, return retry to try again later
            if (syncedCount > 0) {
                return Result.success();
            } else if (failedCount > 0) {
                // All failed - retry later
                return Result.retry();
            }

            return Result.success();

        } catch (Exception e) {
            TRACE.e(TAG + ": Fatal error in sync worker: " + e.getMessage());
            return Result.retry();
        }
    }

    /**
     * Sync a single invoice to the API
     */
    private void syncInvoice(InvoiceEntity invoice, ApiManager apiManager, InvoiceDao invoiceDao) {
        TRACE.i(TAG + ": Syncing invoice " + invoice.id + " (seqNo: " + invoice.seqNo + ")");

        // Use a synchronous approach with CountDownLatch for callback handling
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final boolean[] success = {false};
        final String[] errorMessage = {null};

        apiManager.syncInvoice(invoice, new ApiManager.ApiCallback<com.dspread.pos.models.SyncInvoiceResponse>() {
            @Override
            public void onSuccess(com.dspread.pos.models.SyncInvoiceResponse response) {
                try {
                    // Update invoice status to CERTIFIED
                    long certifiedAt = System.currentTimeMillis();
                    invoiceDao.updateStatus(invoice.id, InvoiceEntity.STATUS_CERTIFIED, certifiedAt);
                    
                    TRACE.i(TAG + ": Invoice " + invoice.id + " synced successfully");
                    success[0] = true;
                } catch (Exception e) {
                    TRACE.e(TAG + ": Error updating invoice status: " + e.getMessage());
                    errorMessage[0] = e.getMessage();
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onError(String error) {
                TRACE.e(TAG + ": Sync failed for invoice " + invoice.id + ": " + error);
                
                // Check if it's a rejection (4xx) or network error
                // For now, we'll leave it as PENDING_DGI for retry
                // If the API returns a specific rejection status, we could mark as REJECTED
                // For network errors, we keep it as PENDING_DGI
                
                errorMessage[0] = error;
                latch.countDown();
            }
        });

        // Wait for callback (with timeout)
        try {
            boolean completed = latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
            if (!completed) {
                TRACE.e(TAG + ": Sync timeout for invoice " + invoice.id);
                throw new RuntimeException("Sync timeout");
            }
        } catch (InterruptedException e) {
            TRACE.e(TAG + ": Sync interrupted for invoice " + invoice.id);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sync interrupted", e);
        }

        if (!success[0] && errorMessage[0] != null) {
            // Check if it's a rejection (client error 4xx)
            // For now, we'll retry all errors. In production, you might want to
            // parse the error response to determine if it's a permanent rejection
            throw new RuntimeException("Sync failed: " + errorMessage[0]);
        }
    }
}

