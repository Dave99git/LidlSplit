package de.th.nuernberg.bme.lidlsplit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class DebtAdapter extends RecyclerView.Adapter<DebtAdapter.DebtViewHolder> {

    public interface OnDebtChangedListener {
        void onDebtChanged();
    }

    private final List<Debt> debts;
    private final OnDebtChangedListener listener;

    public DebtAdapter(List<Debt> debts, OnDebtChangedListener listener) {
        this.debts = debts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DebtViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_debt, parent, false);
        return new DebtViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DebtViewHolder holder, int position) {
        Debt debt = debts.get(position);
        String text = String.format(Locale.getDefault(), "%s schuldet %s %.2f €",
                debt.getDebtor().getName(), debt.getCreditor().getName(), debt.getAmount());
        holder.text.setText(text);
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(debt.isSettled());
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            debt.setSettled(isChecked);
            if (listener != null) listener.onDebtChanged();
        });
    }

    @Override
    public int getItemCount() {
        return debts.size();
    }

    static class DebtViewHolder extends RecyclerView.ViewHolder {
        final TextView text;
        final CheckBox checkBox;

        DebtViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.tvDebtText);
            checkBox = itemView.findViewById(R.id.cbDebt);
        }
    }
}
