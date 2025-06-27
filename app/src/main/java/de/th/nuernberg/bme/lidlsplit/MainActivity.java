package de.th.nuernberg.bme.lidlsplit;

import android.content.Intent;
import android.os.Bundle;
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


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.th.nuernberg.bme.lidlsplit.PurchaseAdapter;
import de.th.nuernberg.bme.lidlsplit.PurchaseDetailActivity;

public class MainActivity extends AppCompatActivity {

    private TextView navPurchases;
    private TextView navPeople;
    private AppDatabaseHelper dbHelper;
    private PurchaseAdapter adapter;
    private List<Purchase> allPurchases;

    private View filterContainer;
    private RadioGroup rgSort;
    private Spinner spinnerStatus;
    private EditText etStartDate;
    private EditText etEndDate;
    private RangeSlider sliderAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new AppDatabaseHelper(this);

        // RecyclerView einrichten
        RecyclerView recyclerView = findViewById(R.id.recyclerPurchases);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        allPurchases = dbHelper.getAllPurchases();
        PurchaseAdapter adapter = new PurchaseAdapter(new ArrayList<>(allPurchases), dbHelper, purchase -> {
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

        // Filter-UI
        filterContainer = findViewById(R.id.filterContainer);
        rgSort = findViewById(R.id.rgSort);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        sliderAmount = findViewById(R.id.sliderAmount);

        double max = 0.0;
        for (Purchase p : allPurchases) {
            if (p.getAmount() > max) max = p.getAmount();
        }
        sliderAmount.setValueFrom(0f);
        sliderAmount.setValueTo((float) max);
        sliderAmount.setValues(0f, (float) max);

        // Filter-Button
        ImageButton filterButton = findViewById(R.id.btnFilter);
        filterButton.setOnClickListener(v -> {
            if (filterContainer.getVisibility() == View.VISIBLE) {
                filterContainer.setVisibility(View.GONE);
            } else {
                filterContainer.setVisibility(View.VISIBLE);
            }
        });

        Button applyFilter = findViewById(R.id.btnApplyFilter);
        applyFilter.setOnClickListener(v -> filterAndSortPurchases());

        Button clearFilter = findViewById(R.id.btnClearFilter);
        clearFilter.setOnClickListener(v -> {
            rgSort.check(R.id.rbSortDate);
            spinnerStatus.setSelection(0);
            etStartDate.setText("");
            etEndDate.setText("");
            sliderAmount.setValues(sliderAmount.getValueFrom(), sliderAmount.getValueTo());
            filterAndSortPurchases();
        });

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
            double max = 0.0;
            for (Purchase p : allPurchases) { if (p.getAmount() > max) max = p.getAmount(); }
            sliderAmount.setValueTo((float) max);
            sliderAmount.setValues(sliderAmount.getValueFrom(), (float) max);
            filterAndSortPurchases();
        }
    }

    private void activateTab(TextView active, TextView inactive) {
        active.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_active));
        inactive.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_inactive));
    }

    private void filterAndSortPurchases() {
        if (allPurchases == null) return;
        List<Purchase> filtered = new ArrayList<>(allPurchases);

        String status = spinnerStatus.getSelectedItem().toString();
        LocalDate start = parseDate(etStartDate.getText().toString().trim());
        LocalDate end = parseDate(etEndDate.getText().toString().trim());
        float minVal = sliderAmount.getValues().get(0);
        float maxVal = sliderAmount.getValues().get(1);

        filtered.removeIf(p -> {
            if ("offen".equals(status) && p.isPaid()) return true;
            if ("bezahlt".equals(status) && !p.isPaid()) return true;
            if (p.getAmount() < minVal || p.getAmount() > maxVal) return true;
            LocalDate d = parseDate(p.getDate());
            if (start != null && (d == null || d.isBefore(start))) return true;
            if (end != null && (d == null || d.isAfter(end))) return true;
            return false;
        });

        if (rgSort.getCheckedRadioButtonId() == R.id.rbSortAmount) {
            filtered.sort((a, b) -> Double.compare(b.getAmount(), a.getAmount()));
        } else {
            filtered.sort((a, b) -> {
                LocalDate da = parseDate(a.getDate());
                LocalDate db = parseDate(b.getDate());
                if (da == null || db == null) return 0;
                return db.compareTo(da);
            });
        }

        adapter.updateData(filtered);
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            if (raw.contains("T")) {
                return LocalDate.parse(raw.substring(0, 10));
            }
            if (raw.matches("\\d{2}\\.\\d{2}\\.\\d{4}.*")) {
                DateTimeFormatter f = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                return LocalDate.parse(raw.substring(0, 10), f);
            }
            if (raw.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                return LocalDate.parse(raw.substring(0, 10));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // Dummy data method removed
}
