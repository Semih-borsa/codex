package com.semih.cepbutcem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.time.*;
import java.util.*;
import org.json.*;

/** Local-only database; transactions make recurring generation and restore atomic. */
public final class Ledger extends SQLiteOpenHelper {
  public Ledger(Context context) {
    this(context, "cepbutcem.db");
  }

  Ledger(Context context, String name) {
    super(context, name, null, 2);
  }

  @Override
  public void onConfigure(SQLiteDatabase db) {
    db.setForeignKeyConstraintsEnabled(true);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE accounts (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, opening"
            + " INTEGER NOT NULL DEFAULT 0)");
    db.execSQL(
        "CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, kind"
            + " TEXT NOT NULL CHECK(kind IN ('I','E')))");
    db.execSQL(
        "CREATE TABLE recurring (id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL"
            + " CHECK(kind IN ('I','E')), amount INTEGER NOT NULL CHECK(amount>0), account_id"
            + " INTEGER NOT NULL REFERENCES accounts(id), category_id INTEGER NOT NULL REFERENCES"
            + " categories(id), note TEXT NOT NULL DEFAULT '', next_date TEXT NOT NULL, day INTEGER"
            + " NOT NULL CHECK(day BETWEEN 1 AND 31))");
    db.execSQL(
        "CREATE TABLE entries (id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL CHECK(kind"
            + " IN ('I','E','T')), amount INTEGER NOT NULL CHECK(amount>0), date TEXT NOT NULL,"
            + " account_id INTEGER NOT NULL REFERENCES accounts(id), target_id INTEGER REFERENCES"
            + " accounts(id), category_id INTEGER REFERENCES categories(id), note TEXT NOT NULL"
            + " DEFAULT '', recurring_id INTEGER REFERENCES recurring(id) ON DELETE SET NULL,"
            + " occurrence TEXT)");
    db.execSQL("CREATE UNIQUE INDEX recurring_once ON entries(recurring_id,occurrence)");
    db.execSQL("CREATE INDEX entries_date ON entries(date)");
    db.execSQL(
        "CREATE TABLE budgets (category_id INTEGER PRIMARY KEY REFERENCES categories(id), amount"
            + " INTEGER NOT NULL CHECK(amount>0))");
    createPlanning(db);
    db.execSQL("INSERT INTO accounts(name,opening) VALUES ('Nakit',0),('Banka',0)");
    for (String name :
        Arrays.asList(
            "Market",
            "Yeme & içme",
            "Ulaşım",
            "Ev & kira",
            "Faturalar",
            "Sağlık",
            "Alışveriş",
            "Eğlence",
            "Eğitim",
            "Diğer gider")) insertCategory(db, name, "E");
    for (String name : Arrays.asList("Maaş", "Ek gelir", "Yatırım", "Hediye", "Diğer gelir"))
      insertCategory(db, name, "I");
  }

  private void insertCategory(SQLiteDatabase db, String name, String kind) {
    ContentValues v = new ContentValues();
    v.put("name", name);
    v.put("kind", kind);
    db.insertOrThrow("categories", null, v);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion == 1 && newVersion == 2) createPlanning(db);
    else throw new IllegalStateException("Desteklenmeyen veritabanı sürümü.");
  }

  private void createPlanning(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE planning (id INTEGER PRIMARY KEY CHECK(id=1), data TEXT NOT NULL)");
  }

  public Planning planning() throws JSONException {
    try (Cursor c = getReadableDatabase().rawQuery("SELECT data FROM planning WHERE id=1", null)) {
      return c.moveToFirst()
          ? validatePlanning(
              new JSONObject(c.getString(0)), accounts(), categories(null), entries())
          : new Planning();
    }
  }

  private Planning validatePlanning(
      JSONObject o,
      List<Finance.Account> accounts,
      List<Finance.Category> categories,
      List<Finance.Entry> entries)
      throws JSONException {
    Set<Long> ids = new HashSet<>();
    Map<Long, String> kinds = new HashMap<>();
    Map<Long, Finance.Entry> records = new HashMap<>();
    for (Finance.Account a : accounts) ids.add(a.id);
    for (Finance.Category c : categories) kinds.put(c.id, c.kind);
    for (Finance.Entry e : entries) records.put(e.id, e);
    return Planning.parse(o, ids, kinds, records);
  }

  public void savePlanning(Planning p) throws JSONException {
    JSONObject o = p.json();
    validatePlanning(o, accounts(), categories(null), entries());
    ContentValues v = new ContentValues();
    v.put("id", 1);
    v.put("data", o.toString());
    getWritableDatabase()
        .insertWithOnConflict("planning", null, v, SQLiteDatabase.CONFLICT_REPLACE);
  }

  /** The expense and its paid marker commit together; repeat clicks cannot double charge. */
  public void payBill(long id, LocalDate date) throws JSONException {
    validDate(date.toString());
    if (date.isAfter(LocalDate.now()))
      throw new IllegalArgumentException("Ödeme tarihi gelecekte olamaz.");
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      Planning p = planning();
      Planning.Bill found = null;
      for (Planning.Bill b : p.bills) if (b.id == id) found = b;
      if (found == null || found.paid != 0)
        throw new IllegalArgumentException("Fatura zaten ödenmiş veya bulunamadı.");
      Finance.Entry e =
          new Finance.Entry(
              0,
              "E",
              found.amount,
              date,
              found.account,
              0,
              found.category,
              "Fatura: " + found.name);
      saveEntry(e);
      found.paid = e.id;
      if (found.monthly) {
        Planning.Bill next = new Planning.Bill();
        next.id = p.nextBillId();
        next.name = found.name;
        next.amount = found.amount;
        next.account = found.account;
        next.category = found.category;
        next.monthly = true;
        next.day = found.day;
        next.due = Finance.nextMonthly(found.due, found.day);
        p.bills.add(next);
      }
      savePlanning(p);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public void undoBill(long id) throws JSONException {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      Planning p = planning();
      for (Planning.Bill b : p.bills)
        if (b.id == id && b.paid != 0) {
          db.delete("entries", "id=?", new String[] {Long.toString(b.paid)});
          b.paid = 0;
          // A generated next bill is retained, preventing loss of later edits/payments.
          b.monthly = false;
        }
      savePlanning(p);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  private void protectBillEntry(long id) {
    try {
      for (Planning.Bill b : planning().bills)
        if (b.paid == id && id != 0)
          throw new IllegalArgumentException(
              "Bu kayıt ödenen bir faturaya bağlı. Fatura ekranında ödemeyi geri alarak"
                  + " düzenleyin.");
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
  }

  public List<Finance.Account> accounts() {
    List<Finance.Account> result = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase().rawQuery("SELECT id,name,opening FROM accounts ORDER BY id", null)) {
      while (c.moveToNext())
        result.add(new Finance.Account(c.getLong(0), c.getString(1), c.getLong(2)));
    }
    return result;
  }

  public List<Finance.Category> categories(String kind) {
    List<Finance.Category> result = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT id,name,kind FROM categories"
                    + (kind == null ? "" : " WHERE kind=?")
                    + " ORDER BY id",
                kind == null ? null : new String[] {kind})) {
      while (c.moveToNext())
        result.add(new Finance.Category(c.getLong(0), c.getString(1), c.getString(2)));
    }
    return result;
  }

  public List<Finance.Entry> entries() {
    List<Finance.Entry> result = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT id,kind,amount,date,account_id,target_id,category_id,note FROM entries"
                    + " ORDER BY date DESC,id DESC",
                null)) {
      while (c.moveToNext())
        result.add(
            new Finance.Entry(
                c.getLong(0),
                c.getString(1),
                c.getLong(2),
                LocalDate.parse(c.getString(3)),
                c.getLong(4),
                c.getLong(5),
                c.getLong(6),
                c.getString(7)));
    }
    return result;
  }

  private ContentValues entryValues(Finance.Entry e) {
    Finance.validate(e);
    ContentValues v = new ContentValues();
    v.put("kind", e.kind);
    v.put("amount", e.amount);
    v.put("date", e.date.toString());
    v.put("account_id", e.accountId);
    v.put("note", e.note == null ? "" : e.note);
    if (Finance.TRANSFER.equals(e.kind)) {
      v.put("target_id", e.targetId);
      v.putNull("category_id");
    } else {
      v.put("category_id", e.categoryId);
      v.putNull("target_id");
    }
    return v;
  }

  private void validateReferences(Finance.Entry e) {
    Finance.validate(e);
    boolean source = false, target = false, category = false;
    for (Finance.Account a : accounts()) {
      if (a.id == e.accountId) source = true;
      if (a.id == e.targetId) target = true;
    }
    for (Finance.Category c : categories(null))
      if (c.id == e.categoryId && c.kind.equals(e.kind)) category = true;
    if (!source || (Finance.TRANSFER.equals(e.kind) ? !target : !category))
      throw new IllegalArgumentException("Hesap veya kategori işlemle uyumlu değil.");
  }

  public void saveEntry(Finance.Entry e) {
    if (e.id != 0) protectBillEntry(e.id);
    validateReferences(e);
    if (e.id == 0) e.id = getWritableDatabase().insertOrThrow("entries", null, entryValues(e));
    else
      getWritableDatabase()
          .update("entries", entryValues(e), "id=?", new String[] {Long.toString(e.id)});
  }

  public void deleteEntry(long id) {
    protectBillEntry(id);
    getWritableDatabase().delete("entries", "id=?", new String[] {Long.toString(id)});
  }

  public long saveAccount(long id, String name, long opening) {
    if (name.trim().isEmpty() || name.trim().length() > 60)
      throw new IllegalArgumentException("Hesap adı 1–60 karakter olmalı.");
    ContentValues v = new ContentValues();
    v.put("name", name.trim());
    v.put("opening", opening);
    if (id == 0) return getWritableDatabase().insertOrThrow("accounts", null, v);
    if (getWritableDatabase().update("accounts", v, "id=?", new String[] {Long.toString(id)}) != 1)
      throw new IllegalArgumentException("Hesap bulunamadı.");
    return id;
  }

  /** Create the card's account and metadata together, or update the existing definition. */
  public void saveCard(Planning.Card card, String newName, long opening) throws JSONException {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      Planning p = planning();
      if (card.account == 0) card.account = saveAccount(0, newName, opening);
      p.cards.removeIf(c -> c.account == card.account);
      p.cards.add(card);
      savePlanning(p);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public void removeCard(long account) throws JSONException {
    Planning p = planning();
    p.cards.removeIf(c -> c.account == account);
    savePlanning(p);
  }

  /** Explicit UI confirmation covers all related records. Foreign keys remain enabled. */
  public void deleteAccount(long id) throws JSONException {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      List<Finance.Account> list = accounts();
      if (list.stream().noneMatch(a -> a.id == id))
        throw new IllegalArgumentException("Hesap bulunamadı.");
      if (list.size() <= 1)
        throw new IllegalArgumentException("En az bir hesap kalmalı. Önce yeni bir hesap ekle.");
      Planning p = planning();
      p.cards.removeIf(c -> c.account == id);
      p.bills.removeIf(b -> b.account == id);
      String value = Long.toString(id);
      db.delete("entries", "account_id=? OR target_id=?", new String[] {value, value});
      db.delete("recurring", "account_id=?", new String[] {value});
      db.delete("accounts", "id=?", new String[] {value});
      savePlanning(p);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public void saveCategory(long id, String name, String kind) {
    if (name.trim().isEmpty() || name.trim().length() > 60)
      throw new IllegalArgumentException("Kategori adı 1–60 karakter olmalı.");
    ContentValues v = new ContentValues();
    v.put("name", name.trim());
    v.put("kind", kind);
    if (id == 0) getWritableDatabase().insertOrThrow("categories", null, v);
    else getWritableDatabase().update("categories", v, "id=?", new String[] {Long.toString(id)});
  }

  public Map<Long, Long> budgets() {
    Map<Long, Long> result = new LinkedHashMap<>();
    try (Cursor c =
        getReadableDatabase().rawQuery("SELECT category_id,amount FROM budgets", null)) {
      while (c.moveToNext()) result.put(c.getLong(0), c.getLong(1));
    }
    return result;
  }

  public void budget(long category, long amount) {
    if (amount == 0) {
      getWritableDatabase()
          .delete("budgets", "category_id=?", new String[] {Long.toString(category)});
      return;
    }
    if (amount < 0) throw new IllegalArgumentException("Bütçe pozitif olmalı.");
    boolean valid = false;
    for (Finance.Category c : categories("E")) if (c.id == category) valid = true;
    if (!valid) throw new IllegalArgumentException("Gider kategorisi seçin.");
    ContentValues v = new ContentValues();
    v.put("category_id", category);
    v.put("amount", amount);
    getWritableDatabase().insertWithOnConflict("budgets", null, v, SQLiteDatabase.CONFLICT_REPLACE);
  }

  public void addRecurring(Finance.Entry e) {
    validateReferences(e);
    if (Finance.TRANSFER.equals(e.kind))
      throw new IllegalArgumentException("Düzenli transfer desteklenmiyor.");
    ContentValues v = new ContentValues();
    v.put("kind", e.kind);
    v.put("amount", e.amount);
    v.put("account_id", e.accountId);
    v.put("category_id", e.categoryId);
    v.put("note", e.note == null ? "" : e.note);
    v.put("next_date", e.date.toString());
    v.put("day", e.date.getDayOfMonth());
    getWritableDatabase().insertOrThrow("recurring", null, v);
  }

  public JSONArray recurring() {
    try {
      return table("recurring");
    } catch (JSONException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public void deleteRecurring(long id) {
    getWritableDatabase().delete("recurring", "id=?", new String[] {Long.toString(id)});
  }

  public int processRecurring(LocalDate today) {
    SQLiteDatabase db = getWritableDatabase();
    int count = 0;
    db.beginTransaction();
    try (Cursor c =
        db.rawQuery(
            "SELECT id,kind,amount,account_id,category_id,note,next_date,day FROM recurring WHERE"
                + " next_date<=?",
            new String[] {today.toString()})) {
      while (c.moveToNext()) {
        LocalDate date = LocalDate.parse(c.getString(6));
        int day = c.getInt(7);
        while (!date.isAfter(today)) {
          Finance.Entry e =
              new Finance.Entry(
                  0,
                  c.getString(1),
                  c.getLong(2),
                  date,
                  c.getLong(3),
                  0,
                  c.getLong(4),
                  c.getString(5));
          ContentValues v = entryValues(e);
          v.put("recurring_id", c.getLong(0));
          v.put("occurrence", date.toString());
          if (db.insertWithOnConflict("entries", null, v, SQLiteDatabase.CONFLICT_IGNORE) != -1)
            count++;
          date = Finance.nextMonthly(date, day);
        }
        ContentValues next = new ContentValues();
        next.put("next_date", date.toString());
        db.update("recurring", next, "id=?", new String[] {Long.toString(c.getLong(0))});
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    return count;
  }

  private JSONArray table(String name) throws JSONException {
    JSONArray rows = new JSONArray();
    try (Cursor c = getReadableDatabase().rawQuery("SELECT * FROM " + name + " ORDER BY 1", null)) {
      while (c.moveToNext()) {
        JSONObject row = new JSONObject();
        for (int i = 0; i < c.getColumnCount(); i++) {
          if (c.isNull(i)) row.put(c.getColumnName(i), JSONObject.NULL);
          else if (c.getType(i) == Cursor.FIELD_TYPE_INTEGER)
            row.put(c.getColumnName(i), c.getLong(i));
          else row.put(c.getColumnName(i), c.getString(i));
        }
        rows.put(row);
      }
    }
    return rows;
  }

  public String backup() throws JSONException {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      JSONObject result = new JSONObject();
      result.put("format", "cepbutcem");
      result.put("version", 3);
      result.put("planning", planning().json());
      result.put("created", LocalDateTime.now().toString());
      for (String name : Arrays.asList("accounts", "categories", "recurring", "entries", "budgets"))
        result.put(name, table(name));
      db.setTransactionSuccessful();
      return result.toString(2);
    } finally {
      db.endTransaction();
    }
  }

  private long integer(JSONObject row, String key) throws JSONException {
    Object o = row.get(key);
    if (!(o instanceof Number) || !o.toString().matches("-?\\d+"))
      throw new JSONException("Tam sayı gerekli: " + key);
    return row.getLong(key);
  }

  private String text(JSONObject row, String key, int max) throws JSONException {
    Object o = row.get(key);
    if (!(o instanceof String) || ((String) o).length() > max)
      throw new JSONException("Geçersiz metin: " + key);
    return (String) o;
  }

  private long amount(JSONObject row, String key, boolean positive) throws JSONException {
    long n = integer(row, key);
    if (n < (positive ? 1 : -100_000_000_000L) || n > 100_000_000_000L)
      throw new JSONException("Geçersiz tutar.");
    return n;
  }

  private Set<Long> ids(JSONArray rows) throws JSONException {
    Set<Long> set = new HashSet<>();
    for (int i = 0; i < rows.length(); i++) {
      long id = integer(rows.getJSONObject(i), "id");
      if (id <= 0 || !set.add(id)) throw new JSONException("Geçersiz veya yinelenen kimlik.");
    }
    return set;
  }

  private void validDate(String date) {
    LocalDate d = LocalDate.parse(date);
    if (d.getYear() < 2000 || d.getYear() > 2100 || !date.equals(d.toString()))
      throw new IllegalArgumentException("Tarih 2000–2100 aralığında olmalı.");
  }

  /** Validate all rows before any deletion. Failed restores roll back the original data. */
  public void restore(String input) throws JSONException {
    JSONObject root = new JSONObject(input);
    if (!"cepbutcem".equals(root.getString("format"))
        || (integer(root, "version") < 1 || integer(root, "version") > 3))
      throw new JSONException("Uyumlu Cep Bütçem yedeği değil.");
    Map<String, JSONArray> tables = new LinkedHashMap<>();
    for (String name : Arrays.asList("accounts", "categories", "recurring", "entries", "budgets")) {
      JSONArray rows = root.getJSONArray(name);
      if (rows.length() > 100000) throw new JSONException("Yedek çok büyük.");
      tables.put(name, rows);
    }
    Set<Long> accounts = ids(tables.get("accounts")),
        categories = ids(tables.get("categories")),
        recurring = ids(tables.get("recurring"));
    ids(tables.get("entries"));
    if (accounts.isEmpty() || categories.isEmpty())
      throw new JSONException("Hesap ve kategori gerekli.");
    Map<Long, String> kinds = new HashMap<>();
    for (int i = 0; i < tables.get("accounts").length(); i++) {
      JSONObject row = tables.get("accounts").getJSONObject(i);
      if (text(row, "name", 60).trim().isEmpty()) throw new JSONException("Boş hesap.");
      amount(row, "opening", false);
    }
    for (int i = 0; i < tables.get("categories").length(); i++) {
      JSONObject row = tables.get("categories").getJSONObject(i);
      String kind = text(row, "kind", 1);
      if (!Arrays.asList("I", "E").contains(kind) || text(row, "name", 60).trim().isEmpty())
        throw new JSONException("Geçersiz kategori.");
      kinds.put(integer(row, "id"), kind);
    }
    for (String name : Arrays.asList("recurring", "entries"))
      for (int i = 0; i < tables.get(name).length(); i++) {
        JSONObject row = tables.get(name).getJSONObject(i);
        String kind = text(row, "kind", 1);
        boolean transfer = "T".equals(kind);
        long source = integer(row, "account_id");
        if (!accounts.contains(source)) throw new JSONException("Eksik hesap.");
        if (!Arrays.asList("I", "E", "T").contains(kind)) throw new JSONException("Geçersiz tür.");
        amount(row, "amount", true);
        text(row, "note", 1000);
        if (transfer) {
          if ("recurring".equals(name)
              || !accounts.contains(integer(row, "target_id"))
              || integer(row, "target_id") == source
              || !row.isNull("category_id")) throw new JSONException("Geçersiz transfer.");
        } else if (!categories.contains(integer(row, "category_id"))
            || !kind.equals(kinds.get(integer(row, "category_id"))))
          throw new JSONException("Kategori türü uyumsuz.");
        if ("entries".equals(name)) {
          validDate(text(row, "date", 10));
          if (!transfer && !row.isNull("target_id"))
            throw new JSONException("Geçersiz hedef hesap.");
          if (!row.isNull("recurring_id")) {
            if (!recurring.contains(integer(row, "recurring_id")))
              throw new JSONException("Eksik düzenli işlem.");
            validDate(text(row, "occurrence", 10));
          }
        } else {
          validDate(text(row, "next_date", 10));
          long day = integer(row, "day");
          if (day < 1 || day > 31) throw new JSONException("Geçersiz gün.");
        }
      }
    Set<Long> budgetIds = new HashSet<>();
    for (int i = 0; i < tables.get("budgets").length(); i++) {
      JSONObject row = tables.get("budgets").getJSONObject(i);
      long id = integer(row, "category_id");
      if (!"E".equals(kinds.get(id)) || !budgetIds.add(id))
        throw new JSONException("Geçersiz bütçe.");
      amount(row, "amount", true);
    }
    Planning restoredPlan = new Planning();
    if (integer(root, "version") >= 2) {
      Map<Long, Finance.Entry> records = new HashMap<>();
      for (int i = 0; i < tables.get("entries").length(); i++) {
        JSONObject r = tables.get("entries").getJSONObject(i);
        long id = r.getLong("id");
        records.put(
            id,
            new Finance.Entry(
                id,
                r.getString("kind"),
                r.getLong("amount"),
                LocalDate.parse(r.getString("date")),
                r.getLong("account_id"),
                r.optLong("target_id"),
                r.optLong("category_id"),
                r.getString("note")));
      }
      restoredPlan = Planning.parse(root.getJSONObject("planning"), accounts, kinds, records);
    }
    Map<String, String[]> columns = new LinkedHashMap<>();
    columns.put("accounts", new String[] {"id", "name", "opening"});
    columns.put("categories", new String[] {"id", "name", "kind"});
    columns.put(
        "recurring",
        new String[] {
          "id", "kind", "amount", "account_id", "category_id", "note", "next_date", "day"
        });
    columns.put(
        "entries",
        new String[] {
          "id",
          "kind",
          "amount",
          "date",
          "account_id",
          "target_id",
          "category_id",
          "note",
          "recurring_id",
          "occurrence"
        });
    columns.put("budgets", new String[] {"category_id", "amount"});
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      db.delete("planning", null, null);
      for (String name : Arrays.asList("entries", "budgets", "recurring", "categories", "accounts"))
        db.delete(name, null, null);
      for (String name : tables.keySet())
        for (int i = 0; i < tables.get(name).length(); i++) {
          JSONObject row = tables.get(name).getJSONObject(i);
          ContentValues values = new ContentValues();
          for (String col : columns.get(name)) {
            if (row.isNull(col)) values.putNull(col);
            else if (row.get(col) instanceof Number) values.put(col, row.getLong(col));
            else values.put(col, row.getString(col));
          }
          db.insertOrThrow(name, null, values);
        }
      savePlanning(restoredPlan);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public String csv() {
    Map<Long, String> accountNames = new HashMap<>(), categoryNames = new HashMap<>();
    for (Finance.Account a : accounts()) accountNames.put(a.id, a.name);
    for (Finance.Category c : categories(null)) categoryNames.put(c.id, c.name);
    StringBuilder result =
        new StringBuilder("\uFEFFTarih;Tür;Tutar (TL);Hesap;Hedef hesap;Kategori;Not\r\n");
    for (Finance.Entry e : entries()) {
      String[] row = {
        e.date.toString(),
        e.kind.equals("I") ? "Gelir" : e.kind.equals("E") ? "Gider" : "Transfer",
        Finance.editable(e.amount),
        accountNames.getOrDefault(e.accountId, ""),
        accountNames.getOrDefault(e.targetId, ""),
        categoryNames.getOrDefault(e.categoryId, ""),
        e.note
      };
      for (int i = 0; i < row.length; i++) {
        if (i > 0) result.append(';');
        result.append(Finance.csvCell(safeSpreadsheet(row[i])));
      }
      result.append("\r\n");
    }
    return result.toString();
  }

  private String safeSpreadsheet(String text) {
    int first = 0;
    while (first < text.length() && Character.isWhitespace(text.charAt(first))) first++;
    if (first < text.length() && "=+-@".indexOf(text.charAt(first)) >= 0) return "'" + text;
    if (!text.isEmpty() && "\t\r\n".indexOf(text.charAt(0)) >= 0) return "'" + text;
    return text;
  }
}
