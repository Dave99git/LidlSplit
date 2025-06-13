package de.th.nuernberg.bme.lidlsplit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PurchaseAdapter extends RecyclerView.Adapter<PurchaseAdapter.PurchaseViewHolder> {

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
        holder.date.setText("Einkauf vom " + purchase.getDate());
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
}
