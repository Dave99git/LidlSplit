package de.th.nuernberg.bme.lidlsplit;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.PersonViewHolder> {

    private final List<Person> people;
    private final AppDatabaseHelper dbHelper;

    public PersonAdapter(List<Person> people, AppDatabaseHelper dbHelper) {
        this.people = people;
        this.dbHelper = dbHelper;
    }

    public void addPerson(Person person) {
        people.add(person);
        notifyItemInserted(people.size() - 1);
    }

    public void updateData(List<Person> newPeople) {
        people.clear();
        people.addAll(newPeople);
        notifyDataSetChanged();
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
        holder.edit.setOnClickListener(v -> showEditDialog(v.getContext(), person, position));
        holder.delete.setOnClickListener(v -> showDeleteDialog(v.getContext(), person, position));
    }

    @Override
    public int getItemCount() {
        return people.size();
    }

    private void showEditDialog(Context context, Person person, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_person, null);
        EditText etName = view.findViewById(R.id.etPersonName);
        etName.setText(person.getName());
        builder.setView(view)
                .setTitle(R.string.dialog_edit_person_title)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v -> {
                String newName = etName.getText().toString().trim();
                if (newName.isEmpty()) {
                    etName.setError(context.getString(R.string.hint_person_name));
                    return;
                }
                dbHelper.updatePerson(person.getId(), newName);
                people.set(position, new Person(person.getId(), newName));
                notifyItemChanged(position);
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void showDeleteDialog(Context context, Person person, int position) {
        new AlertDialog.Builder(context)
                .setMessage(R.string.confirm_delete_person)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    dbHelper.deletePerson(person.getId());
                    people.remove(position);
                    notifyItemRemoved(position);
                })
                .show();
    }

    static class PersonViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final ImageButton edit;
        final ImageButton delete;

        PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvName);
            edit = itemView.findViewById(R.id.btnEdit);
            delete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
