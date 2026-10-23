package com.semih.sicil360;

import static com.semih.sicil360.Model.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Local-first UI. No network permission, automatic sending, accessibility automation or analytics.
 */
public final class MainActivity extends Activity {
  static final int IMPORT = 10, EXPORT = 11, BACKUP = 12, RESTORE = 13, SAMPLE = 14, UNLOCK = 30;
  final ExecutorService worker = Executors.newSingleThreadExecutor();
  final Handler main = new Handler(Looper.getMainLooper());
  State state;
  Vault vault;
  LinearLayout root, body, nav;
  ScrollView scroll;
  TextView status;
  EditText search, preview;
  Spinner templateSelect;
  LinearLayout results;
  TextView characters;
  boolean busy, failed, dark, locked, authenticating, externalPending, destroyed;
  int screen, searchToken;
  final List<android.app.Dialog> dialogs = new ArrayList<>();
  AlertDialog lastDialog;

  interface Gateway {
    List<String> packages();

    void open(Intent intent);
  }

  Gateway gateway =
      new Gateway() {
        public List<String> packages() {
          return installedWhatsApp();
        }

        public void open(Intent intent) {
          startActivity(intent);
        }
      };
  Runnable deferredResult, deferredCompletion;
  AlertDialog mappingWindow;
  int historyPage, directoryToken;
  String selected = "",
      selectedTemplate = "",
      message = "",
      query = "",
      pendingEvent = "",
      notice = "";
  boolean templateLoading;
  final LinkedHashSet<String> selectedPeople = new LinkedHashSet<>();
  final ArrayList<String> queue = new ArrayList<>();
  int queueIndex;
  final DateTimeFormatter date =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", new Locale("tr", "TR"));
  int bg, card, ink, muted, blue, border;
  android.content.SharedPreferences prefs;
  File importing;
  Excel.Table importTable;
  int importHeader;
  List<String> importHeaders;
  final int[] mapping = new int[9];
  Model.ImportMode importMode = Model.ImportMode.KEEP;
  boolean extraColumns;
  List<Row> importRows;
  final Map<String, Integer> resolutions = new HashMap<>();
  Model.Plan importPlan;
  char[] backupPassword;

  interface Work<T> {
    T run() throws Exception;
  }

  interface Done<T> {
    void accept(T value) throws Exception;
  }

  @Override
  public void onCreate(Bundle saved) {
    prefs = getSharedPreferences("settings", 0);
    dark =
        prefs.getInt("theme", 0) == 2
            || (prefs.getInt("theme", 0) == 0
                && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES);
    setTheme(dark ? R.style.AppThemeDark : R.style.AppTheme);
    super.onCreate(saved);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    vault = new Vault(this);
    if (saved != null) {
      screen = saved.getInt("screen");
      selected = saved.getString("selected", "");
      selectedTemplate = saved.getString("template", "");
      message = saved.getString("message", "");
      query = saved.getString("query", "");
      pendingEvent = saved.getString("event", "");
    }
    locked = prefs.getBoolean("lock", false);
    render();
    task(
        () -> {
          File[] cached = getCacheDir().listFiles();
          if (cached != null)
            for (File f : cached)
              if (f.getName().startsWith("sicil-import-")
                  && f.lastModified() < System.currentTimeMillis() - 3600000) f.delete();
          return vault.load();
        },
        value -> {
          state = value;
          render();
          if (locked) {
            render();
            authenticate();
          } else checkReturn();
        });
  }

  @Override
  public void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    capture();
    out.putInt("screen", screen);
    out.putString("selected", selected);
    out.putString("template", selectedTemplate);
    out.putString("message", message);
    out.putString("query", query);
    out.putString("event", pendingEvent);
  }

  @Override
  public void onConfigurationChanged(Configuration c) {
    super.onConfigurationChanged(c);
    capture();
    render();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (state != null) {
      if (locked) {
        render();
        authenticate();
      } else checkReturn();
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (prefs != null && prefs.getBoolean("lock", false) && !authenticating) {
      locked = true;
      for (android.app.Dialog d : new ArrayList<>(dialogs)) d.dismiss();
      dialogs.clear();
    }
  }

  @Override
  public void onDestroy() {
    destroyed = true;
    main.removeCallbacksAndMessages(null);
    worker.shutdown();
    if (importing != null) importing.delete();
    if (backupPassword != null) Arrays.fill(backupPassword, '\0');
    super.onDestroy();
  }

  void capture() {
    if (preview != null) message = preview.getText().toString();
    if (search != null) query = search.getText().toString();
  }

  void colors() {
    bg = dark ? 0xFF151C20 : 0xFFF2F9F7;
    card = dark ? 0xFF232D32 : Color.WHITE;
    ink = dark ? 0xFFF0F6F4 : 0xFF123B42;
    muted = dark ? 0xFFAFBFBD : 0xFF58716F;
    blue = dark ? 0xFF38D9AD : 0xFF006A70;
    border = dark ? 0xFF354441 : 0xFFDCECE6;
  }

  int dp(float n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  TextView text(String s, int size, int color) {
    TextView v = new TextView(this);
    v.setText(s);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setPadding(0, dp(5), 0, dp(5));
    return v;
  }

  TextView bold(String s, int size) {
    TextView t = text(s, size, ink);
    t.setTypeface(null, Typeface.BOLD);
    return t;
  }

  LinearLayout column() {
    LinearLayout v = new LinearLayout(this);
    v.setOrientation(LinearLayout.VERTICAL);
    return v;
  }

  LinearLayout row() {
    LinearLayout v = new LinearLayout(this);
    v.setGravity(Gravity.CENTER_VERTICAL);
    return v;
  }

  void gap(LinearLayout parent, int size) {
    View v = new View(this);
    parent.addView(v, new LinearLayout.LayoutParams(1, dp(size)));
  }

  LinearLayout card(LinearLayout parent) {
    LinearLayout c = column();
    c.setPadding(dp(14), dp(9), dp(14), dp(9));
    c.setBackground(Visuals.surface(card, dp(20), border));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.bottomMargin = dp(9);
    parent.addView(c, p);
    return c;
  }

  Button button(String title, Runnable action) {
    Button b = new Button(this);
    b.setText(title);
    b.setAllCaps(false);
    b.setTextColor(Color.WHITE);
    b.setTextSize(14);
    b.setMinHeight(dp(48));
    b.setPadding(dp(12), dp(7), dp(12), dp(7));
    b.setBackground(Visuals.touch(dark ? 0xFF087C62 : 0xFF006A70, dp(13), 0));
    b.setOnClickListener(
        v -> {
          if (!busy && !failed && !locked) action.run();
        });
    return b;
  }

  void action(LinearLayout parent, String title, Runnable run) {
    Button b = button(title, run);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(0, dp(5), 0, dp(5));
    parent.addView(b, p);
  }

  void whatsappAction(LinearLayout parent) {
    LinearLayout cta = row();
    cta.setTag("whatsapp-open");
    cta.setGravity(Gravity.CENTER);
    cta.setMinimumHeight(dp(54));
    cta.setPadding(dp(12), dp(10), dp(12), dp(10));
    cta.setBackground(Visuals.whatsappButton(dp(15), dark));
    cta.setContentDescription("WhatsApp'ta Aç");
    cta.setFocusable(true);
    cta.setClickable(true);
    cta.setAccessibilityDelegate(
        new View.AccessibilityDelegate() {
          @Override
          public void onInitializeAccessibilityNodeInfo(
              View host, android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName(Button.class.getName());
          }
        });
    android.widget.ImageView icon = new android.widget.ImageView(this);
    icon.setImageDrawable(new Visuals.Icon("whatsapp", Color.WHITE, dp(27)));
    icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    cta.addView(icon, new LinearLayout.LayoutParams(dp(27), dp(27)));
    TextView label = text("WhatsApp'ta Aç", 17, Color.WHITE);
    label.setTypeface(null, Typeface.BOLD);
    label.setPadding(dp(10), 0, 0, 0);
    label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    cta.addView(label);
    cta.setOnClickListener(
        v -> {
          if (!busy && !failed && !locked) prepareMessage();
        });
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.setMargins(0, dp(5), 0, dp(3));
    parent.addView(cta, params);
  }

  EditText input(LinearLayout parent, String label, String initial, boolean multiline) {
    TextView caption = text(label, 12, muted);
    parent.addView(caption);
    EditText e = new EditText(this);
    e.setId(View.generateViewId());
    caption.setLabelFor(e.getId());
    e.setText(initial);
    e.setTextSize(15);
    e.setTextColor(ink);
    e.setHintTextColor(muted);
    e.setSingleLine(!multiline);
    e.setPadding(dp(10), dp(9), dp(10), dp(9));
    e.setBackground(Visuals.surface(dark ? 0xFF1A2428 : 0xFFF0F8F5, dp(10), border));
    e.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT
            | (multiline ? android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
    if (multiline) {
      e.setMinLines(3);
      e.setGravity(Gravity.TOP);
    }
    parent.addView(e, new LinearLayout.LayoutParams(-1, -2));
    return e;
  }

  Spinner spinner(LinearLayout parent, String label, String[] options) {
    TextView caption = text(label, 12, muted);
    parent.addView(caption);
    Spinner s = new Spinner(this);
    s.setId(View.generateViewId());
    caption.setLabelFor(s.getId());
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(
            new ContextThemeWrapper(this, dark ? R.style.AppThemeDark : R.style.AppTheme),
            android.R.layout.simple_spinner_dropdown_item,
            options);
    s.setAdapter(adapter);
    s.setMinimumHeight(dp(48));
    parent.addView(s, new LinearLayout.LayoutParams(-1, -2));
    return s;
  }

  AlertDialog dialog(String title, LinearLayout content) {
    ScrollView view = new ScrollView(this);
    content.setPadding(dp(16), dp(8), dp(16), dp(8));
    view.addView(content);
    return show(
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Kapat", null));
  }

  AlertDialog show(AlertDialog.Builder builder) {
    AlertDialog d = builder.create();
    lastDialog = d;
    dialogs.add(d);
    d.setOnDismissListener(v -> dialogs.remove(d));
    d.show();
    return d;
  }

  void confirm(String title, String description, Runnable run) {
    show(
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(description)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton(
                "Onayla",
                (d, w) -> {
                  if (!busy && !locked && !failed) run.run();
                }));
  }

  void toast(String s) {
    notice = s;
    if (status != null) status.setText(s);
    Toast.makeText(this, s, Toast.LENGTH_LONG).show();
  }

  <T> void task(Work<T> work, Done<T> done) {
    if (busy || destroyed) return;
    busy = true;
    if (status != null) status.setText("İşleniyor…");
    worker.execute(
        () -> {
          try {
            T value = work.run();
            main.post(
                () -> {
                  if (destroyed) return;
                  busy = false;
                  try {
                    if (locked && state != null) {
                      deferredCompletion =
                          () -> {
                            try {
                              done.accept(value);
                            } catch (Exception problem) {
                              error(problem);
                            }
                          };
                      render();
                      authenticate();
                      return;
                    }
                    done.accept(value);
                    if (status != null && notice.isEmpty())
                      status.setText("Veriler Cihazda · Gönderim Sizin Kontrolünüzde");
                  } catch (Exception e) {
                    error(e);
                  }
                });
          } catch (Exception e) {
            main.post(
                () -> {
                  if (!destroyed) {
                    busy = false;
                    if (state == null) failed = true;
                    render();
                    error(e);
                  }
                });
          }
        });
  }

  void error(Exception e) {
    if (e instanceof java.util.zip.ZipException) {
      toast("Dosya Geçerli Bir XLSX Değil Veya Bozuk. Orijinal Dosya Değişmedi.");
      return;
    }
    if (e instanceof java.io.FileNotFoundException || e instanceof SecurityException) {
      toast("Dosyaya Erişilemiyor. Dosyayı Yeniden Seçin Ve Erişim İznini Kontrol Edin.");
      return;
    }
    String s =
        e instanceof IllegalArgumentException || e instanceof IOException ? e.getMessage() : null;
    if (s == null || s.isEmpty())
      s =
          "İşlem tamamlanamadı. Dosyayı, şifreyi ve izinleri kontrol edin. Orijinal dosya"
              + " değişmedi.";
    toast(s);
  }

  void save(State copy, boolean backup, Runnable after) {
    long expected = state.revision;
    task(
        () -> vault.commit(copy, expected, backup),
        value -> {
          state = value;
          if (after != null) after.run();
        });
  }

  void mutate(java.util.function.Consumer<State> change, boolean backup, Runnable after) {
    State base = state;
    task(
        () -> {
          State copy = Codec.copy(base);
          change.accept(copy);
          return vault.commit(copy, base.revision, backup);
        },
        value -> {
          state = value;
          if (after != null) after.run();
        });
  }

  void render() {
    colors();
    root = column();
    root.setPadding(dp(16), dp(12), dp(16), dp(8));
    root.setBackgroundColor(bg);
    getWindow().setStatusBarColor(bg);
    getWindow().setNavigationBarColor(bg);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    LinearLayout header = row();
    TextView brand = bold("Sicil360 Pro", 23);
    brand.setCompoundDrawablesWithIntrinsicBounds(
        new Visuals.Icon("people", blue, dp(29)), null, null, null);
    brand.setCompoundDrawablePadding(dp(10));
    header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
    TextView local = text("YEREL", 10, blue);
    header.addView(local);
    root.addView(header);
    status =
        text(
            busy
                ? "İşleniyor…"
                : notice.isEmpty() ? "Veriler Cihazda · Gönderim Sizin Kontrolünüzde" : notice,
            11,
            muted);
    status.setMaxLines(2);
    root.addView(status);
    search = null;
    preview = null;
    results = null;
    scroll = new ScrollView(this);
    scroll.setFillViewport(false);
    body = column();
    scroll.addView(body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    if (failed) {
      body.addView(bold("Kayıtlar Açılamadı", 22));
      body.addView(
          text(
              "Verilerin üzerine yazılmasını önlemek için düzenleme kapalı. Uygulama verilerini"
                  + " silmeyin. Dosya bozulması veya cihaz anahtarı değişikliği olabilir.",
              15,
              ink));
    } else if (state == null) body.addView(text("Şifreli Kayıtlar Yükleniyor…", 16, ink));
    else if (locked) {
      body.addView(bold("Uygulama Kilitli", 22));
      Button unlock = button("Cihaz Kilidiyle Aç", () -> {});
      unlock.setOnClickListener(v -> authenticate());
      body.addView(unlock);
    } else {
      switch (screen) {
        case 0:
          home();
          break;
        case 1:
          people();
          break;
        case 2:
          templates();
          break;
        case 3:
          history();
          break;
        default:
          settings();
      }
    }
    if (screen == 0
        && state != null
        && !locked
        && !failed
        && !selected.isEmpty()
        && state.people.containsKey(selected)) whatsappAction(root);
    nav = row();
    nav.setBackground(Visuals.surface(card, dp(20), border));
    String[] titles = {"Ara", "Rehber", "Şablonlar", "Geçmiş", "Ayarlar"};
    String[] icons = {"search", "people", "chat", "chart", "settings"};
    for (int i = 0; i < 5; i++) {
      final int tab = i;
      Button b =
          button(
              titles[i],
              () -> {
                capture();
                screen = tab;
                render();
              });
      b.setTextSize(10);
      b.setMinWidth(0);
      b.setMinimumWidth(0);
      b.setPadding(dp(2), dp(6), dp(2), dp(4));
      b.setTextColor(screen == i ? Color.WHITE : muted);
      b.setBackground(
          Visuals.touch(
              screen == i ? (dark ? 0xFF087C62 : 0xFF006A70) : Color.TRANSPARENT, dp(16), 0));
      b.setCompoundDrawablesWithIntrinsicBounds(
          null, new Visuals.Icon(icons[i], screen == i ? Color.WHITE : muted, dp(22)), null, null);
      nav.addView(b, new LinearLayout.LayoutParams(0, dp(58), 1));
    }
    root.addView(nav);
    setContentView(root);
  }

  void home() {
    if (selected.isEmpty() || !state.people.containsKey(selected)) {
      LinearLayout hero = card(body);
      hero.setBackground(new Visuals.Hero(dp(20)));
      TextView title = text("Doğru Kişi. Hazır Mesaj.", 22, Color.WHITE);
      title.setTypeface(null, Typeface.BOLD);
      hero.addView(title);
      hero.addView(
          text(
              state.people.size() + " Kayıt  ·  " + state.templates.size() + " Şablon",
              13,
              0xFFD6F4E8));
    }
    LinearLayout finder = card(body);
    search = input(finder, "Sicil Numarasıyla Hızlı Ara", query, false);
    search.setTag("sicil-search");
    search.setTextSize(21);
    search.setHint("Sicil, Ad Soyad Veya Telefon");
    search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
    search.setSelectAllOnFocus(true);
    results = column();
    finder.addView(results);
    search.addTextChangedListener(
        watcher(
            () -> {
              query = search.getText().toString();
              searchResults();
            }));
    search.setOnEditorActionListener(
        (v, a, e) -> {
          searchResults();
          return true;
        });
    searchResults();
    if (selected.isEmpty() || !state.people.containsKey(selected)) {
      if (state.people.isEmpty()) action(body, "Excel Dosyası İçe Aktar", () -> chooseOpen(IMPORT));
      else if (!state.recent.isEmpty()) {
        body.addView(bold("Son Kullanılanlar", 17));
        for (String id : state.recent) {
          Person p = state.people.get(id);
          if (p != null) action(body, p.sicil + " · " + p.name(), () -> select(id));
        }
      }
      return;
    }
    Person p = state.people.get(selected);
    LinearLayout detail = card(body);
    LinearLayout identity = row();
    identity.addView(bold(p.name(), 20), new LinearLayout.LayoutParams(0, -2, 1));
    ImageButton edit = new ImageButton(this);
    edit.setImageDrawable(new Visuals.Icon("edit", blue, dp(20)));
    edit.setBackground(Visuals.touch(Color.TRANSPARENT, dp(10), 0));
    edit.setContentDescription("Kişiyi Düzenle");
    edit.setOnClickListener(
        v -> {
          if (!busy && !locked) personDialog(p);
        });
    identity.addView(edit, new LinearLayout.LayoutParams(dp(44), dp(44)));
    detail.addView(identity);
    detail.addView(
        text("Sicil: " + p.sicil + (p.birim.isEmpty() ? "" : "  ·  " + p.birim), 13, muted));
    try {
      detail.addView(text("✓ +" + phone(p.telefon) + " · Biçim Geçerli", 15, blue));
    } catch (Exception invalid) {
      detail.addView(text("Telefon Eksik Veya Geçersiz", 15, 0xFFB04E40));
    }

    LinearLayout composer = card(body);
    String[] options = new String[state.templates.size() + 1];
    options[0] = "Özel Mesaj · Şablon Kullanma";
    int selectedIndex = 0;
    for (int i = 0; i < state.templates.size(); i++) {
      Template t = state.templates.get(i);
      options[i + 1] = t.category + " · " + t.name;
      if (t.id.equals(selectedTemplate)) selectedIndex = i + 1;
    }
    templateSelect = spinner(composer, "Mesaj Şablonu", options);
    templateLoading = true;
    templateSelect.setSelection(selectedIndex);
    preview = input(composer, "Mesaj Önizleme · Düzenleyebilirsiniz", message, true);
    preview.setTag("message-preview");
    preview.setMinLines(3);
    preview.setFilters(new InputFilter[] {new InputFilter.LengthFilter(10000)});
    characters = text(message.length() + " Karakter", 11, muted);
    composer.addView(characters);
    preview.addTextChangedListener(
        watcher(
            () -> {
              message = preview.getText().toString();
              characters.setText(message.length() + " Karakter");
            }));
    templateSelect.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {

          public void onNothingSelected(android.widget.AdapterView<?> parent) {}

          public void onItemSelected(
              android.widget.AdapterView<?> parent, View v, int position, long id) {
            String choice = position == 0 ? "" : state.templates.get(position - 1).id;
            if (choice.equals(selectedTemplate)) return;
            selectedTemplate = choice;
            if (position > 0) {
              message = state.templates.get(position - 1).text;
              preview.setText(message);
            }
          }
        });
    composer.addView(text("Mesajı WhatsApp üzerinden siz gönderirsiniz.", 11, muted));

    if (!queue.isEmpty()) {
      body.addView(bold("Sıralı İşlem · " + (queueIndex + 1) + " / " + queue.size(), 16));
      action(body, "Sonraki Kişi / Bu Kişiyi Atla", this::nextQueue);
      action(
          body,
          "Sıralı İşlemi İptal Et",
          () ->
              confirm(
                  "Kuyruğu İptal Et",
                  "Açık WhatsApp mesajları geri çekilmez. Kalan kişilere geçilmeyecek.",
                  () -> {
                    queue.clear();
                    selectedPeople.clear();
                    render();
                  }));
    }
  }

  TextWatcher watcher(Runnable run) {
    return new TextWatcher() {
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      public void onTextChanged(CharSequence s, int start, int before, int count) {}

      public void afterTextChanged(Editable e) {
        run.run();
      }
    };
  }

  void searchResults() {
    if (results == null || state == null) return;
    int token = ++searchToken;
    String q = query;
    State snapshot = state;
    main.postDelayed(
        () -> {
          if (token != searchToken || destroyed || locked || screen != 0) return;
          worker.execute(
              () -> {
                List<Person> found = Model.search(snapshot, q, 30);
                main.post(
                    () -> {
                      if (token != searchToken
                          || destroyed
                          || locked
                          || results == null
                          || screen != 0) return;
                      results.removeAllViews();
                      if (q.trim().isEmpty() || q.equals(selected)) return;
                      if (found.isEmpty()) {
                        results.addView(text("Sicil / Kişi Bulunamadı.", 14, muted));
                        return;
                      }
                      for (Person p : found)
                        action(results, p.sicil + " · " + p.name(), () -> select(p.sicil));
                      if (found.size() == 30)
                        results.addView(text("İlk 30 Sonuç · Aramayı Daraltın", 12, muted));
                    });
              });
        },
        180);
  }

  void select(String id) {
    capture();
    selected = id;
    query = id;
    screen = 0;
    mutate(s -> Model.recent(s, id), false, this::render);
  }

  void people() {
    body.addView(bold("Kayıt Yönetimi", 24));
    LinearLayout tools = card(body);
    action(tools, "Excel İçe Aktar", () -> chooseOpen(IMPORT));
    action(
        tools,
        "Excel Dışa Aktar",
        () ->
            confirm(
                "Excel Dışa Aktarma",
                "Excel dosyası şifresiz olacak ve telefon bilgilerini içerecek. Güvenli bir konum"
                    + " seçin.",
                () ->
                    chooseCreate(
                        EXPORT,
                        "Sicil360-Kisiler.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
    action(tools, "+ Kişi Ekle", () -> personDialog(null));
    if (!selectedPeople.isEmpty()) {
      action(
          tools, "Seçilenlerle Sıralı İşleme Başla · " + selectedPeople.size(), this::startQueue);
      action(
          tools,
          "Seçimi Temizle",
          () -> {
            selectedPeople.clear();
            render();
          });
    }
    EditText filter = input(body, "Rehberde Ara", "", false);
    LinearLayout list = column();
    body.addView(list);
    Runnable populate =
        () -> {
          String q = filter.getText().toString();
          int token = ++directoryToken;
          State snapshot = state;
          worker.execute(
              () -> {
                List<Person> found = Model.search(snapshot, q, 50);
                main.post(
                    () -> {
                      if (destroyed || locked || screen != 1 || token != directoryToken) return;
                      list.removeAllViews();
                      for (Person p : found) {
                        LinearLayout c = card(list);
                        LinearLayout top = row();
                        CheckBox check = new CheckBox(this);
                        check.setButtonTintList(android.content.res.ColorStateList.valueOf(blue));
                        check.setChecked(selectedPeople.contains(p.sicil));
                        check.setContentDescription(p.name() + " Seç");
                        check.setOnCheckedChangeListener(
                            (v, on) -> {
                              if (on) {
                                if (selectedPeople.size() >= 500) {
                                  check.setChecked(false);
                                  toast("Sıralı İşlem En Fazla 500 Kişi Olabilir.");
                                  return;
                                }
                                selectedPeople.add(p.sicil);
                              } else selectedPeople.remove(p.sicil);
                            });
                        top.addView(check);
                        top.addView(bold(p.name(), 16), new LinearLayout.LayoutParams(0, -2, 1));
                        c.addView(top);
                        c.addView(text(p.sicil + " · " + p.telefon, 13, muted));
                        action(c, "Aç / Mesaj Hazırla", () -> select(p.sicil));
                      }
                      if (found.size() == 50)
                        list.addView(
                            text("İlk 50 Kayıt · Diğerleri İçin Aramayı Daraltın", 12, muted));
                    });
              });
        };
    filter.addTextChangedListener(
        watcher(
            () -> {
              main.removeCallbacks(populate);
              main.postDelayed(populate, 180);
            }));
    populate.run();
    action(body, "Seçilenlerle Sıralı İşleme Başla", this::startQueue);
  }

  void personDialog(Person existing) {
    LinearLayout box = column();
    Person original = existing == null ? new Person() : existing;
    EditText id = input(box, "Sicil Numarası", original.sicil, false);
    id.setEnabled(existing == null);
    EditText ad = input(box, "Ad / Ad Soyad", original.ad, false),
        soyad = input(box, "Soyad", original.soyad, false),
        tel = input(box, "Telefon", original.telefon, false);
    tel.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
    EditText birim = input(box, "Birim", original.birim, false),
        unvan = input(box, "Görev / Unvan", original.unvan, false),
        email = input(box, "E-posta", original.email, false),
        note = input(box, "Açıklama", original.not, true),
        durum = input(box, "Durum", original.durum, false);
    StringBuilder fields = new StringBuilder();
    for (Map.Entry<String, String> e : original.extra.entrySet())
      fields.append(e.getKey()).append('=').append(e.getValue()).append('\n');
    EditText extra = input(box, "Özel Alanlar · Her Satır Anahtar=Değer", fields.toString(), true);
    AlertDialog d = dialog(existing == null ? "Kişi Ekle" : "Kişiyi Düzenle", box);
    action(
        box,
        "Kaydet",
        () -> {
          try {
            Person p = new Person();
            p.sicil = id.getText().toString().trim();
            p.ad = ad.getText().toString().trim();
            p.soyad = soyad.getText().toString().trim();
            p.telefon = tel.getText().toString().trim();
            p.birim = birim.getText().toString();
            p.unvan = unvan.getText().toString();
            p.email = email.getText().toString();
            p.not = note.getText().toString();
            p.durum = durum.getText().toString();
            for (String line : extra.getText().toString().split("\n")) {
              if (line.trim().isEmpty()) continue;
              int eq = line.indexOf('=');
              if (eq < 1)
                throw new IllegalArgumentException("Özel Alanı Anahtar=Değer Biçiminde Girin.");
              String key = line.substring(0, eq).trim();
              if (p.extra.containsKey(key))
                throw new IllegalArgumentException("Özel Alan Adları Tekrarlanamaz.");
              p.extra.put(key, line.substring(eq + 1));
            }
            validate(p);
            if (existing == null && state.people.containsKey(p.sicil))
              throw new IllegalArgumentException("Bu Sicil Zaten Var. Mevcut Kaydı Düzenleyin.");
            if (existing == null && state.people.size() >= MAX_PEOPLE)
              throw new IllegalArgumentException("Kişi Sınırı Aşıldı.");
            d.dismiss();
            mutate(
                s -> s.people.put(p.sicil, p),
                true,
                () -> {
                  selected = p.sicil;
                  render();
                });
          } catch (Exception bad) {
            toast(bad.getMessage());
          }
        });
    if (existing != null)
      action(
          box,
          "Kişiyi Sil",
          () ->
              confirm(
                  "Kişiyi Sil",
                  existing.name()
                      + " rehberden kaldırılacak. İşlem geçmişindeki sicil kayıtları kalır.",
                  () -> {
                    d.dismiss();
                    mutate(
                        s -> {
                          s.people.remove(existing.sicil);
                          s.recent.remove(existing.sicil);
                        },
                        true,
                        () -> {
                          selectedPeople.remove(existing.sicil);
                          if (selected.equals(existing.sicil)) selected = "";
                          render();
                        });
                  }));
  }

  void templates() {
    body.addView(bold("Mesaj Şablonları", 24));
    body.addView(
        text("Şablonlar düz metindir. Sicil, isim ve diğer alanlar otomatik eklenmez.", 13, muted));
    action(body, "+ Şablon Oluştur", () -> templateDialog(null, false));
    Set<String> categories = new TreeSet<>();
    for (Template t : state.templates) categories.add(t.category);
    for (String category : categories) {
      body.addView(bold(category, 17));
      for (Template t : state.templates)
        if (t.category.equals(category)) {
          LinearLayout c = card(body);
          c.addView(bold(t.name, 18));
          TextView content = text(t.text, 14, ink);
          content.setMaxLines(4);
          c.addView(content);
          c.addView(
              text(
                  t.lastUsed == 0 ? "Henüz Kullanılmadı" : "Son Kullanım: " + format(t.lastUsed),
                  11,
                  muted));
          action(c, "Düzenle", () -> templateDialog(t, false));
          action(c, "Çoğalt", () -> templateDialog(t, true));
          action(
              c,
              "Sil",
              () ->
                  confirm(
                      "Şablonu Sil",
                      t.name + " kaldırılacak.",
                      () ->
                          mutate(
                              s -> s.templates.removeIf(x -> x.id.equals(t.id)),
                              true,
                              () -> {
                                if (selectedTemplate.equals(t.id)) selectedTemplate = "";
                                render();
                              })));
        }
    }
  }

  void templateDialog(Template source, boolean duplicate) {
    LinearLayout box = column();
    EditText name =
        input(
            box,
            "Şablon Adı",
            source == null ? "" : source.name + (duplicate ? " Kopyası" : ""),
            false);
    EditText category =
        input(box, "Kategori", source == null ? "Özel Mesajlar" : source.category, false);
    EditText content =
        input(box, "Mesaj Metni · Aynen Kullanılır", source == null ? "" : source.text, true);
    content.setMinLines(6);
    AlertDialog d = dialog("Mesaj Şablonu", box);
    action(
        box,
        "Kaydet",
        () -> {
          try {
            String value = literalMessage(content.getText().toString());
            if (name.getText().toString().trim().isEmpty())
              throw new IllegalArgumentException("Şablon Adı Girin.");
            if (name.length() > 100 || category.length() > 100)
              throw new IllegalArgumentException("Ad Ve Kategori En Fazla 100 Karakter Olmalı.");
            if (state.templates.size() >= 1000 && (source == null || duplicate))
              throw new IllegalArgumentException("En Fazla 1000 Şablon.");
            Template t = source == null || duplicate ? new Template() : source.copy();
            t.name = name.getText().toString().trim();
            t.category = category.getText().toString().trim();
            if (t.category.isEmpty()) t.category = "Özel Mesajlar";
            t.text = value;
            d.dismiss();
            mutate(
                s -> {
                  s.templates.removeIf(x -> x.id.equals(t.id));
                  s.templates.add(t);
                },
                true,
                this::render);
          } catch (Exception e) {
            toast(e.getMessage());
          }
        });
  }

  void history() {
    body.addView(bold("İşlem Geçmişi", 24));
    body.addView(
        text(
            "Son 1000 işlem. Mesaj metni ve telefon numarası burada saklanmaz. WhatsApp gönderimi"
                + " uygulama tarafından doğrulanamaz.",
            13,
            muted));
    action(
        body,
        "Geçmişi Temizle",
        () ->
            confirm(
                "Geçmişi Temizle",
                "İşlem kayıtları silinecek; rehber ve şablonlar korunacak.",
                () ->
                    mutate(
                        s -> s.history.clear(),
                        false,
                        () -> {
                          historyPage = 0;
                          render();
                        })));
    for (int i = historyPage * 100;
        i < Math.min((historyPage + 1) * 100, state.history.size());
        i++) {
      Event e = state.history.get(i);
      LinearLayout c = card(body);
      c.addView(bold(e.sicil + " · " + e.template, 15));
      c.addView(text(e.status.label, 14, blue));
      c.addView(text(format(e.time), 12, muted));
      if (e.status != Status.CANCELLED)
        action(c, "Durumu Kendim İşaretle", () -> eventOptions(e.id));
    }
    if (historyPage > 0)
      action(
          body,
          "Önceki 100 İşlem",
          () -> {
            historyPage--;
            render();
          });
    if ((historyPage + 1) * 100 < state.history.size())
      action(
          body,
          "Sonraki 100 İşlem",
          () -> {
            historyPage++;
            render();
          });
  }

  String format(long time) {
    return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(date);
  }

  void settings() {
    body.addView(bold("Ayarlar Ve Yedekleme", 24));
    LinearLayout info = card(body);
    info.addView(bold("Sicil360 Pro · 1.0", 19));
    info.addView(
        text(
            state.people.size()
                + " Kişi · "
                + state.templates.size()
                + " Şablon\nVeriler bu cihazda şifreli saklanır. Sunucu bağlantısı yoktur.",
            14,
            muted));
    action(
        info,
        "Renk Teması",
        () ->
            show(
                new AlertDialog.Builder(this)
                    .setTitle("Görünüm")
                    .setSingleChoiceItems(
                        new String[] {"Sistem", "Açık", "Koyu"},
                        prefs.getInt("theme", 0),
                        (d, index) -> {
                          capture();
                          prefs.edit().putInt("theme", index).apply();
                          d.dismiss();
                          recreate();
                        })
                    .setNegativeButton("Vazgeç", null)));
    action(
        info,
        prefs.getBoolean("lock", false)
            ? "Uygulama Kilidini Kapat"
            : "Cihaz Kilidiyle Uygulamayı Koru",
        () -> {
          android.app.KeyguardManager manager =
              (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
          if (!manager.isDeviceSecure()) {
            toast("Önce Telefon Ayarlarından PIN, Şifre Veya Desen Oluşturun.");
            return;
          }
          boolean enabled = !prefs.getBoolean("lock", false);
          prefs.edit().putBoolean("lock", enabled).apply();
          if (enabled) {
            locked = true;
            render();
            authenticate();
          } else render();
        });
    LinearLayout back = card(body);
    back.addView(bold("Güvenli Yedek", 18));
    back.addView(
        text(
            "Taşınabilir yedek bir şifreyle korunur. Şifre unutulursa yedek açılamaz. Excel dışa"
                + " aktarımı şifreli değildir.",
            13,
            muted));
    action(back, "Şifreli Yedek Oluştur", () -> passwordDialog(false, null));
    action(back, "Şifreli Yedekten Geri Yükle", () -> chooseOpen(RESTORE));
    action(
        back,
        "Son Veri Değişikliğini Geri Al",
        () ->
            confirm(
                "Geri Al",
                "Son kişi/şablon/içe aktarma öncesindeki rehber ve şablonlar geri gelir. İşlem"
                    + " geçmişi korunur.",
                () -> {
                  long rev = state.revision;
                  task(
                      () -> vault.restoreUndo(rev),
                      s -> {
                        state = s;
                        selected = "";
                        selectedPeople.clear();
                        queue.clear();
                        render();
                      });
                }));
    action(
        body,
        "Örnek Excel Şablonu Oluştur",
        () ->
            chooseCreate(
                SAMPLE,
                "Sicil360-Ornek.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    action(
        body,
        "Tüm Kayıtları Sil",
        () ->
            confirm(
                "Tüm Kayıtları Sil",
                "Rehber, şablonlar ve geçmiş kaldırılacak. Kaydettiğiniz dış yedekler silinmez."
                    + " Uygulama içindeki geri alma kopyası da silinir.",
                () -> {
                  long revision = state.revision;
                  task(
                      () -> vault.erase(revision),
                      cleared -> {
                        state = cleared;
                        selected = "";
                        message = "";
                        queue.clear();
                        selectedPeople.clear();
                        render();
                      });
                }));
  }

  void authenticate() {
    if (!locked || authenticating || state == null) return;
    android.app.KeyguardManager manager =
        (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
    if (!manager.isDeviceSecure()) {
      toast("Cihaz Kilidi Kaldırılmış. Telefon Ayarlarında Yeniden Etkinleştirin.");
      return;
    }
    Intent intent =
        manager.createConfirmDeviceCredentialIntent(
            "Sicil360 Pro", "Kişi bilgilerini açmak için cihaz kilidini doğrulayın.");
    if (intent == null) return;
    authenticating = true;
    try {
      startActivityForResult(intent, UNLOCK);
    } catch (Exception e) {
      authenticating = false;
      toast("Cihaz Doğrulaması Başlatılamadı.");
    }
  }

  void prepareMessage() {
    if (locked || busy || failed) return;
    capture();
    if (!query.trim().equals(selected)) {
      toast("Arama Değişti. Mesaj İçin Önce Sonuçtan Kişiyi Seçin.");
      return;
    }
    Person p = state.people.get(selected);
    if (p == null) {
      toast("Önce Kişiyi Seçin.");
      return;
    }
    try {
      String confirmedText = literalMessage(message);
      String link = Model.whatsapp(p, confirmedText);
      String templateName = "Özel Mesaj";
      for (Template t : state.templates) if (t.id.equals(selectedTemplate)) templateName = t.name;
      Event event = new Event();
      event.sicil = p.sicil;
      event.template = templateName;
      mutate(
          s -> addHistory(s, event),
          false,
          () -> {
            show(
                new AlertDialog.Builder(this)
                    .setTitle("Kişiyi Ve Mesajı Onayla")
                    .setMessage(
                        p.name()
                            + "\nSicil: "
                            + p.sicil
                            + "\n+"
                            + phone(p.telefon)
                            + "\n\n"
                            + confirmedText
                            + "\n\nWhatsApp'ta Gönder düğmesine siz basacaksınız.")
                    .setNegativeButton(
                        "Vazgeç", (d, w) -> updateEvent(event.id, Status.CANCELLED, null))
                    .setPositiveButton("Onayla Ve Aç", (d, w) -> chooseWhatsApp(link, event.id))
                    .setOnCancelListener(d -> updateEvent(event.id, Status.CANCELLED, null)));
          });
    } catch (Exception e) {
      toast(e.getMessage());
    }
  }

  List<String> installedWhatsApp() {
    List<String> packages = new ArrayList<>();
    for (String name : new String[] {"com.whatsapp", "com.whatsapp.w4b"})
      try {
        getPackageManager().getApplicationInfo(name, 0);
        packages.add(name);
      } catch (Exception ignored) {
      }
    return packages;
  }

  void chooseWhatsApp(String link, String event) {
    List<String> packages = gateway.packages();
    if (packages.isEmpty()) {
      show(
          new AlertDialog.Builder(this)
              .setTitle("WhatsApp Bulunamadı")
              .setMessage(
                  "WhatsApp veya WhatsApp Business yüklü değil. İsterseniz desteklenen wa.me"
                      + " bağlantısını tarayıcıda açabilirsiniz.")
              .setNegativeButton("İptal", (d, w) -> updateEvent(event, Status.CANCELLED, null))
              .setPositiveButton("Tarayıcıda Aç", (d, w) -> launch(link, null, event))
              .setOnCancelListener(d -> updateEvent(event, Status.CANCELLED, null)));
    } else if (packages.size() == 1) launch(link, packages.get(0), event);
    else
      show(
          new AlertDialog.Builder(this)
              .setTitle("Uygulama Seçin")
              .setItems(
                  new String[] {"WhatsApp", "WhatsApp Business"},
                  (d, i) -> launch(link, packages.get(i), event))
              .setNegativeButton("İptal", (d, w) -> updateEvent(event, Status.CANCELLED, null)));
  }

  void launch(String link, String pkg, String event) {
    updateEvent(
        event,
        Status.ATTEMPT,
        () -> {
          try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            if (pkg != null) intent.setPackage(pkg);
            pendingEvent = event;
            externalPending = true;
            gateway.open(intent);
          } catch (Exception e) {
            externalPending = false;
            pendingEvent = "";
            updateEvent(
                event,
                Status.ERROR,
                () -> toast("Sohbet Açılamadı. Uygulamayı Veya Tarayıcıyı Kontrol Edin."));
          }
        });
  }

  void updateEvent(String id, Status status, Runnable done) {
    mutate(
        s -> {
          for (Event e : s.history)
            if (e.id.equals(id)) {
              e.status = status;
              e.time = System.currentTimeMillis();
            }
          if (status == Status.ATTEMPT)
            for (Template t : s.templates)
              if (t.id.equals(selectedTemplate)) t.lastUsed = System.currentTimeMillis();
        },
        false,
        done);
  }

  void checkReturn() {
    if (pendingEvent.isEmpty() || locked || busy || state == null) return;
    String id = pendingEvent;
    pendingEvent = "";
    externalPending = false;
    main.post(
        () -> {
          if (!destroyed && !locked) eventOptions(id);
        });
  }

  void eventOptions(String id) {
    show(
        new AlertDialog.Builder(this)
            .setTitle("Sonucu Siz Doğrulayın")
            .setItems(
                new String[] {
                  "Sohbet Açıldı · Ben Kontrol Ettim",
                  "Gönderimi Tamamladım · Ben İşaretliyorum",
                  "Gönderim Doğrulanamadı",
                  "İşlemi İptal Ettim"
                },
                (d, i) ->
                    updateEvent(
                        id,
                        new Status[] {
                              Status.CHAT_CONFIRMED,
                              Status.SENT_MARKED,
                              Status.UNVERIFIED,
                              Status.CANCELLED
                            }
                            [i],
                        this::render))
            .setNegativeButton("Daha Sonra", null));
  }

  void startQueue() {
    if (selectedPeople.isEmpty()) {
      toast("Önce Rehberdeki Kutulardan Kişileri Seçin.");
      return;
    }
    queue.clear();
    for (String id : selectedPeople) if (state.people.containsKey(id)) queue.add(id);
    if (queue.isEmpty()) return;
    queueIndex = 0;
    selected = queue.get(0);
    screen = 0;
    query = selected;
    render();
    toast("Şablonu Seçin. Her Kişi İçin Önizlemeyi Onaylayın.");
  }

  void nextQueue() {
    capture();
    confirm(
        "Sonraki Kişi",
        "Mevcut kişinin gönderildiği otomatik kabul edilmez. Sonraki kişiye geçilsin mi?",
        () -> {
          if (++queueIndex >= queue.size()) {
            queue.clear();
            selectedPeople.clear();
            toast("Sıralı Hazırlama Bitti. Gönderim Durumlarını Geçmişten Kontrol Edin.");
          } else {
            selected = queue.get(queueIndex);
            query = selected;
            for (Template t : state.templates) if (t.id.equals(selectedTemplate)) message = t.text;
          }
          render();
        });
  }

  void chooseOpen(int kind) {
    Intent i =
        new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(
                kind == IMPORT
                    ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    : "*/*");
    if (kind == IMPORT)
      i.putExtra(
          Intent.EXTRA_MIME_TYPES,
          new String[] {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/octet-stream"
          });
    try {
      startActivityForResult(i, kind);
    } catch (Exception e) {
      toast("Dosya Seçici Bulunamadı.");
    }
  }

  void chooseCreate(int kind, String name, String mime) {
    try {
      startActivityForResult(
          new Intent(Intent.ACTION_CREATE_DOCUMENT)
              .addCategory(Intent.CATEGORY_OPENABLE)
              .setType(mime)
              .putExtra(Intent.EXTRA_TITLE, name),
          kind);
    } catch (Exception e) {
      toast("Dosya Kaydetme Ekranı Açılamadı.");
      if (backupPassword != null) {
        Arrays.fill(backupPassword, '\0');
        backupPassword = null;
      }
    }
  }

  @Override
  public void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request == UNLOCK) {
      authenticating = false;
      if (result == RESULT_OK) {
        locked = false;
        render();
        if (deferredCompletion != null) {
          Runnable completion = deferredCompletion;
          deferredCompletion = null;
          completion.run();
        }
        if (deferredResult != null) {
          Runnable next = deferredResult;
          deferredResult = null;
          next.run();
        }
        checkReturn();
      } else {
        locked = true;
        render();
      }
      return;
    }
    if (result != RESULT_OK || data == null || data.getData() == null) {
      if (backupPassword != null) {
        Arrays.fill(backupPassword, '\0');
        backupPassword = null;
      }
      return;
    }
    if (locked) {
      deferredResult = () -> onActivityResult(request, result, data);
      render();
      authenticate();
      return;
    }
    Uri uri = data.getData();
    if (request == IMPORT) loadImport(uri);
    else if (request == RESTORE) passwordDialog(true, uri);
    else if (request == BACKUP) {
      char[] password = backupPassword;
      backupPassword = null;
      if (password == null) {
        toast("Şifre Süresi Doldu. Yeniden Yedek Oluşturun.");
        return;
      }
      State snapshot = state;
      task(
          () -> {
            try {
              byte[] encrypted = Backup.seal(Codec.encode(snapshot), password);
              try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                if (out == null) throw new IOException("Dosya Açılamadı.");
                out.write(encrypted);
              }
              return true;
            } finally {
              Arrays.fill(password, '\0');
            }
          },
          ok -> toast("Şifreli Yedek Kaydedildi."));
    } else if (request == EXPORT || request == SAMPLE) {
      State exporting = state;
      List<Person> people = new ArrayList<>();
      if (request == SAMPLE) {
        Person p = new Person();
        p.sicil = "000123";
        p.ad = "Örnek";
        p.soyad = "Kişi";
        p.telefon = "";
        p.birim = "Örnek Birim";
        people.add(p);
      }
      task(
          () -> {
            OutputStream out = getContentResolver().openOutputStream(uri, "wt");
            if (out == null) throw new IOException("Dosya Açılamadı.");
            Excel.export(out, request == EXPORT ? exporting.people.values() : people);
            return true;
          },
          ok -> toast("Excel Dosyası Kaydedildi."));
    }
  }

  void passwordDialog(boolean restore, Uri uri) {
    LinearLayout box = column();
    box.addView(
        text(
            restore
                ? "Yedeğin oluşturulurken kullanılan şifresini girin."
                : "En az 10 karakter. Şifrenizi güvenli saklayın; unutulursa yedek açılamaz.",
            14,
            ink));
    EditText password = input(box, "Yedek Şifresi", "", false);
    password.setInputType(129);
    EditText repeat = restore ? null : input(box, "Şifre Tekrar", "", false);
    if (repeat != null) repeat.setInputType(129);
    AlertDialog d = dialog(restore ? "Yedekten Geri Yükle" : "Şifreli Yedek Oluştur", box);
    action(
        box,
        restore ? "Yedeği Kontrol Et" : "Devam Et",
        () -> {
          char[] secret = password.getText().toString().toCharArray();
          if (secret.length < 10
              || (!restore && !password.getText().toString().equals(repeat.getText().toString()))) {
            Arrays.fill(secret, '\0');
            toast("Şifreleri Kontrol Edin. En Az 10 Karakter Gerekiyor.");
            return;
          }
          password.setText("");
          if (repeat != null) repeat.setText("");
          d.dismiss();
          if (!restore) {
            backupPassword = secret;
            chooseCreate(BACKUP, "Sicil360-Yedek.s360", "application/octet-stream");
          } else
            task(
                () -> {
                  try (InputStream in = getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new IOException("Dosya Açılamadı.");
                    return Codec.decode(Backup.open(Excel.read(in, Codec.MAX_BYTES + 64), secret));
                  } finally {
                    Arrays.fill(secret, '\0');
                  }
                },
                restored ->
                    confirm(
                        "Yedek Doğrulandı",
                        restored.people.size()
                            + " Kişi · "
                            + restored.templates.size()
                            + " Şablon\n"
                            + "Mevcut rehber, şablon ve geçmiş yedekle değiştirilecek. İşlem öncesi"
                            + " şifreli geri alma kopyası oluşturulur.",
                        () -> {
                          save(
                              restored,
                              true,
                              () -> {
                                selected = "";
                                message = "";
                                selectedPeople.clear();
                                queue.clear();
                                render();
                                toast("Yedek Geri Yüklendi.");
                              });
                        }));
        });
  }

  void loadImport(Uri uri) {
    task(
        () -> {
          File tmp = File.createTempFile("sicil-import-", ".xlsx", getCacheDir());
          try (InputStream in = getContentResolver().openInputStream(uri);
              OutputStream out = new FileOutputStream(tmp)) {
            if (in == null) throw new IOException("Dosya Açılamadı.");
            byte[] b = new byte[8192];
            int n, total = 0;
            while ((n = in.read(b)) != -1) {
              if ((total += n) > Excel.MAX_FILE)
                throw new IOException("XLSX En Fazla 32 MB Olabilir.");
              out.write(b, 0, n);
            }
          } catch (Exception e) {
            tmp.delete();
            throw e;
          }
          return tmp;
        },
        tmp -> {
          if (importing != null) importing.delete();
          importing = tmp;
          task(
              () -> Excel.sheets(tmp),
              sheets -> {
                show(
                    new AlertDialog.Builder(this)
                        .setTitle("Excel Sayfasını Seçin")
                        .setItems(
                            sheets.stream().map(s -> s.name).toArray(String[]::new),
                            (d, i) ->
                                task(
                                    () -> Excel.load(tmp, sheets.get(i)),
                                    table -> {
                                      importTable = table;
                                      importHeader =
                                          table.rows.isEmpty() ? 1 : table.rows.firstKey();
                                      mappingDialog();
                                    }))
                        .setNegativeButton("İptal", null));
              });
        });
  }

  void mappingDialog() {
    if (mappingWindow != null) mappingWindow.dismiss();
    LinearLayout box = column();
    EditText header = input(box, "Başlık Satırı Numarası", Integer.toString(importHeader), false);
    header.setInputType(2);
    action(
        box,
        "Başlık Satırını Uygula",
        () -> {
          try {
            int number = Integer.parseInt(header.getText().toString());
            if (!importTable.rows.containsKey(number))
              throw new IllegalArgumentException("Bu Satır Excel'de Bulunamadı.");
            importHeader = number;
            mappingDialog();
          } catch (Exception e) {
            toast("Geçerli Başlık Satırı Girin.");
          }
        });
    importHeaders = importTable.headers(importHeader);
    String[] options = new String[importHeaders.size() + 1];
    options[0] = "Yok / Aktarma";
    for (int i = 0; i < importHeaders.size(); i++)
      options[i + 1] =
          (i + 1) + " · " + (importHeaders.get(i).isEmpty() ? "Başlıksız" : importHeaders.get(i));
    String[] labels = {
      "Sicil Numarası *",
      "Ad / Ad Soyad *",
      "Soyad",
      "Telefon *",
      "Birim",
      "Unvan",
      "E-posta",
      "Açıklama",
      "Durum"
    };
    Spinner[] selectors = new Spinner[9];
    for (int i = 0; i < 9; i++) {
      selectors[i] = spinner(box, labels[i], options);
      selectors[i].setSelection(Excel.suggest(importHeaders, i) + 1);
    }
    Spinner mode =
        spinner(
            box,
            "Mevcut Kayıtlar",
            new String[] {
              "Koru · Eşleşen Sicili Atla",
              "Eşleşen Sicili Güncelle",
              "Listeyi Kontrollü Olarak Değiştir"
            });
    CheckBox extra = new CheckBox(this);
    extra.setText("Diğer Sütunları Özel Alan Olarak Sakla");
    extra.setTextColor(ink);
    box.addView(extra);
    AlertDialog d = dialog("Sütun Eşleştirme", box);
    mappingWindow = d;
    action(
        box,
        "İçe Aktarma Önizlemesi",
        () -> {
          for (int i = 0; i < 9; i++) mapping[i] = selectors[i].getSelectedItemPosition() - 1;
          if (mapping[0] < 0 || mapping[1] < 0 || mapping[3] < 0) {
            toast("Sicil, Ad Ve Telefon Sütunlarını Seçin.");
            return;
          }
          Set<Integer> used = new HashSet<>();
          for (int m : mapping)
            if (m >= 0 && !used.add(m)) {
              toast("Aynı Sütun Birden Fazla Alana Eşleştirilemez.");
              return;
            }
          extraColumns = extra.isChecked();
          importMode = Model.ImportMode.values()[mode.getSelectedItemPosition()];
          resolutions.clear();
          d.dismiss();
          task(
              () -> importTable.mapped(importHeader, mapping, extraColumns),
              rows -> {
                importRows = rows;
                refreshPlan();
              });
        });
  }

  void refreshPlan() {
    State base = state;
    task(
        () -> Model.plan(base, importRows, importMode, new HashMap<>(resolutions)),
        plan -> {
          importPlan = plan;
          previewImport();
        });
  }

  void previewImport() {
    LinearLayout box = column();
    box.addView(
        bold(importPlan.added + " Eklenecek · " + importPlan.updated + " Güncellenecek", 18));
    box.addView(
        text(
            importPlan.skipped
                + " Atlanacak · "
                + importPlan.errors
                + " Hatalı\n"
                + importPlan.removed
                + " Eski Kayıt Kaldırılacak · "
                + importPlan.conflicts.size()
                + " Çakışan Sicil",
            14,
            ink));
    int count = 0;
    for (Row row : importPlan.rows)
      if (!row.error.isEmpty() && count++ < 30)
        box.addView(text("Satır " + row.number + ": " + row.error, 12, muted));
    if (count > 30)
      box.addView(
          text("İlk 30 hata gösteriliyor; tüm hatalı satırlar işlem dışında kalır.", 12, muted));
    AlertDialog d = dialog("Aktarım Önizlemesi", box);
    for (Map.Entry<String, List<Row>> e : importPlan.conflicts.entrySet())
      action(
          box,
          "Çakışmayı Çöz · Sicil " + e.getKey(),
          () -> {
            List<Row> rows = e.getValue();
            String[] options = new String[rows.size() + 1];
            options[0] = "Bu Sicilin Tüm Satırlarını Atla";
            for (int i = 0; i < rows.size(); i++)
              options[i + 1] =
                  "Satır "
                      + rows.get(i).number
                      + " · "
                      + rows.get(i).person.name()
                      + " · "
                      + rows.get(i).person.telefon;
            show(
                new AlertDialog.Builder(this)
                    .setTitle("Aynı Sicil · Siz Seçin")
                    .setItems(
                        options,
                        (dialog, index) -> {
                          resolutions.put(e.getKey(), index == 0 ? -1 : rows.get(index - 1).number);
                          d.dismiss();
                          refreshPlan();
                        })
                    .setNegativeButton("Vazgeç", null));
          });
    action(
        box,
        "Onayla Ve İçe Aktar",
        () -> {
          if (!importPlan.conflicts.isEmpty()) {
            toast("Önce Tüm Çakışmaları Çözün.");
            return;
          }
          confirm(
              "Son Aktarım Onayı",
              importPlan.added
                  + " Ekleme, "
                  + importPlan.updated
                  + " Güncelleme, "
                  + importPlan.removed
                  + " Silme Yapılacak.\n"
                  + importPlan.errors
                  + " Hatalı Satır Aktarılmayacak. İşlem Öncesi Şifreli Geri Alma Kopyası Alınır.",
              () -> {
                d.dismiss();
                save(
                    importPlan.result,
                    true,
                    () -> {
                      selected = "";
                      selectedPeople.clear();
                      queue.clear();
                      screen = 1;
                      render();
                      toast("Excel Aktarımı Tamamlandı.");
                      if (importing != null) importing.delete();
                      importTable = null;
                      importRows = null;
                      importPlan = null;
                    });
              });
        });
  }
}
