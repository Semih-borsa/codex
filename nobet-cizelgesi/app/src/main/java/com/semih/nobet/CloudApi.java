package com.semih.nobet;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.json.*;

/** Supabase Auth + guarded Postgres RPCs. No service key, password storage or HTTP redirects. */
final class CloudApi {
  static final class Config {
    final String url, key;

    Config(String url, String key) throws Exception {
      URI uri = new URI(url.trim());
      if (!"https".equals(uri.getScheme())
          || uri.getHost() == null
          || !uri.getHost().matches("[a-z0-9-]+\\.supabase\\.co")
          || uri.getPort() != -1
          || uri.getUserInfo() != null
          || uri.getQuery() != null
          || uri.getFragment() != null
          || !(uri.getPath().isEmpty() || "/".equals(uri.getPath())))
        throw new Exception("https://PROJE.supabase.co biçiminde Project URL girin.");
      String publicKey = key.trim();
      boolean allowed = publicKey.startsWith("sb_publishable_") && publicKey.length() > 25;
      if (!allowed && publicKey.split("\\.").length == 3) {
        try {
          allowed =
              "anon"
                  .equals(
                      new JSONObject(
                              new String(
                                  Base64.getUrlDecoder().decode(publicKey.split("\\.")[1]),
                                  StandardCharsets.UTF_8))
                          .optString("role"));
        } catch (Exception ignored) {
          allowed = false;
        }
      }
      if (!allowed || publicKey.length() > 4096)
        throw new Exception("Yalnız public/publishable veya anon anahtarını girin.");
      this.url = "https://" + uri.getHost();
      this.key = publicKey;
    }
  }

  static final class Session {
    final String access, refresh, user, email;
    final long expires;

    Session(JSONObject json, Session previous) throws JSONException {
      access = json.getString("access_token");
      refresh = json.getString("refresh_token");
      expires =
          json.optLong(
              "expires_at", System.currentTimeMillis() / 1000 + json.optLong("expires_in", 3600));
      JSONObject who = json.optJSONObject("user");
      user = who == null ? previous.user : who.getString("id");
      email = who == null ? previous.email : who.optString("email");
      if (access.isEmpty() || refresh.isEmpty() || !user.matches("[0-9a-fA-F-]{36}"))
        throw new JSONException("Geçersiz oturum");
    }

    JSONObject json() throws JSONException {
      return new JSONObject()
          .put("access_token", access)
          .put("refresh_token", refresh)
          .put("expires_at", expires)
          .put("user", new JSONObject().put("id", user).put("email", email));
    }
  }

  static final class Failure extends Exception {
    final String kind;

    Failure(String kind, String message) {
      super(message);
      this.kind = kind;
    }
  }

  static final class Response {
    final int code;
    final String body;

    Response(int code, String body) {
      this.code = code;
      this.body = body;
    }
  }

  interface Transport {
    Response post(String path, String token, JSONObject body) throws Exception;
  }

  interface SessionChanged {
    void save(Session session) throws Exception;
  }

  final Config config;
  final Transport transport;
  Session session;
  SessionChanged changed;

  CloudApi(Config config, Session session) {
    this(config, session, null);
  }

  CloudApi(Config config, Session session, Transport transport) {
    this.config = config;
    this.session = session;
    this.transport = transport == null ? this::http : transport;
  }

  private Response http(String path, String token, JSONObject body) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(config.url + path).openConnection();
    connection.setInstanceFollowRedirects(false);
    connection.setConnectTimeout(10000);
    connection.setReadTimeout(15000);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("apikey", config.key);
    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
    if (token != null) connection.setRequestProperty("Authorization", "Bearer " + token);
    connection.setDoOutput(true);
    try {
      byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
      connection.setFixedLengthStreamingMode(payload.length);
      try (OutputStream out = connection.getOutputStream()) {
        out.write(payload);
      }
      int code = connection.getResponseCode();
      InputStream stream =
          code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
      ByteArrayOutputStream data = new ByteArrayOutputStream();
      if (stream != null)
        try (InputStream in = stream) {
          byte[] buffer = new byte[8192];
          int read;
          while ((read = in.read(buffer)) != -1) {
            if (data.size() + read > 4194304) throw new IOException("Yanıt çok büyük");
            data.write(buffer, 0, read);
          }
        }
      return new Response(code, data.toString("UTF-8"));
    } finally {
      connection.disconnect();
    }
  }

  private Response request(String path, String token, JSONObject body) throws Exception {
    try {
      return transport.post(path, token, body);
    } catch (Failure e) {
      throw e;
    } catch (Exception e) {
      throw new Failure(
          "network",
          "Sunucuya bağlanılamadı. İnternet bağlantısını ve proje ayarlarını kontrol edin.");
    }
  }

  private static Failure error(Response r) {
    String body = r.body;
    if (body.contains("email_address_not_authorized"))
      return new Failure(
          "setup",
          "E-posta gönderimi için Supabase SMTP ayarını tamamlayın veya izin verilen e-posta"
              + " adresini kullanın.");
    if (body.contains("otp_expired") || body.contains("otp_disabled"))
      return new Failure("auth", "Doğrulama kodu geçersiz veya süresi dolmuş. Yeni kod isteyin.");
    if (body.contains("weak_password"))
      return new Failure(
          "auth", "Şifre sunucunun güvenlik koşullarını karşılamıyor. Daha güçlü bir şifre seçin.");
    if (body.contains("NOBET_CONFLICT"))
      return new Failure(
          "conflict",
          "Başka bir yönetici daha yeni bir sürüm kaydetti. Yerel değişiklik sunucuya yazılmadı.");
    if (body.contains("NOBET_LAST_ADMIN"))
      return new Failure("role", "Son yönetici kaldırılamaz veya kullanıcıya dönüştürülemez.");
    if (body.contains("NOBET_OWNER_REQUIRED"))
      return new Failure(
          "owner",
          "İlk yöneticinin doğrulanmış hesap kimliği ve e-postası sunucudaki kurulum kaydıyla aynı"
              + " olmalıdır.");
    if (body.contains("NOBET_ALREADY_MEMBER"))
      return new Failure(
          "member", "Bu hesap zaten ekip üyesi. Yetkisini kullanıcı listesinden değiştirin.");
    if (body.contains("NOBET_EMAIL_UNVERIFIED") || body.contains("email_not_confirmed"))
      return new Failure("auth", "Önce e-posta adresinizi doğrulayın.");
    if (body.contains("NOBET_FORBIDDEN") || r.code == 403)
      return new Failure("forbidden", "Bu işlem için ekip erişiminiz veya yönetici yetkiniz yok.");
    if (r.code == 401 || body.contains("invalid_credentials"))
      return new Failure(
          "auth", "Oturum geçersiz veya giriş bilgileri hatalı. Yeniden giriş yapın.");
    if (r.code == 404 || body.contains("PGRST202"))
      return new Failure("setup", "Sunucu fonksiyonları bulunamadı. SQL kurulumunu tamamlayın.");
    if (r.code == 429)
      return new Failure("limit", "Çok sık deneme yapıldı. Biraz sonra yeniden deneyin.");
    return new Failure(
        "server", "İşlem sunucuda tamamlanamadı. Giriş, kurulum ve yetkilerinizi kontrol edin.");
  }

  Session signIn(String email, String password) throws Exception {
    Response response =
        request(
            "/auth/v1/token?grant_type=password",
            null,
            new JSONObject().put("email", email.trim()).put("password", password));
    if (response.code < 200 || response.code >= 300) throw error(response);
    session = new Session(new JSONObject(response.body), null);
    if (changed != null) changed.save(session);
    return session;
  }

  boolean signUp(String name, String email, String password) throws Exception {
    Response response =
        request(
            "/auth/v1/signup",
            null,
            new JSONObject()
                .put("email", email.trim())
                .put("password", password)
                .put("data", new JSONObject().put("full_name", name.trim())));
    if (response.code < 200 || response.code >= 300) throw error(response);
    JSONObject result = new JSONObject(response.body);
    if (result.has("access_token") && !result.isNull("access_token")) {
      session = new Session(result, null);
      if (changed != null) changed.save(session);
      return true;
    }
    return false;
  }

  Session verifyEmail(String email, String code) throws Exception {
    Response response =
        request(
            "/auth/v1/verify",
            null,
            new JSONObject()
                .put("email", email.trim())
                .put("token", code.trim())
                .put("type", "signup"));
    if (response.code < 200 || response.code >= 300) throw error(response);
    session = new Session(new JSONObject(response.body), null);
    if (changed != null) changed.save(session);
    return session;
  }

  void resend(String email) throws Exception {
    Response response =
        request(
            "/auth/v1/resend",
            null,
            new JSONObject().put("email", email.trim()).put("type", "signup"));
    if (response.code < 200 || response.code >= 300) throw error(response);
  }

  void revoke(Session ending) throws Exception {
    Response response = request("/auth/v1/logout?scope=local", ending.access, new JSONObject());
    if (response.code < 200 || response.code >= 300) throw error(response);
  }

  private void refresh() throws Exception {
    if (session == null) throw new Failure("auth", "Giriş yapın.");
    Response response =
        request(
            "/auth/v1/token?grant_type=refresh_token",
            null,
            new JSONObject().put("refresh_token", session.refresh));
    if (response.code < 200 || response.code >= 300) throw error(response);
    session = new Session(new JSONObject(response.body), session);
    if (changed != null) changed.save(session);
  }

  Object rpc(String name, JSONObject args) throws Exception {
    if (session == null) throw new Failure("auth", "Giriş yapın.");
    if (session.expires < System.currentTimeMillis() / 1000 + 30) refresh();
    Response response = request("/rest/v1/rpc/" + name, session.access, args);
    if (response.code == 401) {
      refresh();
      response = request("/rest/v1/rpc/" + name, session.access, args);
    }
    if (response.code < 200 || response.code >= 300) throw error(response);
    if (response.body.trim().isEmpty()) return JSONObject.NULL;
    return new JSONTokener(response.body).nextValue();
  }

  JSONObject head(String team) throws Exception {
    return (JSONObject) rpc("nobet_head", new JSONObject().put("p_team", team));
  }

  JSONObject snapshot(String team) throws Exception {
    return (JSONObject) rpc("nobet_snapshot", new JSONObject().put("p_team", team));
  }

  JSONObject publish(String team, long expected, JSONObject state, String request)
      throws Exception {
    return (JSONObject)
        rpc(
            "nobet_publish",
            new JSONObject()
                .put("p_team", team)
                .put("p_expected", expected)
                .put("p_payload", state)
                .put("p_request", request));
  }
}
