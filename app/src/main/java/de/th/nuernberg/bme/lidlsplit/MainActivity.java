package de.th.nuernberg.bme.lidlsplit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import de.th.nuernberg.bme.lidlsplit.PurchaseAdapter;

public class MainActivity extends AppCompatActivity {

    private TextView navPurchases;
    private TextView navPeople;
    private AppDatabaseHelper dbHelper;
    private PurchaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new AppDatabaseHelper(this);

        // RecyclerView einrichten
        RecyclerView recyclerView = findViewById(R.id.recyclerPurchases);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        PurchaseAdapter adapter = new PurchaseAdapter(dbHelper.getAllPurchases(), dbHelper, purchase -> {
            Intent intent = new Intent(MainActivity.this, EditPurchaseActivity.class);
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

        // Filter-Button
        ImageButton filterButton = findViewById(R.id.btnFilter);
        filterButton.setOnClickListener(v ->
                Toast.makeText(this, "Filter", Toast.LENGTH_SHORT).show());

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
            adapter.updateData(dbHelper.getAllPurchases());
        }
    }

    private void activateTab(TextView active, TextView inactive) {
        active.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_active));
        inactive.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_inactive));
    }

    // Dummy data method removed
}
