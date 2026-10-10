package com.semih.cepbutcem;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.*;

public class MainActivity extends Activity {
  private int BG = 0xFF101820,
      CARD = 0xFF192530,
      LINE = 0xFF2B3A47,
      WHITE = 0xFFF0F5F7,
      MUTED = 0xFF96A6B4,
      GREEN = 0xFF35D6AE,
      RED = 0xFFFF7B8B,
      BLUE = 0xFF739FFF;
  private static final int EXPORT_JSON = 41, EXPORT_CSV = 42, IMPORT_JSON = 43;
  private static final DateTimeFormatter
      MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Finance.TR),
      SHORT = DateTimeFormatter.ofPattern("d MMM", Finance.TR),
      DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", Finance.TR);
  private Ledger ledger;
  private Planning plan = new Planning();
  private List<Finance.Entry> entries;
  private List<Finance.Account> accounts;
  private List<Finance.Category> categories;
  private LinearLayout root, body, navigation;
  private YearMonth month = YearMonth.now();
  private int page = 0, transactionType = 0, analysisMode = 0;
  private long filterAccount = 0, filterCategory = 0;
  private String search = "";
  private boolean categoryIncome = false;
  private LocalDate customFrom = LocalDate.now().withDayOfMonth(1), customTo = LocalDate.now();
  private final ExecutorService io = Executors.newSingleThreadExecutor();
  private static volatile boolean busy = false;
  private static java.lang.ref.WeakReference<MainActivity> foreground =
      new java.lang.ref.WeakReference<>(null);

  private void fileFinished(String success, Exception failure) {
    busy = false;
    new android.os.Handler(android.os.Looper.getMainLooper())
        .post(
            () -> {
              MainActivity current = foreground.get();
              if (current == null || current.isDestroyed()) return;
              current.refresh();
              if (failure != null) current.error("Dosya işlemi tamamlanamadı", failure);
              else current.toast(success);
            });
  }

  @Override
  protected void onPause() {
    if (foreground.get() == this) foreground = new java.lang.ref.WeakReference<>(null);
    super.onPause();
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    ledger = new Ledger(this);
    try {
      plan = ledger.planning();
    } catch (Exception e) {
      error("Ayarlar okunamadı", e);
    }
    month = Finance.cycleMonth(LocalDate.now(), plan.payday);
    if (state != null) {
      month = YearMonth.parse(state.getString("month", month.toString()));
      page = state.getInt("page");
      search = state.getString("search", "");
      transactionType = state.getInt("transactionType");
      analysisMode = state.getInt("analysisMode");
      filterAccount = state.getLong("filterAccount");
      filterCategory = state.getLong("filterCategory");
      categoryIncome = state.getBoolean("categoryIncome");
      customFrom = LocalDate.parse(state.getString("customFrom", customFrom.toString()));
      customTo = LocalDate.parse(state.getString("customTo", customTo.toString()));
    }
    refresh();
  }

  @Override
  protected void onResume() {
    super.onResume();
    foreground = new java.lang.ref.WeakReference<>(this);
    if (ledger != null && !busy) refresh();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    state.putString("month", month.toString());
    state.putInt("page", page);
    state.putString("search", search);
    state.putInt("transactionType", transactionType);
    state.putInt("analysisMode", analysisMode);
    state.putLong("filterAccount", filterAccount);
    state.putLong("filterCategory", filterCategory);
    state.putBoolean("categoryIncome", categoryIncome);
    state.putString("customFrom", customFrom.toString());
    state.putString("customTo", customTo.toString());
  }

  private void refresh() {
    try {
      ledger.processRecurring(LocalDate.now());
      plan = ledger.planning();
      applyPalette();
      entries = ledger.entries();
      accounts = ledger.accounts();
      categories = ledger.categories(null);
      render();
    } catch (Exception e) {
      error("Veriler açılamadı", e);
    }
  }

  private int dp(float n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  private GradientDrawable shape(int color, float radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    return d;
  }

  private TextView text(String value, int size, int color, boolean bold) {
    TextView t = new TextView(this);
    t.setText(value);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setFontFeatureSettings("tnum");
    if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    t.setIncludeFontPadding(false);
    return t;
  }

  private LinearLayout column() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  private LinearLayout row() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.HORIZONTAL);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }

  private void space(LinearLayout l, int height) {
    View v = new View(this);
    l.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
  }

  private void gap(LinearLayout l, int width) {
    View v = new View(this);
    l.addView(v, new LinearLayout.LayoutParams(dp(width), 1));
  }

  private void add(LinearLayout l, View v) {
    l.addView(v, new LinearLayout.LayoutParams(-1, -2));
  }

  private void weighted(LinearLayout l, View v) {
    l.addView(v, new LinearLayout.LayoutParams(0, -2, 1));
  }

  private LinearLayout card(LinearLayout parent) {
    LinearLayout c = column();
    c.setPadding(dp(18), dp(18), dp(18), dp(18));
    c.setBackground(shape(CARD, 20));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = dp(14);
    parent.addView(c, lp);
    return c;
  }

  private Button button(String label, boolean primary, Runnable action) {
    Button b = new Button(this);
    b.setText(label);
    b.setTextSize(14);
    b.setAllCaps(false);
    b.setMinHeight(dp(48));
    b.setMinimumHeight(dp(48));
    b.setTextColor(primary ? BG : WHITE);
    b.setBackgroundTintList(ColorStateList.valueOf(primary ? GREEN : LINE));
    b.setOnClickListener(v -> action.run());
    return b;
  }

  private void section(String label, String sub) {
    space(body, 8);
    add(body, text(label, 20, WHITE, true));
    if (sub != null) {
      space(body, 5);
      add(body, text(sub, 12, MUTED, false));
    }
    space(body, 14);
  }

  private void divider(LinearLayout l) {
    View v = new View(this);
    v.setBackgroundColor(LINE);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(1));
    p.topMargin = dp(12);
    p.bottomMargin = dp(12);
    l.addView(v, p);
  }

  private void hint(LinearLayout c, String s) {
    TextView t = text(s, 12, MUTED, false);
    t.setLineSpacing(dp(3), 1);
    add(c, t);
  }

  private void render() {
    root = column();
    root.setBackgroundColor(BG);
    root.setFitsSystemWindows(true);
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          v.setPadding(
              insets.getSystemWindowInsetLeft(),
              insets.getSystemWindowInsetTop(),
              insets.getSystemWindowInsetRight(),
              insets.getSystemWindowInsetBottom());
          return insets.consumeSystemWindowInsets();
        });
    setContentView(root);
    root.requestApplyInsets();
    LinearLayout header = row();
    header.setPadding(dp(20), dp(14), dp(20), dp(12));
    LinearLayout branding = column();
    add(branding, text("CEP BÜTÇEM", 11, GREEN, true));
    space(branding, 5);
    String[] names = {"Genel bakış", "İşlemler", "Analizler", "Bütçeler", "Hesap & ayarlar"};
    add(branding, text(names[page], 24, WHITE, true));
    weighted(header, branding);
    TextView avatar = text("CB", 14, GREEN, true);
    avatar.setGravity(Gravity.CENTER);
    avatar.setBackground(shape(LINE, 16));
    header.addView(avatar, new LinearLayout.LayoutParams(dp(44), dp(44)));
    avatar.setContentDescription("Cep Bütçem; ayarları aç");
    avatar.setOnClickListener(
        v -> {
          page = 4;
          render();
        });
    add(root, header);
    if (page != 4) {
      LinearLayout period = row();
      period.setPadding(dp(14), 0, dp(14), dp(4));
      Button prev = button("‹", false, () -> changeMonth(-1));
      prev.setContentDescription("Önceki maaş dönemi");
      period.addView(prev, new LinearLayout.LayoutParams(dp(48), dp(48)));
      TextView label =
          text(
              periodStart(month).format(SHORT)
                  + " – "
                  + periodEnd(month).format(DateTimeFormatter.ofPattern("d MMM yyyy", Finance.TR)),
              13,
              WHITE,
              true);
      label.setGravity(Gravity.CENTER);
      weighted(period, label);
      label.setOnClickListener(v -> pickMonth());
      label.setContentDescription("Maaş dönemi seç: " + month.format(MONTH));
      Button next = button("›", false, () -> changeMonth(1));
      next.setContentDescription("Sonraki maaş dönemi");
      period.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48)));
      add(root, period);
    }
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setClipToPadding(false);
    body = column();
    body.setPadding(dp(20), dp(12), dp(20), dp(24));
    scroll.addView(body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    switch (page) {
      case 0:
        dashboard();
        break;
      case 1:
        transactions();
        break;
      case 2:
        analysis();
        break;
      case 3:
        budgets();
        break;
      case 4:
        settings();
        break;
    }
    navigation = row();
    navigation.setPadding(dp(6), dp(6), dp(6), dp(8));
    navigation.setBackgroundColor(CARD);
    String[] icons = {"◈", "⇄", "▥", "◎", "⚙"},
        labels = {"Özet", "İşlemler", "Analiz", "Bütçe", "Ayarlar"};
    for (int i = 0; i < 5; i++) {
      final int tab = i;
      LinearLayout item = column();
      item.setGravity(Gravity.CENTER);
      item.setPadding(0, dp(5), 0, dp(5));
      TextView icon = text(icons[i], 22, i == page ? GREEN : MUTED, true);
      icon.setGravity(Gravity.CENTER);
      add(item, icon);
      space(item, 4);
      TextView label = text(labels[i], 10, i == page ? GREEN : MUTED, i == page);
      label.setGravity(Gravity.CENTER);
      add(item, label);
      if (i == page) item.setBackground(shape(0xFF223A3A, 12));
      item.setContentDescription(labels[i]);
      item.setOnClickListener(
          v -> {
            page = tab;
            refresh();
          });
      navigation.addView(item, new LinearLayout.LayoutParams(0, dp(58), 1));
    }
    add(root, navigation);
  }

  private void changeMonth(int delta) {
    YearMonth next = month.plusMonths(delta);
    if (next.getYear() >= 2000 && next.getYear() <= 2100) {
      month = next;
      render();
    }
  }

  private String capitalize(String s) {
    return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Finance.TR) + s.substring(1);
  }

  private String pct(double n) {
    return Double.isNaN(n) ? "—" : String.format(Finance.TR, "%%%.1f", n);
  }

  private String signedPct(double n) {
    return Double.isNaN(n)
        ? "Baz veri yok"
        : String.format(Finance.TR, "%s%%%.1f", n > 0 ? "+" : "", n);
  }

  private LocalDate effectiveEnd(LocalDate end) {
    return end.isAfter(LocalDate.now()) ? LocalDate.now() : end;
  }

  private List<Finance.Entry> current() {
    return Finance.period(entries, periodStart(month), effectiveEnd(periodEnd(month)));
  }

  private String category(long id) {
    for (Finance.Category c : categories) if (c.id == id) return c.name;
    return "Diğer";
  }

  private String account(long id) {
    for (Finance.Account a : accounts) if (a.id == id) return a.name;
    return "Hesap";
  }

  private void pickMonth() {
    DatePickerDialog d =
        new DatePickerDialog(
            this,
            (view, y, m, day) -> {
              month = YearMonth.of(y, m + 1);
              render();
            },
            month.getYear(),
            month.getMonthValue() - 1,
            1);
    d.setTitle("Maaşın başladığı ayı seç");
    d.getDatePicker().setMinDate(epoch(LocalDate.of(2000, 1, 1)));
    d.getDatePicker().setMaxDate(epoch(LocalDate.of(2100, 12, 31)));
    d.show();
  }

  private long epoch(LocalDate d) {
    return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
  }

  private void dashboard() {
    List<Finance.Entry> list = current();
    long income = Finance.total(list, "I"), expense = Finance.total(list, "E"), balance = 0;
    for (Finance.Account a : accounts)
      balance = Math.addExact(balance, Finance.balance(a, entries, LocalDate.now()));
    LinearLayout hero = card(body);
    hero.setBackground(shape(plan.light ? 0xFFE2F2EC : 0xFF203D3A, 22));
    add(hero, text("TOPLAM VARLIK · BUGÜN", 11, MUTED, true));
    space(hero, 12);
    add(hero, text(Finance.money(balance), 32, WHITE, true));
    space(hero, 9);
    hint(hero, "Tüm hesapların açılış bakiyesi ve bugüne kadarki işlemleri");
    divider(hero);
    LinearLayout metrics = row();
    LinearLayout in = column(), out = column();
    add(in, text("↗  Dönemin geliri", 12, MUTED, false));
    space(in, 6);
    add(in, text(Finance.money(income), 18, GREEN, true));
    add(out, text("↘  Dönemin gideri", 12, MUTED, false));
    space(out, 6);
    add(out, text(Finance.money(expense), 18, RED, true));
    weighted(metrics, in);
    weighted(metrics, out);
    add(hero, metrics);
    LinearLayout actions = row();
    weighted(actions, button("− Gider ekle", true, () -> entryDialog(null, "E")));
    gap(actions, 8);
    weighted(actions, button("+ Gelir ekle", false, () -> entryDialog(null, "I")));
    add(body, actions);
    space(body, 16);
    LinearLayout health = card(body);
    LinearLayout heading = row();
    weighted(heading, text("Maaş dönemi dengesi", 16, WHITE, true));
    heading.addView(
        text(pct(Finance.savingRate(income, expense)), 16, income >= expense ? GREEN : RED, true));
    add(health, heading);
    space(health, 8);
    add(health, text(Finance.money(income - expense), 26, income >= expense ? GREEN : RED, true));
    space(health, 8);
    hint(
        health,
        income == 0
            ? "Tasarruf oranı için gelir kaydı gerekli."
            : "Gelirin "
                + pct(Finance.savingRate(income, expense))
                + " kadarı kaldı. Oran = (gelir − gider) / gelir.");
    keyValue(
        health,
        "Gelirin harcanan kısmı",
        pct(Finance.spendingRate(income, expense)),
        expense > income ? RED : WHITE);
    int days = Finance.cycleDays(month, plan.payday, LocalDate.now());
    divider(health);
    keyValue(
        health,
        "Günlük ortalama gider",
        Finance.money(days > 0 ? Math.round(expense / (double) days) : 0),
        WHITE);
    keyValue(
        health,
        "Dönem sonu gider tahmini",
        days == 0
            ? "Henüz hesaplanamaz"
            : Finance.money(Finance.cycleForecast(expense, month, plan.payday, LocalDate.now())),
        WHITE);
    space(health, 8);
    hint(
        health,
        "Tahmin, gerçekleşmiş günlük ortalamayı maaş döneminin tamamına yayar; kesin sonuç"
            + " değildir.");
    Map<Long, Long> used = Finance.byCategory(list, "E"), limits = ledger.budgets();
    for (Map.Entry<Long, Long> b : limits.entrySet())
      if (used.getOrDefault(b.getKey(), 0L) >= b.getValue() * .8) {
        LinearLayout warning = card(body);
        add(
            warning,
            text(
                used.getOrDefault(b.getKey(), 0L) > b.getValue()
                    ? "Bütçe sınırı aşıldı"
                    : "Bütçe sınırına yaklaşıyorsun",
                15,
                RED,
                true));
        space(warning, 6);
        hint(
            warning,
            category(b.getKey())
                + ": "
                + Finance.money(used.getOrDefault(b.getKey(), 0L))
                + " / "
                + Finance.money(b.getValue()));
        warning.setOnClickListener(
            v -> {
              page = 3;
              render();
            });
        break;
      }
    advice(current(), periodStart(month), effectiveEnd(periodEnd(month)), true);
    obligations(body, false);
    section("Son işlemler", "Seçili maaş dönemi · detay ve düzenleme için kayda dokun");
    List<Finance.Entry> monthAll = Finance.period(entries, periodStart(month), periodEnd(month));
    if (monthAll.isEmpty())
      empty(
          "Yeni bir başlangıç",
          "İlk kaydını ekle; gelir, gider ve analizlerin burada oluşacak.",
          "İlk gideri ekle",
          () -> entryDialog(null, "E"));
    else {
      LinearLayout c = card(body);
      for (int i = 0; i < Math.min(5, monthAll.size()); i++) {
        if (i > 0) divider(c);
        entryRow(c, monthAll.get(i));
      }
      add(
          body,
          button(
              "Tüm işlemleri gör",
              false,
              () -> {
                page = 1;
                render();
              }));
    }
  }

  private void keyValue(LinearLayout c, String label, String value, int color) {
    LinearLayout r = row();
    TextView l = text(label, 13, MUTED, false);
    weighted(r, l);
    TextView v = text(value, 13, color, true);
    v.setGravity(Gravity.END);
    r.addView(v);
    add(c, r);
    space(c, 9);
  }

  private void empty(String title, String explanation, String action, Runnable click) {
    LinearLayout c = card(body);
    add(c, text(title, 18, WHITE, true));
    space(c, 10);
    hint(c, explanation);
    if (action != null) {
      space(c, 14);
      add(c, button(action, true, click));
    }
  }

  private void entryRow(LinearLayout parent, Finance.Entry e) {
    LinearLayout r = row();
    String title = e.kind.equals("T") ? "Hesaplar arası transfer" : category(e.categoryId);
    int color = e.kind.equals("I") ? GREEN : e.kind.equals("E") ? RED : BLUE;
    TextView icon =
        text(e.kind.equals("I") ? "↗" : e.kind.equals("E") ? "↘" : "⇄", 20, color, true);
    icon.setGravity(Gravity.CENTER);
    icon.setBackground(shape(LINE, 12));
    r.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));
    gap(r, 12);
    LinearLayout description = column();
    add(description, text(title, 14, WHITE, true));
    space(description, 5);
    add(
        description,
        text(
            e.date.format(SHORT)
                + " · "
                + account(e.accountId)
                + (e.kind.equals("T") ? " → " + account(e.targetId) : ""),
            11,
            MUTED,
            false));
    if (!e.note.isEmpty()) {
      space(description, 4);
      TextView note = text(e.note, 11, MUTED, false);
      note.setMaxLines(2);
      note.setEllipsize(TextUtils.TruncateAt.END);
      add(description, note);
    }
    if (e.date.isAfter(LocalDate.now())) {
      space(description, 4);
      add(description, text("Planlanan · henüz bakiyede değil", 10, BLUE, false));
    }
    weighted(r, description);
    gap(r, 8);
    TextView amount =
        text(
            (e.kind.equals("I") ? "+" : e.kind.equals("E") ? "−" : "") + Finance.money(e.amount),
            14,
            color,
            true);
    amount.setGravity(Gravity.END);
    r.addView(amount);
    r.setPadding(0, dp(4), 0, dp(4));
    r.setOnClickListener(v -> entryDialog(e, e.kind));
    add(parent, r);
  }

  private void transactions() {
    add(body, button("+ Yeni işlem / transfer", true, () -> entryDialog(null, "E")));
    space(body, 12);
    EditText query = field("Not, kategori veya hesap ara", search, false);
    query.setSingleLine(true);
    add(body, query);
    space(body, 8);
    LinearLayout type = row();
    String[] labels = {"Tümü", "Gider", "Gelir", "Transfer"};
    Spinner types = spinner(Arrays.asList(labels));
    types.setSelection(transactionType);
    weighted(type, types);
    gap(type, 8);
    List<String> accountLabels = new ArrayList<>();
    accountLabels.add("Tüm hesaplar");
    for (Finance.Account a : accounts) accountLabels.add(a.name);
    Spinner ac = spinner(accountLabels);
    for (int i = 0; i < accounts.size(); i++)
      if (accounts.get(i).id == filterAccount) ac.setSelection(i + 1);
    weighted(type, ac);
    add(body, type);
    space(body, 6);
    List<String> categoryLabels = new ArrayList<>();
    categoryLabels.add("Tüm kategoriler");
    for (Finance.Category c : categories) categoryLabels.add(c.name);
    Spinner cats = spinner(categoryLabels);
    for (int i = 0; i < categories.size(); i++)
      if (categories.get(i).id == filterCategory) cats.setSelection(i + 1);
    add(body, cats);
    space(body, 10);
    TextView summary = text("", 12, MUTED, false);
    add(body, summary);
    space(body, 12);
    LinearLayout results = column();
    add(body, results);
    final int[] shown = {100};
    final Runnable[] update = {null};
    update[0] =
        () -> {
          results.removeAllViews();
          List<Finance.Entry> filtered = new ArrayList<>();
          String needle = search.toLowerCase(Finance.TR);
          String kind =
              transactionType == 1
                  ? "E"
                  : transactionType == 2 ? "I" : transactionType == 3 ? "T" : "";
          for (Finance.Entry e : Finance.period(entries, periodStart(month), periodEnd(month)))
            if ((kind.isEmpty() || kind.equals(e.kind))
                && (filterAccount == 0
                    || filterAccount == e.accountId
                    || filterAccount == e.targetId)
                && (filterCategory == 0 || filterCategory == e.categoryId)
                && (e.note
                        + " "
                        + category(e.categoryId)
                        + " "
                        + account(e.accountId)
                        + " "
                        + (e.targetId > 0 ? account(e.targetId) : ""))
                    .toLowerCase(Finance.TR)
                    .contains(needle)) filtered.add(e);
          summary.setText(
              getResources()
                  .getQuantityString(
                      R.plurals.transaction_summary,
                      filtered.size(),
                      filtered.size(),
                      Finance.money(Finance.total(filtered, "I")),
                      Finance.money(Finance.total(filtered, "E"))));
          if (filtered.isEmpty()) {
            LinearLayout c = card(results);
            add(c, text("Eşleşen işlem yok", 16, WHITE, true));
            space(c, 7);
            hint(c, "Maaş dönemini veya filtreleri değiştir; yeni kaydın tarihini kontrol et.");
          } else {
            LinearLayout c = card(results);
            LocalDate last = null;
            for (Finance.Entry e : filtered.subList(0, Math.min(shown[0], filtered.size()))) {
              if (last != null) divider(c);
              if (!e.date.equals(last)) {
                add(c, text(e.date.format(DATE), 11, MUTED, true));
                space(c, 10);
              }
              entryRow(c, e);
              last = e.date;
            }
            if (filtered.size() > shown[0]) {
              add(
                  results,
                  button(
                      "Daha fazla göster (" + (filtered.size() - shown[0]) + " kayıt)",
                      false,
                      () -> {
                        shown[0] += 100;
                        update[0].run();
                      }));
            }
          }
        };
    query.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            search = s.toString();
            update[0].run();
          }

          public void afterTextChanged(Editable e) {}
        });
    types.setOnItemSelectedListener(
        selected(
            pos -> {
              transactionType = pos;
              update[0].run();
            }));
    ac.setOnItemSelectedListener(
        selected(
            pos -> {
              filterAccount = pos == 0 ? 0 : accounts.get(pos - 1).id;
              update[0].run();
            }));
    cats.setOnItemSelectedListener(
        selected(
            pos -> {
              filterCategory = pos == 0 ? 0 : categories.get(pos - 1).id;
              update[0].run();
            }));
    update[0].run();
  }

  private interface Selection {
    void select(int position);
  }

  private AdapterView.OnItemSelectedListener selected(Selection cb) {
    return new AdapterView.OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
        cb.select(pos);
      }

      public void onNothingSelected(AdapterView<?> a) {}
    };
  }

  private void analysis() {
    Spinner mode =
        spinner(
            Arrays.asList(
                "Maaş dönemi", "Son 3 maaş dönemi", "12 dönem · seçili yıl", "Özel tarih aralığı"));
    mode.setSelection(analysisMode);
    add(body, mode);
    mode.setOnItemSelectedListener(
        selected(
            pos -> {
              if (analysisMode != pos) {
                analysisMode = pos;
                render();
              }
            }));
    space(body, 12);
    if (analysisMode == 3) {
      LinearLayout dates = row();
      weighted(
          dates,
          button(
              customFrom.format(DATE),
              false,
              () ->
                  chooseDate(
                      customFrom,
                      d -> {
                        customFrom = d;
                        if (customTo.isBefore(d)) customTo = d;
                        render();
                      })));
      gap(dates, 8);
      weighted(
          dates,
          button(
              customTo.format(DATE),
              false,
              () ->
                  chooseDate(
                      customTo,
                      d -> {
                        customTo = d;
                        if (customFrom.isAfter(d)) customFrom = d;
                        render();
                      })));
      add(body, dates);
      space(body, 12);
    }
    LocalDate from = periodStart(month), to = periodEnd(month);
    if (analysisMode == 1) from = periodStart(month.minusMonths(2));
    else if (analysisMode == 2) {
      from = periodStart(YearMonth.of(month.getYear(), 1));
      to = periodEnd(YearMonth.of(month.getYear(), 12));
    } else if (analysisMode == 3) {
      from = customFrom;
      to = customTo;
    }
    to = effectiveEnd(to);
    final LocalDate rangeFrom = from, rangeTo = to;
    List<Finance.Entry> list = Finance.period(entries, from, to);
    long income = Finance.total(list, "I"), expense = Finance.total(list, "E");
    int days = to.isBefore(from) ? 0 : (int) ChronoUnit.DAYS.between(from, to) + 1;
    LocalDate prevEnd = from.minusDays(1), prevFrom = prevEnd.minusDays(Math.max(0, days - 1));
    if (analysisMode == 0) {
      YearMonth prev = month.minusMonths(1);
      prevFrom = periodStart(prev);
      prevEnd = prevFrom.plusDays(Math.max(0, days - 1));
      if (prevEnd.isAfter(periodEnd(prev))) prevEnd = periodEnd(prev);
    }
    List<Finance.Entry> previous =
        days == 0 ? Collections.emptyList() : Finance.period(entries, prevFrom, prevEnd);
    long oldExpense = Finance.total(previous, "E"), oldIncome = Finance.total(previous, "I");
    add(
        body,
        text(
            days == 0
                ? "Bu dönem henüz başlamadı"
                : from.format(DATE) + " – " + to.format(DATE) + " · " + days + " gün",
            12,
            MUTED,
            false));
    space(body, 14);
    LinearLayout c = card(body);
    add(c, text("Dönem özeti", 17, WHITE, true));
    space(c, 16);
    keyValue(c, "Toplam gelir", Finance.money(income), GREEN);
    keyValue(c, "Toplam gider", Finance.money(expense), RED);
    keyValue(
        c, "Net nakit akışı", Finance.money(income - expense), income >= expense ? GREEN : RED);
    keyValue(
        c,
        "Tasarruf oranı",
        pct(Finance.savingRate(income, expense)),
        income >= expense ? GREEN : RED);
    divider(c);
    keyValue(c, "Gelir değişimi", signedPct(Finance.change(income, oldIncome)), WHITE);
    keyValue(
        c,
        "Gider değişimi",
        signedPct(Finance.change(expense, oldExpense)),
        expense <= oldExpense ? GREEN : RED);
    hint(
        c,
        days == 0
            ? "Karşılaştırma için gerçekleşmiş kayıt gerekli."
            : "Karşılaştırma: "
                + prevFrom.format(DATE)
                + " – "
                + prevEnd.format(DATE)
                + ". Seçili maaş döneminde önceki dönemin aynı gün aralığı; diğer aralıklarda"
                + " önceki eşit uzunluktaki dönem.");
    space(c, 7);
    hint(
        c,
        "Transferler gelir, gider ve tasarruf hesaplarına dahil edilmez. İleri tarihli kayıtlar"
            + " gerçekleşene kadar analiz dışında kalır.");
    keyValue(
        c,
        "Gelirin harcanan kısmı",
        pct(Finance.spendingRate(income, expense)),
        expense > income ? RED : WHITE);
    advice(list, from, to, analysisMode == 0);
    section("Harcama içgörüleri", "Kayıtlarından hesaplanan göstergeler");
    c = card(body);
    Map<Long, Long> spending = Finance.byCategory(list, "E");
    Set<LocalDate> spendingDays = new HashSet<>();
    int count = 0;
    Finance.Entry largest = null;
    for (Finance.Entry e : list)
      if (e.kind.equals("E")) {
        count++;
        spendingDays.add(e.date);
        if (largest == null || e.amount > largest.amount) largest = e;
      }
    keyValue(c, "Gider işlemi sayısı", Integer.toString(count), WHITE);
    keyValue(
        c,
        "Günlük ortalama",
        Finance.money(days > 0 ? Math.round(expense / (double) days) : 0),
        WHITE);
    keyValue(
        c,
        "İşlem başına ortalama",
        Finance.money(count > 0 ? Math.round(expense / (double) count) : 0),
        WHITE);
    keyValue(c, "Harcama yapılan gün", spendingDays.size() + " / " + days, WHITE);
    if (largest != null) {
      divider(c);
      hint(
          c,
          "En büyük gider: "
              + category(largest.categoryId)
              + " · "
              + Finance.money(largest.amount)
              + " · "
              + largest.date.format(DATE));
    }
    if (!spending.isEmpty()) {
      Map.Entry<Long, Long> top = spending.entrySet().iterator().next();
      space(c, 10);
      hint(
          c,
          "En yüksek pay "
              + category(top.getKey())
              + ": "
              + pct(expense > 0 ? 100d * top.getValue() / expense : 0)
              + ". Bu kategorideki %10 azalma "
              + Finance.money(Math.round(top.getValue() * .1))
              + " tasarruf sağlar.");
    }
    if (analysisMode == 0 && month.equals(Finance.cycleMonth(LocalDate.now(), plan.payday))) {
      divider(c);
      long forecast = Finance.cycleForecast(expense, month, plan.payday, LocalDate.now());
      keyValue(c, "Dönem sonu gider tahmini", Finance.money(forecast), WHITE);
      hint(
          c,
          "Geçen günlerin ortalamasına dayanır; tek seferlik giderler tahmini yükseltebilir."
              + " Gelecekteki gelirleri tahmin etmez.");
    }
    section(
        "Kategori dağılımı",
        categoryIncome
            ? "Gelir kaynakların ve dönem değişimleri"
            : "Harcama payların ve dönem değişimleri");
    add(
        body,
        button(
            categoryIncome ? "Gider kategorilerini göster" : "Gelir kategorilerini göster",
            false,
            () -> {
              categoryIncome = !categoryIncome;
              render();
            }));
    space(body, 12);
    String kind = categoryIncome ? "I" : "E";
    Map<Long, Long> sums = Finance.byCategory(list, kind),
        oldSums = Finance.byCategory(previous, kind);
    long categoryTotal = categoryIncome ? income : expense;
    if (sums.isEmpty())
      empty(
          "Bu aralıkta " + (categoryIncome ? "gelir" : "gider") + " yok",
          "Kategori analizi, kayıt ekledikçe kendiliğinden oluşur.",
          null,
          null);
    else {
      c = card(body);
      List<Long> chart = new ArrayList<>(sums.values());
      List<String> labels = new ArrayList<>();
      for (long id : sums.keySet()) labels.add(category(id));
      if (!categoryIncome) add(c, new Charts(this, chart, Collections.emptyList(), labels, true));
      int i = 0;
      for (Map.Entry<Long, Long> s : sums.entrySet()) {
        LinearLayout r = row();
        TextView dot = text("●", 14, Charts.COLORS[i++ % Charts.COLORS.length], true);
        r.addView(dot);
        gap(r, 8);
        weighted(r, text(category(s.getKey()), 14, WHITE, true));
        r.addView(text(Finance.money(s.getValue()), 14, WHITE, true));
        add(c, r);
        space(c, 6);
        hint(
            c,
            pct(100d * s.getValue() / categoryTotal)
                + " pay · önceki döneme göre "
                + signedPct(Finance.change(s.getValue(), oldSums.getOrDefault(s.getKey(), 0L))));
        space(c, 10);
        progress(c, (double) s.getValue() / categoryTotal, GREEN);
        space(c, 14);
      }
    }
    section("6 maaş dönemi eğilimi", "Yeşil: gelir · Pembe: gider · Tutarlar TL");
    c = card(body);
    YearMonth chartEnd = analysisMode == 3 ? YearMonth.from(customTo) : month;
    List<Finance.Entry> occurred =
        Finance.period(entries, LocalDate.of(2000, 1, 1), LocalDate.now());
    List<Long> ins = Finance.cycles(occurred, chartEnd, 6, "I", plan.payday),
        outs = Finance.cycles(occurred, chartEnd, 6, "E", plan.payday);
    List<String> labels = new ArrayList<>();
    for (int i = 5; i >= 0; i--)
      labels.add(chartEnd.minusMonths(i).format(DateTimeFormatter.ofPattern("MMM", Finance.TR)));
    add(c, new Charts(this, ins, outs, labels, false));
    divider(c);
    for (int i = 0; i < 6; i++) {
      YearMonth m = chartEnd.minusMonths(5 - i);
      add(c, text(capitalize(m.format(MONTH)), 12, WHITE, true));
      space(c, 5);
      keyValue(
          c,
          "Gelir / Gider",
          Finance.money(ins.get(i)) + " / " + Finance.money(outs.get(i)),
          MUTED);
    }
    section("Haftanın günleri", "Seçili aralıkta gün başına ortalama gider");
    c = card(body);
    long[] totals = new long[7];
    int[] occurrences = new int[7];
    for (Finance.Entry e : list)
      if (e.kind.equals("E")) totals[e.date.getDayOfWeek().getValue() - 1] += e.amount;
    for (LocalDate d = rangeFrom; !d.isAfter(rangeTo); d = d.plusDays(1))
      occurrences[d.getDayOfWeek().getValue() - 1]++;
    long max = 1;
    long[] averages = new long[7];
    for (int i = 0; i < 7; i++) {
      averages[i] = occurrences[i] > 0 ? Math.round(totals[i] / (double) occurrences[i]) : 0;
      max = Math.max(max, averages[i]);
    }
    String[] weekdays = {"Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"};
    for (int i = 0; i < 7; i++) {
      keyValue(c, weekdays[i], occurrences[i] == 0 ? "—" : Finance.money(averages[i]), WHITE);
      progress(c, (double) averages[i] / max, BLUE);
      space(c, 12);
    }
    hint(
        c,
        "Örneğin toplam pazartesi gideri, aralıktaki pazartesi sayısına bölünür; harcamasız günler"
            + " de hesaba katılır.");
    section("Hesap bazında hareket", "Seçili aralık · transferler ayrı");
    c = card(body);
    for (Finance.Account a : accounts) {
      List<Finance.Entry> own = new ArrayList<>();
      long transferIn = 0, transferOut = 0;
      for (Finance.Entry e : list) {
        if (e.accountId == a.id) own.add(e);
        if (e.kind.equals("T")) {
          if (e.accountId == a.id) transferOut += e.amount;
          if (e.targetId == a.id) transferIn += e.amount;
        }
      }
      add(c, text(a.name, 15, WHITE, true));
      space(c, 10);
      keyValue(c, "Gelir", Finance.money(Finance.total(own, "I")), GREEN);
      keyValue(c, "Gider", Finance.money(Finance.total(own, "E")), RED);
      keyValue(
          c,
          "Transfer giriş / çıkış",
          Finance.money(transferIn) + " / " + Finance.money(transferOut),
          BLUE);
      divider(c);
    }
  }

  private void progress(LinearLayout parent, double fraction, int color) {
    ProgressBar p = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    p.setMax(1000);
    p.setProgress((int) (Math.max(0, Math.min(1, fraction)) * 1000));
    p.setProgressTintList(ColorStateList.valueOf(color));
    p.setProgressBackgroundTintList(ColorStateList.valueOf(LINE));
    add(parent, p);
  }

  private void budgets() {
    add(body, button("+ Kategori bütçesi belirle", true, () -> budgetDialog(0)));
    space(body, 12);
    add(
        body,
        text(
            "Limitler her maaş döneminde tekrar uygulanır. Seçili dönemin gerçekleşmiş giderleri"
                + " gösterilir.",
            12,
            MUTED,
            false));
    space(body, 16);
    Map<Long, Long> limits = ledger.budgets(), spent = Finance.byCategory(current(), "E");
    long totalLimit = 0, totalSpent = 0;
    for (Map.Entry<Long, Long> e : limits.entrySet()) {
      totalLimit += e.getValue();
      totalSpent += spent.getOrDefault(e.getKey(), 0L);
    }
    if (limits.isEmpty()) {
      empty(
          "Harcama sınırlarını sen belirle",
          "Market, ulaşım veya eğlence için maaş dönemi limiti ekle. Kullanım oranını ve kalan"
              + " tutarı buradan izle.",
          "İlk bütçeyi ekle",
          () -> budgetDialog(0));
      return;
    }
    LinearLayout summary = card(body);
    add(summary, text("Bütçelenen kategoriler", 16, WHITE, true));
    space(summary, 12);
    keyValue(summary, "Toplam dönem limiti", Finance.money(totalLimit), WHITE);
    keyValue(summary, "Bu kategorilerde harcanan", Finance.money(totalSpent), WHITE);
    keyValue(
        summary,
        "Kalan",
        Finance.money(totalLimit - totalSpent),
        totalSpent > totalLimit ? RED : GREEN);
    progress(summary, (double) totalSpent / totalLimit, totalSpent > totalLimit ? RED : GREEN);
    space(summary, 8);
    hint(summary, "Limiti olmayan kategoriler bu toplamın dışında kalır.");
    for (Map.Entry<Long, Long> b : limits.entrySet()) {
      long used = spent.getOrDefault(b.getKey(), 0L), left = b.getValue() - used;
      int color = left < 0 ? RED : used >= b.getValue() * .8 ? 0xFFF6BD69 : GREEN;
      LinearLayout c = card(body);
      LinearLayout r = row();
      weighted(r, text(category(b.getKey()), 17, WHITE, true));
      r.addView(text(pct(100d * used / b.getValue()), 15, color, true));
      add(c, r);
      space(c, 14);
      keyValue(
          c, "Harcama / Limit", Finance.money(used) + " / " + Finance.money(b.getValue()), WHITE);
      progress(c, (double) used / b.getValue(), color);
      space(c, 12);
      keyValue(c, left < 0 ? "Aşım" : "Kalan", Finance.money(Math.abs(left)), color);
      if (month.equals(Finance.cycleMonth(LocalDate.now(), plan.payday))) {
        int remaining = (int) ChronoUnit.DAYS.between(LocalDate.now(), periodEnd(month)) + 1;
        hint(
            c,
            "Bugün dahil kalan "
                + remaining
                + " gün için günlük kullanılabilir: "
                + Finance.money(Math.max(0, left) / remaining));
      }
      space(c, 8);
      add(c, button("Limiti düzenle", false, () -> budgetDialog(b.getKey())));
    }
    long uncovered = 0;
    for (Map.Entry<Long, Long> e : spent.entrySet())
      if (!limits.containsKey(e.getKey())) uncovered += e.getValue();
    if (uncovered > 0) {
      LinearLayout c = card(body);
      hint(c, "Bütçe tanımlanmamış kategorilerde " + Finance.money(uncovered) + " harcama var.");
    }
  }

  private void settings() {
    preferences();
    obligations(body, true);
    LinearLayout privacy = card(body);
    add(privacy, text("Senin verin, senin telefonun", 17, GREEN, true));
    space(privacy, 9);
    hint(
        privacy,
        "Hesap veya internet bağlantısı gerekmez. Kayıtlar uygulamanın yerel veritabanında tutulur."
            + " Otomatik bulut yedeği kapalıdır; telefonu değiştirirken JSON yedeğini taşı.");
    section("Hesapların", "Bakiyeler bugüne kadarki gerçekleşmiş işlemleri içerir");
    for (Finance.Account a : accounts) {
      LinearLayout c = card(body);
      LinearLayout r = row();
      weighted(r, text(a.name, 17, WHITE, true));
      r.addView(text(Finance.money(Finance.balance(a, entries, LocalDate.now())), 19, GREEN, true));
      add(c, r);
      space(c, 10);
      hint(c, "Açılış bakiyesi: " + Finance.money(a.opening));
      space(c, 8);
      add(c, button("Hesabı düzenle", false, () -> accountDialog(a)));
    }
    add(body, button("+ Hesap ekle", true, () -> accountDialog(null)));
    space(body, 8);
    add(body, button("⇄ Hesaplar arası transfer", false, () -> entryDialog(null, "T")));
    section("Kategoriler", "Gelir ve gider için kendi kategorilerini ekle");
    LinearLayout c = card(body);
    hint(c, categories.size() + " kategori · İsim değişiklikleri geçmiş kayıtlara da yansır.");
    space(c, 12);
    add(c, button("Kategorileri yönet", false, () -> categoryManager()));
    section(
        "Düzenli işlemler", "Her ay aynı gün · uygulama açılınca vadesi gelen kayıtlar eklenir");
    try {
      JSONArray templates = ledger.recurring();
      if (templates.length() == 0) {
        c = card(body);
        hint(c, "Maaş, kira ve abonelikleri düzenli işlem olarak tanımlayabilirsin.");
      }
      for (int i = 0; i < templates.length(); i++) {
        JSONObject t = templates.getJSONObject(i);
        long id = t.getLong("id");
        c = card(body);
        add(
            c,
            text(
                category(t.getLong("category_id")) + " · " + Finance.money(t.getLong("amount")),
                16,
                t.getString("kind").equals("I") ? GREEN : RED,
                true));
        space(c, 8);
        hint(
            c,
            account(t.getLong("account_id"))
                + " · Her ayın "
                + t.getInt("day")
                + ". günü\nSonraki kayıt: "
                + LocalDate.parse(t.getString("next_date")).format(DATE));
        if (!t.getString("note").isEmpty()) {
          space(c, 6);
          hint(c, t.getString("note"));
        }
        space(c, 10);
        add(
            c,
            button(
                "Düzenli işlemi durdur",
                false,
                () ->
                    confirm(
                        "Düzenli işlem durdurulsun mu?",
                        "Önceden oluşmuş kayıtlar korunur; gelecek aylarda otomatik kayıt"
                            + " eklenmez.",
                        () -> {
                          ledger.deleteRecurring(id);
                          refresh();
                        })));
      }
    } catch (JSONException e) {
      error("Düzenli işlemler okunamadı", e);
    }
    add(body, button("+ Aylık düzenli işlem", true, () -> recurringDialog()));
    space(body, 10);
    hint(
        body,
        "Arka planda bildirim veya banka bağlantısı yoktur. Kısa aylarda ayın son günü kullanılır;"
            + " sonraki ay tercih edilen güne geri dönülür.");
    section("Verilerini taşı", "JSON: tam yedek · CSV: Excel / tablo analizi");
    c = card(body);
    add(c, button("JSON yedeğini kaydet", true, () -> exportFile(EXPORT_JSON)));
    space(c, 8);
    add(c, button("CSV olarak dışa aktar", false, () -> exportFile(EXPORT_CSV)));
    space(c, 8);
    add(c, button("JSON yedeğinden geri yükle", false, () -> importFile()));
    space(c, 12);
    hint(
        c,
        "Yedekler şifrelenmez. Güvenli bir yerde sakla. Geri yükleme mevcut kayıtların tamamını"
            + " değiştirir; önce yedek al.");
    section("Nasıl hesaplanıyor?", null);
    c = card(body);
    hint(
        c,
        "• Tek para birimi: Türk lirası (TL).\n"
            + "• Bakiye = açılış + gelir − gider + transfer giriş − transfer çıkış.\n"
            + "• Açılış bakiyesi gelir değildir.\n"
            + "• İleri tarihli kayıtlar İşlemler'de planlanan olarak görünür.\n"
            + "• Bütçeler maaş döneminde tekrarlanan kategori limitleridir.\n"
            + "• Tahminler geçmiş kayıt ortalamalarına dayanır; yatırım tavsiyesi değildir.\n"
            + "• Yedek geri yükleme sonrası vadesi gelen düzenli kayıtlar işlenir.");
    space(c, 12);
    add(c, text("Cep Bütçem · v1.1.0", 12, GREEN, true));
  }

  public boolean isLightTheme() {
    return plan.light;
  }

  private LocalDate periodStart(YearMonth m) {
    return Finance.cycleStart(m, plan.payday);
  }

  private LocalDate periodEnd(YearMonth m) {
    return Finance.cycleEnd(m, plan.payday);
  }

  private void applyPalette() {
    BG = plan.light ? 0xFFF2F5F9 : 0xFF101820;
    CARD = plan.light ? 0xFFFFFFFF : 0xFF192530;
    LINE = plan.light ? 0xFFD7E0E8 : 0xFF2B3A47;
    WHITE = plan.light ? 0xFF17283A : 0xFFF0F5F7;
    MUTED = plan.light ? 0xFF536477 : 0xFF96A6B4;
    GREEN = plan.light ? 0xFF00795D : 0xFF35D6AE;
    RED = plan.light ? 0xFFBF2448 : 0xFFFF7B8B;
    BLUE = plan.light ? 0xFF315FD0 : 0xFF739FFF;
    getWindow().setStatusBarColor(BG);
    getWindow().setNavigationBarColor(BG);
    int flags = plan.light ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0;
    if (android.os.Build.VERSION.SDK_INT >= 27 && plan.light)
      flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    getWindow().getDecorView().setSystemUiVisibility(flags);
  }

  private void preferences() {
    section(
        "Maaş dönemi ve görünüm", "Tüm özetler, bütçeler ve eğilimler maaş gününe göre hesaplanır");
    LinearLayout c = card(body);
    hint(c, "Her ayın " + plan.payday + ". günü başlangıç · Tasarruf hedefi %" + plan.target);
    space(c, 10);
    add(
        c,
        button(
            "Maaş günü ve hedefi değiştir",
            true,
            () -> {
              LinearLayout f = column();
              label(f, "Maaş aldığın gün · 1–31");
              EditText day = field("Örn. 15", Integer.toString(plan.payday), true);
              add(f, day);
              label(f, "Gelirden ayırmak istediğin tasarruf · %0–100");
              EditText target = field("20", Integer.toString(plan.target), true);
              add(f, target);
              hint(
                  f,
                  "Örneğin 15 seçersen dönem 15 Ekim–14 Kasım olur. 29–31 günleri kısa aylarda son"
                      + " güne çekilir. Maaş gününü değiştirmek geçmiş işlemleri değiştirmez; rapor"
                      + " aralıklarını değiştirir.");
              TextView problem = text("", 12, RED, false);
              add(f, problem);
              AlertDialog d = dialog("Maaş dönemim", f, "Kaydet");
              d.setOnShowListener(
                  v ->
                      d.getButton(-1)
                          .setOnClickListener(
                              w -> {
                                try {
                                  Planning updated = ledger.planning();
                                  updated.payday =
                                      Integer.parseInt(day.getText().toString().trim());
                                  updated.target =
                                      Integer.parseInt(target.getText().toString().trim());
                                  ledger.savePlanning(updated);
                                  plan = updated;
                                  month = Finance.cycleMonth(LocalDate.now(), plan.payday);
                                  d.dismiss();
                                  refresh();
                                } catch (Exception e) {
                                  problem.setText(message(e));
                                }
                              }));
              d.show();
            }));
    space(c, 8);
    add(
        c,
        button(
            plan.light ? "Koyu temaya geç" : "Açık temaya geç",
            false,
            () -> {
              try {
                Planning updated = ledger.planning();
                updated.light = !updated.light;
                ledger.savePlanning(updated);
                refresh();
              } catch (Exception e) {
                error("Tema kaydedilemedi", e);
              }
            }));
  }

  private void advice(List<Finance.Entry> list, LocalDate from, LocalDate to, boolean cycle) {
    LinearLayout c = card(body);
    add(c, text("Bütçe asistanının yorumu", 17, BLUE, true));
    space(c, 8);
    hint(
        c,
        "Telefonunda hesaplanır · Kayıt temelli yorumlar ve senaryolar; tahminler kesin sonuç"
            + " değildir.");
    divider(c);
    long income = Finance.total(list, "I"), expense = Finance.total(list, "E");
    int days = to.isBefore(from) ? 0 : (int) ChronoUnit.DAYS.between(from, to) + 1;
    if (days == 0 || (income == 0 && expense == 0)) {
      hint(
          c,
          "Bu dönem için henüz yeterli kayıt yok. Gelirini, kart alışverişlerini ve faturalarını"
              + " ekledikçe yorumlar gelişir.");
      return;
    }
    if (income == 0)
      hint(
          c,
          "Gider kaydı var, gelir kaydı yok. Harcama oranı ve tasarruf hedefi için maaşını"
              + " eklemelisin; gelirin sıfır olduğu varsayımıyla yüzde üretmiyorum.");
    else {
      hint(
          c,
          "Gelirinin "
              + pct(Finance.spendingRate(income, expense))
              + " kadarını harcadın. "
              + (expense > income
                  ? "Giderlerin gelirini " + Finance.money(expense - income) + " aşıyor."
                  : "Şu ana kadarki gelir−gider farkın " + Finance.money(income - expense) + "."));
      space(c, 10);
      long target = Math.round(income * plan.target / 100d);
      hint(
          c,
          "%"
              + plan.target
              + " tasarruf hedefi için "
              + Finance.money(target)
              + " ayırmalısın. "
              + (income - expense >= target
                  ? "Şu an hedefin " + Finance.money(income - expense - target) + " üzerindesin."
                  : "Hedefe göre " + Finance.money(target - (income - expense)) + " açık var."));
    }
    LocalDate oldFrom = cycle ? periodStart(month.minusMonths(1)) : from.minusDays(days);
    LocalDate oldTo = oldFrom.plusDays(days - 1);
    if (cycle && oldTo.isAfter(periodEnd(month.minusMonths(1))))
      oldTo = periodEnd(month.minusMonths(1));
    List<Finance.Entry> old = Finance.period(entries, oldFrom, oldTo);
    long oldIncome = Finance.total(old, "I"), oldExpense = Finance.total(old, "E");
    space(c, 10);
    if (old.isEmpty())
      hint(
          c,
          "Önceki karşılaştırma aralığında kayıt yok; gelir veya gider artışı hakkında sonuç"
              + " çıkarmıyorum.");
    else {
      hint(
          c,
          "Önceki "
              + oldFrom.format(DATE)
              + "–"
              + oldTo.format(DATE)
              + " aralığına göre gider "
              + signedPct(Finance.change(expense, oldExpense))
              + ", gelir "
              + signedPct(Finance.change(income, oldIncome))
              + ". Maaşın henüz yatmamışsa gelir değişimi ödeme zamanından kaynaklanabilir.");
      Map<Long, Long> before = Finance.byCategory(old, "E"), now = Finance.byCategory(list, "E");
      long biggestDelta = 0, biggestId = 0;
      for (Map.Entry<Long, Long> x : now.entrySet()) {
        long delta = x.getValue() - before.getOrDefault(x.getKey(), 0L);
        if (delta > biggestDelta) {
          biggestDelta = delta;
          biggestId = x.getKey();
        }
      }
      if (biggestDelta > 0) {
        space(c, 10);
        hint(
            c,
            "Gider artışına en çok katkı yapan kategori "
                + category(biggestId)
                + ": "
                + Finance.money(biggestDelta)
                + " ek harcama. Bu kategorideki tek seferlik alışverişleri kontrol edebilirsin.");
      }
    }
    Map<Long, Long> categories = Finance.byCategory(list, "E");
    if (!categories.isEmpty()) {
      Map.Entry<Long, Long> top = categories.entrySet().iterator().next();
      space(c, 10);
      hint(
          c,
          "En büyük harcama alanın "
              + category(top.getKey())
              + ". Burada %10 azaltma senaryosu "
              + Finance.money(Math.round(top.getValue() * .1))
              + " bırakır; zorunlu giderlerinde uygulanabilir olmayabilir.");
    }
    if (cycle && month.equals(Finance.cycleMonth(LocalDate.now(), plan.payday))) {
      divider(c);
      int remaining = (int) ChronoUnit.DAYS.between(LocalDate.now(), periodEnd(month));
      long pending = 0, paidBills = 0;
      Set<Long> periodIds = new HashSet<>();
      for (Finance.Entry e : list) periodIds.add(e.id);
      for (Planning.Bill b : plan.bills)
        if (b.paid != 0 && periodIds.contains(b.paid))
          paidBills = Math.addExact(paidBills, b.amount);
      for (Planning.Bill b : plan.bills)
        if (b.paid == 0 && !b.due.isAfter(periodEnd(month)))
          pending = Math.addExact(pending, b.amount);
      long estimate =
          Finance.obligationForecast(
              expense, paidBills, pending, month, plan.payday, LocalDate.now());
      keyValue(
          c, "Bekleyen / gecikmiş faturalar", Finance.money(pending), pending > 0 ? RED : WHITE);
      keyValue(c, "Dönem sonu gider senaryosu", Finance.money(estimate), WHITE);
      hint(
          c,
          "Ödenmiş takipli faturalar günlük ortalamadan çıkarılır. Diğer giderlerin günlük hızı"
              + " dönem sonuna yayılır; ödenmiş faturalar bir kez, dönem sonuna kadar"
              + " bekleyen/gecikmiş faturalar ayrıca eklenir. Kart borcu ödeme transferleri bu"
              + " gider senaryosuna eklenmez.");
      space(c, 10);
      if (days < 7)
        hint(
            c,
            "Henüz "
                + days
                + " gün geçti. Kısa geçmiş ve maaş günündeki toplu ödemeler tahmini kolayca"
                + " değiştirebilir.");
      if (income > 0) {
        space(c, 10);
        hint(
            c,
            estimate > income
                ? "Bu hız sürerse kaydettiğin gelire göre dönem sonunda yaklaşık "
                    + Finance.money(estimate - income)
                    + " açık oluşabilir. Yeni gelir, tek seferlik giderler veya harcama azaltma"
                    + " sonucu değiştirebilir."
                : "Bu senaryoda kaydettiğin gelirden yaklaşık "
                    + Finance.money(income - estimate)
                    + " kalır. Henüz kaydedilmemiş gelir ve ödemeler dahil değildir.");
        long available = income - expense - pending - Math.round(income * plan.target / 100d);
        if (remaining > 0)
          keyValue(
              c,
              "Yarından itibaren günlük hedef payı",
              Finance.money(Math.max(0, available) / remaining),
              available < 0 ? RED : GREEN);
        if (available < 0)
          hint(
              c,
              "Tasarruf hedefi ve bekleyen faturalar birlikte düşünüldüğünde serbest harcama payı"
                  + " kalmıyor. Hedefi veya isteğe bağlı harcamalarını gözden geçirebilirsin.");
      }
    }
    space(c, 10);
    hint(
        c,
        "Yalnızca girdiğin veriler değerlendirilir. Banka ekstresi, faiz, kur veya yatırım getirisi"
            + " tahmin edilmez. Fatura ve düzenli gider olarak aynı ödemeyi iki yerde tanımlama.");
  }

  private void obligations(LinearLayout parent, boolean full) {
    if (!full && plan.cards.isEmpty() && plan.bills.isEmpty()) {
      LinearLayout c = card(parent);
      hint(c, "Kart tarihlerini ve ödenecek faturalarını ekle; yaklaşan ödemelerini burada gör.");
      add(
          c,
          button(
              "Kart ve fatura takibini ayarla",
              false,
              () -> {
                page = 4;
                render();
              }));
      return;
    }
    section(
        "Kredi kartları ve faturalar",
        "Ödeme takvimi bugüne göre · seçili rapor döneminden bağımsız");
    int shown = 0;
    for (Planning.Card card : plan.cards) {
      if (!full && shown++ >= 2) break;
      LinearLayout c = card(parent);
      Finance.Account a = null;
      for (Finance.Account x : accounts) if (x.id == card.account) a = x;
      if (a == null) continue;
      LocalDate close = Finance.cycleStart(YearMonth.now(), card.closing);
      if (close.isAfter(LocalDate.now()))
        close = Finance.cycleStart(YearMonth.now().minusMonths(1), card.closing);
      LocalDate due = Finance.cardDue(close, card.due);
      long statement = Math.max(0, -Finance.balance(a, entries, close)), payments = 0;
      for (Finance.Entry e : entries)
        if (e.kind.equals("T")
            && e.targetId == a.id
            && e.date.isAfter(close)
            && !e.date.isAfter(LocalDate.now())) payments += e.amount;
      long outstanding = Math.max(0, statement - payments);
      add(c, text(a.name, 16, BLUE, true));
      space(c, 8);
      keyValue(
          c,
          "Güncel net borç",
          Finance.money(Math.max(0, -Finance.balance(a, entries, LocalDate.now()))),
          RED);
      hint(
          c,
          "Son hesap kesimi: "
              + close.format(DATE)
              + " · Son ödeme: "
              + due.format(DATE)
              + "\nSonraki kesim: "
              + Finance.nextMonthly(close, card.closing).format(DATE));
      space(c, 8);
      hint(
          c,
          "Kayıtlara göre önceki ekstre kalan borç tahmini: "
              + Finance.money(outstanding)
              + (outstanding > 0 && due.isBefore(LocalDate.now())
                  ? " · Vade geçmiş görünüyor; banka ekstreni kontrol et."
                  : outstanding > 0
                      ? " · Ödeme için "
                          + Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), due))
                          + " gün var."
                      : " · Kayıtlarda önceki kesime ait borç görünmüyor."));
      hint(
          c,
          "Kesim günündeki bakiye ve sonrasındaki ödeme transferlerinden hesaplanır. Bankanın"
              + " gerçek ekstresi, iade/faiz ve tatil değişiklikleri farklı olabilir.");
      if (full) {
        add(c, button("Kart borcu öde · transfer", true, () -> cardPayment(card)));
        space(c, 6);
        add(c, button("Kart tarihlerini düzenle", false, () -> cardDialog(card)));
      }
    }
    List<Planning.Bill> sorted = new ArrayList<>(plan.bills);
    sorted.sort(Comparator.comparing((Planning.Bill b) -> b.paid != 0).thenComparing(b -> b.due));
    shown = 0;
    for (Planning.Bill b : sorted) {
      if (!full && (b.paid != 0 || shown++ >= 3)) continue;
      LinearLayout c = card(parent);
      add(c, text(b.name + " · " + Finance.money(b.amount), 16, b.paid != 0 ? GREEN : RED, true));
      space(c, 8);
      hint(
          c,
          (b.paid != 0
                  ? "Ödendi"
                  : b.due.isBefore(LocalDate.now())
                      ? "Gecikmiş"
                      : b.due.equals(LocalDate.now()) ? "Bugün ödenecek" : "Bekliyor")
              + " · "
              + b.due.format(DATE)
              + " · "
              + account(b.account)
              + (b.monthly ? " · Aylık tekrar" : ""));
      if (full && b.paid == 0) {
        add(
            c,
            button(
                "Ödemeyi kaydet",
                true,
                () ->
                    chooseDate(
                        LocalDate.now(),
                        date -> {
                          try {
                            ledger.payBill(b.id, date);
                            refresh();
                            toast("Fatura ödendi; gider kaydı eklendi");
                          } catch (Exception e) {
                            error("Ödeme kaydedilemedi", e);
                          }
                        })));
        space(c, 6);
        add(c, button("Faturayı düzenle", false, () -> billDialog(b)));
      } else if (full) {
        add(
            c,
            button(
                "Ödemeyi geri al",
                false,
                () ->
                    confirm(
                        "Ödeme geri alınsın mı?",
                        "Bağlı gider silinir. Oluşmuş sonraki fatura korunur; bu eski fatura"
                            + " yeniden ödeme sonrası tekrar oluşturmaz.",
                        () -> {
                          try {
                            ledger.undoBill(b.id);
                            refresh();
                          } catch (Exception e) {
                            error("Ödeme geri alınamadı", e);
                          }
                        })));
      }
    }
    if (full) {
      add(parent, button("+ Kredi kartı tanımla", true, () -> cardDialog(null)));
      space(parent, 8);
      add(parent, button("+ Fatura ekle", false, () -> billDialog(null)));
      hint(
          parent,
          "Yeni kart için önce Hesaplar bölümünde bir hesap oluştur. Mevcut borcunu negatif açılış"
              + " bakiyesi olarak yaz. Alışveriş giderini kart hesabına, borç ödemesini banka→kart"
              + " transferi olarak kaydet. Faturalar yalnızca ödeme kaydıyla gider olur. Aylık"
              + " faturada ödeme sonrası sonraki fatura aynı tahmini tutarla oluşturulur; tutarını"
              + " yeni fatura geldiğinde düzenle.");
    } else
      add(
          parent,
          button(
              "Tüm ödemeleri yönet",
              false,
              () -> {
                page = 4;
                render();
              }));
  }

  private void cardDialog(Planning.Card existing) {
    LinearLayout f = column();
    label(f, "Kart için ayrı hesap");
    Spinner ac = spinner(accounts);
    add(f, ac);
    if (existing != null)
      for (int i = 0; i < accounts.size(); i++)
        if (accounts.get(i).id == existing.account) ac.setSelection(i);
    label(f, "Hesap kesim günü · 1–31");
    EditText close =
        field("Örn. 10", existing == null ? "" : Integer.toString(existing.closing), true);
    add(f, close);
    label(f, "Son ödeme günü · 1–31");
    EditText due = field("Örn. 20", existing == null ? "" : Integer.toString(existing.due), true);
    add(f, due);
    hint(
        f,
        "Son ödeme, kesimden sonraki ilk seçili gündür; ödeme günü kesim günüyle aynıysa sonraki"
            + " aya alınır. Kısa aylarda son gün kullanılır. Bankanın bildirdiği gerçek tarihler"
            + " esas alınmalı.");
    TextView problem = text("", 12, RED, false);
    add(f, problem);
    AlertDialog d = dialog("Kredi kartı tarihleri", f, "Kaydet");
    if (existing != null)
      d.setButton(
          -3,
          "Takibi kaldır",
          (v, w) -> {
            try {
              Planning updated = ledger.planning();
              updated.cards.removeIf(c -> c.account == existing.account);
              ledger.savePlanning(updated);
              refresh();
            } catch (Exception e) {
              error("Kart kaldırılmadı", e);
            }
          });
    d.setOnShowListener(
        v ->
            d.getButton(-1)
                .setOnClickListener(
                    w -> {
                      try {
                        Planning updated = ledger.planning();
                        long id = ((Finance.Account) ac.getSelectedItem()).id;
                        if (existing != null)
                          updated.cards.removeIf(c -> c.account == existing.account);
                        updated.cards.add(
                            new Planning.Card(
                                id,
                                Integer.parseInt(close.getText().toString().trim()),
                                Integer.parseInt(due.getText().toString().trim())));
                        ledger.savePlanning(updated);
                        d.dismiss();
                        refresh();
                      } catch (Exception e) {
                        problem.setText(message(e));
                      }
                    }));
    d.show();
  }

  private void cardPayment(Planning.Card card) {
    LinearLayout f = column();
    List<Finance.Account> sources = new ArrayList<>();
    for (Finance.Account a : accounts) if (a.id != card.account) sources.add(a);
    if (sources.isEmpty()) {
      toast("Ödeme için ikinci bir hesap oluşturmalısın");
      return;
    }
    label(f, "Ödeme yapılan kaynak hesap");
    Spinner source = spinner(sources);
    add(f, source);
    label(f, "Ödediğin tutar · TL");
    EditText amount = field("", "", true);
    add(f, amount);
    LocalDate[] date = {LocalDate.now()};
    Button dateButton = button(date[0].format(DATE), false, () -> {});
    dateButton.setOnClickListener(
        v ->
            chooseDate(
                date[0],
                x -> {
                  date[0] = x;
                  dateButton.setText(x.format(DATE));
                }));
    add(f, dateButton);
    hint(
        f,
        "Ödeme, kaynak hesaptan kart hesabına transfer olur. Gelir veya gider yüzdelerine yeniden"
            + " eklenmez.");
    TextView problem = text("", 12, RED, false);
    add(f, problem);
    AlertDialog d = dialog("Kart borcu ödemesi", f, "Kaydet");
    d.setOnShowListener(
        v ->
            d.getButton(-1)
                .setOnClickListener(
                    w -> {
                      try {
                        if (date[0].isAfter(LocalDate.now()))
                          throw new IllegalArgumentException("Ödeme tarihi gelecekte olamaz.");
                        ledger.saveEntry(
                            new Finance.Entry(
                                0,
                                "T",
                                Finance.parseMoney(amount.getText().toString()),
                                date[0],
                                ((Finance.Account) source.getSelectedItem()).id,
                                card.account,
                                0,
                                "Kart borcu ödemesi"));
                        d.dismiss();
                        refresh();
                      } catch (Exception e) {
                        problem.setText(message(e));
                      }
                    }));
    d.show();
  }

  private void billDialog(Planning.Bill existing) {
    LinearLayout f = column();
    label(f, "Fatura / ödeme adı");
    EditText name = field("Örn. Elektrik", existing == null ? "" : existing.name, false);
    add(f, name);
    label(f, "Tutar · TL");
    EditText amount = field("", existing == null ? "" : Finance.editable(existing.amount), true);
    add(f, amount);
    label(f, "Ödeme hesabı");
    Spinner ac = spinner(accounts);
    add(f, ac);
    List<Finance.Category> cats = ledger.categories("E");
    label(f, "Gider kategorisi");
    Spinner cat = spinner(cats);
    add(f, cat);
    if (existing != null) {
      for (int i = 0; i < accounts.size(); i++)
        if (accounts.get(i).id == existing.account) ac.setSelection(i);
      for (int i = 0; i < cats.size(); i++)
        if (cats.get(i).id == existing.category) cat.setSelection(i);
    } else
      for (int i = 0; i < cats.size(); i++)
        if (cats.get(i).name.equals("Faturalar")) cat.setSelection(i);
    LocalDate[] date = {existing == null ? LocalDate.now() : existing.due};
    label(f, "Son ödeme tarihi");
    Button due = button(date[0].format(DATE), false, () -> {});
    due.setOnClickListener(
        v ->
            chooseDate(
                date[0],
                x -> {
                  date[0] = x;
                  due.setText(x.format(DATE));
                }));
    add(f, due);
    CheckBox monthly = new CheckBox(this);
    monthly.setText(R.string.bill_repeat);
    monthly.setTextColor(WHITE);
    monthly.setChecked(existing != null && existing.monthly);
    add(f, monthly);
    TextView problem = text("", 12, RED, false);
    add(f, problem);
    AlertDialog d = dialog(existing == null ? "Yeni fatura" : "Faturayı düzenle", f, "Kaydet");
    if (existing != null)
      d.setButton(
          -3,
          "Sil",
          (v, w) -> {
            try {
              Planning updated = ledger.planning();
              updated.bills.removeIf(b -> b.id == existing.id);
              ledger.savePlanning(updated);
              refresh();
            } catch (Exception e) {
              error("Fatura silinemedi", e);
            }
          });
    d.setOnShowListener(
        v ->
            d.getButton(-1)
                .setOnClickListener(
                    w -> {
                      try {
                        Planning updated = ledger.planning();
                        Planning.Bill b = new Planning.Bill();
                        b.id = existing == null ? updated.nextBillId() : existing.id;
                        b.name = name.getText().toString().trim();
                        b.amount = Finance.parseMoney(amount.getText().toString());
                        b.account = ((Finance.Account) ac.getSelectedItem()).id;
                        b.category = ((Finance.Category) cat.getSelectedItem()).id;
                        b.due = date[0];
                        b.day =
                            existing != null && existing.due.equals(date[0])
                                ? existing.day
                                : date[0].getDayOfMonth();
                        b.monthly = monthly.isChecked();
                        updated.bills.removeIf(x -> x.id == b.id);
                        updated.bills.add(b);
                        ledger.savePlanning(updated);
                        d.dismiss();
                        refresh();
                      } catch (Exception e) {
                        problem.setText(message(e));
                      }
                    }));
    d.show();
  }

  private EditText field(String placeholder, String value, boolean numeric) {
    EditText e = new EditText(this);
    e.setTextColor(WHITE);
    e.setHintTextColor(MUTED);
    e.setTextSize(15);
    e.setHint(placeholder);
    e.setText(value);
    e.setPadding(dp(12), dp(12), dp(12), dp(12));
    e.setBackgroundTintList(ColorStateList.valueOf(LINE));
    if (numeric) {
      e.setInputType(
          android.text.InputType.TYPE_CLASS_NUMBER
              | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
              | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
      e.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789,.- "));
    } else
      e.setInputType(
          android.text.InputType.TYPE_CLASS_TEXT
              | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    return e;
  }

  private <T> Spinner spinner(List<T> values) {
    Spinner s = new Spinner(this);
    ArrayAdapter<T> adapter =
        new ArrayAdapter<T>(this, android.R.layout.simple_spinner_item, values) {
          @Override
          public View getView(int pos, View recycled, android.view.ViewGroup parent) {
            TextView t = (TextView) super.getView(pos, recycled, parent);
            t.setTextColor(WHITE);
            t.setTextSize(14);
            t.setPadding(dp(8), dp(13), dp(8), dp(13));
            return t;
          }

          @Override
          public View getDropDownView(int pos, View recycled, android.view.ViewGroup parent) {
            TextView t = (TextView) super.getDropDownView(pos, recycled, parent);
            t.setTextColor(WHITE);
            t.setBackgroundColor(CARD);
            t.setTextSize(14);
            t.setPadding(dp(14), dp(16), dp(14), dp(16));
            return t;
          }
        };
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    s.setAdapter(adapter);
    s.setBackgroundTintList(ColorStateList.valueOf(MUTED));
    return s;
  }

  private void label(LinearLayout form, String label) {
    space(form, 12);
    add(form, text(label, 12, MUTED, true));
    space(form, 5);
  }

  private AlertDialog dialog(String title, LinearLayout form, String positive) {
    ScrollView scroll = new ScrollView(this);
    form.setPadding(dp(20), dp(4), dp(20), dp(12));
    scroll.addView(form);
    return new AlertDialog.Builder(
            this,
            (plan.light
                ? android.R.style.Theme_Material_Light_Dialog_Alert
                : android.R.style.Theme_Material_Dialog_Alert))
        .setTitle(title)
        .setView(scroll)
        .setNegativeButton("Vazgeç", null)
        .setPositiveButton(positive, null)
        .create();
  }

  private interface DateSelection {
    void set(LocalDate d);
  }

  private void chooseDate(LocalDate initial, DateSelection selected) {
    DatePickerDialog d =
        new DatePickerDialog(
            this,
            (v, y, m, day) -> selected.set(LocalDate.of(y, m + 1, day)),
            initial.getYear(),
            initial.getMonthValue() - 1,
            initial.getDayOfMonth());
    d.getDatePicker().setMinDate(epoch(LocalDate.of(2000, 1, 1)));
    d.getDatePicker().setMaxDate(epoch(LocalDate.of(2100, 12, 31)));
    d.show();
  }

  private LocalDate newEntryDate() {
    return month.equals(Finance.cycleMonth(LocalDate.now(), plan.payday))
        ? LocalDate.now()
        : periodStart(month);
  }

  private void entryDialog(Finance.Entry existing, String initialKind) {
    if (existing != null)
      for (Planning.Bill b : plan.bills)
        if (b.paid == existing.id) {
          toast("Fatura kaydı: değişiklik için Ayarlar bölümünde ödemeyi geri al");
          return;
        }
    LinearLayout form = column();
    label(form, "İşlem türü");
    Spinner type = spinner(Arrays.asList("Gider", "Gelir", "Transfer"));
    type.setSelection(initialKind.equals("I") ? 1 : initialKind.equals("T") ? 2 : 0);
    add(form, type);
    label(form, "Tutar · TL");
    EditText amount =
        field("Örn. 1250,50", existing == null ? "" : Finance.editable(existing.amount), true);
    add(form, amount);
    label(form, "Tarih");
    LocalDate[] date = {existing == null ? newEntryDate() : existing.date};
    Button dateButton = button(date[0].format(DATE), false, () -> {});
    dateButton.setOnClickListener(
        v ->
            chooseDate(
                date[0],
                d -> {
                  date[0] = d;
                  dateButton.setText(d.format(DATE));
                }));
    add(form, dateButton);
    label(form, "Hesap / kaynak hesap");
    Spinner source = spinner(accounts);
    add(form, source);
    if (existing != null)
      for (int i = 0; i < accounts.size(); i++)
        if (accounts.get(i).id == existing.accountId) source.setSelection(i);
    TextView destinationLabel = text("Hedef hesap", 12, MUTED, true);
    space(form, 12);
    add(form, destinationLabel);
    Spinner destination = spinner(accounts);
    if (accounts.size() > 1) destination.setSelection(1);
    if (existing != null)
      for (int i = 0; i < accounts.size(); i++)
        if (accounts.get(i).id == existing.targetId) destination.setSelection(i);
    add(form, destination);
    TextView categoryLabel = text("Kategori", 12, MUTED, true);
    space(form, 12);
    add(form, categoryLabel);
    Spinner categorySpinner = spinner(ledger.categories(initialKind.equals("I") ? "I" : "E"));
    add(form, categorySpinner);
    Runnable updateType =
        () -> {
          boolean transfer = type.getSelectedItemPosition() == 2;
          destination.setVisibility(transfer ? View.VISIBLE : View.GONE);
          destinationLabel.setVisibility(transfer ? View.VISIBLE : View.GONE);
          categorySpinner.setVisibility(transfer ? View.GONE : View.VISIBLE);
          categoryLabel.setVisibility(transfer ? View.GONE : View.VISIBLE);
          List<Finance.Category> values =
              ledger.categories(type.getSelectedItemPosition() == 1 ? "I" : "E");
          Spinner fresh = spinner(values);
          categorySpinner.setAdapter(fresh.getAdapter());
          if (existing != null)
            for (int i = 0; i < values.size(); i++)
              if (values.get(i).id == existing.categoryId) categorySpinner.setSelection(i);
        };
    type.setOnItemSelectedListener(selected(pos -> updateType.run()));
    updateType.run();
    label(form, "Not · isteğe bağlı");
    EditText note = field("Bu işlem ne içindi?", existing == null ? "" : existing.note, false);
    note.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1000)});
    add(form, note);
    TextView validation = text("", 12, RED, false);
    space(form, 10);
    add(form, validation);
    AlertDialog d = dialog(existing == null ? "Yeni işlem" : "İşlemi düzenle", form, "Kaydet");
    if (existing != null)
      d.setButton(
          AlertDialog.BUTTON_NEUTRAL,
          "Sil",
          (a, b) ->
              confirm(
                  "İşlem silinsin mi?",
                  "Bu kayıt silinecek ve bakiyeler yeniden hesaplanacak. Düzenli işlemin kuralı"
                      + " varsa devam eder.",
                  () -> {
                    ledger.deleteEntry(existing.id);
                    refresh();
                  }));
    d.setOnShowListener(
        v ->
            d.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    b -> {
                      try {
                        String kind =
                            type.getSelectedItemPosition() == 0
                                ? "E"
                                : type.getSelectedItemPosition() == 1 ? "I" : "T";
                        Finance.Entry e =
                            new Finance.Entry(
                                existing == null ? 0 : existing.id,
                                kind,
                                Finance.parseMoney(amount.getText().toString()),
                                date[0],
                                ((Finance.Account) source.getSelectedItem()).id,
                                kind.equals("T")
                                    ? ((Finance.Account) destination.getSelectedItem()).id
                                    : 0,
                                kind.equals("T")
                                    ? 0
                                    : ((Finance.Category) categorySpinner.getSelectedItem()).id,
                                note.getText().toString().trim());
                        ledger.saveEntry(e);
                        d.dismiss();
                        refresh();
                        toast("İşlem kaydedildi");
                      } catch (Exception ex) {
                        validation.setText(message(ex));
                      }
                    }));
    d.show();
  }

  private void accountDialog(Finance.Account existing) {
    LinearLayout form = column();
    label(form, "Hesap adı");
    EditText name = field("Örn. Maaş hesabı", existing == null ? "" : existing.name, false);
    name.setFilters(new InputFilter[] {new InputFilter.LengthFilter(60)});
    add(form, name);
    label(form, "Açılış bakiyesi · TL");
    EditText opening =
        field("0,00", existing == null ? "0,00" : Finance.editable(existing.opening), true);
    add(form, opening);
    space(form, 10);
    hint(
        form,
        "Takibe başladığın ilk bakiye. Gelir analizine dahil olmaz. Borç için negatif tutar"
            + " girebilirsin.");
    TextView error = text("", 12, RED, false);
    add(form, error);
    AlertDialog d = dialog(existing == null ? "Yeni hesap" : "Hesabı düzenle", form, "Kaydet");
    d.setOnShowListener(
        v ->
            d.getButton(-1)
                .setOnClickListener(
                    b -> {
                      try {
                        ledger.saveAccount(
                            existing == null ? 0 : existing.id,
                            name.getText().toString(),
                            Finance.parseMoney(opening.getText().toString()));
                        d.dismiss();
                        refresh();
                      } catch (Exception e) {
                        error.setText(message(e));
                      }
                    }));
    d.show();
  }

  private void categoryManager() {
    LinearLayout form = column();
    add(form, button("+ Gider kategorisi", true, () -> categoryDialog(null, "E")));
    space(form, 8);
    add(form, button("+ Gelir kategorisi", false, () -> categoryDialog(null, "I")));
    for (Finance.Category c : categories) {
      space(form, 8);
      add(
          form,
          button(
              c.name + " · " + (c.kind.equals("I") ? "Gelir" : "Gider"),
              false,
              () -> categoryDialog(c, c.kind)));
    }
    AlertDialog d = dialog("Kategoriler", form, "Kapat");
    d.setOnShowListener(v -> d.getButton(-1).setOnClickListener(b -> d.dismiss()));
    d.show();
  }

  private void categoryDialog(Finance.Category existing, String kind) {
    LinearLayout form = column();
    label(form, (kind.equals("I") ? "Gelir" : "Gider") + " kategorisi adı");
    EditText name = field("Kategori adı", existing == null ? "" : existing.name, false);
    add(form, name);
    TextView error = text("", 12, RED, false);
    add(form, error);
    AlertDialog d =
        dialog(existing == null ? "Yeni kategori" : "Kategoriyi düzenle", form, "Kaydet");
    d.setOnShowListener(
        v ->
            d.getButton(-1)
                .setOnClickListener(
                    b -> {
                      try {
                        ledger.saveCategory(
                            existing == null ? 0 : existing.id, name.getText().toString(), kind);
                        d.dismiss();
                        refresh();
                        toast("Kategori kaydedildi. Listeyi yeniden açabilirsin.");
                      } catch (Exception e) {
                        error.setText(message(e));
                      }
                    }));
    d.show();
  }

  private void budgetDialog(long categoryId) {
    LinearLayout form = column();
    List<Finance.Category> choices = ledger.categories("E");
    label(form, "Gider kategorisi");
    Spinner cat = spinner(choices);
    for (int i = 0; i < choices.size(); i++)
      if (choices.get(i).id == categoryId) cat.setSelection(i);
    add(form, cat);
    label(form, "Maaş dönemi limiti · TL");
    EditText amount = field("Örn. 5000,00", "", true);
    add(form, amount);
    Runnable update =
        () -> {
          long id = ((Finance.Category) cat.getSelectedItem()).id;
          Long limit = ledger.budgets().get(id);
          amount.setText(limit == null ? "" : Finance.editable(limit));
        };
    cat.setOnItemSelectedListener(selected(pos -> update.run()));
    update.run();
    space(form, 10);
    hint(
        form,
        "Her maaş döneminde aynı limit kullanılır. Limiti kaldırmak için 0 gir. Kayıtların"
            + " silinmez.");
    TextView error = text("", 12, RED, false);
    add(form, error);
    AlertDialog d = dialog("Maaş dönemi bütçesi", form, "Kaydet");
    d.setOnShowListener(
        v ->
            d.getButton(-1)
                .setOnClickListener(
                    b -> {
                      try {
                        ledger.budget(
                            ((Finance.Category) cat.getSelectedItem()).id,
                            Finance.parseMoney(amount.getText().toString()));
                        d.dismiss();
                        refresh();
                      } catch (Exception e) {
                        error.setText(message(e));
                      }
                    }));
    d.show();
  }

  private void recurringDialog() {
    LinearLayout form = column();
    label(form, "Tür");
    Spinner type = spinner(Arrays.asList("Gider", "Gelir"));
    add(form, type);
    label(form, "Tutar · TL");
    EditText amount = field("Örn. 15000,00", "", true);
    add(form, amount);
    label(form, "Hesap");
    Spinner account = spinner(accounts);
    add(form, account);
    label(form, "Kategori");
    Spinner cat = spinner(ledger.categories("E"));
    add(form, cat);
    type.setOnItemSelectedListener(
        selected(
            pos -> cat.setAdapter(spinner(ledger.categories(pos == 0 ? "E" : "I")).getAdapter())));
    label(form, "İlk kayıt tarihi / her ayın günü");
    LocalDate[] date = {LocalDate.now()};
    Button dateButton = button(date[0].format(DATE), false, () -> {});
    dateButton.setOnClickListener(
        v ->
            chooseDate(
                date[0],
                d -> {
                  date[0] = d;
                  dateButton.setText(d.format(DATE));
                }));
    add(form, dateButton);
    label(form, "Not");
    EditText note = field("Örn. Aylık kira", "", false);
    note.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1000)});
    add(form, note);
    space(form, 10);
    hint(
        form,
        "İlk tarih geçmişteyse o tarihten bugüne vadesi gelen tüm aylık kayıtlar eklenir."
            + " Gelecekteyse ilk vade beklenir.");
    TextView error = text("", 12, RED, false);
    add(form, error);
    AlertDialog d = dialog("Aylık düzenli işlem", form, "Oluştur");
    d.setOnShowListener(
        v ->
            d.getButton(-1)
                .setOnClickListener(
                    b -> {
                      try {
                        Finance.Entry e =
                            new Finance.Entry(
                                0,
                                type.getSelectedItemPosition() == 0 ? "E" : "I",
                                Finance.parseMoney(amount.getText().toString()),
                                date[0],
                                ((Finance.Account) account.getSelectedItem()).id,
                                0,
                                ((Finance.Category) cat.getSelectedItem()).id,
                                note.getText().toString().trim());
                        ledger.addRecurring(e);
                        d.dismiss();
                        refresh();
                        toast("Düzenli işlem oluşturuldu");
                      } catch (Exception ex) {
                        error.setText(message(ex));
                      }
                    }));
    d.show();
  }

  private void confirm(String title, String content, Runnable action) {
    new AlertDialog.Builder(
            this,
            (plan.light
                ? android.R.style.Theme_Material_Light_Dialog_Alert
                : android.R.style.Theme_Material_Dialog_Alert))
        .setTitle(title)
        .setMessage(content)
        .setNegativeButton("Vazgeç", null)
        .setPositiveButton(
            "Onayla",
            (d, w) -> {
              try {
                action.run();
              } catch (Exception e) {
                error("İşlem tamamlanamadı", e);
              }
            })
        .show();
  }

  private void toast(String message) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }

  private String message(Exception e) {
    return e.getMessage() == null
        ? "İşlem tamamlanamadı. Girdi ve dosyayı kontrol et."
        : e.getMessage();
  }

  private void error(String title, Exception e) {
    if (!isFinishing())
      new AlertDialog.Builder(
              this,
              (plan.light
                  ? android.R.style.Theme_Material_Light_Dialog_Alert
                  : android.R.style.Theme_Material_Dialog_Alert))
          .setTitle(title)
          .setMessage(message(e))
          .setPositiveButton("Tamam", null)
          .show();
  }

  private void exportFile(int request) {
    if (busy) {
      toast("Dosya işlemi sürüyor");
      return;
    }
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType(request == EXPORT_JSON ? "application/json" : "text/csv");
    intent.putExtra(
        Intent.EXTRA_TITLE,
        "cep-butcem-" + LocalDate.now() + (request == EXPORT_JSON ? ".json" : ".csv"));
    try {
      startActivityForResult(intent, request);
    } catch (ActivityNotFoundException e) {
      error("Dosya seçici bulunamadı", e);
    }
  }

  private void importFile() {
    if (busy) {
      toast("Dosya işlemi sürüyor");
      return;
    }
    confirm(
        "Yedek geri yükleme",
        "Mevcut kayıtların tamamı yedekteki verilerle değişecek. Önce JSON yedeğini almanı"
            + " öneririz. Dosya seçimine devam edilsin mi?",
        () -> {
          Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
          i.addCategory(Intent.CATEGORY_OPENABLE);
          i.setType("*/*");
          try {
            startActivityForResult(i, IMPORT_JSON);
          } catch (ActivityNotFoundException e) {
            error("Dosya seçici bulunamadı", e);
          }
        });
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK || data == null || data.getData() == null) return;
    android.net.Uri uri = data.getData();
    busy = true;
    toast("Dosya işleniyor…");
    io.execute(
        () -> {
          String success;
          try {
            if (request == IMPORT_JSON) {
              String content;
              try (InputStream stream = getContentResolver().openInputStream(uri)) {
                if (stream == null) throw new IOException("Dosya açılamadı.");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = stream.read(buffer)) != -1) {
                  if (out.size() + n > 16 * 1024 * 1024)
                    throw new IOException("Yedek en çok 16 MB olabilir.");
                  out.write(buffer, 0, n);
                }
                content = out.toString(StandardCharsets.UTF_8.name());
              }
              ledger.restore(content);
              success = "Yedek geri yüklendi";
            } else {
              String content = request == EXPORT_JSON ? ledger.backup() : ledger.csv();
              try (OutputStream stream = getContentResolver().openOutputStream(uri, "wt")) {
                if (stream == null) throw new IOException("Dosya yazılamadı.");
                stream.write(content.getBytes(StandardCharsets.UTF_8));
              }
              success =
                  request == EXPORT_JSON ? "JSON yedeği kaydedildi" : "CSV dosyası kaydedildi";
            }
            fileFinished(success, null);
          } catch (Exception e) {
            fileFinished(null, e);
          }
        });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    io.shutdown(); /* Tasks may still be writing: close only after the queue drains. */
    new Thread(
            () -> {
              try {
                io.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
              if (io.isTerminated()) ledger.close();
            },
            "ledger-close")
        .start();
  }
}
