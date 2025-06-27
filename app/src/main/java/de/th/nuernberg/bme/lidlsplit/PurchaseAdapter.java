package de.th.nuernberg.bme.lidlsplit;

import android.view.LayoutInflater;
import android.view.View;
import android.app.AlertDialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PurchaseAdapter extends RecyclerView.Adapter<PurchaseAdapter.PurchaseViewHolder> {

    private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public interface OnPurchaseEditListener {
        void onEdit(Purchase purchase);
    }

    private final List<Purchase> purchases;
    private final AppDatabaseHelper dbHelper;
    private final OnPurchaseEditListener editListener;

    public PurchaseAdapter(List<Purchase> purchases, AppDatabaseHelper dbHelper, OnPurchaseEditListener listener) {
        this.purchases = purchases;
        this.dbHelper = dbHelper;
        this.editListener = listener;
    }

    public void updateData(List<Purchase> newPurchases) {
        purchases.clear();
        purchases.addAll(newPurchases);
        notifyDataSetChanged();
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
        holder.amount.setText(String.format(java.util.Locale.getDefault(), "%.2f€", purchase.getAmount()));
        String statusText = purchase.isPaid() ? "Status: bezahlt" : "Status: offen";
        holder.status.setText(statusText);
        holder.itemView.setOnClickListener(v -> editListener.onEdit(purchase));
        holder.edit.setOnClickListener(v -> editListener.onEdit(purchase));
        holder.delete.setOnClickListener(v -> showDeleteDialog(v.getContext(), purchase, holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return purchases.size();
    }

    static class PurchaseViewHolder extends RecyclerView.ViewHolder {
        final TextView date;
        final TextView amount;
        final TextView status;
        final ImageButton edit;
        final ImageButton delete;

        PurchaseViewHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.tvDate);
            amount = itemView.findViewById(R.id.tvAmount);
            status = itemView.findViewById(R.id.tvStatus);
            edit = itemView.findViewById(R.id.btnEditPurchase);
            delete = itemView.findViewById(R.id.btnDeletePurchase);
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

    private void showDeleteDialog(Context context, Purchase purchase, int position) {
        new AlertDialog.Builder(context)
                .setMessage(R.string.confirm_delete_purchase)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    dbHelper.deletePurchase(purchase.getId());
                    purchases.remove(position);
                    notifyItemRemoved(position);
                })
                .show();
    }
}
