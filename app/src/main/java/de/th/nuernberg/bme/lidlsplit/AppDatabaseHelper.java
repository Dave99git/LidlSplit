package de.th.nuernberg.bme.lidlsplit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "app.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_PERSONS = "persons";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";

    public static final String TABLE_PURCHASES = "purchases";

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createPersons = "CREATE TABLE " + TABLE_PERSONS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT NOT NULL)";
        db.execSQL(createPersons);

        String createPurchases = "CREATE TABLE " + TABLE_PURCHASES + " (id INTEGER PRIMARY KEY AUTOINCREMENT)";
        db.execSQL(createPurchases);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PERSONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PURCHASES);
        onCreate(db);
    }

    public long addPerson(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        return db.insert(TABLE_PERSONS, null, values);
    }

    public List<Person> getAllPersons() {
        List<Person> people = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PERSONS, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
            people.add(new Person(id, name));
        }
        cursor.close();
        return people;
    }

    /**
     * Update the name of a person in the database.
     *
     * @param id       database id of the person
     * @param newName  new name to store
     * @return number of affected rows
     */
    public int updatePerson(long id, String newName) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, newName);
        return db.update(TABLE_PERSONS, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }

    /**
     * Delete a person from the database.
     *
     * @param id database id of the person to remove
     * @return number of affected rows
     */
    public int deletePerson(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_PERSONS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }
}
