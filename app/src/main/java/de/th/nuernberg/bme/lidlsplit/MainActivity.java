package de.th.nuernberg.bme.lidlsplit;

import android.content.Intent;
import android.os.Bundle;
import android.app.DatePickerDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.slider.RangeSlider;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import de.th.nuernberg.bme.lidlsplit.PurchaseAdapter;
import de.th.nuernberg.bme.lidlsplit.PurchaseDetailActivity;

public class MainActivity extends AppCompatActivity {

    private TextView navPurchases;
    private TextView navPeople;
    private AppDatabaseHelper dbHelper;
    private PurchaseAdapter adapter;
    private View filterLayout;
    private RadioGroup rgSort;
    private Spinner spinnerStatus;
    private EditText etStartDate;
    private EditText etEndDate;
    private RangeSlider sliderAmount;
    private double minAmount = 0.0;
    private double maxAmount = 0.0;
    private java.util.List<Purchase> allPurchases;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new AppDatabaseHelper(this);

        allPurchases = dbHelper.getAllPurchases();
        minAmount = 0.0;
        maxAmount = 0.0;
        for (Purchase p : allPurchases) {
            if (p.getAmount() < minAmount || minAmount == 0.0) minAmount = p.getAmount();
            if (p.getAmount() > maxAmount) maxAmount = p.getAmount();
        }

        RecyclerView recyclerView = findViewById(R.id.recyclerPurchases);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        PurchaseAdapter adapter = new PurchaseAdapter(new java.util.ArrayList<>(allPurchases), dbHelper, purchase -> {
            Intent intent = new Intent(MainActivity.this, PurchaseDetailActivity.class);
            intent.putExtra("purchase_id", purchase.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
        this.adapter = adapter;

        // "+ Einkauf hinzufügen" Button
        Button addButton = findViewById(R.id.btnAddPurchase);
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NewPurchaseActivity.class);
            startActivity(intent);
        });

        filterLayout = findViewById(R.id.layoutFilter);
        rgSort = findViewById(R.id.rgSort);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        sliderAmount = findViewById(R.id.sliderAmount);

        sliderAmount.setValueFrom((float) minAmount);
        sliderAmount.setValueTo((float) maxAmount);
        sliderAmount.setValues((float) minAmount, (float) maxAmount);

        ImageButton filterButton = findViewById(R.id.btnFilter);
        filterButton.setOnClickListener(v -> {
            if (filterLayout.getVisibility() == View.GONE) {
                filterLayout.setVisibility(View.VISIBLE);
            } else {
                filterLayout.setVisibility(View.GONE);
            }
        });

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        Button apply = findViewById(R.id.btnApplyFilter);
        Button reset = findViewById(R.id.btnResetFilter);
        apply.setOnClickListener(v -> applyFilters());
        reset.setOnClickListener(v -> resetFilters());

        // Custom Footer Navigation
        navPurchases = findViewById(R.id.navPurchases);
        navPeople = findViewById(R.id.navPeople);

        // Initiale Auswahl: Einkäufe
        activateTab(navPurchases, navPeople);

        // Listener
        navPurchases.setOnClickListener(v -> activateTab(navPurchases, navPeople));

        navPeople.setOnClickListener(v -> {
            activateTab(navPeople, navPurchases);
            startActivity(new Intent(this, PersonsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            allPurchases = dbHelper.getAllPurchases();
            minAmount = 0.0;
            maxAmount = 0.0;
            for (Purchase p : allPurchases) {
                if (p.getAmount() < minAmount || minAmount == 0.0) minAmount = p.getAmount();
                if (p.getAmount() > maxAmount) maxAmount = p.getAmount();
            }
            sliderAmount.setValueFrom((float) minAmount);
            sliderAmount.setValueTo((float) maxAmount);
            sliderAmount.setValues((float) minAmount, (float) maxAmount);
            applyFilters();
        }
    }

    private void activateTab(TextView active, TextView inactive) {
        active.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_active));
        inactive.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_inactive));
    }

    private void showDatePicker(final EditText target) {
        java.time.LocalDate now = java.time.LocalDate.now();
        DatePickerDialog dlg = new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format(java.util.Locale.getDefault(), "%02d.%02d.%04d", day, month + 1, year);
            target.setText(date);
        }, now.getYear(), now.getMonthValue() - 1, now.getDayOfMonth());
        dlg.show();
    }

    private java.time.LocalDate parseDate(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            if (raw.contains("T")) return java.time.LocalDate.parse(raw.substring(0, 10));
            if (raw.matches("\\d{2}\\.\\d{2}\\.\\d{4}.*")) {
                java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
                return java.time.LocalDate.parse(raw.substring(0, 10), df);
            }
            if (raw.matches("\\d{4}-\\d{2}-\\d{2}.*")) return java.time.LocalDate.parse(raw.substring(0, 10));
        } catch (Exception ignored) { }
        return null;
    }

    private void applyFilters() {
        java.util.List<Purchase> filtered = new java.util.ArrayList<>();
        java.time.LocalDate start = parseDate(etStartDate.getText().toString());
        java.time.LocalDate end = parseDate(etEndDate.getText().toString());
        float min = sliderAmount.getValues().get(0);
        float max = sliderAmount.getValues().get(1);
        String status = spinnerStatus.getSelectedItem().toString();

        for (Purchase p : allPurchases) {
            if (status.equals(getString(R.string.status_open)) && p.isPaid()) continue;
            if (status.equals(getString(R.string.status_paid)) && !p.isPaid()) continue;
            if (p.getAmount() < min || p.getAmount() > max) continue;
            java.time.LocalDate d = parseDate(p.getDate());
            if (start != null && (d == null || d.isBefore(start))) continue;
            if (end != null && (d == null || d.isAfter(end))) continue;
            filtered.add(p);
        }

        java.util.Comparator<Purchase> comp;
        if (rgSort.getCheckedRadioButtonId() == R.id.rbSortAmount) {
            comp = (a, b) -> Double.compare(b.getAmount(), a.getAmount());
        } else {
            comp = (a, b) -> {
                java.time.LocalDate d1 = parseDate(b.getDate());
                java.time.LocalDate d2 = parseDate(a.getDate());
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return -1;
                if (d2 == null) return 1;
                return d1.compareTo(d2);
            };
        }
        java.util.Collections.sort(filtered, comp);
        adapter.updateData(filtered);
    }

    private void resetFilters() {
        rgSort.check(R.id.rbSortDate);
        spinnerStatus.setSelection(0);
        etStartDate.setText("");
        etEndDate.setText("");
        sliderAmount.setValues((float) minAmount, (float) maxAmount);
        applyFilters();
    }

    // Dummy data method removed
}
