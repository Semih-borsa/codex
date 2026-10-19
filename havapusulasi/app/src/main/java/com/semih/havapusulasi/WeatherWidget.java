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
    views.setViewVisibility(R.id.widget_details, compact ? View.GONE : View.VISIBLE);
    String age =
        city == null
            ? "Seçilen şehir yok; Şehir düğmesinden seç."
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
    views.setOnClickPendingIntent(R.id.widget_open, launch);
    Intent configure =
        new Intent(c, WidgetConfigureActivity.class)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .setData(Uri.parse("havapusulasi://configure/" + id));
    views.setOnClickPendingIntent(
        R.id.widget_configure,
        PendingIntent.getActivity(
            c, id, configure, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    Intent refresh =
        new Intent(c, WeatherWidget.class)
            .setAction(REFRESH)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .setData(Uri.parse("havapusulasi://refresh/" + id));
    views.setOnClickPendingIntent(
        R.id.widget_refresh,
        PendingIntent.getBroadcast(
            c, id, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    return views;
  }

  static void update(Context c, int id) {
    if (!owns(c, id)) return;
    try (WeatherStore store = new WeatherStore(c)) {
      City city = city(c, id, store.cities());
      Forecast f = city == null ? null : store.cached(city.id);
      AppWidgetManager manager = AppWidgetManager.getInstance(c);
      boolean compact =
          manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
              < 170;
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
      edit.remove("city_" + id).remove("state_" + id).remove("stateAt_" + id);
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
