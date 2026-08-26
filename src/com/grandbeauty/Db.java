package com.grandbeauty;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Db extends SQLiteOpenHelper {
    private static final String NAME = "bookings.db";
    private static final int VERSION = 2;
    private static Db inst;

    public static synchronized Db get(Context ctx) {
        if (inst == null) inst = new Db(ctx.getApplicationContext());
        return inst;
    }

    private Db(Context c) { super(c, NAME, null, VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE clients(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL, phone TEXT DEFAULT '', note TEXT DEFAULT ''," +
                "created_at INTEGER)");
        db.execSQL("CREATE TABLE appts(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "client_id INTEGER NOT NULL, start_ms INTEGER NOT NULL," +
                "service TEXT NOT NULL, amount REAL, paid REAL, note TEXT DEFAULT ''," +
                "remind INTEGER DEFAULT 1, status TEXT DEFAULT 'up')");
        db.execSQL("CREATE INDEX idx_appts_start ON appts(start_ms)");
        db.execSQL("CREATE INDEX idx_appts_client ON appts(client_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            db.execSQL("ALTER TABLE appts ADD COLUMN paid REAL");
            db.execSQL("UPDATE appts SET paid = amount WHERE paid IS NULL");
        }
    }

    private static Client rdClient(Cursor c) {
        Client x = new Client();
        x.id = c.getLong(0);
        x.name = c.getString(1);
        x.phone = c.getString(2);
        x.note = c.getString(3);
        return x;
    }

    private static Appt rdAppt(Cursor c) {
        Appt a = new Appt();
        a.id = c.getLong(0);
        a.clientId = c.getLong(1);
        a.start = c.getLong(2);
        a.service = c.getString(3);
        if (!c.isNull(4)) a.amount = c.getDouble(4);
        if (!c.isNull(5)) a.paid = c.getDouble(5);
        a.note = c.getString(6);
        a.remind = c.getInt(7) != 0;
        a.status = c.getString(8);
        if (!c.isNull(9)) a.clientName = c.getString(9);
        if (!c.isNull(10)) a.clientPhone = c.getString(10);
        return a;
    }

    private static final String APPT_COLS =
            "a.id, a.client_id, a.start_ms, a.service, a.amount, a.paid, a.note, a.remind, a.status," +
            "c.name, c.phone FROM appts a LEFT JOIN clients c ON c.id = a.client_id";

    public List<Appt> appts(String search, Long clientId) {
        ArrayList<String> args = new ArrayList<>();
        String q = "SELECT " + APPT_COLS + " WHERE 1=1";
        if (clientId != null) {
            q += " AND a.client_id = ?";
            args.add(String.valueOf(clientId));
        }
        if (search != null && !search.trim().isEmpty()) {
            q += " AND (c.name LIKE ? OR a.service LIKE ? OR c.phone LIKE ?)";
            String like = "%" + search.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        q += " ORDER BY a.start_ms ASC";
        Cursor c = getReadableDatabase().rawQuery(q, args.toArray(new String[0]));
        ArrayList<Appt> out = new ArrayList<>();
        while (c.moveToNext()) out.add(rdAppt(c));
        c.close();
        return out;
    }

    public Appt appt(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT " + APPT_COLS + " WHERE a.id = ?", new String[]{String.valueOf(id)});
        Appt a = c.moveToFirst() ? rdAppt(c) : null;
        c.close();
        return a;
    }

    public long putAppt(Appt a) {
        ContentValues v = new ContentValues();
        v.put("client_id", a.clientId);
        v.put("start_ms", a.start);
        v.put("service", a.service);
        v.put("amount", a.amount);
        v.put("paid", a.paid);
        v.put("note", a.note);
        v.put("remind", a.remind ? 1 : 0);
        v.put("status", a.status);
        SQLiteDatabase db = getWritableDatabase();
        if (a.id > 0) {
            db.update("appts", v, "id=?", new String[]{String.valueOf(a.id)});
            return a.id;
        }
        return db.insert("appts", null, v);
    }

    public void setStatus(long id, String status) {
        ContentValues v = new ContentValues();
        v.put("status", status);
        getWritableDatabase().update("appts", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteAppt(long id) {
        getWritableDatabase().delete("appts", "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteClientCascade(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("appts", "client_id=?", new String[]{String.valueOf(id)});
        db.delete("clients", "id=?", new String[]{String.valueOf(id)});
    }

    public void settleClient(long clientId) {
        getWritableDatabase().execSQL(
                "UPDATE appts SET paid = amount WHERE client_id = ? AND amount IS NOT NULL" +
                " AND status != 'canc' AND (paid IS NULL OR paid < amount)",
                new Object[]{clientId});
    }

    public List<Client> clients(String search) {
        String q = "SELECT id, name, phone, note FROM clients";
        List<String> args = new ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            q += " WHERE name LIKE ? OR phone LIKE ?";
            String like = "%" + search.trim() + "%";
            args.add(like);
            args.add(like);
        }
        q += " ORDER BY name COLLATE NOCASE ASC";
        Cursor c = getReadableDatabase().rawQuery(q, args.toArray(new String[0]));
        ArrayList<Client> out = new ArrayList<>();
        while (c.moveToNext()) out.add(rdClient(c));
        c.close();
        return out;
    }

    public Client client(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, name, phone, note FROM clients WHERE id=?",
                new String[]{String.valueOf(id)});
        Client x = c.moveToFirst() ? rdClient(c) : null;
        c.close();
        return x;
    }

    public long putClient(Client x) {
        ContentValues v = new ContentValues();
        v.put("name", x.name);
        v.put("phone", x.phone);
        v.put("note", x.note);
        SQLiteDatabase db = getWritableDatabase();
        if (x.id > 0) {
            db.update("clients", v, "id=?", new String[]{String.valueOf(x.id)});
            return x.id;
        }
        v.put("created_at", System.currentTimeMillis());
        return db.insert("clients", null, v);
    }

    public static class Stats {
        public int visits;
        public double total;
        public double balance;
        public long last;
    }

    public Stats stats(long clientId) {
        Stats s = new Stats();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*)," +
                " IFNULL(SUM(CASE WHEN amount IS NULL THEN 0 ELSE amount END),0)," +
                " IFNULL(SUM(CASE WHEN amount IS NULL OR status='canc' THEN 0" +
                "   ELSE amount - IFNULL(paid, amount) END),0)," +
                " IFNULL(MAX(CASE WHEN status='done' THEN start_ms END),0)" +
                " FROM appts WHERE client_id=?",
                new String[]{String.valueOf(clientId)});
        if (c.moveToFirst()) {
            s.visits = c.getInt(0);
            s.total = c.getDouble(1);
            s.balance = c.getDouble(2);
            s.last = c.getLong(3);
        }
        c.close();
        return s;
    }

    /** clientId -> outstanding balance, only clients that owe > 0 */
    public Map<Long, Double> owedByClient() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT client_id, SUM(amount - IFNULL(paid, amount)) FROM appts" +
                " WHERE amount IS NOT NULL AND status != 'canc'" +
                " AND (paid IS NULL OR paid < amount - 0.001)" +
                " GROUP BY client_id", null);
        HashMap<Long, Double> out = new HashMap<>();
        while (c.moveToNext()) out.put(c.getLong(0), c.getDouble(1));
        c.close();
        return out;
    }

    public double totalOwed() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT IFNULL(SUM(amount - IFNULL(paid, amount)),0) FROM appts" +
                " WHERE amount IS NOT NULL AND status != 'canc'" +
                " AND (paid IS NULL OR paid < amount - 0.001)", null);
        double v = c.moveToFirst() ? c.getDouble(0) : 0;
        c.close();
        return v;
    }

    /** most-overdue regular: done visit 35–180 days ago, lowest last-visit first. null if none */
    public Client regularToNudge() {
        long now = System.currentTimeMillis();
        String[] args = {
                String.valueOf(now - 180L * 86_400_000L),
                String.valueOf(now - 35L * 86_400_000L)
        };
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT c.id, c.name, c.phone, c.note FROM clients c JOIN" +
                " (SELECT client_id, MAX(CASE WHEN status='done' THEN start_ms END) last" +
                "  FROM appts GROUP BY client_id) x ON x.client_id = c.id" +
                " WHERE x.last BETWEEN ? AND ? ORDER BY x.last ASC LIMIT 1", args);
        Client x = c.moveToFirst() ? rdClient(c) : null;
        c.close();
        return x;
    }
}
