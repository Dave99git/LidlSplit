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

import java.util.ArrayList;
import java.util.List;

public class PersonsActivity extends AppCompatActivity {

    private TextView navPurchases;
    private TextView navPeople;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_persons);

        // RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerPersons);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        PersonAdapter adapter = new PersonAdapter(createDummyPersons());
        recyclerView.setAdapter(adapter);

        // Add person button
        Button addButton = findViewById(R.id.btnAddPerson);
        addButton.setOnClickListener(v ->
                Toast.makeText(this, "+ Person hinzufügen", Toast.LENGTH_SHORT).show());

        // Filter button
        ImageButton filterButton = findViewById(R.id.btnFilter);
        filterButton.setOnClickListener(v ->
                Toast.makeText(this, "Filter", Toast.LENGTH_SHORT).show());

        // Footer navigation
        navPurchases = findViewById(R.id.navPurchases);
        navPeople = findViewById(R.id.navPeople);

        // Initially people tab active
        activateTab(navPeople, navPurchases);

        navPurchases.setOnClickListener(v -> {
            activateTab(navPurchases, navPeople);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        navPeople.setOnClickListener(v -> activateTab(navPeople, navPurchases));
    }

    private void activateTab(TextView active, TextView inactive) {
        active.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_active));
        inactive.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_inactive));
    }

    private List<Person> createDummyPersons() {
        List<Person> people = new ArrayList<>();
        people.add(new Person("Alice"));
        people.add(new Person("Bob"));
        people.add(new Person("Charlie"));
        return people;
    }
}
