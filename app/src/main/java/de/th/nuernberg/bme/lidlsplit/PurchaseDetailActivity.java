package de.th.nuernberg.bme.lidlsplit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.widget.NestedScrollView;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.th.nuernberg.bme.lidlsplit.Article;
import de.th.nuernberg.bme.lidlsplit.Debt;
import de.th.nuernberg.bme.lidlsplit.DebtPair;
import de.th.nuernberg.bme.lidlsplit.Person;
import de.th.nuernberg.bme.lidlsplit.Purchase;
import de.th.nuernberg.bme.lidlsplit.PurchaseItem;
import de.th.nuernberg.bme.lidlsplit.PersonSelectAdapter;
import de.th.nuernberg.bme.lidlsplit.ReceiptItemAdapter;
import de.th.nuernberg.bme.lidlsplit.DebtAdapter;

public class PurchaseDetailActivity extends AppCompatActivity {

    private AppDatabaseHelper dbHelper;
    private PersonSelectAdapter personAdapter;
    private ReceiptItemAdapter itemAdapter;
    private final List<Person> selectedPersons = new ArrayList<>();
    private final List<PurchaseItem> items = new ArrayList<>();
    private RecyclerView itemRecycler;
    private RecyclerView invoiceRecycler;
    private DebtAdapter debtAdapter;
    private final List<Debt> debts = new ArrayList<>();
    private Button btnSave;
    private String purchaseDate = "";
    private double purchaseTotal = 0.0;
    private long purchaseId;
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
        setContentView(R.layout.activity_purchase_detail);

        scrollView = findViewById(R.id.scrollContent);

        dbHelper = new AppDatabaseHelper(this);

        purchaseId = getIntent().getLongExtra("purchase_id", -1);
        if (purchaseId == -1) {
            finish();
            return;
        }

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
        btnSave = findViewById(R.id.btnSaveInvoice);
        btnSave.setOnClickListener(v -> saveChanges());

        TextView navPurchases = findViewById(R.id.navPurchases);
        TextView navPeople = findViewById(R.id.navPeople);
        activateTab(navPurchases, navPeople);
        navPurchases.setOnClickListener(v -> activateTab(navPurchases, navPeople));
        navPeople.setOnClickListener(v -> {
            activateTab(navPeople, navPurchases);
            startActivity(new Intent(this, PersonsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });

        loadPurchase();
    }

    private void loadPurchase() {
        Purchase purchase = dbHelper.getPurchase(purchaseId);
        if (purchase == null) {
            finish();
            return;
        }

        purchaseDate = purchase.getDate();
        purchaseTotal = purchase.getAmount();
        tvDate.setText(purchaseDate);
        tvTotal.setText(getString(R.string.total_label, purchaseTotal));
        selectedPersons.clear();
        selectedPersons.addAll(purchase.getPersons());
        personAdapter.updateData(new ArrayList<>(selectedPersons));
        personAdapter.setPayerId(purchase.getPayerId());

        items.clear();
        for (Article a : purchase.getArticles()) {
            items.add(new PurchaseItem(a.getName(), a.getPrice()));
        }
        itemAdapter = new ReceiptItemAdapter(items, selectedPersons);
        itemRecycler.setAdapter(itemAdapter);
        Map<Integer, Set<Long>> map = new HashMap<>();
        int idx = 0;
        for (Article a : purchase.getArticles()) {
            List<Person> ps = purchase.getAssignments().get(a);
            if (ps == null) { idx++; continue; }
            Set<Long> set = new java.util.HashSet<>();
            for (Person p : ps) set.add(p.getId());
            map.put(idx, set);
            idx++;
        }
        itemAdapter.setAssignments(map);

        Map<Long, Double> totals = itemAdapter.calculateTotals();
        debts.clear();
        long payerId = purchase.getPayerId();
        Person payer = null;
        for (Person p : selectedPersons) if (p.getId() == payerId) payer = p;
        if (payer == null && !selectedPersons.isEmpty()) payer = selectedPersons.get(0);
        for (Person p : selectedPersons) {
            if (p.getId() == payerId) continue;
            double amount = totals.getOrDefault(p.getId(), 0.0);
            Debt d = new Debt(p, payer, amount);
            Boolean settled = purchase.getDebtStatus().get(new DebtPair(p.getId(), payerId));
            if (settled != null) d.setSettled(settled);
            debts.add(d);
        }
        debtAdapter.notifyDataSetChanged();
        layoutInvoiceHeader.setVisibility(View.VISIBLE);
        invoiceRecycler.setVisibility(View.VISIBLE);
        updatePaidLabelVisibility();
    }

    private void createInvoice() {
        if (!itemAdapter.allItemsAssigned()) {
            Toast.makeText(this, getString(R.string.error_assign_person), Toast.LENGTH_LONG).show();
            return;
        }

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
        updatePaidLabelVisibility();
        btnSave.setVisibility(View.VISIBLE);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void updatePurchaseStatus() {
        // callback required by DebtAdapter, currently no additional status handling
    }

    private void updatePaidLabelVisibility() {
        if (selectedPersons.isEmpty()) {
            tvPaidLabel.setVisibility(View.GONE);
        } else {
            tvPaidLabel.setVisibility(View.VISIBLE);
        }
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
                        if (checked[i]) selectedPersons.add(persons.get(i));
                    }
                    personAdapter.updateData(new ArrayList<>(selectedPersons));
                    itemAdapter = new ReceiptItemAdapter(items, selectedPersons);
                    itemRecycler.setAdapter(itemAdapter);
                    updatePaidLabelVisibility();
                })
                .show();
    }

    private void saveChanges() {
        if (purchaseDate.isEmpty() || purchaseTotal == 0.0) {
            Toast.makeText(this, "Keine Rechnung", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Article> articles = new ArrayList<>();
        for (PurchaseItem pi : items) articles.add(new Article(pi.getName(), pi.getPrice()));
        Map<Integer, Set<Long>> assignIds = itemAdapter.getAssignments();
        Map<Article, List<Person>> assignments = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            Set<Long> sel = assignIds.get(i);
            if (sel == null) continue;
            Article art = articles.get(i);
            List<Person> ps = new ArrayList<>();
            for (Long pid : sel) for (Person p : selectedPersons) if (p.getId() == pid) ps.add(p);
            assignments.put(art, ps);
        }
        Map<DebtPair, Boolean> debtMap = new HashMap<>();
        for (Debt d : debts) debtMap.put(new DebtPair(d.getDebtor().getId(), d.getCreditor().getId()), d.isSettled());
        dbHelper.updatePurchase(purchaseId, purchaseDate, purchaseTotal, personAdapter.getPayerId(), selectedPersons, articles, assignments, debtMap);
        finish();
    }
}
