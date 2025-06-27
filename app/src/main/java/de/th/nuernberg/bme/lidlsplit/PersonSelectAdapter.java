package de.th.nuernberg.bme.lidlsplit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PersonSelectAdapter extends RecyclerView.Adapter<PersonSelectAdapter.PersonSelectViewHolder> {

    private final List<Person> people;
    /** ID of the currently selected payer or -1 if none selected */
    private long payerId = -1;

    public PersonSelectAdapter(List<Person> people) {
        this.people = people;
    }

    public void updateData(List<Person> newPeople) {
        people.clear();
        people.addAll(newPeople);
        payerId = -1;
        notifyDataSetChanged();
    }

    /**
     * Returns the currently selected payer id or -1 if none was chosen.
     */
    public long getPayerId() {
        return payerId;
    }

    public void setPayerId(long id) {
        this.payerId = id;
        notifyDataSetChanged();
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
        holder.checkBox.setChecked(person.getId() == payerId);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                payerId = person.getId();
                notifyDataSetChanged();
            } else if (payerId == person.getId()) {
                payerId = -1;
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
