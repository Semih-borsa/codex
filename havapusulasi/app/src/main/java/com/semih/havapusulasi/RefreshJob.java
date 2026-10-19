package com.semih.havapusulasi;

import android.app.job.*;
import android.content.*;
import android.os.PersistableBundle;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RefreshJob extends JobService {
  static final int ID = 7104, WIDGET_BASE = 200000000;
  private final ExecutorService worker = Executors.newFixedThreadPool(2);
  private final ConcurrentHashMap<Integer, Run> runs = new ConcurrentHashMap<>();

  private static final class Run {
    final AtomicBoolean stopped = new AtomicBoolean();
    final int widget;

    Run(int widget) {
      this.widget = widget;
    }
  }

  static void configure(Context c) {
    JobScheduler jobs = c.getSystemService(JobScheduler.class);
    android.content.SharedPreferences p = DailyReminder.prefs(c);
    if (!p.getBoolean("background", false)) {
      jobs.cancel(ID);
      return;
    }
    int minutes = SyncPolicy.minutes(p.getInt("refreshMinutes", 30));
    int network =
        p.getBoolean("wifiOnly", true) ? JobInfo.NETWORK_TYPE_UNMETERED : JobInfo.NETWORK_TYPE_ANY;
    JobInfo existing = jobs.getPendingJob(ID);
    if (existing != null
        && existing.getIntervalMillis() == SyncPolicy.interval(minutes)
        && existing.getNetworkType() == network) return;
    int result =
        jobs.schedule(
            new JobInfo.Builder(ID, new ComponentName(c, RefreshJob.class))
                .setRequiredNetworkType(network)
                .setPersisted(true)
                .setPeriodic(SyncPolicy.interval(minutes), SyncPolicy.flex(minutes))
                .build());
    if (result != JobScheduler.RESULT_SUCCESS)
      throw new IllegalStateException("Android arka plan işini planlayamadı.");
  }

  static boolean requestWidget(Context c, int widget) {
    if (!WeatherWidget.owns(c, widget) || widget <= 0 || widget > Integer.MAX_VALUE - WIDGET_BASE)
      return false;
    JobScheduler jobs = c.getSystemService(JobScheduler.class);
    int id = WIDGET_BASE + widget;
    if (jobs.getPendingJob(id) != null) return true;
    PersistableBundle extras = new PersistableBundle();
    extras.putInt("widget", widget);
    WeatherWidget.state(c, widget, "Yenileme istendi; bağlantı bekleniyor…");
    int result =
        jobs.schedule(
            new JobInfo.Builder(id, new ComponentName(c, RefreshJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setExtras(extras)
                .build());
    if (result != JobScheduler.RESULT_SUCCESS) {
      WeatherWidget.state(c, widget, "İstek planlanamadı; yeniden dene.");
      return false;
    }
    return true;
  }

  static List<City> ordered(List<City> cities, long selected, List<Long> widgets) {
    LinkedHashMap<Long, City> out = new LinkedHashMap<>();
    for (long id : widgets) for (City c : cities) if (c.id == id) out.put(c.id, c);
    for (City c : cities) if (c.id == selected) out.put(c.id, c);
    for (City c : cities) out.put(c.id, c);
    return new ArrayList<>(out.values());
  }

  public boolean onStartJob(JobParameters params) {
    int widget = params.getExtras().getInt("widget", 0);
    Run run = new Run(widget);
    Run old = runs.put(params.getJobId(), run);
    if (old != null) old.stopped.set(true);
    worker.execute(
        () -> {
          boolean failed = false;
          int updated = 0, consecutiveFailures = 0;
          try (WeatherStore store = new WeatherStore(this)) {
            List<City> cities = store.cities();
            if (widget > 0) {
              City chosen =
                  WeatherWidget.owns(this, widget)
                      ? WeatherWidget.city(this, widget, cities)
                      : null;
              cities = chosen == null ? Collections.emptyList() : Collections.singletonList(chosen);
              if (cities.isEmpty()) failed = true;
            } else
              cities =
                  ordered(
                      cities,
                      DailyReminder.prefs(this).getLong("selected", 0),
                      WeatherWidget.cityIds(this));
            for (City city : cities) {
              if (run.stopped.get()
                  || widget == 0 && !DailyReminder.prefs(this).getBoolean("background", false))
                break;
              try {
                String raw = WeatherApi.fetch(WeatherApi.forecastUrl(city));
                if (!run.stopped.get()
                    && (widget > 0 || DailyReminder.prefs(this).getBoolean("background", false))) {
                  store.save(city.id, raw, System.currentTimeMillis());
                  updated++;
                  consecutiveFailures = 0;
                  WeatherWidget.updateAll(this);
                }
              } catch (Exception e) {
                failed = true;
                if (++consecutiveFailures >= 2) break;
              }
            }
            if (!run.stopped.get()) {
              if (widget > 0)
                WeatherWidget.state(
                    this,
                    widget,
                    failed ? "Yenilenemedi; bağlantıyı ve şehir seçimini kontrol et." : "");
              else
                DailyReminder.prefs(this)
                    .edit()
                    .putLong("backgroundAttempt", System.currentTimeMillis())
                    .putBoolean("backgroundFailed", failed)
                    .putInt("backgroundUpdated", updated)
                    .apply();
            }
          } finally {
            runs.remove(params.getJobId(), run);
            if (!run.stopped.get()) jobFinished(params, false);
          }
        });
    return true;
  }

  public boolean onStopJob(JobParameters params) {
    Run run = runs.remove(params.getJobId());
    if (run != null) {
      run.stopped.set(true);
      if (run.widget > 0)
        WeatherWidget.state(this, run.widget, "Yenileme durduruldu; yeniden dene.");
    }
    return false;
  }

  public void onDestroy() {
    for (Run run : runs.values()) run.stopped.set(true);
    worker.shutdown();
    super.onDestroy();
  }
}
