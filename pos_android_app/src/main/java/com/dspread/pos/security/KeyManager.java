package com.dspread.pos.security;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.dspread.pos.utils.TRACE;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;


/**
 * Manages ECDSA P-256 key pair in Android Keystore
 * Provides signing functionality for invoice hash verification
 */
public class KeyManager {
    private static final String TAG = "KeyManager";
    private static final String KEYSTORE_NAME = "AndroidKeyStore";
    private static final String KEY_ALIAS = "DGI_INVOICE_SIGNING_KEY";
    private static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";
    
    private static KeyManager instance;
    private Context context;
    private KeyStore keyStore;

    private KeyManager(Context context) {
        this.context = context.getApplicationContext();
        initializeKeyStore();
        ensureKeyExists();
    }

    /**
     * Initialize KeyManager
     */
    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new KeyManager(context);
        }
    }

    /**
     * Get singleton instance
     */
    public static synchronized KeyManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("KeyManager not initialized. Call initialize() first.");
        }
        return instance;
    }

    /**
     * Initialize Android Keystore
     */
    private void initializeKeyStore() {
        try {
            keyStore = KeyStore.getInstance(KEYSTORE_NAME);
            keyStore.load(null);
            TRACE.i(TAG + ": Android Keystore initialized successfully");
        } catch (Exception e) {
            TRACE.e(TAG + ": Error initializing Keystore: " + e.getMessage());
            throw new RuntimeException("Failed to initialize Android Keystore", e);
        }
    }

    /**
     * Ensure signing key exists, generate if not
     */
    private void ensureKeyExists() {
        try {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateKeyPair();
            } else {
                TRACE.i(TAG + ": Signing key already exists");
            }
        } catch (Exception e) {
            TRACE.e(TAG + ": Error checking key existence: " + e.getMessage());
            throw new RuntimeException("Failed to check/generate signing key", e);
        }
    }

    /**
     * Generate ECDSA P-256 key pair in Android Keystore
     */
    private void generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_NAME);

            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1")) // P-256
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build();

            keyPairGenerator.initialize(spec);
            keyPairGenerator.generateKeyPair();

            TRACE.i(TAG + ": ECDSA P-256 key pair generated successfully");
        } catch (Exception e) {
            TRACE.e(TAG + ": Error generating key pair: " + e.getMessage());
            throw new RuntimeException("Failed to generate ECDSA key pair", e);
        }
    }

    /**
     * Sign a hash using the private key
     * 
     * @param hash The hash bytes to sign (typically SHA-256)
     * @return Base64-encoded signature
     */
    @NonNull
    public String sign(byte[] hash) {
        if (hash == null || hash.length == 0) {
            throw new IllegalArgumentException("Hash cannot be null or empty");
        }

        try {
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
            if (privateKey == null) {
                throw new IllegalStateException("Private key not found in Keystore");
            }

            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(hash);
            byte[] signatureBytes = signature.sign();

            String signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP);
            TRACE.i(TAG + ": Hash signed successfully");
            return signatureBase64;
        } catch (Exception e) {
            TRACE.e(TAG + ": Error signing hash: " + e.getMessage());
            throw new RuntimeException("Failed to sign hash", e);
        }
    }

    /**
     * Get public key as Base64 string
     * 
     * @return Base64-encoded public key
     */
    @NonNull
    public String getPublicKeyBase64() {
        try {
            java.security.cert.Certificate cert = keyStore.getCertificate(KEY_ALIAS);
            if (cert == null) {
                throw new IllegalStateException("Certificate not found in Keystore");
            }

            PublicKey publicKey = cert.getPublicKey();
            byte[] publicKeyBytes = publicKey.getEncoded();
            return Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            TRACE.e(TAG + ": Error getting public key: " + e.getMessage());
            throw new RuntimeException("Failed to get public key", e);
        }
    }

    /**
     * Verify if the key pair exists
     * 
     * @return true if key exists, false otherwise
     */
    public boolean hasKey() {
        try {
            return keyStore.containsAlias(KEY_ALIAS);
        } catch (Exception e) {
            TRACE.e(TAG + ": Error checking key existence: " + e.getMessage());
            return false;
        }
    }
}

