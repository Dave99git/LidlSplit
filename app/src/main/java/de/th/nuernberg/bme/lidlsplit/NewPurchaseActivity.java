package de.th.nuernberg.bme.lidlsplit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import de.th.nuernberg.bme.lidlsplit.ReceiptScanner;


import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NewPurchaseActivity extends AppCompatActivity {

    private AppDatabaseHelper dbHelper;
    private PersonSelectAdapter personAdapter;
    private ReceiptItemAdapter itemAdapter;
    private final List<Person> selectedPersons = new ArrayList<>();
    private RecyclerView itemRecycler;
    private final List<PurchaseItem> items = new ArrayList<>();
    private final ActivityResultLauncher<String> filePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::processImage);

    private TextView navPurchases;
    private TextView navPeople;
    private RecyclerView invoiceRecycler;
    private DebtAdapter debtAdapter;
    private final List<Debt> debts = new ArrayList<>();
    private TextView tvInvoiceHeader;
    private TextView tvSettledLabel;
    private TextView tvPaidLabel;
    private TextView tvAddress;
    private TextView tvDate;
    private TextView tvTotal;
    private TextView tvPurchaseHeader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_purchase);

        dbHelper = new AppDatabaseHelper(this);

        Button addPerson = findViewById(R.id.btnAddPersonSelect);
        addPerson.setOnClickListener(v -> showPersonSelectDialog());

        RecyclerView personRecycler = findViewById(R.id.recyclerPersonsSelect);
        personRecycler.setLayoutManager(new LinearLayoutManager(this));
        personAdapter = new PersonSelectAdapter(selectedPersons);
        personRecycler.setAdapter(personAdapter);

        itemRecycler = findViewById(R.id.recyclerItems);
        itemRecycler.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new ReceiptItemAdapter(items, selectedPersons);
        itemRecycler.setAdapter(itemAdapter);

        Button upload = findViewById(R.id.btnUploadReceipt);
        upload.setOnClickListener(v -> filePicker.launch("image/*"));

        Button create = findViewById(R.id.btnCreateInvoice);
        create.setOnClickListener(v -> createInvoice());

        invoiceRecycler = findViewById(R.id.recyclerInvoice);
        invoiceRecycler.setLayoutManager(new LinearLayoutManager(this));
        debtAdapter = new DebtAdapter(debts, this::updatePurchaseStatus);
        invoiceRecycler.setAdapter(debtAdapter);
        tvInvoiceHeader = findViewById(R.id.text_invoice_header);
        tvSettledLabel = findViewById(R.id.tvSettledLabel);
        tvPaidLabel = findViewById(R.id.tvPaidLabel);
        tvAddress = findViewById(R.id.tvAddress);
        tvDate = findViewById(R.id.tvDate);
        tvTotal = findViewById(R.id.tvTotal);
        tvPurchaseHeader = findViewById(R.id.tvPurchaseHeader);

        navPurchases = findViewById(R.id.navPurchases);
        navPeople = findViewById(R.id.navPeople);
        activateTab(navPurchases, navPeople);

        navPurchases.setOnClickListener(v -> activateTab(navPurchases, navPeople));
        navPeople.setOnClickListener(v -> {
            activateTab(navPeople, navPurchases);
            startActivity(new Intent(this, PersonsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });
    }

    private void processImage(Uri uri) {
        if (uri == null) return;
        ReceiptScanner.scanImage(this, uri, new ReceiptScanner.Callback() {
            @Override
            public void onSuccess(ReceiptData data, List<PurchaseItem> parsedItems) {
                items.clear();
                items.addAll(parsedItems);
                itemAdapter.notifyDataSetChanged();
                updateUi(data);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(NewPurchaseActivity.this, "OCR failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUi(ReceiptData meta) {
        if (items.isEmpty()) {
            itemRecycler.setVisibility(View.GONE);
        } else {
            itemRecycler.setVisibility(View.VISIBLE);
        }

        tvPurchaseHeader.setVisibility(View.VISIBLE);
        tvInvoiceHeader.setVisibility(View.GONE);
        tvSettledLabel.setVisibility(View.GONE);
        debts.clear();
        debtAdapter.notifyDataSetChanged();
        invoiceRecycler.setVisibility(View.GONE);

        if (meta.getDateTime() != null) {
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            tvDate.setText(df.format(meta.getDateTime()));
        } else {
            tvDate.setText("");
        }

        StringBuilder addr = new StringBuilder();
        if (meta.getStreet() != null) {
            addr.append(meta.getStreet());
        }
        if (meta.getCity() != null) {
            if (addr.length() > 0) addr.append("\n");
            addr.append(meta.getCity());
        }
        tvAddress.setText(addr.toString());

        if (meta.getTotal() != 0.0) {
            tvTotal.setText(getString(R.string.total_label, meta.getTotal()));
        } else {
            tvTotal.setText("");
        }
    }


    private void createInvoice() {
        if (!itemAdapter.allItemsAssigned()) {
            Toast.makeText(this, getString(R.string.error_assign_person), Toast.LENGTH_LONG).show();
            return;
        }
        itemAdapter.notifyDataSetChanged();
        itemRecycler.setVisibility(View.VISIBLE);

        long payerId = personAdapter.getPayerId();
        if (payerId == -1) {
            Toast.makeText(this, getString(R.string.error_choose_payer), Toast.LENGTH_LONG).show();
            return;
        }

        Map<Long, Double> totals = itemAdapter.calculateTotals();
        Person payer = null;
        for (Person p : selectedPersons) {
            if (p.getId() == payerId) {
                payer = p;
                break;
            }
        }
        if (payer == null) {
            Toast.makeText(this, getString(R.string.error_choose_payer), Toast.LENGTH_LONG).show();
            return;
        }

        debts.clear();
        for (Person p : selectedPersons) {
            if (p.getId() == payerId) continue;
            double value = totals.getOrDefault(p.getId(), 0.0);
            debts.add(new Debt(p, payer, value));
        }
        debtAdapter.notifyDataSetChanged();

        tvInvoiceHeader.setVisibility(View.VISIBLE);
        tvSettledLabel.setVisibility(View.VISIBLE);
        invoiceRecycler.setVisibility(View.VISIBLE);
        updatePurchaseStatus();
    }

    private void updatePurchaseStatus() {
        boolean allPaid = true;
        for (Debt d : debts) {
            if (!d.isSettled()) {
                allPaid = false;
                break;
            }
        }
        // Placeholder for status handling; in a real app this would update the purchase entry
        Log.d("PurchaseStatus", allPaid ? "paid" : "open");
    }

    private void activateTab(TextView active, TextView inactive) {
        active.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_active));
        inactive.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_inactive));
    }

    private void showPersonSelectDialog() {
        final List<Person> persons = dbHelper.getAllPersons();
        final String[] names = new String[persons.size()];
        final boolean[] checked = new boolean[persons.size()];
        for (int i = 0; i < persons.size(); i++) {
            Person p = persons.get(i);
            names[i] = p.getName();
            for (Person sel : selectedPersons) {
                if (sel.getId() == p.getId()) {
                    checked[i] = true;
                    break;
                }
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.dialog_select_persons_title)
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    selectedPersons.clear();
                    for (int i = 0; i < persons.size(); i++) {
                        if (checked[i]) {
                            selectedPersons.add(persons.get(i));
                        }
                    }
                    personAdapter.updateData(new ArrayList<>(selectedPersons));
                    itemAdapter = new ReceiptItemAdapter(items, selectedPersons);
                    itemRecycler.setAdapter(itemAdapter);
                    if (selectedPersons.isEmpty()) {
                        tvPaidLabel.setVisibility(View.GONE);
                    } else {
                        tvPaidLabel.setVisibility(View.VISIBLE);
                    }
                })
                .show();
    }
}
