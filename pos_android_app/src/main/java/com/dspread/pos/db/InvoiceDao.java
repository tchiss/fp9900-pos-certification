package com.dspread.pos.db;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object for InvoiceEntity
 * Provides CRUD operations and queries for invoice management
 */
@Dao
public interface InvoiceDao {
    /**
     * Insert a new invoice entity
     */
    @Insert
    void insert(InvoiceEntity invoice);

    /**
     * Update an existing invoice entity
     */
    @Update
    void update(InvoiceEntity invoice);

    /**
     * Get invoice by ID
     */
    @Query("SELECT * FROM invoices WHERE id = :id")
    InvoiceEntity getById(@NonNull String id);

    /**
     * Get the last invoice (highest seqNo) with status PENDING_DGI, CERTIFIED, or SIGNED_LOCAL
     * Used for chain hash calculation
     * Includes SIGNED_LOCAL to ensure chain integrity even if invoices are briefly in that state
     */
    @Query("SELECT * FROM invoices WHERE status IN ('PENDING_DGI', 'CERTIFIED', 'SIGNED_LOCAL') ORDER BY seqNo DESC LIMIT 1")
    InvoiceEntity getLastInvoice();

    /**
     * Atomically get last invoice and calculate next sequence number
     * This prevents race conditions when multiple invoices are created concurrently
     * Uses a transaction to ensure atomicity
     */
    @Transaction
    default ChainInfo getLastInvoiceAndCalculateNext() {
        InvoiceEntity lastInvoice = getLastInvoice();
        String prevHash = (lastInvoice != null) ? lastInvoice.hash : "GENESIS";
        long nextSeqNo = (lastInvoice != null) ? lastInvoice.seqNo + 1 : 1;
        return new ChainInfo(prevHash, nextSeqNo);
    }

    /**
     * Helper class for chain information
     */
    class ChainInfo {
        public final String prevHash;
        public final long nextSeqNo;

        public ChainInfo(String prevHash, long nextSeqNo) {
            this.prevHash = prevHash;
            this.nextSeqNo = nextSeqNo;
        }
    }

    /**
     * Get all invoices with status PENDING_DGI
     * Used for synchronization
     */
    @Query("SELECT * FROM invoices WHERE status = 'PENDING_DGI' ORDER BY seqNo ASC")
    List<InvoiceEntity> getPendingInvoices();

    /**
     * Get all invoices
     */
    @Query("SELECT * FROM invoices ORDER BY seqNo ASC")
    List<InvoiceEntity> getAllInvoices();

    /**
     * Update invoice status and certifiedAt timestamp
     */
    @Query("UPDATE invoices SET status = :status, certifiedAt = :certifiedAt WHERE id = :id")
    void updateStatus(@NonNull String id, @NonNull String status, Long certifiedAt);

    /**
     * Delete invoice by ID
     */
    @Query("DELETE FROM invoices WHERE id = :id")
    void deleteById(@NonNull String id);

    /**
     * Count pending invoices
     */
    @Query("SELECT COUNT(*) FROM invoices WHERE status = 'PENDING_DGI'")
    int countPendingInvoices();
}

