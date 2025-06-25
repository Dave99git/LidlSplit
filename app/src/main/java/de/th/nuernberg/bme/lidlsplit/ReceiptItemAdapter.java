package de.th.nuernberg.bme.lidlsplit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReceiptItemAdapter extends RecyclerView.Adapter<ReceiptItemAdapter.ItemViewHolder> {

    private final List<PurchaseItem> items;
    private final List<Person> persons;
    private final Map<Integer, Set<Long>> assignments = new HashMap<>();

    public ReceiptItemAdapter(List<PurchaseItem> items, List<Person> persons) {
        this.items = items;
        this.persons = persons;
    }

    public Map<Long, Double> calculateTotals() {
        Map<Long, Double> totals = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            PurchaseItem item = items.get(i);
            Set<Long> selected = assignments.get(i);
            if (selected == null || selected.isEmpty()) {
                continue;
            }
            double share = item.getPrice() / selected.size();
            for (Long id : selected) {
                double current = totals.containsKey(id) ? totals.get(id) : 0.0;
                totals.put(id, current + share);
            }
        }
        return totals;
    }

    public boolean allItemsAssigned() {
        for (int i = 0; i < items.size(); i++) {
            Set<Long> sel = assignments.get(i);
            if (sel == null || sel.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        PurchaseItem item = items.get(position);
        holder.name.setText(item.getName());
        holder.price.setText(String.format("%.2f€", item.getPrice()));
        holder.container.removeAllViews();

        Context ctx = holder.container.getContext();
        CheckBox allBox = new CheckBox(ctx);
        allBox.setText("Alle");
        holder.container.addView(allBox);

        List<CheckBox> personBoxes = new ArrayList<>();
        for (Person p : persons) {
            CheckBox cb = new CheckBox(ctx);
            cb.setText(p.getName());
            cb.setTag(p.getId());
            holder.container.addView(cb);
            personBoxes.add(cb);
        }

        Set<Long> selected = assignments.get(position);
        if (selected == null) {
            selected = new HashSet<>();
            assignments.put(position, selected);
        }

        for (CheckBox cb : personBoxes) {
            long id = (long) cb.getTag();
            cb.setChecked(selected.contains(id));
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selected.add(id);
                } else {
                    selected.remove(id);
                    allBox.setOnCheckedChangeListener(null);
                    allBox.setChecked(false);
                    allBox.setOnCheckedChangeListener((b, checked) -> toggleAll(checked, personBoxes, selected));
                }
            });
        }

        allBox.setOnCheckedChangeListener((buttonView, isChecked) -> toggleAll(isChecked, personBoxes, selected));
    }

    private void toggleAll(boolean checked, List<CheckBox> boxes, Set<Long> sel) {
        if (checked) {
            for (CheckBox cb : boxes) {
                cb.setOnCheckedChangeListener(null);
                cb.setChecked(true);
                cb.setOnCheckedChangeListener((buttonView, isC) -> {
                    if (isC) {
                        sel.add((Long) cb.getTag());
                    } else {
                        sel.remove((Long) cb.getTag());
                    }
                });
                sel.add((Long) cb.getTag());
            }
        } else {
            for (CheckBox cb : boxes) {
                cb.setOnCheckedChangeListener(null);
                cb.setChecked(false);
                cb.setOnCheckedChangeListener((buttonView, isC) -> {
                    if (isC) {
                        sel.add((Long) cb.getTag());
                    } else {
                        sel.remove((Long) cb.getTag());
                    }
                });
            }
            sel.clear();
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView price;
        final LinearLayout container;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvItemName);
            price = itemView.findViewById(R.id.tvItemPrice);
            container = itemView.findViewById(R.id.checkboxContainer);
        }
    }
}
