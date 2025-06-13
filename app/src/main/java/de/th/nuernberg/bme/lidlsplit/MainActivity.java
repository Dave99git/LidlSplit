package de.th.nuernberg.bme.lidlsplit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerPurchases);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        PurchaseAdapter adapter = new PurchaseAdapter(createDummyPurchases());
        recyclerView.setAdapter(adapter);

        Button addButton = findViewById(R.id.btnAddPurchase);
        addButton.setOnClickListener(v ->
                Toast.makeText(this, "+ Einkauf hinzufügen", Toast.LENGTH_SHORT).show());

        ImageButton filterButton = findViewById(R.id.btnFilter);
        filterButton.setOnClickListener(v ->
                Toast.makeText(this, "Filter", Toast.LENGTH_SHORT).show());

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnItemSelectedListener(item -> true);
    }

    private List<Purchase> createDummyPurchases() {
        List<Purchase> purchases = new ArrayList<>();
        purchases.add(new Purchase("29.03.2025", "54,99€", false));
        purchases.add(new Purchase("30.03.2025", "31,20€", true));
        purchases.add(new Purchase("31.03.2025", "9,49€", false));
        return purchases;
    }
}