package com.dspread.pos.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity for storing invoices with DGI certification data
 * Includes hash, signature, chain hash (prevHash), and sequence number
 */
@Entity(tableName = "invoices")
public class InvoiceEntity {
    @PrimaryKey
    @NonNull
    public String id;

    public String payload;        // JSON canonique
    public String hash;           // SHA-256 hex
    public String signature;      // Base64
    public String prevHash;       // hex (hash de la facture précédente ou "GENESIS")
    public long seqNo;            // Numéro de séquence monotone

    public String status;         // SIGNED_LOCAL, PENDING_DGI, CERTIFIED, REJECTED
    public long createdAt;        // Timestamp de création

    public Long certifiedAt;     // Timestamp de certification (nullable)

    // Status constants
    public static final String STATUS_SIGNED_LOCAL = "SIGNED_LOCAL";
    public static final String STATUS_PENDING_DGI = "PENDING_DGI";
    public static final String STATUS_CERTIFIED = "CERTIFIED";
    public static final String STATUS_REJECTED = "REJECTED";

    public InvoiceEntity() {
    }

    @androidx.room.Ignore
    public InvoiceEntity(@NonNull String id, String payload, String hash, String signature,
                        String prevHash, long seqNo, String status, long createdAt) {
        this.id = id;
        this.payload = payload;
        this.hash = hash;
        this.signature = signature;
        this.prevHash = prevHash;
        this.seqNo = seqNo;
        this.status = status;
        this.createdAt = createdAt;
        this.certifiedAt = null;
    }

    // Getters and Setters
    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public void setPrevHash(String prevHash) {
        this.prevHash = prevHash;
    }

    public long getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(long seqNo) {
        this.seqNo = seqNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCertifiedAt() {
        return certifiedAt;
    }

    public void setCertifiedAt(Long certifiedAt) {
        this.certifiedAt = certifiedAt;
    }
}

