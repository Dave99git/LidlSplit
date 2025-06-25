package de.th.nuernberg.bme.lidlsplit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NewPurchaseActivity extends AppCompatActivity {

    private AppDatabaseHelper dbHelper;
    private PersonSelectAdapter personAdapter;
    private ReceiptItemAdapter itemAdapter;
    private final List<PurchaseItem> items = new ArrayList<>();
    private final ActivityResultLauncher<String> filePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::processImage);

    private TextView navPurchases;
    private TextView navPeople;
    private TextView tvResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_purchase);

        dbHelper = new AppDatabaseHelper(this);

        RecyclerView personRecycler = findViewById(R.id.recyclerPersonsSelect);
        personRecycler.setLayoutManager(new LinearLayoutManager(this));
        personAdapter = new PersonSelectAdapter(dbHelper.getAllPersons());
        personRecycler.setAdapter(personAdapter);

        RecyclerView itemRecycler = findViewById(R.id.recyclerItems);
        itemRecycler.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new ReceiptItemAdapter(items, dbHelper.getAllPersons());
        itemRecycler.setAdapter(itemAdapter);

        Button upload = findViewById(R.id.btnUploadReceipt);
        upload.setOnClickListener(v -> filePicker.launch("image/*"));

        Button create = findViewById(R.id.btnCreateInvoice);
        create.setOnClickListener(v -> createInvoice());

        tvResult = findViewById(R.id.tvResult);

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
        items.clear();
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(" ");
            if (parts.length < 2) continue;
            String pricePart = parts[parts.length - 1].replace("€", "").replace(",", ".");
            try {
                double price = Double.parseDouble(pricePart);
                StringBuilder nameBuilder = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (i > 0) nameBuilder.append(' ');
                    nameBuilder.append(parts[i]);
                }
                items.add(new PurchaseItem(nameBuilder.toString(), price));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void createInvoice() {
        if (!itemAdapter.allItemsAssigned()) {
            Toast.makeText(this, getString(R.string.error_assign_person), Toast.LENGTH_LONG).show();
            return;
        }
        Map<Long, Double> totals = itemAdapter.calculateTotals();
        StringBuilder sb = new StringBuilder();
        for (Person p : dbHelper.getAllPersons()) {
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
}
