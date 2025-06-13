package de.th.nuernberg.bme.lidlsplit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView navPurchases;
    private TextView navPeople;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // RecyclerView einrichten
        RecyclerView recyclerView = findViewById(R.id.recyclerPurchases);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        PurchaseAdapter adapter = new PurchaseAdapter(createDummyPurchases());
        recyclerView.setAdapter(adapter);

        // "+ Einkauf hinzufügen" Button
        Button addButton = findViewById(R.id.btnAddPurchase);
        addButton.setOnClickListener(v ->
                Toast.makeText(this, "+ Einkauf hinzufügen", Toast.LENGTH_SHORT).show());

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
        navPurchases.setOnClickListener(v -> {
            activateTab(navPurchases, navPeople);
            Toast.makeText(this, "Einkäufe ausgewählt", Toast.LENGTH_SHORT).show();
            // TODO: Hier später zu "Einkäufe"-Ansicht wechseln
        });

        navPeople.setOnClickListener(v -> {
            activateTab(navPeople, navPurchases);
            Toast.makeText(this, "Personen ausgewählt", Toast.LENGTH_SHORT).show();
            // TODO: Hier später zu "Personen"-Ansicht wechseln
        });
    }

    private void activateTab(TextView active, TextView inactive) {
        active.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_active));
        inactive.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_inactive));
    }

    private List<Purchase> createDummyPurchases() {
        List<Purchase> purchases = new ArrayList<>();
        purchases.add(new Purchase("29.03.2025", "54,99€", false));
        purchases.add(new Purchase("30.03.2025", "31,20€", true));
        purchases.add(new Purchase("31.03.2025", "9,49€", false));
        return purchases;
    }
}
