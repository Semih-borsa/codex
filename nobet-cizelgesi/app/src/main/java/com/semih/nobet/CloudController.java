package com.semih.nobet;

import static com.semih.nobet.Roster.*;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/**
 * Foreground polling and serialized authenticated uploads with persisted compare-and-swap drafts.
 */
final class CloudController {
  interface Host {
    State cloudState();

    void cloudState(State state);

    boolean cloudDraft();

    State localState() throws Exception;

    void cloudRender();

    void cloudNotice(String message);
  }

  interface Work {
    Object run() throws Exception;
  }

  interface Done {
    void accept(Object value) throws Exception;
  }

  final Activity activity;
  final Host host;
  final CloudVault vault;
  final ExecutorService worker = Executors.newSingleThreadExecutor();
  final Handler main = new Handler(Looper.getMainLooper());
  final android.content.SharedPreferences preferences;
  CloudApi api;
  String team = "", role = "viewer", teamName = "", status = "Yerel Liste • Ortak Ekip Bağlı Değil";
  long revision = 0;
  JSONObject confirmed, pending;
  String requestId = "";
  boolean online = false,
      publishing = false,
      fetching = false,
      conflict = false,
      disposed = false,
      foreground = false,
      accessRevoked = false;
  AlertDialog panel;
  boolean dark;
  int jobs = 0;
  long generation = 0;
  final Runnable poll =
      new Runnable() {
        public void run() {
          if (!foreground || disposed) return;
          if (active() && pending == null) fetch(false);
          main.postDelayed(this, 10000);
        }
      };

  CloudController(Activity activity, Host host) {
    this.activity = activity;
    this.host = host;
    vault = new CloudVault(activity);
    preferences = activity.getSharedPreferences("cloud-public-config", 0);
    try {
      String url = preferences.getString("url", ""), key = preferences.getString("key", "");
      if (!url.isEmpty()) {
        CloudApi.Config config = new CloudApi.Config(url, key);
        JSONObject saved = vault.read("session");
        CloudApi.Session session =
            saved != null && config.url.equals(saved.optString("project"))
                ? new CloudApi.Session(saved.getJSONObject("session"), null)
                : null;
        api = new CloudApi(config, session);
        bindSession();
        if (session != null) {
          team = preferences.getString("team-" + session.user, "");
          if (!team.isEmpty()) {
            JSONObject cache = vault.read(cacheName());
            host.cloudState(new State());
            if (cache != null
                && config.url.equals(cache.optString("project"))
                && session.user.equals(cache.optString("user"))) {
              revision = cache.getLong("revision");
              role = cache.getString("role");
              teamName = cache.getString("name");
              confirmed = cache.optJSONObject("confirmed");
              pending = cache.optJSONObject("pending");
              requestId = cache.optString("request");
              host.cloudState(Store.decode((pending != null ? pending : confirmed).toString()));
            }
            status =
                pending == null
                    ? "Son Kayıt • Sunucu Kontrol Ediliyor"
                    : "Bu Telefonda Bekleyen Değişiklik Var";
          }
        }
      }
    } catch (Exception e) {
      team = "";
      online = false;
      status = "Ortak Oturum Açılamadı • Yeniden Giriş Yapın";
    }
  }

  private void bindSession() {
    api.changed =
        session ->
            vault.write(
                "session",
                new JSONObject().put("project", api.config.url).put("session", session.json()));
  }

  boolean active() {
    return api != null && api.session != null && !team.isEmpty();
  }

  String key() {
    return active() ? api.config.url + "/" + api.session.user + "/" + team : "local";
  }

  private String cacheName() {
    return "team-" + api.session.user + "-" + team;
  }

  boolean canEdit() {
    return !active()
        || (online && !accessRevoked && "admin".equals(role) && pending == null && !publishing);
  }

  String editDeniedMessage() {
    if (pending != null || publishing)
      return "Önce bekleyen değişikliğin sunucuya gönderilmesini bekleyin veya Ortak Ekip"
          + " ekranından çözün.";
    if (!online) return "Ortak liste şu anda yalnız görüntülenebilir. Bağlantıyı yenileyin.";
    return "Bu hesap yalnız görüntüleme yetkisine sahip. Düzenleme için yönetici yetkisi gerekir.";
  }

  static boolean readAction(String label) {
    return Arrays.asList(
                "",
                "Çizelge",
                "Personel",
                "Mazeret",
                "İstatistik",
                "Ayarlar",
                "‹ Önceki",
                "Sonraki ›",
                "Hafta Seç",
                "Resmî Tatil Takvimi",
                "‹",
                "›")
            .contains(label)
        || label.contains("PDF");
  }

  void start() {
    foreground = true;
    main.removeCallbacks(poll);
    main.post(poll);
  }

  void stop() {
    foreground = false;
    main.removeCallbacks(poll);
  }

  void destroy() {
    disposed = true;
    stop();
    worker.shutdownNow();
    if (panel != null) panel.dismiss();
  }

  private void task(Work work, Done done) {
    if (disposed) return;
    long epoch = generation;
    jobs++;
    worker.execute(
        () -> {
          try {
            Object value = work.run();
            main.post(
                () -> {
                  jobs--;
                  if (disposed || epoch != generation) return;
                  try {
                    done.accept(value);
                  } catch (Exception e) {
                    failure(e);
                  }
                });
          } catch (Exception e) {
            main.post(
                () -> {
                  jobs--;
                  if (!disposed && epoch == generation) failure(e);
                });
          }
        });
  }

  private void failure(Exception error) {
    publishing = false;
    fetching = false;
    online = false;
    String text =
        error instanceof CloudApi.Failure
            ? error.getMessage()
            : "İşlem tamamlanamadı. Bağlantıyı ve kurulum ayarlarını kontrol edin.";
    if (error instanceof CloudApi.Failure) {
      String kind = ((CloudApi.Failure) error).kind;
      if ("conflict".equals(kind)) {
        conflict = true;
        text = "Çakışma • Yerel Değişiklik Gönderilmedi";
      }
      if ("forbidden".equals(kind) || "auth".equals(kind)) {
        accessRevoked = true;
        text = "Ortak Ekip Erişimi Doğrulanamadı • Yeniden Giriş / Yenile";
      }
    }
    status = text;
    host.cloudNotice(text);
    host.cloudRender();
  }

  private void cache() throws Exception {
    vault.write(
        cacheName(),
        new JSONObject()
            .put("project", api.config.url)
            .put("user", api.session.user)
            .put("name", teamName)
            .put("role", role)
            .put("revision", revision)
            .put(
                "confirmed",
                confirmed == null ? new JSONObject(Store.encode(new State())) : confirmed)
            .put("pending", pending == null ? JSONObject.NULL : pending)
            .put("request", requestId));
  }

  boolean stage(State state) {
    if (!active() || !canEdit()) {
      host.cloudNotice("Yalnız çevrimiçi yöneticiler ortak listeyi değiştirebilir.");
      return false;
    }
    try {
      pending = new JSONObject(Store.encode(state));
      requestId = UUID.randomUUID().toString();
      cache();
      upload();
      return true;
    } catch (Exception e) {
      pending = null;
      requestId = "";
      host.cloudNotice("Yerel güvenli kopya kaydedilemedi; sunucuya gönderilmedi.");
      return false;
    }
  }

  void restoreVisible() {
    try {
      if (pending != null) host.cloudState(Store.decode(pending.toString()));
      else if (confirmed != null) host.cloudState(Store.decode(confirmed.toString()));
      host.cloudRender();
    } catch (Exception e) {
      failure(e);
    }
  }

  private void upload() {
    if (!active() || pending == null || publishing) return;
    publishing = true;
    status = "Değişiklik Sunucuya Gönderiliyor";
    JSONObject content = pending;
    String id = requestId;
    long expected = revision;
    String target = team;
    task(
        () -> api.publish(target, expected, content, id),
        value -> {
          publishing = false;
          JSONObject snapshot = (JSONObject) value;
          pending = null;
          requestId = "";
          conflict = false;
          boolean keepDraft =
              host.cloudDraft()
                  && snapshot.getLong("revision") == expected + 1
                  && "admin".equals(snapshot.getString("role"));
          apply(snapshot, true, keepDraft);
          host.cloudNotice("Ortak liste güncellendi. Diğer telefonlar yenilemede görecek.");
        });
  }

  private void apply(JSONObject snapshot, boolean replace) throws Exception {
    apply(snapshot, replace, false);
  }

  private void apply(JSONObject snapshot, boolean replace, boolean keepOwnDraft) throws Exception {
    String previousRole = role, previousName = teamName, previousStatus = status;
    boolean wasOnline = online;
    long next = snapshot.getLong("revision");
    String nextRole = snapshot.getString("role");
    if (!Arrays.asList("admin", "viewer").contains(nextRole)) throw new Exception();
    online = true;
    accessRevoked = false;
    role = nextRole;
    teamName = snapshot.getString("name");
    JSONObject payload = snapshot.optJSONObject("payload");
    if (payload == null) payload = new JSONObject(Store.encode(new State()));
    // Validate remote state before touching a cache or the visible list.
    State loaded = Store.decode(payload.toString());
    if (pending != null) {
      status =
          next != revision
              ? "Sunucuda Yeni Sürüm Var • Yerel Değişiklik Bekliyor"
              : "Yerel Değişiklik Gönderilmeyi Bekliyor";
      host.cloudRender();
      return;
    }
    if (!replace && host.cloudDraft() && next != revision && "admin".equals(role)) {
      status = "Sunucuda Yeni Sürüm Var • Taslak Korunuyor";
      host.cloudRender();
      return;
    }
    boolean changed =
        next != revision || confirmed == null || replace || !role.equals(previousRole);
    revision = next;
    confirmed = payload;
    if (changed && !keepOwnDraft) host.cloudState(loaded);
    status =
        teamName
            + " • "
            + ("admin".equals(role) ? "Yönetici" : "Kullanıcı")
            + " • Sürüm "
            + revision;
    if (changed || !teamName.equals(previousName)) cache();
    if (changed || !teamName.equals(previousName) || !wasOnline || !status.equals(previousStatus))
      host.cloudRender();
  }

  private void fetch(boolean feedback) {
    if (!active() || fetching || publishing) return;
    if (pending != null) {
      if (feedback)
        host.cloudNotice("Bekleyen değişikliği yeniden gönderin veya sunucudaki listeyi alın.");
      return;
    }
    fetching = true;
    String target = team;
    long knownRevision = revision;
    JSONObject knownPayload = confirmed;
    task(
        () -> {
          JSONObject head = api.head(target);
          if (knownPayload == null || head.getLong("revision") != knownRevision)
            return api.snapshot(target);
          return head.put("payload", knownPayload);
        },
        value -> {
          fetching = false;
          apply((JSONObject) value, false);
          if (feedback) host.cloudNotice("Ortak liste kontrol edildi.");
        });
  }

  private void listTeams() {
    task(
        () -> {
          api.rpc("nobet_accept_invitations", new JSONObject());
          return api.rpc("nobet_my_teams", new JSONObject());
        },
        value -> {
          JSONArray teams = (JSONArray) value;
          if (teams.length() == 0) {
            team = "";
            status = "Giriş Yapıldı • Ekip Daveti Bekleniyor";
            open();
            return;
          }
          if (teams.length() == 1) selectTeam(teams.getJSONObject(0).getString("id"));
          else {
            String[] names = new String[teams.length()];
            for (int i = 0; i < names.length; i++)
              names[i] = teams.getJSONObject(i).getString("name");
            new AlertDialog.Builder(activity)
                .setTitle("Ortak Ekip Seç")
                .setItems(
                    names,
                    (d, index) -> {
                      try {
                        selectTeam(teams.getJSONObject(index).getString("id"));
                      } catch (Exception e) {
                        failure(e);
                      }
                    })
                .setNegativeButton("Vazgeç", null)
                .show();
          }
        });
  }

  private void selectTeam(String id) throws Exception {
    if (!id.matches("[0-9a-fA-F-]{36}")) throw new Exception();
    if (pending != null) {
      host.cloudNotice("Önce bekleyen değişikliği çözün.");
      return;
    }
    if (id.equals(team) && confirmed != null) {
      fetch(true);
      return;
    }
    generation++;
    fetching = false;
    team = id;
    confirmed = null;
    revision = 0;
    role = "viewer";
    online = false;
    host.cloudState(new State());
    preferences.edit().putString("team-" + api.session.user, id).apply();
    task(
        () -> api.snapshot(id),
        value -> {
          apply((JSONObject) value, true);
          open();
        });
  }

  private LinearLayout column() {
    LinearLayout layout = new LinearLayout(activity);
    layout.setOrientation(LinearLayout.VERTICAL);
    return layout;
  }

  private TextView label(String text) {
    TextView view = new TextView(activity);
    view.setText(text);
    view.setTextSize(13);
    view.setTextColor(dark ? 0xFFE7EEF7 : 0xFF18304C);
    view.setPadding(0, 8, 0, 8);
    return view;
  }

  private Button button(String text, Runnable action) {
    Button button = new Button(activity);
    button.setAllCaps(false);
    button.setText(text);
    button.setTextSize(12);
    button.setTextColor(Color.WHITE);
    button.setBackgroundTintList(null);
    button.setBackground(Visuals.touch(0xFF2A6FB1, 18, 0));
    button.setMinHeight((int) (48 * activity.getResources().getDisplayMetrics().density));
    button.setOnClickListener(
        v -> {
          if (jobs > 0) {
            host.cloudNotice("Sunucu işlemi sürüyor; tamamlanınca yeniden deneyin.");
            return;
          }
          action.run();
        });
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.topMargin = 10;
    button.setLayoutParams(lp);
    return button;
  }

  void banner(LinearLayout parent, boolean dark) {
    this.dark = dark;
    if (api == null || (api.session == null && team.isEmpty())) return;
    TextView banner = label(status);
    banner.setTextSize(11);
    banner.setPadding(24, 8, 24, 8);
    banner.setTag("cloud-status");
    banner.setOnClickListener(v -> open());
    parent.addView(banner);
  }

  void settings(LinearLayout parent, boolean dark) {
    this.dark = dark;
    parent.addView(button("Kullanıcılar Ve Ortak Ekip", this::open));
  }

  void open() {
    if (disposed) return;
    if (panel != null) panel.dismiss();
    LinearLayout box = column();
    box.setPadding(32, 12, 32, 12);
    box.addView(label(status));
    if (api == null) {
      box.addView(
          label("Supabase projenizi kurduktan sonra Project URL ve public anahtarını bağlayın."));
      box.addView(button("Sunucu Bağlantısını Ayarla", this::configure));
    } else if (api.session == null) {
      box.addView(button("Giriş Yap", () -> auth(false)));
      box.addView(button("Yeni Hesap Oluştur", () -> auth(true)));
      box.addView(button("E-posta Kodunu Doğrula", () -> verifyEmail("")));
      box.addView(button("Sunucu Bağlantısını Değiştir", this::configure));
    } else {
      box.addView(label(api.session.email));
      if (accessRevoked) box.addView(button("Oturumu Yenile • Yeniden Giriş", () -> auth(false)));
      box.addView(
          button(
              "Hesap Kimliğini Kopyala",
              () -> {
                ((android.content.ClipboardManager)
                        activity.getSystemService(Context.CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("Hesap Kimliği", api.session.user));
                host.cloudNotice("Hesap kimliği kopyalandı.");
              }));
      box.addView(button("Ekipleri Ve Davetleri Yenile", this::listTeams));
      if (!active()) box.addView(button("İlk Yönetici Olarak Ekip Oluştur", this::createTeam));
      if (active()) {
        box.addView(button("Ortak Listeyi Yenile", () -> fetch(true)));
        if (pending != null) {
          box.addView(label("Bu telefondaki değişiklik henüz ortak listeye uygulanmadı."));
          box.addView(button("Bekleyen Değişikliği Yeniden Gönder", this::upload));
          box.addView(button("Yerel Kopyayı Arşivle Ve Sunucudakini Aç", this::discardPending));
        } else if ("admin".equals(role)) {
          box.addView(button("Kullanıcıları Ve Yetkileri Yönet", this::members));
          if (revision == 0)
            box.addView(button("Telefondaki Yerel Listeyi İlk Kez Paylaş", this::importLocal));
        }
      }
      box.addView(button("Çıkış Yap • Yerel Listeye Dön", this::logout));
    }
    ScrollView scroll = new ScrollView(activity);
    scroll.addView(box);
    panel =
        new AlertDialog.Builder(activity)
            .setTitle("Kullanıcılar Ve Ortak Ekip")
            .setView(scroll)
            .setNegativeButton("Kapat", null)
            .show();
  }

  private EditText input(LinearLayout box, String hint, boolean password) {
    EditText edit = new EditText(activity);
    edit.setHint(hint);
    edit.setSingleLine(true);
    edit.setInputType(
        password
            ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            : InputType.TYPE_CLASS_TEXT);
    box.addView(edit);
    return edit;
  }

  private void configure() {
    if (api != null && api.session != null) {
      host.cloudNotice("Sunucuyu değiştirmeden önce çıkış yapın.");
      return;
    }
    LinearLayout box = column();
    box.setPadding(32, 8, 32, 8);
    EditText url = input(box, "Project URL (https://...supabase.co)", false),
        key = input(box, "Public / Publishable Anahtar", false);
    if (api != null) {
      url.setText(api.config.url);
      key.setText(api.config.key);
    }
    AlertDialog dialog =
        new AlertDialog.Builder(activity)
            .setTitle("Sunucu Bağlantısı")
            .setView(box)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Kaydet", null)
            .show();
    dialog
        .getButton(-1)
        .setOnClickListener(
            v -> {
              try {
                CloudApi.Config config =
                    new CloudApi.Config(url.getText().toString(), key.getText().toString());
                generation++;
                api = new CloudApi(config, null);
                bindSession();
                preferences
                    .edit()
                    .putString("url", config.url)
                    .putString("key", config.key)
                    .apply();
                vault.delete("session");
                dialog.dismiss();
                open();
              } catch (Exception e) {
                url.setError(e.getMessage());
              }
            });
  }

  private void auth(boolean register) {
    CloudApi.Session previousSession = api.session;
    boolean renewing = previousSession != null;
    LinearLayout box = column();
    box.setPadding(32, 8, 32, 8);
    EditText name = register ? input(box, "Ad Soyad", false) : null;
    EditText email = input(box, "E-posta", false);
    if (renewing) {
      email.setText(previousSession.email);
      email.setEnabled(false);
    }
    email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    EditText password = input(box, register ? "Şifre (En Az 8 Karakter)" : "Şifre", true);
    AlertDialog dialog =
        new AlertDialog.Builder(activity)
            .setTitle(register ? "Yeni Hesap" : "Giriş Yap")
            .setView(box)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton(register ? "Hesap Oluştur" : "Giriş Yap", null)
            .show();
    dialog
        .getButton(-1)
        .setOnClickListener(
            v -> {
              String address = email.getText().toString().trim(),
                  secret = password.getText().toString();
              if (!address.contains("@")
                  || secret.isEmpty()
                  || (register && secret.length() < 8)
                  || (register && name.getText().toString().trim().isEmpty())) {
                email.setError("E-posta, Şifre Ve Ad Bilgilerini Kontrol Edin");
                return;
              }
              String fullName = register ? name.getText().toString() : "";
              password.setText("");
              dialog.dismiss();
              task(
                  () -> {
                    if (register) return api.signUp(fullName, address, secret);
                    api.signIn(address, secret);
                    return true;
                  },
                  value -> {
                    if (Boolean.TRUE.equals(value)) {
                      status = "Giriş Yapıldı • Ekipler Kontrol Ediliyor";
                      listTeams();
                    } else {
                      host.cloudNotice("Doğrulama e-postasını açın; ardından giriş yapın.");
                      open();
                    }
                  });
            });
  }

  private void verifyEmail(String address) {
    LinearLayout box = column();
    box.setPadding(32, 8, 32, 8);
    EditText email = input(box, "E-posta", false),
        code = input(box, "E-postadaki Doğrulama Kodu", false);
    email.setText(address);
    code.setInputType(InputType.TYPE_CLASS_NUMBER);
    AlertDialog dialog =
        new AlertDialog.Builder(activity)
            .setTitle("E-posta Adresini Doğrula")
            .setView(box)
            .setNegativeButton("Vazgeç", null)
            .setNeutralButton("Kodu Yeniden Gönder", null)
            .setPositiveButton("Doğrula", null)
            .show();
    dialog
        .getButton(-3)
        .setOnClickListener(
            v -> {
              String value = email.getText().toString().trim();
              if (!value.contains("@")) {
                email.setError("E-posta Girin");
                return;
              }
              task(
                  () -> {
                    api.resend(value);
                    return true;
                  },
                  r ->
                      host.cloudNotice(
                          "Doğrulama kodu yeniden istendi. E-posta kutunuzu kontrol edin."));
            });
    dialog
        .getButton(-1)
        .setOnClickListener(
            v -> {
              String value = email.getText().toString().trim(),
                  otp = code.getText().toString().trim();
              if (!value.contains("@") || !otp.matches("[0-9]{6,10}")) {
                code.setError("E-posta Ve Kodu Kontrol Edin");
                return;
              }
              code.setText("");
              dialog.dismiss();
              task(() -> api.verifyEmail(value, otp), r -> listTeams());
            });
  }

  private void createTeam() {
    LinearLayout box = column();
    box.setPadding(32, 8, 32, 8);
    EditText name = input(box, "Ekip / Kurum Adı", false);
    AlertDialog dialog =
        new AlertDialog.Builder(activity)
            .setTitle("İlk Yönetici • Ekip Oluştur")
            .setMessage(
                "Doğrulanmış hesabınızın kimliği ve e-postası sunucuda ilk yönetici olarak"
                    + " tanımlanmış olmalıdır.")
            .setView(box)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Oluştur", null)
            .show();
    dialog
        .getButton(-1)
        .setOnClickListener(
            v -> {
              String value = name.getText().toString().trim();
              if (value.isEmpty()) {
                name.setError("Ekip Adı Girin");
                return;
              }
              dialog.dismiss();
              task(
                  () -> api.rpc("nobet_create_team", new JSONObject().put("p_name", value)),
                  id -> selectTeam(id.toString()));
            });
  }

  private void importLocal() {
    try {
      State local = host.localState();
      new AlertDialog.Builder(activity)
          .setTitle("Yerel Listeyi Paylaş")
          .setMessage(
              local.people.size()
                  + " personel, "
                  + local.weeks.size()
                  + " kayıtlı hafta ve mazeret/tatil ayarları ekiple paylaşılacak. Telefondaki eski"
                  + " yerel dosya korunur.")
          .setNegativeButton("Vazgeç", null)
          .setPositiveButton(
              "Paylaş",
              (d, w) -> {
                if (stage(local)) {
                  host.cloudState(local);
                  host.cloudRender();
                }
              })
          .show();
    } catch (Exception e) {
      failure(e);
    }
  }

  private void discardPending() {
    new AlertDialog.Builder(activity)
        .setTitle("Sunucudaki Listeyi Al")
        .setMessage(
            "Yerel değişiklik şifreli cihaz arşivine saklanacak. Ortak listeyi değiştirmeden"
                + " sunucudaki son sürüm açılır.")
        .setNegativeButton("Vazgeç", null)
        .setPositiveButton(
            "Arşivle Ve Aç",
            (d, w) -> {
              try {
                if (pending != null)
                  vault.write(
                      "archive-" + System.currentTimeMillis(),
                      new JSONObject()
                          .put("team", team)
                          .put("revision", revision)
                          .put("payload", pending));
                pending = null;
                requestId = "";
                conflict = false;
                cache();
                task(
                    () -> api.snapshot(team),
                    value -> {
                      apply((JSONObject) value, true);
                      open();
                    });
              } catch (Exception e) {
                failure(e);
              }
            })
        .show();
  }

  private void logout() {
    if (pending != null) {
      host.cloudNotice("Çıkıştan önce bekleyen değişikliği gönderin veya arşivleyin.");
      return;
    }
    new AlertDialog.Builder(activity)
        .setTitle("Çıkış Yap")
        .setMessage("Ortak ekip bu telefonda kapanır. Eski kişisel yerel listeniz açılır.")
        .setNegativeButton("Vazgeç", null)
        .setPositiveButton(
            "Çıkış Yap",
            (d, w) -> {
              try {
                if (jobs != 0) {
                  host.cloudNotice("Devam eden sunucu işlemi bitince yeniden deneyin.");
                  return;
                }
                generation++;
                fetching = false;
                CloudApi closing = api;
                CloudApi.Session ending = api.session;
                vault.delete("session");
                api.session = null;
                worker.execute(
                    () -> {
                      try {
                        closing.revoke(ending);
                      } catch (Exception ignored) {
                      }
                    });
                team = "";
                role = "viewer";
                online = false;
                confirmed = null;
                status = "Yerel Liste • Ortak Ekip Bağlı Değil";
                host.cloudState(host.localState());
                host.cloudRender();
                open();
              } catch (Exception e) {
                failure(e);
              }
            })
        .show();
  }

  private void members() {
    task(
        () -> api.rpc("nobet_members_list", new JSONObject().put("p_team", team)),
        value -> {
          JSONObject data = (JSONObject) value;
          LinearLayout box = column();
          box.setPadding(32, 8, 32, 8);
          box.addView(button("+ Kullanıcı Davet Et", this::invite));
          JSONArray users = data.getJSONArray("members");
          for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            String id = user.getString("id"),
                email = user.getString("email"),
                role = user.getString("role");
            box.addView(
                label(
                    user.optString("name")
                        + "\n"
                        + email
                        + " • "
                        + ("admin".equals(role) ? "Yönetici" : "Kullanıcı")));
            box.addView(
                button(
                    "Yetkiyi Değiştir",
                    () ->
                        new AlertDialog.Builder(activity)
                            .setTitle(email)
                            .setSingleChoiceItems(
                                new String[] {
                                  "Kullanıcı • Yalnız Görüntüle",
                                  "Yönetici • Düzenle Ve Yönetici Ata"
                                },
                                "admin".equals(role) ? 1 : 0,
                                (d, index) -> {
                                  d.dismiss();
                                  confirmRole(
                                      "nobet_set_role",
                                      new String[] {
                                        "p_user", id, "p_role", index == 1 ? "admin" : "viewer"
                                      },
                                      "Bu hesabın yetkisi değiştirilecek.");
                                })
                            .setNegativeButton("Vazgeç", null)
                            .show()));
            box.addView(
                button(
                    "Ekipten Çıkar",
                    () ->
                        confirmRole(
                            "nobet_remove_member",
                            new String[] {"p_user", id},
                            email + " ekipten çıkarılacak.")));
          }
          JSONArray invites = data.getJSONArray("invites");
          for (int i = 0; i < invites.length(); i++) {
            JSONObject invitation = invites.getJSONObject(i);
            String email = invitation.getString("email");
            box.addView(
                label(
                    "Davet Bekliyor • "
                        + email
                        + " • "
                        + ("admin".equals(invitation.getString("role"))
                            ? "Yönetici"
                            : "Kullanıcı")));
            box.addView(
                button(
                    "Daveti İptal Et",
                    () ->
                        confirmRole(
                            "nobet_cancel_invite",
                            new String[] {"p_email", email},
                            "Bekleyen davet kaldırılacak.")));
          }
          ScrollView scroll = new ScrollView(activity);
          scroll.addView(box);
          new AlertDialog.Builder(activity)
              .setTitle("Kullanıcı Yetkileri")
              .setView(scroll)
              .setNegativeButton("Kapat", null)
              .show();
        });
  }

  private void confirmRole(String rpc, String[] args, String message) {
    new AlertDialog.Builder(activity)
        .setTitle("Yetki İşlemini Onayla")
        .setMessage(message)
        .setNegativeButton("Vazgeç", null)
        .setPositiveButton(
            "Onayla",
            (d, w) -> {
              task(
                  () -> {
                    JSONObject body = new JSONObject().put("p_team", team);
                    for (int i = 0; i < args.length; i += 2) body.put(args[i], args[i + 1]);
                    return api.rpc(rpc, body);
                  },
                  value -> {
                    host.cloudNotice("Kullanıcı yetkisi güncellendi.");
                    fetch(false);
                    members();
                  });
            })
        .show();
  }

  private void invite() {
    LinearLayout box = column();
    box.setPadding(32, 8, 32, 8);
    EditText email = input(box, "Davet Edilecek E-posta", false);
    CheckBox admin = new CheckBox(activity);
    String adminLabel = "Yönetici Olsun";
    admin.setText(adminLabel);
    box.addView(admin);
    AlertDialog dialog =
        new AlertDialog.Builder(activity)
            .setTitle("Kullanıcı Davet Et")
            .setMessage(
                "Kullanıcı aynı e-postayla hesap açıp doğruladıktan sonra Ekipleri Ve Davetleri"
                    + " Yenile'ye dokunur. Uygulama davet e-postası göndermez.")
            .setView(box)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Davet Et", null)
            .show();
    dialog
        .getButton(-1)
        .setOnClickListener(
            v -> {
              String address = email.getText().toString().trim();
              if (!address.contains("@")) {
                email.setError("E-posta Girin");
                return;
              }
              dialog.dismiss();
              String role = admin.isChecked() ? "admin" : "viewer";
              task(
                  () ->
                      api.rpc(
                          "nobet_invite",
                          new JSONObject()
                              .put("p_team", team)
                              .put("p_email", address)
                              .put("p_role", role)),
                  value -> {
                    host.cloudNotice("Davet kaydedildi.");
                    members();
                  });
            });
  }
}
