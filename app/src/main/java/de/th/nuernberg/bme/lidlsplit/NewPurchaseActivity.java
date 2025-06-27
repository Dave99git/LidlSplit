package de.th.nuernberg.bme.lidlsplit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.util.Log;
import android.widget.LinearLayout;
import androidx.core.widget.NestedScrollView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import de.th.nuernberg.bme.lidlsplit.ReceiptScanner;
import de.th.nuernberg.bme.lidlsplit.Article;
import de.th.nuernberg.bme.lidlsplit.DebtPair;


import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private Button btnSaveInvoice;
    private String purchaseDate = "";
    private double purchaseTotal = 0.0;
    private boolean invoicePaid = false;
    private LinearLayout layoutInvoiceHeader;
    private TextView tvInvoiceHeader;
    private TextView tvSettledLabel;
    private TextView tvPaidLabel;
    private TextView tvAddress;
    private TextView tvDate;
    private TextView tvTotal;
    private TextView tvPurchaseHeader;
    private NestedScrollView scrollView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_purchase);

        scrollView = findViewById(R.id.scrollContent);

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
        layoutInvoiceHeader = findViewById(R.id.layoutInvoiceHeader);
        tvInvoiceHeader = findViewById(R.id.text_invoice_header);
        tvSettledLabel = findViewById(R.id.tvSettledLabel);
        tvPaidLabel = findViewById(R.id.tvPaidLabel);
        tvAddress = findViewById(R.id.tvAddress);
        tvDate = findViewById(R.id.tvDate);
        tvTotal = findViewById(R.id.tvTotal);
        tvPurchaseHeader = findViewById(R.id.tvPurchaseHeader);
        btnSaveInvoice = findViewById(R.id.btnSaveInvoice);
        btnSaveInvoice.setOnClickListener(v -> saveInvoice());
        btnSaveInvoice.setVisibility(View.GONE);

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
        layoutInvoiceHeader.setVisibility(View.GONE);
        debts.clear();
        debtAdapter.notifyDataSetChanged();
        invoiceRecycler.setVisibility(View.GONE);
        btnSaveInvoice.setVisibility(View.GONE);
        invoicePaid = false;

        if (meta.getDateTime() != null) {
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            purchaseDate = df.format(meta.getDateTime());
            tvDate.setText(purchaseDate);
        } else {
            tvDate.setText("");
            purchaseDate = "";
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
            purchaseTotal = meta.getTotal();
            tvTotal.setText(getString(R.string.total_label, meta.getTotal()));
        } else {
            purchaseTotal = 0.0;
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

        layoutInvoiceHeader.setVisibility(View.VISIBLE);
        invoiceRecycler.setVisibility(View.VISIBLE);
        updatePurchaseStatus();
        btnSaveInvoice.setVisibility(View.VISIBLE);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void updatePurchaseStatus() {
        boolean allPaid = true;
        for (Debt d : debts) {
            if (!d.isSettled()) {
                allPaid = false;
                break;
            }
        }
        invoicePaid = allPaid;
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

    private void saveInvoice() {
        if (purchaseDate.isEmpty() || purchaseTotal == 0.0) {
            Toast.makeText(this, "Keine Rechnung", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Article> articles = new ArrayList<>();
        for (PurchaseItem pi : items) {
            articles.add(new Article(pi.getName(), pi.getPrice()));
        }
        Map<Integer, java.util.Set<Long>> assignIds = itemAdapter.getAssignments();
        Map<Article, List<Person>> assignments = new java.util.HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            java.util.Set<Long> sel = assignIds.get(i);
            if (sel == null) continue;
            Article art = articles.get(i);
            List<Person> plist = new ArrayList<>();
            for (Long pid : sel) {
                for (Person p : selectedPersons) {
                    if (p.getId() == pid) plist.add(p);
                }
            }
            assignments.put(art, plist);
        }
        Map<DebtPair, Boolean> debtMap = new java.util.HashMap<>();
        for (Debt d : debts) {
            debtMap.put(new DebtPair(d.getDebtor().getId(), d.getCreditor().getId()), d.isSettled());
        }
        dbHelper.addPurchase(purchaseDate, purchaseTotal, personAdapter.getPayerId(), selectedPersons, articles, assignments, debtMap);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
