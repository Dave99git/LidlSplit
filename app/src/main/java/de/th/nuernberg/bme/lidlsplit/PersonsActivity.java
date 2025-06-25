package de.th.nuernberg.bme.lidlsplit;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class PersonsActivity extends AppCompatActivity {

    private TextView navPurchases;
    private TextView navPeople;
    private PersonAdapter adapter;
    private AppDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_persons);

        dbHelper = new AppDatabaseHelper(this);

        // RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerPersons);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PersonAdapter(dbHelper.getAllPersons(), dbHelper);
        recyclerView.setAdapter(adapter);

        // Add person button
        Button addButton = findViewById(R.id.btnAddPerson);
        addButton.setOnClickListener(v -> showAddPersonDialog());

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
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });

        navPeople.setOnClickListener(v -> activateTab(navPeople, navPurchases));
    }

    private void showAddPersonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_person, null);
        EditText etName = view.findViewById(R.id.etPersonName);
        builder.setView(view)
                .setTitle(R.string.dialog_add_person_title)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                if (name.isEmpty()) {
                    etName.setError(getString(R.string.hint_person_name));
                    return;
                }
                long id = dbHelper.addPerson(name);
                adapter.addPerson(new Person(id, name));
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void activateTab(TextView active, TextView inactive) {
        active.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_active));
        inactive.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_inactive));
    }

}
