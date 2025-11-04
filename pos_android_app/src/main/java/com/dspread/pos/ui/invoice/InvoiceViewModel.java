package com.dspread.pos.ui.invoice;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

import androidx.annotation.NonNull;
import me.goldze.mvvmhabit.base.BaseViewModel;
import androidx.lifecycle.MutableLiveData;

import com.dspread.pos.managers.ApiManager;
import com.dspread.pos.managers.PrinterManager;
import com.dspread.pos.managers.StorageManager;
import com.dspread.pos.models.InvoiceData;
import com.dspread.pos.models.InvoiceLine;
import com.dspread.pos.models.CertificationResponse;
import com.dspread.pos.models.InvoiceVerificationResponse;
import com.dspread.pos.utils.TRACE;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel pour la gestion des factures et certification DGI
 * Migré depuis InvoiceScreen.tsx de React Native
 */
public class InvoiceViewModel extends BaseViewModel {

    // LiveData pour les résultats de certification
    public MutableLiveData<CertificationResult> certificationResult = new MutableLiveData<>();
    
    // LiveData pour les messages d'erreur
    public MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // LiveData pour le statut de synchronisation
    public MutableLiveData<SyncStatus> syncStatus = new MutableLiveData<>();
    
    // LiveData pour les résultats d'impression
    public MutableLiveData<PrintResult> printResult = new MutableLiveData<>();
    
    // LiveData pour l'état de chargement
    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    // LiveData pour l'état de connectivité
    public MutableLiveData<Boolean> isOnline = new MutableLiveData<>(true);

    private ApiManager apiManager;
    private PrinterManager printerManager;
    private StorageManager storageManager;
    private Context context;

    public InvoiceViewModel(@NonNull Application application) {
        super(application);
        this.context = application.getApplicationContext();
        initializeManagers();
    }

    private void initializeManagers() {
        // Initialize each manager individually
        try {
            apiManager = ApiManager.getInstance(context);
            TRACE.i("InvoiceViewModel: ApiManager obtained successfully");
        } catch (Exception e) {
            TRACE.e("InvoiceViewModel: Error getting ApiManager: " + e.getMessage());
            apiManager = null;
        }

        try {
            storageManager = StorageManager.getInstance();
            TRACE.i("InvoiceViewModel: StorageManager obtained successfully");
        } catch (IllegalStateException e) {
            TRACE.i("InvoiceViewModel: StorageManager not initialized yet: " + e.getMessage());
            storageManager = null;
        } catch (Exception e) {
            TRACE.e("InvoiceViewModel: Error getting StorageManager: " + e.getMessage());
            storageManager = null;
        }

        try {
            printerManager = PrinterManager.getInstance();
            TRACE.i("InvoiceViewModel: PrinterManager obtained successfully");
        } catch (IllegalStateException e) {
            TRACE.i("InvoiceViewModel: PrinterManager not initialized yet: " + e.getMessage());
            printerManager = null;
        } catch (Exception e) {
            TRACE.e("InvoiceViewModel: Error getting PrinterManager: " + e.getMessage());
            printerManager = null;
        }
    }

    public void loadInitialData() {
        TRACE.i("InvoiceViewModel: loadInitialData() called - CHECKING MANAGERS");
        
        // Check connectivity first
        checkConnectivity();
        
        // Check if managers are available
        new Thread(() -> {
            try {
                int attempts = 0;
                int maxAttempts = 100; // Wait up to 10 seconds
                
                TRACE.i("InvoiceViewModel: Starting manager initialization check...");
                
                            while ((apiManager == null || storageManager == null) && attempts < maxAttempts) {
                                Thread.sleep(100);
                                initializeManagers();
                                attempts++;
                                
                                if (attempts % 10 == 0) {
                                    TRACE.i("InvoiceViewModel: Attempt " + attempts + "/" + maxAttempts + 
                                           " - ApiManager: " + (apiManager != null ? "OK" : "NULL") + 
                                           ", StorageManager: " + (storageManager != null ? "OK" : "NULL"));
                                    
                                    // Try to get instances directly for debugging
                                    try {
                                        ApiManager testApi = ApiManager.getInstance(context);
                                        TRACE.i("InvoiceViewModel: Direct ApiManager.getInstance() successful: " + (testApi != null ? "OK" : "NULL"));
                                    } catch (Exception e) {
                                        TRACE.i("InvoiceViewModel: Direct ApiManager.getInstance() failed: " + e.getMessage());
                                    }
                                    
                                    try {
                                        StorageManager testStorage = StorageManager.getInstance();
                                        TRACE.i("InvoiceViewModel: Direct StorageManager.getInstance() successful: " + (testStorage != null ? "OK" : "NULL"));
                                    } catch (Exception e) {
                                        TRACE.i("InvoiceViewModel: Direct StorageManager.getInstance() failed: " + e.getMessage());
                                    }
                                }
                            }
                
                if (apiManager != null && storageManager != null) {
                    TRACE.i("InvoiceViewModel: All managers initialized successfully after " + attempts + " attempts");
                    loadPendingInvoices();
                } else {
                    TRACE.e("InvoiceViewModel: Some managers failed to initialize after " + attempts + " attempts");
                    errorMessage.postValue("Some services are not available. Please restart the app.");
                }
            } catch (Exception e) {
                TRACE.e("InvoiceViewModel: Error in background initialization: " + e.getMessage());
                errorMessage.postValue("Initialization error: " + e.getMessage());
            }
        }).start();
    }

    private void checkConnectivity() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            isOnline.postValue(false);
            TRACE.e("InvoiceViewModel: ConnectivityManager is null");
            return;
        }
        
        boolean isConnected = false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Use modern API for Android 6.0+
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
                if (capabilities != null) {
                    // Check if network has internet capability (not just connected to WiFi/mobile)
                    isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                 capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                    TRACE.i("InvoiceViewModel: Modern connectivity check - Internet: " + 
                           capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) +
                           ", Validated: " + 
                           capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
                }
            }
        } else {
            // Fallback for older Android versions
            try {
                android.net.NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
                isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
            } catch (Exception e) {
                TRACE.e("InvoiceViewModel: Error checking connectivity: " + e.getMessage());
                isConnected = false;
            }
        }
        
        isOnline.postValue(isConnected);
        TRACE.i("InvoiceViewModel: Connectivity status: " + isConnected);
        
        // Additional check: try to verify real internet access
        if (isConnected) {
            verifyInternetAccess();
        }
    }
    
    /**
     * Vérifie l'accès Internet réel en vérifiant la connectivité
     */
    private void verifyInternetAccess() {
        // This is called asynchronously, but we set a reasonable default
        // The actual API call will verify real connectivity
        TRACE.i("InvoiceViewModel: Internet access verification - will be confirmed during API call");
    }

    private void loadPendingInvoices() {
        if (storageManager == null) {
            TRACE.i("InvoiceViewModel: StorageManager not initialized, skipping pending invoices check");
            syncStatus.postValue(new SyncStatus(false, "Storage not available", 0));
            return;
        }
        
        try {
            List<InvoiceData> pending = storageManager.getPendingInvoices();
            SyncStatus status = new SyncStatus(
                pending.size() > 0,
                pending.size() > 0 ? pending.size() + " invoice(s) pending" : "No pending invoices",
                pending.size()
            );
            syncStatus.postValue(status);
        } catch (Exception e) {
            TRACE.e("InvoiceViewModel: Error loading pending invoices" + ": " + e.getMessage());
            errorMessage.postValue("Error loading pending invoices");
        }
    }

    private void initializePrinter() {
        if (printerManager == null) {
            TRACE.i("InvoiceViewModel: PrinterManager not initialized, skipping printer setup");
            return;
        }
        
        try {
            printerManager.initialize(new PrinterManager.PrinterCallback() {
                @Override
                public void onSuccess() {
                    TRACE.i("InvoiceViewModel: Printer initialized successfully");
                }

                @Override
                public void onError(String error) {
                    TRACE.e("InvoiceViewModel: Printer initialization failed: " + error);
                    errorMessage.postValue("Printer initialization error: " + error);
                }
            });
        } catch (Exception e) {
            TRACE.e("InvoiceViewModel: Printer initialization error" + ": " + e.getMessage());
            errorMessage.postValue("Printer initialization error");
        }
    }

    public void submitInvoice(InvoiceData invoiceData) {
        isLoading.setValue(true);
        
        TRACE.i("InvoiceViewModel: Submitting invoice for certification");
        
        // Check if managers are available
        if (apiManager == null) {
            isLoading.setValue(false);
            errorMessage.setValue("API service not available. Please restart the app.");
            TRACE.e("InvoiceViewModel: ApiManager not initialized");
            return;
        }
        
        if (storageManager == null) {
            isLoading.setValue(false);
            errorMessage.setValue("Storage service not available. Please wait for initialization or restart the app.");
            TRACE.e("InvoiceViewModel: StorageManager not initialized");
            return;
        }
        
        // Re-check connectivity right before submitting (in case status changed)
        checkConnectivity();
        Boolean onlineStatus = isOnline.getValue();
        
        // If connectivity check fails, try anyway (API will handle network errors properly)
        // Don't automatically save offline - let the API attempt first
        if (onlineStatus == null || !onlineStatus) {
            TRACE.w("InvoiceViewModel: Connectivity check indicates offline, but attempting API call anyway");
            // Continue to try API call - if it fails, error handler will save offline
        }

        // Submit to DGI API using complete certification flow
        apiManager.certifyInvoice(invoiceData, new ApiManager.ApiCallback<InvoiceVerificationResponse>() {
            @Override
            public void onSuccess(InvoiceVerificationResponse response) {
                isLoading.postValue(false);
                
                CertificationResult result = new CertificationResult(
                    true,
                    response.getStatus(),
                    response.getMecefCode(),
                    response.getQrCode(),
                    response.getFiscalizationDate(),
                    null
                );
                certificationResult.postValue(result);
                
                // Save the response
                if (storageManager != null) {
                    storageManager.saveCertifiedInvoice(invoiceData, response);
                }
                
                // TOUJOURS imprimer après certification réussie (FISCALIZED)
                String status = response.getStatus();
                if ("FISCALIZED".equals(status)) {
                    TRACE.i("InvoiceViewModel: Invoice certified successfully, starting print process");
                    // Imprimer automatiquement après certification
                    printInvoice(invoiceData, response);
                } else {
                    TRACE.w("InvoiceViewModel: Invoice status is " + status + ", not printing");
                }
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                
                TRACE.e("InvoiceViewModel: Certification error: " + error);
                
                // Check if it's a network error
                boolean isNetworkError = error != null && (
                    error.toLowerCase().contains("connection") ||
                    error.toLowerCase().contains("network") ||
                    error.toLowerCase().contains("timeout") ||
                    error.toLowerCase().contains("unreachable") ||
                    error.toLowerCase().contains("no internet")
                );
                
                if (isNetworkError) {
                    // Only save offline if it's actually a network error
                    TRACE.i("InvoiceViewModel: Network error detected, saving invoice offline");
                    saveInvoiceOffline(invoiceData);
                    
                    // Update connectivity status
                    checkConnectivity();
                }
                
                CertificationResult result = new CertificationResult(
                    false,
                    "ERROR",
                    null,
                    null,
                    null,
                    "Creation failed: " + error
                );
                certificationResult.postValue(result);
            }
        });
    }

    private void saveInvoiceOffline(InvoiceData invoiceData) {
        if (storageManager == null) {
            errorMessage.postValue("Storage not available. Cannot save invoice offline.");
            isLoading.postValue(false);
            TRACE.e("InvoiceViewModel: StorageManager not available for offline save");
            return;
        }
        
        try {
            storageManager.savePendingInvoice(invoiceData);
            loadPendingInvoices(); // Update status
            
            TRACE.i("InvoiceViewModel: Invoice saved offline");
        } catch (Exception e) {
            TRACE.e("InvoiceViewModel: Error saving invoice offline: " + e.getMessage());
            errorMessage.postValue("Error during offline save");
        }
    }

    public void printInvoice(InvoiceData invoiceData, CertificationResponse response) {
        TRACE.i("InvoiceViewModel: Printing invoice with CertificationResponse");
        
        if (invoiceData == null) {
            TRACE.e("InvoiceViewModel: InvoiceData is null");
            printResult.setValue(new PrintResult(false, "Invoice data is null"));
            return;
        }
        
        if (response == null) {
            TRACE.e("InvoiceViewModel: CertificationResponse is null");
            printResult.setValue(new PrintResult(false, "Certification response is null"));
            return;
        }
        
        if (printerManager == null) {
            // Essayer de ré-obtenir PrinterManager
            try {
                printerManager = PrinterManager.getInstance();
            } catch (IllegalStateException e) {
                TRACE.e("InvoiceViewModel: PrinterManager not initialized: " + e.getMessage());
                printResult.setValue(new PrintResult(false, "Printer service not available. Please restart the app."));
                return;
            }
        }
        
        TRACE.i("InvoiceViewModel: Starting print - Invoice: " + invoiceData.getExternalNum() + 
               ", Status: " + response.getStatus());
        
        printerManager.printInvoice(invoiceData, response, new PrinterManager.PrintCallback() {
            @Override
            public void onSuccess() {
                TRACE.i("InvoiceViewModel: Print completed successfully");
                PrintResult result = new PrintResult(true, null);
                printResult.postValue(result);
            }

            @Override
            public void onError(String error) {
                TRACE.e("InvoiceViewModel: Print failed: " + error);
                PrintResult result = new PrintResult(false, error != null ? error : "Unknown print error");
                printResult.postValue(result);
            }
        });
    }

    public void printInvoice(InvoiceData invoiceData, InvoiceVerificationResponse response) {
        TRACE.i("InvoiceViewModel: Printing invoice from InvoiceVerificationResponse");
        
        if (response == null) {
            TRACE.e("InvoiceViewModel: InvoiceVerificationResponse is null");
            printResult.setValue(new PrintResult(false, "Response data is null"));
            return;
        }
        
        // Convert InvoiceVerificationResponse to CertificationResponse for printing
        // Note: InvoiceVerificationResponse.getQrCode() maps to CertificationResponse.qrData
        CertificationResponse certResponse = new CertificationResponse(
            response.getStatus() != null ? response.getStatus() : "UNKNOWN",
            response.getMecefCode(),
            response.getQrCode(), // qrCode from API -> qrData in CertificationResponse
            response.getInvoiceId(),
            response.getFiscalizationDate()
        );
        
        TRACE.i("InvoiceViewModel: Print request - Status: " + certResponse.getStatus() + 
               ", MECEF: " + (certResponse.getMecefCode() != null ? certResponse.getMecefCode() : "null") +
               ", QR: " + (certResponse.getQrData() != null ? "present" : "null"));
        
        printInvoice(invoiceData, certResponse);
    }

    public void downloadAndPrintInvoice(InvoiceData invoiceData, InvoiceVerificationResponse verificationResponse) {
        TRACE.i("InvoiceViewModel: Downloading PDF and printing invoice");
        
        if (apiManager == null) {
            printResult.setValue(new PrintResult(false, "API service not available"));
            TRACE.e("InvoiceViewModel: ApiManager not available for PDF download");
            return;
        }
        
        // Download PDF first
        apiManager.getInvoicePdf(verificationResponse.getInvoiceId(), new ApiManager.ApiCallback<byte[]>() {
            @Override
            public void onSuccess(byte[] pdfData) {
                TRACE.i("InvoiceViewModel: PDF downloaded successfully, size: " + pdfData.length + " bytes");
                
                // Now print the invoice with PDF data
                printInvoiceWithPdf(invoiceData, verificationResponse, pdfData);
            }

            @Override
            public void onError(String error) {
                TRACE.e("InvoiceViewModel: PDF download failed: " + error);
                // Fallback to regular printing without PDF
                printInvoice(invoiceData, verificationResponse);
            }
        });
    }

    private void printInvoiceWithPdf(InvoiceData invoiceData, InvoiceVerificationResponse verificationResponse, byte[] pdfData) {
        TRACE.i("InvoiceViewModel: Printing invoice with PDF data");
        
        if (printerManager == null) {
            printResult.setValue(new PrintResult(false, "Printer service not available"));
            TRACE.e("InvoiceViewModel: PrinterManager not available");
            return;
        }
        
        // Convert to CertificationResponse and add PDF data
        CertificationResponse certResponse = new CertificationResponse(
            verificationResponse.getStatus(),
            verificationResponse.getMecefCode(),
            verificationResponse.getQrCode(),
            verificationResponse.getInvoiceId(),
            verificationResponse.getFiscalizationDate()
        );
        
        // TODO: Update PrinterManager to handle PDF data
        // For now, use regular printing
        printInvoice(invoiceData, certResponse);
    }

    public void syncPendingInvoices() {
        if (!isOnline.getValue()) {
            errorMessage.setValue("No internet connection to sync");
            return;
        }

        if (storageManager == null) {
            errorMessage.setValue("Storage service not available");
            TRACE.e("InvoiceViewModel: StorageManager not available for sync");
            return;
        }

        isLoading.setValue(true);
        
        TRACE.i("InvoiceViewModel: Syncing pending invoices");
        
        storageManager.syncPendingInvoices(new StorageManager.SyncCallback() {
            @Override
            public void onSuccess(int syncedCount) {
                isLoading.postValue(false);
                loadPendingInvoices(); // Update status
                
                if (syncedCount > 0) {
                    errorMessage.postValue(syncedCount + " invoice(s) synchronized successfully");
                } else {
                    errorMessage.postValue("No invoices to sync");
                }
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue("Sync error: " + error);
            }
        });
    }

    public void checkConnectivityAndSync() {
        checkConnectivity();
        
        // If online and there are pending invoices, suggest synchronization
        if (isOnline.getValue() && storageManager != null) {
            try {
                List<InvoiceData> pending = storageManager.getPendingInvoices();
                if (pending.size() > 0) {
                    // TODO: Show notification or button to sync
                }
            } catch (Exception e) {
                TRACE.e("InvoiceViewModel: Error checking pending invoices: " + e.getMessage());
            }
        }
    }

    public void printTestPage() {
        TRACE.i("InvoiceViewModel: Printing test page");
        
        if (printerManager == null) {
            printResult.setValue(new PrintResult(false, "Printer service not available"));
            TRACE.e("InvoiceViewModel: PrinterManager not available for test page");
            return;
        }
        
        printerManager.printTestPage(new PrinterManager.PrintCallback() {
            @Override
            public void onSuccess() {
                PrintResult result = new PrintResult(true, null);
                printResult.postValue(result);
            }

            @Override
            public void onError(String error) {
                PrintResult result = new PrintResult(false, error);
                printResult.postValue(result);
            }
        });
    }

    // Classes internes pour les résultats
    public static class CertificationResult {
        private boolean success;
        private String status;
        private String mecefCode;
        private String qrData;
        private String timestamp;
        private String errorMessage;

        public CertificationResult(boolean success, String status, String mecefCode, 
                                 String qrData, String timestamp, String errorMessage) {
            this.success = success;
            this.status = status;
            this.mecefCode = mecefCode;
            this.qrData = qrData;
            this.timestamp = timestamp;
            this.errorMessage = errorMessage;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getStatus() { return status; }
        public String getMecefCode() { return mecefCode; }
        public String getQrData() { return qrData; }
        public String getTimestamp() { return timestamp; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class SyncStatus {
        private boolean visible;
        private String message;
        private int pendingCount;

        public SyncStatus(boolean visible, String message, int pendingCount) {
            this.visible = visible;
            this.message = message;
            this.pendingCount = pendingCount;
        }

        // Getters
        public boolean isVisible() { return visible; }
        public String getMessage() { return message; }
        public int getPendingCount() { return pendingCount; }
    }

    public static class PrintResult {
        private boolean success;
        private String errorMessage;

        public PrintResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
}
