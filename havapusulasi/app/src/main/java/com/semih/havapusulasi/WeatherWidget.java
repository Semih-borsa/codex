package com.semih.havapusulasi;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.RemoteViews;
import java.util.*;

public final class WeatherWidget extends AppWidgetProvider {
  static final String NEXT = "com.semih.havapusulasi.WIDGET_NEXT_DAYS";
  static final String REFRESH = "com.semih.havapusulasi.WIDGET_REFRESH";

  static ComponentName component(Context c) {
    return new ComponentName(c, WeatherWidget.class);
  }

  static int[] ids(Context c) {
    return AppWidgetManager.getInstance(c).getAppWidgetIds(component(c));
  }

  static boolean owns(Context c, int id) {
    AppWidgetProviderInfo info = AppWidgetManager.getInstance(c).getAppWidgetInfo(id);
    return info != null && component(c).equals(info.provider);
  }

  static SharedPreferences prefs(Context c) {
    return c.getSharedPreferences("widgets", Context.MODE_PRIVATE);
  }

  static City city(Context c, int widget, List<City> cities) {
    long id = prefs(c).getLong("city_" + widget, 0);
    if (id == 0) {
      id = DailyReminder.prefs(c).getLong("selected", 0);
      for (City city : cities) if (city.id == id) return city;
      return cities.isEmpty() ? null : cities.get(0);
    }
    for (City city : cities) if (city.id == id) return city;
    return null;
  }

  static List<Long> cityIds(Context c) {
    List<Long> list = new ArrayList<>();
    try (WeatherStore store = new WeatherStore(c)) {
      List<City> cities = store.cities();
      for (int id : ids(c)) {
        City city = city(c, id, cities);
        if (city != null) list.add(city.id);
      }
    }
    return list;
  }

  static void state(Context c, int id, String message) {
    if (!owns(c, id)) return;
    prefs(c)
        .edit()
        .putString("state_" + id, message)
        .putLong("stateAt_" + id, System.currentTimeMillis())
        .apply();
    update(c, id);
  }

  private static CharSequence metricText(String label, String value) {
    android.text.SpannableString text = new android.text.SpannableString(label + "\n" + value);
    int start = label.length() + 1;
    text.setSpan(
        new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, text.length(), 0);
    text.setSpan(new android.text.style.RelativeSizeSpan(16f / 13f), start, text.length(), 0);
    text.setSpan(
        new android.text.style.ForegroundColorSpan(android.graphics.Color.WHITE),
        start,
        text.length(),
        0);
    return text;
  }

  static RemoteViews views(
      Context c, int id, City city, Forecast f, long now, String state, boolean compact) {
    RemoteViews views = new RemoteViews(c.getPackageName(), R.layout.weather_widget);
    Forecast.Hour h = f == null ? null : f.current(now);
    views.setTextViewText(R.id.widget_city, city == null ? "Şehir seç" : city.name);
    views.setTextViewText(
        R.id.widget_temperature, h == null ? "—" : Forecast.value(h.temperature, "°"));
    views.setTextViewTextSize(
        R.id.widget_temperature, android.util.TypedValue.COMPLEX_UNIT_SP, compact ? 34 : 44);
    views.setTextViewText(R.id.widget_symbol, Forecast.icon(h == null ? null : h.code));
    views.setTextViewText(
        R.id.widget_details,
        h == null
            ? "Bu saatin tahmini yok"
            : Forecast.description(h.code) + " • Yağış " + Forecast.value(h.rain, "%"));
    views.setViewVisibility(R.id.widget_details, View.VISIBLE);
    views.setTextViewText(
        R.id.widget_feels,
        metricText("Hissedilen", Forecast.value(h == null ? null : h.feels, "°C")));
    views.setTextViewText(
        R.id.widget_wind,
        metricText("Rüzgâr", Forecast.value(h == null ? null : h.wind, " km/sa")));
    views.setTextViewText(
        R.id.widget_humidity,
        metricText("Nem", Forecast.value(h == null ? null : h.humidity, "%")));
    List<Forecast.Day> days = f == null ? Collections.emptyList() : f.futureDays(now);
    int pages = Math.max(1, (days.size() + 2) / 3);
    int page = Math.floorMod(prefs(c).getInt("page_" + id, 0), pages);
    boolean showDays = !compact && !days.isEmpty();
    views.setViewVisibility(R.id.widget_forecast_header, compact ? View.GONE : View.VISIBLE);
    views.setViewVisibility(R.id.widget_forecast_labels, showDays ? View.VISIBLE : View.GONE);
    views.setViewVisibility(R.id.widget_forecast_rows, showDays ? View.VISIBLE : View.GONE);
    views.setViewVisibility(R.id.widget_next, showDays && pages > 1 ? View.VISIBLE : View.GONE);
    views.setTextViewText(
        R.id.widget_forecast_title,
        "Gelecek günler" + (showDays ? " · " + (page + 1) + "/" + pages : ""));
    views.setViewVisibility(R.id.widget_forecast_empty, showDays ? View.GONE : View.VISIBLE);
    views.setTextViewText(
        R.id.widget_forecast_empty,
        compact
            ? "Günlük tahminler için widget’ı büyüt."
            : "Gelecek günlere ait kayıt yok; tahmini yenile.");
    int[] rows = {R.id.widget_day_row_0, R.id.widget_day_row_1, R.id.widget_day_row_2};
    int[] dates = {R.id.widget_day_date_0, R.id.widget_day_date_1, R.id.widget_day_date_2};
    int[] conditions = {
      R.id.widget_day_condition_0, R.id.widget_day_condition_1, R.id.widget_day_condition_2
    };
    int[] ranges = {R.id.widget_day_range_0, R.id.widget_day_range_1, R.id.widget_day_range_2};
    for (int i = 0; i < 3; i++) {
      int index = page * 3 + i;
      views.setViewVisibility(rows[i], index < days.size() ? View.VISIBLE : View.GONE);
      if (index >= days.size()) continue;
      Forecast.Day day = days.get(index);
      views.setTextViewText(
          dates[i],
          day.date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM EEE", Forecast.TR)));
      views.setTextViewText(
          conditions[i], Forecast.icon(day.code) + " " + Forecast.description(day.code));
      views.setTextViewText(
          ranges[i],
          c.getString(
              R.string.temperature_range,
              Forecast.value(day.high, "°"),
              Forecast.value(day.low, "°")));
      views.setContentDescription(
          rows[i],
          day.date
              + ", "
              + Forecast.description(day.code)
              + ", en yüksek "
              + Forecast.value(day.high, " derece")
              + ", en düşük "
              + Forecast.value(day.low, " derece"));
    }
    String age =
        city == null
            ? "Seçilen şehir yok; widget’ın şehir seçimini güncelle."
            : f == null
                ? "Henüz indirilmiş tahmin yok"
                : f.lastUpdated(now, java.time.ZoneId.systemDefault())
                    + (f.upcoming(now, 1).isEmpty()
                        ? "\nTahmin süresi doldu"
                        : now < f.downloaded
                            ? "\nCihaz saati değişmiş"
                            : f.stale(now) ? "\nEski kayıt" : "");
    views.setTextViewText(R.id.widget_age, age);
    String automatic =
        !RefreshJob.enabled(c)
            ? "Otomatik kapalı • Ayarlar’dan aç"
            : "Otomatik: "
                + SyncPolicy.minutes(DailyReminder.prefs(c).getInt("refreshMinutes", 30))
                + " dk"
                + (DailyReminder.prefs(c).getBoolean("wifiOnly", true)
                    ? " • Yalnızca Wi-Fi"
                    : " • İnternet");
    views.setTextViewText(R.id.widget_state, state.isEmpty() ? automatic : state);
    views.setContentDescription(
        R.id.widget_root,
        (city == null ? "Şehir seç" : city.label())
            + ". "
            + (h == null
                ? "Tahmin yok"
                : Forecast.value(h.temperature, " derece") + ", " + Forecast.description(h.code))
            + ". "
            + age
            + ". "
            + (state.isEmpty() ? automatic : state));
    Intent open =
        new Intent(c, MainActivity.class)
            .putExtra("cityId", city == null ? 0 : city.id)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    PendingIntent launch =
        PendingIntent.getActivity(
            c, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    for (int target :
        new int[] {
          R.id.widget_root,
          R.id.widget_city,
          R.id.widget_date,
          R.id.widget_temperature,
          R.id.widget_symbol,
          R.id.widget_details,
          R.id.widget_age,
          R.id.widget_state,
          R.id.widget_host
        }) views.setOnClickPendingIntent(target, launch);
    for (int target :
        new int[] {
          R.id.widget_feels,
          R.id.widget_wind,
          R.id.widget_humidity,
          R.id.widget_forecast_title,
          R.id.widget_forecast_empty,
          R.id.widget_day_row_0,
          R.id.widget_day_row_1,
          R.id.widget_day_row_2
        }) views.setOnClickPendingIntent(target, launch);
    views.setOnClickPendingIntent(
        R.id.widget_next,
        PendingIntent.getBroadcast(
            c,
            id,
            new Intent(c, WeatherWidget.class)
                .setAction(NEXT)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .setData(Uri.parse("havapusulasi://days/" + id)),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    return views;
  }

  static void update(Context c, int id) {
    if (!owns(c, id)) return;
    try (WeatherStore store = new WeatherStore(c)) {
      City city = city(c, id, store.cities());
      Forecast f = city == null ? null : store.cached(city.id);
      AppWidgetManager manager = AppWidgetManager.getInstance(c);
      boolean compact =
          manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 430)
              < 420;
      String status = prefs(c).getString("state_" + id, "");
      if (f != null && f.downloaded >= prefs(c).getLong("stateAt_" + id, 0)) status = "";
      manager.updateAppWidget(
          id, views(c, id, city, f, System.currentTimeMillis(), status, compact));
    }
  }

  static void updateAll(Context c) {
    for (int id : ids(c)) update(c, id);
  }

  public void onUpdate(Context c, AppWidgetManager manager, int[] ids) {
    for (int id : ids) update(c, id);
    RefreshJob.configure(c);
  }

  public void onAppWidgetOptionsChanged(
      Context c, AppWidgetManager manager, int id, Bundle options) {
    update(c, id);
  }

  public void onReceive(Context c, Intent intent) {
    super.onReceive(c, intent);
    if (NEXT.equals(intent.getAction())) {
      int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
      if (owns(c, id)) {
        int page = prefs(c).getInt("page_" + id, 0);
        // The view normalizes this bounded preference to the available day count.
        prefs(c).edit().putInt("page_" + id, Math.floorMod(page + 1, 2)).apply();
        update(c, id);
      }
    }
    if (REFRESH.equals(intent.getAction())) {
      int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
      if (owns(c, id)) RefreshJob.requestWidget(c, id);
    }
  }

  public void onDeleted(Context c, int[] ids) {
    SharedPreferences.Editor edit = prefs(c).edit();
    for (int id : ids) {
      if (id > 0 && id < Integer.MAX_VALUE - RefreshJob.WIDGET_BASE)
        c.getSystemService(android.app.job.JobScheduler.class).cancel(RefreshJob.WIDGET_BASE + id);
      edit.remove("city_" + id).remove("state_" + id).remove("stateAt_" + id).remove("page_" + id);
    }
    edit.apply();
  }

  public void onRestored(Context c, int[] oldIds, int[] newIds) {
    SharedPreferences p = prefs(c);
    SharedPreferences.Editor edit = p.edit();
    for (int i = 0; i < Math.min(oldIds.length, newIds.length); i++)
      edit.putLong("city_" + newIds[i], p.getLong("city_" + oldIds[i], 0));
    edit.apply();
    updateAll(c);
  }
}
