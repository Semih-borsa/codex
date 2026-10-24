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
  final ScrollMemory scrollMemory = new ScrollMemory();
  String directoryQuery = "";
  TextView status;
  EditText search, preview;
  RadioGroup templateChoices;
  static final int DEBT_IMPORT = 32, DEBT_SAMPLE = 33;
  String debtLedger = "", debtQuery = "";
  int debtPage;
  boolean debtImport;
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
  int historyPage, directoryToken, crmMode, followFilter, followPage;
  String historyPerson = "", followQuery = "";
  String selected = "",
      selectedTemplate = "",
      message = "",
      query = "",
      pendingEvent = "",
      notice = "";
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
    scrollMemory.load(saved);
    if (saved != null) {
      directoryQuery = saved.getString("directoryQuery", "");
      debtPage = saved.getInt("debtPage");
      historyPage = saved.getInt("historyPage");
      followPage = saved.getInt("followPage");
      screen = saved.getInt("screen");
      debtLedger = saved.getString("debtLedger", "");
      debtQuery = saved.getString("debtQuery", "");
      crmMode = saved.getInt("crmMode");
      followFilter = saved.getInt("followFilter");
      historyPerson = saved.getString("historyPerson", "");
      followQuery = saved.getString("followQuery", "");
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
    scrollMemory.save(out);
    out.putString("directoryQuery", directoryQuery);
    out.putInt("debtPage", debtPage);
    out.putInt("historyPage", historyPage);
    out.putInt("followPage", followPage);
    out.putInt("screen", screen);
    out.putString("debtLedger", debtLedger);
    out.putString("debtQuery", debtQuery);
    out.putInt("crmMode", crmMode);
    out.putInt("followFilter", followFilter);
    out.putString("historyPerson", historyPerson);
    out.putString("followQuery", followQuery);
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

  String scrollContext() {
    if (state == null) return "loading";
    if (locked) return "locked";
    if (failed) return "failed";
    if (screen == 0) return "home:" + selected;
    if (screen == 3) {
      if (crmMode == 3) return "payments:" + debtLedger + ":" + debtPage + ":" + debtQuery;
      if (crmMode == 1) return "follow:" + followFilter + ":" + followPage + ":" + followQuery;
      if (crmMode == 0) return "history:" + historyPerson + ":" + historyPage;
      return "reports";
    }
    return "tab:" + screen;
  }

  void render() {
    scrollMemory.remember();
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
    scroll =
        new ScrollView(this) {
          @Override
          public boolean dispatchTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) scrollMemory.userInput();
            return super.dispatchTouchEvent(event);
          }

          @Override
          public boolean performAccessibilityAction(int action, Bundle arguments) {
            if (action == 4096 || action == 8192 || action == 16908344 || action == 16908346)
              scrollMemory.userInput();
            return super.performAccessibilityAction(action, arguments);
          }

          @Override
          public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) scrollMemory.userInput();
            return super.dispatchKeyEvent(event);
          }
        };
    scroll.setFocusableInTouchMode(true);
    scroll.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
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
    String[] titles = {"Ara", "Rehber", "Şablonlar", "Takip", "Ayarlar"};
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
    scroll.requestFocus();
    scrollMemory.attach(scroll, scrollContext());
  }

  void home() {
    if (!state.debts.isEmpty()) action(body, "Borç Ve Ödemeler", this::openPayments);
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

    detail.addView(text("Takip: " + p.followUp.label, 13, blue));
    action(detail, "Takip Ve Notlar", () -> crmDialog(p.sicil));
    LinearLayout composer = card(body);
    composer.addView(text("Mesaj Şablonu", 12, muted));
    templateChoices = new RadioGroup(this);
    templateChoices.setOrientation(RadioGroup.VERTICAL);
    templateChoices.setTag("template-choices");
    addTemplateChoice("", "Özel Mesaj · Şablon Kullanma");
    for (Template t : state.templates) addTemplateChoice(t.id, t.name + " · " + t.category);
    composer.addView(templateChoices, new LinearLayout.LayoutParams(-1, -2));
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
    templateChoices.setOnCheckedChangeListener(
        (group, checkedId) -> {
          RadioButton chosen = group.findViewById(checkedId);
          if (chosen == null) return;
          String choice = (String) chosen.getTag();
          if (choice.equals(selectedTemplate)) return;
          selectedTemplate = choice;
          for (Template t : state.templates) {
            if (t.id.equals(choice)) {
              preview.setText(t.text);
              break;
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

  void addTemplateChoice(String id, String title) {
    RadioButton option = new RadioButton(this);
    option.setId(View.generateViewId());
    option.setTag(id);
    option.setText(title);
    option.setTextSize(14);
    option.setTextColor(ink);
    option.setMinHeight(dp(48));
    option.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    option.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_LTR);
    option.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
    option.setPadding(dp(10), dp(5), dp(6), dp(5));
    option.setButtonTintList(android.content.res.ColorStateList.valueOf(blue));
    option.setBackground(Visuals.touch(dark ? 0xFF1A2428 : 0xFFF0F8F5, dp(10), border));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.bottomMargin = dp(5);
    templateChoices.addView(option, params);
    option.setChecked(id.equals(selectedTemplate));
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
                      if (q.trim().isEmpty() || q.trim().equals(selected)) return;
                      Person exact = state.people.get(q.trim());
                      if (exact != null && !busy && queue.isEmpty()) {
                        select(exact.sicil);
                        return;
                      }
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
    action(tools, "Borç Ve Ödemeler", this::openPayments);
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
    EditText filter = input(body, "Rehberde Ara", directoryQuery, false);
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
                        c.setTag("directory-person-" + p.sicil);
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
              directoryQuery = filter.getText().toString();
              scrollMemory.userInput();
              scroll.scrollTo(0, 0);
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
          c.setTag("template-" + t.id);
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
                  int index = -1;
                  for (int i = 0; i < s.templates.size(); i++)
                    if (s.templates.get(i).id.equals(t.id)) {
                      index = i;
                      break;
                    }
                  if (index < 0) s.templates.add(t);
                  else s.templates.set(index, t);
                },
                true,
                this::render);
          } catch (Exception e) {
            toast(e.getMessage());
          }
        });
  }

  void history() {
    body.addView(bold("İletişim Ve Takip", 24));
    LinearLayout sections = row();
    String[] names = {"Geçmiş", "Takip", "Raporlar", "Ödemeler"};
    for (int i = 0; i < names.length; i++) {
      final int mode = i;
      Button button =
          button(
              names[i],
              () -> {
                crmMode = mode;
                historyPage = 0;
                followPage = 0;
                render();
              });
      button.setTextSize(12);
      button.setMinWidth(0);
      button.setMinimumWidth(0);
      sections.addView(button, new LinearLayout.LayoutParams(0, dp(48), 1));
    }
    body.addView(sections);
    gap(body, 8);
    if (crmMode == 3) {
      paymentsPage();
      return;
    }
    if (crmMode == 1) {
      followList();
      return;
    }
    if (crmMode == 2) {
      crmReport();
      return;
    }
    body.addView(
        text(
            "Son 1000 kayıt · Mesaj içeriği değil, şablon adı saklanır. Gönderim yalnız kullanıcı"
                + " beyanıdır.",
            12,
            muted));
    if (!historyPerson.isEmpty()) {
      Person person = state.people.get(historyPerson);
      body.addView(
          bold(person == null ? historyPerson : person.name() + " · " + historyPerson, 16));
      action(
          body,
          "Tüm Kişilerin Geçmişi",
          () -> {
            historyPerson = "";
            historyPage = 0;
            render();
          });
    }
    List<Event> visible = new ArrayList<>();
    for (Event event : state.history)
      if (historyPerson.isEmpty() || historyPerson.equals(event.sicil)) visible.add(event);
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
    for (int i = historyPage * 100; i < Math.min((historyPage + 1) * 100, visible.size()); i++) {
      Event e = visible.get(i);
      LinearLayout c = card(body);
      c.setTag("history-" + e.id);
      Person person = state.people.get(e.sicil);
      c.addView(
          bold(
              (person == null ? e.sicil : person.name() + " · " + e.sicil) + " · " + e.template,
              15));
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
    if ((historyPage + 1) * 100 < visible.size())
      action(
          body,
          "Sonraki 100 İşlem",
          () -> {
            historyPage++;
            render();
          });
  }

  void openPayments() {
    capture();
    screen = 3;
    crmMode = 3;
    debtPage = 0;
    render();
  }

  Payments.Ledger activeDebt() {
    if (state.debts.isEmpty()) return null;
    for (Payments.Ledger l : state.debts) if (l.id.equals(debtLedger)) return l;
    Payments.Ledger l = state.debts.get(state.debts.size() - 1);
    debtLedger = l.id;
    return l;
  }

  void amountLine(LinearLayout parent, String label, long amount) {
    TextView caption = text(label, 12, muted);
    parent.addView(caption);
    TextView value = bold(Payments.format(amount), 22);
    value.setAutoSizeTextTypeUniformWithConfiguration(
        12, 22, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
    value.setMaxLines(1);
    parent.addView(value, new LinearLayout.LayoutParams(-1, dp(36)));
  }

  void paymentsPage() {
    action(
        body,
        "Borç Excel Şablonu İndir",
        () ->
            chooseCreate(
                DEBT_SAMPLE,
                "Sicil360-Borc-Sablonu.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    action(body, "Borç Excel Listesi Yükle", () -> chooseOpen(DEBT_IMPORT));
    Payments.Ledger ledger = activeDebt();
    if (ledger == null) {
      body.addView(
          text(
              "İlk Satır Başlık: A Sıra No · B Cari Kodu · C Ticari Unvanı · D Adı · E Soyadı · F"
                  + " Bakiye. Telefon Gerekmez.",
              14,
              ink));
      return;
    }
    action(
        body,
        "Liste Seç · " + ledger.period + " · " + ledger.name,
        () ->
            show(
                new AlertDialog.Builder(this)
                    .setTitle("Borç Listesi Seçin")
                    .setItems(
                        state.debts.stream()
                            .map(l -> l.period + " · " + l.name)
                            .toArray(String[]::new),
                        (d, i) -> {
                          debtLedger = state.debts.get(i).id;
                          debtPage = 0;
                          debtQuery = "";
                          render();
                        })
                    .setNegativeButton("Vazgeç", null)));
    action(body, "Listeyi Düzenle / Sil", () -> debtListDialog(ledger.id));
    Payments.Totals totals = Payments.totals(ledger);
    LinearLayout summary = card(body);
    summary.setTag("payment-summary");
    amountLine(summary, "İlk Listenin Genel Toplamı", totals.original);
    amountLine(summary, "Ödenen Tutar", totals.paid);
    amountLine(summary, "Kalan Borç", totals.remaining);
    if (totals.excess > 0) amountLine(summary, "Fazla Ödeme", totals.excess);
    summary.addView(
        text(ledger.accounts.size() + " Kişi · " + totals.closed + " Tamamı Ödendi", 12, muted));
    LinearLayout filters = card(body);
    EditText query = input(filters, "Ad Soyad, Ticari Unvan Veya Cari Kodu", debtQuery, false);
    action(
        filters,
        "Ara",
        () -> {
          debtQuery = query.getText().toString();
          debtPage = 0;
          render();
        });
    query.setHint("Ad Soyad Veya Ticari Unvan");
    List<Payments.Account> accounts = new ArrayList<>();
    String q = Model.fold(debtQuery);
    for (Payments.Account a : ledger.accounts.values())
      if (q.isEmpty() || Model.fold(a.code + " " + a.name() + " " + a.title).contains(q))
        accounts.add(a);
    debtPage = Math.min(debtPage, Math.max(0, (accounts.size() - 1) / 30));
    for (int i = debtPage * 30; i < Math.min((debtPage + 1) * 30, accounts.size()); i++) {
      Payments.Account a = accounts.get(i);
      LinearLayout item = card(body);
      item.setTag("debt-account-" + a.code);
      LinearLayout info = column();
      info.addView(bold(a.name(), 18));
      info.addView(text("Cari Kodu: " + a.code, 13, muted));
      if (!a.title.isEmpty()) info.addView(text(a.title, 13, muted));
      info.addView(
          text(
              a.remaining() == 0
                  ? (a.excess() > 0 ? "Tamamı Ödendi · Fazla Ödeme Var" : "Tamamı Ödendi")
                  : a.paid() > 0 ? "Kısmi Ödeme" : "Ödeme Bekliyor",
              14,
              blue));
      amountLine(info, "İlk Borç", a.original);
      amountLine(info, "Ödenen", a.paid());
      amountLine(info, "Kalan", a.remaining());
      if (a.excess() > 0) amountLine(info, "Fazla Ödeme", a.excess());
      Payments.Trend trend =
          Payments.trend(state, a.code, java.time.YearMonth.parse(ledger.period));
      info.addView(text("Son 3 Ayda " + trend.appearances + " Farklı Ayda Listede", 12, muted));
      info.addView(
          text("Yüklenen Dönemlerde Borcun %" + trend.percent + " Kadarı Ödenmiş", 12, muted));
      if (trend.unpaidStreak >= 3)
        info.addView(
            text(trend.unpaidStreak + " Ardışık Dönemde Ödeme Kaydı Girilmemiş", 13, 0xFFB04E40));
      if (a.remaining() == 0) info.setAlpha(.55f);
      item.addView(info);
      action(
          item,
          a.remaining() > 0 ? "Ödeme Gir" : "Ödeme Detayı",
          () -> paymentDialog(ledger.id, a.code));
      action(item, "Düzenle / Sil", () -> debtAccountDialog(ledger.id, a.code));
    }
    if (accounts.isEmpty()) body.addView(text("Kayıt Bulunamadı.", 14, muted));
    if (debtPage > 0)
      action(
          body,
          "Önceki 30 Kişi",
          () -> {
            debtPage--;
            render();
          });
    if ((debtPage + 1) * 30 < accounts.size())
      action(
          body,
          "Sonraki 30 Kişi",
          () -> {
            debtPage++;
            render();
          });
  }

  void debtListDialog(String id) {
    Payments.Ledger ledger = Payments.find(state, id);
    LinearLayout box = column();
    EditText name = input(box, "Liste Adı", ledger.name, false);
    name.setTag("debt-edit-name");
    name.setFilters(new InputFilter[] {new InputFilter.LengthFilter(100)});
    EditText period = input(box, "Dönem (YYYY-AA, Örnek: 2026-10)", ledger.period, false);
    period.setTag("debt-edit-period");
    box.addView(
        text(
            "Dönem değişince geçmiş analizleri yeniden hesaplanır. Aynı dönemde cari kod"
                + " çakışmasına izin verilmez.",
            12,
            muted));
    AlertDialog window = dialog("Listeyi Düzenle / Sil", box);
    action(
        box,
        "Liste Değişikliklerini Kaydet",
        () -> {
          String title = name.getText().toString(), month = period.getText().toString().trim();
          confirm(
              "Listeyi Güncelle",
              "Liste Adı Ve Dönemi Güncellenecek. Kişiler Ve Ödeme Kayıtları Korunacak.",
              () ->
                  mutate(
                      copy -> Payments.editLedger(copy, id, title, month),
                      false,
                      () -> {
                        window.dismiss();
                        render();
                        toast("Liste Güncellendi.");
                      }));
        });
    action(
        box,
        "Bu Listenin Tamamını Sil",
        () ->
            confirm(
                "Borç Listesini Sil",
                ledger.name
                    + " · "
                    + ledger.period
                    + "\n"
                    + ledger.accounts.size()
                    + " Kişi Ve Bu Listeye Ait Tüm Ödeme Geçmişi Silinecek. Diğer Listeler Ve"
                    + " Rehber Korunacak. Silme Geri Alınamaz; Gerekirse Önce Şifreli Yedek Alın.",
                () ->
                    mutate(
                        copy -> Payments.removeLedger(copy, id),
                        false,
                        () -> {
                          window.dismiss();
                          debtLedger = "";
                          debtQuery = "";
                          debtPage = 0;
                          render();
                          toast("Borç Listesi Silindi.");
                        })));
  }

  void debtAccountDialog(String ledgerId, String code) {
    Payments.Account a = Payments.find(state, ledgerId).accounts.get(code);
    if (a == null) {
      toast("Cari Kaydı Bulunamadı.");
      return;
    }
    LinearLayout box = column();
    EditText newCode = input(box, "Cari Kodu", a.code, false);
    newCode.setTag("debt-edit-code");
    EditText first = input(box, "Adı", a.first, false);
    first.setTag("debt-edit-first");
    EditText last = input(box, "Soyadı", a.last, false);
    EditText title = input(box, "Ticari Unvanı", a.title, false);
    EditText original =
        input(
            box,
            "İlk Borç Tutarı (TL)",
            java.math.BigDecimal.valueOf(a.original, 2).toPlainString().replace('.', ','),
            false);
    original.setTag("debt-edit-original");
    original.setInputType(
        android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
    box.addView(
        text(
            "Ödemeler korunur. İlk borcu düzeltmek liste toplamını değiştirir. Cari kod değişirse"
                + " önceki aylardaki eski kodla bağlantı değişir; diğer listeler otomatik"
                + " düzenlenmez.",
            12,
            muted));
    AlertDialog window = dialog("Borç Kaydını Düzenle / Sil", box);
    action(
        box,
        "Kayıt Değişikliklerini Kaydet",
        () -> {
          try {
            long amount = Payments.money(original.getText().toString(), false);
            String nextCode = newCode.getText().toString(),
                ad = first.getText().toString(),
                soyad = last.getText().toString(),
                unvan = title.getText().toString();
            confirm(
                "Borç Kaydını Güncelle",
                a.name()
                    + " · "
                    + a.code
                    + "\nYeni İlk Borç: "
                    + Payments.format(amount)
                    + "\nListe Toplamı Yeniden Hesaplanacak. Ödeme Geçmişi Korunacak.",
                () ->
                    mutate(
                        copy ->
                            Payments.edit(copy, ledgerId, code, nextCode, ad, soyad, unvan, amount),
                        false,
                        () -> {
                          window.dismiss();
                          render();
                          toast("Borç Kaydı Güncellendi.");
                        }));
          } catch (Exception e) {
            error(e);
          }
        });
    action(
        box,
        "Bu Kaydı Sil",
        () ->
            confirm(
                "Borç Kaydını Sil",
                a.name()
                    + " · "
                    + a.code
                    + "\n"
                    + "Bu Kişi Ve Bu Listedeki Ödeme Geçmişi Silinecek. Liste Toplamları Ve"
                    + " Analizler Yeniden Hesaplanacak. Diğer Aylardaki Kayıtları Ve Rehber"
                    + " Korunacak. Silme Geri Alınamaz.",
                () ->
                    mutate(
                        copy -> Payments.removeAccount(copy, ledgerId, code),
                        false,
                        () -> {
                          window.dismiss();
                          render();
                          toast("Borç Kaydı Silindi.");
                        })));
  }

  void paymentDialog(String ledgerId, String code) {
    Payments.Account a = Payments.find(state, ledgerId).accounts.get(code);
    if (a == null) {
      toast("Cari Kaydı Bulunamadı.");
      return;
    }
    LinearLayout box = column();
    box.addView(bold(a.name() + " · " + a.code, 18));
    Payments.Trend history =
        Payments.trend(
            state, a.code, java.time.YearMonth.parse(Payments.find(state, ledgerId).period));
    box.addView(
        text(
            "Son 3 Ayda "
                + history.appearances
                + " Ay Listede · Borcun %"
                + history.percent
                + " Kadarı Ödenmiş",
            13,
            muted));
    if (history.unpaidStreak >= 3)
      box.addView(
          text(history.unpaidStreak + " Ardışık Dönemde Ödeme Kaydı Girilmemiş", 14, 0xFFB04E40));
    amountLine(box, "İlk Borç", a.original);
    amountLine(box, "Ödenen", a.paid());
    amountLine(box, "Kalan", a.remaining());
    EditText amount = input(box, "Ödeme Tutarı (TL)", "", false);
    amount.setTag("payment-amount");
    amount.setInputType(
        android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
    EditText note = input(box, "Ödeme Notu (İsteğe Bağlı)", "", false);
    note.setFilters(new InputFilter[] {new InputFilter.LengthFilter(500)});
    AlertDialog window = dialog("Ödeme Gir", box);
    action(
        box,
        "Girilen Ödemeyi Kaydet",
        () -> {
          try {
            long cents = Payments.money(amount.getText().toString(), false);
            if (cents <= 0) throw new IllegalArgumentException("Ödeme Sıfırdan Büyük Olmalı.");
            confirmPayment(ledgerId, code, cents, note.getText().toString(), window);
          } catch (Exception error) {
            error(error);
          }
        });
    if (a.remaining() > 0)
      action(
          box,
          "Tamamı Ödendi",
          () -> confirmPayment(ledgerId, code, a.remaining(), note.getText().toString(), window));
    box.addView(bold("Ödeme Geçmişi", 16));
    int shown = 0;
    for (int i = a.payments.size() - 1; i >= 0 && shown < 100; i--, shown++) {
      Payments.Entry entry = a.payments.get(i);
      box.addView(
          text(
              format(entry.time)
                  + " · "
                  + Payments.format(entry.amount)
                  + (entry.cancelled ? " · İptal Edildi" : ""),
              13,
              entry.cancelled ? muted : ink));
      if (!entry.note.isEmpty()) box.addView(text(entry.note, 12, muted));
      if (!entry.cancelled)
        action(
            box,
            "Bu Ödemeyi İptal Et",
            () ->
                confirm(
                    "Ödemeyi İptal Et",
                    Payments.format(entry.amount)
                        + " Ödeme Hesaptan Çıkarılacak. İlk Borç Değişmeyecek.",
                    () ->
                        mutate(
                            copy -> {
                              Payments.Account target =
                                  Payments.find(copy, ledgerId).accounts.get(code);
                              for (Payments.Entry e : target.payments)
                                if (e.id.equals(entry.id)) e.cancelled = true;
                            },
                            false,
                            () -> {
                              window.dismiss();
                              render();
                              paymentDialog(ledgerId, code);
                            })));
    }
    if (a.payments.size() > 100)
      box.addView(text("Son 100 Ödeme Gösteriliyor; Tümü Şifreli Yedekte Korunur.", 12, muted));
  }

  void confirmPayment(String ledgerId, String code, long cents, String note, AlertDialog window) {
    Payments.Account a = Payments.find(state, ledgerId).accounts.get(code);
    String detail = a.name() + "\nÖdeme: " + Payments.format(cents);
    if (cents > a.remaining())
      detail += "\nBu Ödemenin Borcu Aşan Kısmı: " + Payments.format(cents - a.remaining());
    confirm(
        "Ödemeyi Onayla",
        detail,
        () ->
            mutate(
                copy -> Payments.add(Payments.find(copy, ledgerId).accounts.get(code), cents, note),
                false,
                () -> {
                  window.dismiss();
                  render();
                  toast("Ödeme Kaydedildi.");
                }));
  }

  void previewDebtImport(Excel.Table table) {
    task(
        () -> Payments.read(table),
        ledger -> {
          if (state.debts.size() >= 100) {
            toast("En Fazla 100 Borç Listesi Saklanabilir.");
            return;
          }
          LinearLayout box = column();
          EditText name =
              input(
                  box,
                  "Liste Adı / Dönem",
                  "Kesinti Listesi "
                      + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                  false);
          name.setFilters(new InputFilter[] {new InputFilter.LengthFilter(100)});
          String[] monthNames = {
            "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül",
            "Ekim", "Kasım", "Aralık"
          };
          Spinner month = spinner(box, "Listenin Ait Olduğu Ay", monthNames);
          month.setSelection(LocalDate.now().getMonthValue() - 1);
          EditText year = input(box, "Yıl", String.valueOf(LocalDate.now().getYear()), false);
          year.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
          box.addView(
              text(
                  "Geçmiş karşılaştırması için dosyanın ait olduğu gerçek ayı seçin. Listeye girmek"
                      + " ve ödeme girmemiş olmak ayrı değerlendirilir.",
                  12,
                  muted));
          box.addView(
              text(ledger.accounts.size() + " Kişi · Cari Koduna Göre Ayrı Kayıt", 14, ink));
          amountLine(box, "İlk Listenin Genel Toplamı", Payments.totals(ledger).original);
          if (!ledger.importWarnings.isEmpty()) {
            box.addView(bold("Tutar Kontrolü · " + ledger.importWarnings.size() + " Uyarı", 16));
            for (int i = 0; i < Math.min(20, ledger.importWarnings.size()); i++)
              box.addView(text(ledger.importWarnings.get(i), 13, muted));
            if (ledger.importWarnings.size() > 20)
              box.addView(text("İlk 20 Uyarı Gösteriliyor.", 12, muted));
            box.addView(
                text(
                    "Kontrol Ederek Onaylayın. Kaydettikten Sonra Düzenle / Sil İle Borç Tutarını"
                        + " Düzeltebilirsiniz.",
                    13,
                    ink));
          }
          box.addView(
              text(
                  "Bu toplam ödeme girildiğinde değişmez. Mevcut rehber ve önceki borç listeleri"
                      + " korunur. Telefon numarası gerekmez.",
                  13,
                  muted));
          AlertDialog preview = dialog("Borç Listesi Önizlemesi", box);
          action(
              box,
              "Onayla Ve Borç Listesini Ekle",
              () -> {
                String title = name.getText().toString().trim();
                if (title.isEmpty()) {
                  toast("Liste Adı Girin.");
                  return;
                }
                try {
                  int y = Integer.parseInt(year.getText().toString().trim());
                  if (y < 1900 || y > 9999) throw new IllegalArgumentException();
                  ledger.period =
                      java.time.YearMonth.of(y, month.getSelectedItemPosition() + 1).toString();
                } catch (Exception invalid) {
                  toast("Geçerli Bir Yıl Girin.");
                  return;
                }
                for (Payments.Ledger old : state.debts)
                  if (old.period.equals(ledger.period)) {
                    if (old.fingerprint.equals(ledger.fingerprint)) {
                      preview.dismiss();
                      debtLedger = old.id;
                      openPayments();
                      toast("Bu Dönemin Listesi Zaten Kayıtlı. Ödemeler Korundu.");
                      return;
                    }
                    for (String code : ledger.accounts.keySet())
                      if (old.accounts.containsKey(code)) {
                        toast(
                            "Bu Dönemde Aynı Cari Kod Zaten Kayıtlı. Mükerrer Borç Eklenmedi. Yeni"
                                + " Ay İse Ay/Yıl Seçimini Düzeltin.");
                        return;
                      }
                  }
                ledger.name = title;
                mutate(
                    copy -> {
                      if (copy.debts.size() >= 100)
                        throw new IllegalArgumentException("Borç Listesi Sınırı.");
                      copy.debts.add(ledger);
                    },
                    false,
                    () -> {
                      preview.dismiss();
                      if (importing != null) {
                        importing.delete();
                        importing = null;
                      }
                      debtLedger = ledger.id;
                      openPayments();
                      int warnings = 0;
                      for (Payments.Account account : ledger.accounts.values())
                        if (Payments.trend(
                                    state, account.code, java.time.YearMonth.parse(ledger.period))
                                .unpaidStreak
                            >= 3) warnings++;
                      if (warnings > 0)
                        show(
                            new AlertDialog.Builder(this)
                                .setTitle("Geçmiş Dönem Uyarısı")
                                .setMessage(
                                    warnings
                                        + " Kişinin En Az 3 Ardışık Döneminde Ödeme Kaydı"
                                        + " Girilmemiş. Kişi Kartlarında Ayrıntıları"
                                        + " Görebilirsiniz. Bu Bilgi Yalnız Uygulamaya Yüklenen"
                                        + " Liste Ve Ödemelere Dayanır.")
                                .setPositiveButton("Tamam", null));
                      else toast("Borç Listesi Eklendi.");
                    });
              });
        });
  }

  void crmDialog(String id) {
    Person person = state.people.get(id);
    if (person == null) {
      toast("Kişi Kaydı Bulunamadı.");
      return;
    }
    LinearLayout box = column();
    box.addView(bold(person.name() + " · " + person.sicil, 17));
    Spinner follow =
        spinner(
            box, "Takip Durumu", new String[] {"İşlem Bekliyor", "Tekrar İletişim", "Tamamlandı"});
    follow.setTag("crm-follow");
    follow.setSelection(person.followUp.ordinal());
    EditText notes = input(box, "Kişi Notları", person.not, true);
    notes.setTag("crm-notes");
    notes.setFilters(new InputFilter[] {new InputFilter.LengthFilter(10000)});
    action(
        box,
        "Kişinin İletişim Geçmişi",
        () -> {
          lastDialog.dismiss();
          capture();
          historyPerson = id;
          crmMode = 0;
          historyPage = 0;
          screen = 3;
          render();
        });
    AlertDialog dialog = dialog("Takip Ve Notlar", box);
    dialog.setButton(
        AlertDialog.BUTTON_POSITIVE,
        "Kaydet",
        (android.content.DialogInterface.OnClickListener) null);
    dialog
        .getButton(AlertDialog.BUTTON_POSITIVE)
        .setOnClickListener(
            v -> {
              if (busy || locked || failed) return;
              FollowUp choice = FollowUp.values()[follow.getSelectedItemPosition()];
              String note = notes.getText().toString();
              capture();
              mutate(
                  copy -> {
                    Person target = copy.people.get(id);
                    if (target == null)
                      throw new IllegalArgumentException("Kişi Kaydı Bulunamadı.");
                    if (target.followUp != choice)
                      target.followUpChanged = System.currentTimeMillis();
                    target.followUp = choice;
                    target.not = note;
                  },
                  true,
                  () -> {
                    dialog.dismiss();
                    render();
                    toast("Takip Ve Notlar Kaydedildi.");
                  });
            });
  }

  void followList() {
    LinearLayout filters = card(body);
    Spinner filter =
        spinner(
            filters,
            "Takip Durumu",
            new String[] {"Tümü", "İşlem Bekliyor", "Tekrar İletişim", "Tamamlandı"});
    filter.setSelection(followFilter);
    EditText query = input(filters, "Sicil Veya Ad Soyad", followQuery, false);
    action(
        filters,
        "Listele",
        () -> {
          followFilter = filter.getSelectedItemPosition();
          followQuery = query.getText().toString();
          followPage = 0;
          render();
        });
    List<Person> people =
        Crm.people(
            state, followFilter == 0 ? null : FollowUp.values()[followFilter - 1], followQuery);
    body.addView(text(people.size() + " Kayıt", 13, muted));
    for (int i = followPage * 50; i < Math.min((followPage + 1) * 50, people.size()); i++) {
      Person p = people.get(i);
      LinearLayout item = card(body);
      item.setTag("follow-person-" + p.sicil);
      item.addView(bold(p.name() + " · " + p.sicil, 16));
      item.addView(text(p.followUp.label, 14, blue));
      if (!p.not.isEmpty()) {
        TextView note = text(p.not, 13, muted);
        note.setMaxLines(3);
        item.addView(note);
      }
      action(item, "Takip Ve Notlar", () -> crmDialog(p.sicil));
      action(item, "Mesaj Hazırla", () -> select(p.sicil));
    }
    if (followPage > 0)
      action(
          body,
          "Önceki 50 Kişi",
          () -> {
            followPage--;
            render();
          });
    if ((followPage + 1) * 50 < people.size())
      action(
          body,
          "Sonraki 50 Kişi",
          () -> {
            followPage++;
            render();
          });
  }

  void crmReport() {
    Crm.Report r = Crm.report(state, LocalDate.now(), ZoneId.systemDefault());
    body.addView(
        bold("Bugün · " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), 18));
    reportLine("Hazırlanan Mesajlar", r.preparedToday);
    reportLine("Gönderildi Olarak İşaretlenen", r.sentMarkedToday);
    reportLine("Bugün Tamamlanan Kişi İşlemleri", r.completedToday);
    body.addView(bold("Güncel Takip Durumu", 18));
    reportLine("Toplam Kişi", r.total);
    reportLine("İşlem Bekliyor", r.pending);
    reportLine("Tekrar İletişim", r.again);
    reportLine("Tamamlandı", r.complete);
    body.addView(
        text(
            "Mesaj sayıları saklanan son 1000 geçmiş kaydına dayanır. Gönderim sayısı, bugün"
                + " hazırlanan mesajlardan kullanıcı tarafından gönderildi işaretlenenlerdir."
                + " Tamamlanan kişi sayısı güncel takip durumuna göre hesaplanır; gönderim beyanı"
                + " takip durumunu otomatik değiştirmez.",
            12,
            muted));
    action(body, "Raporu Yenile", this::render);
  }

  void reportLine(String label, int value) {
    LinearLayout item = card(body);
    LinearLayout line = row();
    line.addView(text(label, 14, ink), new LinearLayout.LayoutParams(0, -2, 1));
    line.addView(bold(String.valueOf(value), 22));
    item.addView(line);
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
                (kind == IMPORT || kind == DEBT_IMPORT)
                    ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    : "*/*");
    if (kind == IMPORT || kind == DEBT_IMPORT)
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
    if (request == IMPORT || request == DEBT_IMPORT) {
      debtImport = request == DEBT_IMPORT;
      loadImport(uri);
    } else if (request == DEBT_SAMPLE) {
      task(
          () -> {
            try (InputStream in = getAssets().open("Borc-Sablonu.xlsx");
                OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
              if (out == null) throw new IOException("Dosya Kaydedilemedi.");
              byte[] buffer = new byte[8192];
              int count;
              while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            }
            return true;
          },
          ok ->
              toast(
                  "Borç Şablonu Kaydedildi. Doldurduktan Sonra Borç Excel Listesi Yükle İle"
                      + " Açın."));
    } else if (request == RESTORE) passwordDialog(true, uri);
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
                            + "Mevcut rehber, şablon, geçmiş ve ödeme listeleri yedekle"
                            + " değiştirilecek. İşlem öncesi şifreli geri alma kopyası"
                            + " oluşturulur.",
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
                                      if (debtImport || Payments.looksLike(table))
                                        previewDebtImport(table);
                                      else mappingDialog();
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
