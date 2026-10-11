package com.semih.ilacim;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class MainActivity extends Activity {
  static final int TEAL = 0xff087f8c,
      INK = 0xff183341,
      MUTED = 0xff637889,
      BG = 0xfff5f7fb,
      RED = 0xffb74546;
  Store store;
  LinearLayout content, root, nav;
  int tab = 0;
  String query = "";
  EditText barcodeInput;
  final ExecutorService network = Executors.newSingleThreadExecutor();
  final DateTimeFormatter human = DateTimeFormatter.ofPattern("dd.MM.uuuu");

  protected void onCreate(Bundle b) {
    super.onCreate(b);
    store = new Store(this);
    if (b != null) tab = b.getInt("tab", 0);
    render();
    Scheduler.refresh(this);
    if (Build.VERSION.SDK_INT >= 33
        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        && !getSharedPreferences("ui", 0).getBoolean("notificationAsked", false)) {
      new AlertDialog.Builder(this)
          .setTitle("Hatırlatmaları aç")
          .setMessage(
              "Günlük ilaç saatleri ve randevular için bildirim izni gerekir. Ayarlardan daha sonra"
                  + " da açabilirsin.")
          .setPositiveButton(
              "İzin ver",
              (d, w) -> {
                getSharedPreferences("ui", 0).edit().putBoolean("notificationAsked", true).apply();
                requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 12);
              })
          .setNegativeButton(
              "Daha sonra",
              (d, w) ->
                  getSharedPreferences("ui", 0)
                      .edit()
                      .putBoolean("notificationAsked", true)
                      .apply())
          .show();
    }
  }

  protected void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putInt("tab", tab);
  }

  protected void onDestroy() {
    network.shutdownNow();
    store.close();
    super.onDestroy();
  }

  int dp(int v) {
    return (int) (v * getResources().getDisplayMetrics().density + .5f);
  }

  GradientDrawable shape(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    return d;
  }

  LinearLayout column() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  TextView text(String value, int size, int color, boolean bold) {
    TextView t = new TextView(this);
    t.setText(value);
    t.setTextSize(size);
    t.setTextColor(color);
    if (bold) t.setTypeface(null, Typeface.BOLD);
    t.setPadding(0, dp(5), 0, dp(5));
    return t;
  }

  Button button(String label, boolean primary, Runnable action) {
    Button b = new Button(this);
    b.setText(label);
    b.setAllCaps(false);
    b.setTextSize(14);
    b.setTextColor(primary ? Color.WHITE : TEAL);
    b.setBackground(shape(primary ? TEAL : 0xffe8f5f5, 14));
    b.setMinHeight(dp(48));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(50));
    lp.setMargins(0, dp(7), 0, dp(5));
    b.setLayoutParams(lp);
    b.setOnClickListener(v -> action.run());
    return b;
  }

  LinearLayout card(LinearLayout parent) {
    LinearLayout l = column();
    l.setPadding(dp(18), dp(12), dp(18), dp(14));
    l.setBackground(shape(Color.WHITE, 22));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(0, dp(8), 0, dp(6));
    parent.addView(l, p);
    return l;
  }

  void heading(String title, String subtitle) {
    content.addView(text(title, 28, INK, true));
    content.addView(text(subtitle, 14, MUTED, false));
  }

  void message(String title, String body) {
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(body)
        .setPositiveButton("Tamam", null)
        .show();
  }

  void toast(String t) {
    Toast.makeText(this, t, Toast.LENGTH_LONG).show();
  }

  void save() {
    store.save();
    Scheduler.refresh(this);
    render();
  }

  void render() {
    root = column();
    root.setBackgroundColor(BG);
    root.setFitsSystemWindows(true);
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    content = column();
    content.setPadding(dp(20), dp(12), dp(20), dp(24));
    scroll.addView(content);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    nav = new LinearLayout(this);
    nav.setPadding(dp(6), dp(6), dp(6), dp(8));
    nav.setBackgroundColor(Color.WHITE);
    String[] labels = {"Bugün", "İlaçlarım", "Randevular", "Ayarlar"};
    for (int i = 0; i < labels.length; i++) {
      final int target = i;
      Button b =
          button(
              labels[i],
              tab == i,
              () -> {
                tab = target;
                render();
              });
      b.setTextSize(12);
      nav.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1));
    }
    root.addView(nav);
    setContentView(root);
    try {
      switch (tab) {
        case 0:
          today();
          break;
        case 1:
          medicines();
          break;
        case 2:
          appointments();
          break;
        default:
          settings();
      }
    } catch (Exception e) {
      content.addView(
          text(
              "Kayıt görüntülenemedi. Yedeklerini koru ve uygulamayı yeniden aç.", 16, RED, false));
    }
  }

  boolean visible(JSONObject m) {
    return !m.optBoolean("archived", false);
  }

  String name(JSONObject m) {
    return m.optString("name")
        + (m.optString("strength").isBlank() ? "" : " · " + m.optString("strength"));
  }

  String expiryStatus(JSONObject m) {
    LocalDate end = Rules.expiry(m.optString("expiry")).atEndOfMonth();
    long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), end);
    return days < 0
        ? "Süresi doldu"
        : days <= 60 ? "Son " + days + " gün" : "SKT " + m.optString("expiry");
  }

  void today() throws Exception {
    heading(
        "Sağlığına yer aç.",
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern("d MMMM uuuu, EEEE", new Locale("tr", "TR"))));
    int active = 0, warning = 0;
    JSONArray meds = store.list("meds");
    for (int i = 0; i < meds.length(); i++) {
      JSONObject m = meds.getJSONObject(i);
      if (!visible(m)) continue;
      active++;
      if (m.getInt("stock") <= m.getInt("low")
          || !Rules.expiry(m.getString("expiry"))
              .atEndOfMonth()
              .isAfter(LocalDate.now().plusDays(60))) warning++;
    }
    LinearLayout hero = card(content);
    hero.setBackground(shape(TEAL, 22));
    hero.addView(text("İLACIM · EVDEKİ SAĞLIK DÜZENİN", 12, 0xffc6eeed, true));
    hero.addView(text(active + " ilaç  ·  " + warning + " uyarı", 25, Color.WHITE, true));
    hero.addView(
        text(
            "Dozlarını kaydet, stoklarını ve randevularını tek yerden takip et.",
            14,
            Color.WHITE,
            false));
    content.addView(button("+ İlaç ekle / barkod okut", true, () -> medicineForm(null, "")));
    content.addView(text("Günlük ilaç planın", 20, INK, true));
    ArrayList<JSONObject> slots = new ArrayList<>();
    JSONArray plans = store.list("plans");
    LocalDate date = LocalDate.now();
    for (int i = 0; i < plans.length(); i++) {
      JSONObject p = plans.getJSONObject(i), m = store.find("meds", p.getString("med"));
      if (m == null
          || !visible(m)
          || !p.optBoolean("active", true)
          || date.isBefore(LocalDate.parse(p.getString("start")))
          || date.isAfter(LocalDate.parse(p.getString("end")))) continue;
      for (String time : Rules.times(p.getString("times")))
        slots.add(new JSONObject().put("plan", p).put("med", m).put("time", time));
    }
    slots.sort(Comparator.comparing(o -> o.optString("time")));
    if (slots.isEmpty()) {
      LinearLayout empty = card(content);
      empty.addView(text("Bugün için plan yok", 18, INK, true));
      empty.addView(
          text(
              "İlacını ekledikten sonra doktorunun önerdiği miktar ve saatlerle bir plan oluştur.",
              14,
              MUTED,
              false));
      empty.addView(button("İlaç planı oluştur", false, () -> planForm(null)));
    }
    for (JSONObject slot : slots) {
      JSONObject p = slot.getJSONObject("plan"), m = slot.getJSONObject("med");
      String time = slot.getString("time"), id = Rules.slot(p.getString("id"), date, time);
      JSONObject log = store.find("logs", id);
      LinearLayout c = card(content);
      c.addView(text(time + "  ·  " + name(m), 18, INK, true));
      c.addView(
          text(
              amount(p.getInt("dose"))
                  + " "
                  + m.optString("unit", "adet")
                  + " · "
                  + p.optString("note", ""),
              14,
              MUTED,
              false));
      if (log == null) {
        c.addView(button("Aldım · stoğu güncelle", true, () -> record(p, m, time, false)));
        c.addView(button("Bu dozu atladım", false, () -> record(p, m, time, true)));
      } else {
        c.addView(
            text(
                log.optString("status").equals("taken")
                    ? "✓ Aldın · " + log.optString("recorded")
                    : "Atlandı",
                15,
                TEAL,
                true));
        c.addView(button("Kaydı geri al", false, () -> undo(log)));
      }
    }
    content.addView(button("Planları yönet", false, this::plansDialog));
    if (warning > 0) {
      content.addView(text("Dolabında dikkat isteyenler", 20, INK, true));
      for (int i = 0; i < meds.length(); i++) {
        JSONObject m = meds.getJSONObject(i);
        if (!visible(m)) continue;
        boolean low = m.getInt("stock") <= m.getInt("low"),
            exp = !Rules.expiry(m.getString("expiry")).atEndOfMonth().isAfter(date.plusDays(60));
        if (!low && !exp) continue;
        LinearLayout c = card(content);
        c.addView(text(name(m), 17, INK, true));
        c.addView(
            text(
                (exp ? expiryStatus(m) + " · " : "")
                    + "Stok: "
                    + amount(m.getInt("stock"))
                    + " "
                    + m.optString("unit"),
                14,
                RED,
                false));
      }
    }
    content.addView(button("İlaç kullanım geçmişi", false, this::history));
  }

  void record(JSONObject p, JSONObject m, String time, boolean skipped) {
    try {
      String id = Rules.slot(p.getString("id"), LocalDate.now(), time);
      if (store.find("logs", id) != null) {
        toast("Bu doz zaten kaydedildi.");
        return;
      }
      int dose = p.getInt("dose");
      if (!skipped && Rules.expired(m.getString("expiry"), LocalDate.now())) {
        message(
            "Son kullanma tarihi dolmuş",
            "Bu kutuyla kullanım kaydı oluşturulmadı. İlacı kullanmadan önce eczacınla görüş ve"
                + " uygun kutuyu planına bağla.");
        return;
      }
      if (!skipped && m.getInt("stock") < dose) {
        message(
            "Stok yetersiz",
            "Kayıtlı stok bu dozdan az. Önce kutunun miktarını kontrol edip stok bilgisini"
                + " düzelt.");
        return;
      }
      new AlertDialog.Builder(this)
          .setTitle(skipped ? "Doz atlandı olarak kaydedilsin mi?" : "Bu dozu aldığını onayla")
          .setMessage(
              skipped
                  ? "Stok değişmez. Uygulama telafi dozu önermez."
                  : name(m)
                      + " · "
                      + amount(dose)
                      + " "
                      + m.optString("unit")
                      + " stoktan düşülecek.")
          .setNegativeButton("Vazgeç", null)
          .setPositiveButton(
              "Kaydet",
              (d, w) -> {
                try {
                  if (store.find("logs", id) != null) return;
                  if (!skipped) m.put("stock", m.getInt("stock") - dose);
                  store
                      .list("logs")
                      .put(
                          new JSONObject()
                              .put("id", id)
                              .put("med", m.getString("id"))
                              .put("name", name(m))
                              .put("date", LocalDate.now().toString())
                              .put("time", time)
                              .put(
                                  "recorded",
                                  LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
                              .put("status", skipped ? "skipped" : "taken")
                              .put("amount", skipped ? 0 : dose));
                  save();
                } catch (Exception e) {
                  toast("Kayıt kaydedilemedi.");
                }
              })
          .show();
    } catch (Exception e) {
      toast("Kayıt oluşturulamadı.");
    }
  }

  void undo(JSONObject log) {
    new AlertDialog.Builder(this)
        .setTitle("Kullanım kaydını geri al")
        .setMessage("Düşülen miktar aynı kutunun stoğuna geri eklenecek.")
        .setNegativeButton("Vazgeç", null)
        .setPositiveButton(
            "Geri al",
            (d, w) -> {
              try {
                JSONObject m = store.find("meds", log.getString("med"));
                if (m != null)
                  m.put("stock", Math.addExact(m.getInt("stock"), log.getInt("amount")));
                store.remove("logs", log.getString("id"));
                save();
              } catch (Exception e) {
                toast("Kayıt geri alınamadı.");
              }
            })
        .show();
  }

  void history() {
    LinearLayout body = column();
    try {
      JSONArray logs = store.list("logs");
      if (logs.length() == 0) body.addView(text("Henüz kullanım kaydı yok.", 16, MUTED, false));
      for (int i = logs.length() - 1; i >= Math.max(0, logs.length() - 100); i--) {
        JSONObject l = logs.getJSONObject(i);
        LinearLayout c = card(body);
        c.addView(text(l.optString("name"), 17, INK, true));
        c.addView(
            text(
                l.optString("date")
                    + " · "
                    + l.optString("time")
                    + " · "
                    + (l.optString("status").equals("taken") ? "Alındı" : "Atlandı"),
                14,
                MUTED,
                false));
        c.addView(button("Geri al", false, () -> undo(l)));
      }
      body.addView(
          text("Son 100 kayıt gösterilir. JSON yedeği tüm geçmişi içerir.", 13, MUTED, false));
    } catch (Exception ignored) {
    }
    showBody("Kullanım geçmişi", body);
  }

  void medicines() throws Exception {
    heading("İlaç dolabım", "Her kutu, son kullanma tarihi ve stoğuyla ayrı takip edilir.");
    content.addView(button("+ Yeni ilaç ekle", true, () -> medicineForm(null, "")));
    EditText search = field(content, "İlaç, kategori veya barkod ara", query, 1);
    search.setSingleLine(true);
    search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
    search.setOnEditorActionListener(
        (v, a, event) -> {
          query = search.getText().toString().trim();
          render();
          return true;
        });
    content.addView(
        button(
            "Ara",
            false,
            () -> {
              query = search.getText().toString().trim();
              render();
            }));
    JSONArray meds = store.list("meds");
    int shown = 0;
    for (int i = 0; i < meds.length(); i++) {
      JSONObject m = meds.getJSONObject(i);
      if (!visible(m)
          || !(name(m) + m.optString("category") + m.optString("barcode"))
              .toLowerCase(new Locale("tr"))
              .contains(query.toLowerCase(new Locale("tr")))) continue;
      shown++;
      LinearLayout c = card(content);
      c.addView(text(name(m), 20, INK, true));
      c.addView(
          text(
              m.optString("category", "Diğer") + " · " + m.optString("form", ""),
              14,
              MUTED,
              false));
      c.addView(
          text(
              expiryStatus(m) + "  |  " + amount(m.optInt("stock")) + " " + m.optString("unit"),
              16,
              Rules.expired(m.optString("expiry"), LocalDate.now()) ? RED : TEAL,
              true));
      if (!m.optString("owner").isBlank())
        c.addView(text("Kullanıcı: " + m.optString("owner"), 14, MUTED, false));
      if (!m.optString("location").isBlank())
        c.addView(text("Konum: " + m.optString("location"), 14, MUTED, false));
      c.addView(button("Düzenle / stok ekle", false, () -> medicineForm(m, "")));
      c.addView(button("Günlük kullanım planı", false, () -> planForm(m)));
    }
    if (shown == 0) {
      LinearLayout c = card(content);
      c.addView(text("Dolabın hazır", 20, INK, true));
      c.addView(
          text(
              query.isEmpty()
                  ? "İlk ilacını barkodla veya elle ekleyerek başla."
                  : "Aramana uygun ilaç bulunamadı.",
              14,
              MUTED,
              false));
    }
    content.addView(button("Arşivlenmiş kutular", false, this::archiveDialog));
  }

  EditText field(LinearLayout parent, String label, String value, int type) {
    parent.addView(text(label, 13, MUTED, true));
    EditText e = new EditText(this);
    e.setText(value);
    e.setInputType(type);
    e.setTextSize(16);
    e.setTextColor(INK);
    e.setPadding(dp(12), dp(10), dp(12), dp(10));
    e.setBackground(shape(0xfff0f4f8, 10));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(0, 0, 0, dp(8));
    e.setLayoutParams(p);
    parent.addView(e);
    return e;
  }

  String value(EditText e) {
    return e.getText().toString().trim();
  }

  String amount(int n) {
    return java.math.BigDecimal.valueOf(n, 3)
        .stripTrailingZeros()
        .toPlainString()
        .replace('.', ',');
  }

  int number(EditText e) {
    try {
      return Rules.quantity(value(e));
    } catch (Exception x) {
      throw new IllegalArgumentException(
          "Miktarı 0–1.000.000 arası, en fazla üç ondalıkla gir (örn. 0,5).");
    }
  }

  void dateField(EditText e) {
    e.setFocusable(false);
    e.setOnClickListener(
        v -> {
          LocalDate initial;
          try {
            initial = LocalDate.parse(value(e));
          } catch (Exception ex) {
            initial = LocalDate.now();
          }
          new DatePickerDialog(
                  this,
                  (view, y, m, d) -> e.setText(LocalDate.of(y, m + 1, d).toString()),
                  initial.getYear(),
                  initial.getMonthValue() - 1,
                  initial.getDayOfMonth())
              .show();
        });
  }

  AlertDialog showBody(String title, LinearLayout body) {
    body.setPadding(dp(18), dp(8), dp(18), dp(18));
    ScrollView sc = new ScrollView(this);
    sc.addView(body);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setView(sc)
            .setNegativeButton("Kapat", null)
            .create();
    dialog.show();
    return dialog;
  }

  void medicineForm(JSONObject existing, String scanned) {
    LinearLayout b = column();
    boolean edit = existing != null;
    JSONObject old = edit ? existing : new JSONObject();
    EditText barcode =
        field(
            b,
            "Barkod / GTIN (isteğe bağlı)",
            scanned.isEmpty() ? old.optString("barcode") : scanned,
            android.text.InputType.TYPE_CLASS_NUMBER);
    barcodeInput = barcode;
    b.addView(
        button(
            "Kamerayla barkod / karekod okut",
            false,
            () -> {
              barcodeInput = barcode;
              if (checkSelfPermission(Manifest.permission.CAMERA)
                  == PackageManager.PERMISSION_GRANTED)
                startActivityForResult(new Intent(this, ScannerActivity.class), 20);
              else requestPermissions(new String[] {Manifest.permission.CAMERA}, 21);
            }));
    LinearLayout details = column();
    details.setVisibility(View.GONE);
    EditText title = field(b, "İlaç adı *", old.optString("name"), 1),
        strength = field(details, "Doz / güç (örn. 500 mg)", old.optString("strength"), 1),
        category =
            field(details, "Kategori (API doldurabilir)", old.optString("category", "Diğer"), 1),
        form = field(details, "İlaç formu (tablet, şurup…)", old.optString("form", "Tablet"), 1);
    b.addView(
        button(
            "Barkoddan API bilgilerini getir",
            false,
            () -> lookup(barcode, title, strength, category, form)));
    b.addView(
        text(
            "API yalnızca ürün bilgisi içindir. Doz ve saatler otomatik önerilmez.",
            13,
            MUTED,
            false));
    EditText expiry =
        field(
            b,
            "Son kullanma tarihi * · AA/YYYY",
            old.optString("expiry"),
            android.text.InputType.TYPE_CLASS_DATETIME);
    expiry.setHint("12/2026");
    expiry.setOnClickListener(
        v -> {
          Calendar cal = Calendar.getInstance();
          try {
            YearMonth ym = Rules.expiry(value(expiry));
            cal.set(Calendar.YEAR, ym.getYear());
            cal.set(Calendar.MONTH, ym.getMonthValue() - 1);
          } catch (Exception ignored) {
          }
          new DatePickerDialog(
                  this,
                  (view, y, m, d) ->
                      expiry.setText(String.format(Locale.ROOT, "%02d/%04d", m + 1, y)),
                  cal.get(Calendar.YEAR),
                  cal.get(Calendar.MONTH),
                  1)
              .show();
        });
    EditText stock = field(b, "Mevcut miktar *", amount(old.optInt("stock", 0)), 8194),
        unit = field(b, "Stok birimi (adet, ml, damla…)", old.optString("unit", "adet"), 1),
        low = field(details, "Az stok uyarısı eşiği", amount(old.optInt("low", 5000)), 8194),
        owner = field(details, "Kimin ilacı? (isteğe bağlı)", old.optString("owner"), 1),
        location = field(details, "Saklama yeri", old.optString("location"), 1),
        note =
            field(
                details,
                "Not / doktor talimatı",
                old.optString("note"),
                android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    b.addView(
        button(
            "Diğer bilgiler · kategori, doz, konum, not",
            false,
            () ->
                details.setVisibility(
                    details.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE)));
    b.addView(details);
    b.addView(
        text(
            "AA/YYYY tarihleri o ayın son günüyle değerlendirilir. Saklama koşullarını ürünün"
                + " talimatına göre uygula. Aynı ilacın yeni kutusunu ayrı ekle.",
            13,
            MUTED,
            false));
    AlertDialog dialog = showBody(edit ? "Kutuyu düzenle" : "İlaç ekle", b);
    b.addView(
        button(
            "Kutuyu kaydet",
            true,
            () -> {
              try {
                if (value(title).isBlank()) throw new IllegalArgumentException("İlaç adını gir.");
                Rules.expiry(value(expiry));
                int qty = number(stock), threshold = number(low);
                String code = value(barcode).isBlank() ? "" : Rules.barcode(value(barcode));
                if (!code.isBlank() && !Rules.validGtin(code))
                  throw new IllegalArgumentException(
                      "Barkodun kontrol basamağı geçersiz. Kodu kontrol et veya barkod alanını boş"
                          + " bırak.");
                if (value(unit).isBlank()) throw new IllegalArgumentException("Stok birimini gir.");
                JSONObject m =
                    new JSONObject()
                        .put("id", old.optString("id", Store.id()))
                        .put("name", value(title))
                        .put("barcode", code)
                        .put("strength", value(strength))
                        .put("category", value(category).isEmpty() ? "Diğer" : value(category))
                        .put("form", value(form))
                        .put("expiry", value(expiry))
                        .put("stock", qty)
                        .put("unit", value(unit))
                        .put("low", threshold)
                        .put("owner", value(owner))
                        .put("location", value(location))
                        .put("note", value(note))
                        .put("archived", false);
                if (edit) store.remove("meds", old.getString("id"));
                store.list("meds").put(m);
                save();
                dialog.dismiss();
                toast("İlaç kaydedildi.");
              } catch (Exception e) {
                message(
                    "Bilgileri kontrol et",
                    e instanceof IllegalArgumentException
                        ? e.getMessage()
                        : "Kayıt kaydedilemedi.");
              }
            }));
    if (edit)
      b.addView(
          button(
              "Kutuyu arşivle",
              false,
              () ->
                  new AlertDialog.Builder(this)
                      .setTitle("Kutuyu arşivle")
                      .setMessage("Bu kutunun planları durdurulur. Kullanım geçmişi korunur.")
                      .setNegativeButton("Vazgeç", null)
                      .setPositiveButton(
                          "Arşivle",
                          (d, w) -> {
                            try {
                              existing.put("archived", true);
                              JSONArray plans = store.list("plans");
                              for (int i = 0; i < plans.length(); i++) {
                                JSONObject p = plans.getJSONObject(i);
                                if (p.optString("med").equals(existing.optString("id")))
                                  p.put("active", false);
                              }
                              save();
                              dialog.dismiss();
                            } catch (Exception e) {
                              toast("Arşivlenemedi.");
                            }
                          })
                      .show()));
  }

  void archiveDialog() {
    LinearLayout b = column();
    JSONArray meds = store.list("meds");
    int count = 0;
    for (int i = 0; i < meds.length(); i++) {
      JSONObject m = meds.optJSONObject(i);
      if (visible(m)) continue;
      count++;
      LinearLayout c = card(b);
      c.addView(text(name(m), 17, INK, true));
      c.addView(
          button(
              "Dolaba geri getir",
              false,
              () -> {
                try {
                  m.put("archived", false);
                  save();
                  toast("Kutu geri getirildi. Planı ayrıca etkinleştir.");
                } catch (Exception ignored) {
                }
              }));
    }
    if (count == 0) b.addView(text("Arşivlenmiş kutu yok.", 16, MUTED, false));
    showBody("Arşiv", b);
  }

  void planForm(JSONObject chosen) {
    ArrayList<JSONObject> meds = new ArrayList<>();
    JSONArray all = store.list("meds");
    for (int i = 0; i < all.length(); i++)
      if (visible(all.optJSONObject(i))) meds.add(all.optJSONObject(i));
    if (meds.isEmpty()) {
      message("Önce ilaç ekle", "Plan oluşturmak için dolabına en az bir ilaç ekle.");
      return;
    }
    LinearLayout b = column();
    b.addView(
        text(
            "Doktorunun reçetesindeki miktar ve saatleri gir. Uygulama tedavi veya telafi dozu"
                + " belirlemez.",
            14,
            MUTED,
            false));
    Spinner select = new Spinner(this);
    String[] labels = new String[meds.size()];
    int selected = 0;
    for (int i = 0; i < meds.size(); i++) {
      JSONObject m = meds.get(i);
      labels[i] = name(m) + " · " + m.optString("expiry") + " · " + m.optString("unit");
      if (chosen != null && m.optString("id").equals(chosen.optString("id"))) selected = i;
    }
    select.setAdapter(
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
    select.setSelection(selected);
    b.addView(select);
    EditText dose = field(b, "Her seferinde miktar · kutunun stok birimiyle", "1", 8194),
        times = field(b, "Günlük saatler · virgülle ayır", "08:00, 20:00", 1),
        start = field(b, "Başlangıç tarihi", LocalDate.now().toString(), 1),
        end = field(b, "Bitiş tarihi", LocalDate.now().plusDays(30).toString(), 1),
        note = field(b, "Talimat / aç-tok bilgisi", "", 1);
    dateField(start);
    dateField(end);
    AlertDialog dialog = showBody("Günlük ilaç planı", b);
    b.addView(
        button(
            "Planı oluştur",
            true,
            () -> {
              try {
                JSONObject m = meds.get(select.getSelectedItemPosition());
                int amount = number(dose);
                if (amount < 1)
                  throw new IllegalArgumentException("Doz miktarı sıfırdan büyük olmalı.");
                List<String> ts = Rules.times(value(times));
                LocalDate from = LocalDate.parse(value(start)), to = LocalDate.parse(value(end));
                if (to.isBefore(from))
                  throw new IllegalArgumentException("Bitiş tarihi başlangıçtan önce olamaz.");
                JSONArray plans = store.list("plans");
                for (int i = 0; i < plans.length(); i++) {
                  JSONObject p = plans.getJSONObject(i);
                  if (p.optBoolean("active", true)
                      && p.optString("med").equals(m.optString("id"))
                      && !LocalDate.parse(p.optString("end")).isBefore(from)
                      && !LocalDate.parse(p.optString("start")).isAfter(to)) {
                    List<String> existing = Rules.times(p.optString("times"));
                    for (String time : ts)
                      if (existing.contains(time))
                        throw new IllegalArgumentException(
                            "Bu kutunun aynı saatte etkin bir planı var. Önce eski planı durdur.");
                  }
                }
                store
                    .list("plans")
                    .put(
                        new JSONObject()
                            .put("id", Store.id())
                            .put("med", m.getString("id"))
                            .put("dose", amount)
                            .put("times", String.join(", ", ts))
                            .put("start", from.toString())
                            .put("end", to.toString())
                            .put("note", value(note))
                            .put("active", true));
                save();
                dialog.dismiss();
                toast("Plan oluşturuldu.");
              } catch (Exception e) {
                message(
                    "Planı kontrol et",
                    e instanceof IllegalArgumentException ? e.getMessage() : "Plan kaydedilemedi.");
              }
            }));
  }

  void plansDialog() {
    LinearLayout b = column();
    JSONArray plans = store.list("plans");
    if (plans.length() == 0) b.addView(text("Henüz plan oluşturmadın.", 16, MUTED, false));
    for (int i = 0; i < plans.length(); i++) {
      JSONObject p = plans.optJSONObject(i), m = store.find("meds", p.optString("med"));
      if (m == null) continue;
      LinearLayout c = card(b);
      c.addView(text(name(m), 18, INK, true));
      c.addView(
          text(
              p.optString("times")
                  + " · "
                  + amount(p.optInt("dose"))
                  + " "
                  + m.optString("unit")
                  + "\n"
                  + p.optString("start")
                  + " → "
                  + p.optString("end"),
              14,
              MUTED,
              false));
      c.addView(
          button(
              p.optBoolean("active", true) ? "Planı durdur" : "Planı yeniden etkinleştir",
              false,
              () -> {
                try {
                  if (!p.optBoolean("active", true) && !visible(m)) {
                    toast("Önce kutuyu arşivden geri getir.");
                    return;
                  }
                  if (!p.optBoolean("active", true)) {
                    JSONArray other = store.list("plans");
                    for (int j = 0; j < other.length(); j++) {
                      JSONObject q = other.optJSONObject(j);
                      if (q == p
                          || !q.optBoolean("active", true)
                          || !q.optString("med").equals(p.optString("med"))) continue;
                      if (!LocalDate.parse(q.optString("end"))
                              .isBefore(LocalDate.parse(p.optString("start")))
                          && !LocalDate.parse(q.optString("start"))
                              .isAfter(LocalDate.parse(p.optString("end")))) {
                        for (String t : Rules.times(p.optString("times")))
                          if (Rules.times(q.optString("times")).contains(t)) {
                            toast("Aynı saat için başka bir etkin plan var.");
                            return;
                          }
                      }
                    }
                  }
                  p.put("active", !p.optBoolean("active", true));
                  save();
                  toast("Plan güncellendi.");
                } catch (Exception e) {
                  toast("Plan güncellenemedi.");
                }
              }));
    }
    b.addView(button("+ Yeni plan", true, () -> planForm(null)));
    showBody("İlaç planların", b);
  }

  void appointments() throws Exception {
    heading("Randevularım", "Doktor ziyaretlerini ve hazırlık notlarını bir arada tut.");
    content.addView(button("+ Doktor randevusu ekle", true, () -> appointmentForm(null)));
    ArrayList<JSONObject> all = new ArrayList<>();
    JSONArray a = store.list("appointments");
    for (int i = 0; i < a.length(); i++) all.add(a.getJSONObject(i));
    all.sort(Comparator.comparing(o -> o.optString("when")));
    if (all.isEmpty()) {
      LinearLayout c = card(content);
      c.addView(text("Takvimin sakin", 20, INK, true));
      c.addView(
          text("Yeni randevunu ekle; yaklaşık bir saat önce hatırlatalım.", 14, MUTED, false));
    }
    for (JSONObject r : all) {
      LinearLayout c = card(content);
      LocalDateTime when = LocalDateTime.parse(r.getString("when"));
      c.addView(
          text(
              when.format(DateTimeFormatter.ofPattern("dd MMM · HH:mm", new Locale("tr"))),
              18,
              TEAL,
              true));
      c.addView(text(r.getString("doctor"), 20, INK, true));
      c.addView(text(r.optString("branch") + " · " + r.optString("place"), 14, MUTED, false));
      if (!r.optString("note").isBlank()) c.addView(text(r.optString("note"), 14, MUTED, false));
      c.addView(
          text(
              r.optBoolean("done")
                  ? "✓ Tamamlandı"
                  : when.isBefore(LocalDateTime.now()) ? "Geçmiş randevu" : "Yaklaşan randevu",
              14,
              MUTED,
              true));
      c.addView(button("Düzenle", false, () -> appointmentForm(r)));
      c.addView(
          button(
              r.optBoolean("done") ? "Tamamlandı işaretini kaldır" : "Tamamlandı olarak işaretle",
              false,
              () -> {
                try {
                  r.put("done", !r.optBoolean("done"));
                  save();
                } catch (Exception ignored) {
                }
              }));
    }
  }

  void appointmentForm(JSONObject existing) {
    boolean edit = existing != null;
    JSONObject old = edit ? existing : new JSONObject();
    LinearLayout b = column();
    EditText doctor = field(b, "Doktor adı *", old.optString("doctor"), 1),
        branch = field(b, "Branş", old.optString("branch"), 1),
        place = field(b, "Hastane / klinik", old.optString("place"), 1);
    LocalDateTime initial = LocalDateTime.now().plusDays(1);
    try {
      initial = LocalDateTime.parse(old.optString("when"));
    } catch (Exception ignored) {
    }
    EditText date = field(b, "Randevu tarihi", initial.toLocalDate().toString(), 1),
        time =
            field(b, "Saat", initial.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")), 1),
        note =
            field(
                b,
                "Hazırlık / sorular / sonuç notu",
                old.optString("note"),
                android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    dateField(date);
    time.setFocusable(false);
    time.setOnClickListener(
        v -> {
          LocalTime t = LocalTime.parse(value(time));
          new TimePickerDialog(
                  this,
                  (view, h, m) -> time.setText(String.format(Locale.ROOT, "%02d:%02d", h, m)),
                  t.getHour(),
                  t.getMinute(),
                  true)
              .show();
        });
    AlertDialog dialog = showBody(edit ? "Randevuyu düzenle" : "Yeni randevu", b);
    b.addView(
        button(
            "Randevuyu kaydet",
            true,
            () -> {
              try {
                if (value(doctor).isBlank())
                  throw new IllegalArgumentException("Doktor adını gir.");
                LocalDateTime at =
                    LocalDate.parse(value(date)).atTime(LocalTime.parse(value(time)));
                JSONObject r =
                    new JSONObject()
                        .put("id", old.optString("id", Store.id()))
                        .put("doctor", value(doctor))
                        .put("branch", value(branch))
                        .put("place", value(place))
                        .put("when", at.toString())
                        .put("note", value(note))
                        .put("done", old.optBoolean("done"));
                if (edit) store.remove("appointments", old.getString("id"));
                store.list("appointments").put(r);
                save();
                dialog.dismiss();
              } catch (Exception e) {
                message(
                    "Randevuyu kontrol et",
                    e instanceof IllegalArgumentException
                        ? e.getMessage()
                        : "Randevu kaydedilemedi.");
              }
            }));
    if (edit)
      b.addView(
          button(
              "Randevuyu sil",
              false,
              () ->
                  new AlertDialog.Builder(this)
                      .setTitle("Randevu silinsin mi?")
                      .setNegativeButton("Vazgeç", null)
                      .setPositiveButton(
                          "Sil",
                          (d, w) -> {
                            store.remove("appointments", old.optString("id"));
                            save();
                            dialog.dismiss();
                          })
                      .show()));
  }

  void settings() {
    heading("Senin düzenin", "Verilerin cihazında; kontrol sende.");
    LinearLayout reminder = card(content);
    reminder.addView(text("Hatırlatmalar", 20, INK, true));
    boolean enabled = getSharedPreferences("reminders", 0).getBoolean("enabled", true);
    boolean permitted =
        Build.VERSION.SDK_INT < 33
            || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    reminder.addView(
        text(
            (enabled ? "Planlama açık" : "Planlama kapalı")
                + " · "
                + (permitted ? "Bildirim izni açık" : "Bildirim izni gerekli"),
            14,
            MUTED,
            false));
    reminder.addView(
        button(
            enabled ? "Hatırlatmaları kapat" : "Hatırlatmaları aç",
            false,
            () -> {
              getSharedPreferences("reminders", 0).edit().putBoolean("enabled", !enabled).apply();
              Scheduler.refresh(this);
              render();
            }));
    reminder.addView(
        button(
            "Bildirim izni / sistem ayarları",
            false,
            () -> {
              if (Build.VERSION.SDK_INT >= 33 && !permitted)
                requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 12);
              else
                startActivity(
                    new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName()));
            }));
    reminder.addView(
        button(
            "Deneme bildirimi gönder",
            false,
            () -> {
              if (!permitted) {
                toast("Önce bildirim izni ver.");
                return;
              }
              Scheduler.notify(
                  this,
                  100000,
                  "İlacım hazır",
                  "Bu bir deneme bildirimi; kullanım kaydı oluşturmaz.");
              toast("Deneme bildirimi gönderildi.");
            }));
    reminder.addView(
        text(
            "Android'in pil tasarrufu ve boşta çalışma kuralları bildirimleri geciktirebilir. Zorla"
                + " durdurma sonrası uygulamayı aç. Kritik dozlar için tek hatırlatma yöntemin bu"
                + " uygulama olmasın.",
            13,
            MUTED,
            false));
    LinearLayout api = card(content);
    api.addView(text("Barkod bilgi servisi", 20, INK, true));
    api.addView(
        text(
            "Manuel ekleme her zaman çalışır. Bir API sağlayıcısı seçtiğinde HTTPS bağlantısını ve"
                + " yanıt alanlarını tanımla.",
            14,
            MUTED,
            false));
    api.addView(button("API bağlantısını yapılandır", false, this::apiForm));
    LinearLayout backup = card(content);
    backup.addView(text("Yedek ve geri yükleme", 20, INK, true));
    backup.addView(
        text(
            "JSON yedeği ilaçları, planları, randevuları ve geçmişini içerir. API anahtarını"
                + " içermez. Dosya sağlık bilgisi içerir; güvenli bir yerde sakla.",
            14,
            MUTED,
            false));
    backup.addView(
        button(
            "JSON yedeği dışa aktar",
            false,
            () ->
                startActivityForResult(
                    new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .setType("application/json")
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_TITLE, "Ilacim-" + LocalDate.now() + ".json"),
                    30)));
    backup.addView(
        button(
            "JSON yedeğini geri yükle",
            false,
            () ->
                startActivityForResult(
                    new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .setType("application/json")
                        .addCategory(Intent.CATEGORY_OPENABLE),
                    31)));
    LinearLayout about = card(content);
    about.addView(text("İlacım 1.0", 18, INK, true));
    about.addView(
        text(
            "Android 8 ve üzeri · Yerel SQLite\n"
                + "İlaç ve randevu kayıtları otomatik olarak bir sunucuya gönderilmez. Barkod"
                + " sorgusu yaparsan yalnızca barkod seçtiğin servise iletilir. Uygulama ilaç"
                + " etkileşimi, tanı veya doz önerisi yapmaz.",
            14,
            MUTED,
            false));
  }

  void apiForm() {
    SharedPreferences pref = getSharedPreferences("api", 0);
    LinearLayout b = column();
    b.addView(
        text(
            "Sağlayıcının belgelerine göre yapılandır. GET isteği kullanılır; adres {barcode}"
                + " içermelidir. API anahtarını yalnızca güvenilir sağlayıcının alan adı için gir.",
            14,
            MUTED,
            false));
    EditText url =
        field(
            b,
            "HTTPS URL şablonu",
            pref.getString("url", ""),
            android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_URI);
    url.setHint("https://servis.example/medicines/{barcode}");
    EditText
        header = field(b, "Anahtar HTTP başlığı", pref.getString("header", "Authorization"), 1),
        prefix = field(b, "Anahtar öneki (Bearer veya boş)", pref.getString("prefix", "Bearer"), 1),
        key =
            field(
                b,
                "API anahtarı · boş bırakılırsa korunur",
                "",
                android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD),
        namePath = field(b, "İlaç adı JSON yolu", pref.getString("namePath", "name"), 1),
        categoryPath =
            field(b, "Kategori JSON yolu", pref.getString("categoryPath", "category"), 1),
        strengthPath = field(b, "Güç JSON yolu", pref.getString("strengthPath", "strength"), 1),
        formPath = field(b, "Form JSON yolu", pref.getString("formPath", "form"), 1);
    b.addView(
        text(
            "Örnek: data.product.name. Nesne içindeki metin alanları desteklenir; dizi yanıtı veya"
                + " farklı kimlik doğrulama kullanan servisler ayrı adaptör gerektirir.",
            13,
            MUTED,
            false));
    AlertDialog dialog = showBody("API ayarları", b);
    b.addView(
        button(
            "Bağlantı ayarlarını kaydet",
            true,
            () -> {
              try {
                URI u = URI.create(value(url).replace("{barcode}", "1234567890128"));
                if (!"https".equalsIgnoreCase(u.getScheme())
                    || u.getHost() == null
                    || u.getUserInfo() != null
                    || !value(url).contains("{barcode}")
                    || u.getFragment() != null)
                  throw new IllegalArgumentException(
                      "Geçerli HTTPS adresi ve {barcode} yer tutucusu gerekli.");
                if (!value(header).matches("[A-Za-z0-9-]+")
                    || value(prefix).contains("\n")
                    || value(prefix).contains("\r")
                    || value(key).contains("\n")
                    || value(key).contains("\r"))
                  throw new IllegalArgumentException("HTTP başlıklarını kontrol et.");
                if (value(namePath).isBlank())
                  throw new IllegalArgumentException("İlaç adı JSON yolunu gir.");
                if (!value(key).isEmpty()) Vault.save(this, value(key));
                if (!pref.edit()
                    .putString("url", value(url))
                    .putString("header", value(header))
                    .putString("prefix", value(prefix))
                    .putString("namePath", value(namePath))
                    .putString("categoryPath", value(categoryPath))
                    .putString("strengthPath", value(strengthPath))
                    .putString("formPath", value(formPath))
                    .commit()) throw new IllegalStateException("Ayarlar kaydedilemedi");
                dialog.dismiss();
                toast("Ayarlar kaydedildi. Gerçek bağlantıyı ilaç eklerken sınayabilirsin.");
              } catch (Exception e) {
                message(
                    "Ayarları kontrol et",
                    e instanceof IllegalArgumentException
                        ? e.getMessage()
                        : "Anahtar güvenli olarak kaydedilemedi.");
              }
            }));
    b.addView(
        button(
            "API anahtarını ve bağlantıyı sil",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("API bağlantısı silinsin mi?")
                    .setNegativeButton("Vazgeç", null)
                    .setPositiveButton(
                        "Sil",
                        (d, w) -> {
                          pref.edit().clear().apply();
                          dialog.dismiss();
                          toast("API bağlantısı silindi.");
                        })
                    .show()));
  }

  static String jsonPath(JSONObject object, String path) {
    if (path.isBlank()) return "";
    Object value = object;
    for (String part : path.split("\\.")) {
      if (!(value instanceof JSONObject)) return "";
      value = ((JSONObject) value).opt(part);
    }
    return value instanceof String ? (String) value : "";
  }

  void lookup(
      EditText barcode, EditText name, EditText strength, EditText category, EditText form) {
    SharedPreferences p = getSharedPreferences("api", 0);
    String template = p.getString("url", "");
    if (template.isEmpty()) {
      message(
          "API henüz bağlı değil",
          "Ayarlar → API bağlantısını yapılandır bölümünden hizmetini ekle. İlacı şimdi elle"
              + " kaydedebilirsin.");
      return;
    }
    final String code;
    try {
      code = Rules.barcode(value(barcode));
      if (!Rules.validGtin(code))
        throw new IllegalArgumentException("Barkodun kontrol basamağını kontrol et.");
    } catch (Exception e) {
      message("Barkodu kontrol et", e.getMessage());
      return;
    }
    new AlertDialog.Builder(this)
        .setTitle("Ürün bilgilerini sorgula")
        .setMessage(
            "Barkod seçtiğin API hizmetine gönderilecek. Dönen bilgiler mevcut ad, kategori, güç ve"
                + " form alanlarını güncelleyebilir; kaydetmeden önce kontrol et.")
        .setNegativeButton("Vazgeç", null)
        .setPositiveButton(
            "Sorgula",
            (dialog, w) -> {
              ProgressDialog progress = new ProgressDialog(this);
              progress.setMessage("İlaç bilgileri sorgulanıyor…");
              progress.setCancelable(false);
              progress.show();
              network.execute(
                  () -> {
                    String error = null;
                    JSONObject result = null;
                    javax.net.ssl.HttpsURLConnection conn = null;
                    try {
                      URL address =
                          new URL(template.replace("{barcode}", URLEncoder.encode(code, "UTF-8")));
                      if (!"https".equalsIgnoreCase(address.getProtocol())) throw new IOException();
                      conn = (javax.net.ssl.HttpsURLConnection) address.openConnection();
                      conn.setInstanceFollowRedirects(false);
                      conn.setConnectTimeout(12000);
                      conn.setReadTimeout(12000);
                      conn.setRequestProperty("Accept", "application/json");
                      String token = Vault.read(this);
                      if (!token.isEmpty()) {
                        String prefix = p.getString("prefix", "");
                        conn.setRequestProperty(
                            p.getString("header", "Authorization"),
                            (prefix.isEmpty() ? "" : prefix + " ") + token);
                      }
                      int status = conn.getResponseCode();
                      if (status == 401 || status == 403)
                        throw new IllegalArgumentException(
                            "API erişimi reddedildi. Anahtar ve hizmet izinlerini kontrol et.");
                      if (status == 404)
                        throw new IllegalArgumentException(
                            "Bu barkod için ilaç bulunamadı. Elle ekleyebilirsin.");
                      if (status >= 300 && status < 400)
                        throw new IllegalArgumentException(
                            "Servis yönlendirme döndürdü. Anahtarı korumak için yönlendirme"
                                + " izlenmedi; doğrudan HTTPS adresini gir.");
                      if (status != 200)
                        throw new IllegalArgumentException(
                            "API yanıtı: HTTP " + status + ". Elle ekleme kullanılabilir.");
                      try (InputStream in = conn.getInputStream()) {
                        result =
                            new JSONObject(
                                new String(
                                    readLimited(in, 1024 * 1024),
                                    java.nio.charset.StandardCharsets.UTF_8));
                      }
                      if (jsonPath(result, p.getString("namePath", "name")).isBlank())
                        throw new IllegalArgumentException(
                            "İlaç adı bulunamadı. JSON alan yollarını sağlayıcının yanıtına göre"
                                + " ayarla.");
                    } catch (Exception e) {
                      error =
                          e instanceof IllegalArgumentException
                              ? e.getMessage()
                              : "Bağlantı kurulamadı veya yanıt desteklenmiyor. URL, ağ ve API"
                                    + " ayarlarını kontrol et.";
                    } finally {
                      if (conn != null) conn.disconnect();
                    }
                    final String message = error;
                    final JSONObject data = result;
                    runOnUiThread(
                        () -> {
                          if (isFinishing() || isDestroyed()) return;
                          progress.dismiss();
                          if (message != null) {
                            message("Sorgu tamamlanamadı", message);
                            return;
                          }
                          name.setText(jsonPath(data, p.getString("namePath", "name")));
                          ((View) category.getParent()).setVisibility(View.VISIBLE);
                          String c = jsonPath(data, p.getString("categoryPath", "category")),
                              s = jsonPath(data, p.getString("strengthPath", "strength")),
                              f = jsonPath(data, p.getString("formPath", "form"));
                          if (!c.isBlank()) category.setText(c);
                          if (!s.isBlank()) strength.setText(s);
                          if (!f.isBlank()) form.setText(f);
                          toast(
                              c.isBlank()
                                  ? "Ürün bulundu; API kategori döndürmedi. Kategoriyi elle seç."
                                  : "Bilgiler geldi. Kutuyla eşleştiğini kontrol edip kaydet.");
                        });
                  });
            })
        .show();
  }

  static byte[] readLimited(InputStream in, int max) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    int n;
    while ((n = in.read(buf)) != -1) {
      if (out.size() + n > max) throw new IOException("Dosya çok büyük");
      out.write(buf, 0, n);
    }
    return out.toByteArray();
  }

  public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
    super.onRequestPermissionsResult(code, permissions, grants);
    if (code == 21) {
      if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED)
        startActivityForResult(new Intent(this, ScannerActivity.class), 20);
      else toast("Kamera izni verilmedi. Barkodu elle girebilirsin.");
    }
    if (code == 12) {
      Scheduler.refresh(this);
      render();
    }
  }

  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK || data == null) return;
    if (request == 20) {
      if (barcodeInput != null) barcodeInput.setText(data.getStringExtra("barcode"));
      return;
    }
    if (data.getData() == null) return;
    if (request == 30) {
      try (OutputStream out = getContentResolver().openOutputStream(data.getData(), "wt")) {
        if (out == null) throw new IOException();
        out.write(store.data.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        toast("Yedek dışa aktarıldı.");
      } catch (Exception e) {
        message("Yedek kaydedilemedi", "Dosya konumunu ve depolama erişimini kontrol et.");
      }
    }
    if (request == 31) {
      try (InputStream in = getContentResolver().openInputStream(data.getData())) {
        if (in == null) throw new IOException();
        JSONObject imported =
            new JSONObject(
                new String(
                    readLimited(in, 5 * 1024 * 1024), java.nio.charset.StandardCharsets.UTF_8));
        Store.validate(imported);
        new AlertDialog.Builder(this)
            .setTitle("Yedek geri yüklensin mi?")
            .setMessage(
                "Mevcut ilaçlar, planlar, randevular ve kullanım geçmişi bu yedekle değiştirilecek."
                    + " Önce mevcut verilerini dışa aktarman önerilir. API anahtarın değişmez.")
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton(
                "Geri yükle",
                (d, w) -> {
                  JSONObject old = store.data;
                  try {
                    store.data = imported;
                    store.save();
                    Scheduler.refresh(this);
                    render();
                    toast("Yedek geri yüklendi.");
                  } catch (Exception e) {
                    store.data = old;
                    message("Geri yüklenemedi", "Mevcut kayıtlar korundu.");
                  }
                })
            .show();
      } catch (Exception e) {
        message(
            "Yedek açılamadı",
            "Dosya geçersiz, desteklenmeyen sürümde veya çok büyük. Mevcut kayıtlar değişmedi.");
      }
    }
  }
}
