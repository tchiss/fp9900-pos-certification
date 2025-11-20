package com.dspread.pos.db;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
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
     * Get the last invoice (highest seqNo) with status PENDING_DGI or CERTIFIED
     * Used for chain hash calculation
     */
    @Query("SELECT * FROM invoices WHERE status IN ('PENDING_DGI', 'CERTIFIED') ORDER BY seqNo DESC LIMIT 1")
    InvoiceEntity getLastInvoice();

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

