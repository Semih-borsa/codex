package com.semih.cepbutcem;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
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
      BLUE = 0xFF739FFF,
      ACCENT = 0xFF35D6AE;
  private final ArrayDeque<Integer> backPages = new ArrayDeque<>();
  private static final int EXPORT_JSON = 41, EXPORT_CSV = 42, IMPORT_JSON = 43;
  private static final DateTimeFormatter
      MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Finance.TR),
      SHORT = DateTimeFormatter.ofPattern("d MMM", Finance.TR),
      DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", Finance.TR);
  private Ledger ledger;
  private FeatureStore features;
  private Insights.Forecast forecastData;
  private List<Finance.Entry> goalHistory = new ArrayList<>();
  private FeatureStore.Snapshot undoSnapshot;
  private boolean undoAdded;
  private long undoUntil;
  private Planning plan = new Planning();
  private int upcomingOffset;
  private final int[] featureOffsets = new int[3];
  private List<Finance.Entry> entries = new ArrayList<>(), recent = new ArrayList<>();
  private Map<Long, Long> accountBalances = new HashMap<>();
  private Map<Long, LocalDate> paidDates = new HashMap<>();
  private Map<Long, String> accountNames = new HashMap<>(), categoryNames = new HashMap<>();
  private String loadedKey = "";
  private int loadGeneration = 0;
  private LinearLayout dayPanel;
  private LocalDate selectedDay;
  private int paymentOffset = 0;
  private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());
  private boolean loaded = false;
  private List<Finance.Account> accounts;
  private List<Finance.Category> categories;
  private LinearLayout root, body, navigation;
  private YearMonth month = YearMonth.now();
  private int page = 0, transactionType = 0, analysisMode = 0;
  private long filterAccount = 0, filterCategory = 0;
  private String search = "";
  private boolean categoryIncome = false;
  private boolean advisorExpanded = false;
  private LocalDate customFrom = LocalDate.now().withDayOfMonth(1), customTo = LocalDate.now();
  private final ExecutorService io = Executors.newSingleThreadExecutor();
  private static volatile boolean busy = false;
  private static java.lang.ref.WeakReference<MainActivity> foreground =
      new java.lang.ref.WeakReference<>(null);

  private void fileFinished(String success, Exception failure, boolean restored) {
    busy = false;
    new android.os.Handler(android.os.Looper.getMainLooper())
        .post(
            () -> {
              MainActivity current = foreground.get();
              if (current == null || current.isDestroyed()) return;
              if (restored) {
                current.salaryDraft = null;
                current.salaryRaisesTab = false;
              }
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
    features = new FeatureStore(ledger);
    try {
      plan = ledger.planning();
    } catch (Exception e) {
      error("Ayarlar okunamadı", e);
    }
    month = Finance.cycleMonth(LocalDate.now(), plan.payday);
    if (state != null) {
      month = YearMonth.parse(state.getString("month", month.toString()));
      page = state.getInt("page");
      undoUntil = state.getLong("undoUntil");
      undoAdded = state.getBoolean("undoAdded");
      if (state.containsKey("undoSnapshot") && android.os.SystemClock.elapsedRealtime() < undoUntil)
        try {
          undoSnapshot = new FeatureStore.Snapshot(state.getString("undoSnapshot"));
          long token = undoUntil;
          ui.postDelayed(
              () -> {
                if (!isDestroyed() && undoUntil == token) {
                  undoSnapshot = null;
                  render();
                }
              },
              Math.max(0, undoUntil - android.os.SystemClock.elapsedRealtime()));
        } catch (JSONException ignored) {
        }
      int[] history = state.getIntArray("backPages");
      if (history != null) for (int previous : history) backPages.addLast(previous);
      search = state.getString("search", "");
      transactionType = state.getInt("transactionType");
      analysisMode = state.getInt("analysisMode");
      filterAccount = state.getLong("filterAccount");
      filterCategory = state.getLong("filterCategory");
      categoryIncome = state.getBoolean("categoryIncome");
      advisorExpanded = state.getBoolean("advisorExpanded");
      if (state.containsKey("selectedDay"))
        selectedDay = LocalDate.parse(state.getString("selectedDay"));
      customFrom = LocalDate.parse(state.getString("customFrom", customFrom.toString()));
      customTo = LocalDate.parse(state.getString("customTo", customTo.toString()));
    }
    if (android.os.Build.VERSION.SDK_INT >= 33)
      getOnBackInvokedDispatcher()
          .registerOnBackInvokedCallback(
              android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::navigateBack);
    if (getIntent().getBooleanExtra("payments", false)) page = 5;
    refresh();
  }

  private void navigateBack() {
    if (page >= 5) {
      page = backPages.isEmpty() ? (page == 7 || page == 8 ? 4 : 0) : backPages.removeLast();
      render();
    } else finish();
  }

  private void openPage(int target) {
    if (target == page) return;
    if (backPages.size() >= 16) backPages.removeFirst();
    backPages.addLast(page);
    page = target;
    render();
  }

  @Override
  public void onBackPressed() {
    navigateBack();
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
    if (undoSnapshot != null) {
      state.putString("undoSnapshot", undoSnapshot.json());
      state.putLong("undoUntil", undoUntil);
      state.putBoolean("undoAdded", undoAdded);
    }
    state.putIntArray("backPages", backPages.stream().mapToInt(Integer::intValue).toArray());
    state.putString("search", search);
    state.putInt("transactionType", transactionType);
    state.putInt("analysisMode", analysisMode);
    state.putLong("filterAccount", filterAccount);
    state.putLong("filterCategory", filterCategory);
    state.putBoolean("categoryIncome", categoryIncome);
    state.putBoolean("advisorExpanded", advisorExpanded);
    if (selectedDay != null) state.putString("selectedDay", selectedDay.toString());
    state.putString("customFrom", customFrom.toString());
    state.putString("customTo", customTo.toString());
  }

  private String dataKey() {
    return month + "/" + analysisMode + "/" + customFrom + "/" + customTo + "/" + plan.payday;
  }

  private void refresh() {
    if (isDestroyed() || io.isShutdown()) return;
    int generation = ++loadGeneration;
    YearMonth selected = month;
    int mode = analysisMode;
    LocalDate cFrom = customFrom, cTo = customTo;
    if (!loaded) {
      TextView loading = text("Bütçen hazırlanıyor…", 18, WHITE, true);
      loading.setGravity(Gravity.CENTER);
      loading.setBackgroundColor(BG);
      setContentView(loading);
    }
    io.execute(
        () -> {
          try {
            ledger.processRecurring(LocalDate.now());
            Planning nextPlan = ledger.planning();
            LocalDate from = Finance.cycleStart(selected.minusMonths(6), nextPlan.payday),
                to = Finance.cycleEnd(selected, nextPlan.payday);
            if (mode == 2) {
              from = Finance.cycleStart(YearMonth.of(selected.getYear() - 1, 1), nextPlan.payday);
              to = Finance.cycleEnd(YearMonth.of(selected.getYear(), 12), nextPlan.payday);
            } else if (mode == 3) {
              long days = ChronoUnit.DAYS.between(cFrom, cTo) + 1;
              LocalDate prev = cFrom.minusDays(days);
              LocalDate chart =
                  Finance.cycleStart(YearMonth.from(cTo).minusMonths(5), nextPlan.payday);
              from = Collections.min(Arrays.asList(from, prev, chart));
              to = Collections.max(Arrays.asList(to, cTo));
            }
            LocalDate today = LocalDate.now();
            YearMonth active = Finance.cycleMonth(today, nextPlan.payday);
            List<Finance.Entry> reports = ledger.reportEntries(from, to);
            LocalDate goalFrom = Finance.cycleStart(active.minusMonths(3), nextPlan.payday);
            List<Finance.Entry> goalReports =
                !from.isAfter(goalFrom) && !to.isBefore(today)
                    ? reports
                    : ledger.reportEntries(goalFrom, today);
            Insights.Forecast nextForecast = features.forecast(nextPlan, selected, today, reports);
            List<Finance.Account> nextAccounts = ledger.accounts();
            List<Finance.Category> nextCategories = ledger.categories(null);
            Map<Long, Long> balances = ledger.balances(LocalDate.now());
            Map<Long, LocalDate> dates = ledger.paidDates(nextPlan);
            List<Finance.Entry> nextRecent =
                ledger.recentEntries(
                    Finance.cycleStart(selected, nextPlan.payday),
                    Finance.cycleEnd(selected, nextPlan.payday),
                    8);
            Reminders.check(getApplicationContext());
            ui.post(
                () -> {
                  if (isDestroyed() || generation != loadGeneration) return;
                  plan = nextPlan;
                  forecastData = nextForecast;
                  goalHistory = goalReports;
                  entries = reports;
                  accounts = nextAccounts;
                  categories = nextCategories;
                  accountBalances = balances;
                  paidDates = dates;
                  recent = nextRecent;
                  accountNames.clear();
                  categoryNames.clear();
                  for (Finance.Account x : accounts) accountNames.put(x.id, x.name);
                  for (Finance.Category x : categories) categoryNames.put(x.id, x.name);
                  loaded = true;
                  loadedKey = dataKey();
                  applyPalette();
                  render();
                  Reminders.schedule(this);
                });
          } catch (Exception e) {
            ui.post(
                () -> {
                  if (!isDestroyed() && generation == loadGeneration) error("Veriler açılamadı", e);
                });
          }
        });
  }

  private long balance(Finance.Account a) {
    return accountBalances.getOrDefault(a.id, a.opening);
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
    GradientDrawable background = shape(CARD, 20);
    background.setStroke(dp(1), LINE);
    c.setBackground(background);
    c.setElevation(dp(plan.light ? 1 : 0));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = dp(12);
    parent.addView(c, lp);
    return c;
  }

  private Button button(String label, boolean primary, Runnable action) {
    Button b = new Button(this);
    b.setText(label);
    b.setTextSize(13);
    b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    b.setAllCaps(false);
    b.setMinHeight(dp(48));
    b.setMinimumHeight(dp(48));
    b.setTextColor(primary ? (plan.light ? 0xFFFFFFFF : BG) : WHITE);
    b.setBackgroundTintList(null);
    GradientDrawable surface = shape(primary ? ACCENT : (plan.light ? 0xFFF4E8DE : 0xFF202A3A), 14);
    b.setBackground(
        new RippleDrawable(
            ColorStateList.valueOf(plan.light ? 0x22000000 : 0x33FFFFFF), surface, null));
    b.setElevation(0);
    b.setStateListAnimator(null);
    b.setPadding(dp(14), dp(10), dp(14), dp(10));
    b.setOnClickListener(v -> action.run());
    return b;
  }

  private Button periodArrow(String label, Runnable action) {
    Button b = button(label, false, action);
    b.setTextSize(22);
    b.setBackground(
        new RippleDrawable(ColorStateList.valueOf(tint(GREEN, 22)), shape(CARD, 12), null));
    b.setPadding(0, 0, 0, 0);
    b.setMinHeight(dp(48));
    b.setMinimumHeight(dp(48));
    return b;
  }

  private View quickAction(String label, String symbol, int color, Runnable action) {
    LinearLayout tile = column();
    tile.setGravity(Gravity.CENTER);
    tile.setPadding(dp(4), dp(10), dp(4), dp(10));
    GradientDrawable surface = shape(CARD, 16);
    surface.setStroke(dp(1), LINE);
    tile.setBackground(new RippleDrawable(ColorStateList.valueOf(tint(color, 25)), surface, null));
    tile.addView(icon(symbol, color, 22), new LinearLayout.LayoutParams(dp(22), dp(22)));
    space(tile, 7);
    TextView title = text(label, 11, WHITE, true);
    title.setGravity(Gravity.CENTER);
    add(tile, title);
    tile.setMinimumHeight(dp(64));
    tile.setContentDescription(label);
    tile.setOnClickListener(v -> action.run());
    return tile;
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
    if (!loaded || !loadedKey.equals(dataKey())) {
      refresh();
      return;
    }
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
    header.setPadding(dp(20), dp(10), dp(20), dp(10));
    LinearLayout branding = column();
    add(branding, text("CEP BÜTÇEM", 10, MUTED, true));
    space(branding, 5);
    String[] names = {
      "Genel bakış",
      "İşlemler",
      "Analizler",
      "Bütçeler",
      "Hesap & ayarlar",
      "Ödeme takvimi",
      "Bütçe asistanı",
      "Kredilerim",
      "Maaş hesaplayıcı",
      "İşlem şablonları",
      "Birikim hedefleri",
      "Kart alışveriş taksitleri"
    };
    add(branding, text(names[page], 22, WHITE, true));
    weighted(header, branding);
    View avatar = icon(page >= 5 ? "back" : "settings", page >= 5 ? WHITE : ACCENT, 22);
    LinearLayout avatarHolder = row();
    avatarHolder.setGravity(Gravity.CENTER);
    avatarHolder.setBackground(shape(CARD, 16));
    avatarHolder.addView(avatar, new LinearLayout.LayoutParams(dp(22), dp(22)));
    header.addView(avatarHolder, new LinearLayout.LayoutParams(dp(44), dp(44)));
    avatarHolder.setContentDescription(
        page >= 5 ? "Önceki sayfaya dön" : "Cep Bütçem; ayarları aç");
    avatarHolder.setOnClickListener(
        v -> {
          if (page >= 5) navigateBack();
          else {
            backPages.clear();
            page = 4;
            render();
          }
        });
    add(root, header);
    if (page != 4 && page != 5 && page != 8 && page < 9) {
      LinearLayout period = row();
      period.setPadding(dp(4), 0, dp(4), 0);
      period.setBackground(shape(CARD, 14));
      Button prev = periodArrow("‹", () -> changeMonth(-1));
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
      Button next = periodArrow("›", () -> changeMonth(1));
      next.setContentDescription("Sonraki maaş dönemi");
      period.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48)));
      LinearLayout.LayoutParams periodParams = new LinearLayout.LayoutParams(-1, -2);
      periodParams.setMargins(dp(20), 0, dp(20), dp(6));
      root.addView(period, periodParams);
    }
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setVerticalScrollBarEnabled(false);
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
      case 5:
        paymentPage();
        break;
      case 6:
        topicAdvice();
        advice(current(), periodStart(month), effectiveEnd(periodEnd(month)), true);
        break;
      case 7:
        loansPage();
        break;
      case 8:
        salaryPage();
        break;
      case 9:
        templatesPage();
        break;
      case 10:
        goalsPage();
        break;
      case 11:
        purchasesPage();
        break;
    }
    undoBanner();
    navigation = row();
    navigation.setPadding(dp(5), dp(5), dp(5), dp(5));
    GradientDrawable navSurface = shape(CARD, 24);
    navSurface.setStroke(dp(1), LINE);
    navigation.setBackground(navSurface);
    navigation.setElevation(dp(6));
    int selectedTab = page < 5 ? page : page == 6 ? 2 : page == 5 ? 0 : 4;
    String[] icons = {"home", "list", "chart", "target", "settings"},
        labels = {"Özet", "İşlemler", "Analiz", "Bütçe", "Ayarlar"};
    for (int i = 0; i < 5; i++) {
      final int tab = i;
      LinearLayout item = column();
      item.setGravity(Gravity.CENTER);
      item.setPadding(0, dp(5), 0, dp(5));
      View tabIcon = icon(icons[i], i == selectedTab ? ACCENT : MUTED, 22);
      item.addView(tabIcon, new LinearLayout.LayoutParams(dp(22), dp(22)));
      space(item, 4);
      TextView label = text(labels[i], 10, i == selectedTab ? ACCENT : MUTED, i == selectedTab);
      label.setGravity(Gravity.CENTER);
      add(item, label);
      if (i == selectedTab) item.setBackground(shape(plan.light ? 0xFFF0DED2 : 0xFF183B35, 16));
      item.setSelected(i == selectedTab);
      item.setContentDescription(labels[i]);
      item.setOnClickListener(
          v -> {
            backPages.clear();
            page = tab;
            refresh();
          });
      navigation.addView(item, new LinearLayout.LayoutParams(0, dp(54), 1));
    }
    LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(-1, -2);
    navParams.setMargins(dp(14), dp(4), dp(14), dp(10));
    root.addView(navigation, navParams);
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
    return categoryNames.getOrDefault(id, "Diğer");
  }

  private String account(long id) {
    return accountNames.getOrDefault(id, "Hesap");
  }

  private void pickMonth() {
    DatePickerDialog d =
        new DatePickerDialog(
            this,
            plan.light ? R.style.LightDialog : R.style.DarkDialog,
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
    for (Finance.Account a : accounts) balance = Math.addExact(balance, balance(a));
    double rate = Finance.spendingRate(income, expense);
    LinearLayout hero = card(body);
    hero.setBackground(new Design.BalanceSurface(Finance.spendingBand(rate), dp(24), plan.light));
    LinearLayout top = row();
    weighted(top, text("TOPLAM NET VARLIK", 10, 0xFFDCF4EF, true));
    top.addView(chip(spendingStatus(rate), 0xFFFFFFFF, 0x33FFFFFF));
    add(hero, top);
    space(hero, 10);
    TextView amount = text(Finance.money(balance), 34, 0xFFFFFFFF, true);
    amount.setMaxLines(1);
    amount.setAutoSizeTextTypeUniformWithConfiguration(
        18, 34, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
    amount.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
    add(hero, amount);
    space(hero, 6);
    add(hero, text("Tüm hesapların · bugün", 11, 0xFFFAF3FF, false));
    space(hero, 18);
    LinearLayout budget = column();
    budget.setPadding(dp(12), dp(12), dp(12), dp(12));
    if (plan.budgetNeedle) {
      budget.setBackground(shape(plan.light ? 0xFFFFFDFA : 0xFF121A2E, 16));
      add(budget, text("Dönem bütçem", 16, WHITE, true));
      space(budget, 12);
      add(budget, new Design.BudgetGauge(this, rate, plan.light));
      LinearLayout budgetRow = row();
      LinearLayout incomeFacts = column(), expenseFacts = column();
      add(incomeFacts, text("Gelir", 11, MUTED, false));
      add(incomeFacts, text(Finance.money(income), 17, GREEN, true));
      add(expenseFacts, text("Gider", 11, MUTED, false));
      add(expenseFacts, text(Finance.money(expense), 17, RED, true));
      weighted(budgetRow, incomeFacts);
      weighted(budgetRow, expenseFacts);
      add(budget, budgetRow);
      space(budget, 8);
      LinearLayout legend = row();
      weighted(legend, text("● %0–50", 10, 0xFF10B981, false));
      weighted(legend, text("● %50–80", 10, 0xFFF59E0B, false));
      weighted(legend, text("● %80 ve üzeri", 10, 0xFFEF4444, false));
      add(budget, legend);
      space(budget, 8);
      add(budget, text(Finance.money(income - expense) + " dönem dengesi", 12, WHITE, true));
      add(budget, text("Yalnızca gider / gelir · transferler hariç", 10, MUTED, false));
    } else {
      budget.setBackground(shape(0x25000000, 16));
      LinearLayout br = row();
      br.setGravity(Gravity.CENTER_VERTICAL);
      br.addView(
          new Design.BudgetCircle(this, rate, plan.light),
          new LinearLayout.LayoutParams(dp(112), dp(112)));
      gap(br, 12);
      LinearLayout facts = column();
      add(facts, text("Dönem bütçem", 16, 0xFFFFFFFF, true));
      space(facts, 8);
      add(
          facts,
          text(Finance.money(expense) + " / " + Finance.money(income), 12, 0xFFFFFFFF, false));
      space(facts, 6);
      add(facts, text(Finance.money(income - expense) + " dönem dengesi", 12, 0xFFFFFFFF, true));
      space(facts, 6);
      add(
          facts,
          text(
              Double.isNaN(rate) ? "Oran için gelir gerekli" : "Gider / gelir · transferler hariç",
              10,
              0xFFEFE4DB,
              false));
      space(facts, 7);
      LinearLayout legend = row();
      weighted(legend, text("● 0–50", 9, 0xFF10B981, false));
      weighted(legend, text("● 50–80", 9, 0xFFF59E0B, false));
      weighted(legend, text("● 80+", 9, 0xFFEF4444, false));
      add(facts, legend);
      weighted(br, facts);
      add(budget, br);
    }
    add(hero, budget);
    heroCardDebts(hero);
    int days = Finance.cycleDays(month, plan.payday, LocalDate.now());
    LocalDate oldStart = periodStart(month.minusMonths(1)),
        oldEnd = oldStart.plusDays(Math.max(0, days - 1));
    if (oldEnd.isAfter(periodEnd(month.minusMonths(1)))) oldEnd = periodEnd(month.minusMonths(1));
    List<Finance.Entry> old =
        days == 0 ? Collections.emptyList() : Finance.period(entries, oldStart, oldEnd);
    LinearLayout actions = row();
    weighted(actions, quickAction("− Gider ekle", "down", RED, () -> entryDialog(null, "E")));
    gap(actions, 8);
    weighted(actions, quickAction("+ Gelir ekle", "up", GREEN, () -> entryDialog(null, "I")));
    gap(actions, 8);
    weighted(actions, quickAction("Transfer", "repeat", BLUE, () -> entryDialog(null, "T")));
    add(body, actions);
    space(body, 14);
    LinearLayout metrics = row();
    weighted(
        metrics,
        metric("Gelir", income, "up", GREEN, Finance.change(income, Finance.total(old, "I"))));
    gap(metrics, 10);
    weighted(
        metrics,
        metric("Gider", expense, "down", RED, Finance.change(expense, Finance.total(old, "E"))));
    add(body, metrics);
    space(body, 12);
    if (!plan.extras.goals.isEmpty()) {
      section("Birikim hedeflerin", "Gerçek hesap bakiyeleri · detay için dokun");
      LinearLayout gc = card(body);
      for (int i = 0; i < Math.min(3, plan.extras.goals.size()); i++) {
        Enhancements.Goal g = plan.extras.goals.get(i);
        keyValue(
            gc,
            g.name,
            Finance.money(accountBalances.getOrDefault(g.account, 0L))
                + " / "
                + Finance.money(g.target),
            GREEN);
      }
      add(gc, button("Hedefleri yönet", false, () -> openPage(10)));
    }
    templateShortcuts();
    quickRepeat(recent);
    creditCardOverview();
    section("Nakit ve banka hesapların", "Bugünün bakiyeleri · detay için dokun");
    HorizontalScrollView wallets = new HorizontalScrollView(this);
    wallets.setHorizontalScrollBarEnabled(false);
    LinearLayout walletRow = row();
    for (Finance.Account a : accounts) {
      if (plan.cardAccounts.contains(a.id)) continue;
      long value = balance(a);
      boolean cardAccount = plan.cards.stream().anyMatch(c -> c.account == a.id);
      LinearLayout wallet = column();
      wallet.setPadding(dp(16), dp(16), dp(16), dp(16));
      wallet.setBackground(shape(CARD, 18));
      LinearLayout walletTop = row();
      walletTop.addView(
          icon(cardAccount ? "card" : "wallet", cardAccount ? BLUE : GREEN, 20),
          new LinearLayout.LayoutParams(dp(20), dp(20)));
      gap(walletTop, 8);
      weighted(walletTop, text(a.name, 13, WHITE, true));
      add(wallet, walletTop);
      space(wallet, 18);
      TextView valueText = text(Finance.money(value), 20, value < 0 ? RED : WHITE, true);
      valueText.setMaxLines(1);
      valueText.setAutoSizeTextTypeUniformWithConfiguration(
          12, 20, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
      add(wallet, valueText);
      space(wallet, 6);
      add(wallet, text(cardAccount ? "Kredi kartı hesabı" : "Hesap bakiyesi", 10, MUTED, false));
      LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(dp(206), -2);
      wp.rightMargin = dp(10);
      walletRow.addView(wallet, wp);
      wallet.setContentDescription(a.name + " · " + Finance.money(value) + " · hesap detayı");
      wallet.setOnClickListener(v -> accountDetail(a));
    }
    wallets.addView(walletRow);
    add(body, wallets);
    space(body, 8);
    add(body, button("+ Hesap ekle", false, () -> accountDialog(null)));
    space(body, 16);
    upcomingPreview();
    LinearLayout assistant = card(body);
    assistant.setBackground(
        gradient(plan.light ? 0xFFF1EFFF : 0xFF232442, plan.light ? 0xFFEAF6F4 : 0xFF152D35, 22));
    cardTitle(assistant, "spark", "Bütçe asistanı", BLUE);
    if (days == 0) hint(assistant, "Bu maaş dönemi henüz başlamadı.");
    else if (forecastData != null
        && forecastData.ready
        && month.equals(Finance.cycleMonth(LocalDate.now(), plan.payday))) {
      keyValue(
          assistant,
          "Tahmini dönem dengesi",
          Finance.money(forecastData.projectedNet),
          forecastData.projectedNet < 0 ? RED : GREEN);
      hint(
          assistant,
          "Sabit ödemeler ile değişken harcama hızını ayrı değerlendiriyorum. Hesap yöntemi dönem"
              + " sonu görünümünde.");
    } else
      hint(
          assistant,
          "Gelir, gider ve ödeme kayıtlarınla yorumlar gelişir. Az veride kesin tahmin"
              + " üretmiyorum.");
    space(assistant, 10);
    add(
        assistant,
        button(
            "Asistan yorumlarını incele",
            false,
            () -> {
              openPage(6);
            }));
    Map<Long, Long> used = Finance.byCategory(list, "E");
    for (Map.Entry<Long, Long> b : ledger.budgets().entrySet())
      if (used.getOrDefault(b.getKey(), 0L) >= b.getValue() * .8) {
        LinearLayout warning = card(body);
        cardTitle(
            warning,
            "target",
            used.getOrDefault(b.getKey(), 0L) > b.getValue()
                ? "Bütçe sınırı aşıldı"
                : "Bütçe sınırına yaklaşıyorsun",
            RED);
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
    enhancedForecast();
    section("Son işlemler", "Düzenlemek için dokun · tekrar eklemek için basılı tut");
    List<Finance.Entry> monthAll = recent;
    if (monthAll.isEmpty())
      empty(
          "İlk kaydınla başlayalım",
          "Maaşını veya günlük harcamanı ekle; görünüm kayıtlarınla dolacak.",
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

  private View icon(String name, int color, int size) {
    return new Design.Icon(this, name, color);
  }

  private int tint(int color, int alpha) {
    return (color & 0x00FFFFFF) | (alpha << 24);
  }

  private GradientDrawable gradient(int from, int to, int radius) {
    GradientDrawable d =
        new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {from, to});
    d.setCornerRadius(dp(radius));
    return d;
  }

  private String spendingStatus(double rate) {
    switch (Finance.spendingBand(rate)) {
      case 0:
        return "DENGEDE";
      case 1:
        return "DİKKAT";
      case 2:
        return "YÜKSEK HARCAMA";
      default:
        return "GELİR BEKLENİYOR";
    }
  }

  private TextView chip(String label, int color, int background) {
    TextView t = text(label, 10, color, true);
    t.setPadding(dp(9), dp(5), dp(9), dp(5));
    t.setBackground(shape(background, 50));
    return t;
  }

  private void cardTitle(LinearLayout c, String icon, String title, int color) {
    LinearLayout r = row();
    r.addView(icon(icon, color, 20), new LinearLayout.LayoutParams(dp(20), dp(20)));
    gap(r, 9);
    weighted(r, text(title, 16, WHITE, true));
    add(c, r);
    space(c, 12);
  }

  private LinearLayout metric(String label, long amount, String name, int color, double delta) {
    LinearLayout c = column();
    c.setPadding(dp(14), dp(12), dp(14), dp(12));
    GradientDrawable b = shape(CARD, 18);
    b.setStroke(dp(1), LINE);
    c.setBackground(b);
    LinearLayout r = row();
    r.addView(icon(name, color, 17), new LinearLayout.LayoutParams(dp(17), dp(17)));
    gap(r, 6);
    weighted(r, text(label, 12, MUTED, false));
    add(c, r);
    space(c, 6);
    TextView value = text(Finance.money(amount), 19, WHITE, true);
    value.setMaxLines(1);
    value.setAutoSizeTextTypeUniformWithConfiguration(
        12, 19, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
    add(c, value);
    space(c, 6);
    add(
        c,
        text(
            Double.isNaN(delta) ? "İlk dönem" : signedPct(delta) + " · önceki dönem",
            10,
            MUTED,
            false));
    return c;
  }

  private boolean isBillEntry(long id) {
    for (Planning.Bill b : plan.bills) if (b.paid == id && id != 0) return true;
    return false;
  }

  private void repeatEntry(Finance.Entry e) {
    if (isBillEntry(e.id)) {
      toast("Fatura ödemelerini Ödeme takviminden yönetebilirsin");
      return;
    }
    entryDialog(Finance.repeat(e, newEntryDate()), e.kind);
  }

  private void quickRepeat(List<Finance.Entry> period) {
    List<Finance.Entry> recent = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (Finance.Entry e : period)
      if (e.kind.equals("E")
          && !e.date.isAfter(LocalDate.now())
          && !isBillEntry(e.id)
          && seen.add(e.categoryId + ":" + e.accountId + ":" + e.amount)) {
        recent.add(e);
        if (recent.size() == 3) break;
      }
    if (recent.isEmpty()) return;
    section("Hızlı tekrar", "Önceki giderini yeni tarih ve tutarla gözden geçirerek ekle");
    for (Finance.Entry e : recent) {
      Button b =
          button(
              category(e.categoryId) + " · " + Finance.money(e.amount),
              false,
              () -> repeatEntry(e));
      add(body, b);
      space(body, 6);
    }
  }

  private void accountDetail(Finance.Account a) {
    LinearLayout f = column();
    cardTitle(f, "wallet", a.name, GREEN);
    keyValue(f, "Bugünkü bakiye", Finance.money(balance(a)), WHITE);
    keyValue(f, "Açılış", Finance.money(a.opening), MUTED);
    List<Finance.Entry> list = current();
    long in = 0, out = 0;
    for (Finance.Entry e : list) {
      if (e.accountId == a.id && e.kind.equals("I")) in += e.amount;
      if (e.accountId == a.id && e.kind.equals("E")) out += e.amount;
    }
    keyValue(f, "Dönemin geliri", Finance.money(in), GREEN);
    keyValue(f, "Dönemin gideri", Finance.money(out), RED);
    hint(f, "Transferler dönem gelir/giderine dahil değildir. Bakiye bugüne göre hesaplanır.");
    AlertDialog d = dialog("Hesap detayı", f, "Kapat");
    add(
        f,
        button(
            "Hesabı düzenle",
            false,
            () -> {
              d.dismiss();
              accountDialog(a);
            }));
    add(f, button("Hesabı sil", false, () -> deleteAccountDialog(a, d)));
    d.setOnShowListener(v -> d.getButton(-1).setOnClickListener(w -> d.dismiss()));
    d.show();
  }

  private void upcomingPreview() {
    LinearLayout c = card(body);
    cardTitle(c, "calendar", "Yaklaşan ödemeler", BLUE);
    hint(c, "Bugün ve önümüzdeki 7 gün · güncel tarihe göre");
    LocalDate today = LocalDate.now();
    List<Planning.Upcoming> upcoming = plan.upcoming(today);
    upcomingOffset = Math.min(upcomingOffset, Math.max(0, ((upcoming.size() - 1) / 20) * 20));
    if (upcoming.isEmpty()) hint(c, "Önümüzdeki 7 gün için bekleyen ödeme yok.");
    for (int i = upcomingOffset; i < Math.min(upcoming.size(), upcomingOffset + 20); i++) {
      Planning.Upcoming item = upcoming.get(i);
      String name = item.bill == null ? account(item.card.account) + " · Ekstre" : item.bill.name;
      divider(c);
      LinearLayout r = row();
      LinearLayout description = column();
      add(description, text(name, 13, WHITE, true));
      long days = ChronoUnit.DAYS.between(today, item.date);
      add(
          description,
          text(
              days == 0 ? "Bugün ödenecek" : days + " gün kaldı · " + item.date.format(DATE),
              11,
              days == 0 ? RED : MUTED,
              false));
      weighted(r, description);
      r.addView(text(Finance.money(item.amount), 13, WHITE, true));
      add(c, r);
      space(c, 8);
      Button paid =
          button(
              "Ödendi olarak işaretle",
              true,
              () -> {
                if (item.card != null) cardPayment(item.card);
                else if (item.bill.loan != 0) loanPaymentChoice(item.bill);
                else
                  confirm(
                      "Ödeme kaydedilsin mi?",
                      name
                          + " · "
                          + Finance.money(item.amount)
                          + " bugün ödendi olarak işaretlenir ve gider kaydı oluşturulur.",
                      () -> {
                        try {
                          ledger.payBill(item.bill.id, LocalDate.now());
                          refresh();
                          toast("Ödendi; gider kaydı eklendi");
                        } catch (Exception e) {
                          error("Ödeme kaydedilemedi", e);
                        }
                      });
              });
      paid.setContentDescription(name + " ödemesini işaretle");
      add(c, paid);
    }
    if (upcomingOffset > 0)
      add(
          c,
          button(
              "Önceki 20 yaklaşan ödeme",
              false,
              () -> {
                upcomingOffset -= 20;
                render();
              }));
    if (upcoming.size() > upcomingOffset + 20)
      add(
          c,
          button(
              "Sonraki 20 yaklaşan ödeme",
              false,
              () -> {
                upcomingOffset += 20;
                render();
              }));
    space(c, 12);
    add(
        c,
        button(
            "Ödeme takvimini aç",
            false,
            () -> {
              openPage(5);
            }));
  }

  private void paymentPage() {
    long total = 0;
    int overdue = 0, paid = 0;
    for (Planning.Bill b : plan.bills) {
      if (b.isPaid()) {
        paid++;
        continue;
      }
      total += b.amount;
      if (b.due.isBefore(LocalDate.now())) overdue++;
    }
    LinearLayout c = card(body);
    cardTitle(c, "calendar", "Ödeme merkezim", BLUE);
    keyValue(c, "Bekleyen faturalar", Finance.money(total), WHITE);
    keyValue(c, "Gecikmiş / ödenmiş", overdue + " / " + paid, overdue > 0 ? RED : GREEN);
    hint(
        c,
        "Kart borçları bu fatura toplamına dahil değildir. Kart ödemeleri ayrı hesap transferleri"
            + " olarak izlenir.");
    add(
        c,
        button(
            "Kredilerim ve taksitler",
            false,
            () -> {
              openPage(7);
            }));
    add(c, button("+ Fatura ekle", false, () -> billDialog(null)));
    obligations(body, true);
  }

  private String debtChange(long current, Long previous) {
    if (previous == null) return "Önceki dönem verisi yok";
    if (previous == 0)
      return current == 0 ? "Önceki döneme göre değişmedi" : "Önceki dönem borç yoktu";
    double delta = Finance.change(current, previous);
    return String.format(
        Finance.TR,
        "%s %%%.1f · önceki dönem",
        delta > 0 ? "↑ Artış" : delta < 0 ? "↓ Azalış" : "Değişim",
        Math.abs(delta));
  }

  private void heroCardDebts(LinearLayout hero) {
    if (plan.cards.isEmpty()) return;
    Planning.CardTotals totals = plan.cardTotals();
    long[] previous = plan.previousCardTotals(Finance.cycleMonth(LocalDate.now(), plan.payday));
    space(hero, 12);
    LinearLayout debts = row();
    String[] labels = {"Toplam ekstre borcu", "Dönem içi"};
    long[] values = {totals.remaining, totals.current};
    for (int i = 0; i < 2; i++) {
      if (i > 0) gap(debts, 8);
      LinearLayout box = column();
      box.setPadding(dp(12), dp(14), dp(12), dp(14));
      box.setBackground(shape(0x25000000, 16));
      add(box, text(labels[i], 11, 0xFFF5EDE7, true));
      space(box, 7);
      TextView value =
          text(totals.known == 0 ? "—" : Finance.money(values[i]), 21, 0xFFFFFFFF, true);
      value.setMaxLines(1);
      value.setAutoSizeTextTypeUniformWithConfiguration(
          12, 21, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
      add(box, value);
      space(box, 5);
      add(
          box,
          text(
              i == 0 ? Finance.money(totals.paid) + " ödendi" : "Provizyon dahil",
              10,
              0xFFF5EDE7,
              false));
      space(box, 8);
      add(
          box,
          text(
              debtChange(values[i], previous == null ? null : previous[i]), 10, 0xFFFFFFFF, false));
      weighted(debts, box);
    }
    add(hero, debts);
    if (totals.unknown > 0) {
      space(hero, 8);
      add(
          hero,
          text(
              totals.unknown + " kartın banka özeti eksik; toplam dışında", 10, 0xFFF5EDE7, false));
    }
  }

  private void creditCardOverview() {
    section("Kredi kartlarım", "Ekstre · dönem içi · provizyon / bankadan güncellediğin tutarlar");
    if (plan.cards.isEmpty()) {
      LinearLayout c = card(body);
      cardTitle(c, "card", "İlk kredi kartını ekle", BLUE);
      hint(
          c,
          "Kart adına, kesim ve ödeme günlerine göre ayrı bir kart oluştur. Banka tutarlarını"
              + " istediğin zaman güncelle.");
    }
    for (Planning.Card card : plan.cards) creditCardTile(body, card, false);
    add(body, button("+ Kredi kartı ekle", true, () -> cardDialog(null)));
    space(body, 16);
  }

  private void creditCardTile(LinearLayout parent, Planning.Card card, boolean full) {
    Finance.Account a =
        accounts.stream().filter(x -> x.id == card.account).findFirst().orElse(null);
    if (a == null) return;
    LinearLayout c = card(parent);
    GradientDrawable surface = gradient(plan.light ? 0xFFF0F1FF : 0xFF232E54, CARD, 22);
    surface.setStroke(dp(1), tint(BLUE, 65));
    c.setBackground(surface);
    LinearLayout header = row();
    header.addView(icon("card", BLUE, 22), new LinearLayout.LayoutParams(dp(22), dp(22)));
    gap(header, 10);
    weighted(header, text(a.name, 18, WHITE, true));
    header.addView(chip("KREDİ KARTI", BLUE, tint(BLUE, 24)));
    add(c, header);
    space(c, 16);
    add(
        c,
        text(
            card.updated == null ? "Kayıtlı hesap borcu" : "Bankadaki toplam · provizyon dahil",
            11,
            MUTED,
            false));
    TextView total =
        text(
            Finance.money(card.updated == null ? Math.max(0, -balance(a)) : card.bankTotal()),
            27,
            WHITE,
            true);
    total.setMaxLines(1);
    total.setAutoSizeTextTypeUniformWithConfiguration(
        15, 27, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
    add(c, total);
    space(c, 14);
    LinearLayout buckets = row();
    weighted(
        buckets, cardAmount("Ekstre borcu", card.updated == null ? null : card.statement, BLUE));
    gap(buckets, 6);
    weighted(buckets, cardAmount("Dönem içi", card.updated == null ? null : card.current, GREEN));
    gap(buckets, 6);
    weighted(
        buckets,
        cardAmount(
            "Provizyon",
            card.updated == null ? null : card.pending,
            plan.light ? 0xFF9D5700 : 0xFFF6BD60));
    add(c, buckets);
    if (card.updated != null)
      hint(c, Finance.money(card.statementPaid) + " ekstre ödemesi yapıldı");
    LocalDate close = card.statementClosing();
    LocalDate due = card.statementDue();
    long monthly = 0, remaining = 0;
    for (Enhancements.Purchase purchase : plan.extras.purchases)
      if (purchase.account == card.account && purchase.paid < purchase.count) {
        monthly = Math.addExact(monthly, purchase.amount);
        remaining = Math.addExact(remaining, purchase.remaining());
      }
    if (remaining > 0) {
      hint(
          c,
          "Takipli alışveriş taksitleri: "
              + Finance.money(monthly)
              + " / ay · "
              + Finance.money(remaining)
              + " kalan. Ekstreye ayrıca eklenmez.");
      add(c, button("Alışveriş taksitlerini gör", false, () -> openPage(11)));
    }
    space(c, 14);
    keyValue(c, "Hesap Kesim Tarihi", close.format(DATE), MUTED);
    keyValue(
        c,
        "Son ödeme tarihi",
        due.format(DATE),
        card.updated != null && card.statement > 0 && due.isBefore(LocalDate.now()) ? RED : BLUE);
    hint(
        c,
        card.updated == null
            ? "Banka özeti henüz girilmedi; üç tutarı güncelle."
            : "Son güncelleme: " + card.updated.format(DATE) + " · Bankadan elle girildi");
    LinearLayout actions = row();
    weighted(actions, button("Tutarları güncelle", true, () -> bankAmountsDialog(card)));
    gap(actions, 8);
    weighted(actions, button("Kartı yönet", false, () -> cardDialog(card)));
    add(c, actions);
    if (full) {
      space(c, 8);
      add(c, button("Kart borcu öde · transfer", false, () -> cardPayment(card)));
      hint(
          c,
          "Banka özeti gelir/gider veya hesap bakiyesi oluşturmaz. Ödeme sonrası bankadaki üç"
              + " tutarı yeniden güncelle. Gerçek banka ekstresindeki tarihler esas alınır.");
    }
  }

  private LinearLayout cardAmount(String name, Long amount, int color) {
    LinearLayout c = column();
    c.setPadding(dp(8), dp(11), dp(8), dp(11));
    c.setBackground(shape(plan.light ? 0xCCFFFFFF : 0xAA101B32, 12));
    add(c, text(name, 10, MUTED, false));
    space(c, 6);
    TextView value = text(amount == null ? "—" : Finance.money(amount), 14, color, true);
    value.setMaxLines(1);
    value.setAutoSizeTextTypeUniformWithConfiguration(
        9, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
    add(c, value);
    return c;
  }

  private long nonnegativeMoney(EditText field) {
    long value = Finance.parseMoney(field.getText().toString());
    if (value < 0) throw new IllegalArgumentException("Banka tutarları sıfır veya pozitif olmalı.");
    return value;
  }

  private void bankAmountsDialog(Planning.Card card) {
    LinearLayout f = column();
    label(f, "Ekstre borcu · kalan / TL");
    EditText statement = field("0,00", Finance.editable(card.statement), true);
    add(f, statement);
    label(f, "Bu ekstre için ödenen tutar · TL");
    EditText statementPaid = field("0,00", Finance.editable(card.statementPaid), true);
    add(f, statementPaid);
    label(f, "Dönem içi borç · ekstreye girmeyen / TL");
    EditText current = field("0,00", Finance.editable(card.current), true);
    add(f, current);
    label(f, "Provizyon · bekleyen işlemler / TL");
    EditText pending = field("0,00", Finance.editable(card.pending), true);
    add(f, pending);
    hint(
        f,
        "Bankada görünen kalan ekstreyi, kesim sonrası borcu ve bekleyen provizyonu birbirini"
            + " içermeyen tutarlar olarak gir. Ödenen tutar kalan borca dahil değildir. Yeni"
            + " ekstrede ödeneni sıfırla. Bu alanlar finansal işlem oluşturmaz; daha önceki ödemeni"
            + " burada giriyorsan tekrar ödeme olarak kaydetme.");
    TextView problem = text("", 12, RED, false);
    add(f, problem);
    AlertDialog d = dialog(account(card.account) + " · Banka özeti", f, "Güncelle");
    d.setOnShowListener(
        v ->
            d.getButton(-1)
                .setOnClickListener(
                    w -> {
                      try {
                        Planning.Card updated =
                            new Planning.Card(card.account, card.closing, card.due);
                        updated.statement = nonnegativeMoney(statement);
                        updated.statementPaid = nonnegativeMoney(statementPaid);
                        updated.current = nonnegativeMoney(current);
                        updated.pending = nonnegativeMoney(pending);
                        updated.updated = LocalDate.now();
                        ledger.saveCard(updated, null, 0);
                        d.dismiss();
                        refresh();
                        toast("Banka özeti güncellendi");
                      } catch (Exception e) {
                        problem.setText(message(e));
                      }
                    }));
    d.show();
  }

  private void spendingMap() {
    section("Harcama haritası", "Maaş dönemin · bir gün seç, özetini aşağıda gör");
    LinearLayout c = card(body);
    LocalDate start = periodStart(month), end = periodEnd(month);
    Map<LocalDate, Long> amounts = Finance.dailyExpenses(entries, start, effectiveEnd(end));
    add(
        c,
        new Design.Heatmap(
            this, start, end, LocalDate.now(), amounts, plan.light, this::dayDetail));
    hint(
        c,
        "Seçilen günün geliri ve gideri takvimin altında görünür. İşlem listesi isteğe bağlı"
            + " açılır; transferler toplamların dışında kalır.");
    List<String> labels = new ArrayList<>();
    List<LocalDate> dates = new ArrayList<>();
    labels.add("Gün seç…");
    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
      dates.add(d);
      labels.add(d.format(DATE));
    }
    Spinner chooser = spinner(labels);
    add(c, chooser);
    chooser.setOnItemSelectedListener(
        selected(
            pos -> {
              if (pos > 0) dayDetail(dates.get(pos - 1));
            }));
    dayPanel = column();
    add(c, dayPanel);
    if (selectedDay != null && !selectedDay.isBefore(start) && !selectedDay.isAfter(end))
      dayDetail(selectedDay);
  }

  private void dayDetail(LocalDate date) {
    if (dayPanel == null) return;
    selectedDay = date;
    dayPanel.removeAllViews();
    divider(dayPanel);
    add(dayPanel, text(date.format(DATE) + " · Gün özeti", 16, WHITE, true));
    space(dayPanel, 12);
    List<Finance.Entry> day = Finance.period(entries, date, date);
    long income = Finance.total(day, "I"), expense = Finance.total(day, "E");
    keyValue(dayPanel, "Toplam gelir", Finance.money(income), GREEN);
    keyValue(dayPanel, "Toplam gider", Finance.money(expense), RED);
    keyValue(
        dayPanel, "Gün dengesi", Finance.money(income - expense), income >= expense ? GREEN : RED);
    if (date.isAfter(LocalDate.now()))
      hint(dayPanel, "Planlanan gün · henüz gerçekleşmiş bakiyeye dahil değil.");
    LinearLayout list = column();
    list.setVisibility(View.GONE);
    Button expand = button("İşlem detaylarını göster ▾", false, () -> {});
    add(dayPanel, expand);
    add(dayPanel, list);
    final boolean[] open = {false};
    final int[] offset = {0};
    final int[] request = {0};
    Runnable[] load = {null};
    load[0] =
        () -> {
          int token = ++request[0];
          int position = offset[0];
          io.execute(
              () -> {
                List<Finance.Entry> rows = ledger.dayEntries(date, 50, position);
                ui.post(
                    () -> {
                      if (isDestroyed() || !date.equals(selectedDay) || token != request[0]) return;
                      list.removeAllViews();
                      if (rows.isEmpty()) hint(list, "Bu gün için işlem kaydı yok.");
                      for (Finance.Entry e : rows) {
                        divider(list);
                        entryRow(list, e);
                      }
                      if (position > 0)
                        add(
                            list,
                            button(
                                "Önceki 50 işlem",
                                false,
                                () -> {
                                  offset[0] = Math.max(0, offset[0] - 50);
                                  load[0].run();
                                }));
                      if (rows.size() == 50)
                        add(
                            list,
                            button(
                                "Sonraki 50 işlem",
                                false,
                                () -> {
                                  offset[0] += 50;
                                  load[0].run();
                                }));
                    });
              });
        };
    expand.setOnClickListener(
        v -> {
          open[0] = !open[0];
          list.setVisibility(open[0] ? View.VISIBLE : View.GONE);
          expand.setText(open[0] ? "İşlem detaylarını gizle ▴" : "İşlem detaylarını göster ▾");
          if (open[0]) load[0].run();
        });
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
    LinearLayout iconBox = row();
    iconBox.setGravity(Gravity.CENTER);
    iconBox.setBackground(shape(tint(color, plan.light ? 24 : 32), 14));
    iconBox.addView(
        icon(e.kind.equals("I") ? "up" : e.kind.equals("E") ? "down" : "repeat", color, 21),
        new LinearLayout.LayoutParams(dp(21), dp(21)));
    r.addView(iconBox, new LinearLayout.LayoutParams(dp(42), dp(42)));
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
    r.setPadding(0, dp(8), 0, dp(8));
    r.setMinimumHeight(dp(64));
    r.setOnClickListener(v -> entryDialog(e, e.kind));
    r.setOnLongClickListener(
        v -> {
          repeatEntry(e);
          return true;
        });
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
    final int[] offset = {0}, requests = {0};
    final Runnable[] update = {null};
    update[0] =
        () -> {
          int token = ++requests[0], position = offset[0];
          String needle = search;
          String kind =
              transactionType == 1
                  ? "E"
                  : transactionType == 2 ? "I" : transactionType == 3 ? "T" : "";
          long accountId = filterAccount, categoryId = filterCategory;
          LocalDate from = periodStart(month), to = periodEnd(month);
          summary.setText(R.string.transactions_loading);
          ui.postDelayed(
              () -> {
                if (token != requests[0] || isDestroyed()) return;
                io.execute(
                    () -> {
                      try {
                        Ledger.EntryPage found =
                            ledger.entryPage(
                                from, to, kind, accountId, categoryId, needle, 50, position);
                        ui.post(
                            () -> {
                              if (isDestroyed()
                                  || token != requests[0]
                                  || page != 1
                                  || !results.isAttachedToWindow()) return;
                              results.removeAllViews();
                              summary.setText(
                                  getResources()
                                      .getQuantityString(
                                          R.plurals.transaction_summary,
                                          (int) found.count,
                                          found.count,
                                          Finance.money(found.income),
                                          Finance.money(found.expense)));
                              if (found.rows.isEmpty()) {
                                LinearLayout c = card(results);
                                hint(c, "Eşleşen işlem yok. Dönemi veya filtreleri değiştir.");
                              } else {
                                LinearLayout c = card(results);
                                LocalDate last = null;
                                for (Finance.Entry e : found.rows) {
                                  if (last != null) divider(c);
                                  if (!e.date.equals(last)) {
                                    add(c, text(e.date.format(DATE), 11, MUTED, true));
                                    space(c, 10);
                                  }
                                  entryRow(c, e);
                                  last = e.date;
                                }
                              }
                              if (position > 0)
                                add(
                                    results,
                                    button(
                                        "Önceki 50 işlem",
                                        false,
                                        () -> {
                                          offset[0] = Math.max(0, offset[0] - 50);
                                          update[0].run();
                                        }));
                              if (found.count > position + 50)
                                add(
                                    results,
                                    button(
                                        "Sonraki 50 işlem",
                                        false,
                                        () -> {
                                          offset[0] += 50;
                                          update[0].run();
                                        }));
                            });
                      } catch (Exception ex) {
                        ui.post(
                            () -> {
                              if (token == requests[0] && !isDestroyed())
                                summary.setText(message(ex));
                            });
                      }
                    });
              },
              180);
        };
    query.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            search = s.toString();
            offset[0] = 0;
            update[0].run();
          }

          public void afterTextChanged(Editable e) {}
        });
    types.setOnItemSelectedListener(
        selected(
            pos -> {
              transactionType = pos;
              offset[0] = 0;
              update[0].run();
            }));
    ac.setOnItemSelectedListener(
        selected(
            pos -> {
              filterAccount = pos == 0 ? 0 : accounts.get(pos - 1).id;
              offset[0] = 0;
              update[0].run();
            }));
    cats.setOnItemSelectedListener(
        selected(
            pos -> {
              filterCategory = pos == 0 ? 0 : categories.get(pos - 1).id;
              offset[0] = 0;
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
    double incomeChange = Finance.change(income, oldIncome);
    double expenseChange = Finance.change(expense, oldExpense);
    keyValue(
        c, "Gelir değişimi", signedPct(incomeChange), Double.isNaN(incomeChange) ? MUTED : WHITE);
    keyValue(
        c,
        "Gider değişimi",
        signedPct(expenseChange),
        Double.isNaN(expenseChange) ? MUTED : expense <= oldExpense ? GREEN : RED);
    keyValue(
        c,
        "Gelirin harcanan kısmı",
        pct(Finance.spendingRate(income, expense)),
        expense > income ? RED : WHITE);
    hint(c, "Gerçekleşen işlemler · transferler hariç");
    LinearLayout calculationDetails = column();
    calculationDetails.setVisibility(View.GONE);
    hint(
        calculationDetails,
        days == 0
            ? "Karşılaştırma için gerçekleşmiş kayıt gerekli."
            : "Karşılaştırma: "
                + prevFrom.format(DATE)
                + " – "
                + prevEnd.format(DATE)
                + ". Seçili maaş döneminde önceki dönemin aynı gün aralığı; diğer aralıklarda"
                + " önceki eşit uzunluktaki dönem.");
    hint(calculationDetails, "İleri tarihli kayıtlar gerçekleşene kadar analiz dışında kalır.");
    Button calculationToggle = button("Hesaplama bilgisi ▾", false, () -> {});
    calculationToggle.setOnClickListener(
        v -> {
          boolean opening = calculationDetails.getVisibility() != View.VISIBLE;
          calculationDetails.setVisibility(opening ? View.VISIBLE : View.GONE);
          calculationToggle.setText(
              opening ? "Hesaplama bilgisini gizle ▴" : "Hesaplama bilgisi ▾");
        });
    add(c, calculationToggle);
    add(c, calculationDetails);
    if (advisorExpanded) advice(list, from, to, analysisMode == 0);
    add(
        body,
        button(
            advisorExpanded ? "Asistan yorumlarını daralt" : "Asistan yorumlarını aç",
            false,
            () -> {
              advisorExpanded = !advisorExpanded;
              render();
            }));
    if (analysisMode == 0) spendingMap();
    section("Harcama içgörüleri", "Kayıtlarından hesaplanan göstergeler");
    c = card(body);
    Map<Long, Long> spending = Finance.byCategory(list, "E");
    Set<LocalDate> spendingDays = new HashSet<>();
    long count = 0;
    Finance.Entry largest = null;
    for (Finance.Entry e : list)
      if (e.kind.equals("E")) {
        count += e.recordCount;
        spendingDays.add(e.date);
        if (largest == null || e.largestAmount > largest.largestAmount) largest = e;
      }
    keyValue(c, "Gider işlemi sayısı", Long.toString(count), WHITE);
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
              + Finance.money(largest.largestAmount)
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
    if (analysisMode == 0) enhancedForecast();
    categoryInsights();
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

  private void financeTools() {
    LinearLayout c = card(body);
    cardTitle(c, "target", "Finans araçların", GREEN);
    add(c, button("İşlem şablonları", false, () -> openPage(9)));
    space(c, 8);
    add(c, button("Birikim hedefleri", false, () -> openPage(10)));
    space(c, 8);
    add(c, button("Kart alışveriş taksitleri", false, () -> openPage(11)));
    space(c, 8);
    add(
        c,
        button(
            "Kredilerim ve taksitler",
            false,
            () -> {
              openPage(7);
            }));
    space(c, 8);
    add(
        c,
        button(
            "Maaş hesaplayıcı",
            false,
            () -> {
              openPage(8);
            }));
    space(c, 8);
    add(
        c,
        button(
            "Konuya özel yardım",
            false,
            () -> {
              openPage(6);
            }));
  }

  private void loansPage() {
    LinearLayout intro = card(body);
    cardTitle(intro, "card", "Krediler ve geri ödeme", BLUE);
    hint(
        intro,
        "Bankanın gerçek taksitini ve vadesini kaydet. Taksitler ödeme takvimine eklenir. Öderken"
            + " gidere eklemeyi veya yalnızca takip etmeyi seçersin. Anapara otomatik gelir"
            + " yazılmaz.");
    add(intro, button("+ Kredi ekle", true, this::loanDialog));
    if (plan.loans.isEmpty()) hint(intro, "Henüz kayıtlı kredi yok.");
    for (Planning.Loan l : plan.loans) {
      LinearLayout c = card(body);
      cardTitle(c, "card", l.name, BLUE);
      long paid = 0;
      int paidCount = 0, trackedCount = 0;
      Planning.Bill next = null;
      for (Planning.Bill b : plan.bills)
        if (b.loan == l.id) {
          if (b.isPaid()) {
            paid += b.amount;
            paidCount++;
            if (b.trackedPaid != null) trackedCount++;
          } else if (next == null || b.due.isBefore(next.due)) next = b;
        }
      keyValue(c, "Anapara", Finance.money(l.principal), WHITE);
      keyValue(c, "Taksit / vade", Finance.money(l.installment) + " / " + l.count + " ay", WHITE);
      keyValue(c, "Toplam geri ödeme", Finance.money(l.total()), WHITE);
      keyValue(c, "Finansman maliyeti", Finance.money(l.total() - l.principal), MUTED);
      keyValue(c, "Ödenen taksitler", paidCount + " / " + l.count, GREEN);
      if (trackedCount > 0)
        hint(
            c, trackedCount + " taksit yalnızca takip için işaretli; gider ve bakiyeye eklenmedi.");
      keyValue(c, "Kalan taksit toplamı", Finance.money(l.total() - paid), BLUE);
      hint(c, "Kalan tutar gelecekteki taksit toplamıdır; bankanın erken kapama borcu değildir.");
      progress(c, (double) paidCount / l.count, GREEN);
      if (next != null) {
        space(c, 10);
        Planning.Bill upcoming = next;
        keyValue(
            c,
            "Sıradaki taksit",
            next.due.format(DATE),
            next.due.isBefore(LocalDate.now()) ? RED : BLUE);
        add(c, button("Bu taksiti ödedim", true, () -> loanPaymentChoice(upcoming)));
      }
      space(c, 8);
      add(
          c,
          button(
              "Takibi kaldır",
              false,
              () ->
                  confirm(
                      "Kredi takibi kaldırılsın mı?",
                      "Bekleyen taksitler ve kredi planı kaldırılır. Önceden ödediğin taksitlerin"
                          + " gider kayıtları korunur.",
                      () -> {
                        try {
                          ledger.removeLoan(l.id);
                          refresh();
                        } catch (Exception e) {
                          error("Kredi kaldırılamadı", e);
                        }
                      })));
    }
    add(
        body,
        button(
            "Ödeme takvimini aç",
            false,
            () -> {
              openPage(5);
            }));
  }

  private void loanPaymentChoice(Planning.Bill bill) {
    java.util.function.Consumer<Boolean> record =
        deduct ->
            chooseDate(
                LocalDate.now(),
                date -> {
                  try {
                    ledger.payLoan(bill.id, date, deduct);
                    refresh();
                    toast(
                        deduct
                            ? "Taksit gider olarak kaydedildi"
                            : "Taksit yalnızca takipte ödendi; bakiye değişmedi");
                  } catch (Exception e) {
                    error("Taksit kaydedilemedi", e);
                  }
                });
    new AlertDialog.Builder(this)
        .setTitle("Bakiyeden düşülsün mü?")
        .setMessage(
            bill.name
                + " · "
                + Finance.money(bill.amount)
                + "\n\nEvet: "
                + account(bill.account)
                + " hesabına gider eklenir.\n"
                + "Hayır: yalnızca kredi takibinde ödenmiş görünür; bakiye ve gelir/gider"
                + " toplamları değişmez.")
        .setPositiveButton("Evet · gider ekle", (d, w) -> record.accept(true))
        .setNegativeButton("Hayır · yalnızca takip", (d, w) -> record.accept(false))
        .setNeutralButton("Vazgeç", null)
        .show();
  }

  private void loanDialog() {
    LinearLayout f = column();
    label(f, "Kredi adı");
    EditText name = field("Örn. İhtiyaç kredisi", "", false);
    add(f, name);
    label(f, "Anapara · TL");
    EditText principal = field("50000,00", "", true);
    add(f, principal);
    label(f, "Vade · ay (1–360)");
    EditText count = field("12", "12", true);
    add(f, count);
    label(f, "Bankanın gerçek aylık taksiti · TL");
    EditText installment = field("", "", true);
    add(f, installment);
    label(f, "İsteğe bağlı aylık faiz · % (tahmin)");
    EditText rate = field("3,5", "", true);
    add(f, rate);
    TextView problem = text("", 12, RED, false);
    add(
        f,
        button(
            "Faizle taksit tahmini hesapla",
            false,
            () -> {
              try {
                long estimate =
                    Calculators.creditInstallment(
                        Finance.parseMoney(principal.getText().toString()),
                        Calculators.decimal(rate.getText().toString()),
                        Integer.parseInt(count.getText().toString()));
                installment.setText(Finance.editable(estimate));
                problem.setText(R.string.credit_estimate_note);
              } catch (Exception e) {
                problem.setText(message(e));
              }
            }));
    label(f, "Ödeme hesabı");
    Spinner account = spinner(accounts);
    add(f, account);
    List<Finance.Category> expenseCats = ledger.categories("E");
    label(f, "Gider kategorisi");
    Spinner category = spinner(expenseCats);
    add(f, category);
    for (int i = 0; i < expenseCats.size(); i++)
      if (expenseCats.get(i).name.equals("Faturalar")) category.setSelection(i);
    LocalDate[] first = {LocalDate.now()};
    label(f, "İlk taksit tarihi");
    Button date = button(first[0].format(DATE), false, () -> {});
    date.setOnClickListener(
        v ->
            chooseDate(
                first[0],
                d -> {
                  first[0] = d;
                  date.setText(d.format(DATE));
                }));
    add(f, date);
    hint(
        f,
        "Eşit taksitli kredi takibi. Aynı ödeme için ayrıca fatura/düzenli gider oluşturma."
            + " Değişken taksitler bu modelin kapsamı dışında.");
    add(f, problem);
    AlertDialog dialog = dialog("Yeni kredi", f, "Kaydet");
    dialog.setOnShowListener(
        v ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    w -> {
                      try {
                        Planning.Loan loan = new Planning.Loan();
                        loan.name = name.getText().toString().trim();
                        loan.principal = Finance.parseMoney(principal.getText().toString());
                        loan.installment = Finance.parseMoney(installment.getText().toString());
                        loan.count = Integer.parseInt(count.getText().toString());
                        loan.first = first[0];
                        loan.account = ((Finance.Account) account.getSelectedItem()).id;
                        loan.category = ((Finance.Category) category.getSelectedItem()).id;
                        if (loan.count < 1 || loan.count > 360)
                          throw new IllegalArgumentException("Vade 1–360 ay olmalı.");
                        ledger.addLoan(loan);
                        dialog.dismiss();
                        refresh();
                        toast("Kredi ve taksitleri eklendi");
                      } catch (Exception e) {
                        problem.setText(message(e));
                      }
                    }));
    dialog.show();
  }

  private SalarySettings salaryDraft;
  private boolean salaryRaisesTab = false;
  private static final String[] SALARY_MONTHS = {
    "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
    "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
  };

  private void salaryWatch(EditText input, java.util.function.Consumer<String> change) {
    input.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            change.accept(s.toString());
          }

          public void afterTextChanged(Editable e) {}
        });
  }

  private String salaryPct(java.math.BigDecimal value) {
    return "%" + String.format(Finance.TR, "%.2f", value);
  }

  private void salaryPage() {
    try {
      if (salaryDraft == null) salaryDraft = SalarySettings.parse(plan.salary.json());
    } catch (Exception e) {
      toast(message(e));
      return;
    }
    SalarySettings draft = salaryDraft;
    LinearLayout tabs = row();
    weighted(
        tabs,
        button(
            "Maaş Hesapla",
            !salaryRaisesTab,
            () -> {
              salaryRaisesTab = false;
              render();
            }));
    weighted(
        tabs,
        button(
            "Zam Oranları",
            salaryRaisesTab,
            () -> {
              salaryRaisesTab = true;
              render();
            }));
    add(body, tabs);
    LinearLayout result = column();
    TextView status = text("", 12, MUTED, false);
    Button save =
        button(
            "Kaydet",
            true,
            () -> {
              try {
                SalarySettings validated = SalarySettings.parse(draft.json());
                SalarySettings previous = plan.salary;
                plan.salary = validated;
                try {
                  ledger.savePlanning(plan);
                } catch (Exception e) {
                  plan.salary = previous;
                  throw e;
                }
                toast("Maaş bilgileri kaydedildi");
                render();
              } catch (Exception e) {
                status.setText(message(e));
                status.setTextColor(RED);
              }
            });
    Runnable calculate =
        () -> {
          result.removeAllViews();
          try {
            boolean changed = !draft.json().toString().equals(plan.salary.json().toString());
            save.setEnabled(changed);
            save.setText(changed ? "Kaydet" : "Kaydedildi");
            status.setText(changed ? "Kaydedilmemiş değişiklik var" : "Maaş bilgilerin kaydedildi");
            status.setTextColor(changed ? BLUE : MUTED);
            if (draft.current.trim().isEmpty()) {
              hint(result, "Mevcut maaşını gir, hesaplama burada görünsün.");
              return;
            }
            int year = SalarySettings.year(draft.year), start = draft.july ? 6 : 0;
            java.math.BigDecimal[] rates = new java.math.BigDecimal[6];
            for (int i = 0; i < 6; i++)
              rates[i] =
                  Calculators.salaryPercent(
                      draft.inflation.getOrDefault(year + "-" + (start + i), ""));
            java.math.BigDecimal periodRaise =
                Calculators.salaryPercent(
                    draft.raises.getOrDefault(year + (draft.july ? "-temmuz" : "-ocak"), ""));
            int nextYear = draft.july ? year + 1 : year;
            String nextPeriod = draft.july ? "Ocak" : "Temmuz";
            java.math.BigDecimal nextRaise =
                Calculators.salaryPercent(
                    draft.raises.getOrDefault(nextYear + (draft.july ? "-ocak" : "-temmuz"), ""));
            long current = Finance.parseMoney(draft.current);
            if (current == 0) {
              hint(result, "Mevcut maaşını gir, hesaplama burada görünsün.");
              return;
            }
            Calculators.Salary salary = Calculators.salary(current, periodRaise, nextRaise, rates);
            LinearLayout detail = card(result);
            cardTitle(detail, "chart", "Hesaplama Detayı", BLUE);
            keyValue(detail, "Mevcut maaş", Finance.money(current), WHITE);
            keyValue(detail, "6 aylık kümülatif enflasyon", salaryPct(salary.cumulative), BLUE);
            keyValue(
                detail,
                "Enflasyon farkı",
                salary.difference.signum() > 0 ? salaryPct(salary.difference) : "Yok",
                GREEN);
            keyValue(detail, nextYear + " " + nextPeriod + " zammı", salaryPct(nextRaise), GREEN);
            hint(
                detail,
                "Mevcut maaş "
                    + year
                    + " "
                    + (draft.july ? "Temmuz" : "Ocak")
                    + " zammını zaten içerir ("
                    + salaryPct(periodRaise)
                    + "). Bu zam yalnızca enflasyon farkının kıyas tabanıdır.");
            LinearLayout estimate = card(result);
            cardTitle(estimate, "up", nextYear + " " + nextPeriod + " Tahmini Maaşın", GREEN);
            add(estimate, text(Finance.money(salary.estimated), 30, GREEN, true));
            hint(estimate, "Mevcuttan " + salaryPct(salary.increase) + " artış");
            hint(
                estimate,
                "Boş aylık enflasyon ve zam alanları hesapta %0 kabul edilir. Bu tahmin bordro veya"
                    + " kesin ödeme tutarı değildir; gelir kaydı oluşturmaz.");
          } catch (Exception e) {
            hint(result, message(e));
          }
        };
    if (salaryRaisesTab) {
      LinearLayout f = card(body);
      cardTitle(f, "calendar", "Başlangıç Yılı", BLUE);
      EditText first = field(String.valueOf(LocalDate.now().getYear()), draft.firstYear, true);
      add(f, first);
      LinearLayout rates = column();
      add(body, rates);
      Runnable showRates =
          () -> {
            rates.removeAllViews();
            try {
              int firstYear = SalarySettings.year(draft.firstYear);
              for (int year = firstYear; year <= firstYear + 1; year++) {
                LinearLayout c = card(rates);
                cardTitle(c, "up", year + " Toplu Sözleşme Zamları", BLUE);
                for (String period : Arrays.asList("ocak", "temmuz")) {
                  String key = year + "-" + period;
                  label(c, period.equals("ocak") ? "Ocak Zammı · %" : "Temmuz Zammı · %");
                  EditText input = field("0,00", draft.raises.getOrDefault(key, ""), true);
                  input.setInputType(
                      android.text.InputType.TYPE_CLASS_NUMBER
                          | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                          | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
                  add(c, input);
                  salaryWatch(
                      input,
                      value -> {
                        draft.raises.put(key, value);
                        calculate.run();
                      });
                }
              }
            } catch (Exception e) {
              hint(rates, "Geçerli bir başlangıç yılı gir (2000–2099).");
            }
          };
      salaryWatch(
          first,
          value -> {
            draft.firstYear = value;
            showRates.run();
            calculate.run();
          });
      showRates.run();
    } else {
      LinearLayout f = card(body);
      cardTitle(f, "up", "Maaş Hesapla", GREEN);
      label(f, "Mevcut (güncel) maaşın · TL");
      EditText current = field("0,00", draft.current, true);
      add(f, current);
      label(f, "Yıl");
      EditText yearInput = field(String.valueOf(LocalDate.now().getYear()), draft.year, true);
      add(f, yearInput);
      label(f, "Dönem");
      Spinner period = spinner(Arrays.asList("Ocak – Haziran", "Temmuz – Aralık"));
      period.setSelection(draft.july ? 1 : 0);
      add(f, period);
      LinearLayout inflation = card(body);
      cardTitle(inflation, "chart", "Aylık Enflasyon · %", BLUE);
      hint(inflation, "Bilmediğin aylar boş kalabilir; hesapta %0 kabul edilir.");
      LinearLayout months = column();
      add(inflation, months);
      Runnable showMonths =
          () -> {
            months.removeAllViews();
            try {
              int year = SalarySettings.year(draft.year), start = draft.july ? 6 : 0;
              for (int i = start; i < start + 6; i++) {
                String key = year + "-" + i;
                label(months, SALARY_MONTHS[i]);
                EditText input = field("boş", draft.inflation.getOrDefault(key, ""), true);
                input.setInputType(
                    android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                        | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
                add(months, input);
                salaryWatch(
                    input,
                    value -> {
                      draft.inflation.put(key, value);
                      calculate.run();
                    });
              }
            } catch (Exception e) {
              hint(months, "Geçerli bir hesap yılı gir (2000–2099).");
            }
          };
      salaryWatch(
          current,
          value -> {
            draft.current = value;
            calculate.run();
          });
      salaryWatch(
          yearInput,
          value -> {
            draft.year = value;
            showMonths.run();
            calculate.run();
          });
      period.setOnItemSelectedListener(
          selected(
              position -> {
                if (draft.july != (position == 1)) {
                  draft.july = position == 1;
                  showMonths.run();
                  calculate.run();
                }
              }));
      showMonths.run();
      add(body, result);
    }
    add(body, save);
    add(body, status);
    calculate.run();
  }

  private void topicAdvice() {
    LinearLayout c = card(body);
    cardTitle(c, "target", "Hangi konuda yardımcı olayım?", GREEN);
    Spinner topic =
        spinner(
            Arrays.asList(
                "Maaşa kadar harcama payım",
                "Kredi taksit yüküm",
                "Kredi kartı ödemelerim",
                "Kategori tasarrufu",
                "Satın almadan önce dene"));
    add(c, topic);
    EditText purchase = field("Planladığın alışveriş · TL", "", true);
    purchase.setVisibility(View.GONE);
    add(c, purchase);
    LinearLayout answer = column();
    add(c, answer);
    Runnable calculate =
        () -> {
          answer.removeAllViews();
          List<Finance.Entry> current = current();
          long income = Finance.total(current, "I"), expense = Finance.total(current, "E");
          LocalDate end = periodEnd(month);
          int choice = topic.getSelectedItemPosition();
          long unpaid = 0;
          for (Planning.Bill b : plan.bills)
            if (!b.isPaid() && !b.due.isAfter(end)) unpaid += b.amount;
          if (choice == 0 || choice == 4) {
            if (!month.equals(Finance.cycleMonth(LocalDate.now(), plan.payday))) {
              hint(answer, "Bu hesap bugünkü maaş dönemin için anlamlıdır. Güncel dönemi seç.");
              return;
            }
            if (income == 0) {
              hint(
                  answer,
                  "Önce bu dönemin gerçekleşmiş gelirini kaydet; beklenen maaşı kazanılmış gibi"
                      + " saymıyorum.");
              return;
            }
            long buy = 0;
            if (choice == 4)
              try {
                buy = nonnegativeMoney(purchase);
              } catch (Exception ex) {
                hint(answer, message(ex));
                return;
              }
            long target = Math.round(income * plan.target / 100d),
                available = income - expense - unpaid - target;
            long remaining = ChronoUnit.DAYS.between(LocalDate.now(), end) + 1;
            keyValue(answer, "Bekleyen faturalar + taksitler", Finance.money(unpaid), BLUE);
            keyValue(
                answer,
                "Tasarruf hedefi sonrası pay",
                Finance.money(available),
                available < 0 ? RED : GREEN);
            keyValue(
                answer,
                "Bugün dahil günlük pay",
                Finance.money(Math.max(0, available) / remaining),
                GREEN);
            if (choice == 4) {
              keyValue(
                  answer,
                  "Alışveriş sonrası günlük pay",
                  Finance.money(Math.max(0, available - buy) / remaining),
                  BLUE);
              if (buy > Math.max(0, available))
                hint(
                    answer,
                    "Bu alışveriş hedef payını "
                        + Finance.money(buy - Math.max(0, available))
                        + " aşar. Ertelemeyi veya daha düşük tutarı deneyebilirsin.");
              hint(answer, "Bu bir senaryo; hiçbir işlem oluşturulmadı.");
            }
            hint(
                answer,
                "Kaydettiğin gelir, gider, tasarruf hedefi ve dönem sonuna kadar bekleyen ödemeler"
                    + " kullanıldı. Kart ödemeleri gider olarak ikinci kez düşülmez; banka nakit"
                    + " durumunu ayrıca kontrol et.");
          } else if (choice == 1) {
            long burden = 0;
            for (Planning.Bill b : plan.bills)
              if (b.loan != 0 && !b.due.isBefore(periodStart(month)) && !b.due.isAfter(end))
                burden += b.amount;
            keyValue(answer, "Dönemin planlı kredi taksitleri", Finance.money(burden), BLUE);
            hint(
                answer,
                income > 0
                    ? "Kayıtlı gelirinin "
                        + pct(100d * burden / income)
                        + " kadarı. Yeni kredi almadan önce faturalar ve yaşam giderleri sonrası"
                        + " kalan payı karşılaştır."
                    : "Gelir kaydı olmadan taksit/gelir oranı hesaplanamaz.");
            hint(
                answer,
                "Bu oran kişisel bir gösterge; kredi onayı veya erken kapama teklifi değildir.");
          } else if (choice == 2) {
            long statements = 0;
            for (Planning.Card card : plan.cards)
              if (card.updated != null) statements += card.statement;
            keyValue(answer, "Son girilen ekstre borçları", Finance.money(statements), BLUE);
            hint(
                answer,
                "Bu tutar elle girilen banka özetlerinden gelir. Ödeme yaptıysan önce kart"
                    + " tutarlarını güncelle; provizyon kesinleşmiş ekstre değildir.");
            add(
                answer,
                button(
                    "Ödeme takvimini aç",
                    false,
                    () -> {
                      openPage(5);
                    }));
          } else {
            Map<Long, Long> sums = Finance.byCategory(current, "E");
            if (sums.isEmpty()) {
              hint(answer, "Öneri için bu dönemde gider kaydı gerekli.");
              return;
            }
            Map.Entry<Long, Long> top = sums.entrySet().iterator().next();
            hint(
                answer,
                "En yüksek harcama "
                    + category(top.getKey())
                    + ": "
                    + Finance.money(top.getValue())
                    + ". %10 azaltma senaryosu "
                    + Finance.money(Math.round(top.getValue() * .1))
                    + " bırakır. Zorunlu giderlerde kesinti yerine aynı ihtiyacın daha uygun"
                    + " alternatifini değerlendir.");
          }
        };
    topic.setOnItemSelectedListener(
        selected(
            pos -> {
              purchase.setVisibility(pos == 4 ? View.VISIBLE : View.GONE);
              if (pos != 4) calculate.run();
              else {
                answer.removeAllViews();
                hint(answer, "Alışveriş tutarını girip senaryoyu hesapla.");
              }
            }));
    add(c, button("Seçtiğim konuyu değerlendir", true, calculate));
    hint(
        c,
        "Açıklanabilir yerel hesaplar · internete veri göndermez · yalnızca kayıtlarını kullanır.");
  }

  private void reminderSettings() {
    LinearLayout c = card(body);
    cardTitle(c, "calendar", "Ödeme bildirimleri", BLUE);
    android.content.SharedPreferences prefs = Reminders.prefs(this);
    boolean enabled = prefs.getBoolean("enabled", false);
    hint(
        c,
        enabled
            ? "Hatırlatma açık · Android izin durumu: "
                + (Reminders.allowed(this) ? "izinli" : "bildirim izni kapalı")
            : "Hatırlatma kapalı. Açtığında fatura, kredi taksiti ve ekstre ödemeleri kontrol"
                + " edilir.");
    add(
        c,
        button(
            enabled ? "Hatırlatmayı kapat" : "Ödeme hatırlatmalarını aç",
            !enabled,
            () -> {
              prefs.edit().putBoolean("enabled", !enabled).remove("lastSent").apply();
              if (!enabled
                  && android.os.Build.VERSION.SDK_INT >= 33
                  && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                      != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(
                    new String[] {android.Manifest.permission.POST_NOTIFICATIONS}, 71);
              Reminders.schedule(this);
              if (!enabled && Reminders.allowed(this)) io.execute(() -> Reminders.check(this));
              render();
            }));
    if (enabled) {
      label(c, "Kaç gün önceden?");
      Spinner lead = spinner(Arrays.asList("Vade günü", "1 gün önce", "3 gün önce"));
      int current = prefs.getInt("lead", 1);
      lead.setSelection(current == 0 ? 0 : current == 1 ? 1 : 2);
      add(c, lead);
      lead.setOnItemSelectedListener(
          selected(
              pos -> {
                int value = pos == 0 ? 0 : pos == 1 ? 1 : 3;
                if (value != prefs.getInt("lead", 1)) {
                  prefs.edit().putInt("lead", value).remove("lastSent").apply();
                  Reminders.schedule(this);
                }
              }));
      label(c, "Günlük kontrol saati");
      Spinner hour = spinner(Arrays.asList("09:00", "12:00", "18:00"));
      int saved = prefs.getInt("hour", 9);
      hour.setSelection(saved == 9 ? 0 : saved == 12 ? 1 : 2);
      add(c, hour);
      hour.setOnItemSelectedListener(
          selected(
              pos -> {
                int value = pos == 0 ? 9 : pos == 1 ? 12 : 18;
                if (value != prefs.getInt("hour", 9)) {
                  prefs.edit().putInt("hour", value).apply();
                  Reminders.schedule(this);
                }
              }));
      add(
          c,
          button(
              "Deneme bildirimi gönder",
              false,
              () -> {
                if (!Reminders.allowed(this)) {
                  toast("Android ayarlarından Cep Bütçem bildirimlerine izin ver.");
                  Intent settings =
                      new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                          .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
                  startActivity(settings);
                } else {
                  Reminders.test(this);
                  toast("Deneme bildirimi gönderildi");
                }
              }));
    }
    hint(
        c,
        "Telefonun yerel saatine göre günlük kontrol. Enerji tasarrufu bildirimi geciktirebilir;"
            + " kesin dakika alarmı değildir. Zorla durdurma sonrası uygulamayı yeniden aç. Kilit"
            + " ekranında tutarlar gizlenir.");
  }

  @Override
  public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
    super.onRequestPermissionsResult(code, permissions, grants);
    if (code == 71) {
      Reminders.schedule(this);
      if (Reminders.allowed(this)) io.execute(() -> Reminders.check(this));
      render();
    }
  }

  private void settings() {
    preferences();
    financeTools();
    reminderSettings();
    LinearLayout payments = card(body);
    cardTitle(payments, "calendar", "Ödemelerin", BLUE);
    hint(
        payments,
        plan.cards.size()
            + " kredi kartı · "
            + plan.bills.stream().filter(b -> !b.isPaid()).count()
            + " bekleyen fatura");
    add(
        payments,
        button(
            "Ödeme takvimini aç",
            false,
            () -> {
              openPage(5);
            }));
    space(payments, 8);
    add(payments, button("+ Kredi kartı ekle", true, () -> cardDialog(null)));
    LinearLayout privacy = card(body);
    add(privacy, text("Senin verin, senin telefonun", 17, GREEN, true));
    space(privacy, 9);
    hint(
        privacy,
        "Hesap veya internet bağlantısı gerekmez. Kayıtlar uygulamanın yerel veritabanında tutulur."
            + " Otomatik bulut yedeği kapalıdır; telefonu değiştirirken JSON yedeğini taşı.");
    section("Banka ve nakit hesapların", "Bakiyeler bugüne kadarki gerçekleşmiş işlemleri içerir");
    for (Finance.Account a : accounts) {
      if (plan.cardAccounts.contains(a.id)) continue;
      LinearLayout c = card(body);
      LinearLayout r = row();
      weighted(r, text(a.name, 17, WHITE, true));
      r.addView(text(Finance.money(balance(a)), 19, GREEN, true));
      add(c, r);
      space(c, 10);
      hint(c, "Açılış bakiyesi: " + Finance.money(a.opening));
      space(c, 8);
      add(c, button("Hesabı düzenle", false, () -> accountDialog(a)));
      space(c, 6);
      Button delete = button("Hesabı sil", false, () -> deleteAccountDialog(a, null));
      delete.setTextColor(RED);
      add(c, delete);
    }
    if (!plan.cardAccounts.isEmpty()) {
      section("Kartların", "Banka hesaplarından ayrı · takip ve geçmiş kayıtlar");
      for (Finance.Account a : accounts)
        if (plan.cardAccounts.contains(a.id)) {
          Planning.Card tracked =
              plan.cards.stream().filter(c -> c.account == a.id).findFirst().orElse(null);
          LinearLayout c = card(body);
          cardTitle(c, "card", a.name, BLUE);
          hint(
              c,
              tracked == null
                  ? "Kart takibi kapalı · geçmiş işlemler korundu"
                  : "Kredi kartı · banka tutarları kart özetinde");
          space(c, 8);
          add(
              c,
              button(
                  tracked == null ? "Kart takibini yeniden aç" : "Kartı yönet",
                  false,
                  () -> cardDialog(tracked == null ? new Planning.Card(a.id, 1, 10) : tracked)));
          space(c, 6);
          add(
              c,
              button(
                  "Kart hesabını ve kayıtlarını sil", false, () -> deleteAccountDialog(a, null)));
        }
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
    add(c, text("Cep Bütçem · v" + getString(R.string.app_version), 12, GREEN, true));
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
    ACCENT = plan.light ? 0xFFA74E32 : 0xFF35D6AE;
    BG = plan.light ? 0xFFFCF5EF : 0xFF0C111B;
    CARD = plan.light ? 0xFFFFFFFF : 0xFF171E2B;
    LINE = plan.light ? 0xFFEADCD2 : 0xFF293243;
    WHITE = plan.light ? 0xFF30231E : 0xFFF3F6FA;
    MUTED = plan.light ? 0xFF79675E : 0xFF9AA8BA;
    GREEN = plan.light ? 0xFF00795D : 0xFF35D6AE;
    RED = plan.light ? 0xFFBF2448 : 0xFFFF7B8B;
    BLUE = plan.light ? 0xFF5261C9 : 0xFF99ABFF;
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
    space(c, 8);
    add(
        c,
        button(
            plan.budgetNeedle ? "Halka görünümüne geç" : "Yarım daire ibreye geç",
            false,
            () ->
                featureAction(
                    () -> {
                      Planning updated = ledger.planning();
                      updated.budgetNeedle = !updated.budgetNeedle;
                      ledger.savePlanning(updated);
                    })));
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
    if (cycle && month.equals(Finance.cycleMonth(LocalDate.now(), plan.payday))) enhancedForecast();
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
      creditCardTile(parent, card, full);
    }
    List<Planning.Bill> sorted = new ArrayList<>(plan.bills);
    sorted.sort(Comparator.comparing((Planning.Bill b) -> b.isPaid()).thenComparing(b -> b.due));
    shown = 0;
    if (full) {
      paymentOffset = Math.min(paymentOffset, Math.max(0, ((sorted.size() - 1) / 20) * 20));
      hint(
          parent,
          "Ödemeler "
              + (sorted.isEmpty() ? 0 : paymentOffset + 1)
              + "–"
              + Math.min(sorted.size(), paymentOffset + 20)
              + " / "
              + sorted.size());
    }
    List<Planning.Bill> visible =
        full ? sorted.subList(paymentOffset, Math.min(sorted.size(), paymentOffset + 20)) : sorted;
    for (Planning.Bill b : visible) {
      if (!full && (b.isPaid() || shown++ >= 3)) continue;
      LinearLayout c = card(parent);
      add(c, text(b.name + " · " + Finance.money(b.amount), 16, b.isPaid() ? GREEN : RED, true));
      space(c, 8);
      hint(
          c,
          (b.isPaid()
                  ? (b.trackedPaid != null ? "Ödendi · yalnızca takip" : "Ödendi · gider kaydı")
                  : b.due.isBefore(LocalDate.now())
                      ? "Gecikmiş"
                      : b.due.equals(LocalDate.now()) ? "Bugün ödenecek" : "Bekliyor")
              + " · "
              + b.due.format(DATE)
              + " · "
              + account(b.account)
              + (b.monthly ? " · Aylık tekrar" : ""));
      if (full && !b.isPaid()) {
        add(
            c,
            button(
                "Ödemeyi kaydet",
                true,
                () -> {
                  if (b.loan != 0) {
                    loanPaymentChoice(b);
                    return;
                  }
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
                      });
                }));
        space(c, 6);
        if (b.loan == 0) add(c, button("Faturayı düzenle", false, () -> billDialog(b)));
      } else if (full) {
        add(
            c,
            button(
                "Ödemeyi geri al",
                false,
                () ->
                    confirm(
                        "Ödeme geri alınsın mı?",
                        "Gider kaydı varsa silinir; yalnızca takip ödemesinde bakiye değişmez."
                            + " Sonraki fatura korunur; bu eski fatura yeniden ödeme sonrası tekrar"
                            + " oluşturmaz.",
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
      if (paymentOffset > 0)
        add(
            parent,
            button(
                "Önceki 20 ödeme",
                false,
                () -> {
                  paymentOffset -= 20;
                  render();
                }));
      if (sorted.size() > paymentOffset + 20)
        add(
            parent,
            button(
                "Sonraki 20 ödeme",
                false,
                () -> {
                  paymentOffset += 20;
                  render();
                }));
      add(parent, button("+ Kredi kartı ekle", true, () -> cardDialog(null)));
      space(parent, 8);
      add(parent, button("+ Fatura ekle", false, () -> billDialog(null)));
      hint(
          parent,
          "Kart eklerken yeni hesap oluşturabilir veya mevcut bir hesabı seçebilirsin. Banka"
              + " tutarlarını kartın üzerinden güncelle. Alışveriş giderini kart hesabına, borç"
              + " ödemesini banka→kart transferi olarak kaydet. Faturalar yalnızca ödeme kaydıyla"
              + " gider olur. Aylık faturada ödeme sonrası sonraki fatura aynı tahmini tutarla"
              + " oluşturulur; tutarını yeni fatura geldiğinde düzenle.");
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
    AlertDialog[] owner = {null};
    List<Finance.Account> choices = new ArrayList<>();
    if (existing == null) {
      choices.add(new Finance.Account(0, "Yeni kart hesabı oluştur", 0));
      for (Finance.Account a : accounts)
        if (plan.cards.stream().noneMatch(c -> c.account == a.id)) choices.add(a);
    } else {
      for (Finance.Account a : accounts) if (a.id == existing.account) choices.add(a);
    }
    label(f, "Kart hesabı");
    Spinner ac = spinner(choices);
    add(f, ac);
    ac.setEnabled(existing == null);
    LinearLayout creation = column();
    label(creation, "Yeni kredi kartının adı");
    EditText name = field("Örn. Bonus kartım", "", false);
    name.setFilters(new InputFilter[] {new InputFilter.LengthFilter(60)});
    add(creation, name);
    label(creation, "Takip başlangıcındaki kesinleşmiş borç · TL");
    EditText opening = field("0,00", "0,00", true);
    add(creation, opening);
    hint(
        creation,
        "Pozitif borç gir; kart hesabına negatif açılış bakiyesi yazılır. Provizyonu buraya ekleme."
            + " Banka özeti ayrıca güncellenir.");
    add(f, creation);
    creation.setVisibility(existing == null ? View.VISIBLE : View.GONE);
    ac.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            creation.setVisibility(choices.get(position).id == 0 ? View.VISIBLE : View.GONE);
          }

          public void onNothingSelected(AdapterView<?> parent) {}
        });
    label(f, "Hesap kesim günü · 1–31");
    EditText close =
        field("Örn. 10", existing == null ? "" : Integer.toString(existing.closing), true);
    add(f, close);
    label(f, "Son ödeme günü · 1–31");
    EditText due = field("Örn. 20", existing == null ? "" : Integer.toString(existing.due), true);
    add(f, due);
    hint(
        f,
        "Son ödeme kesimden sonraki ilk seçili gündür. Bankanın bildirdiği gerçek tarihler esas"
            + " alınmalı.");
    if (existing != null) {
      add(
          f,
          button(
              "Ekstreye özel tarihler",
              false,
              () -> {
                owner[0].dismiss();
                cardDatesDialog(existing);
              }));
      space(f, 8);
      add(
          f,
          button(
              "Banka tutarlarını güncelle",
              true,
              () -> {
                owner[0].dismiss();
                bankAmountsDialog(existing);
              }));
      space(f, 8);
      add(
          f,
          button(
              "Kart borcu öde · transfer",
              false,
              () -> {
                owner[0].dismiss();
                cardPayment(existing);
              }));
      space(f, 8);
      add(
          f,
          button(
              "Kart hesabını ve kayıtlarını sil",
              false,
              () -> {
                Finance.Account a = choices.get(0);
                deleteAccountDialog(a, owner[0]);
              }));
    }
    TextView problem = text("", 12, RED, false);
    add(f, problem);
    AlertDialog d =
        dialog(existing == null ? "Kredi kartı ekle" : "Kredi kartını yönet", f, "Kaydet");
    owner[0] = d;
    if (existing != null)
      d.setButton(
          -3,
          "Kart takibini sil",
          (v, w) ->
              confirm(
                  "Kart takibi silinsin mi?",
                  account(existing.account)
                      + " özet kartı ve banka tutarları kaldırılır. Hesabı, alışverişleri ve ödeme"
                      + " transferleri korunur; hesabı ayrıca Ayarlar'dan silebilirsin.",
                  () -> {
                    try {
                      ledger.removeCard(existing.account);
                      refresh();
                    } catch (Exception e) {
                      error("Kart silinemedi", e);
                    }
                  }));
    d.setOnShowListener(
        v ->
            d.getButton(-1)
                .setOnClickListener(
                    w -> {
                      try {
                        long id = ((Finance.Account) ac.getSelectedItem()).id;
                        Planning.Card card =
                            new Planning.Card(
                                id,
                                Integer.parseInt(close.getText().toString().trim()),
                                Integer.parseInt(due.getText().toString().trim()));
                        if (existing != null) {
                          Planning.Card saved =
                              ledger.planning().cards.stream()
                                  .filter(c -> c.account == existing.account)
                                  .findFirst()
                                  .orElseThrow(
                                      () ->
                                          new IllegalArgumentException("Kart artık mevcut değil."));
                          card.statement = saved.statement;
                          card.statementPaid = saved.statementPaid;
                          card.current = saved.current;
                          card.pending = saved.pending;
                          card.updated = saved.updated;
                        }
                        ledger.saveCard(
                            card,
                            name.getText().toString(),
                            id == 0 ? -nonnegativeMoney(opening) : 0);
                        d.dismiss();
                        refresh();
                        if (existing == null) bankAmountsDialog(card);
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
    CheckBox transfer = new CheckBox(this);
    transfer.setText(R.string.card_payment_transfer);
    transfer.setTextColor(WHITE);
    transfer.setButtonTintList(ColorStateList.valueOf(ACCENT));
    transfer.setChecked(!sources.isEmpty());
    transfer.setEnabled(!sources.isEmpty());
    add(f, transfer);
    label(f, "Ödeme yapılan kaynak hesap");
    Spinner source = spinner(sources);
    source.setEnabled(transfer.isChecked());
    transfer.setOnCheckedChangeListener((button, checked) -> source.setEnabled(checked));
    add(f, source);
    label(f, "Ödediğin tutar · TL");
    EditText amount =
        field(
            "",
            card.updated != null && card.statement > 0 ? Finance.editable(card.statement) : "",
            true);
    add(f, amount);
    CheckBox reduceStatement = new CheckBox(this);
    reduceStatement.setText(R.string.card_payment_statement);
    reduceStatement.setTextColor(WHITE);
    reduceStatement.setButtonTintList(ColorStateList.valueOf(ACCENT));
    reduceStatement.setChecked(card.updated != null && card.statement > 0);
    reduceStatement.setEnabled(card.updated != null && card.statement > 0);
    add(f, reduceStatement);
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
        "Transfer seçiliyse kaynak hesaptan kart hesabına aktarılır; yeniden gider sayılmaz."
            + " Transferi kapatırsan yalnızca ekstre takibi güncellenir. Tutarı kısmi ödeme için"
            + " değiştirebilirsin. Ekstre takibi seçiliyken kalan azalır, ödenen artar.");
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
                        ledger.payCard(
                            card,
                            Finance.parseMoney(amount.getText().toString()),
                            date[0],
                            sources.isEmpty() ? 0 : ((Finance.Account) source.getSelectedItem()).id,
                            transfer.isChecked(),
                            reduceStatement.isChecked());
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
    GradientDrawable inputBackground = shape(plan.light ? 0xFFF1F3F7 : 0xFF202A3A, 12);
    inputBackground.setStroke(dp(1), LINE);
    e.setBackground(inputBackground);
    e.setBackgroundTintList(null);
    e.setMinHeight(dp(48));
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
    return new AlertDialog.Builder(this, (plan.light ? R.style.LightDialog : R.style.DarkDialog))
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
            plan.light ? R.style.LightDialog : R.style.DarkDialog,
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
    if (existing != null && existing.id != 0)
      for (Planning.Bill b : plan.bills)
        if (b.paid == existing.id) {
          toast("Fatura kaydı: değişiklik için Ödeme takviminde ödemeyi geri al");
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
    List<Finance.Account> bankAccounts = new ArrayList<>(), creditAccounts = new ArrayList<>();
    for (Finance.Account a : accounts)
      (plan.cardAccounts.contains(a.id) ? creditAccounts : bankAccounts).add(a);
    Spinner paymentMethod = spinner(Arrays.asList("Banka / nakit", "Kredi kartı"));
    TextView paymentLabel = text("Ödeme yöntemi", 12, MUTED, true);
    add(form, paymentLabel);
    add(form, paymentMethod);
    TextView cardSourceLabel = text("Ödeme kartı", 12, MUTED, true);
    add(form, cardSourceLabel);
    Spinner creditSource = spinner(creditAccounts);
    add(form, creditSource);
    TextView sourceLabel = text("Banka / nakit kaynak hesabı", 12, MUTED, true);
    add(form, sourceLabel);
    Spinner source = spinner(bankAccounts);
    add(form, source);
    if (existing != null)
      for (int i = 0; i < accounts.size(); i++)
        if (accounts.get(i).id == existing.accountId) {
          if (plan.cardAccounts.contains(existing.accountId)) {
            paymentMethod.setSelection(1);
            for (int j = 0; j < creditAccounts.size(); j++)
              if (creditAccounts.get(j).id == existing.accountId) creditSource.setSelection(j);
          } else
            for (int j = 0; j < bankAccounts.size(); j++)
              if (bankAccounts.get(j).id == existing.accountId) source.setSelection(j);
        }
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
    boolean[] firstSource = {true};
    Runnable updateType =
        () -> {
          boolean transfer = type.getSelectedItemPosition() == 2;
          boolean cardExpense =
              type.getSelectedItemPosition() == 0
                  && paymentMethod.getSelectedItemPosition() == 1
                  && !creditAccounts.isEmpty();
          paymentLabel.setVisibility(
              type.getSelectedItemPosition() == 0 && !creditAccounts.isEmpty()
                  ? View.VISIBLE
                  : View.GONE);
          paymentMethod.setVisibility(paymentLabel.getVisibility());
          creditSource.setVisibility(cardExpense ? View.VISIBLE : View.GONE);
          source.setVisibility(cardExpense ? View.GONE : View.VISIBLE);
          sourceLabel.setVisibility(source.getVisibility());
          sourceLabel.setText(transfer ? "Kaynak hesap" : "Banka / nakit kaynak hesabı");
          cardSourceLabel.setVisibility(creditSource.getVisibility());
          long selectedId =
              source.getSelectedItem() == null
                  ? 0
                  : ((Finance.Account) source.getSelectedItem()).id;
          if (firstSource[0] && existing != null) selectedId = existing.accountId;
          firstSource[0] = false;
          List<Finance.Account> sourceValues = transfer ? accounts : bankAccounts;
          source.setAdapter(spinner(sourceValues).getAdapter());
          for (int j = 0; j < sourceValues.size(); j++)
            if (sourceValues.get(j).id == selectedId) source.setSelection(j);
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
    paymentMethod.setOnItemSelectedListener(selected(pos -> updateType.run()));
    type.setOnItemSelectedListener(selected(pos -> updateType.run()));
    updateType.run();
    label(form, "Not · isteğe bağlı");
    EditText note = field("Bu işlem ne içindi?", existing == null ? "" : existing.note, false);
    note.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1000)});
    add(form, note);
    TextView validation = text("", 12, RED, false);
    space(form, 10);
    add(form, validation);
    AlertDialog d =
        dialog(
            existing == null || existing.id == 0 ? "Yeni işlem" : "İşlemi düzenle", form, "Kaydet");
    if (existing != null && existing.id != 0)
      d.setButton(
          AlertDialog.BUTTON_NEUTRAL,
          "Sil",
          (a, b) ->
              confirm(
                  "İşlem silinsin mi?",
                  "Bu kayıt silinecek ve bakiyeler yeniden hesaplanacak. Düzenli işlemin kuralı"
                      + " varsa devam eder.",
                  () -> {
                    featureAction(() -> showUndo(features.deleteWithUndo(existing.id), false));
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
                                ((Finance.Account)
                                        (kind.equals("E")
                                                    && creditSource.getVisibility() == View.VISIBLE
                                                ? creditSource
                                                : source)
                                            .getSelectedItem())
                                    .id,
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

  private void deleteAccountDialog(Finance.Account a, AlertDialog owner) {
    if (accounts.size() <= 1) {
      toast("En az bir hesap kalmalı. Önce yeni bir hesap ekle.");
      return;
    }
    long count = ledger.accountEntryCount(a.id);
    long bills = plan.bills.stream().filter(b -> b.account == a.id).count();
    int recurring = 0;
    try {
      JSONArray rules = ledger.recurring();
      for (int i = 0; i < rules.length(); i++)
        if (rules.getJSONObject(i).getLong("account_id") == a.id) recurring++;
    } catch (Exception e) {
      error("Bağlı kayıtlar okunamadı", e);
      return;
    }
    confirm(
        "Hesap kalıcı olarak silinsin mi?",
        a.name
            + " hesabıyla birlikte "
            + count
            + " işlem, "
            + bills
            + " fatura, "
            + recurring
            + " düzenli kural ve varsa kredi kartı takibi silinir.\n\n"
            + "Bu hesaba giren/çıkan transferler de silinir; diğer hesapların bakiyesi ve geçmiş"
            + " raporlar değişebilir. Geri almak için önceden aldığın JSON yedeği gerekir.",
        () -> {
          try {
            ledger.deleteAccount(a.id);
            if (owner != null) owner.dismiss();
            if (filterAccount == a.id) filterAccount = 0;
            refresh();
            toast("Hesap ve bağlı kayıtlar silindi");
          } catch (Exception e) {
            error("Hesap silinemedi", e);
          }
        });
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
    if (existing != null)
      d.setButton(-3, "Hesabı sil", (v, w) -> deleteAccountDialog(existing, null));
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
    new AlertDialog.Builder(this, (plan.light ? R.style.LightDialog : R.style.DarkDialog))
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
      new AlertDialog.Builder(this, (plan.light ? R.style.LightDialog : R.style.DarkDialog))
          .setTitle(title)
          .setMessage(message(e))
          .setPositiveButton("Tamam", null)
          .show();
  }

  private interface FeatureAction {
    void run() throws Exception;
  }

  private void featureAction(FeatureAction action) {
    try {
      action.run();
      refresh();
    } catch (Exception e) {
      error("İşlem tamamlanamadı", e);
    }
  }

  private EditText featureField(LinearLayout form, String title, String value, boolean number) {
    label(form, title);
    EditText input = field(title, value, number);
    add(form, input);
    return input;
  }

  private Button featureDate(LinearLayout form, String title, LocalDate[] value) {
    label(form, title);
    Button b = button(value[0].format(DATE), false, () -> {});
    b.setOnClickListener(
        v ->
            chooseDate(
                value[0],
                d -> {
                  value[0] = d;
                  b.setText(d.format(DATE));
                }));
    add(form, b);
    return b;
  }

  private void featureDialog(String title, LinearLayout form, FeatureAction save) {
    TextView validation = text("", 12, RED, false);
    add(form, validation);
    AlertDialog d = dialog(title, form, "Kaydet");
    d.setOnShowListener(
        v ->
            d.getButton(-1)
                .setOnClickListener(
                    b -> {
                      try {
                        save.run();
                        d.dismiss();
                        refresh();
                      } catch (Exception e) {
                        validation.setText(message(e));
                      }
                    }));
    d.show();
  }

  private List<Finance.Account> banks() {
    List<Finance.Account> result = new ArrayList<>();
    for (Finance.Account a : accounts) if (!plan.cardAccounts.contains(a.id)) result.add(a);
    return result;
  }

  private void selectAccount(Spinner s, List<Finance.Account> values, long id) {
    for (int i = 0; i < values.size(); i++) if (values.get(i).id == id) s.setSelection(i);
  }

  private void removeFeature(String type, long id) {
    confirm(
        "Takip kaydı silinsin mi?",
        "Gerçek işlemler ve hesap bakiyeleri korunur.",
        () -> featureAction(() -> features.remove(type, id)));
  }

  private void templateShortcuts() {
    if (plan.extras.templates.isEmpty()) return;
    section("Kayıtlı hızlı işlemler", "Tek dokunuşla bugüne ekle · 10 saniye içinde geri al");
    for (int i = 0; i < Math.min(4, plan.extras.templates.size()); i++) {
      Enhancements.Template t = plan.extras.templates.get(i);
      add(
          body,
          button(
              t.name + " · " + Finance.money(t.amount),
              false,
              () ->
                  featureAction(
                      () -> showUndo(features.applyTemplate(t.id, LocalDate.now()), true))));
      space(body, 6);
    }
    add(body, button("Şablonları yönet", false, () -> openPage(9)));
  }

  private <T> List<T> featureSlice(List<T> values, int slot) {
    featureOffsets[slot] =
        Math.min(featureOffsets[slot], Math.max(0, ((values.size() - 1) / 20) * 20));
    return values.subList(featureOffsets[slot], Math.min(values.size(), featureOffsets[slot] + 20));
  }

  private void featurePager(int count, int slot) {
    if (count <= 20) return;
    LinearLayout c = card(body);
    hint(
        c,
        (featureOffsets[slot] + 1)
            + "–"
            + Math.min(count, featureOffsets[slot] + 20)
            + " / "
            + count);
    if (featureOffsets[slot] > 0)
      add(
          c,
          button(
              "Önceki 20 kayıt",
              false,
              () -> {
                featureOffsets[slot] -= 20;
                render();
              }));
    if (featureOffsets[slot] + 20 < count)
      add(
          c,
          button(
              "Sonraki 20 kayıt",
              false,
              () -> {
                featureOffsets[slot] += 20;
                render();
              }));
  }

  private void templatesPage() {
    LinearLayout intro = card(body);
    cardTitle(intro, "repeat", "Hızlı işlem şablonları", BLUE);
    hint(
        intro,
        "Sık kullandığın gelir ve giderleri sakla. Kullan düğmesi bugüne tek kayıt ekler; tekrar"
            + " kayıt istemezsen Geri al'a dokun.");
    add(intro, button("+ Şablon ekle", true, () -> templateDialog(null)));
    for (Enhancements.Template t : featureSlice(plan.extras.templates, 0)) {
      LinearLayout c = card(body);
      cardTitle(c, "repeat", t.name, t.kind.equals("I") ? GREEN : RED);
      keyValue(c, t.kind.equals("I") ? "Gelir" : "Gider", Finance.money(t.amount), WHITE);
      hint(c, category(t.category) + " · " + account(t.account));
      add(
          c,
          button(
              "Bugüne ekle",
              true,
              () ->
                  featureAction(
                      () -> showUndo(features.applyTemplate(t.id, LocalDate.now()), true))));
      add(c, button("Düzenle", false, () -> templateDialog(t)));
      add(c, button("Şablonu sil", false, () -> removeFeature("template", t.id)));
    }
    featurePager(plan.extras.templates.size(), 0);
  }

  private void templateDialog(Enhancements.Template old) {
    LinearLayout f = column();
    EditText name = featureField(f, "Şablon adı", old == null ? "" : old.name, false);
    name.setFilters(new InputFilter[] {new InputFilter.LengthFilter(60)});
    label(f, "İşlem türü");
    Spinner kind = spinner(Arrays.asList("Gider", "Gelir"));
    add(f, kind);
    if (old != null && old.kind.equals("I")) kind.setSelection(1);
    EditText amount =
        featureField(f, "Tutar · TL", old == null ? "" : Finance.editable(old.amount), true);
    label(f, "Hesap / ödeme kartı");
    Spinner ac = spinner(accounts);
    add(f, ac);
    label(f, "Kategori");
    Spinner cat = spinner(ledger.categories("E"));
    add(f, cat);
    Runnable update =
        () -> {
          String k = kind.getSelectedItemPosition() == 1 ? "I" : "E";
          List<Finance.Account> as = k.equals("I") ? banks() : accounts;
          ac.setAdapter(spinner(as).getAdapter());
          if (old != null) selectAccount(ac, as, old.account);
          List<Finance.Category> cs = ledger.categories(k);
          cat.setAdapter(spinner(cs).getAdapter());
          if (old != null)
            for (int i = 0; i < cs.size(); i++)
              if (cs.get(i).id == old.category) cat.setSelection(i);
        };
    kind.setOnItemSelectedListener(selected(p -> update.run()));
    update.run();
    EditText note = featureField(f, "Açıklama", old == null ? "" : old.note, false);
    note.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1000)});
    featureDialog(
        "İşlem şablonu",
        f,
        () -> {
          if (ac.getSelectedItem() == null || cat.getSelectedItem() == null)
            throw new IllegalArgumentException(
                "Önce bu işlem türü için uygun bir hesap ve kategori ekle.");
          Enhancements.Template t = new Enhancements.Template();
          t.id = old == null ? 0 : old.id;
          t.name = name.getText().toString().trim();
          t.kind = kind.getSelectedItemPosition() == 1 ? "I" : "E";
          t.amount = Finance.parseMoney(amount.getText().toString());
          t.account = ((Finance.Account) ac.getSelectedItem()).id;
          t.category = ((Finance.Category) cat.getSelectedItem()).id;
          t.note = note.getText().toString().trim();
          features.saveTemplate(t);
        });
  }

  private void goalsPage() {
    LinearLayout intro = card(body);
    cardTitle(intro, "target", "Birikim hedeflerin", GREEN);
    hint(
        intro,
        "Her hedefin ayrı bir birikim hesabı var. Para ayırmak hesaplar arası transferdir; gelir"
            + " veya gider değildir. Hedef hesabındaki gerçek bakiye ilerlemeyi belirler.");
    add(intro, button("+ Hedef ekle", true, () -> goalDialog(null)));
    for (Enhancements.Goal g : featureSlice(plan.extras.goals, 1)) {
      LinearLayout c = card(body);
      cardTitle(c, "target", g.name, GREEN);
      long funded = accountBalances.getOrDefault(g.account, 0L),
          remaining = Math.max(0, g.target - funded);
      keyValue(
          c, "Birikmiş / hedef", Finance.money(funded) + " / " + Finance.money(g.target), WHITE);
      add(c, new Design.SpendingMeter(this, 100d * funded / g.target, plan.light));
      hint(c, account(g.account) + " · " + Finance.money(remaining) + " kaldı");
      if (g.deadline != null)
        hint(
            c,
            "Hedef tarihi: "
                + g.deadline.format(DATE)
                + (g.deadline.isBefore(LocalDate.now()) && remaining > 0 ? " · tarih geçti" : ""));
      long months =
          Insights.goalMonths(
              goalHistory,
              g.account,
              remaining,
              Finance.cycleMonth(LocalDate.now(), plan.payday),
              plan.payday);
      hint(
          c,
          months == 0
              ? "Hedef tamamlandı"
              : months < 0
                  ? "Tamamlanma tahmini için 3 tamamlanmış maaş döneminde pozitif net birikim"
                      + " gerekli."
                  : "Son 3 tamamlanmış maaş döneminin net transfer ortalamasıyla yaklaşık "
                      + months
                      + " dönem kaldı; garanti değildir.");
      add(c, button("Para ayır", true, () -> goalMove(g, true)));
      add(c, button("Birikimden geri aktar", false, () -> goalMove(g, false)));
      add(c, button("Hedefi düzenle", false, () -> goalDialog(g)));
      add(c, button("Hedef takibini sil", false, () -> removeFeature("goal", g.id)));
    }
    featurePager(plan.extras.goals.size(), 1);
  }

  private void goalDialog(Enhancements.Goal old) {
    LinearLayout f = column();
    EditText name = featureField(f, "Hedef adı", old == null ? "" : old.name, false);
    name.setFilters(new InputFilter[] {new InputFilter.LengthFilter(60)});
    EditText target =
        featureField(f, "Hedef tutar · TL", old == null ? "" : Finance.editable(old.target), true);
    List<Finance.Account> values = new ArrayList<>();
    values.add(new Finance.Account(0, "Yeni birikim hesabı oluştur", 0));
    for (Finance.Account a : banks())
      if (plan.extras.goals.stream()
          .noneMatch(g -> g.account == a.id && (old == null || g.id != old.id))) values.add(a);
    label(f, "Hedefe özel hesap");
    Spinner ac = spinner(values);
    add(f, ac);
    if (old != null) {
      selectAccount(ac, values, old.account);
      ac.setEnabled(false);
    }
    Switch deadline = new Switch(this);
    deadline.setText(R.string.goal_deadline);
    deadline.setTextColor(WHITE);
    deadline.setChecked(old != null && old.deadline != null);
    add(f, deadline);
    LocalDate[] date = {
      old != null && old.deadline != null ? old.deadline : LocalDate.now().plusMonths(6)
    };
    Button dateButton = featureDate(f, "Hedef tarihi", date);
    dateButton.setEnabled(deadline.isChecked());
    deadline.setOnCheckedChangeListener((v, on) -> dateButton.setEnabled(on));
    featureDialog(
        "Birikim hedefi",
        f,
        () -> {
          Enhancements.Goal g = new Enhancements.Goal();
          g.id = old == null ? 0 : old.id;
          g.name = name.getText().toString().trim();
          g.target = Finance.parseMoney(target.getText().toString());
          g.account = ((Finance.Account) ac.getSelectedItem()).id;
          g.deadline = deadline.isChecked() ? date[0] : null;
          features.saveGoal(g);
        });
  }

  private void goalMove(Enhancements.Goal g, boolean deposit) {
    LinearLayout f = column();
    List<Finance.Account> values = banks();
    values.removeIf(a -> a.id == g.account);
    if (values.isEmpty()) {
      toast("Önce başka bir banka / nakit hesabı ekle.");
      return;
    }
    label(f, deposit ? "Kaynak hesap" : "Paranın aktarılacağı hesap");
    Spinner ac = spinner(values);
    add(f, ac);
    EditText amount = featureField(f, "Transfer tutarı · TL", "", true);
    hint(f, "Bu hareket gelir veya gider olarak sayılmaz. Kaynak hesap bakiyesi yeterli olmalı.");
    featureDialog(
        deposit ? "Hedefe para ayır" : "Birikimi geri aktar",
        f,
        () ->
            features.moveGoal(
                g.id,
                ((Finance.Account) ac.getSelectedItem()).id,
                Finance.parseMoney(amount.getText().toString()),
                deposit));
  }

  private void purchasesPage() {
    LinearLayout intro = card(body);
    cardTitle(intro, "card", "Kart alışveriş taksitleri", BLUE);
    hint(
        intro,
        "Kart ekstrenden bağımsız takip listesi. Ödendi işaretlemek gider, transfer veya ekstre"
            + " değişikliği oluşturmaz. Kart borcunun gerçek ödemesini kart ekranından yönet.");
    add(intro, button("+ Alışveriş taksiti ekle", true, () -> purchaseDialog(null)));
    for (Enhancements.Purchase p : featureSlice(plan.extras.purchases, 2)) {
      LinearLayout c = card(body);
      cardTitle(c, "card", p.name, BLUE);
      hint(c, account(p.account));
      keyValue(c, "Aylık taksit", Finance.money(p.amount), WHITE);
      keyValue(c, "Ödenen / toplam", p.paid + " / " + p.count, WHITE);
      keyValue(c, "Kalan yük", Finance.money(p.remaining()), WHITE);
      if (p.paid < p.count) {
        hint(c, "Sıradaki taksit: " + p.nextDue().format(DATE));
        add(
            c,
            button(
                "Bir taksiti ödendi işaretle · takip",
                true,
                () -> featureAction(() -> features.markPurchase(p.id, p.paid, 1))));
      } else hint(c, "Tüm taksitler tamamlandı");
      if (p.paid > 0)
        add(
            c,
            button(
                "Son işareti geri al",
                false,
                () -> featureAction(() -> features.markPurchase(p.id, p.paid, -1))));
      add(c, button("Düzenle", false, () -> purchaseDialog(p)));
      add(c, button("Taksit takibini sil", false, () -> removeFeature("purchase", p.id)));
    }
    featurePager(plan.extras.purchases.size(), 2);
  }

  private void purchaseDialog(Enhancements.Purchase old) {
    List<Finance.Account> values = new ArrayList<>();
    for (Finance.Account a : accounts) if (plan.cardAccounts.contains(a.id)) values.add(a);
    if (values.isEmpty()) {
      toast("Önce kredi kartı ekle.");
      return;
    }
    LinearLayout f = column();
    EditText name = featureField(f, "Alışveriş adı", old == null ? "" : old.name, false);
    name.setFilters(new InputFilter[] {new InputFilter.LengthFilter(60)});
    label(f, "Kredi kartı");
    Spinner ac = spinner(values);
    add(f, ac);
    if (old != null) selectAccount(ac, values, old.account);
    EditText amount =
        featureField(
            f, "Aylık taksit tutarı · TL", old == null ? "" : Finance.editable(old.amount), true);
    EditText count =
        featureField(
            f, "Toplam taksit · 1–360", old == null ? "" : Integer.toString(old.count), true);
    EditText paid =
        featureField(
            f, "Ödenmiş taksit sayısı", old == null ? "0" : Integer.toString(old.paid), true);
    LocalDate[] date = {old == null ? LocalDate.now() : old.first};
    featureDate(f, "İlk taksit tarihi", date);
    featureDialog(
        "Kart alışveriş taksiti",
        f,
        () -> {
          Enhancements.Purchase p = new Enhancements.Purchase();
          p.id = old == null ? 0 : old.id;
          p.name = name.getText().toString().trim();
          p.account = ((Finance.Account) ac.getSelectedItem()).id;
          p.amount = Finance.parseMoney(amount.getText().toString());
          p.count = Integer.parseInt(count.getText().toString());
          p.paid = Integer.parseInt(paid.getText().toString());
          p.first = date[0];
          features.savePurchase(p);
        });
  }

  private void cardDatesDialog(Planning.Card card) {
    if (card.updated == null) {
      toast("Önce bankadaki tutarları güncelle.");
      return;
    }
    LinearLayout f = column();
    LocalDate[] close = {card.statementClosing()}, due = {card.statementDue()};
    featureDate(f, "Bankanın gerçek Hesap Kesim Tarihi", close);
    featureDate(f, "Bankanın gerçek Son Ödeme Tarihi", due);
    hint(
        f,
        "Bu ekstreye özel tarihler ödemelerde ve hatırlatmalarda kullanılır. Aylık varsayılan"
            + " günler korunur; son 24 tarih kaydı saklanır.");
    for (Map.Entry<LocalDate, LocalDate[]> record : card.dates.descendingMap().entrySet())
      hint(
          f,
          record.getKey().format(DATE)
              + " dönemi · kesim "
              + record.getValue()[0].format(DATE)
              + " · ödeme "
              + record.getValue()[1].format(DATE));
    add(
        f,
        button(
            "Bu ekstre için varsayılan günlere dön",
            false,
            () -> featureAction(() -> features.clearCardDates(card.account))));
    featureDialog(
        "Ekstreye özel tarihler", f, () -> features.saveCardDates(card.account, close[0], due[0]));
  }

  private void showUndo(FeatureStore.Snapshot snapshot, boolean added) {
    undoSnapshot = snapshot;
    undoAdded = added;
    undoUntil = android.os.SystemClock.elapsedRealtime() + 10000;
    long token = undoUntil;
    ui.postDelayed(
        () -> {
          if (!isDestroyed() && undoUntil == token) {
            undoSnapshot = null;
            render();
          }
        },
        10000);
  }

  private void undoBanner() {
    if (undoSnapshot == null || android.os.SystemClock.elapsedRealtime() >= undoUntil) {
      undoSnapshot = null;
      return;
    }
    LinearLayout banner = row();
    banner.setPadding(dp(16), dp(4), dp(16), dp(4));
    weighted(banner, text(undoAdded ? "İşlem eklendi" : "İşlem silindi", 12, WHITE, true));
    banner.addView(
        button(
            "Geri al",
            false,
            () ->
                featureAction(
                    () -> {
                      if (android.os.SystemClock.elapsedRealtime() >= undoUntil) {
                        undoSnapshot = null;
                        toast("Geri alma süresi doldu.");
                        return;
                      }
                      if (undoAdded) features.undoAdded(undoSnapshot);
                      else features.restoreDeleted(undoSnapshot);
                      undoSnapshot = null;
                    })));
    add(root, banner);
  }

  private void enhancedForecast() {
    if (!month.equals(Finance.cycleMonth(LocalDate.now(), plan.payday)) || forecastData == null)
      return;
    Insights.Forecast f = forecastData;
    LinearLayout c = card(body);
    cardTitle(c, "chart", "Dönem sonu görünümü", BLUE);
    keyValue(c, "Kaydedilmiş sabit gider", Finance.money(f.fixedExpense), WHITE);
    keyValue(c, "Değişken gider", Finance.money(f.variableExpense), WHITE);
    keyValue(c, "Bekleyen / planlı gider", Finance.money(f.plannedExpense), WHITE);
    keyValue(c, "Planlı gelecek gelir", Finance.money(f.plannedIncome), GREEN);
    if (f.ready) {
      keyValue(c, "Tahmini dönem sonu gider", Finance.money(f.projectedExpense), WHITE);
      keyValue(
          c,
          "Tahmini dönem dengesi",
          Finance.money(f.projectedNet),
          f.projectedNet < 0 ? RED : GREEN);
    } else
      hint(
          c,
          "Tahmin için veri az: en az 7 gün ve 3 farklı günde gider kaydı gerekli. Bilinen ödemeler"
              + " yukarıda gösteriliyor.");
    keyValue(c, "Günlük kullanılabilir", Finance.money(f.dailyAvailable), GREEN);
    hint(
        c,
        "Sabit gider: yinelenen kayıtlar ve gider olarak ödenmiş faturalar. Tahmin = mevcut gider +"
            + " planlı gider + değişken günlük ortalama × kalan gün. Tek seferlik harcamalar sonucu"
            + " etkileyebilir; garanti değildir.");
    hint(
        c,
        "Günlük tutar, hedef birikim payı ayrılmış gelir bütçesi ile likit hesap bakiyesinin küçük"
            + " olanına göre hesaplanır. Birikim hesapları ve ödeme tarihine kadar gereken "
            + Finance.money(f.reservedCards)
            + " ekstre rezervi kullanılabilir nakitten ayrılır. Gelecek gelir, gerçekleşene kadar"
            + " harcanabilir sayılmaz.");
  }

  private void categoryInsights() {
    section("Kategori hareketleri", "Maaş dönemleri · son 6 dönem · çubuğa dokun");
    List<Finance.Category> cs = ledger.categories("E");
    if (cs.isEmpty()) return;
    LinearLayout c = card(body);
    Spinner pick = spinner(cs);
    add(c, pick);
    LinearLayout chart = column(), detail = column();
    add(c, chart);
    add(c, detail);
    Runnable draw =
        () -> {
          chart.removeAllViews();
          detail.removeAllViews();
          Finance.Category category = (Finance.Category) pick.getSelectedItem();
          if (category == null) return;
          long[] totals = new long[6];
          LocalDate today = LocalDate.now();
          long max = 1;
          for (int i = 0; i < 6; i++) {
            YearMonth m = month.minusMonths(5 - i);
            totals[i] =
                Finance.byCategory(
                        Finance.period(entries, periodStart(m), effectiveEnd(periodEnd(m))), "E")
                    .getOrDefault(category.id, 0L);
            max = Math.max(max, totals[i]);
          }
          for (int i = 0; i < 6; i++) {
            final int index = i;
            YearMonth m = month.minusMonths(5 - i);
            LinearLayout r = row();
            r.setMinimumHeight(dp(48));
            TextView name =
                text(m.format(DateTimeFormatter.ofPattern("MMM yy", Finance.TR)), 11, MUTED, false);
            r.addView(name, new LinearLayout.LayoutParams(dp(60), -2));
            View bar = new View(this);
            bar.setBackground(shape(tint(BLUE, 180), 4));
            LinearLayout.LayoutParams bp =
                new LinearLayout.LayoutParams(
                    0, dp(12), (float) Math.max(.015, (double) totals[i] / max));
            r.addView(bar, bp);
            View rest = new View(this);
            r.addView(
                rest,
                new LinearLayout.LayoutParams(
                    0, dp(12), (float) Math.max(.001, 1d - (double) totals[i] / max)));
            r.setContentDescription(m.format(MONTH) + ": " + Finance.money(totals[i]));
            r.setOnClickListener(
                v -> {
                  detail.removeAllViews();
                  hint(detail, category.name + " · " + m.format(MONTH));
                  keyValue(detail, "Toplam gider", Finance.money(totals[index]), WHITE);
                  hint(
                      detail,
                      periodStart(m).format(DATE)
                          + " – "
                          + effectiveEnd(periodEnd(m)).format(DATE)
                          + (periodEnd(m).isAfter(today) ? " · dönem henüz tamamlanmadı" : ""));
                  if (index > 0 && totals[index - 1] > 0 && !periodEnd(m).isAfter(today))
                    hint(
                        detail,
                        "Önceki döneme göre "
                            + signedPct(Finance.change(totals[index], totals[index - 1])));
                  if (totals[index] == 0)
                    hint(
                        detail,
                        "Bu aralıkta kayıt yok; eksik veriler sıfır harcama olduğunu kanıtlamaz.");
                });
            add(chart, r);
          }
        };
    pick.setOnItemSelectedListener(selected(p -> draw.run()));
    draw.run();
    List<Insights.Anomaly> anomalies =
        Insights.anomalies(entries, month, plan.payday, LocalDate.now());
    for (int i = 0; i < Math.min(3, anomalies.size()); i++) {
      Insights.Anomaly a = anomalies.get(i);
      LinearLayout warning = card(body);
      cardTitle(warning, "chart", category(a.category) + " artışı", RED);
      hint(
          warning,
          Finance.money(a.current)
              + " · "
              + a.periods
              + " kayıtlı önceki dönemin karşılaştırılabilir gün ortalaması "
              + Finance.money(a.average)
              + ". Yaklaşık "
              + String.format(Finance.TR, "%.1f", a.ratio)
              + " kat. Tek seferlik alım veya eksik kayıt olup olmadığını kontrol et.");
    }
    if (anomalies.isEmpty())
      hint(
          c,
          "En az 3 geçmiş dönemde kategori kaydı varsa, karşılaştırılabilir günlerde ortalamanın 2"
              + " katını aşan giderler burada belirtilir.");
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
            fileFinished(success, null, request == IMPORT_JSON);
          } catch (Exception e) {
            fileFinished(null, e, false);
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
