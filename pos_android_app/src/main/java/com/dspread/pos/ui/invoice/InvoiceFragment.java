package com.dspread.pos.ui.invoice;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.dspread.pos.common.base.BaseFragment;
import com.dspread.pos.TitleProviderListener;
import com.dspread.pos.models.InvoiceData;
import com.dspread.pos.models.InvoiceLine;
import com.dspread.pos.models.Issuer;
import com.dspread.pos.models.Customer;
import com.dspread.pos.utils.InvoiceTestData;
import com.dspread.pos_android_app.BR;
import com.dspread.pos_android_app.R;
import com.dspread.pos_android_app.databinding.FragmentInvoiceBinding;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment principal pour la gestion des factures et certification DGI
 * Migré depuis InvoiceScreen.tsx de React Native
 */
public class InvoiceFragment extends BaseFragment<FragmentInvoiceBinding, InvoiceViewModel> implements TitleProviderListener {

    @Override
    public int initContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return R.layout.fragment_invoice;
    }

    @Override
    public int initVariableId() {
        return BR.viewModel;
    }

    @Override
    public InvoiceViewModel initViewModel() {
        return new ViewModelProvider(this).get(InvoiceViewModel.class);
    }

    @Override
    public void initData() {
        super.initData();
        
        // Initialiser les listeners
        initListeners();
        
        // Charger les données initiales
        viewModel.loadInitialData();

        // Définir un externalNum aléatoire par défaut s'il est vide
        if (binding.etExternalNum.getText() == null || binding.etExternalNum.getText().toString().trim().isEmpty()) {
            binding.etExternalNum.setText(generateExternalNum());
        }
    }

    private void initListeners() {
        // Button listeners
        binding.btnSync.setOnClickListener(v -> viewModel.syncPendingInvoices());
        binding.btnTestPrint.setOnClickListener(v -> viewModel.printTestPage());
        binding.btnSubmit.setOnClickListener(v -> submitInvoice());
        binding.btnAddItem.setOnClickListener(v -> addSampleItem());
        
        // Certification result listener
        viewModel.certificationResult.observe(this, result -> {
            if (result != null) {
                handleCertificationResult(result);
            }
        });

        // Error message listener
        viewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        // Sync status listener
        viewModel.syncStatus.observe(this, status -> {
            if (status != null) {
                updateSyncStatus(status);
            }
        });

        // Print result listener
        viewModel.printResult.observe(this, result -> {
            if (result != null) {
                handlePrintResult(result);
            }
        });
    }

    private void handleCertificationResult(InvoiceViewModel.CertificationResult result) {
        if (result.isSuccess()) {
            if ("FISCALIZED".equals(result.getStatus())) {
                Toast.makeText(getContext(), "Invoice certified successfully!", Toast.LENGTH_SHORT).show();
                // Show certification details
                showCertificationDetails(result);
            } else if ("PENDING".equals(result.getStatus())) {
                Toast.makeText(getContext(), "Invoice queued for synchronization", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getContext(), "Certification error: " + result.getErrorMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showCertificationDetails(InvoiceViewModel.CertificationResult result) {
        // TODO: Show certification details in a dialog or new view
        // Include: mecefCode, qrData, timestamp, etc.
    }

    private void updateSyncStatus(InvoiceViewModel.SyncStatus status) {
        // Update interface to reflect sync status
        binding.syncStatus.setText(status.getMessage());
        binding.syncStatus.setVisibility(status.isVisible() ? View.VISIBLE : View.GONE);
        
        if (status.getPendingCount() > 0) {
            binding.pendingCount.setText(String.valueOf(status.getPendingCount()));
            binding.pendingCount.setVisibility(View.VISIBLE);
        } else {
            binding.pendingCount.setVisibility(View.GONE);
        }
    }

    private void handlePrintResult(InvoiceViewModel.PrintResult result) {
        if (result.isSuccess()) {
            Toast.makeText(getContext(), "Print successful!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Print error: " + result.getErrorMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void submitInvoice() {
        try {
            // Create InvoiceData from form fields
            InvoiceData invoiceData = createInvoiceFromForm();
            
            // Validate invoice data
            if (!invoiceData.isValid()) {
                Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Submit invoice for certification
            viewModel.submitInvoice(invoiceData);
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error creating invoice: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private InvoiceData createInvoiceFromForm() {
        // Get form data
        String externalNum = binding.etExternalNum.getText().toString().trim();
        String machineNum = binding.etMachineNum.getText().toString().trim();
        String issuerName = binding.etIssuerName.getText().toString().trim();
        String issuerId = binding.etIssuerId.getText().toString().trim();
        String customerName = binding.etCustomerName.getText().toString().trim();
        String customerId = binding.etCustomerId.getText().toString().trim();
        
        // Create issuer and customer with tel (required by API)
        // Use default tel if not provided - should be added to UI later
        String issuerTel = "+237123456789";
        String customerTel = "+237123456789";
        Issuer issuer = new Issuer(issuerName, issuerId, issuerTel);
        Customer customer = new Customer(customerName, customerId, customerTel);
        
        // Create sample invoice lines (for now, we'll use test data)
        List<InvoiceLine> invoiceLines = new ArrayList<>();
        invoiceLines.add(new InvoiceLine("Article1", 5, 10000, 5));
        invoiceLines.add(new InvoiceLine("Article2", 2, 3500, 20));
        
        // Format date as ISO 8601 with 3-digit milliseconds and Z timezone
        // Expected format: 2025-10-19T15:44:52.922Z
        String issueDate = Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        
        // Create invoice data
        InvoiceData invoiceData = new InvoiceData(
            externalNum,
            machineNum,
            issuer,
            customer,
            invoiceLines,
            0, // totalHt - will be calculated
            0, // totalVat - will be calculated
            0, // totalTtc - will be calculated
            issueDate
        );
        
        // Calculate totals
        invoiceData.calculateTotals();
        
        return invoiceData;
    }

    private void addSampleItem() {
        // Préserver la valeur saisie d'externalNum
        String currentExternal = binding.etExternalNum.getText() != null ? binding.etExternalNum.getText().toString().trim() : "";

        // For now, just use test data
        InvoiceData testInvoice = InvoiceTestData.createTestInvoice();
        
        // Update form fields with test data (sans écraser externalNum saisi)
        binding.etMachineNum.setText(testInvoice.getMachineNum());
        binding.etIssuerName.setText(testInvoice.getIssuer().getName());
        binding.etIssuerId.setText(testInvoice.getIssuer().getIdentityNumber());
        binding.etCustomerName.setText(testInvoice.getCustomer().getName());
        binding.etCustomerId.setText(testInvoice.getCustomer().getIdentityNumber());

        // Restaurer externalNum si l'utilisateur l'a saisi, sinon générer un nouveau aléatoire
        if (currentExternal != null && !currentExternal.isEmpty()) {
            binding.etExternalNum.setText(currentExternal);
        } else {
            binding.etExternalNum.setText(generateExternalNum());
        }
        
        // Update total display
        binding.tvTotal.setText(String.format("%.2f FCFA", testInvoice.getTotalTtc() / 100.0));
        
        Toast.makeText(getContext(), "Sample data loaded", Toast.LENGTH_SHORT).show();
    }

    private String generateExternalNum() {
        // Format: FCT-YYYYMMDD-HHMMSS-XXX (XXX = suffixe aléatoire)
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        String ts = java.time.LocalDateTime.now(java.time.ZoneId.of("UTC")).format(fmt);
        int rand = (int)(Math.random() * 1000);
        String suffix = String.format("%03d", rand);
        return "FCT-" + ts + "-" + suffix;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check connectivity and sync if necessary
        viewModel.checkConnectivityAndSync();
    }

    @Override
    public String getTitle() {
        return "DGI Certification";
    }
}
