package com.semih.nova;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.speech.*;
import android.speech.tts.*;
import android.text.InputFilter;
import android.text.util.Linkify;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MainActivity extends Activity implements RecognitionListener {
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler ui = new Handler(Looper.getMainLooper());
  private Store db;
  private SharedPreferences prefs;
  private LinearLayout root, body;
  private ScrollView scroll;
  private EditText composer;
  private TextView live, status;
  private Button voiceButton;
  private int page = 0, shownPage = -1;
  private boolean light, busy, modelJob, recording, ttsReady, follow = true;
  private String draft = "", streamText = "", operation = "";
  private final AtomicBoolean cancel = new AtomicBoolean();
  private Documents.Result document;
  private final VoiceRecorder recorder = new VoiceRecorder();
  private boolean localRecording;
  private SpeechRecognizer recognizer;
  private TextToSpeech tts;
  private final Conversation conversation = new Conversation();
  private boolean foreground;
  private String activeUtterance = "";
  private WeatherQuery pendingWeather;
  private boolean lastWeather;
  private String lastResearchQuery = "";
  private EditText messageRecipient;
  private int bg, surface, text, muted, accent, line;
  private static final Locale TR = new Locale("tr", "TR");

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    db = new Store(this);
    prefs = getSharedPreferences("nova", MODE_PRIVATE);
    light = prefs.getBoolean("light", false);
    if (state != null) {
      page = state.getInt("page");
      draft = state.getString("draft", "");
    }
    if (getIntent().getBooleanExtra("tasks", false)) page = 1;
    tts =
        new TextToSpeech(
            this,
            result -> {
              ttsReady = result == TextToSpeech.SUCCESS;
            });
    tts.setOnUtteranceProgressListener(
        new UtteranceProgressListener() {
          public void onStart(String id) {}

          public void onDone(String id) {
            ui.post(
                () -> {
                  if (!id.equals(activeUtterance)) return;
                  activeUtterance = "";
                  int token = conversation.session();
                  if (conversation.canListen(token))
                    ui.postDelayed(
                        () -> {
                          if (foreground && page == 0 && conversation.canListen(token))
                            startSpeech();
                        },
                        500);
                });
          }

          public void onError(String id) {
            ui.post(
                () -> {
                  if (id.equals(activeUtterance)) {
                    stopConversation(false);
                    info("Sesli cevap okunamadı; konuşma durduruldu.");
                  }
                });
          }
        });
    ReminderReceiver.all(this);
    render();
  }

  @Override
  protected void onSaveInstanceState(Bundle s) {
    super.onSaveInstanceState(s);
    s.putInt("page", page);
    s.putString("draft", composer != null && page == 0 ? composer.getText().toString() : draft);
  }

  @Override
  protected void onResume() {
    super.onResume();
    foreground = true;
    ReminderReceiver.all(this);
  }

  @Override
  protected void onPause() {
    foreground = false;
    stopConversation(false);
    if (recording) {
      recorder.cancel();
      if (recognizer != null) recognizer.cancel();
      recording = false;
      localRecording = false;
    }
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    conversation.stop();
    activeUtterance = "";
    cancel.set(true);
    recorder.cancel();
    NativeBridge.stop();
    if (recognizer != null) recognizer.destroy();
    if (tts != null) {
      tts.stop();
      tts.shutdown();
    }
    worker.execute(
        () -> {
          NativeBridge.release();
          db.close();
        });
    worker.shutdown();
    super.onDestroy();
  }

  private int dp(float n) {
    return (int) (getResources().getDisplayMetrics().density * n + .5f);
  }

  private void colors() {
    bg = Color.parseColor(light ? "#F7F8FC" : "#0B1020");
    surface = Color.parseColor(light ? "#FFFFFF" : "#151D30");
    text = Color.parseColor(light ? "#172033" : "#F3F5FC");
    muted = Color.parseColor(light ? "#59647B" : "#A7B1C8");
    accent = Color.parseColor(light ? "#5B4FE9" : "#AAA2FF");
    line = Color.parseColor(light ? "#E6E9F2" : "#29334B");
  }

  private GradientDrawable shape(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    return d;
  }

  private LinearLayout col() {
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

  private void add(LinearLayout p, View v) {
    p.addView(v, new LinearLayout.LayoutParams(-1, -2));
  }

  private void gap(LinearLayout p, int n) {
    View v = new View(this);
    p.addView(v, new LinearLayout.LayoutParams(1, dp(n)));
  }

  private void weight(LinearLayout p, View v) {
    p.addView(v, new LinearLayout.LayoutParams(0, -2, 1));
  }

  private TextView label(String s, int sp, int color, boolean bold) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextSize(sp);
    t.setTextColor(color);
    t.setFontFeatureSettings("kern");
    t.setLineSpacing(dp(3), 1);
    if (bold) t.setTypeface(null, Typeface.BOLD);
    return t;
  }

  private TextView copy(LinearLayout p, String s, int sp, int color, boolean bold) {
    TextView t = label(s, sp, color, bold);
    add(p, t);
    return t;
  }

  private LinearLayout card(LinearLayout p) {
    LinearLayout c = col();
    c.setPadding(dp(18), dp(18), dp(18), dp(18));
    GradientDrawable d = shape(surface, 22);
    d.setStroke(dp(1), line);
    c.setBackground(d);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = dp(12);
    p.addView(c, lp);
    return c;
  }

  private Button button(String s, boolean primary, Runnable action) {
    Button b = new Button(this);
    b.setText(s);
    b.setTextSize(13);
    b.setAllCaps(false);
    b.setMinHeight(dp(48));
    b.setMinimumHeight(dp(48));
    b.setTextColor(primary ? Color.WHITE : accent);
    b.setPadding(dp(12), dp(8), dp(12), dp(8));
    b.setBackground(shape(primary ? (light ? 0xFF5B4FE9 : 0xFF6255CE) : surface, 15));
    b.setOnClickListener(v -> action.run());
    return b;
  }

  private EditText field(String hint, String value, int max) {
    EditText e = new EditText(this);
    e.setTextColor(text);
    e.setHintTextColor(muted);
    e.setTextSize(15);
    e.setHint(hint);
    e.setText(value);
    e.setPadding(dp(14), dp(12), dp(14), dp(12));
    e.setBackground(shape(bg, 14));
    e.setFilters(new InputFilter[] {new InputFilter.LengthFilter(max)});
    return e;
  }

  private void info(String message) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }

  private String error(Throwable e) {
    return e.getMessage() == null ? "İşlem tamamlanamadı." : e.getMessage();
  }

  private void confirm(String title, String detail, Runnable action) {
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(detail)
        .setNegativeButton("Vazgeç", null)
        .setPositiveButton("Onaylıyorum", (d, w) -> action.run())
        .show();
  }

  private String modelPath() {
    return prefs.getString("model", "");
  }

  private boolean hasModel() {
    String path = modelPath();
    return !path.isEmpty() && new File(path).isFile();
  }

  private void render() {
    if (isDestroyed()) return;
    int y = shownPage == page && scroll != null ? scroll.getScrollY() : 0;
    if (shownPage == 0 && composer != null) draft = composer.getText().toString();
    shownPage = page;
    composer = null;
    live = null;
    status = null;
    voiceButton = null;
    colors();
    getWindow().setStatusBarColor(bg);
    getWindow().setNavigationBarColor(bg);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            light
                ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                : 0);
    root = col();
    root.setBackgroundColor(bg);
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
    LinearLayout head = row();
    head.setPadding(dp(22), dp(12), dp(22), dp(14));
    ImageView logo = new ImageView(this);
    logo.setImageResource(R.drawable.ic_nova);
    head.addView(logo, new LinearLayout.LayoutParams(dp(42), dp(42)));
    View spacer = new View(this);
    head.addView(spacer, new LinearLayout.LayoutParams(dp(12), 1));
    LinearLayout title = col();
    copy(title, "NOVA", 11, accent, true);
    copy(
        title,
        new String[] {"Adaş", "Görevler", "Hafıza", "Belgeler", "Ayarlar"}[page],
        22,
        text,
        true);
    weight(head, title);
    TextView dot = label("● KİŞİSEL", 10, 0xFF37BAA3, true);
    head.addView(dot);
    add(root, head);
    scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setClipToPadding(false);
    scroll.setVerticalScrollBarEnabled(false);
    body = col();
    body.setPadding(dp(20), dp(8), dp(20), dp(16));
    scroll.addView(body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    scroll.setOnScrollChangeListener(
        (v, sx, sy, ox, oy) -> {
          if (page == 0) follow = body.getHeight() - scroll.getHeight() - sy < dp(120);
        });
    switch (page) {
      case 0:
        chat();
        break;
      case 1:
        tasks();
        break;
      case 2:
        memories();
        break;
      case 3:
        documents();
        break;
      default:
        settings();
    }
    if (page == 0) compose();
    LinearLayout nav = row();
    nav.setPadding(dp(8), dp(6), dp(8), dp(8));
    nav.setBackground(shape(surface, 22));
    String[] names = {"Sohbet", "Görevler", "Hafıza", "Belgeler", "Ayarlar"};
    String[] icons = {"✦", "✓", "◇", "▤", "⚙"};
    for (int i = 0; i < 5; i++) {
      final int index = i;
      LinearLayout item = col();
      item.setGravity(Gravity.CENTER);
      item.setPadding(dp(2), dp(7), dp(2), dp(7));
      TextView icon = label(icons[i], 21, i == page ? accent : muted, true);
      item.addView(icon);
      TextView name = label(names[i], 10, i == page ? accent : muted, i == page);
      item.addView(name);
      item.setContentDescription(names[i]);
      item.setOnClickListener(
          v -> {
            if (conversation.active()) stopConversation(false);
            if (recording) {
              info("Önce ses kaydını bitir.");
              return;
            }
            page = index;
            render();
          });
      nav.addView(item, new LinearLayout.LayoutParams(0, dp(58), 1));
    }
    LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(-1, -2);
    nl.setMargins(dp(12), dp(4), dp(12), dp(8));
    root.addView(nav, nl);
    final ScrollView current = scroll;
    current
        .getViewTreeObserver()
        .addOnPreDrawListener(
            new android.view.ViewTreeObserver.OnPreDrawListener() {
              public boolean onPreDraw() {
                current.getViewTreeObserver().removeOnPreDrawListener(this);
                current.scrollTo(0, y);
                return true;
              }
            });
  }

  private void chat() {
    List<Store.Row> messages = db.messages();
    if (messages.isEmpty()) {
      LinearLayout hero = card(body);
      GradientDrawable gradient =
          new GradientDrawable(
              GradientDrawable.Orientation.TL_BR,
              light ? new int[] {0xFFE8E3FF, 0xFFF1F5FF} : new int[] {0xFF2B2252, 0xFF162B3B});
      gradient.setCornerRadius(dp(24));
      hero.setBackground(gradient);
      copy(hero, "SANA ÖZEL BİR ALAN", 10, accent, true);
      gap(hero, 18);
      copy(hero, "Merhaba,\nben Adaş.", 34, text, true);
      gap(hero, 10);
      copy(
          hero,
          "Birlikte düşünelim. Bir soru sor, bir belgeyi keşfet ya da aklındakileri paylaş.",
          15,
          muted,
          false);
      gap(hero, 22);
      copy(hero, "Kişisel  ·  Ücretsiz  ·  Cihazında", 11, accent, true);
      if (!hasModel()) {
        LinearLayout setup = card(body);
        copy(setup, "Önce Adaş'ın modelini kuralım", 17, text, true);
        gap(setup, 7);
        copy(
            setup,
            "Sohbet için ücretsiz yerel model gerekir. İlk indirme yaklaşık 1,1 GB; sonrasında"
                + " internet gerekmez.",
            13,
            muted,
            false);
        gap(setup, 10);
        add(
            setup,
            button(
                "Model kurulumunu aç",
                true,
                () -> {
                  page = 4;
                  render();
                }));
      }
      copy(body, "NEREDEN BAŞLAYALIM?", 11, muted, true);
      gap(body, 10);
      for (String s :
          new String[] {
            "Bugünümü planlamama yardımcı ol",
            "Bir konuyu sade bir dille açıkla",
            "Birlikte yeni bir fikir geliştirelim"
          }) {
        LinearLayout c = card(body);
        copy(c, s + "  ↗", 14, text, false);
        c.setOnClickListener(
            v -> {
              composer.setText(s);
              composer.requestFocus();
            });
      }
    }
    for (Store.Row m : messages) {
      LinearLayout c = card(body);
      if (m.role.equals("user")) c.setBackground(shape(light ? 0xFFEDE9FF : 0xFF282347, 20));
      copy(c, m.role.equals("user") ? "SEN" : "ADAŞ", 10, accent, true);
      gap(c, 8);
      TextView value = copy(c, m.body, 15, text, false);
      value.setTextIsSelectable(true);
      if (m.role.equals("tool")) Linkify.addLinks(value, Linkify.WEB_URLS);
    }
    if (busy && !modelJob) {
      LinearLayout c = card(body);
      copy(c, "ADAŞ · " + (streamText.isEmpty() ? "Düşünüyor…" : "Yanıtlıyor"), 10, accent, true);
      gap(c, 8);
      live =
          copy(
              c,
              streamText.isEmpty()
                  ? (lastWeather
                      ? "Ücretsiz hava tahmini kaynağına bağlanılıyor. İnternet gerekir."
                      : "Model ilk açılışta biraz zaman alabilir.")
                  : streamText,
              15,
              text,
              false);
    }
    if (document != null) {
      LinearLayout c = card(body);
      copy(c, "SOHBETE EKLİ BELGE", 10, accent, true);
      copy(c, document.name, 14, text, true);
      copy(c, "Soruyla ilgili sınırlı metin bölümü kullanılır.", 11, muted, false);
      add(
          c,
          button(
              "Belgeyi sohbetten çıkar",
              false,
              () -> {
                document = null;
                render();
              }));
    }
  }

  private ScrollView dialogContent(View form) {
    ScrollView content = new ScrollView(this);
    content.addView(form);
    return content;
  }

  private void messageDraft(MessageDraft seed) {
    LinearLayout form = col();
    form.setPadding(dp(20), dp(8), dp(20), dp(8));
    if (!seed.hint.isEmpty())
      copy(form, "Kişi ipucu: " + seed.hint + ". Doğru numarayı sen seç.", 13, muted, false);
    EditText target = field("Alıcı telefon numarası (+90…)", seed.recipient, 40);
    target.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
    messageRecipient = target;
    add(form, target);
    add(
        form,
        button(
            "Rehberden numara seç",
            false,
            () -> {
              try {
                startActivityForResult(
                    new Intent(
                        Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
                    31);
              } catch (ActivityNotFoundException e) {
                info("Kişi seçici yok; numarayı elle yaz.");
              }
            }));
    EditText content = field("Mesaj metni", seed.body, 2000);
    content.setMinLines(3);
    add(form, content);
    Spinner channel = new Spinner(this);
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            new String[] {"WhatsApp (internet)", "SMS (operatör ücreti olabilir)"});
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    channel.setAdapter(adapter);
    channel.setSelection(seed.whatsapp ? 0 : 1);
    add(form, channel);
    CheckBox sms = new CheckBox(this);
    sms.setText(R.string.sms_cost_consent);
    sms.setTextColor(text);
    add(form, sms);
    channel.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(AdapterView<?> p) {}

          public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
            sms.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
            sms.setChecked(false);
          }
        });
    copy(
        form,
        "Adaş otomatik mesaj göndermez. Önce önizleme, sonra onay; son Gönder işlemi WhatsApp/SMS"
            + " uygulamasında sende.",
        12,
        muted,
        false);
    AlertDialog editor =
        new AlertDialog.Builder(this)
            .setTitle("Mesaj taslağı")
            .setView(dialogContent(form))
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Önizle", null)
            .create();
    editor.setOnDismissListener(
        d -> {
          if (messageRecipient == target) messageRecipient = null;
        });
    editor.setOnShowListener(
        d ->
            editor
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      MessageDraft snapshot = new MessageDraft();
                      snapshot.recipient = target.getText().toString().trim();
                      snapshot.body = content.getText().toString().trim();
                      snapshot.whatsapp = channel.getSelectedItemPosition() == 0;
                      if (!snapshot.whatsapp && !sms.isChecked()) {
                        info(
                            "SMS için olası operatör ücretini onaylamalısın. Ücretsiz seçenek için"
                                + " WhatsApp seç.");
                        return;
                      }
                      try {
                        String preview = snapshot.preview();
                        new AlertDialog.Builder(this)
                            .setTitle("Alıcı ve metni kontrol et")
                            .setMessage(preview)
                            .setNegativeButton("Geri dön", null)
                            .setPositiveButton(
                                "Onayla ve aç",
                                (dialog, w) -> {
                                  try {
                                    Intent intent = MessageIntents.build(snapshot);
                                    try {
                                      startActivity(intent);
                                    } catch (ActivityNotFoundException absent) {
                                      if (!snapshot.whatsapp) throw absent;
                                      startActivity(intent.setPackage("com.whatsapp.w4b"));
                                    }
                                    editor.dismiss();
                                    db.message(
                                        "notice",
                                        "Taslak ekranı açıldı. Mesajın gönderildiğini doğrulayamam;"
                                            + " son Gönder işlemi ilgili uygulamada sende.");
                                    render();
                                  } catch (ActivityNotFoundException e) {
                                    info(
                                        "İlgili mesaj uygulaması kurulu değil. Mesaj"
                                            + " gönderilmedi.");
                                  } catch (RuntimeException e) {
                                    info("Taslak açılamadı: " + error(e));
                                  }
                                })
                            .show();
                      } catch (IllegalArgumentException e) {
                        info(e.getMessage());
                      }
                    }));
    editor.show();
  }

  private void researchDialog() {
    EditText topic = field("Araştırılacak konu", "", 200);
    new AlertDialog.Builder(this)
        .setTitle("Kaynaklı araştırma")
        .setMessage("Konu DuckDuckGo ve Wikipedia'ya gönderilir. Özel bilgilerini yazma.")
        .setView(topic)
        .setNegativeButton("Vazgeç", null)
        .setPositiveButton(
            "Araştır",
            (d, w) -> {
              if (busy) {
                info("Önce devam eden işlemi bitir.");
                return;
              }
              composer.setText(
                  getString(R.string.research_command, topic.getText().toString().trim()));
              send();
            })
        .show();
  }

  private void compose() {
    LinearLayout area = col();
    area.setPadding(dp(16), dp(8), dp(16), dp(4));
    LinearLayout input = row();
    composer = field("Adaş'a bir şey sor…", draft, 700);
    composer.setMaxLines(4);
    composer.setMinLines(1);
    composer.setContentDescription("Adaş'a mesaj");
    weight(input, composer);
    Button send =
        button(
            busy ? "Durdur" : "Gönder",
            true,
            () -> {
              if (busy) {
                stopWork();
                return;
              }
              send();
            });
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(90), -2);
    p.leftMargin = dp(8);
    input.addView(send, p);
    add(area, input);
    LinearLayout tools = row();
    voiceButton = button(conversationLabel(), false, this::toggleConversation);
    voiceButton.setContentDescription("Sesli sohbeti başlat veya bitir");
    weight(tools, voiceButton);
    Button say =
        button(
            "Seslendir",
            false,
            () -> {
              List<Store.Row> ms = db.messages();
              for (int i = ms.size() - 1; i >= 0; i--)
                if (ms.get(i).role.equals("assistant")) {
                  speak(ms.get(i).body);
                  return;
                }
              info("Önce bir cevap oluştur.");
            });
    weight(tools, say);
    add(area, tools);
    LinearLayout actions = row();
    weight(
        actions,
        button(
            "Hatırlat",
            false,
            () -> {
              stopConversation(false);
              taskDialog(null);
            }));
    weight(
        actions,
        button(
            "Mesaj",
            false,
            () -> {
              stopConversation(false);
              messageDraft(new MessageDraft());
            }));
    weight(actions, button("Araştır", false, this::researchDialog));
    add(area, actions);
    if (!lastResearchQuery.isEmpty())
      add(
          area,
          button(
              "Kaynakları tarayıcıda ara ↗",
              false,
              () -> {
                try {
                  startActivity(
                      new Intent(
                          Intent.ACTION_VIEW,
                          Uri.parse("https://duckduckgo.com/")
                              .buildUpon()
                              .appendQueryParameter("q", lastResearchQuery)
                              .build()));
                } catch (ActivityNotFoundException e) {
                  info("Tarayıcı bulunamadı.");
                }
              }));
    add(root, area);
  }

  private void send() {
    if (recording || busy) return;
    String q = composer.getText().toString().trim();
    if (q.isEmpty()) return;
    if (ReminderDraft.matches(q) || MessageDraft.matches(q)) {
      stopConversation(false);
      pendingWeather = null;
      lastWeather = false;
      db.message("user", q);
      boolean reminder = !MessageDraft.matches(q) && ReminderDraft.matches(q);
      db.message(
          "notice",
          reminder
              ? "Hatırlatma henüz kaydedilmedi. Tarih, saat ve sesli okuma seçimini onayla."
              : "Mesaj gönderilmedi. Alıcıyı ve metni önizle; yalnızca taslak açılabilir.");
      draft = "";
      composer.setText(R.string.status_1);
      render();
      bottom();
      if (reminder) taskDialog(null, ReminderDraft.parse(q, ZonedDateTime.now()));
      else messageDraft(MessageDraft.parse(q));
      return;
    }
    final String research = Research.matches(q) ? Research.query(q) : null;
    if (research != null) pendingWeather = null;
    List<Store.Row> history = db.messages();
    WeatherQuery weather = null;
    if (WeatherQuery.matches(q) || (lastWeather && WeatherQuery.followup(q)))
      weather = WeatherQuery.parse(q, prefs.getString("weatherCity", ""));
    else if (pendingWeather != null && !WeatherQuery.normalize(q).matches("vazgec|iptal|bosver")) {
      weather = new WeatherQuery(Prompt.cut(q, 60), pendingWeather.day);
      pendingWeather = null;
    }
    boolean decline =
        pendingWeather != null && WeatherQuery.normalize(q).matches("vazgec|iptal|bosver");
    if (decline) {
      pendingWeather = null;
      weather = null;
    }
    String currentInfo =
        decline ? "Tamam, hava durumu sorgusunu iptal ettim." : AnswerQuality.currentInfo(q);
    if (research == null && weather == null && currentInfo == null && !hasModel()) {
      stopConversation(false);
      info(
          "Yerel modeli kurmadan genel sohbet yapılamaz. Hava durumu şehir adıyla model olmadan da"
              + " çalışır.");
      page = 4;
      render();
      return;
    }
    db.message("user", q);
    draft = "";
    composer.setText(R.string.status_1);
    busy = true;
    modelJob = false;
    cancel.set(false);
    streamText =
        research == null ? "" : "Web ve Wikipedia kaynaklarına bağlanılıyor. İnternet gerekir.";
    activeUtterance = "";
    if (tts != null) tts.stop();
    NativeBridge.reset();
    conversation.move(conversation.session(), Conversation.State.THINKING);
    lastWeather = weather != null;
    render();
    bottom();
    if (weather != null && weather.city.isEmpty()) {
      pendingWeather = weather;
      lastWeather = true;
      completeAnswer("Hangi şehir için hava durumunu öğrenmek istiyorsun?", "notice");
      return;
    }
    if (research != null) {
      lastResearchQuery = research;
      lastWeather = false;
      worker.execute(
          () -> {
            try {
              completeAnswer(Research.find(research), "tool");
            } catch (Exception e) {
              completeAnswer("Araştırma tamamlanamadı: " + error(e), "notice");
            }
          });
      return;
    }
    final WeatherQuery request = weather;
    lastWeather = weather != null;
    String prompt =
        Prompt.build(
            history, db.memories(), q, document == null ? "" : Prompt.relevant(document.text, q));
    String path = modelPath();
    worker.execute(
        () -> {
          try {
            if (request != null) {
              try {
                Weather.Result found = Weather.get(request);
                if (!cancel.get()) prefs.edit().putString("weatherCity", found.city).apply();
                completeAnswer(found.answer, "tool");
              } catch (Exception e) {
                completeAnswer(
                    "Hava durumu kaynağına ulaşamadım: "
                        + error(e)
                        + " İnternet bağlantısını ve şehir adını kontrol et. Güncel sıcaklık veya"
                        + " tahmin uydurmayacağım.",
                    "notice");
              }
              return;
            }
            if (currentInfo != null) {
              completeAnswer(currentInfo, "notice");
              return;
            }
            String answer = generate(path, prompt);
            boolean bad = AnswerQuality.retry(answer, history, q);
            if (bad && !cancel.get()) {
              NativeBridge.reset();
              streamText = "";
              String fresh =
                  Prompt.build(
                      Collections.emptyList(),
                      db.memories(),
                      q,
                      document == null ? "" : Prompt.relevant(document.text, q));
              answer = generate(path, fresh);
              bad = AnswerQuality.retry(answer, history, q);
            }
            if (bad && !cancel.get()) {
              completeAnswer(
                  "Bu soruya güvenilir ve ilgili bir cevap üretemedim. Aynı cevabı tekrar"
                      + " sunmayacağım. Soruyu daha dar bir konuya ayırabiliriz; küçük yerel"
                      + " modelin anlama kapasitesi sınırlı.",
                  "notice");
              return;
            }
            completeAnswer(answer, "assistant");
          } catch (Throwable e) {
            ui.post(
                () -> {
                  busy = false;
                  streamText = "";
                  stopConversation(false);
                  if (isDestroyed()) return;
                  render();
                  if (foreground)
                    new AlertDialog.Builder(this)
                        .setTitle("Adaş cevap oluşturamadı")
                        .setMessage(error(e))
                        .setPositiveButton("Tamam", null)
                        .show();
                });
          }
        });
  }

  private String generate(String path, String prompt) {
    byte[] bytes =
        NativeBridge.generate(
            path,
            prompt.getBytes(StandardCharsets.UTF_8),
            part -> {
              String value = new String(part, StandardCharsets.UTF_8);
              if (AnswerQuality.loop(value)) NativeBridge.stop();
              ui.post(
                  () -> {
                    if (isDestroyed() || cancel.get()) return;
                    streamText = value;
                    if (live != null) {
                      live.setText(value);
                      if (follow) bottom();
                    }
                  });
            });
    String identity = new String(NativeBridge.modelInfo(), StandardCharsets.UTF_8);
    prefs.edit().putString("modelInfo", identity).apply();
    String normalized = identity.toLowerCase(Locale.ROOT);
    if (!(normalized.contains("qwen2") || normalized.contains("qwen3"))
        || !normalized.contains("instruct"))
      throw new IllegalStateException(
          "Seçili model bu sohbet profiliyle uyumlu değil: "
              + identity
              + ". Ayarlar'dan Qwen2.5-1.5B-Instruct Q4_K_M dosyasını kur. Dosyan silinmedi.");
    return new String(bytes, StandardCharsets.UTF_8).trim();
  }

  private void completeAnswer(String answer, String role) {
    if (!cancel.get()) db.message(role, answer);
    ui.post(
        () -> {
          busy = false;
          modelJob = false;
          streamText = "";
          if (isDestroyed()) return;
          render();
          bottom();
          if (!cancel.get() && (conversation.active() || prefs.getBoolean("speak", false))) {
            if (!speak(
                    role.equals("tool") && answer.startsWith("Araştırma:")
                        ? Prompt.cut(answer.replaceAll("https://\\S+", ""), 1200)
                        : answer)
                && conversation.active()) stopConversation(false);
          }
        });
  }

  private String conversationLabel() {
    if (!conversation.active()) return getString(R.string.status_5);
    switch (conversation.state()) {
      case LISTENING:
        return getString(R.string.status_4);
      case TRANSCRIBING:
        return "Çözümleniyor · bitir";
      case THINKING:
        return "Düşünüyor · bitir";
      default:
        return "Konuşuyor · bitir";
    }
  }

  private void refreshVoiceLabel() {
    if (voiceButton != null) voiceButton.setText(conversationLabel());
  }

  private void toggleConversation() {
    if (conversation.active()) {
      stopConversation(true);
      return;
    }
    if (busy) {
      info("Önce devam eden işlemi bitir.");
      return;
    }
    if (!ttsReady) {
      info("Ses motoru hazırlanıyor. Biraz sonra tekrar dene.");
      return;
    }
    if (offlineVoice() == null) {
      info("Karşılıklı konuşma için Android'de çevrimdışı Türkçe ses paketini kurmalısın.");
      return;
    }
    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO}, 12);
      return;
    }
    cancel.set(false);
    conversation.start();
    startSpeech();
    refreshVoiceLabel();
  }

  private void stopConversation(boolean user) {
    boolean active = conversation.active();
    conversation.stop();
    activeUtterance = "";
    if (active) {
      recorder.cancel();
      if (recognizer != null) recognizer.cancel();
      recording = false;
      localRecording = false;
      if (tts != null) tts.stop();
      if (busy) {
        cancel.set(true);
        NativeBridge.stop();
      }
    }
    refreshVoiceLabel();
    if (user) info("Sesli sohbet bitti; mikrofon kapalı.");
  }

  private void acceptedSpeech(String value) {
    if (isDestroyed() || !foreground || page != 0) return;
    String recognized = Prompt.cut(value.trim(), 700);
    if (recognized.isEmpty()) {
      stopConversation(false);
      info("Konuşma anlaşılamadı; yeniden başlatabilirsin.");
      return;
    }
    if (composer != null) {
      composer.setText(recognized);
      composer.setSelection(composer.length());
    }
    if (conversation.active()) send();
  }

  private void bottom() {
    if (page == 0 && scroll != null) scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
  }

  private void stopWork() {
    stopConversation(false);
    cancel.set(true);
    NativeBridge.stop();
    operation = "Durduruluyor…";
    if (status != null) status.setText(operation);
    if (tts != null) tts.stop();
    info("Durdurma isteği gönderildi.");
  }

  private void memories() {
    copy(body, "Seni tanıyan, kontrolü sende kalan hafıza.", 17, text, true);
    gap(body, 7);
    copy(
        body,
        "Adaş kendiliğinden bilgi kaydetmez. Buraya eklediğin bilgiler sorulara cevap verirken"
            + " kullanılabilir.",
        13,
        muted,
        false);
    gap(body, 14);
    add(body, button("+ Hafıza ekle", true, () -> memoryDialog(null)));
    gap(body, 16);
    List<Store.Row> rows = db.memories();
    if (rows.isEmpty())
      copy(
          card(body),
          "Henüz kayıt yok. Örneğin ilgi alanlarını veya tercihlerini ekleyebilirsin.",
          14,
          muted,
          false);
    for (Store.Row r : rows) {
      LinearLayout c = card(body);
      copy(c, r.title, 17, text, true);
      gap(c, 7);
      copy(c, r.body, 14, muted, false);
      gap(c, 10);
      LinearLayout actions = row();
      weight(actions, button("Düzenle", false, () -> memoryDialog(r)));
      weight(
          actions,
          button(
              "Sil",
              false,
              () ->
                  confirm(
                      "Hafıza silinsin mi?",
                      r.title
                          + "\n"
                          + "Bu bilgi bundan sonraki sorulara eklenmeyecek. Eski sohbetlerdeki"
                          + " kopyaları ayrıca yönetebilirsin.",
                      () -> {
                        db.removeMemory(r.id);
                        render();
                      })));
      add(c, actions);
    }
    copy(
        body,
        "Son 100 hafıza gösterilir. Sorulara sınırlı sayıda kısa hafıza eklenir.",
        11,
        muted,
        false);
  }

  private void memoryDialog(Store.Row r) {
    LinearLayout form = col();
    form.setPadding(dp(20), dp(10), dp(20), dp(10));
    EditText title = field("Başlık", r == null ? "" : r.title, 80),
        value = field("Adaş neyi hatırlasın?", r == null ? "" : r.body, 1000);
    add(form, title);
    gap(form, 10);
    add(form, value);
    AlertDialog d =
        new AlertDialog.Builder(this)
            .setTitle(r == null ? "Hafıza ekle" : "Hafızayı düzenle")
            .setView(form)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Kaydet", null)
            .create();
    d.setOnShowListener(
        v ->
            d.getButton(-1)
                .setOnClickListener(
                    w -> {
                      String a = title.getText().toString().trim(),
                          b = value.getText().toString().trim();
                      if (a.isEmpty() || b.isEmpty()) {
                        info("Başlık ve bilgi gerekli.");
                        return;
                      }
                      db.saveMemory(r == null ? 0 : r.id, a, b);
                      d.dismiss();
                      render();
                    }));
    d.show();
  }

  private String date(long time) {
    return new SimpleDateFormat("dd MMM yyyy · HH:mm", TR).format(new Date(time));
  }

  private void tasks() {
    copy(body, "Aklında tutmak zorunda değilsin.", 23, text, true);
    gap(body, 8);
    copy(
        body,
        "Görevlerini kaydet; Adaş zamanı yaklaşınca cihaz bildirimiyle hatırlatsın.",
        14,
        muted,
        false);
    gap(body, 14);
    add(body, button("+ Görev ve hatırlatma", true, () -> taskDialog(null)));
    gap(body, 12);
    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    boolean enabled =
        nm.areNotificationsEnabled()
            && (Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED);
    NotificationChannel reminders = nm.getNotificationChannel(ReminderReceiver.CHANNEL);
    enabled =
        enabled
            && (reminders == null
                || reminders.getImportance() != NotificationManager.IMPORTANCE_NONE);
    LinearLayout note = card(body);
    copy(note, enabled ? "Bildirimler açık" : "Bildirim izni gerekli", 14, accent, true);
    copy(
        note,
        "Sesli görevler, çevrimdışı Türkçe ses kuruluysa okunur. Sessiz mod ve Rahatsız Etmeyin"
            + " korunur. Xiaomi pil kısıtları veya zorla durdurma hatırlatmayı engelleyebilir.",
        12,
        muted,
        false);
    if (!enabled) add(note, button("Bildirim iznini aç", false, this::notificationPermission));
    AlarmManager alarm = getSystemService(AlarmManager.class);
    boolean exact = Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms();
    copy(
        note,
        exact ? "Kesin zamanlama izni açık" : "Kesin zamanlama izni kapalı: uyarılar gecikebilir.",
        12,
        muted,
        false);
    if (!exact)
      add(
          note,
          button(
              "Kesin zamanlama iznini incele",
              false,
              () -> {
                try {
                  startActivity(
                      new Intent(
                          android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                          Uri.parse("package:" + getPackageName())));
                } catch (ActivityNotFoundException e) {
                  info("Bu cihazda izin ekranı bulunamadı.");
                }
              }));
    List<Store.Row> rows = db.tasks();
    if (rows.isEmpty())
      copy(
          card(body),
          "İlk görevini ekle. Görevler internet olmadan da saklanır.",
          14,
          muted,
          false);
    for (Store.Row r : rows) {
      LinearLayout c = card(body);
      copy(
          c,
          r.done
              ? "✓ TAMAMLANDI"
              : r.time < System.currentTimeMillis() ? "ZAMANI GELDİ" : "YAKLAŞAN",
          10,
          r.done ? muted : accent,
          true);
      gap(c, 7);
      copy(c, r.title, 18, text, true);
      copy(c, date(r.time) + (r.audible ? " · Sesli + bildirim" : " · Bildirim"), 12, muted, false);
      gap(c, 8);
      LinearLayout actions = row();
      weight(
          actions,
          button(
              r.done ? "Geri aç" : "Tamamlandı",
              false,
              () -> {
                db.done(r.id, !r.done);
                if (!r.done) ReminderReceiver.cancel(this, r.id);
                else ReminderReceiver.schedule(this, r.id, r.time);
                render();
              }));
      weight(actions, button("Düzenle", false, () -> taskDialog(r)));
      weight(
          actions,
          button(
              "Sil",
              false,
              () ->
                  confirm(
                      "Görev silinsin mi?",
                      r.title + "\nHatırlatması da iptal edilecek.",
                      () -> {
                        ReminderReceiver.cancel(this, r.id);
                        db.removeTask(r.id);
                        render();
                      })));
      add(c, actions);
    }
    copy(
        body,
        "Son 100 görev listelenir; hatırlatma planlaması tüm bekleyen görevleri kapsar.",
        11,
        muted,
        false);
  }

  private void taskDialog(Store.Row r) {
    taskDialog(r, null);
  }

  private void taskDialog(Store.Row r, ReminderDraft parsed) {
    LinearLayout form = col();
    form.setPadding(dp(20), dp(8), dp(20), dp(8));
    EditText title =
        field("Görev adı", r == null ? (parsed == null ? "" : parsed.title) : r.title, 200);
    add(form, title);
    if (parsed != null && !parsed.issue.isEmpty()) copy(form, parsed.issue, 13, muted, false);
    CheckBox audible = new CheckBox(this);
    audible.setText(R.string.audible_consent);
    audible.setTextColor(text);
    audible.setChecked(r != null ? r.audible : parsed != null && parsed.audible);
    add(form, audible);
    copy(
        form,
        "Saat dilimi: "
            + java.time.ZoneId.systemDefault().getId()
            + ". Bildirim ve ses motoru izinlerini Görevler'den kontrol et.",
        12,
        muted,
        false);
    gap(form, 14);
    boolean[] chosen = {r != null || parsed == null || parsed.due != null};
    Calendar selected = Calendar.getInstance();
    selected.setTimeInMillis(r == null ? System.currentTimeMillis() + 3600000 : r.time);
    if (parsed != null && parsed.due != null)
      selected.setTimeInMillis(parsed.due.toInstant().toEpochMilli());
    selected.set(Calendar.SECOND, 0);
    selected.set(Calendar.MILLISECOND, 0);
    Button when =
        button(chosen[0] ? date(selected.getTimeInMillis()) : "Tarih ve saat seç", false, () -> {});
    when.setOnClickListener(
        v ->
            new DatePickerDialog(
                    this,
                    (picker, y, m, d) -> {
                      selected.set(y, m, d);
                      new TimePickerDialog(
                              this,
                              (clock, h, min) -> {
                                selected.set(Calendar.HOUR_OF_DAY, h);
                                selected.set(Calendar.MINUTE, min);
                                chosen[0] = true;
                                when.setText(date(selected.getTimeInMillis()));
                              },
                              selected.get(Calendar.HOUR_OF_DAY),
                              selected.get(Calendar.MINUTE),
                              true)
                          .show();
                    },
                    selected.get(Calendar.YEAR),
                    selected.get(Calendar.MONTH),
                    selected.get(Calendar.DAY_OF_MONTH))
                .show());
    add(form, when);
    AlertDialog d =
        new AlertDialog.Builder(this)
            .setTitle(
                parsed != null
                    ? "Hatırlatmayı onayla"
                    : r == null ? "Yeni görev" : "Görevi düzenle")
            .setView(dialogContent(form))
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton(parsed != null ? "Onayla ve kaydet" : "Kaydet", null)
            .create();
    d.setOnShowListener(
        v ->
            d.getButton(-1)
                .setOnClickListener(
                    w -> {
                      String name = title.getText().toString().trim();
                      if (name.isEmpty()) {
                        info("Görev adı gerekli.");
                        return;
                      }
                      if (!chosen[0] || selected.getTimeInMillis() <= System.currentTimeMillis()) {
                        info("Gelecekte bir tarih ve saat seç.");
                        return;
                      }
                      long id =
                          db.saveTask(
                              r == null ? 0 : r.id,
                              name,
                              selected.getTimeInMillis(),
                              audible.isChecked());
                      if (r == null || !r.done)
                        ReminderReceiver.schedule(this, id, selected.getTimeInMillis());
                      d.dismiss();
                      render();
                    }));
    d.show();
  }

  private void notificationPermission() {
    if (Build.VERSION.SDK_INT >= 33
        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 11);
    else
      startActivity(
          new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
              .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName()));
  }

  private void documents() {
    copy(body, "Dosyalarınla birlikte düşün.", 25, text, true);
    gap(body, 9);
    copy(
        body,
        "PDF, Word (.docx), Excel (.xlsx) ve metin dosyalarını cihazında incele. Dosyaların"
            + " sunucuya gönderilmez.",
        14,
        muted,
        false);
    gap(body, 15);
    add(
        body,
        button(
            "+ Belge seç",
            true,
            () -> {
              if (busy) {
                info("Önce devam eden işlemi bitir.");
                return;
              }
              Intent i =
                  new Intent(Intent.ACTION_OPEN_DOCUMENT)
                      .setType("*/*")
                      .addCategory(Intent.CATEGORY_OPENABLE);
              i.putExtra(
                  Intent.EXTRA_MIME_TYPES,
                  new String[] {
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "text/plain"
                  });
              startActivityForResult(i, 21);
            }));
    gap(body, 14);
    LinearLayout limits = card(body);
    copy(limits, "Kapsamı bilerek incele", 16, text, true);
    gap(limits, 7);
    copy(
        limits,
        "En fazla 10 MB. Taranmış PDF'lerde OCR yoktur. Excel formülleri çalıştırılmaz; kayıtlı"
            + " değerler okunur. Uzun belgelerden soruyla ilgili sınırlı bölüm seçilir.",
        13,
        muted,
        false);
    if (busy && modelJob) {
      status = copy(card(body), operation, 14, accent, false);
    }
    if (document != null) {
      LinearLayout c = card(body);
      copy(c, document.name, 19, text, true);
      gap(c, 8);
      copy(c, document.coverage, 12, muted, false);
      gap(c, 12);
      copy(c, document.text.length() + " karakter çıkarıldı", 11, accent, true);
      gap(c, 10);
      copy(c, Prompt.cut(document.text, 1800), 13, text, false);
      gap(c, 14);
      add(
          c,
          button(
              "Bu belge hakkında Adaş'a sor",
              true,
              () -> {
                page = 0;
                render();
                composer.setText(R.string.status_2);
              }));
    }
  }

  private void settings() {
    LinearLayout model = card(body);
    copy(model, "ADAŞ'IN YEREL MODELİ", 10, accent, true);
    gap(model, 9);
    copy(model, "Önerilen: Qwen2.5 · 1.5B Instruct", 21, text, true);
    gap(model, 7);
    copy(
        model,
        "Ücretsiz · Q4_K_M · yaklaşık 1,1 GB\n"
            + "Xiaomi 12T Pro / 12 GB RAM hedefi\n"
            + "Sohbet cihazda çalışır; API anahtarı gerekmez.",
        13,
        muted,
        false);
    gap(model, 10);
    if (hasModel()) {
      File f = new File(modelPath());
      copy(model, "Seçili GGUF · " + f.length() / 1048576 + " MB", 13, accent, true);
      copy(
          model,
          prefs.getString(
              "modelInfo",
              "Model kimliği ilk çalıştırmada doğrulanacak. Şu an yalnızca dosya seçili."),
          12,
          muted,
          false);
      copy(model, "Dosyanın model uyumu ilk sohbette denetlenir.", 11, muted, false);
    } else copy(model, "Henüz model seçilmedi", 13, muted, false);
    gap(model, 12);
    add(
        model,
        button(
            "Ücretsiz modeli indir",
            true,
            () -> {
              if (busy) {
                info("Önce devam eden işlemi bitir.");
                return;
              }
              confirm(
                  "Yerel model indirilsin mi?",
                  "Hugging Face üzerindeki resmî Qwen deposundan yaklaşık 1,1 GB indirilecek."
                      + " Ücretli API yoktur; bağlantı tarifendeki veri kullanılır. Wi-Fi ve en az"
                      + " 2 GB boş alan önerilir. İndirme boyunca uygulamayı açık tut.",
                  () -> modelAction(null));
            }));
    gap(model, 8);
    add(
        model,
        button(
            "İndirilmiş GGUF dosyasını seç",
            false,
            () -> {
              if (busy) {
                info("Önce devam eden işlemi bitir.");
                return;
              }
              confirm(
                  "Model dosyası seç",
                  "Güvenilir kaynaktan Qwen2.5-1.5B-Instruct Q4_K_M GGUF dosyasını seç. Dosya"
                      + " uygulama alanına kopyalanır; kaynağı silinmez. Uyumsuz modeller"
                      + " çalışmayabilir.",
                  () ->
                      startActivityForResult(
                          new Intent(Intent.ACTION_OPEN_DOCUMENT)
                              .setType("*/*")
                              .addCategory(Intent.CATEGORY_OPENABLE),
                          22));
            }));
    if (busy && modelJob) {
      gap(model, 10);
      status = copy(model, operation, 13, accent, false);
      add(model, button("İşlemi durdur", false, this::stopWork));
    }
    LinearLayout voiceCard = card(body);
    copy(voiceCard, "Türkçe çevrimdışı bas-konuş", 18, text, true);
    gap(voiceCard, 8);
    copy(
        voiceCard,
        "Whisper tiny çok dilli · yaklaşık 75 MB. Ses telefonda çözülür; 30 saniyeye kadar kayıt."
            + " Model yoksa yalnızca cihazın çevrimdışı servisi denenir.",
        13,
        muted,
        false);
    gap(voiceCard, 10);
    String voicePath = prefs.getString("voiceModel", "");
    copy(
        voiceCard,
        !voicePath.isEmpty() && new File(voicePath).isFile()
            ? "Yerel ses modeli seçili"
            : "Yerel ses modeli henüz kurulmadı",
        12,
        accent,
        true);
    add(
        voiceCard,
        button(
            "Ücretsiz ses modelini indir",
            false,
            () -> {
              if (busy) {
                info("Önce devam eden işlemi bitir.");
                return;
              }
              confirm(
                  "Ses modeli indirilsin mi?",
                  "Hugging Face ggerganov/whisper.cpp deposundan ggml-tiny.bin yaklaşık 75 MB"
                      + " indirilecek. Ücretli servis kullanılmaz.",
                  () -> modelAction(null, true));
            }));
    gap(voiceCard, 8);
    add(
        voiceCard,
        button(
            "İndirilmiş ggml-tiny.bin seç",
            false,
            () -> {
              if (busy) return;
              startActivityForResult(
                  new Intent(Intent.ACTION_OPEN_DOCUMENT)
                      .setType("*/*")
                      .addCategory(Intent.CATEGORY_OPENABLE),
                  23);
            }));
    LinearLayout weatherCard = card(body);
    copy(weatherCard, "Güncel hava durumu", 18, text, true);
    copy(
        weatherCard,
        "Şehir: "
            + prefs.getString("weatherCity", "Henüz seçilmedi")
            + "\n"
            + "Open-Meteo ücretsiz kişisel kullanım. Yalnızca sorunca internetten şehir ve tahmin"
            + " alınır; GPS veya sohbet geçmişi gönderilmez.",
        13,
        muted,
        false);
    add(
        weatherCard,
        button(
            "Hava durumu şehrimi değiştir",
            false,
            () -> {
              EditText city = field("Örneğin İstanbul", prefs.getString("weatherCity", ""), 60);
              new AlertDialog.Builder(this)
                  .setTitle("Hava durumu şehri")
                  .setView(city)
                  .setNegativeButton("Vazgeç", null)
                  .setPositiveButton(
                      "Kaydet",
                      (d, w) -> {
                        prefs
                            .edit()
                            .putString("weatherCity", city.getText().toString().trim())
                            .apply();
                        render();
                      })
                  .show();
            }));
    LinearLayout view = card(body);
    copy(view, "Sana göre görünüm", 18, text, true);
    gap(view, 10);
    add(
        view,
        button(
            light ? "Koyu temaya geç" : "Açık temaya geç",
            false,
            () -> {
              light = !light;
              prefs.edit().putBoolean("light", light).apply();
              render();
            }));
    gap(view, 8);
    boolean speech = prefs.getBoolean("speak", false);
    add(
        view,
        button(
            speech ? "Otomatik sesli cevap: açık" : "Otomatik sesli cevap: kapalı",
            false,
            () -> {
              prefs.edit().putBoolean("speak", !speech).apply();
              render();
            }));
    add(
        view,
        button(
            "Sesi durdur",
            false,
            () -> {
              if (tts != null) tts.stop();
            }));
    LinearLayout privacy = card(body);
    copy(privacy, "Kontrol sende", 18, text, true);
    gap(privacy, 8);
    copy(
        privacy,
        "Adaş ödeme yapamaz ve sistem ayarlarını değiştiremez. Silme işlemleri onay ister. Mesajlar"
            + " yalnızca onaylı taslak olarak açılır; son Gönder işlemi sende. Kritik işlemler için"
            + " bağlı bir araç yoktur. Hafıza, görev ve sohbet silme yalnızca açık onayla yapılır.",
        13,
        muted,
        false);
    gap(privacy, 12);
    add(
        privacy,
        button(
            "Sohbet geçmişini sil",
            false,
            () -> {
              if (busy) {
                info("Önce devam eden işlemi bitir.");
                return;
              }
              confirm(
                  "Sohbet geçmişi silinsin mi?",
                  "Tüm sohbet mesajları silinecek. Hafıza ve görevler korunacak. Bu işlem geri"
                      + " alınamaz.",
                  () -> {
                    db.clearChat();
                    info("Sohbet geçmişi silindi.");
                    render();
                  });
            }));
    gap(privacy, 8);
    add(privacy, button("Model ve yarım dosyaları yönet", false, this::manageModels));
    LinearLayout about = card(body);
    copy(about, "NOVA AI 1.2 · Adaş", 17, text, true);
    copy(
        about,
        "Kişisel ve ücretsiz Android sürümü\n"
            + "Yerel motor: llama.cpp b6800 (MIT)\n"
            + "Önerilen model: Qwen2.5-1.5B-Instruct (Apache-2.0)\n"
            + "PDF: PDFBox Android (Apache-2.0)\n"
            + "Android 8+; Whisper ses modeliyle çevrimdışı bas-konuş. Alternatif sistem tanıması"
            + " Android 12+ ve cihaz desteği gerektirir.",
        12,
        muted,
        false);
    gap(about, 8);
    copy(
        about,
        "Adaş hata yapabilir. Önemli bilgileri kaynaklarıyla kontrol et. Hız, pil ve sıcaklık"
            + " gerçek cihazda değişir.",
        12,
        muted,
        false);
  }

  private void modelAction(Uri uri) {
    modelAction(uri, false);
  }

  private void modelAction(Uri uri, boolean voice) {
    busy = true;
    modelJob = true;
    cancel.set(false);
    operation = "Model hazırlanıyor…";
    render();
    worker.execute(
        () -> {
          try {
            File ready =
                uri == null
                    ? Models.download(this, cancel, value -> progress(value), voice)
                    : Models.imported(this, uri, cancel, value -> progress(value), voice);
            prefs.edit().putString(voice ? "voiceModel" : "model", ready.getAbsolutePath()).apply();
            if (!voice) prefs.edit().remove("modelInfo").apply();
            ui.post(
                () -> {
                  busy = false;
                  modelJob = false;
                  if (isDestroyed()) return;
                  render();
                  info("Model dosyası hazır. Sohbet ekranından Adaş'a yazabilirsin.");
                });
          } catch (Exception e) {
            ui.post(
                () -> {
                  busy = false;
                  modelJob = false;
                  if (isDestroyed()) return;
                  render();
                  new AlertDialog.Builder(this)
                      .setTitle("Model hazırlanamadı")
                      .setMessage(
                          error(e)
                              + "\n"
                              + "Yarım dosyalar kendiliğinden silinmez; model yönetiminden onayla"
                              + " temizleyebilirsin.")
                      .setPositiveButton("Tamam", null)
                      .show();
                });
          }
        });
  }

  private void progress(String message) {
    ui.post(
        () -> {
          operation = message;
          if (!isDestroyed() && status != null) status.setText(message);
        });
  }

  private void manageModels() {
    if (busy) {
      info("Önce devam eden işlemi bitir.");
      return;
    }
    File[] files = Models.folder(this).listFiles();
    if (files == null || files.length == 0) {
      info("Henüz model dosyası yok.");
      return;
    }
    String[] choices = new String[files.length];
    for (int i = 0; i < files.length; i++)
      choices[i] = files[i].getName() + " · " + files[i].length() / 1048576 + " MB";
    new AlertDialog.Builder(this)
        .setTitle("Silinecek dosyayı seç")
        .setItems(
            choices,
            (d, i) -> {
              File file = files[i];
              confirm(
                  "Bu model dosyası silinsin mi?",
                  choices[i]
                      + "\n"
                      + "Yalnızca uygulama içindeki bu kopya silinecek. Seçili modelse yeniden"
                      + " model kurman gerekir.",
                  () -> {
                    worker.execute(
                        () -> {
                          NativeBridge.release();
                          boolean removed = file.delete();
                          ui.post(
                              () -> {
                                if (removed
                                    && prefs
                                        .getString("voiceModel", "")
                                        .equals(file.getAbsolutePath()))
                                  prefs.edit().remove("voiceModel").apply();
                                if (removed && modelPath().equals(file.getAbsolutePath()))
                                  prefs.edit().remove("model").apply();
                                if (!isDestroyed()) {
                                  info(removed ? "Seçilen dosya silindi." : "Dosya silinemedi.");
                                  render();
                                }
                              });
                        });
                  });
            })
        .setNegativeButton("Vazgeç", null)
        .show();
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK || data == null || data.getData() == null) return;
    Uri uri = data.getData();
    if (request == 31) {
      if (messageRecipient != null) {
        try (Cursor contact =
            getContentResolver()
                .query(
                    uri,
                    new String[] {ContactsContract.CommonDataKinds.Phone.NUMBER},
                    null,
                    null,
                    null)) {
          if (contact != null && contact.moveToFirst())
            messageRecipient.setText(contact.getString(0));
        } catch (Exception e) {
          info("Seçilen kişinin numarası okunamadı: " + error(e));
        }
      }
      return;
    }
    if (request == 22 || request == 23) {
      modelAction(uri, request == 23);
      return;
    }
    if (request == 21) {
      if (busy) return;
      busy = true;
      modelJob = true;
      operation = "Belge cihazda okunuyor…";
      render();
      worker.execute(
          () -> {
            try {
              Documents.Result found = Documents.read(this, uri);
              ui.post(
                  () -> {
                    busy = false;
                    modelJob = false;
                    if (isDestroyed()) return;
                    document = found;
                    render();
                  });
            } catch (Exception e) {
              ui.post(
                  () -> {
                    busy = false;
                    modelJob = false;
                    if (isDestroyed()) return;
                    render();
                    new AlertDialog.Builder(this)
                        .setTitle("Belge okunamadı")
                        .setMessage(error(e))
                        .setPositiveButton("Tamam", null)
                        .show();
                  });
            }
          });
    }
  }

  private void finishSpeech() {
    if (recording && localRecording) {
      recorder.stop();
      if (voiceButton != null) voiceButton.setText(R.string.status_3);
    } else if (recording && recognizer != null) {
      recognizer.stopListening();
      if (voiceButton != null) voiceButton.setText(R.string.status_3);
    }
  }

  private void startSpeech() {
    if (busy || recording || !foreground || page != 0) return;
    conversation.move(conversation.session(), Conversation.State.LISTENING);
    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO}, 12);
      return;
    }
    String voicePath = prefs.getString("voiceModel", "");
    if (!voicePath.isEmpty() && new File(voicePath).isFile()) {
      recording = true;
      localRecording = true;
      NativeBridge.reset();
      refreshVoiceLabel();
      recorder.start(
          this,
          new VoiceRecorder.Result() {
            public void error(String message) {
              ui.post(
                  () -> {
                    recording = false;
                    localRecording = false;
                    if (isDestroyed()) return;
                    if (voiceButton != null) voiceButton.setText(R.string.status_0);
                    boolean report = conversation.active() && foreground;
                    stopConversation(false);
                    if (report) info(message);
                  });
            }

            public void done(float[] samples) {
              ui.post(
                  () -> {
                    recording = false;
                    localRecording = false;
                    if (isDestroyed()) return;
                    busy = true;
                    modelJob = true;
                    operation = "Ses cihazda Türkçe metne çevriliyor…";
                    conversation.move(conversation.session(), Conversation.State.TRANSCRIBING);
                    refreshVoiceLabel();
                    if (voiceButton != null) voiceButton.setText(R.string.status_3);
                    worker.execute(
                        () -> {
                          try {
                            byte[] decoded = NativeBridge.transcribe(voicePath, samples);
                            String value = new String(decoded, StandardCharsets.UTF_8).trim();
                            ui.post(
                                () -> {
                                  busy = false;
                                  modelJob = false;
                                  if (isDestroyed()) return;
                                  if (conversation.active()) acceptedSpeech(value);
                                  refreshVoiceLabel();
                                });
                          } catch (Throwable e) {
                            ui.post(
                                () -> {
                                  busy = false;
                                  modelJob = false;
                                  if (isDestroyed()) return;
                                  if (voiceButton != null) voiceButton.setText(R.string.status_0);
                                  stopConversation(false);
                                  if (foreground && !cancel.get()) info(MainActivity.this.error(e));
                                });
                          }
                        });
                  });
            }
          },
          conversation.active());
      return;
    }
    if (Build.VERSION.SDK_INT < 31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
      stopConversation(false);
      info(
          "Bu cihazda çevrimdışı konuşma tanıma servisi hazır değil. Android dil/ses paketini"
              + " kontrol et; metinle devam edebilirsin. Bulut servisine geçilmeyecek.");
      return;
    }
    if (recognizer == null) {
      recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
      recognizer.setRecognitionListener(this);
    }
    Intent i =
        new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
    recording = true;
    refreshVoiceLabel();
    recognizer.startListening(i);
  }

  private Voice offlineVoice() {
    if (!ttsReady || tts == null) return null;
    return OfflineSpeech.find(tts);
  }

  private boolean speak(String value) {
    Voice selected = offlineVoice();
    if (selected == null) {
      info(
          "Çevrimdışı Türkçe ses bulunamadı. Android metin okuma ayarlarından Türkçe ses paketini"
              + " kur.");
      return false;
    }
    if (tts.setVoice(selected) != TextToSpeech.SUCCESS) {
      info("Türkçe ses etkinleştirilemedi.");
      return false;
    }
    conversation.move(conversation.session(), Conversation.State.SPEAKING);
    refreshVoiceLabel();
    activeUtterance = "adas-" + conversation.session() + "-" + System.nanoTime();
    String spoken = value.split("\n\nKaynak:", 2)[0].replaceAll("[*#`]", "");
    int result =
        tts.speak(
            Prompt.cut(spoken, TextToSpeech.getMaxSpeechInputLength()),
            TextToSpeech.QUEUE_FLUSH,
            null,
            activeUtterance);
    if (result != TextToSpeech.SUCCESS) {
      activeUtterance = "";
      return false;
    }
    return true;
  }

  @Override
  public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
    super.onRequestPermissionsResult(request, permissions, grants);
    if (request == 11) {
      ReminderReceiver.all(this);
      render();
    }
    if (request == 12)
      info(
          grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED
              ? "İzin verildi. Sesli sohbet düğmesine tekrar dokun."
              : "Mikrofon izni verilmedi; metinle devam edebilirsin.");
  }

  public void onReadyForSpeech(Bundle b) {}

  public void onBeginningOfSpeech() {}

  public void onRmsChanged(float f) {}

  public void onBufferReceived(byte[] b) {}

  public void onEndOfSpeech() {
    recording = false;
    conversation.move(conversation.session(), Conversation.State.TRANSCRIBING);
    refreshVoiceLabel();
  }

  public void onError(int code) {
    if (!conversation.active()) {
      recording = false;
      return;
    }
    stopConversation(false);
    recording = false;
    if (voiceButton != null) voiceButton.setText(R.string.status_0);
    info(
        "Çevrimdışı ses çözümlenemedi ("
            + code
            + "). Türkçe dil paketi ve cihaz desteğini kontrol et.");
  }

  public void onResults(Bundle results) {
    recording = false;
    ArrayList<String> values = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
    if (conversation.active() && values != null && !values.isEmpty()) acceptedSpeech(values.get(0));
    else if (values == null || values.isEmpty()) stopConversation(false);
    refreshVoiceLabel();
  }

  public void onPartialResults(Bundle b) {}

  public void onEvent(int t, Bundle b) {}
}
