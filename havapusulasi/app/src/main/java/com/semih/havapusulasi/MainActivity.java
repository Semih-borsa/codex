package com.semih.havapusulasi;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
  private WeatherStore store;
  private SharedPreferences prefs;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler ui = new Handler(Looper.getMainLooper());
  private LinearLayout root, body;
  private ScrollView scroll;
  private int page, shown = -1, bg, surface, text, muted, accent;
  private boolean busy, light;
  private long selected;
  private String feedback = "";
  private List<City> searchResults = Collections.emptyList();
  private String query = "";
  private EditText search;

  public void onCreate(Bundle state) {
    super.onCreate(state);
    store = new WeatherStore(this);
    prefs = DailyReminder.prefs(this);
    selected = prefs.getLong("selected", 0);
    long widgetCity = getIntent().getLongExtra("cityId", 0);
    for (City city : store.cities()) if (city.id == widgetCity) selected = widgetCity;
    if (state != null) {
      page = state.getInt("page");
      query = state.getString("query", "");
      feedback = state.getString("feedback", "");
    }
    DailyReminder.channel(this);
    DailyReminder.schedule(this);
    RefreshJob.configure(this);
    render();
  }

  protected void onResume() {
    super.onResume();
    if (store != null) {
      DailyReminder.schedule(this);
      render();
    }
  }

  protected void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putInt("page", page);
    b.putString("query", search == null ? query : search.getText().toString());
    b.putString("feedback", feedback);
  }

  protected void onDestroy() {
    worker.execute(store::close);
    worker.shutdown();
    super.onDestroy();
  }

  private int dp(float n) {
    return (int) (n * getResources().getDisplayMetrics().density + .5f);
  }

  private LinearLayout column() {
    LinearLayout x = new LinearLayout(this);
    x.setOrientation(LinearLayout.VERTICAL);
    return x;
  }

  private LinearLayout row() {
    LinearLayout x = new LinearLayout(this);
    x.setGravity(Gravity.CENTER_VERTICAL);
    return x;
  }

  private GradientDrawable shape(int color, int radius) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(color);
    g.setCornerRadius(dp(radius));
    return g;
  }

  private void add(LinearLayout p, View v) {
    p.addView(v, new LinearLayout.LayoutParams(-1, -2));
  }

  private void weight(LinearLayout p, View v) {
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
    lp.setMargins(dp(3), 0, dp(3), 0);
    p.addView(v, lp);
  }

  private void gap(LinearLayout p, int size) {
    p.addView(new View(this), new LinearLayout.LayoutParams(1, dp(size)));
  }

  private TextView copy(LinearLayout p, String s, int size, int color, boolean bold) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setLineSpacing(dp(2), 1.04f);
    t.setIncludeFontPadding(false);
    t.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
    if (size <= 11 && bold) t.setLetterSpacing(.09f);
    add(p, t);
    return t;
  }

  private LinearLayout card(LinearLayout p) {
    LinearLayout c = column();
    c.setPadding(dp(20), dp(20), dp(20), dp(20));
    GradientDrawable panel = shape(surface, 24);
    panel.setStroke(dp(1), light ? 0xFFE2E8F2 : 0xFF27364D);
    c.setBackground(panel);
    c.setElevation(dp(light ? 1 : 0));
    LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-1, -2);
    l.bottomMargin = dp(14);
    p.addView(c, l);
    return c;
  }

  private Button button(String title, boolean primary, Runnable action) {
    Button b = new Button(this);
    b.setText(title);
    b.setAllCaps(false);
    b.setTextColor(primary ? (light ? Color.WHITE : 0xFF081B38) : accent);
    b.setTextSize(14);
    b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    b.setMinHeight(dp(48));
    b.setMinimumHeight(dp(48));
    b.setPadding(dp(12), dp(8), dp(12), dp(8));
    GradientDrawable fill = shape(primary ? accent : (light ? 0xFFEDF3FE : 0xFF1E304A), 16);
    b.setBackground(
        new RippleDrawable(ColorStateList.valueOf(light ? 0x224A78FF : 0x337BB5FF), fill, null));
    b.setOnClickListener(v -> action.run());
    return b;
  }

  private void navigate(int where) {
    if (search != null) query = search.getText().toString();
    page = where;
    feedback = "";
    render();
  }

  private void render() {
    if (isDestroyed() || isFinishing()) return;
    int y = shown == page && scroll != null ? scroll.getScrollY() : 0;
    shown = page;
    String theme = prefs.getString("theme", "system");
    light =
        theme.equals("light")
            || theme.equals("system")
                && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    != Configuration.UI_MODE_NIGHT_YES;
    bg = light ? 0xFFF4F7FC : 0xFF0A1220;
    surface = light ? Color.WHITE : 0xFF142137;
    text = light ? 0xFF16243B : 0xFFF1F5FF;
    muted = light ? 0xFF596B85 : 0xFFA9B8CF;
    accent = light ? 0xFF245DD8 : 0xFF91BDFF;
    getWindow().setStatusBarColor(bg);
    getWindow().setNavigationBarColor(bg);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            light
                ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                : 0);
    root = column();
    root.setBackgroundColor(bg);
    setContentView(root);
    // Respect gesture/status bars, including target-35 edge-to-edge enforcement.
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          if (Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets a =
                insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            v.setPadding(a.left, a.top, a.right, a.bottom);
          } else
            v.setPadding(
                insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom());
          return insets;
        });
    root.requestApplyInsets();
    LinearLayout header = row();
    header.setPadding(dp(20), dp(18), dp(20), dp(16));
    LinearLayout titles = column();
    copy(titles, "HAVA PUSULASI", 11, accent, true);
    gap(titles, 7);
    copy(
        titles,
        page == 0 ? "Hava durumun." : page == 1 ? "Şehirlerin." : "Tercihlerin.",
        27,
        text,
        true);
    weight(header, titles);
    TextView star = new TextView(this);
    star.setText("☀");
    star.setGravity(Gravity.CENTER);
    star.setTextSize(23);
    star.setBackground(shape(light ? 0xFFE6EEFF : 0xFF203556, 18));
    star.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    star.setTextColor(accent);
    header.addView(star, new LinearLayout.LayoutParams(dp(50), dp(50)));
    add(root, header);
    scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    body = column();
    body.setPadding(dp(18), dp(8), dp(18), dp(20));
    scroll.setClipToPadding(false);
    scroll.setVerticalScrollBarEnabled(false);
    FrameLayout.LayoutParams content =
        new FrameLayout.LayoutParams(
            getResources().getConfiguration().screenWidthDp > 720 ? dp(720) : -1,
            -2,
            Gravity.CENTER_HORIZONTAL);
    scroll.addView(body, content);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    search = null;
    if (!feedback.isEmpty()) {
      LinearLayout message = card(body);
      copy(message, feedback, 13, accent, false);
    }
    if (page == 0) home();
    else if (page == 1) cities();
    else settings();
    LinearLayout nav = row();
    nav.setPadding(dp(8), dp(8), dp(8), dp(8));
    nav.setBackground(shape(surface, 24));
    nav.setElevation(dp(4));
    String[] names = {"◈  Tahmin", "⌖  Şehirler", "☷  Ayarlar"};
    for (int i = 0; i < 3; i++) {
      final int index = i;
      Button tab = button(names[i], false, () -> navigate(index));
      tab.setTextColor(page == i ? accent : muted);
      tab.setBackground(
          new RippleDrawable(
              ColorStateList.valueOf(0x22608BDF),
              shape(page == i ? (light ? 0xFFE8F0FF : 0xFF233B60) : Color.TRANSPARENT, 18),
              null));
      tab.setContentDescription(names[i] + (page == i ? ", seçili" : ""));
      weight(nav, tab);
    }
    LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(-1, -2);
    navParams.setMargins(dp(16), dp(4), dp(16), dp(12));
    root.addView(nav, navParams);
    scroll.post(() -> scroll.scrollTo(0, y));
    WeatherWidget.updateAll(this);
  }

  private void home() {
    List<City> cities = store.cities();
    if (cities.isEmpty()) {
      LinearLayout hero = card(body);
      copy(hero, "Havayı bil. Gününü rahat planla.", 29, text, true);
      gap(hero, 8);
      hero.addView(new WeatherArt(this, 1), new LinearLayout.LayoutParams(-1, dp(150)));
      copy(
          hero,
          "Bir şehir ekle, 7 günlük tahmini indir. Son başarılı kayıt internet olmadan da yanında.",
          15,
          muted,
          false);
      gap(hero, 16);
      add(hero, button("İlk şehrimi ekle", true, () -> navigate(1)));
      LinearLayout privacy = card(body);
      copy(privacy, "Sade, kişisel, anahtarsız", 16, text, true);
      gap(privacy, 6);
      copy(
          privacy,
          "Hesap, ücretli yapay zekâ, sosyal akış ve harita yok. Konum izni istenmez. Tahmin"
              + " indirmek için internet gerekir.",
          14,
          muted,
          false);
      return;
    }
    City chosen = cities.get(0);
    for (City c : cities) if (c.id == selected) chosen = c;
    selected = chosen.id;
    prefs.edit().putLong("selected", selected).apply();
    final City city = chosen;
    HorizontalScrollView chips = new HorizontalScrollView(this);
    chips.setHorizontalScrollBarEnabled(false);
    LinearLayout selectors = row();
    for (City c : cities) {
      Button b =
          button(
              c.name,
              c.id == city.id,
              () -> {
                selected = c.id;
                prefs.edit().putLong("selected", selected).apply();
                feedback = "";
                render();
              });
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
      lp.rightMargin = dp(8);
      selectors.addView(b, lp);
    }
    chips.addView(selectors);
    add(body, chips);
    gap(body, 14);
    Forecast f = store.cached(city.id);
    long now = System.currentTimeMillis();
    List<Forecast.Hour> hours = f == null ? Collections.emptyList() : f.upcoming(now, 24);
    Forecast.Hour current = f == null ? null : f.current(now);
    LinearLayout hero = card(body);
    GradientDrawable gradient =
        new GradientDrawable(
            GradientDrawable.Orientation.TL_BR, new int[] {0xFF1B3C73, 0xFF142640, 0xFF102037});
    gradient.setCornerRadius(dp(26));
    gradient.setStroke(dp(1), 0xFF36547D);
    hero.setBackground(gradient);
    copy(hero, city.label(), 24, Color.WHITE, true);
    gap(hero, 7);
    copy(
        hero,
        Instant.ofEpochMilli(now)
            .atZone(ZoneId.of(city.zone))
            .format(DateTimeFormatter.ofPattern("d MMMM EEEE", Forecast.TR)),
        13,
        0xFFBACCE8,
        false);
    gap(hero, 12);
    LinearLayout weather = row();
    LinearLayout temp = column();
    TextView temperature =
        copy(
            temp,
            current == null ? "—" : Forecast.value(current.temperature, "°"),
            76,
            Color.WHITE,
            false);
    temperature.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
    copy(
        temp,
        current == null ? "Tahmin bekleniyor" : Forecast.description(current.code),
        18,
        Color.WHITE,
        true);
    gap(temp, 6);
    copy(temp, "Bu saatin model tahmini", 11, 0xFFBACCE8, false);
    weight(weather, temp);
    int artWidth = getResources().getConfiguration().screenWidthDp < 380 ? 105 : 132;
    weather.addView(
        new WeatherArt(this, current == null ? null : current.code),
        new LinearLayout.LayoutParams(dp(artWidth), dp(145)));
    add(hero, weather);
    gap(hero, 18);
    copy(
        hero,
        f == null ? "Henüz indirilmiş tahmin yok" : f.status(now),
        12,
        current == null || f != null && f.stale(now) ? 0xFFFFD18A : 0xFFBACCE8,
        false);
    gap(hero, 14);
    Button refresh =
        button(busy ? "Tahmin indiriliyor…" : "Tahmini yenile", true, () -> refresh(city));
    refresh.setTextColor(0xFF102440);
    refresh.setBackground(
        new RippleDrawable(ColorStateList.valueOf(0x334675C1), shape(0xFFDCEAFF, 16), null));
    refresh.setEnabled(!busy);
    refresh.setAlpha(busy ? .6f : 1f);
    add(hero, refresh);
    if (current != null) {
      LinearLayout first = row();
      metric(first, "Hissedilen", Forecast.value(current.feels, "°C"), "◉");
      metric(first, "Yağış olasılığı", Forecast.value(current.rain, "%"), "☂");
      add(body, first);
      gap(body, 10);
      LinearLayout second = row();
      metric(second, "Rüzgâr", Forecast.value(current.wind, " km/sa"), "≋");
      metric(second, "Nem", Forecast.value(current.humidity, "%"), "◈");
      add(body, second);
      gap(body, 18);
    }
    LinearLayout tip = card(body);
    copy(tip, "GÜNLÜK PUSULAN", 10, accent, true);
    gap(tip, 6);
    copy(tip, Forecast.advice(current), 14, text, false);
    copy(tip, "Otomatik kurallara dayanır; resmî tehlike uyarısı değildir.", 11, muted, false);
    copy(body, "Önümüzdeki 24 saat", 19, text, true);
    gap(body, 5);
    copy(body, "Şehrin yerel saati • " + (f == null ? city.zone : f.zone), 12, muted, false);
    gap(body, 12);
    if (hours.isEmpty())
      copy(
          card(body),
          "Bu saatten sonrası için kayıt yok. Bağlantı kurup yenile; eski sıcaklığı güncelmiş gibi"
              + " göstermiyoruz.",
          14,
          muted,
          false);
    else {
      HorizontalScrollView strip = new HorizontalScrollView(this);
      LinearLayout list = row();
      for (Forecast.Hour h : hours) {
        LinearLayout item = column();
        item.setPadding(dp(15), dp(14), dp(15), dp(14));
        item.setBackground(shape(light ? 0xFFEAF1FF : 0xFF1B2C46, 20));
        copy(
            item,
            Instant.ofEpochMilli(h.at)
                .atZone(ZoneId.of(f.zone))
                .format(DateTimeFormatter.ofPattern("HH:mm", Forecast.TR)),
            12,
            muted,
            true);
        copy(item, Forecast.icon(h.code), 25, accent, false);
        copy(item, Forecast.value(h.temperature, "°C"), 22, text, true);
        copy(item, Forecast.value(h.rain, "%") + " yağış", 11, muted, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(100), -2);
        lp.rightMargin = dp(9);
        list.addView(item, lp);
      }
      strip.setHorizontalScrollBarEnabled(false);
      strip.addView(list);
      add(body, strip);
      gap(body, 18);
    }
    copy(body, "7 günlük bakış", 19, text, true);
    gap(body, 10);
    if (f != null) {
      LocalDate today = Instant.ofEpochMilli(now).atZone(ZoneId.of(f.zone)).toLocalDate();
      for (Forecast.Day d : f.days)
        if (!d.date.isBefore(today)) {
          LinearLayout day = card(body);
          LinearLayout line = row();
          TextView icon = new TextView(this);
          icon.setText(Forecast.icon(d.code));
          icon.setTextSize(25);
          icon.setTextColor(accent);
          line.addView(icon, new LinearLayout.LayoutParams(dp(42), -2));
          LinearLayout label = column();
          copy(
              label,
              d.date.equals(today)
                  ? "Bugün"
                  : d.date.format(DateTimeFormatter.ofPattern("d MMM, EEEE", Forecast.TR)),
              14,
              text,
              true);
          gap(label, 4);
          copy(label, Forecast.description(d.code), 12, muted, false);
          weight(line, label);
          TextView range = new TextView(this);
          range.setText(
              getString(
                  R.string.temperature_range,
                  Forecast.value(d.low, "°"),
                  Forecast.value(d.high, "°")));
          range.setTextColor(text);
          range.setTextSize(15);
          line.addView(range);
          add(day, line);
        }
    }
    sources(body);
  }

  private void refresh(City city) {
    if (busy) return;
    busy = true;
    feedback = "";
    render();
    worker.execute(
        () -> {
          String result;
          try {
            String raw = WeatherApi.fetch(WeatherApi.forecastUrl(city));
            if (!isDestroyed()) store.save(city.id, raw, System.currentTimeMillis());
            result = "Tahmin indirildi. Saatlik değerler model tahminidir, ölçüm değildir.";
          } catch (Exception e) {
            result =
                "Yenilenemedi: "
                    + error(e)
                    + (store.cached(city.id) != null
                        ? " Son başarılı kayıt korunuyor."
                        : " Bu şehir için henüz indirilmiş tahmin yok.");
          }
          final String finalResult = result;
          ui.post(
              () -> {
                busy = false;
                feedback = finalResult;
                if (!isDestroyed()) render();
              });
        });
  }

  private static String error(Exception e) {
    if (e instanceof java.net.UnknownHostException
        || e instanceof java.net.ConnectException
        || e instanceof java.net.NoRouteToHostException)
      return "İnternet bağlantısı kurulamadı. Wi-Fi veya mobil veriyi kontrol et.";
    if (e instanceof java.net.SocketTimeoutException)
      return "Hava servisi zamanında yanıt vermedi. Biraz sonra yeniden dene.";
    if (e instanceof javax.net.ssl.SSLException)
      return "Güvenli bağlantı kurulamadı. Telefonun tarihini ve bağlantısını kontrol et.";
    if (e instanceof org.json.JSONException) return "Servisten gelen tahmin verisi okunamadı.";
    String s = e.getMessage();
    return s == null ? "Bağlantı veya servis hatası." : s.length() > 180 ? s.substring(0, 180) : s;
  }

  private void cities() {
    copy(body, "Şehir veya ilçe ara", 21, text, true);
    gap(body, 6);
    copy(
        body,
        "Arama sorgusu Open-Meteo'ya gönderilir. GPS ve rehber izni gerekmez. En fazla 12 şehir"
            + " kaydedilir.",
        13,
        muted,
        false);
    gap(body, 10);
    search = new EditText(this);
    search.setSingleLine(true);
    search.setTextColor(text);
    search.setHintTextColor(muted);
    search.setHint("Örneğin Bursa, London");
    search.setText(query);
    search.setPadding(dp(16), dp(12), dp(16), dp(12));
    search.setBackground(shape(surface, 16));
    add(body, search);
    gap(body, 8);
    add(
        body,
        button(
            busy ? "Bekleniyor…" : "Şehir ara",
            true,
            () -> {
              if (busy) return;
              query = search.getText().toString().trim();
              busy = true;
              feedback = "";
              render();
              worker.execute(
                  () -> {
                    List<City> found = Collections.emptyList();
                    String message = "";
                    try {
                      found = WeatherApi.search(query, WeatherApi::fetch);
                      if (found.isEmpty()) message = "Şehir bulunamadı; adını daha açık yaz.";
                    } catch (Exception e) {
                      message = "Şehir araması yapılamadı: " + error(e);
                    }
                    final List<City> result = found;
                    final String note = message;
                    ui.post(
                        () -> {
                          busy = false;
                          searchResults = result;
                          feedback = note;
                          render();
                        });
                  });
            }));
    gap(body, 14);
    for (City c : searchResults) {
      LinearLayout result = card(body);
      copy(result, c.label(), 16, text, true);
      copy(result, c.zone, 12, muted, false);
      add(result, button("Bu şehri ekle", false, () -> addCity(c)));
    }
    if (searchResults.isEmpty()) {
      copy(body, "Hızlı ekle", 13, muted, true);
      gap(body, 8);
      LinearLayout quick = row();
      City[] presets = {
        new City(0, "İstanbul", "İstanbul", "Türkiye", 41.01, 28.98, "Europe/Istanbul"),
        new City(0, "Ankara", "Ankara", "Türkiye", 39.93, 32.85, "Europe/Istanbul"),
        new City(0, "İzmir", "İzmir", "Türkiye", 38.42, 27.14, "Europe/Istanbul")
      };
      for (City c : presets) weight(quick, button(c.name, false, () -> addCity(c)));
      add(body, quick);
      copy(
          body,
          "Hazır şehir çevrimdışı eklenebilir; hava tahmini ilk kez internetten indirilmelidir.",
          11,
          muted,
          false);
      gap(body, 18);
    }
    copy(body, "Kaydedilen şehirler", 19, text, true);
    gap(body, 10);
    for (City c : store.cities()) {
      LinearLayout saved = card(body);
      copy(saved, c.label(), 17, text, true);
      Forecast f = store.cached(c.id);
      copy(
          saved,
          f == null ? "Henüz kayıtlı tahmin yok" : f.status(System.currentTimeMillis()),
          12,
          muted,
          false);
      gap(saved, 10);
      LinearLayout actions = row();
      weight(
          actions,
          button(
              "Tahmine git",
              true,
              () -> {
                selected = c.id;
                prefs.edit().putLong("selected", selected).apply();
                navigate(0);
              }));
      weight(
          actions,
          button(
              "Sil",
              false,
              () ->
                  new AlertDialog.Builder(dialogContext())
                      .setTitle("Şehir silinsin mi?")
                      .setMessage(c.label() + " ve bu şehrin kaydedilmiş hava verisi silinecek.")
                      .setNegativeButton("Vazgeç", null)
                      .setPositiveButton(
                          "Sil",
                          (d, w) -> {
                            store.remove(c.id);
                            render();
                          })
                      .show()));
      add(saved, actions);
    }
    sources(body);
  }

  private void addCity(City city) {
    try {
      selected = store.add(city);
      prefs.edit().putLong("selected", selected).apply();
      searchResults = Collections.emptyList();
      page = 0;
      feedback = "Şehir eklendi. Tahmini yenile ile internetten hava verisini indir.";
      render();
    } catch (Exception e) {
      feedback = error(e);
      render();
    }
  }

  private void settings() {
    LinearLayout theme = card(body);
    copy(theme, "TEMA · AÇIK / KOYU", 11, accent, true);
    copy(
        theme,
        "Açık, koyu veya telefonun sistem temasını seç. Tercihin sonraki açılışta korunur.",
        13,
        muted,
        false);
    gap(theme, 8);
    LinearLayout themes = row();
    for (String name : new String[] {"system", "light", "dark"})
      weight(
          themes,
          button(
              name.equals("system") ? "Sistem" : name.equals("light") ? "Açık mod" : "Koyu mod",
              prefs.getString("theme", "system").equals(name),
              () -> {
                prefs.edit().putString("theme", name).apply();
                render();
              }));
    add(theme, themes);
    LinearLayout reminders = card(body);
    copy(reminders, "Günün hava özeti", 20, text, true);
    gap(reminders, 7);
    copy(
        reminders,
        "Seçtiğin saatte bütün kayıtlı şehirlerin son indirilmiş tahminini bildirir. Bildirim için"
            + " internet gerekmez; eski veriler açıkça belirtilir.",
        14,
        muted,
        false);
    Switch enabled = new Switch(this);
    enabled.setTextColor(text);
    enabled.setText(getString(R.string.daily_notification));
    enabled.setChecked(prefs.getBoolean("reminder", false));
    enabled.setOnCheckedChangeListener(
        (b, on) -> {
          prefs.edit().putBoolean("reminder", on).apply();
          DailyReminder.schedule(this);
          if (on
              && Build.VERSION.SDK_INT >= 33
              && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                  != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 11);
          render();
        });
    add(reminders, enabled);
    gap(reminders, 10);
    add(
        reminders,
        button(
            String.format(
                Forecast.TR,
                "Bildirim saati • %02d:%02d",
                prefs.getInt("hour", 8),
                prefs.getInt("minute", 0)),
            false,
            () ->
                new TimePickerDialog(
                        this,
                        (v, h, m) -> {
                          prefs.edit().putInt("hour", h).putInt("minute", m).apply();
                          DailyReminder.schedule(this);
                          render();
                        },
                        prefs.getInt("hour", 8),
                        prefs.getInt("minute", 0),
                        true)
                    .show()));
    gap(reminders, 8);
    copy(
        reminders,
        "Saat dilimi: " + ZoneId.systemDefault().getId() + " (telefon saati)",
        12,
        muted,
        false);
    copy(
        reminders,
        DailyReminder.allowed(this)
            ? "Bildirim izni açık"
            : "Bildirim izni/kanalı kapalı; uyarı gönderilemez.",
        12,
        accent,
        true);
    copy(
        reminders,
        DailyReminder.exact(this)
            ? "Kesin zamanlama kullanılabilir"
            : "Yaklaşık zamanlama: Android uyarıyı geciktirebilir.",
        12,
        muted,
        false);
    long next = prefs.getLong("nextAlarm", 0);
    if (next > 0)
      copy(
          reminders,
          "Sonraki plan: "
              + Instant.ofEpochMilli(next)
                  .atZone(ZoneId.systemDefault())
                  .format(DateTimeFormatter.ofPattern("d MMM HH:mm", Forecast.TR)),
          12,
          muted,
          false);
    if (!DailyReminder.allowed(this))
      add(
          reminders,
          button(
              "Bildirim izinlerini incele",
              false,
              () -> {
                if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED)
                  requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 11);
                else
                  startActivity(
                      new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                          .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
              }));
    if (Build.VERSION.SDK_INT >= 31 && !DailyReminder.exact(this))
      add(
          reminders,
          button(
              "Kesin zamanlama iznini incele",
              false,
              () -> {
                try {
                  startActivity(
                      new Intent(
                          Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                          Uri.parse("package:" + getPackageName())));
                } catch (ActivityNotFoundException e) {
                  toast("İzin ekranı bulunamadı.");
                }
              }));
    copy(
        reminders,
        "Yeniden başlatılınca gelecek uyarı yeniden planlanır. Telefon kapalıyken kaçırılan uyarı"
            + " sonradan gönderilmez. Zorla durdurma ve Xiaomi pil kısıtları teslimi"
            + " engelleyebilir. Ayarlar kendiliğinden değiştirilmez.",
        12,
        muted,
        false);
    gap(reminders, 10);
    add(
        reminders,
        button(
            "Örnek bildirimi göster",
            false,
            () -> {
              try {
                boolean sent =
                    DailyReminder.deliver(
                        this,
                        DailyReminder.summary(store.cities(), store, System.currentTimeMillis()),
                        "Deneme");
                toast(sent ? "Deneme bildirimi oluşturuldu." : "Önce bildirim iznini aç.");
              } catch (Exception e) {
                toast(error(e));
              }
            }));
    LinearLayout sync = card(body);
    copy(sync, "İsteğe bağlı arka plan yenileme", 17, text, true);
    copy(
        sync,
        "30 dakika, 1 saat veya 3 saat aralık seçebilirsin. Android pil tasarrufu/Doze nedeniyle"
            + " tam saat garantisi vermez; widget son başarılı yenileme saatini gösterir. Kayıtlı"
            + " şehirler yenilenir, iki ardışık bağlantı hatasında sonraki döngü beklenir.",
        13,
        muted,
        false);
    LinearLayout intervals = row();
    for (int minutes : new int[] {30, 60, 180})
      weight(
          intervals,
          button(
              minutes == 30 ? "30 dakika" : minutes == 60 ? "1 saat" : "3 saat",
              SyncPolicy.minutes(prefs.getInt("refreshMinutes", 30)) == minutes,
              () -> {
                prefs.edit().putInt("refreshMinutes", minutes).apply();
                RefreshJob.configure(this);
                render();
              }));
    add(sync, intervals);
    Switch wifi = new Switch(this);
    wifi.setTextColor(text);
    wifi.setText(R.string.wifi_only);
    wifi.setChecked(prefs.getBoolean("wifiOnly", true));
    wifi.setOnCheckedChangeListener(
        (b, on) -> {
          prefs.edit().putBoolean("wifiOnly", on).apply();
          RefreshJob.configure(this);
          WeatherWidget.updateAll(this);
        });
    add(sync, wifi);
    copy(
        sync,
        "Wi-Fi seçeneği Android'in ölçülmeyen ağ sınıflandırmasını kullanır. Kapalıysa mobil veri"
            + " de kullanılabilir; operatör ücreti oluşabilir. Elle yenileme mevcut bağlantıyı"
            + " kullanır.",
        12,
        muted,
        false);
    Switch background = new Switch(this);
    background.setTextColor(text);
    background.setText(R.string.background_refresh);
    background.setChecked(RefreshJob.enabled(this));
    background.setOnCheckedChangeListener(
        (b, on) -> {
          prefs.edit().putBoolean("background", on).apply();
          RefreshJob.configure(this);
          WeatherWidget.updateAll(this);
        });
    add(sync, background);
    long attempt = prefs.getLong("backgroundAttempt", 0);
    if (attempt > 0)
      copy(
          sync,
          "Son deneme: "
              + Instant.ofEpochMilli(attempt)
                  .atZone(ZoneId.systemDefault())
                  .format(DateTimeFormatter.ofPattern("d MMM HH:mm", Forecast.TR))
              + (prefs.getBoolean("backgroundFailed", false)
                  ? " • Bazı şehirler yenilenemedi"
                  : " • Tamamlandı"),
          12,
          muted,
          false);
    LinearLayout widget = card(body);
    copy(widget, "Ana ekran widget’ı", 19, text, true);
    copy(
        widget,
        "Widget’a dokununca ilgili şehirde uygulama açılır. Yenileme uygulamadaki Tahmini yenile"
            + " düğmesinden yapılır. Şehir, widget eklenirken veya başlatıcının desteklediği"
            + " Düzenle seçeneğinden seçilir. Sonraki düğmesi diğer tahmin günlerine geçer.",
        13,
        muted,
        false);
    add(
        widget,
        button(
            "Ana ekrana widget ekle",
            false,
            () -> {
              android.appwidget.AppWidgetManager manager =
                  android.appwidget.AppWidgetManager.getInstance(this);
              if (!manager.isRequestPinAppWidgetSupported()) {
                toast(
                    "Ana ekrana basılı tut → Widget’lar → HavaPusulası. Başlatıcın hızlı eklemeyi"
                        + " desteklemiyor.");
                return;
              }
              boolean requested =
                  manager.requestPinAppWidget(WeatherWidget.component(this), null, null);
              toast(
                  requested
                      ? "Başlatıcıdaki ekleme ve şehir seçimini tamamla."
                      : "Widget ekleme isteği kabul edilmedi.");
            }));
    copy(
        widget,
        "Widget ekliyse otomatik yenileme varsayılan açıktır. Daha önce kapattıysan kapalı"
            + " kalır; yukarıdaki anahtardan açabilirsin. Yalnızca Wi-Fi açıksa mobil veride"
            + " bekler. Widget yenilense bile son yenileme saati ancak yeni veri inince değişir.",
        12,
        muted,
        false);
    LinearLayout info = card(body);
    copy(info, "HavaPusulası 1.2", 19, text, true);
    gap(info, 7);
    copy(
        info,
        "Kişisel kullanım • Hesap ve API anahtarı yok\n"
            + "Derece: °C • Rüzgâr: km/sa • Bildirim: telefon saati\n"
            + "Şehirler ve tahminler cihazın özel alanında tutulur. GPS, reklam, takip ve yapay"
            + " zekâ servisi yoktur. Otomatik yedekleme kapalıdır; uygulamayı kaldırmak kayıtları"
            + " siler.",
        13,
        muted,
        false);
    gap(info, 8);
    copy(
        info,
        "Sonraki adım: Tahmin ile güvenilir gerçekleşen gözlemi karşılaştıran doğruluk ekranı. İlk"
            + " sürümde doğruluk yüzdesi hesaplanmaz.",
        13,
        text,
        false);
    sources(body);
  }

  private void metric(LinearLayout parent, String title, String value, String symbol) {
    LinearLayout cell = column();
    cell.setPadding(dp(16), dp(16), dp(16), dp(16));
    GradientDrawable fill = shape(surface, 20);
    fill.setStroke(dp(1), light ? 0xFFE2E8F2 : 0xFF27364D);
    cell.setBackground(fill);
    copy(cell, symbol + "  " + title, 12, muted, false);
    gap(cell, 9);
    copy(cell, value, 23, text, true);
    weight(parent, cell);
  }

  private Context dialogContext() {
    return new ContextThemeWrapper(
        this,
        light
            ? android.R.style.Theme_Material_Light_Dialog_Alert
            : android.R.style.Theme_Material_Dialog_Alert);
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_LONG).show();
  }

  private void sources(LinearLayout parent) {
    LinearLayout c = card(parent);
    copy(c, "KAYNAK VE KAPSAM", 10, accent, true);
    gap(c, 6);
    copy(
        c,
        "Hava: Open-Meteo • Şehir: GeoNames\n"
            + "Veri lisansı: CC BY 4.0. Değerler yuvarlanıp Türkçe gösterilir; sağlayıcı onayı ima"
            + " edilmez. Kişisel ve ticari olmayan kullanım içindir. Tahminler değişebilir; resmî"
            + " meteoroloji uyarılarını ayrıca kontrol et.",
        12,
        muted,
        false);
    gap(c, 10);
    add(
        c,
        button(
            "Open-Meteo ve kullanım koşulları ↗",
            false,
            () -> open("https://open-meteo.com/en/terms")));
    gap(c, 6);
    add(
        c,
        button(
            "CC BY 4.0 lisansı ↗",
            false,
            () -> open("https://creativecommons.org/licenses/by/4.0/")));
  }

  private void open(String url) {
    try {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    } catch (ActivityNotFoundException e) {
      toast("Tarayıcı bulunamadı.");
    }
  }

  @Override
  public void onRequestPermissionsResult(int r, String[] p, int[] grants) {
    super.onRequestPermissionsResult(r, p, grants);
    render();
  }
}
