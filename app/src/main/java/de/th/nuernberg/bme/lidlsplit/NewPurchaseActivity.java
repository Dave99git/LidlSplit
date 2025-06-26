package de.th.nuernberg.bme.lidlsplit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private TextView tvResult;
    private TextView tvPaidLabel;

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

        tvResult = findViewById(R.id.tvResult);
        tvPaidLabel = findViewById(R.id.tvPaidLabel);

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
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(result -> {
                        parseText(result.getText());
                        itemAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "OCR failed", Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            Toast.makeText(this, "Image error", Toast.LENGTH_SHORT).show();
        }
    }

    private void parseText(String text) {
        ReceiptParser parser = new ReceiptParser();
        ReceiptData data = parser.parse(text);

        items.clear();
        items.addAll(data.getItems());

        StringBuilder sb = new StringBuilder();
        if (data.getDateTime() != null) {
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            sb.append(df.format(data.getDateTime())).append("\n");
        }
        if (data.getStreet() != null) {
            sb.append(data.getStreet()).append("\n");
        }
        if (data.getCity() != null) {
            sb.append(data.getCity()).append("\n");
        }
        if (data.getTotal() != 0.0) {
            sb.append(String.format(Locale.getDefault(), "Gesamt: %.2f€", data.getTotal()));
        }
        tvResult.setText(sb.toString());
    }

    private void createInvoice() {
        if (!itemAdapter.allItemsAssigned()) {
            Toast.makeText(this, getString(R.string.error_assign_person), Toast.LENGTH_LONG).show();
            return;
        }
        Map<Long, Double> totals = itemAdapter.calculateTotals();
        StringBuilder sb = new StringBuilder();
        for (Person p : selectedPersons) {
            Double value = totals.get(p.getId());
            if (value != null) {
                sb.append(String.format(Locale.getDefault(), "%s: %.2f€\n", p.getName(), value));
            }
        }
        tvResult.setText(sb.toString());
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
