package com.semih.havapusulasi;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import java.util.*;

public final class WidgetConfigureActivity extends Activity {
  public void onCreate(Bundle state) {
    super.onCreate(state);
    setResult(RESULT_CANCELED);
    int id =
        getIntent()
            .getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    if (!WeatherWidget.owns(this, id)) {
      finish();
      return;
    }
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    int pad = (int) (24 * getResources().getDisplayMetrics().density);
    root.setPadding(pad, pad, pad, pad);
    root.setBackgroundColor(0xFF0A1220);
    setContentView(root);
    TextView title = new TextView(this);
    title.setText(R.string.widget_configure_title);
    title.setTextSize(24);
    title.setTextColor(Color.WHITE);
    root.addView(title);
    TextView note = new TextView(this);
    note.setText(R.string.widget_configure_note);
    note.setTextSize(14);
    note.setTextColor(0xFFA9B8CF);
    root.addView(note);
    List<City> cities;
    try (WeatherStore store = new WeatherStore(this)) {
      cities = store.cities();
    }
    if (cities.isEmpty()) {
      Button open = new Button(this);
      open.setText(R.string.widget_add_city);
      root.addView(open);
      open.setOnClickListener(
          v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
          });
      return;
    }
    List<String> options = new ArrayList<>();
    options.add("Uygulamada seçili şehri izle");
    for (City city : cities) options.add(city.label());
    Spinner choice = new Spinner(this);
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    choice.setAdapter(adapter);
    root.addView(choice);
    long current = WeatherWidget.prefs(this).getLong("city_" + id, 0);
    for (int i = 0; i < cities.size(); i++)
      if (cities.get(i).id == current) choice.setSelection(i + 1);
    Button save = new Button(this);
    save.setText(R.string.widget_save);
    root.addView(save);
    save.setOnClickListener(
        v -> {
          if (!WeatherWidget.owns(this, id)) {
            finish();
            return;
          }
          int position = choice.getSelectedItemPosition();
          long city = position <= 0 ? 0 : cities.get(position - 1).id;
          WeatherWidget.prefs(this)
              .edit()
              .putLong("city_" + id, city)
              .putString("state_" + id, "")
              .apply();
          WeatherWidget.update(this, id);
          RefreshJob.configure(this);
          setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id));
          finish();
        });
  }
}
