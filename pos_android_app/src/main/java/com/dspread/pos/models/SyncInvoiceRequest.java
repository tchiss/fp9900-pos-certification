    package com.dspread.pos.models;

import com.google.gson.annotations.SerializedName;

/**
 * Request model for invoice synchronization
 * Contains enriched payload with hash, signature, chain hash, and sequence number
 */
public class SyncInvoiceRequest {
    @SerializedName("deviceId")
    private String deviceId;

    @SerializedName("payload")
    private Object payload; // Parsed JSON object

    @SerializedName("hash")
    private String hash;

    @SerializedName("signature")
    private String signature;

    @SerializedName("prevHash")
    private String prevHash;

    @SerializedName("seqNo")
    private long seqNo;

    @SerializedName("timestamp")
    private long timestamp;

    public SyncInvoiceRequest() {
    }

    public SyncInvoiceRequest(String deviceId, Object payload, String hash, String signature,
                             String prevHash, long seqNo, long timestamp) {
        this.deviceId = deviceId;
        this.payload = payload;
        this.hash = hash;
        this.signature = signature;
        this.prevHash = prevHash;
        this.seqNo = seqNo;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

