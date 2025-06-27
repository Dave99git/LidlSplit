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
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_PERSONS = "persons";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";

    public static final String TABLE_PURCHASES = "purchases";
    public static final String COLUMN_PURCHASE_DATE = "date";
    public static final String COLUMN_PURCHASE_AMOUNT = "amount";
    public static final String COLUMN_PURCHASE_PAID = "paid";

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createPersons = "CREATE TABLE " + TABLE_PERSONS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT NOT NULL)";
        db.execSQL(createPersons);

        String createPurchases = "CREATE TABLE " + TABLE_PURCHASES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PURCHASE_DATE + " TEXT NOT NULL, " +
                COLUMN_PURCHASE_AMOUNT + " REAL NOT NULL, " +
                COLUMN_PURCHASE_PAID + " INTEGER NOT NULL)";
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

    public long addPurchase(String date, double amount, boolean paid) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PURCHASE_DATE, date);
        values.put(COLUMN_PURCHASE_AMOUNT, amount);
        values.put(COLUMN_PURCHASE_PAID, paid ? 1 : 0);
        return db.insert(TABLE_PURCHASES, null, values);
    }

    public List<Purchase> getAllPurchases() {
        List<Purchase> purchases = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PURCHASES, null, null, null, null, null, "id DESC");
        while (cursor.moveToNext()) {
            String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PURCHASE_DATE));
            double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PURCHASE_AMOUNT));
            boolean paid = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PURCHASE_PAID)) == 1;
            purchases.add(new Purchase(date, String.format(java.util.Locale.getDefault(), "%.2f€", amount), paid));
        }
        cursor.close();
        return purchases;
    }
}
