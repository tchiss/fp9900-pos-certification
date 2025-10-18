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

import java.time.LocalDateTime;
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
        
        // Create issuer and customer
        Issuer issuer = new Issuer(issuerName, issuerId);
        Customer customer = new Customer(customerName, customerId);
        
        // Create sample invoice lines (for now, we'll use test data)
        List<InvoiceLine> invoiceLines = new ArrayList<>();
        invoiceLines.add(new InvoiceLine("Article1", 5, 10000, 5));
        invoiceLines.add(new InvoiceLine("Article2", 2, 3500, 20));
        
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
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ".212Z"
        );
        
        // Calculate totals
        invoiceData.calculateTotals();
        
        return invoiceData;
    }

    private void addSampleItem() {
        // For now, just use test data
        InvoiceData testInvoice = InvoiceTestData.createTestInvoice();
        
        // Update form fields with test data
        binding.etExternalNum.setText(testInvoice.getExternalNum());
        binding.etMachineNum.setText(testInvoice.getMachineNum());
        binding.etIssuerName.setText(testInvoice.getIssuer().getName());
        binding.etIssuerId.setText(testInvoice.getIssuer().getIdentityNumber());
        binding.etCustomerName.setText(testInvoice.getCustomer().getName());
        binding.etCustomerId.setText(testInvoice.getCustomer().getIdentityNumber());
        
        // Update total display
        binding.tvTotal.setText(String.format("%.2f FCFA", testInvoice.getTotalTtc() / 100.0));
        
        Toast.makeText(getContext(), "Sample data loaded", Toast.LENGTH_SHORT).show();
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
