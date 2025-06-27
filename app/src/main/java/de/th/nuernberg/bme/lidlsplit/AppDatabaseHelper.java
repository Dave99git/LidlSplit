package de.th.nuernberg.bme.lidlsplit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "app.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_PERSONS = "persons";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";

    public static final String TABLE_PURCHASES = "purchases";
    public static final String COLUMN_PURCHASE_DATE = "date";
    public static final String COLUMN_PURCHASE_AMOUNT = "amount";
    public static final String COLUMN_PURCHASE_PAID = "paid";
    public static final String COLUMN_PURCHASE_PAYER = "payer_id";

    public static final String TABLE_PURCHASE_PERSONS = "purchase_persons";
    public static final String TABLE_ARTICLES = "articles";
    public static final String COLUMN_ARTICLE_NAME = "name";
    public static final String COLUMN_ARTICLE_PRICE = "price";
    public static final String TABLE_ARTICLE_PERSONS = "article_persons";
    public static final String TABLE_DEBTS = "debts";

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
                COLUMN_PURCHASE_PAYER + " INTEGER, " +
                COLUMN_PURCHASE_PAID + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + COLUMN_PURCHASE_PAYER + ") REFERENCES " + TABLE_PERSONS + "(" + COLUMN_ID + "))";
        db.execSQL(createPurchases);

        String createPurchasePersons = "CREATE TABLE " + TABLE_PURCHASE_PERSONS + " (" +
                "purchase_id INTEGER NOT NULL, " +
                "person_id INTEGER NOT NULL, " +
                "FOREIGN KEY(purchase_id) REFERENCES " + TABLE_PURCHASES + "(id), " +
                "FOREIGN KEY(person_id) REFERENCES " + TABLE_PERSONS + "(" + COLUMN_ID + "))";
        db.execSQL(createPurchasePersons);

        String createArticles = "CREATE TABLE " + TABLE_ARTICLES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "purchase_id INTEGER NOT NULL, " +
                COLUMN_ARTICLE_NAME + " TEXT NOT NULL, " +
                COLUMN_ARTICLE_PRICE + " REAL NOT NULL, " +
                "FOREIGN KEY(purchase_id) REFERENCES " + TABLE_PURCHASES + "(id))";
        db.execSQL(createArticles);

        String createArticlePersons = "CREATE TABLE " + TABLE_ARTICLE_PERSONS + " (" +
                "article_id INTEGER NOT NULL, " +
                "person_id INTEGER NOT NULL, " +
                "FOREIGN KEY(article_id) REFERENCES " + TABLE_ARTICLES + "(id), " +
                "FOREIGN KEY(person_id) REFERENCES " + TABLE_PERSONS + "(" + COLUMN_ID + "))";
        db.execSQL(createArticlePersons);

        String createDebts = "CREATE TABLE " + TABLE_DEBTS + " (" +
                "purchase_id INTEGER NOT NULL, " +
                "debtor_id INTEGER NOT NULL, " +
                "creditor_id INTEGER NOT NULL, " +
                "settled INTEGER NOT NULL, " +
                "FOREIGN KEY(purchase_id) REFERENCES " + TABLE_PURCHASES + "(id), " +
                "FOREIGN KEY(debtor_id) REFERENCES " + TABLE_PERSONS + "(" + COLUMN_ID + "), " +
                "FOREIGN KEY(creditor_id) REFERENCES " + TABLE_PERSONS + "(" + COLUMN_ID + "))";
        db.execSQL(createDebts);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEBTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ARTICLE_PERSONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ARTICLES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PURCHASE_PERSONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PURCHASES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PERSONS);
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

    public long addPurchase(String date, double amount, long payerId,
                            List<Person> persons,
                            List<Article> articles,
                            Map<Article, List<Person>> assignments,
                            Map<DebtPair, Boolean> debts) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PURCHASE_DATE, date);
        values.put(COLUMN_PURCHASE_AMOUNT, amount);
        values.put(COLUMN_PURCHASE_PAYER, payerId);
        values.put(COLUMN_PURCHASE_PAID, 0);
        long purchaseId = db.insert(TABLE_PURCHASES, null, values);

        for (Person p : persons) {
            ContentValues pv = new ContentValues();
            pv.put("purchase_id", purchaseId);
            pv.put("person_id", p.getId());
            db.insert(TABLE_PURCHASE_PERSONS, null, pv);
        }

        Map<Article, Long> articleIds = new java.util.HashMap<>();
        for (Article a : articles) {
            ContentValues av = new ContentValues();
            av.put("purchase_id", purchaseId);
            av.put(COLUMN_ARTICLE_NAME, a.getName());
            av.put(COLUMN_ARTICLE_PRICE, a.getPrice());
            long aid = db.insert(TABLE_ARTICLES, null, av);
            articleIds.put(a, aid);
        }

        if (assignments != null) {
            for (Map.Entry<Article, List<Person>> e : assignments.entrySet()) {
                Long aid = articleIds.get(e.getKey());
                if (aid == null) continue;
                for (Person p : e.getValue()) {
                    ContentValues ap = new ContentValues();
                    ap.put("article_id", aid);
                    ap.put("person_id", p.getId());
                    db.insert(TABLE_ARTICLE_PERSONS, null, ap);
                }
            }
        }

        if (debts != null) {
            for (Map.Entry<DebtPair, Boolean> d : debts.entrySet()) {
                ContentValues dv = new ContentValues();
                dv.put("purchase_id", purchaseId);
                dv.put("debtor_id", d.getKey().getDebtorId());
                dv.put("creditor_id", d.getKey().getCreditorId());
                dv.put("settled", d.getValue() ? 1 : 0);
                db.insert(TABLE_DEBTS, null, dv);
            }
        }

        updatePaidStatus(purchaseId);
        return purchaseId;
    }

    private void updatePaidStatus(long purchaseId) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(TABLE_DEBTS, new String[]{"settled"}, "purchase_id=?", new String[]{String.valueOf(purchaseId)}, null, null, null);
        boolean paid = true;
        while (c.moveToNext()) {
            if (c.getInt(0) == 0) { paid = false; break; }
        }
        c.close();
        ContentValues v = new ContentValues();
        v.put(COLUMN_PURCHASE_PAID, paid ? 1 : 0);
        db.update(TABLE_PURCHASES, v, "id=?", new String[]{String.valueOf(purchaseId)});
    }

    public int updatePurchase(long id, String date, double amount, long payerId,
                              List<Person> persons,
                              List<Article> articles,
                              Map<Article, List<Person>> assignments,
                              Map<DebtPair, Boolean> debts) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PURCHASE_DATE, date);
        values.put(COLUMN_PURCHASE_AMOUNT, amount);
        values.put(COLUMN_PURCHASE_PAYER, payerId);
        db.update(TABLE_PURCHASES, values, "id=?", new String[]{String.valueOf(id)});

        db.delete(TABLE_PURCHASE_PERSONS, "purchase_id=?", new String[]{String.valueOf(id)});
        db.delete(TABLE_ARTICLE_PERSONS, "article_id IN (SELECT id FROM " + TABLE_ARTICLES + " WHERE purchase_id=?)", new String[]{String.valueOf(id)});
        db.delete(TABLE_ARTICLES, "purchase_id=?", new String[]{String.valueOf(id)});
        db.delete(TABLE_DEBTS, "purchase_id=?", new String[]{String.valueOf(id)});

        for (Person p : persons) {
            ContentValues pv = new ContentValues();
            pv.put("purchase_id", id);
            pv.put("person_id", p.getId());
            db.insert(TABLE_PURCHASE_PERSONS, null, pv);
        }

        Map<Article, Long> articleIds = new java.util.HashMap<>();
        for (Article a : articles) {
            ContentValues av = new ContentValues();
            av.put("purchase_id", id);
            av.put(COLUMN_ARTICLE_NAME, a.getName());
            av.put(COLUMN_ARTICLE_PRICE, a.getPrice());
            long aid = db.insert(TABLE_ARTICLES, null, av);
            articleIds.put(a, aid);
        }

        if (assignments != null) {
            for (Map.Entry<Article, List<Person>> e : assignments.entrySet()) {
                Long aid = articleIds.get(e.getKey());
                if (aid == null) continue;
                for (Person p : e.getValue()) {
                    ContentValues ap = new ContentValues();
                    ap.put("article_id", aid);
                    ap.put("person_id", p.getId());
                    db.insert(TABLE_ARTICLE_PERSONS, null, ap);
                }
            }
        }

        if (debts != null) {
            for (Map.Entry<DebtPair, Boolean> d : debts.entrySet()) {
                ContentValues dv = new ContentValues();
                dv.put("purchase_id", id);
                dv.put("debtor_id", d.getKey().getDebtorId());
                dv.put("creditor_id", d.getKey().getCreditorId());
                dv.put("settled", d.getValue() ? 1 : 0);
                db.insert(TABLE_DEBTS, null, dv);
            }
        }

        updatePaidStatus(id);
        return 1;
    }

    public int deletePurchase(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_PURCHASES, "id=?", new String[]{String.valueOf(id)});
    }

    public Purchase getPurchase(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PURCHASES, null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }

        String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PURCHASE_DATE));
        double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PURCHASE_AMOUNT));
        long payer = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PURCHASE_PAYER));
        cursor.close();

        List<Person> persons = new ArrayList<>();
        Cursor pc = db.rawQuery("SELECT p." + COLUMN_ID + ", p." + COLUMN_NAME + " FROM " + TABLE_PERSONS + " p JOIN " + TABLE_PURCHASE_PERSONS + " pp ON p." + COLUMN_ID + "=pp.person_id WHERE pp.purchase_id=?", new String[]{String.valueOf(id)});
        while (pc.moveToNext()) {
            persons.add(new Person(pc.getLong(0), pc.getString(1)));
        }
        pc.close();

        List<Article> articles = new ArrayList<>();
        Map<Long, Article> articleById = new java.util.HashMap<>();
        Cursor ac = db.query(TABLE_ARTICLES, null, "purchase_id=?", new String[]{String.valueOf(id)}, null, null, null);
        while (ac.moveToNext()) {
            long aid = ac.getLong(ac.getColumnIndexOrThrow(COLUMN_ID));
            String name = ac.getString(ac.getColumnIndexOrThrow(COLUMN_ARTICLE_NAME));
            double price = ac.getDouble(ac.getColumnIndexOrThrow(COLUMN_ARTICLE_PRICE));
            Article a = new Article(name, price);
            articles.add(a);
            articleById.put(aid, a);
        }
        ac.close();

        Map<Article, List<Person>> assignments = new java.util.HashMap<>();
        Cursor ap = db.query(TABLE_ARTICLE_PERSONS, null, "article_id IN (SELECT id FROM " + TABLE_ARTICLES + " WHERE purchase_id=?)", new String[]{String.valueOf(id)}, null, null, null);
        while (ap.moveToNext()) {
            long aid = ap.getLong(ap.getColumnIndexOrThrow("article_id"));
            long pid = ap.getLong(ap.getColumnIndexOrThrow("person_id"));
            Article art = articleById.get(aid);
            if (art == null) continue;
            Person person = null;
            for (Person p : persons) if (p.getId() == pid) { person = p; break; }
            if (person == null) continue;
            assignments.computeIfAbsent(art, k -> new ArrayList<>()).add(person);
        }
        ap.close();

        Map<DebtPair, Boolean> debts = new java.util.HashMap<>();
        Cursor dc = db.query(TABLE_DEBTS, null, "purchase_id=?", new String[]{String.valueOf(id)}, null, null, null);
        while (dc.moveToNext()) {
            long debtor = dc.getLong(dc.getColumnIndexOrThrow("debtor_id"));
            long creditor = dc.getLong(dc.getColumnIndexOrThrow("creditor_id"));
            boolean settled = dc.getInt(dc.getColumnIndexOrThrow("settled")) == 1;
            debts.put(new DebtPair(debtor, creditor), settled);
        }
        dc.close();

        boolean paid = true;
        for (Boolean b : debts.values()) { if (!b) { paid = false; break; } }

        return new Purchase(id, date, amount, paid, payer, persons, articles, assignments, debts);
    }

    public List<Purchase> getAllPurchases() {
        List<Purchase> purchases = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PURCHASES, null, null, null, null, null, "id DESC");
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PURCHASE_DATE));
            double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PURCHASE_AMOUNT));

            Cursor dc = db.query(TABLE_DEBTS, new String[]{"settled"}, "purchase_id=?", new String[]{String.valueOf(id)}, null, null, null);
            boolean paid = true;
            while (dc.moveToNext()) {
                if (dc.getInt(0) == 0) { paid = false; break; }
            }
            dc.close();

            purchases.add(new Purchase(id, date, amount, paid));
        }
        cursor.close();
        return purchases;
    }
}
