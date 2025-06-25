package de.th.nuernberg.bme.lidlsplit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.PersonViewHolder> {

    private final List<Person> people;

    public PersonAdapter(List<Person> people) {
        this.people = people;
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person, parent, false);
        return new PersonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        Person person = people.get(position);
        holder.name.setText(person.getName());
    }

    @Override
    public int getItemCount() {
        return people.size();
    }

    static class PersonViewHolder extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final TextView name;
        final ImageButton edit;

        PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.imgAvatar);
            name = itemView.findViewById(R.id.tvName);
            edit = itemView.findViewById(R.id.btnEdit);
        }
    }
}
