package de.th.nuernberg.bme.lidlsplit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PersonSelectAdapter extends RecyclerView.Adapter<PersonSelectAdapter.PersonSelectViewHolder> {

    private final List<Person> people;
    private final Set<Long> selectedIds = new HashSet<>();

    public PersonSelectAdapter(List<Person> people) {
        this.people = people;
    }

    public void updateData(List<Person> newPeople) {
        people.clear();
        people.addAll(newPeople);
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public Set<Long> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }

    @NonNull
    @Override
    public PersonSelectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person_checkbox, parent, false);
        return new PersonSelectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonSelectViewHolder holder, int position) {
        final Person person = people.get(position);
        holder.name.setText(person.getName());
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedIds.contains(person.getId()));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedIds.add(person.getId());
            } else {
                selectedIds.remove(person.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return people.size();
    }

    static class PersonSelectViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final CheckBox checkBox;

        PersonSelectViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvName);
            checkBox = itemView.findViewById(R.id.cbPerson);
        }
    }
}
