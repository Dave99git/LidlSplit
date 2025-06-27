package de.th.nuernberg.bme.lidlsplit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class EditPurchaseActivity extends AppCompatActivity {

    private AppDatabaseHelper dbHelper;
    private long purchaseId;
    private EditText etDate;
    private EditText etAmount;
    private CheckBox cbPaid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_purchase);

        dbHelper = new AppDatabaseHelper(this);

        etDate = findViewById(R.id.etDate);
        etAmount = findViewById(R.id.etAmount);
        cbPaid = findViewById(R.id.cbPaid);
        Button save = findViewById(R.id.btnSavePurchase);

        purchaseId = getIntent().getLongExtra("purchase_id", -1);
        if (purchaseId != -1) {
            Purchase purchase = dbHelper.getPurchase(purchaseId);
            if (purchase != null) {
                etDate.setText(purchase.getDate());
                etAmount.setText(String.format(java.util.Locale.getDefault(), "%.2f", purchase.getAmount()));
                cbPaid.setChecked(purchase.isPaid());
            }
        }

        save.setOnClickListener(v -> saveChanges());
    }

    private void saveChanges() {
        String date = etDate.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        if (date.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Bitte alle Felder ausfüllen", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount = Double.parseDouble(amountStr.replace(',', '.'));
        boolean paid = cbPaid.isChecked();

        if (purchaseId == -1) return;
        dbHelper.updatePurchase(purchaseId, date, amount, paid);
        finish();
    }
}
