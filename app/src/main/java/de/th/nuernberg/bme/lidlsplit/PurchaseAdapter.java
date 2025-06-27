package de.th.nuernberg.bme.lidlsplit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PurchaseAdapter extends RecyclerView.Adapter<PurchaseAdapter.PurchaseViewHolder> {

    private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final List<Purchase> purchases;

    public PurchaseAdapter(List<Purchase> purchases) {
        this.purchases = purchases;
    }

    @NonNull
    @Override
    public PurchaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase, parent, false);
        return new PurchaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PurchaseViewHolder holder, int position) {
        Purchase purchase = purchases.get(position);
        holder.date.setText("Einkauf vom " + formatDate(purchase.getDate()));
        holder.amount.setText(purchase.getAmount());
        String statusText = purchase.isPaid() ? "Status: bezahlt" : "Status: offen";
        holder.status.setText(statusText);
    }

    @Override
    public int getItemCount() {
        return purchases.size();
    }

    static class PurchaseViewHolder extends RecyclerView.ViewHolder {
        final TextView date;
        final TextView amount;
        final TextView status;

        PurchaseViewHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.tvDate);
            amount = itemView.findViewById(R.id.tvAmount);
            status = itemView.findViewById(R.id.tvStatus);
        }
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        try {
            if (raw.contains("T")) {
                LocalDate d = LocalDate.parse(raw.substring(0, 10));
                return d.format(OUTPUT_FORMAT);
            }
            if (raw.matches("\\d{2}\\.\\d{2}\\.\\d{4}.*")) {
                LocalDate d = LocalDate.parse(raw.substring(0, 10), OUTPUT_FORMAT);
                return d.format(OUTPUT_FORMAT);
            }
            if (raw.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                LocalDate d = LocalDate.parse(raw.substring(0, 10));
                return d.format(OUTPUT_FORMAT);
            }
        } catch (Exception ignored) {
        }
        return raw;
    }
}
