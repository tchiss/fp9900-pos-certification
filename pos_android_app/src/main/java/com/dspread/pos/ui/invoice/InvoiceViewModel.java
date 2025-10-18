package com.dspread.pos.ui.invoice;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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
        try {
            apiManager = ApiManager.getInstance();
            printerManager = PrinterManager.getInstance();
            storageManager = StorageManager.getInstance();
        } catch (IllegalStateException e) {
            // Managers not yet initialized, will be initialized later
            TRACE.i("Managers not yet initialized, will retry later");
        }
    }

    public void loadInitialData() {
        TRACE.i("InvoiceViewModel: Loading initial data");
        
        // Move heavy operations to background thread
        new Thread(() -> {
            try {
                // Wait a bit for managers to initialize
                Thread.sleep(500);
                
                // Try to initialize managers again if they weren't ready
                if (apiManager == null || printerManager == null || storageManager == null) {
                    initializeManagers();
                }
                
                // Check connectivity (light operation, can stay on main thread)
                checkConnectivity();
                
                // Load pending invoices (heavy I/O operation)
                loadPendingInvoices();
                
                // Initialize printer (heavy operation)
                initializePrinter();
            } catch (Exception e) {
                TRACE.e("InvoiceViewModel: Error in background initialization: " + e.getMessage());
                errorMessage.postValue("Initialization error: " + e.getMessage());
            }
        }).start();
    }

    private void checkConnectivity() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        isOnline.postValue(isConnected);
        
        TRACE.i("InvoiceViewModel: Connectivity status: " + isConnected);
    }

    private void loadPendingInvoices() {
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
        
        // Check connectivity before submitting
        if (!isOnline.getValue()) {
            // Offline mode - save locally
            saveInvoiceOffline(invoiceData);
            return;
        }

        // Submit to DGI API using complete certification flow
        apiManager.certifyInvoiceComplete(invoiceData, new ApiManager.InvoiceCertificationCallback() {
            @Override
            public void onSuccess(InvoiceVerificationResponse response) {
                isLoading.setValue(false);
                
                CertificationResult result = new CertificationResult(
                    true,
                    response.getStatus(),
                    response.getMecefCode(),
                    response.getQrCode(),
                    response.getFiscalizationDate(),
                    null
                );
                certificationResult.setValue(result);
                
                // Save the response
                storageManager.saveCertifiedInvoice(invoiceData, response);
                
                // Print automatically if fiscalized
                if ("FISCALIZED".equals(response.getStatus())) {
                    printInvoice(invoiceData, response);
                }
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                
                // In case of error, save offline
                saveInvoiceOffline(invoiceData);
                
                CertificationResult result = new CertificationResult(
                    false,
                    "ERROR",
                    null,
                    null,
                    null,
                    error
                );
                certificationResult.setValue(result);
            }
        });
    }

    private void saveInvoiceOffline(InvoiceData invoiceData) {
        try {
            storageManager.savePendingInvoice(invoiceData);
            loadPendingInvoices(); // Update status
            
            TRACE.i("InvoiceViewModel: Invoice saved offline");
        } catch (Exception e) {
            TRACE.e("InvoiceViewModel: Error saving invoice offline: " + e.getMessage());
            errorMessage.setValue("Error during offline save");
        }
    }

    public void printInvoice(InvoiceData invoiceData, CertificationResponse response) {
        TRACE.i("InvoiceViewModel: Printing invoice");
        
        printerManager.printInvoice(invoiceData, response, new PrinterManager.PrintCallback() {
            @Override
            public void onSuccess() {
                PrintResult result = new PrintResult(true, null);
                printResult.setValue(result);
            }

            @Override
            public void onError(String error) {
                PrintResult result = new PrintResult(false, error);
                printResult.setValue(result);
            }
        });
    }

    public void printInvoice(InvoiceData invoiceData, InvoiceVerificationResponse response) {
        TRACE.i("InvoiceViewModel: Printing invoice");
        
        // Convert InvoiceVerificationResponse to CertificationResponse for printing
        CertificationResponse certResponse = new CertificationResponse(
            response.getStatus(),
            response.getMecefCode(),
            response.getQrCode(),
            response.getInvoiceId(),
            response.getFiscalizationDate()
        );
        
        printInvoice(invoiceData, certResponse);
    }

    public void syncPendingInvoices() {
        if (!isOnline.getValue()) {
            errorMessage.setValue("No internet connection to sync");
            return;
        }

        isLoading.setValue(true);
        
        TRACE.i("InvoiceViewModel: Syncing pending invoices");
        
        storageManager.syncPendingInvoices(new StorageManager.SyncCallback() {
            @Override
            public void onSuccess(int syncedCount) {
                isLoading.setValue(false);
                loadPendingInvoices(); // Update status
                
                if (syncedCount > 0) {
                    errorMessage.setValue(syncedCount + " invoice(s) synchronized successfully");
                } else {
                    errorMessage.setValue("No invoices to sync");
                }
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue("Sync error: " + error);
            }
        });
    }

    public void checkConnectivityAndSync() {
        checkConnectivity();
        
        // If online and there are pending invoices, suggest synchronization
        if (isOnline.getValue()) {
            List<InvoiceData> pending = storageManager.getPendingInvoices();
            if (pending.size() > 0) {
                // TODO: Show notification or button to sync
            }
        }
    }

    public void printTestPage() {
        TRACE.i("InvoiceViewModel: Printing test page");
        
        printerManager.printTestPage(new PrinterManager.PrintCallback() {
            @Override
            public void onSuccess() {
                PrintResult result = new PrintResult(true, null);
                printResult.setValue(result);
            }

            @Override
            public void onError(String error) {
                PrintResult result = new PrintResult(false, error);
                printResult.setValue(result);
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
