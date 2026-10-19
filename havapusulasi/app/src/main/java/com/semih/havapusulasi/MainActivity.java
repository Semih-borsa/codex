package com.semih.havapusulasi;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
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
    t.setLineSpacing(dp(2), 1.1f);
    if (bold) t.setTypeface(null, Typeface.BOLD);
    add(p, t);
    return t;
  }

  private LinearLayout card(LinearLayout p) {
    LinearLayout c = column();
    c.setPadding(dp(20), dp(18), dp(20), dp(18));
    c.setBackground(shape(surface, 24));
    LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-1, -2);
    l.bottomMargin = dp(14);
    p.addView(c, l);
    return c;
  }

  private Button button(String title, boolean primary, Runnable action) {
    Button b = new Button(this);
    b.setText(title);
    b.setAllCaps(false);
    b.setTextColor(primary ? (light ? Color.WHITE : 0xFF09252B) : accent);
    b.setTextSize(14);
    b.setMinHeight(dp(48));
    b.setMinimumHeight(dp(48));
    b.setPadding(dp(12), dp(8), dp(12), dp(8));
    b.setBackground(shape(primary ? accent : (light ? 0xFFEAF1F1 : 0xFF19313E), 16));
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
    bg = light ? 0xFFF3F7F5 : 0xFF0B1824;
    surface = light ? Color.WHITE : 0xFF142936;
    text = light ? 0xFF13333C : 0xFFEAF5F4;
    muted = light ? 0xFF506872 : 0xFFA6BDC5;
    accent = light ? 0xFF13747B : 0xFF76D2CF;
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
    header.setPadding(dp(20), dp(14), dp(20), dp(12));
    LinearLayout titles = column();
    copy(titles, "HAVA PUSULASI", 11, accent, true);
    copy(
        titles,
        page == 0
            ? "Günün rotasını belirle."
            : page == 1 ? "Şehirlerin, tek yerde." : "Kontrol sende.",
        21,
        text,
        true);
    weight(header, titles);
    TextView star = new TextView(this);
    star.setText("✦");
    star.setTextSize(28);
    star.setTextColor(accent);
    header.addView(star);
    add(root, header);
    scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    body = column();
    body.setPadding(dp(18), dp(8), dp(18), dp(20));
    scroll.addView(body);
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
    nav.setPadding(dp(10), dp(8), dp(10), dp(10));
    String[] names = {"◈  Tahmin", "⌖  Şehirler", "☷  Ayarlar"};
    for (int i = 0; i < 3; i++) {
      final int index = i;
      weight(nav, button(names[i], page == i, () -> navigate(index)));
    }
    add(root, nav);
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
            GradientDrawable.Orientation.TL_BR,
            light ? new int[] {0xFFDEEEEA, 0xFFEAF3EE} : new int[] {0xFF1A4148, 0xFF182D42});
    gradient.setCornerRadius(dp(26));
    hero.setBackground(gradient);
    copy(hero, city.label(), 21, text, true);
    gap(hero, 6);
    copy(
        hero,
        f == null ? "Henüz indirilmiş tahmin yok" : f.status(now),
        12,
        current == null || f != null && f.stale(now) ? (light ? 0xFF8F5D18 : 0xFFE9BE77) : accent,
        true);
    LinearLayout weather = row();
    LinearLayout temp = column();
    copy(temp, current == null ? "—" : Forecast.value(current.temperature, "°"), 58, text, true);
    copy(
        temp,
        current == null ? "Tahmini indirerek başla" : Forecast.description(current.code),
        17,
        text,
        true);
    copy(temp, "Bu saatin model tahmini", 12, muted, false);
    weight(weather, temp);
    weather.addView(
        new WeatherArt(this, current == null ? null : current.code),
        new LinearLayout.LayoutParams(dp(150), dp(140)));
    add(hero, weather);
    if (current != null) {
      gap(hero, 8);
      copy(
          hero,
          "Hissedilen "
              + Forecast.value(current.feels, "°C")
              + "    Yağış "
              + Forecast.value(current.rain, "%"),
          14,
          text,
          false);
      copy(
          hero,
          "Rüzgâr "
              + Forecast.value(current.wind, " km/sa")
              + "    Nem "
              + Forecast.value(current.humidity, "%"),
          14,
          muted,
          false);
    }
    gap(hero, 16);
    Button refresh =
        button(busy ? "Tahmin indiriliyor…" : "Tahmini yenile", true, () -> refresh(city));
    refresh.setEnabled(!busy);
    add(hero, refresh);
    if (f != null)
      copy(hero, "İndirildi: " + f.time(f.downloaded) + " • " + f.zone, 11, muted, false);
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
        item.setBackground(shape(surface, 19));
        copy(item, f.time(h.at), 11, muted, false);
        copy(item, Forecast.icon(h.code), 25, accent, false);
        copy(item, Forecast.value(h.temperature, "°C"), 22, text, true);
        copy(item, Forecast.value(h.rain, "%") + " yağış", 11, muted, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(125), -2);
        lp.rightMargin = dp(9);
        list.addView(item, lp);
      }
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
          copy(
              day,
              d.date.format(DateTimeFormatter.ofPattern("d MMM, EEEE", Forecast.TR)),
              14,
              text,
              true);
          copy(
              day,
              Forecast.description(d.code)
                  + "   "
                  + Forecast.value(d.low, "°")
                  + " / "
                  + Forecast.value(d.high, "°"),
              14,
              muted,
              false);
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
                  new AlertDialog.Builder(this)
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
        "Sıcaklık, yağış ve son yenileme saati uygulamayı açmadan görünür. Her widget için ayrı"
            + " şehir seçebilir veya uygulamadaki seçimi izleyebilirsin. Yenile düğmesi internet"
            + " üzerinden yalnızca o şehri ister; Şehir düğmesi seçimi değiştirir.",
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
    copy(info, "HavaPusulası 1.0", 19, text, true);
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
