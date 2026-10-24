package com.semih.nobet;

import static com.semih.nobet.Roster.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class MainActivity extends Activity implements CloudController.Host {
  private CloudController cloud;
  private final Handler dayHandler = new Handler(Looper.getMainLooper());
  private LocalDate displayedDate = LocalDate.now();
  private final Runnable refreshDay =
      () -> {
        LocalDate now = LocalDate.now();
        if (!now.equals(displayedDate)) {
          displayedDate = now;
          if (this.root != null && this.screen == 0) renderKeepingPosition();
        }
        long delay =
            java.time.Duration.between(
                    java.time.ZonedDateTime.now(),
                    now.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()))
                .toMillis();
        dayHandler.postDelayed(this.refreshDay, Math.max(1000, delay + 1000));
      };
  private State state;
  private Store store;
  private LinearLayout root, body, tabs;
  private ScrollView pageScroll;
  private LocalDate selected = monday(LocalDate.now());
  private int screen = 0;
  private TextView notice;
  private String noticeText = "";
  private int noticeToken;
  private boolean failed = false;
  private Week draft;
  private ArrayList<String> weekendOrderDraft;
  private final Set<String> expandedLeavePeople = new HashSet<>(),
      expandedLeaveTypes = new HashSet<>();
  private int ink = Color.rgb(19, 38, 60),
      teal = Color.rgb(42, 111, 177),
      muted = Color.rgb(85, 105, 124),
      surface = Color.rgb(237, 245, 253),
      cardColor = Color.WHITE,
      warningColor = Color.rgb(155, 83, 0);
  private int themeMode;
  private boolean darkMode;
  private int lineColor = 0xFFDCE7F4;
  private final DateTimeFormatter shortDate = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  @Override
  public void onCreate(Bundle b) {
    themeMode = getSharedPreferences("appearance", 0).getInt("theme", 0);
    boolean dark =
        themeMode == 2
            || (themeMode == 0
                && (getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES);
    darkMode = dark;
    setTheme(dark ? R.style.AppThemeDark : R.style.AppTheme);
    if (dark) {
      ink = Color.rgb(231, 238, 247);
      teal = Color.rgb(137, 187, 235);
      muted = Color.rgb(174, 189, 209);
      surface = 0xFF0E1829;
      lineColor = 0xFF293D56;
      cardColor = Color.rgb(26, 42, 62);
      warningColor = Color.rgb(255, 194, 112);
    }
    super.onCreate(b);
    store = new Store(this);
    try {
      state = store.load();
    } catch (Exception e) {
      state = new State();
      failed = true;
    }
    cloud = new CloudController(this, this);
    if (b != null) {
      try {
        if (b.containsKey("draft")
            && cloud.key().equals(b.getString("cloudKey", "local"))
            && (!cloud.active()
                || (b.getLong("cloudRevision", -1) == cloud.revision
                    && "admin".equals(cloud.role))))
          draft = Store.decode(b.getString("draft")).weeks.get(0);
      } catch (Exception ignored) {
      }
      selected = LocalDate.parse(b.getString("week", selected.toString()));
      screen = b.getInt("screen", 0);
      if (b.getStringArrayList("expandedLeavePeople") != null)
        expandedLeavePeople.addAll(b.getStringArrayList("expandedLeavePeople"));
      if (b.getStringArrayList("expandedLeaveTypes") != null)
        expandedLeaveTypes.addAll(b.getStringArrayList("expandedLeaveTypes"));
      if (cloud.key().equals(b.getString("cloudKey", "local"))
          && (!cloud.active()
              || (b.getLong("cloudRevision", -1) == cloud.revision && "admin".equals(cloud.role))))
        weekendOrderDraft = b.getStringArrayList("weekendOrderDraft");
    }
    render();
    if (failed)
      new AlertDialog.Builder(this)
          .setTitle("Veriler Okunamadı")
          .setMessage(
              "Kayıt dosyası açılamadı. Üzerine yazılmasını önlemek için düzenleme kapatıldı."
                  + " Uygulama verilerini silmeyin.")
          .setPositiveButton("Tamam", null)
          .show();
  }

  @Override
  public void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putStringArrayList("expandedLeavePeople", new ArrayList<>(expandedLeavePeople));
    b.putStringArrayList("expandedLeaveTypes", new ArrayList<>(expandedLeaveTypes));
    b.putString("cloudKey", cloud == null ? "local" : cloud.key());
    b.putLong("cloudRevision", cloud != null && cloud.active() ? cloud.revision : -1);
    b.putString("week", selected.toString());
    b.putInt("screen", screen);
    if (weekendOrderDraft != null) b.putStringArrayList("weekendOrderDraft", weekendOrderDraft);
    if (draft != null)
      try {
        State temp = new State();
        temp.weeks.add(draft);
        b.putString("draft", Store.encode(temp));
      } catch (Exception ignored) {
      }
  }

  @Override
  public State cloudState() {
    return state;
  }

  @Override
  public void cloudState(State next) {
    state = next;
    draft = null;
    weekendOrderDraft = null;
  }

  @Override
  public boolean cloudDraft() {
    return draft != null || weekendOrderDraft != null;
  }

  @Override
  public State localState() throws Exception {
    return store.load();
  }

  @Override
  public void cloudRender() {
    if (root != null) renderKeepingPosition();
  }

  @Override
  public void cloudNotice(String message) {
    toast(message);
  }

  @Override
  public void onResume() {
    super.onResume();
    if (cloud != null) cloud.start();
    dayHandler.removeCallbacks(refreshDay);
    refreshDay.run();
  }

  @Override
  public void onPause() {
    dayHandler.removeCallbacks(refreshDay);
    if (cloud != null) cloud.stop();
    super.onPause();
  }

  @Override
  public void onDestroy() {
    if (cloud != null) cloud.destroy();
    super.onDestroy();
  }

  private boolean mayEdit() {
    if (cloud == null || cloud.canEdit()) return true;
    toast(cloud.editDeniedMessage());
    return false;
  }

  private int dp(float n) {
    return (int) (getResources().getDisplayMetrics().density * n + .5f);
  }

  private TextView text(String str, int size, int color) {
    TextView v = new TextView(this);
    v.setText(str);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setPadding(0, dp(3), 0, dp(3));
    v.setFontFeatureSettings("kern");
    return v;
  }

  private LinearLayout column() {
    LinearLayout view = new LinearLayout(this);
    view.setOrientation(LinearLayout.VERTICAL);
    return view;
  }

  private LinearLayout row() {
    LinearLayout view = new LinearLayout(this);
    view.setGravity(Gravity.CENTER_VERTICAL);
    return view;
  }

  private LinearLayout panel() {
    LinearLayout view = column();
    view.setPadding(dp(14), dp(14), dp(14), dp(14));
    view.setBackground(Visuals.surface(cardColor, dp(20), lineColor));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.setMargins(0, dp(5), 0, dp(7));
    body.addView(view, lp);
    return view;
  }

  private TextView bold(String value, int size, int color) {
    TextView view = text(value, size, color);
    view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    return view;
  }

  private void heading(String value) {
    TextView view = bold(value, 20, ink);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.topMargin = dp(12);
    lp.bottomMargin = dp(5);
    body.addView(view, lp);
  }

  private void note(String value) {
    TextView view = text(value, 12, muted);
    view.setLineSpacing(dp(2), 1);
    body.addView(view);
  }

  private Button button(String value, Runnable action) {
    Button b = new Button(this);
    b.setText(value);
    b.setAllCaps(false);
    b.setTextSize(13);
    b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    b.setTextColor(Color.WHITE);
    b.setBackgroundTintList(null);
    b.setStateListAnimator(null);
    b.setElevation(0);
    b.setBackground(Visuals.touch(0xFF2A6FB1, dp(14), 0));
    b.setMinHeight(dp(48));
    b.setMinimumHeight(dp(48));
    b.setPadding(dp(14), dp(8), dp(14), dp(8));
    b.setOnClickListener(
        v -> {
          if (failed) {
            toast("Kayıtlar okunamadığı için değişiklikler kapalı.");
            return;
          }
          if (cloud != null && !cloud.canEdit() && !CloudController.readAction(value)) {
            toast(cloud.editDeniedMessage());
            return;
          }
          action.run();
        });
    return b;
  }

  private void soft(Button b, String icon) {
    b.setTextColor(ink);
    b.setBackground(Visuals.touch(cardColor, dp(14), lineColor));
    b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
    b.setCompoundDrawablesWithIntrinsicBounds(
        new Visuals.Icon(icon, teal, dp(19)), null, new Visuals.Icon("right", muted, dp(14)), null);
    b.setCompoundDrawablePadding(dp(10));
  }

  private void action(String value, Runnable action) {
    actionTo(body, value, action);
  }

  private void noteTo(LinearLayout parent, String value) {
    TextView view = text(value, 12, muted);
    view.setLineSpacing(dp(2), 1);
    parent.addView(view);
  }

  private void actionTo(LinearLayout parent, String value, Runnable action) {
    Button b = button(value, action);
    boolean primary =
        value.startsWith("+")
            || value.startsWith("Otomatik")
            || value.contains("Kaydet")
            || value.contains("Onayla")
            || value.contains("PDF");
    String icon =
        value.contains("PDF")
            ? "pdf"
            : value.contains("Kaydet") || value.contains("Onayla")
                ? "check"
                : value.startsWith("+")
                    ? "plus"
                    : value.startsWith("Otomatik")
                        ? "spark"
                        : value.contains("Sıra")
                            ? "people"
                            : value.contains("Şube") || value.contains("Kuralları")
                                ? "building"
                                : value.contains("Mazeret") ? "leave" : "edit";
    if (primary) {
      b.setCompoundDrawablesWithIntrinsicBounds(
          new Visuals.Icon(icon, Color.WHITE, dp(18)), null, null, null);
      b.setCompoundDrawablePadding(dp(8));
    } else soft(b, icon);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.setMargins(0, dp(4), 0, dp(4));
    parent.addView(b, lp);
  }

  private void appearance() {
    new AlertDialog.Builder(this)
        .setTitle("Kurumsal Renk Teması")
        .setSingleChoiceItems(
            new String[] {"Sistem Teması", "Açık • Kurumsal Mavi", "Koyu • Kurumsal Mavi"},
            themeMode,
            (dialog, which) -> {
              getSharedPreferences("appearance", 0).edit().putInt("theme", which).apply();
              dialog.dismiss();
              recreate();
            })
        .setNegativeButton("Vazgeç", null)
        .show();
  }

  private void toast(String s) {
    noticeText = s;
    int token = ++noticeToken;
    if (notice != null) {
      notice.setText(s);
      notice.setVisibility(View.VISIBLE);
    }
    new Handler(Looper.getMainLooper())
        .postDelayed(
            () -> {
              if (token == noticeToken) {
                noticeText = "";
                if (notice != null) notice.setVisibility(View.GONE);
              }
            },
            9000);
  }

  private AlertDialog confirm(String title, String message, Runnable r) {
    return new AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setNegativeButton("Vazgeç", null)
        .setPositiveButton("Onayla", (d, w) -> r.run())
        .show();
  }

  private boolean save() {
    if (cloud != null && cloud.active()) {
      boolean saved = cloud.stage(state);
      if (!saved) cloud.restoreVisible();
      return saved;
    }
    try {
      store.save(state);
      return true;
    } catch (Exception e) {
      failed = true;
      toast("Kaydedilemedi. Verileri korumak için düzenleme durduruldu; uygulamayı yeniden açın.");
      return false;
    }
  }

  private Week saved() {
    return state.weeks.stream().filter(w -> w.monday.equals(selected)).findFirst().orElse(null);
  }

  private String range() {
    return selected.format(shortDate) + " – " + selected.plusDays(6).format(shortDate);
  }

  private void render() {
    root = column();
    root.setBackgroundColor(surface);
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          root.setPadding(
              0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
          return insets;
        });
    LinearLayout header = row();
    header.setPadding(dp(18), dp(7), dp(18), dp(6));
    ImageView mark = new ImageView(this);
    mark.setImageDrawable(new Visuals.Icon("calendar", Color.WHITE, dp(23)));
    mark.setPadding(dp(9), dp(9), dp(9), dp(9));
    mark.setBackground(Visuals.surface(0xFF2A6FB1, dp(13), 0));
    header.addView(mark, new LinearLayout.LayoutParams(dp(42), dp(42)));
    LinearLayout brand = column();
    brand.setPadding(dp(11), 0, 0, 0);
    brand.addView(bold("Nöbet Çizelgesi", 20, ink));
    String[] subtitles = {
      "Haftalık Planlama",
      "Ekip Yönetimi",
      "Uygunluk Ve Kısıtlar",
      "Görev Dağılımı",
      "Uygulama Ayarları"
    };
    brand.addView(text(subtitles[screen], 11, muted));
    header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
    ImageButton theme = new ImageButton(this);
    theme.setOnClickListener(v -> appearance());
    theme.setTag("theme-control");
    theme.setContentDescription("Renk Teması Seç • Şu An: " + (darkMode ? "Koyu" : "Açık"));
    theme.setTooltipText("Renk Teması");
    theme.setPadding(dp(12), dp(12), dp(12), dp(12));
    theme.setScaleType(ImageView.ScaleType.CENTER);
    theme.setImageDrawable(new Visuals.Icon(darkMode ? "sun" : "moon", teal, dp(24)));
    theme.setBackground(Visuals.touch(cardColor, dp(16), lineColor));
    header.addView(theme, new LinearLayout.LayoutParams(dp(48), dp(48)));
    root.addView(header);
    if (cloud != null) cloud.banner(root, darkMode);
    pageScroll = new ScrollView(this);
    pageScroll.setClipToPadding(false);
    pageScroll.setVerticalScrollBarEnabled(false);
    body = column();
    body.setPadding(dp(18), dp(5), dp(18), dp(22));
    pageScroll.setFillViewport(true);
    pageScroll.addView(body);
    root.addView(pageScroll, new LinearLayout.LayoutParams(-1, 0, 1));
    notice = text(noticeText, 13, Color.WHITE);
    notice.setPadding(dp(14), dp(12), dp(14), dp(12));
    notice.setBackground(Visuals.surface(0xFF2A6FB1, dp(16), 0));
    notice.setVisibility(noticeText.isEmpty() ? View.GONE : View.VISIBLE);
    LinearLayout.LayoutParams noticeParams = new LinearLayout.LayoutParams(-1, -2);
    noticeParams.setMargins(dp(18), 0, dp(18), dp(6));
    root.addView(notice, noticeParams);
    tabs = row();
    tabs.setPadding(dp(5), dp(5), dp(5), dp(5));
    tabs.setBackground(Visuals.surface(cardColor, dp(24), lineColor));
    tabs.setElevation(dp(3));
    tabs.setTag("bottom-navigation");
    String[] titles = {"Çizelge", "Personel", "Mazeret", "İstatistik", "Ayarlar"};
    String[] icons = {"calendar", "people", "leave", "chart", "settings"};
    for (int i = 0; i < titles.length; i++) {
      final int index = i;
      Button tab =
          button(
              titles[i],
              () -> {
                screen = index;
                render();
              });
      tab.setTag("nav-" + i);
      tab.setSelected(i == screen);
      int color = i == screen ? Color.WHITE : muted;
      tab.setTextColor(color);
      tab.setTextSize(10);
      tab.setMinWidth(0);
      tab.setMinimumWidth(0);
      tab.setMinHeight(0);
      tab.setMinimumHeight(0);
      tab.setPadding(0, dp(5), 0, dp(3));
      tab.setGravity(Gravity.CENTER);
      tab.setBackground(Visuals.touch(i == screen ? 0xFF2A6FB1 : Color.TRANSPARENT, dp(19), 0));
      tab.setCompoundDrawablesWithIntrinsicBounds(
          null, new Visuals.Icon(icons[i], color, dp(21)), null, null);
      tab.setCompoundDrawablePadding(dp(3));
      tabs.addView(tab, new LinearLayout.LayoutParams(0, dp(52), 1));
    }
    LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(-1, -2);
    navParams.setMargins(dp(14), dp(4), dp(14), dp(9));
    root.addView(tabs, navParams);
    setContentView(root);
    if (screen == 0) program();
    else if (screen == 1) people();
    else if (screen == 2) leaves();
    else if (screen == 3) stats();
    else settings();
  }

  private void renderKeepingPosition() {
    int y = pageScroll == null ? 0 : pageScroll.getScrollY();
    String anchor = null;
    int offset = 0;
    if (pageScroll != null) {
      for (int i = 0; i < body.getChildCount(); i++) {
        View child = body.getChildAt(i);
        if (child.getTag() instanceof String
            && ((String) child.getTag()).startsWith("roster-day-")
            && child.getBottom() > y
            && child.getTop() < y + pageScroll.getHeight()) {
          anchor = (String) child.getTag();
          offset = child.getTop() - y;
          break;
        }
      }
    }
    render();
    ScrollView restored = pageScroll;
    LinearLayout content = body;
    String tag = anchor;
    int topOffset = offset;
    restored.setFocusableInTouchMode(true);
    restored.requestFocus();
    restored
        .getViewTreeObserver()
        .addOnPreDrawListener(
            new android.view.ViewTreeObserver.OnPreDrawListener() {
              @Override
              public boolean onPreDraw() {
                restored.getViewTreeObserver().removeOnPreDrawListener(this);
                View target = tag == null ? null : content.findViewWithTag(tag);
                restored.scrollTo(0, target == null ? y : Math.max(0, target.getTop() - topOffset));
                return true;
              }
            });
  }

  private String proper(String value) {
    return value.isEmpty()
        ? value
        : value.substring(0, 1).toUpperCase(new Locale("tr", "TR")) + value.substring(1);
  }

  private TextView chip(String value, boolean onBlue) {
    TextView view = bold(value, 10, onBlue ? Color.WHITE : teal);
    view.setPadding(dp(9), dp(5), dp(9), dp(5));
    view.setBackground(
        Visuals.surface(onBlue ? 0x20000000 : darkMode ? 0xFF233E5A : 0xFFE9F2FD, dp(20), 0));
    return view;
  }

  private void weekHero() {
    Week week = draft != null ? draft : saved();
    LinearLayout hero = column();
    hero.setTag("week-hero");
    hero.setPadding(dp(18), dp(16), dp(18), dp(16));
    hero.setBackground(new Visuals.Hero(dp(24)));
    LinearLayout top = row();
    TextView caption = bold("HAFTALIK NÖBET PLANI", 10, 0xFFDCEBFF);
    caption.setLetterSpacing(.09f);
    top.addView(caption, new LinearLayout.LayoutParams(0, -2, 1));
    top.addView(chip(draft != null ? "Taslak" : week != null ? "Onaylı" : "Yeni Plan", true));
    hero.addView(top);
    String person =
        week == null
            ? ""
            : week.duties.stream()
                .filter(d -> d.place.equals(WORK) && weekend(d.date))
                .map(d -> state.name(d.person))
                .findFirst()
                .orElse("");
    TextView label =
        text(
            person.isEmpty() ? "Dengeli Bir Hafta Planla" : "Hafta Sonu Nöbetçisi", 12, 0xFFDCEBFF);
    label.setPadding(0, dp(14), 0, dp(1));
    hero.addView(label);
    hero.addView(
        bold(
            person.isEmpty()
                ? (state.people.stream().anyMatch(p -> p.active)
                    ? "Yeni Haftaya Hazırsın"
                    : "Önce Ekibini Ekle")
                : person,
            23,
            Color.WHITE));
    LinearLayout metrics = row();
    metrics.setPadding(0, dp(14), 0, 0);
    String[] values = {
      Long.toString(state.people.stream().filter(p -> p.active).count()),
      Integer.toString(week == null ? 0 : week.duties.size()),
      Long.toString(state.places.stream().filter(p -> !p.deleted).count())
    };
    String[] names = {"Aktif Personel", "Atama", "Görev Yeri"};
    for (int i = 0; i < 3; i++) {
      LinearLayout metric = column();
      metric.addView(bold(values[i], 21, Color.WHITE));
      metric.addView(text(names[i], 10, 0xFFDCEBFF));
      metrics.addView(metric, new LinearLayout.LayoutParams(0, -2, 1));
    }
    hero.addView(metrics);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.setMargins(0, dp(12), 0, dp(9));
    body.addView(hero, lp);
  }

  private void scheduleTable(Week savedWeek) {
    scheduleTable(savedWeek, LocalDate.now());
  }

  private void scheduleTable(Week savedWeek, LocalDate today) {
    Week week = savedWeek == null ? new Week(selected) : savedWeek;
    List<Place> places = displayPlaces(state, week);
    TextView caption = bold("Haftalık Nöbet Planı", 16, ink);
    caption.setPadding(0, dp(4), 0, 0);
    body.addView(caption);
    int groupSize =
        getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density
                < 400
            ? 2
            : 3;
    for (int part = 0; part < Math.max(1, places.size()); part += groupSize) {
      List<Place> group = places.subList(part, Math.min(places.size(), part + groupSize));
      LinearLayout header = row();
      header.setBaselineAligned(false);
      header.setTag(part == 0 ? "schedule-table-header" : "schedule-table-header-" + part);
      header.setBackground(Visuals.surface(0xFF2A6FB1, dp(10), 0));
      scheduleCell(header, "Gün / Tarih", true, true, null);
      for (Place place : group)
        scheduleCell(header, place.name + (place.deleted ? " (Silindi)" : ""), false, true, null);
      LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
      hp.topMargin = dp(4);
      body.addView(header, hp);
      for (int index = 0; index < 6; index++) {
        final int number = index;
        boolean current = ScheduleTable.isToday(week, index, today);
        int todayInk = darkMode ? 0xFFFFE7AD : 0xFF634616;
        LocalDate day = week.monday.plusDays(index);
        LinearLayout line = row();
        line.setBaselineAligned(false);
        line.setTag("roster-day-" + day + (part == 0 ? "" : "-part" + part));
        line.setBackground(
            current
                ? new Visuals.TodayRow(darkMode, dp(1), dp(4))
                : new Visuals.TableRow(
                    index % 2 == 0 ? cardColor : (darkMode ? 0xFF182B42 : 0xFFE7F1FD), dp(1)));
        String label =
            index == 5
                ? "Hafta Sonu\nNöbetçisi"
                : proper(
                        day.getDayOfWeek()
                            .getDisplayName(
                                java.time.format.TextStyle.FULL, new Locale("tr", "TR")))
                    + "\n"
                    + day.format(DateTimeFormatter.ofPattern("dd.MM"));
        TextView dayCell = scheduleCell(line, label, true, false, null);
        if (!Holidays.on(state, day).isEmpty()
            || (index == 5 && !Holidays.on(state, day.plusDays(1)).isEmpty()))
          dayCell.setTextColor(teal);
        if (current) {
          android.text.SpannableString marked = new android.text.SpannableString(label + "\nBugün");
          marked.setSpan(
              new Visuals.TodayBadge(
                  9 * getResources().getDisplayMetrics().scaledDensity, darkMode),
              label.length() + 1,
              marked.length(),
              android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
          dayCell.setText(marked);
          dayCell.setPadding(dp(5), dp(2), dp(5), dp(2));
          dayCell.setLineSpacing(0, .95f);
          dayCell.setTextColor(todayInk);
          dayCell.setTypeface(null, Typeface.BOLD);
          line.setContentDescription(label.replace('\n', ' ') + " • Bugün");
        }
        for (Place place : group) {
          String value = ScheduleTable.person(state, week, index, place.id);
          Runnable editAction =
              savedWeek == null || place.deleted
                  ? null
                  : () -> {
                    if (number < 5) edit(day, place.id);
                    else if (place.id.equals(WORK)) edit(day, place.id);
                    else
                      new AlertDialog.Builder(this)
                          .setTitle(place.name + " • Hafta Sonu")
                          .setItems(
                              new String[] {"Cumartesi", "Pazar"},
                              (d, which) -> edit(day.plusDays(which), place.id))
                          .setNegativeButton("Vazgeç", null)
                          .show();
                  };
          TextView cell = scheduleCell(line, value, false, false, editAction);
          if (current) {
            cell.setTextColor(todayInk);
            cell.setTypeface(null, Typeface.BOLD);
          }
          cell.setTag("assignment-" + day + "-" + place.id);
          cell.setContentDescription(
              label.replace('\n', ' ')
                  + " • "
                  + place.name
                  + ": "
                  + value
                  + (current ? ", Bugün" : "")
                  + (editAction == null ? "" : ", Düzenle"));
        }
        body.addView(line, new LinearLayout.LayoutParams(-1, -2));
      }
    }
  }

  private TextView scheduleCell(
      LinearLayout line, String value, boolean day, boolean header, Runnable change) {
    TextView cell =
        change == null
            ? text(value, header ? 11 : 12, header ? Color.WHITE : ink)
            : button(value, change);
    cell.setTextSize(header ? 11 : 12);
    cell.setTextColor(header ? Color.WHITE : ink);
    cell.setGravity(Gravity.CENTER);
    cell.setMinWidth(0);
    cell.setMinimumWidth(0);
    cell.setPadding(dp(5), dp(7), dp(5), dp(7));
    cell.setMinHeight(dp(header ? 36 : 48));
    if (header) cell.setTypeface(null, Typeface.BOLD);
    if (change != null) {
      cell.setBackground(Visuals.touch(Color.TRANSPARENT, 0, 0));
    }
    line.addView(cell, new LinearLayout.LayoutParams(0, -1, day ? 1.05f : 1.5f));
    return cell;
  }

  private void dayHeader(LinearLayout card, LocalDate day) {
    LinearLayout header = row();
    LinearLayout badge = column();
    badge.setGravity(Gravity.CENTER);
    badge.setBackground(Visuals.surface(darkMode ? 0xFF243F5D : 0xFFEAF3FF, dp(12), 0));
    TextView date = bold(Integer.toString(day.getDayOfMonth()), 21, teal);
    date.setPadding(0, 0, 0, 0);
    date.setGravity(Gravity.CENTER);
    badge.addView(date);
    TextView month =
        bold(
            day.format(DateTimeFormatter.ofPattern("MMM", new Locale("tr", "TR")))
                .toUpperCase(new Locale("tr", "TR")),
            9,
            teal);
    month.setPadding(0, 0, 0, 0);
    month.setGravity(Gravity.CENTER);
    badge.addView(month);
    header.addView(badge, new LinearLayout.LayoutParams(dp(44), dp(49)));
    LinearLayout title = column();
    title.setPadding(dp(12), 0, dp(6), 0);
    title.addView(
        bold(
            proper(
                day.getDayOfWeek()
                    .getDisplayName(java.time.format.TextStyle.FULL, new Locale("tr", "TR"))),
            16,
            ink));
    title.addView(text(weekend(day) ? "Hafta Sonu" : "Hafta İçi", 11, muted));
    header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
    if (day.equals(LocalDate.now())) header.addView(chip("Bugün", false));
    card.addView(header);
  }

  private String weekCaption() {
    LocalDate end = selected.plusDays(6);
    Locale tr = new Locale("tr", "TR");
    String endText =
        end.getDayOfMonth()
            + " "
            + proper(end.getMonth().getDisplayName(java.time.format.TextStyle.FULL, tr))
            + " "
            + end.getYear();
    if (selected.getYear() != end.getYear())
      return selected.getDayOfMonth()
          + " "
          + proper(selected.getMonth().getDisplayName(java.time.format.TextStyle.FULL, tr))
          + " "
          + selected.getYear()
          + " – "
          + endText;
    if (selected.getMonth() != end.getMonth())
      return selected.getDayOfMonth()
          + " "
          + proper(selected.getMonth().getDisplayName(java.time.format.TextStyle.FULL, tr))
          + " – "
          + endText;
    return selected.getDayOfMonth() + " – " + endText;
  }

  private AlertDialog weekendList() {
    List<WeekendList.Entry> entries = WeekendList.entries(state);
    if (entries.isEmpty()) {
      return new AlertDialog.Builder(this)
          .setTitle("Hafta Sonu Nöbet Çizelgesi")
          .setMessage(
              "Henüz kayıtlı hafta yok. Önce haftalık çizelgeyi kaydedin; hafta sonu nöbetçileri"
                  + " burada tarih sırasıyla görünecek.")
          .setPositiveButton("Tamam", null)
          .show();
    }
    return new AlertDialog.Builder(this)
        .setTitle("Hafta Sonu Nöbet Çizelgesi")
        .setItems(
            entries.stream().map(e -> e.label).toArray(String[]::new),
            (dialog, index) -> {
              Runnable open =
                  () -> {
                    selected = entries.get(index).monday;
                    draft = null;
                    render();
                  };
              if (draft != null)
                confirm(
                    "Kayıtlı Haftaya Geç",
                    "Kaydedilmemiş taslak kapanacak. Seçilen kayıtlı hafta açılsın mı?",
                    open);
              else open.run();
            })
        .setNegativeButton("Kapat", null)
        .show();
  }

  private void program() {
    LinearLayout nav = new LinearLayout(this);
    nav.addView(
        button(
            "‹ Önceki",
            () -> {
              selected = selected.minusWeeks(1);
              draft = null;
              render();
            }),
        new LinearLayout.LayoutParams(0, dp(52), 1));
    nav.addView(
        button(
            "Hafta Seç",
            () ->
                date(
                    selected,
                    d -> {
                      selected = monday(d);
                      draft = null;
                      render();
                    })),
        new LinearLayout.LayoutParams(0, dp(52), 1));
    nav.addView(
        button(
            "Sonraki ›",
            () -> {
              selected = selected.plusWeeks(1);
              draft = null;
              render();
            }),
        new LinearLayout.LayoutParams(0, dp(52), 1));
    nav.setBackground(Visuals.surface(cardColor, dp(18), lineColor));
    nav.setPadding(dp(2), dp(2), dp(2), dp(2));
    for (int i = 0; i < 3; i++) {
      Button control = (Button) nav.getChildAt(i);
      control.setBackground(Visuals.touch(Color.TRANSPARENT, dp(14), 0));
      control.setTextColor(ink);
      control.setMinWidth(0);
      control.setMinimumWidth(0);
      control.setPadding(dp(3), 0, dp(3), 0);
      if (i == 1) {
        control.setText(weekCaption());
        control.setTextSize(12);
        control.setContentDescription("Hafta Seç • " + range());
        control.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1));
      } else {
        control.setText(i == 0 ? "‹" : "›");
        control.setTextSize(26);
        control.setContentDescription(i == 0 ? "Önceki Hafta" : "Sonraki Hafta");
        control.setLayoutParams(new LinearLayout.LayoutParams(dp(39), dp(48)));
      }
    }
    body.addView(nav);
    scheduleTable(draft != null ? draft : saved());
    action("Hafta Sonu Nöbet Çizelgesi ▾", this::weekendList);
    action("Çizelgede Gösterilecek Şubeler", () -> chooseVisiblePlaces(null));
    int shortcutStart = body.getChildCount() - 1;

    if (state.people.stream().noneMatch(p -> p.active)) {
      note(
          "Başlamak için Personel bölümüne 5 kişinin adını ekleyin. Sonradan ekleyebilir, pasife"
              + " alabilir veya yeniden etkinleştirebilirsiniz.");
      action(
          "Personel Ekle",
          () -> {
            screen = 1;
            render();
          });
      return;
    }
    action(
        "Otomatik Dağıtım Öner",
        () ->
            chooseVisiblePlaces(
                () -> {
                  try {
                    draft = generate(state, selected);
                    render();
                  } catch (IllegalArgumentException e) {
                    toast(e.getMessage());
                  }
                }));
    action(
        "Boş Çizelge Aç • Manuel Oluştur",
        () ->
            chooseVisiblePlaces(
                () ->
                    confirm(
                        "Manuel Çizelge",
                        "Bu hafta için boş bir öneri açılacak. Mevcut kayıt, siz onaylayana kadar"
                            + " değişmeyecek. Günlerdeki işyeri/şube alanlarına dokunarak personel"
                            + " seçin.",
                        () -> {
                          draft = new Week(selected);
                          draft.rotationRevision = state.rotationRevision;
                          Week previous =
                              state.weeks.stream()
                                  .filter(x -> x.monday.isBefore(selected))
                                  .max(Comparator.comparing(x -> x.monday))
                                  .orElse(null);
                          if (previous != null) {
                            if (previous.rotationRevision == state.rotationRevision)
                              draft.cursor = previous.cursor;
                            for (Debt d : previous.debts)
                              draft.debts.add(new Debt(d.person, d.end));
                          }
                          draft.warnings.add(
                              "Manuel çizelge: boş alanları ve mazeretleri kontrol ederek"
                                  + " onaylayın.");
                          render();
                        })));
    LinearLayout shortcuts = row();
    String[] quickLabels = {"Şubeler", "Otomatik Dağıt", "Manuel Liste"};
    String[] quickIcons = {"building", "spark", "edit"};
    for (int i = 0; i < 3; i++) {
      Button control = (Button) body.getChildAt(shortcutStart);
      body.removeViewAt(shortcutStart);
      control.setContentDescription(control.getText());
      control.setText(quickLabels[i]);
      control.setTextSize(11);
      control.setGravity(Gravity.CENTER);
      control.setMinWidth(0);
      control.setMinimumWidth(0);
      control.setPadding(dp(4), dp(10), dp(4), dp(9));
      control.setCompoundDrawablesWithIntrinsicBounds(
          null, new Visuals.Icon(quickIcons[i], i == 1 ? Color.WHITE : teal, dp(22)), null, null);
      control.setCompoundDrawablePadding(dp(5));
      LinearLayout.LayoutParams quickParams = new LinearLayout.LayoutParams(0, dp(79), 1);
      quickParams.setMargins(i == 0 ? 0 : dp(6), 0, 0, 0);
      shortcuts.addView(control, quickParams);
    }
    LinearLayout.LayoutParams quickParams = new LinearLayout.LayoutParams(-1, -2);
    quickParams.bottomMargin = dp(5);
    body.addView(shortcuts, shortcutStart, quickParams);
    Week w = draft != null ? draft : saved();
    if (w == null) {
      note("Bu hafta için henüz onaylı çizelge yok.");
      return;
    }
    boolean preview = draft != null;
    heading(preview ? "Onay Bekleyen Öneri" : "Onaylı Çizelge");
    if (!preview)
      action(
          "Kaydedilmiş Listeyi Düzenle",
          () -> {
            draft = copy(w);
            renderKeepingPosition();
            toast("Gün/yer alanlarına dokunup düzenleyin; ardından değişiklikleri kaydedin.");
          });
    for (String warning : w.warnings) body.addView(text("• " + warning, 14, warningColor));
    if (preview) {
      action(
          saved() != null ? "Değişiklikleri Kaydet" : "Dağıtımı Onayla Ve Kaydet",
          () -> approveDraft());
      action(
          "Öneriyi İptal Et",
          () -> {
            draft = null;
            render();
          });
    }
    action("Hafta Sonu Nöbetçisini Atla / Mazeret Gir", () -> skip(w));
    action("Bu Hafta " + state.placeName(BRANCH) + " Muafiyeti", () -> branchExcuse());
    if (!preview)
      action(
          "PDF Oluştur Ve Paylaş",
          () -> {
            if (!validate(state, w).isEmpty()) {
              toast("Çizelgede personel/mazeret çakışması var; yeniden dağıtın.");
              return;
            }
            try {
              PdfExport.share(this, PdfExport.create(this, state, w));
            } catch (Exception e) {
              toast("PDF oluşturulamadı veya paylaşım uygulaması bulunamadı.");
            }
          });
    List<Debt> waiting = preview ? w.debts : pendingDebts(state, selected.plusWeeks(1));
    if (!waiting.isEmpty()) {
      heading("Bekleyen Hafta Sonu Sırası");
      for (Debt d : waiting)
        note(
            state.name(d.person)
                + " • mazeret sonu "
                + d.end.format(shortDate)
                + " • ilk uygun hafta sonu öncelikli");
    }
    note(
        "Onay, çizelge planını kaydeder. İstatistikte geçmiş/bugünkü nöbetler ile gelecek atamalar"
            + " ayrı gösterilir. Gerçekleşmeyen nöbetleri çizelgede düzeltin.");
  }

  private void approveDraft() {
    if (!mayEdit()) return;
    if (draft == null) return;
    List<String> errors = validate(state, draft);
    if (!errors.isEmpty()) {
      toast(String.join("\n", errors));
      return;
    }
    long missing = Math.max(0, expectedSlots(state) - draft.duties.size());
    String msg =
        "Atamalar Ayarlar'daki seçili günlere ve haftalık sınırlara göre yapılır. Ana yerde iki"
            + " hafta sonu günü de seçiliyse aynı kişi atanır; bu kişi perşembe/cuma izinlidir."
            + " Gizli şubelerin atamaları ve istatistikleri korunur.\n\n"
            + (missing > 0
                ? missing + " boş nöbet alanı var. Eksik çizelge olarak kaydedilecek.\n\n"
                : "")
            + String.join("\n", draft.warnings);
    if (state.weeks.stream().anyMatch(w -> w.monday.isAfter(selected)))
      msg +=
          "\n\n"
              + "Yalnız bu hafta güncellenecek; diğer onaylı haftalar korunacak. İstatistikler"
              + " güncel çizelgelerden yeniden hesaplanır.";
    confirm(
        "Dağıtımı Teyit Edin",
        msg,
        () -> {
          try {
            approve(state, draft);
            if (save()) {
              draft = null;
              render();
              toast("Çizelge onaylandı.");
            }
          } catch (IllegalArgumentException e) {
            toast(e.getMessage());
          }
        });
  }

  private void people() {
    heading("Personel");
    note(
        "Hafta sonu sırasını Ayarlar → Hafta Sonu Nöbet Sırası bölümünden düzenleyin. Pasif"
            + " personel yeni atamalarda kullanılmaz.");
    action("+ Personel Ekle", () -> personDialog(null));
    action(
        "Nöbet Sırasını Düzenle",
        () -> {
          screen = 4;
          render();
        });
    for (Person p : state.people) {
      LinearLayout card = panel();
      LinearLayout identity = row();
      String[] words = p.name.trim().split("\\s+");
      String initials =
          words[0].substring(0, 1)
              + (words.length > 1 ? words[words.length - 1].substring(0, 1) : "");
      TextView avatar = bold(initials.toUpperCase(new Locale("tr", "TR")), 16, teal);
      avatar.setGravity(Gravity.CENTER);
      avatar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      avatar.setBackground(Visuals.surface(darkMode ? 0xFF243E5B : 0xFFE7F1FE, dp(14), 0));
      identity.addView(avatar, new LinearLayout.LayoutParams(dp(45), dp(45)));
      LinearLayout title = column();
      title.setPadding(dp(11), 0, dp(6), 0);
      title.addView(bold(p.name, 15, ink));
      int rank = activeWeekendIds().indexOf(p.id) + 1;
      title.addView(
          text(
              rank > 0 ? "Hafta Sonu • " + rank + ". Sıra" : "Geçmiş Kayıtları Korunuyor",
              11,
              muted));
      identity.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
      identity.addView(chip(p.active ? "Aktif" : "Pasif", false));
      card.addView(identity);
      LinearLayout row = new LinearLayout(this);
      row.addView(button("Adı Düzenle", () -> personDialog(p)));
      row.addView(
          button(
              p.active ? "Personeli Çıkar" : "Yeniden Etkinleştir",
              () ->
                  confirm(
                      "Personel Durumu",
                      p.active
                          ? "Personel pasife alınacak; geçmişi korunacak. Bugün ve sonraki"
                              + " atamaları kaldırılacak; yeniden dağıtım gerekecek."
                          : "Personel yeniden atamalara katılacak.",
                      () -> {
                        p.active = !p.active;
                        if (!p.active) {
                          for (Week wk : state.weeks) {
                            wk.duties.removeIf(
                                d -> d.person.equals(p.id) && !d.date.isBefore(LocalDate.now()));
                            wk.debts.removeIf(d -> d.person.equals(p.id));
                          }
                        }
                        if (save()) {
                          draft = null;
                          render();
                        }
                      })));
      for (int i = 0; i < row.getChildCount(); i++) {
        Button control = (Button) row.getChildAt(i);
        soft(control, i == 0 ? "edit" : "people");
        control.setTextSize(11);
        control.setCompoundDrawablesWithIntrinsicBounds(
            new Visuals.Icon(i == 0 ? "edit" : "people", teal, dp(15)), null, null, null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(i == 0 ? 0 : dp(5), dp(12), 0, 0);
        control.setLayoutParams(lp);
      }
      card.addView(row);
    }
    note(
        "Yerel liste cihazda tutulur. Ortak ekip etkinse kabul edilen değişiklikler bağlı sunucuyla"
            + " paylaşılır. Uygulamayı kaldırmak cihazdaki kayıtları ve oturumları silebilir.");
  }

  private void personDialog(Person p) {
    if (!mayEdit()) return;
    EditText input = new EditText(this);
    input.setSingleLine(true);
    input.setHint("Ad Soyad");
    if (p != null) input.setText(p.name);
    LinearLayout fields = new LinearLayout(this);
    fields.setOrientation(LinearLayout.VERTICAL);
    fields.setPadding(dp(18), 0, dp(18), 0);
    fields.addView(input);
    List<String> ids = p == null ? editableWeekendIds() : activeWeekendIds();
    EditText position = new EditText(this);
    position.setSingleLine(true);
    position.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    position.setHint("Hafta Sonu Sıra Numarası");
    position.setText(
        getString(R.string.limit_value, p == null ? ids.size() + 1 : ids.indexOf(p.id) + 1));
    if (p == null) {
      fields.addView(text("Hafta Sonu Nöbet Sırası (1–" + (ids.size() + 1) + ")", 14, ink));
      fields.addView(position);
      fields.addView(
          text(
              "Yeni sıra sonraki otomatik öneride 1. sıradan başlar. Bekleyen mazeret hakları"
                  + " korunur.",
              13,
              muted));
    }
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(p == null ? "Personel Ekle" : "Adı Düzenle")
            .setView(fields)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Kaydet", null)
            .create();
    dialog.setOnShowListener(
        x ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      String name = input.getText().toString().trim().replaceAll("\\s+", " ");
                      if (name.isEmpty() || name.length() > 60) {
                        input.setError("1–60 karakter girin.");
                        return;
                      }
                      if (state.people.stream()
                          .anyMatch(person -> person != p && person.name.equalsIgnoreCase(name))) {
                        input.setError("Bu isim zaten var; ayırt edici bir ad girin.");
                        return;
                      }
                      if (p == null) {
                        int rank;
                        try {
                          rank = Integer.parseInt(position.getText().toString());
                          if (rank < 1 || rank > ids.size() + 1) throw new NumberFormatException();
                        } catch (NumberFormatException e) {
                          position.setError("1–" + (ids.size() + 1) + " arasında sıra girin.");
                          return;
                        }
                        Person added = new Person(UUID.randomUUID().toString(), name);
                        state.people.add(added);
                        ids.add(rank - 1, added.id);
                        setWeekendOrder(state, ids);
                        weekendOrderDraft = null;
                      } else p.name = name;
                      if (save()) {
                        dialog.dismiss();
                        draft = null;
                        render();
                      }
                    }));
    dialog.show();
  }

  private void leaves() {
    heading("Mazeret Ve Kısıtlar");
    note(
        "Aynı personele birden fazla mazeret veya kısıt ekleyebilirsiniz. Tarih aralığı ya da her"
            + " hafta tekrarlanan günler seçin. Genel mazeret tüm yerleri, yer kısıtı yalnız"
            + " seçtiğiniz yeri engeller.");
    action(
        "+ Mazeret / Kısıt Ekle",
        () -> leaveDialog(null, ALL, selected, selected.plusDays(6), "Senelik izin"));
    java.text.Collator collator = java.text.Collator.getInstance(new Locale("tr", "TR"));
    Set<String> ids = new LinkedHashSet<>();
    for (Person p : state.people) if (p.active) ids.add(p.id);
    for (Leave l : state.leaves) ids.add(l.person);
    List<String> people = new ArrayList<>(ids);
    people.sort((a, b) -> collator.compare(state.name(a), state.name(b)));
    for (String id : people) {
      List<Leave> records = new ArrayList<>();
      for (Leave l : state.leaves) if (l.person.equals(id)) records.add(l);
      LinearLayout card = panel();
      LinearLayout details = column();
      details.setTag("leave-person-details-" + id);
      Button person =
          accordion(
              card,
              details,
              state.name(id) + " • " + records.size() + " Kayıt",
              id,
              expandedLeavePeople);
      person.setTag("leave-person-" + id);
      Map<String, List<Leave>> types = new TreeMap<>(collator);
      for (Leave l : records)
        types
            .computeIfAbsent(
                l.days == 0 ? proper(l.reason) : "Haftalık Kısıt", key -> new ArrayList<>())
            .add(l);
      for (Map.Entry<String, List<Leave>> entry : types.entrySet()) {
        LinearLayout typeDetails = column();
        String key = id + "|" + entry.getKey();
        typeDetails.setTag("leave-type-details-" + key);
        Button type =
            accordion(
                details,
                typeDetails,
                entry.getKey() + " • " + entry.getValue().size(),
                key,
                expandedLeaveTypes);
        type.setTag("leave-type-" + key);
        entry
            .getValue()
            .sort(Comparator.comparing((Leave l) -> l.start).reversed().thenComparing(l -> l.end));
        for (Leave l : entry.getValue()) leaveCard(typeDetails, l);
        details.addView(typeDetails);
      }
      if (records.isEmpty()) details.addView(text("Kayıtlı Mazeret Veya Kısıt Yok.", 13, muted));
      card.addView(details);
    }
  }

  private Button accordion(
      LinearLayout parent, LinearLayout details, String title, String key, Set<String> expanded) {
    Button toggle = button(title, () -> {});
    toggle.setAllCaps(false);
    toggle.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
    toggle.setPadding(dp(10), dp(8), dp(10), dp(8));
    toggle.setTextColor(ink);
    toggle.setBackground(Visuals.touch(Color.TRANSPARENT, dp(10), 0));
    Runnable update =
        () -> {
          boolean open = expanded.contains(key);
          details.setVisibility(open ? View.VISIBLE : View.GONE);
          toggle.setCompoundDrawablesWithIntrinsicBounds(
              null,
              null,
              new Visuals.Icon(open ? "chevron-up" : "chevron-down", teal, dp(20)),
              null);
          toggle.setContentDescription(title + (open ? " • Daralt" : " • Genişlet"));
        };
    toggle.setOnClickListener(
        v -> {
          if (!expanded.add(key)) expanded.remove(key);
          update.run();
        });
    update.run();
    parent.addView(toggle, new LinearLayout.LayoutParams(-1, -2));
    return toggle;
  }

  private void leaveCard(LinearLayout parent, Leave l) {
    LinearLayout card = column();
    card.setPadding(dp(6), dp(6), dp(6), dp(6));
    parent.addView(card);
    TextView t =
        text(
            state.name(l.person)
                + " • "
                + l.reason
                + "\n"
                + leavePeriod(l)
                + " • "
                + state.placeName(l.place),
            14,
            ink);
    t.setLineSpacing(dp(3), 1);
    t.setCompoundDrawablesWithIntrinsicBounds(
        new Visuals.Icon("leave", teal, dp(23)), null, null, null);
    t.setCompoundDrawablePadding(dp(10));
    card.addView(t);
    LinearLayout row = new LinearLayout(this);
    row.addView(button("Düzenle", () -> leaveDialog(l, l.place, l.start, l.end, l.reason)));
    row.addView(
        button(
            "Sil",
            () ->
                confirm(
                    "Mazereti Sil",
                    "Bu mazeret kaldırılacak. Mevcut çizelgeleri yeniden üretmeden atamalar"
                        + " değişmez.",
                    () -> {
                      state.leaves.remove(l);
                      for (Week wk : state.weeks)
                        for (Debt debt : wk.debts)
                          if (debt.person.equals(l.person) && debt.end.equals(l.end))
                            debt.end = LocalDate.now().minusDays(1);
                      if (save()) {
                        draft = null;
                        render();
                      }
                    })));
    for (int i = 0; i < row.getChildCount(); i++) {
      Button control = (Button) row.getChildAt(i);
      soft(control, i == 0 ? "edit" : "leave");
      control.setCompoundDrawablesWithIntrinsicBounds(
          new Visuals.Icon(i == 0 ? "edit" : "leave", teal, dp(16)), null, null, null);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
      lp.setMargins(i == 0 ? 0 : dp(6), dp(10), 0, 0);
      control.setLayoutParams(lp);
    }
    card.addView(row);
  }

  private void date(LocalDate initial, java.util.function.Consumer<LocalDate> consumer) {
    new DatePickerDialog(
            this,
            (v, y, m, d) -> consumer.accept(LocalDate.of(y, m + 1, d)),
            initial.getYear(),
            initial.getMonthValue() - 1,
            initial.getDayOfMonth())
        .show();
  }

  private void dateTitled(
      String title, LocalDate initial, java.util.function.Consumer<LocalDate> consumer) {
    DatePickerDialog picker =
        new DatePickerDialog(
            this,
            (v, y, m, d) -> consumer.accept(LocalDate.of(y, m + 1, d)),
            initial.getYear(),
            initial.getMonthValue() - 1,
            initial.getDayOfMonth());
    picker.setTitle(title);
    picker.show();
  }

  private Spinner spinner(String[] values) {
    Spinner s = new Spinner(this);
    s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
    s.setMinimumHeight(dp(52));
    return s;
  }

  private String leavePeriod(Leave leave) {
    String period =
        leave.start.format(shortDate)
            + " – "
            + (leave.end.equals(LocalDate.MAX) ? "Süresiz" : leave.end.format(shortDate));
    if (leave.days == 0) return period;
    String[] names = {"Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"};
    List<String> selectedDays = new ArrayList<>();
    for (int i = 0; i < 7; i++)
      if ((leave.days & (1 << i)) != 0) selectedDays.add("Her " + names[i]);
    return String.join(", ", selectedDays)
        + (leave.end.equals(LocalDate.MAX) ? "" : " • Eski Tarihli Kayıt");
  }

  private final class WeeklyRestrictionRow {
    final LinearLayout view = new LinearLayout(MainActivity.this);
    final Spinner location;
    final CheckBox[] days = new CheckBox[7];

    WeeklyRestrictionRow(List<Place> locations, String selectedPlace, int mask) {
      view.setOrientation(LinearLayout.VERTICAL);
      view.setTag("weekly-restriction-row");
      view.setPadding(0, dp(6), 0, dp(6));
      List<String> names = new ArrayList<>();
      names.add("Tüm Görev Yerleri");
      for (Place p : locations) names.add(p.name);
      location = spinner(names.toArray(new String[0]));
      for (int i = 0; i < locations.size(); i++)
        if (locations.get(i).id.equals(selectedPlace)) location.setSelection(i + 1);
      view.addView(location);
      String[] weekdays = {
        "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"
      };
      for (int i = 0; i < 7; i += 2) {
        LinearLayout line = new LinearLayout(MainActivity.this);
        for (int j = i; j < Math.min(i + 2, 7); j++) {
          days[j] = new CheckBox(MainActivity.this);
          days[j].setText(getString(R.string.each_weekday, weekdays[j]));
          days[j].setTextSize(13);
          days[j].setTextColor(ink);
          days[j].setTag("leave-day-" + j);
          days[j].setChecked((mask & (1 << j)) != 0);
          line.addView(days[j], new LinearLayout.LayoutParams(0, -2, 1));
        }
        view.addView(line);
      }
    }

    int mask() {
      int value = 0;
      for (int i = 0; i < days.length; i++) if (days[i].isChecked()) value |= 1 << i;
      return value;
    }
  }

  private AlertDialog leaveDialog(
      Leave existing, String place, LocalDate start, LocalDate end, String reason) {
    if (!mayEdit()) return null;
    List<Person> list =
        state.people.stream()
            .filter(p -> p.active || existing != null && p.id.equals(existing.person))
            .collect(java.util.stream.Collectors.toList());
    if (list.isEmpty()) {
      toast("Önce personel ekleyin.");
      return null;
    }
    java.text.Collator leaveNames = java.text.Collator.getInstance(new Locale("tr", "TR"));
    list.sort((a, b) -> leaveNames.compare(a.name, b.name));
    LinearLayout box = new LinearLayout(this);
    box.setPadding(dp(20), dp(8), dp(20), dp(8));
    box.setOrientation(LinearLayout.VERTICAL);
    Spinner who = spinner(list.stream().map(p -> p.name).toArray(String[]::new));
    box.addView(who);
    if (existing != null)
      for (int i = 0; i < list.size(); i++)
        if (list.get(i).id.equals(existing.person)) who.setSelection(i);
    List<String> types = new ArrayList<>(Arrays.asList("Senelik İzin", "Rapor", "Diğer Mazeret"));
    List<Place> locations =
        state.places.stream()
            .filter(p -> !p.deleted || existing != null && p.id.equals(existing.place))
            .collect(java.util.stream.Collectors.toList());
    for (Place location : locations) types.add("Yalnız " + location.name + " kısıtı");
    Spinner type = spinner(types.toArray(new String[0]));
    int index = reason.equals("Rapor") ? 1 : reason.equalsIgnoreCase("Senelik izin") ? 0 : 2;
    if (!place.equals(ALL))
      for (int i = 0; i < locations.size(); i++)
        if (locations.get(i).id.equals(place)) index = 3 + i;
    type.setSelection(index);
    box.addView(type);
    LocalDate[] dates = {start, end};
    Button from = button("Başlangıç: " + start.format(shortDate), () -> {}),
        to = button("Bitiş: " + end.format(shortDate), () -> {});
    from.setOnClickListener(
        v ->
            date(
                dates[0],
                d -> {
                  dates[0] = d;
                  from.setText(getString(R.string.starts, d.format(shortDate)));
                }));
    to.setOnClickListener(
        v ->
            date(
                dates[1],
                d -> {
                  dates[1] = d;
                  to.setText(getString(R.string.ends, d.format(shortDate)));
                }));
    CheckBox weekly = new CheckBox(this);
    weekly.setText(R.string.weekly_repeat);
    weekly.setTextColor(ink);
    weekly.setTag("leave-weekly");
    weekly.setChecked(existing != null && existing.days != 0);
    box.addView(weekly);
    LinearLayout recurring = new LinearLayout(this);
    recurring.setOrientation(LinearLayout.VERTICAL);
    LinearLayout rowsBox = new LinearLayout(this);
    rowsBox.setOrientation(LinearLayout.VERTICAL);
    recurring.addView(rowsBox);
    List<WeeklyRestrictionRow> rows = new ArrayList<>();
    java.util.function.BiConsumer<String, Integer> addRow =
        (selectedPlace, mask) -> {
          WeeklyRestrictionRow row = new WeeklyRestrictionRow(locations, selectedPlace, mask);
          Button remove =
              button(
                  "− Kısıtı Kaldır",
                  () -> {
                    if (rows.size() == 1) {
                      toast("En az bir kısıt satırı kalmalı.");
                      return;
                    }
                    rows.remove(row);
                    rowsBox.removeView(row.view);
                  });
          remove.setTag("weekly-remove-row");
          row.view.addView(remove);
          rows.add(row);
          rowsBox.addView(row.view);
        };
    addRow.accept(place, existing == null ? 0 : existing.days);
    Button plus = button("+ Kısıt Ekle", () -> addRow.accept(ALL, 0));
    plus.setTag("weekly-add-row");
    recurring.addView(plus);
    box.addView(recurring);
    box.addView(from);
    box.addView(to);
    from.setTag("leave-start-date");
    to.setTag("leave-end-date");
    Runnable updateMode =
        () -> {
          recurring.setVisibility(weekly.isChecked() ? View.VISIBLE : View.GONE);
          type.setVisibility(weekly.isChecked() ? View.GONE : View.VISIBLE);
          from.setVisibility(weekly.isChecked() ? View.GONE : View.VISIBLE);
          to.setVisibility(weekly.isChecked() ? View.GONE : View.VISIBLE);
        };
    weekly.setOnCheckedChangeListener(
        (view, checked) -> {
          if (!checked && dates[1].equals(LocalDate.MAX)) {
            dates[0] = LocalDate.now();
            dates[1] = dates[0].plusDays(6);
            from.setText(getString(R.string.starts, dates[0].format(shortDate)));
            to.setText(getString(R.string.ends, dates[1].format(shortDate)));
          }
          updateMode.run();
        });
    updateMode.run();
    ScrollView fields = new ScrollView(this);
    fields.addView(box);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("Mazeret / Kısıt")
            .setView(fields)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Teyit Et", null)
            .create();
    dialog.setOnShowListener(
        x ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      String person = list.get(who.getSelectedItemPosition()).id;
                      List<Leave> newLeaves = new ArrayList<>();
                      if (weekly.isChecked()) {
                        for (int i = 0; i < rows.size(); i++) {
                          WeeklyRestrictionRow row = rows.get(i);
                          if (row.mask() == 0) {
                            toast((i + 1) + ". kısıt için en az bir gün seçin.");
                            return;
                          }
                          int selectedLocation = row.location.getSelectedItemPosition();
                          String target =
                              selectedLocation == 0 ? ALL : locations.get(selectedLocation - 1).id;
                          newLeaves.add(
                              new Leave(
                                  person,
                                  target,
                                  LocalDate.now(),
                                  LocalDate.MAX,
                                  "Haftalık Kısıt",
                                  row.mask()));
                        }
                      } else {
                        if (dates[1].isBefore(dates[0])) {
                          toast("Bitiş tarihi başlangıçtan önce olamaz.");
                          return;
                        }
                        int t = type.getSelectedItemPosition();
                        String target = t >= 3 ? locations.get(t - 3).id : ALL;
                        newLeaves.add(
                            new Leave(
                                person,
                                target,
                                dates[0],
                                dates[1],
                                type.getSelectedItem().toString()));
                      }
                      StringBuilder summary = new StringBuilder(state.name(person));
                      for (Leave leave : newLeaves)
                        summary
                            .append("\n\n")
                            .append(leavePeriod(leave))
                            .append("\n")
                            .append(state.placeName(leave.place));
                      confirm(
                          "Mazereti Kaydet",
                          summary
                              + "\n\n"
                              + "Seçili günlerde atanmayacak. Yalnız çakışan atamalar kaldırılır;"
                              + " boşalan yerleri yeniden planlayın.",
                          () -> {
                            if (existing != null) {
                              state.leaves.remove(existing);
                              for (Week wk : state.weeks)
                                for (Debt debt : wk.debts)
                                  if (debt.person.equals(existing.person)
                                      && debt.end.equals(existing.end))
                                    debt.end = LocalDate.now().minusDays(1);
                            }
                            for (Leave leave : newLeaves) {
                              state.leaves.add(leave);
                              removeConflicts(leave);
                            }
                            if (save()) {
                              dialog.dismiss();
                              draft = null;
                              render();
                              toast("Mazeret kaydedildi. Boşalan günler için dağıtım önerin.");
                            }
                          });
                    }));
    dialog.show();
    return dialog;
  }

  private void removeConflicts(Leave l) {
    for (Week w : state.weeks) {
      boolean affectedWeekend =
          w.duties.stream()
              .anyMatch(
                  d ->
                      weekend(d.date)
                          && d.place.equals(WORK)
                          && l.blocks(d.person, d.place, d.date));
      if (affectedWeekend) {
        String id =
            w.duties.stream()
                .filter(
                    d ->
                        weekend(d.date)
                            && d.place.equals(WORK)
                            && l.blocks(d.person, d.place, d.date))
                .map(d -> d.person)
                .findFirst()
                .orElse("");
        w.duties.removeIf(d -> weekend(d.date) && d.person.equals(id) && d.place.equals(WORK));
        if (w.debts.stream().noneMatch(d -> d.person.equals(id))) w.debts.add(new Debt(id, l.end));
      }
      w.duties.removeIf(d -> l.blocks(d.person, d.place, d.date));
    }
  }

  private void branchExcuse() {
    if (!mayEdit()) return;
    List<Person> list =
        state.people.stream().filter(p -> p.active).collect(java.util.stream.Collectors.toList());
    new AlertDialog.Builder(this)
        .setTitle("Bu Hafta Şubeye Gitmeyecek Kişi")
        .setItems(
            list.stream().map(p -> p.name).toArray(String[]::new),
            (d, i) ->
                confirm(
                    "Şube Muafiyetini Teyit Edin",
                    list.get(i).name
                        + " "
                        + range()
                        + " haftasında şubeye hiç atanmayacak. İşyeri için uygunluğu devam eder."
                        + " Yalnız çakışan şube atamaları"
                        + " kaldırılır.",
                    () -> {
                      Leave leave =
                          new Leave(
                              list.get(i).id,
                              BRANCH,
                              selected,
                              selected.plusDays(6),
                              "Haftalık şube muafiyeti");
                      state.leaves.add(leave);
                      removeConflicts(leave);
                      if (save()) {
                        draft = generate(state, selected);
                        render();
                      }
                    }))
        .setNegativeButton("Vazgeç", null)
        .show();
  }

  private void skip(Week w) {
    if (!mayEdit()) return;
    String id =
        w.duties.stream()
            .filter(d -> weekend(d.date) && d.place.equals(WORK))
            .map(d -> d.person)
            .findFirst()
            .orElse("");
    if (id.isEmpty()) {
      toast("Atlanacak hafta sonu nöbetçisi yok.");
      return;
    }
    LocalDate start =
        w.duties.stream()
            .filter(d -> weekend(d.date) && d.place.equals(WORK))
            .map(d -> d.date)
            .min(Comparator.naturalOrder())
            .orElse(selected.plusDays(5));
    dateTitled(
        "Mazeret Bitiş Tarihini Seçin",
        selected.plusDays(6),
        end -> {
          if (end.isBefore(start)) {
            toast("Bitiş ilk nöbet gününden önce olamaz.");
            return;
          }
          confirm(
              "Hafta Sonu Sırasını Atla",
              state.name(id)
                  + " için "
                  + start.format(shortDate)
                  + " – "
                  + end.format(shortDate)
                  + " tüm görev yerlerinde mazeret kaydedilsin mi?\n"
                  + "Mazeret bittikten sonraki ilk uygun hafta sonu öncelikli atanacak. Diğer"
                  + " haftaların çakışmayan atamaları korunur.",
              () -> {
                Leave leave = new Leave(id, ALL, start, end, "Hafta Sonu mazereti");
                state.leaves.add(leave);
                removeConflicts(leave);
                if (save()) {
                  draft = generate(state, selected);
                  render();
                }
              });
        });
  }

  private AlertDialog edit(LocalDate day, String place) {
    if (!mayEdit()) return null;
    Week original = draft != null ? draft : saved();
    if (original == null) return null;
    if (weekend(day) && place.equals(WORK)) {
      replaceWeekend(original);
      return null;
    }
    Week edited = copy(original);
    List<Person> options = new ArrayList<>();
    options.add(new Person("", "Boş Bırak"));
    options.addAll(
        state.people.stream().filter(p -> p.active).collect(java.util.stream.Collectors.toList()));
    return new AlertDialog.Builder(this)
        .setTitle(day.format(shortDate) + " • " + state.placeName(place))
        .setItems(
            options.stream().map(p -> p.name).toArray(String[]::new),
            (dialog, i) -> {
              edited.duties.removeIf(d -> d.date.equals(day) && d.place.equals(place));
              Person p = options.get(i);
              if (!p.id.isEmpty()) edited.duties.add(new Duty(day, place, p.id));
              edited.warnings.add(
                  "Elle düzenleme: "
                      + day.format(shortDate)
                      + " "
                      + state.placeName(place)
                      + " → "
                      + p.name);
              List<Duty> added = new ArrayList<>();
              if (!p.id.isEmpty()) added.add(new Duty(day, place, p.id));
              applyManualChange(edited, added);
            })
        .setNegativeButton("Vazgeç", null)
        .show();
  }

  private AlertDialog applyManualChange(Week edited, List<Duty> added) {
    if (!mayEdit()) return null;
    Set<String> messages = new LinkedHashSet<>();
    boolean simultaneous = false, repeated = false, leaveConflict = false;
    for (Duty duty : added) {
      for (Leave leave : state.leaves)
        if (leave.blocks(duty.person, duty.place, duty.date)) {
          leaveConflict = true;
          edited.leaveOverrides.add(leaveOverrideKey(duty, leave));
          messages.add(
              state.name(duty.person)
                  + " • "
                  + duty.date.format(shortDate)
                  + " • "
                  + state.placeName(duty.place)
                  + "\nMazeret / Kısıt: "
                  + leave.reason
                  + "\n"
                  + leavePeriod(leave));
        }
      List<String> otherPlaces = new ArrayList<>();
      for (Duty other : edited.duties) {
        if (other.date.equals(duty.date)
            && other.person.equals(duty.person)
            && !other.place.equals(duty.place)) {
          otherPlaces.add(state.placeName(other.place));
          edited.simultaneousOverrides.add(
              simultaneousKey(duty.date, duty.person, other.place, duty.place));
        }
      }
      if (!otherPlaces.isEmpty()) {
        simultaneous = true;
        messages.add(
            state.name(duty.person)
                + " • "
                + duty.date.format(shortDate)
                + "\nMevcut görev yerleri: "
                + String.join(", ", otherPlaces)
                + "\nEk atama: "
                + state.placeName(duty.place));
      }
      Place location = state.place(duty.place);
      List<String> previousDates = new ArrayList<>();
      for (Duty other : edited.duties)
        if (other.person.equals(duty.person)
            && other.place.equals(duty.place)
            && !other.date.equals(duty.date)) previousDates.add(other.date.format(shortDate));
      if (location.limit > 0 && previousDates.size() >= location.limit) {
        repeated = true;
        edited.limitOverrides.add(duty.place + "|" + duty.person);
        messages.add(
            state.name(duty.person)
                + " • "
                + location.name
                + "\nÖnceki atamalar: "
                + String.join(", ", previousDates)
                + "\nHaftalık kişi sınırı aşılacak.");
      }
    }
    List<String> invalid = validate(state, edited);
    if (!invalid.isEmpty()) {
      toast(String.join("\n", invalid));
      return null;
    }
    Runnable accept =
        () -> {
          draft = edited;
          renderKeepingPosition();
        };
    if (simultaneous || repeated || leaveConflict) {
      return confirm(
          leaveConflict
              ? "Mazeret / Kısıt İstisnası"
              : simultaneous ? "Aynı Gün Atama Teyidi" : "Tekrar Atama Onayı",
          String.join("\n\n", messages) + "\n\nBu atamayı yine de yapmak istiyor musun?",
          accept);
    }
    accept.run();
    return null;
  }

  private void replaceWeekend(Week original) {
    if (!mayEdit()) return;
    String old =
        original.duties.stream()
            .filter(d -> weekend(d.date) && d.place.equals(WORK))
            .map(d -> d.person)
            .findFirst()
            .orElse("");
    List<Person> options =
        state.people.stream()
            .filter(p -> p.active && !p.id.equals(old))
            .collect(java.util.stream.Collectors.toList());
    if (options.isEmpty()) {
      toast("Başka aktif personel yok.");
      return;
    }
    List<LocalDate> weekendDays = workWeekendDays(state, selected);
    if (weekendDays.isEmpty()) {
      toast("Ana görev yerinde hafta sonu günü seçili değil; Ayarlar'dan düzenleyin.");
      return;
    }
    if (old.isEmpty()) {
      new AlertDialog.Builder(this)
          .setTitle("Seçili Hafta Sonu Günleri • Manuel Atama")
          .setItems(
              options.stream().map(p -> p.name).toArray(String[]::new),
              (dialog, i) -> {
                Week manual = copy(original);
                Person p = options.get(i);
                manual.duties.removeIf(d -> weekend(d.date) && d.place.equals(WORK));
                for (LocalDate day : weekendDays) manual.duties.add(new Duty(day, WORK, p.id));
                manual.cursor = p.id;
                manual.debts.removeIf(d -> d.person.equals(p.id));
                List<Duty> added = new ArrayList<>();
                for (Duty duty : manual.duties)
                  if (duty.place.equals(WORK) && duty.person.equals(p.id) && weekend(duty.date))
                    added.add(duty);
                applyManualChange(manual, added);
              })
          .setNegativeButton("Vazgeç", null)
          .show();
      return;
    }
    new AlertDialog.Builder(this)
        .setTitle("Seçili Hafta Sonu Günlerini Devralacak Kişi")
        .setItems(
            options.stream().map(p -> p.name).toArray(String[]::new),
            (dialog, i) -> {
              Person replacement = options.get(i);
              Set<String> leavePermissions = new HashSet<>();
              Set<String> leaveMessages = new LinkedHashSet<>();
              for (LocalDate day : weekendDays) {
                Duty duty = new Duty(day, WORK, replacement.id);
                for (Leave leave : state.leaves)
                  if (leave.blocks(replacement.id, WORK, day)) {
                    leavePermissions.add(leaveOverrideKey(duty, leave));
                    leaveMessages.add(
                        day.format(shortDate) + " • " + leave.reason + "\n" + leavePeriod(leave));
                  }
              }
              Runnable continueChange =
                  () ->
                      dateTitled(
                          "Devreden Kişinin Mazeret Bitişi",
                          selected.plusDays(6),
                          end -> {
                            if (end.isBefore(weekendDays.get(0))) {
                              toast("Bitiş ilk seçili nöbet gününden önce olamaz.");
                              return;
                            }
                            confirm(
                                "Hafta Sonu Değişimini Teyit Edin",
                                state.name(old)
                                    + " yerine "
                                    + replacement.name
                                    + " seçili hafta sonu günlerine atanacak. Devreden kişi için"
                                    + " mazeret bitişi: "
                                    + end.format(shortDate)
                                    + ". İki yerde de mazeret kaydedilir. Perşembe/cuma izinleri ve"
                                    + " hafta içi atamalar yeniden hesaplanır. Diğer haftalar"
                                    + " korunur; öneri ayrıca onayınızı ister.",
                                () -> {
                                  if (!old.isEmpty()) {
                                    Leave leave =
                                        new Leave(
                                            old, ALL, weekendDays.get(0), end, "Hafta Sonu devri");
                                    state.leaves.add(leave);
                                    removeConflicts(leave);
                                  }
                                  if (save()) {
                                    try {
                                      Week replacementWeek =
                                          generateManualReplacement(
                                              state, selected, replacement.id);
                                      replacementWeek.leaveOverrides.addAll(leavePermissions);
                                      List<String> invalid = validate(state, replacementWeek);
                                      if (!invalid.isEmpty()) {
                                        toast(String.join("\n", invalid));
                                        return;
                                      }
                                      draft = replacementWeek;
                                      renderKeepingPosition();
                                    } catch (IllegalArgumentException e) {
                                      toast(e.getMessage());
                                    }
                                  }
                                });
                          });
              if (leavePermissions.isEmpty()) continueChange.run();
              else
                confirm(
                    "Mazeret / Kısıt İstisnası",
                    replacement.name
                        + " • "
                        + state.placeName(WORK)
                        + "\n\n"
                        + String.join("\n\n", leaveMessages)
                        + "\n\nMazeretine rağmen bu hafta sonuna atamak istiyor musun?",
                    continueChange);
            })
        .setNegativeButton("Vazgeç", null)
        .show();
  }

  private void chooseVisiblePlaces(Runnable after) {
    if (!mayEdit()) return;
    List<Place> choices = statisticPlaces(state);
    if (choices.isEmpty()) {
      if (after != null) after.run();
      return;
    }
    boolean[] checked = new boolean[choices.size()];
    String[] names = new String[choices.size()];
    for (int i = 0; i < choices.size(); i++) {
      Place p = choices.get(i);
      checked[i] = !state.hiddenPlaces.contains(p.id);
      names[i] = p.name + (p.deleted ? " (Silindi)" : "");
    }
    new AlertDialog.Builder(this)
        .setTitle("Çizelge/PDF'de Gösterilecek Şubeler")
        .setMultiChoiceItems(names, checked, (dialog, which, value) -> checked[which] = value)
        .setNegativeButton("Vazgeç", null)
        .setPositiveButton(
            "Görünümü Kaydet",
            (dialog, which) -> {
              for (int i = 0; i < choices.size(); i++) {
                String id = choices.get(i).id;
                if (checked[i]) state.hiddenPlaces.remove(id);
                else state.hiddenPlaces.add(id);
              }
              if (save()) {
                if (after != null) after.run();
                else render();
              }
            })
        .show();
  }

  private List<String> activeWeekendIds() {
    List<String> ids = new ArrayList<>();
    for (Person person : weekendOrder(state)) if (person.active) ids.add(person.id);
    return ids;
  }

  private List<String> editableWeekendIds() {
    List<String> active = activeWeekendIds();
    if (weekendOrderDraft != null
        && (weekendOrderDraft.size() != active.size()
            || !new HashSet<>(weekendOrderDraft).equals(new HashSet<>(active))))
      weekendOrderDraft = null;
    return weekendOrderDraft == null ? active : new ArrayList<>(weekendOrderDraft);
  }

  private void moveWeekendPerson(String id, int delta) {
    if (!mayEdit()) return;
    List<String> ids = editableWeekendIds();
    int from = ids.indexOf(id), to = from + delta;
    if (from < 0 || to < 0 || to >= ids.size()) return;
    weekendOrderDraft = new ArrayList<>(ids);
    Collections.swap(weekendOrderDraft, from, to);
    render();
  }

  private void confirmWeekendOrder(List<String> ids) {
    if (!mayEdit()) return;
    StringBuilder preview = new StringBuilder();
    for (int n = 0; n < ids.size(); n++)
      preview.append(n + 1).append(". Sıra • ").append(state.name(ids.get(n))).append("\n");
    confirm(
        "Hafta Sonu Nöbet Sırasını Kaydet",
        preview
            + "\n"
            + "Sonraki otomatik dağıtım 1. sıradan başlar. Onaylı listeler değişmez; bekleyen"
            + " mazeret hakları korunur.",
        () -> {
          setWeekendOrder(state, ids);
          if (save()) {
            weekendOrderDraft = null;
            draft = null;
            render();
          }
        });
  }

  private void weekendSettings() {
    heading("Hafta Sonu Nöbet Sırası");
    note(
        "↑ / ↓ ile personelleri taşıyın, ardından Sırayı Kaydet'e dokunun. Yeni sıra sonraki"
            + " otomatik öneride 1. sıradan başlar; onaylı listeler ve bekleyen mazeret hakları"
            + " korunur.");
    List<String> ids = editableWeekendIds();
    for (int i = 0; i < ids.size(); i++) {
      String id = ids.get(i);
      LinearLayout row = new LinearLayout(this);
      Button name =
          button(
              (i + 1) + ". Sıra • " + state.name(id),
              () -> {
                String[] positions = new String[ids.size()];
                for (int n = 0; n < positions.length; n++) positions[n] = (n + 1) + ". Sıra";
                new AlertDialog.Builder(this)
                    .setTitle(state.name(id) + " • Yeni Sıra")
                    .setSingleChoiceItems(
                        positions,
                        ids.indexOf(id),
                        (dialog, which) -> {
                          dialog.dismiss();
                          List<String> reordered = new ArrayList<>(ids);
                          reordered.remove(id);
                          reordered.add(which, id);
                          confirmWeekendOrder(reordered);
                        })
                    .setNegativeButton("Vazgeç", null)
                    .show();
              });
      soft(name, "people");
      name.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
      name.setTextSize(13);
      name.setMinWidth(0);
      name.setMinimumWidth(0);
      name.setPadding(dp(5), 0, dp(5), 0);
      row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
      for (int delta : new int[] {-1, 1}) {
        Button arrow = button(delta < 0 ? "↑" : "↓", () -> moveWeekendPerson(id, delta));
        arrow.setTag((delta < 0 ? "weekend-up-" : "weekend-down-") + id);
        arrow.setContentDescription(
            state.name(id) + (delta < 0 ? " • Yukarı Taşı" : " • Aşağı Taşı"));
        arrow.setMinWidth(0);
        arrow.setMinimumWidth(0);
        arrow.setPadding(0, 0, 0, 0);
        arrow.setEnabled(delta < 0 ? i > 0 : i < ids.size() - 1);
        if (!arrow.isEnabled()) arrow.setAlpha(.45f);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(44), dp(48)));
      }
      LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
      rowParams.bottomMargin = dp(6);
      body.addView(row, rowParams);
    }
    if (weekendOrderDraft != null) {
      action("Sırayı Kaydet", () -> confirmWeekendOrder(new ArrayList<>(weekendOrderDraft)));
      action(
          "Sıra Değişikliklerini İptal Et",
          () -> {
            weekendOrderDraft = null;
            render();
          });
    }
    action("+ Sıraya Yeni Personel Ekle", () -> personDialog(null));
  }

  private AlertDialog holidayCalendar() {
    LinearLayout box = column();
    box.setPadding(dp(16), dp(8), dp(16), dp(8));
    LinearLayout yearRow = row();
    int[] year = {selected.getYear()};
    TextView yearLabel = bold(Integer.toString(year[0]), 17, ink);
    yearLabel.setGravity(Gravity.CENTER);
    LinearLayout list = column();
    ScrollView scroll = new ScrollView(this);
    scroll.addView(list);
    Runnable[] refresh = new Runnable[1];
    refresh[0] =
        () -> {
          yearLabel.setText(String.format(new Locale("tr", "TR"), "%d", year[0]));
          list.removeAllViews();
          for (Holidays.Holiday h : Holidays.forYear(state, year[0], true)) {
            LinearLayout card = column();
            card.setPadding(0, dp(10), 0, dp(8));
            card.addView(
                bold(
                    h.date.format(shortDate) + " • " + (h.units == 1 ? "Yarım Gün" : "Tam Gün"),
                    13,
                    h.enabled ? teal : muted));
            card.addView(text(h.name + (h.enabled ? "" : " • Devre Dışı"), 14, ink));
            LinearLayout controls = row();
            Button edit = button("Düzenle", () -> holidayDialog(h, year[0], refresh[0]));
            edit.setTag("holiday-edit-" + h.id);
            soft(edit, "edit");
            controls.addView(edit, new LinearLayout.LayoutParams(0, -2, 1));
            Button remove =
                button(
                    h.enabled ? "Kaldır" : "Etkinleştir",
                    () ->
                        confirm(
                            h.enabled ? "Tatili Kaldır" : "Tatili Etkinleştir",
                            h.name
                                + " • "
                                + h.date.format(shortDate)
                                + ". Nöbet kayıtları korunur; resmî tatil sayımları yeniden"
                                + " hesaplanır.",
                            () -> {
                              if (h.automatic())
                                Holidays.put(
                                    state,
                                    new Holidays.Holiday(
                                        h.id, h.name, h.date, h.units, !h.enabled));
                              else state.holidays.removeIf(old -> old.id.equals(h.id));
                              if (save()) {
                                refresh[0].run();
                                renderKeepingPosition();
                              }
                            }));
            remove.setTag("holiday-remove-" + h.id);
            soft(remove, "calendar");
            controls.addView(remove, new LinearLayout.LayoutParams(0, -2, 1));
            card.addView(controls);
            if (h.automatic() && state.holidays.stream().anyMatch(old -> old.id.equals(h.id))) {
              Button reset =
                  button(
                      "Otomatik Kaydı Geri Yükle",
                      () ->
                          confirm(
                              "Otomatik Tatili Geri Yükle",
                              "Bu tatil için yaptığınız değişiklik kaldırılır; takvimdeki asıl"
                                  + " tarih, ad ve süre geri gelir.",
                              () -> {
                                state.holidays.removeIf(old -> old.id.equals(h.id));
                                if (save()) {
                                  refresh[0].run();
                                  renderKeepingPosition();
                                }
                              }));
              soft(reset, "calendar");
              card.addView(reset);
            }
            list.addView(card);
          }
        };
    Button previous =
        button(
            "‹",
            () -> {
              if (year[0] > 1900) {
                year[0]--;
                refresh[0].run();
              }
            });
    previous.setContentDescription("Önceki Tatil Yılı");
    Button next =
        button(
            "›",
            () -> {
              if (year[0] < 2200) {
                year[0]++;
                refresh[0].run();
              }
            });
    next.setContentDescription("Sonraki Tatil Yılı");
    yearRow.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(48)));
    yearRow.addView(yearLabel, new LinearLayout.LayoutParams(0, dp(48), 1));
    yearRow.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48)));
    box.addView(yearRow);
    TextView scope =
        text(
            "Sabit Tatiller: 2020 Ve Sonrası • Dinî Tatiller: 2020–2032. İdarî İzinleri Manuel"
                + " Ekleyin. Yarım Gün: 0,5.",
            11,
            muted);
    scope.setPadding(0, dp(8), 0, dp(8));
    box.addView(scope);
    Button add = button("+ Resmî Tatil Ekle", () -> holidayDialog(null, year[0], refresh[0]));
    add.setTag("holiday-add");
    box.addView(add);
    box.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    refresh[0].run();
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("Resmî Tatil Takvimi")
            .setView(box)
            .setNegativeButton("Kapat", null)
            .create();
    dialog.show();
    dialog
        .getWindow()
        .setLayout(-1, (int) (getResources().getDisplayMetrics().heightPixels * .85f));
    return dialog;
  }

  private AlertDialog holidayDialog(Holidays.Holiday current, int year, Runnable refresh) {
    if (!mayEdit()) return null;
    LinearLayout box = column();
    box.setPadding(dp(20), dp(8), dp(20), dp(8));
    EditText name = new EditText(this);
    name.setHint("Tatil Adı");
    name.setSingleLine(true);
    name.setTag("holiday-name");
    if (current != null) name.setText(current.name);
    box.addView(name);
    LocalDate[] chosen = {current != null ? current.date : LocalDate.of(year, 1, 1)};
    Button dateButton = button(chosen[0].format(shortDate), null);
    dateButton.setTag("holiday-date");
    dateButton.setOnClickListener(
        v ->
            date(
                chosen[0],
                value -> {
                  chosen[0] = value;
                  dateButton.setText(value.format(shortDate));
                }));
    box.addView(dateButton);
    Spinner duration = spinner(new String[] {"Tam Gün • 1", "Yarım Gün • 0,5 (13.00 Sonrası)"});
    duration.setTag("holiday-duration");
    duration.setSelection(current != null && current.units == 1 ? 1 : 0);
    box.addView(duration);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(current == null ? "Resmî Tatil Ekle" : "Resmî Tatili Düzenle")
            .setView(box)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Kaydet", null)
            .create();
    dialog.show();
    dialog
        .getButton(-1)
        .setOnClickListener(
            v -> {
              if (name.getText().toString().trim().isEmpty()) {
                name.setError("Tatil Adını Girin");
                return;
              }
              int units = duration.getSelectedItemPosition() == 0 ? 2 : 1;
              List<Holidays.Holiday> others = Holidays.on(state, chosen[0]);
              boolean overlap =
                  others.stream().anyMatch(h -> current == null || !h.id.equals(current.id));
              confirm(
                  "Resmî Tatili Kaydet",
                  name.getText().toString().trim()
                      + " • "
                      + chosen[0].format(shortDate)
                      + (units == 1 ? " • Yarım Gün (0,5)" : " • Tam Gün (1)")
                      + (overlap
                          ? "\n"
                              + "Bu tarihte başka tatil var. Aynı görev iki kez sayılmaz;"
                              + " en uzun tatil süresi kullanılır."
                          : "")
                      + "\n"
                      + "Geçmiş ve gelecek resmî tatil sayımları güncellenir. Nöbet"
                      + " kayıtları değişmez.",
                  () -> {
                    Holidays.put(
                        state,
                        new Holidays.Holiday(
                            current == null ? "custom:" + UUID.randomUUID() : current.id,
                            name.getText().toString(),
                            chosen[0],
                            units,
                            current == null || current.enabled));
                    if (save()) {
                      dialog.dismiss();
                      refresh.run();
                      renderKeepingPosition();
                    }
                  });
            });
    return dialog;
  }

  private void settings() {
    if (cloud != null) cloud.settings(body, darkMode);
    action("Resmî Tatil Takvimi", this::holidayCalendar);
    weekendSettings();
    heading("Ayarlar • Görev Yerleri");
    note(
        "Yer adını değiştirmek geçmiş nöbetleri silmez. Çizelge, PDF ve istatistiklerde yeni ad"
            + " görünür. İlk iki yerin de günlerini değiştirebilirsiniz; hiç gün seçmezseniz yeni"
            + " planlarda atama yapılmaz. Ana yerde seçili iki hafta sonu günü varsa aynı kişi"
            + " atanır.");
    action("+ Yeni Şube / Görev Yeri Ekle", () -> placeDialog(null));
    for (Place place : state.places) {
      if (place.deleted) continue;
      LinearLayout card = panel();
      card.addView(bold(place.name, 17, ink));
      StringBuilder days = new StringBuilder();
      String[] names = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};
      for (int i = 0; i < 7; i++)
        if ((place.days & (1 << i)) != 0) days.append(names[i]).append(" ");
      if (days.length() == 0) days.append("Kapalı (gün seçilmedi)");
      noteTo(
          card,
          days
              + " • Kişi başı haftalık: "
              + (place.limit == 0 ? "Sınırsız" : place.limit)
              + " • "
              + (place.automatic ? "Otomatik Dağıtım" : "Yalnız Manuel"));
      actionTo(card, "Adı / Kuralları Düzenle", () -> placeDialog(place));
      if (!place.id.equals(WORK) && !place.id.equals(BRANCH))
        actionTo(
            card,
            "Şubeyi Sil",
            () ->
                confirm(
                    "Görev Yerini Sil",
                    place.name
                        + " yeni dağıtımlardan çıkarılacak. Geçmiş ve bugünkü"
                        + " nöbetler/istatistikler korunur; yarın ve sonraki atamaları kaldırılır."
                        + " Silinen yerler bölümünden geri alabilirsiniz.",
                    () -> {
                      deletePlace(state, place.id, LocalDate.now());
                      if (save()) {
                        draft = null;
                        render();
                        toast("Şube silindi; geçmiş kayıtlar korundu.");
                      }
                    }));
    }
    List<Place> deleted = new ArrayList<>();
    for (Place p : state.places) if (p.deleted) deleted.add(p);
    if (!deleted.isEmpty())
      action(
          "Silinen Görev Yerleri (" + deleted.size() + ")",
          () ->
              new AlertDialog.Builder(this)
                  .setTitle("Geri Alınacak Görev Yerini Seçin")
                  .setItems(
                      deleted.stream().map(p -> p.name).toArray(String[]::new),
                      (d, i) -> {
                        Place p = deleted.get(i);
                        confirm(
                            "Şubeyi Geri Al",
                            p.name
                                + " yeniden kullanılabilecek. Kaldırılmış gelecek atamaları"
                                + " kendiliğinden geri gelmez.",
                            () -> {
                              p.deleted = false;
                              if (save()) {
                                draft = null;
                                render();
                              }
                            });
                      })
                  .setNegativeButton("Vazgeç", null)
                  .show());
    note(
        "Yeni bir yer eklendiğinde eski listelere kendiliğinden personel yazılmaz. Kaydedilmiş"
            + " haftayı açıp yer alanlarına dokunarak düzenleyebilir veya yeni öneri"
            + " oluşturabilirsiniz.");
  }

  private void placeDialog(Place current) {
    if (!mayEdit()) return;
    if (current == null && state.places.stream().filter(p -> !p.deleted).count() >= 20) {
      toast("En fazla 20 görev yeri eklenebilir.");
      return;
    }
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setPadding(dp(20), dp(8), dp(20), dp(8));
    EditText name = new EditText(this);
    name.setSingleLine(true);
    name.setHint("Görev Yeri Adı");
    if (current != null) name.setText(current.name);
    box.addView(name);
    boolean fixed = current != null && (current.id.equals(WORK) || current.id.equals(BRANCH));
    CheckBox[] days = new CheckBox[7];
    String[] labels = {"Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"};
    for (int i = 0; i < 7; i++) {
      days[i] = new CheckBox(this);
      days[i].setText(labels[i]);
      days[i].setChecked(((current == null ? 31 : current.days) & (1 << i)) != 0);
      days[i].setEnabled(true);
      box.addView(days[i]);
    }
    TextView limitLabel = text("Kişi Başı Haftalık Üst Sınır (0 = Sınırsız)", 14, ink);
    box.addView(limitLabel);
    EditText limit = new EditText(this);
    limit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    limit.setText(getString(R.string.limit_value, current == null ? 1 : current.limit));
    limit.setEnabled(!fixed);
    box.addView(limit);
    CheckBox automatic = new CheckBox(this);
    automatic.setText(R.string.auto_place);
    automatic.setChecked(current == null || current.automatic);
    automatic.setEnabled(!fixed);
    box.addView(automatic);
    ScrollView scroll = new ScrollView(this);
    scroll.addView(box);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(current == null ? "Yeni Görev Yeri" : "Görev Yeri Ayarları")
            .setView(scroll)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Kaydet", null)
            .create();
    dialog.setOnShowListener(
        x ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      String value = name.getText().toString().trim().replaceAll("\\s+", " ");
                      if (value.isEmpty() || value.length() > 40) {
                        name.setError("1–40 karakter girin.");
                        return;
                      }
                      if (state.places.stream()
                          .anyMatch(p -> p != current && p.name.equalsIgnoreCase(value))) {
                        name.setError(
                            "Bu ad kayıtlı. Silindiyse Silinen görev yerleri bölümünden geri"
                                + " alın.");
                        return;
                      }
                      int mask = 0;
                      for (int i = 0; i < 7; i++) if (days[i].isChecked()) mask |= 1 << i;
                      int max;
                      try {
                        max = Integer.parseInt(limit.getText().toString());
                        if (max < 0 || max > 7) throw new NumberFormatException();
                      } catch (NumberFormatException e) {
                        limit.setError("0–7 arasında girin.");
                        return;
                      }

                      if (current == null) {
                        Place p = new Place(UUID.randomUUID().toString(), value, mask, max);
                        p.automatic = automatic.isChecked();
                        state.places.add(p);
                      } else {
                        current.name = value;
                        current.days = mask;
                        current.limit = max;
                        current.automatic = automatic.isChecked();
                      }
                      if (save()) {
                        draft = null;
                        dialog.dismiss();
                        render();
                        toast(
                            "Kaydedildi. Seçili günler yeni dağıtımlarda uygulanır; mevcut listeler"
                                + " ve geçmiş korunur.");
                      }
                    }));
    dialog.show();
  }

  private void stats() {
    heading("Nöbet İstatistikleri");
    action(
        "İstatistikleri PDF Olarak Paylaş",
        () -> {
          try {
            PdfExport.share(this, PdfExport.createStatistics(this, state, LocalDate.now()));
          } catch (Exception e) {
            toast("İstatistik PDF'si oluşturulamadı veya paylaşım uygulaması bulunamadı.");
          }
        });
    heading("Bugün Ve Öncesi");
    statisticsTable(false);
    List<StatRow> planned = statistics(state, LocalDate.now(), true);
    boolean any = false;
    for (StatRow row : planned) for (long count : row.counts) if (count > 0) any = true;
    if (any) {
      heading("Gelecek Atamalar");
      statisticsTable(true);
    }
    heading("Resmî Tatiller • Bugün Ve Öncesi");
    action(
        "Resmî Tatilleri PDF Olarak Paylaş",
        () -> {
          try {
            PdfExport.share(this, PdfExport.createHolidayStatistics(this, state, LocalDate.now()));
          } catch (Exception e) {
            toast("Resmî tatil PDF'si oluşturulamadı veya paylaşım uygulaması bulunamadı.");
          }
        });
    statisticsTable(false, true);
    boolean holidayFuture =
        Holidays.statistics(state, LocalDate.now(), true).stream()
            .anyMatch(r -> java.util.Arrays.stream(r.counts).anyMatch(n -> n > 0));
    if (holidayFuture) {
      heading("Resmî Tatiller • Gelecek Atamalar");
      statisticsTable(true, true);
    }
  }

  private void statisticsTable(boolean future) {
    statisticsTable(future, false);
  }

  private void statisticsTable(boolean future, boolean holidays) {
    List<String> labels =
        holidays ? PdfExport.holidayLabels(state) : PdfExport.statisticsLabels(state);
    List<StatRow> rows =
        holidays
            ? Holidays.statistics(state, LocalDate.now(), future)
            : statistics(state, LocalDate.now(), future);
    // Extra branches continue vertically to keep every column readable on narrow screens.
    float density = getResources().getDisplayMetrics().density;
    int available = (int) (getResources().getDisplayMetrics().widthPixels / density) - 36;
    int columns = Math.max(2, Math.min(5, (available - 116) / 62));
    for (int first = 0; first < labels.size(); first += columns) {
      int end = Math.min(labels.size(), first + columns);
      LinearLayout table = new LinearLayout(this);
      table.setOrientation(LinearLayout.VERTICAL);
      table.setTag(holidays ? "holiday-statistics-table" : "statistics-table");
      table.setBackground(Visuals.surface(cardColor, dp(14), lineColor));
      table.setClipToOutline(true);
      LinearLayout header = new LinearLayout(this);
      addStatCell(header, "Personel", true, true, false);
      for (int i = first; i < end; i++) addStatCell(header, labels.get(i), false, true, false);
      table.addView(header);
      int index = 0;
      for (StatRow row : rows) {
        LinearLayout line = new LinearLayout(this);
        boolean stripe = index++ % 2 != 0;
        addStatCell(
            line, row.person.name + (row.person.active ? "" : " · Pasif"), true, false, stripe);
        for (int i = first; i < end; i++)
          addStatCell(
              line,
              holidays ? Holidays.days(row.counts[i]) : Long.toString(row.counts[i]),
              false,
              false,
              stripe);
        table.addView(line);
      }
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
      params.bottomMargin = dp(8);
      body.addView(table, params);
    }
    if (rows.isEmpty()) note("Henüz Personel Eklenmedi.");
  }

  private void addStatCell(
      LinearLayout row, String value, boolean name, boolean header, boolean stripe) {
    TextView cell = text(value, header ? 12 : 13, header ? Color.WHITE : ink);
    cell.setPadding(dp(5), dp(7), dp(5), dp(7));
    cell.setMinWidth(0);
    cell.setGravity((name ? Gravity.START : Gravity.CENTER_HORIZONTAL) | Gravity.CENTER_VERTICAL);
    cell.setBackground(
        new Visuals.TableRow(
            header ? Color.rgb(42, 111, 177) : stripe ? surface : cardColor, dp(1)));
    if (header) cell.setTypeface(null, Typeface.BOLD);
    row.addView(cell, new LinearLayout.LayoutParams(0, -1, name ? 1.85f : 1f));
  }
}
