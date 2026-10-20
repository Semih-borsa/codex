package com.semih.izin360;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintManager;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class MainActivity extends Activity {
  static final String ORIGIN = "https://app.izin360.local";
  WebView web;
  ValueCallback<Uri[]> fileCallback;
  String pendingExport;
  final ExecutorService executor = Executors.newFixedThreadPool(2);

  @SuppressLint(
      "SetJavaScriptEnabled") // Only bundled, intercepted HTTPS assets are loaded; outside
                              // documents are blocked.
  public void onCreate(Bundle state) {
    super.onCreate(state);
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setFitsSystemWindows(true);
    layout.setBackgroundColor(0xff152135);
    web = new WebView(this);
    WebView.setWebContentsDebuggingEnabled(
        (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0);
    web.setBackgroundColor(0xfff5f7fc);
    layout.addView(web, new LinearLayout.LayoutParams(-1, -1));
    setContentView(layout);
    WebSettings settings = web.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(false);
    settings.setAllowContentAccess(true);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    settings.setMediaPlaybackRequiresUserGesture(true);
    android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
    web.addJavascriptInterface(new Bridge(), "Android");
    web.setWebViewClient(
        new WebViewClient() {
          public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return !request.getUrl().toString().startsWith(ORIGIN + "/");
          }

          public WebResourceResponse shouldInterceptRequest(
              WebView view, WebResourceRequest request) {
            Uri u = request.getUrl();
            String path = u.getPath();
            if (!"app.izin360.local".equals(u.getHost())
                || !"https".equals(u.getScheme())
                || path == null
                || path.contains(".."))
              return response(403, "Forbidden", new byte[0], "text/plain");
            if (path.equals("/")) path = "/index.html";
            String mime =
                path.endsWith(".js")
                    ? "text/javascript"
                    : path.endsWith(".css")
                        ? "text/css"
                        : path.endsWith(".svg")
                            ? "image/svg+xml"
                            : path.endsWith(".webmanifest")
                                ? "application/manifest+json"
                                : path.endsWith(".html") ? "text/html" : "application/octet-stream";
            try (InputStream in = getAssets().open(path.substring(1))) {
              return response(200, "OK", read(in, 3 * 1024 * 1024), mime);
            } catch (Exception e) {
              return response(404, "Not Found", new byte[0], "text/plain");
            }
          }

          public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
            if (req.isForMainFrame())
              Toast.makeText(
                      MainActivity.this,
                      "Uygulama ekranı açılamadı. Android System WebView güncellemesini kontrol"
                          + " et.",
                      Toast.LENGTH_LONG)
                  .show();
          }
        });
    web.setWebChromeClient(
        new WebChromeClient() {
          public boolean onShowFileChooser(
              WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (fileCallback != null) fileCallback.onReceiveValue(null);
            fileCallback = callback;
            try {
              startActivityForResult(
                  new Intent(Intent.ACTION_OPEN_DOCUMENT)
                      .addCategory(Intent.CATEGORY_OPENABLE)
                      .setType("application/json"),
                  11);
            } catch (Exception e) {
              fileCallback.onReceiveValue(null);
              fileCallback = null;
            }
            return true;
          }
        });
    web.loadUrl(ORIGIN + "/");
  }

  WebResourceResponse response(int code, String reason, byte[] bytes, String mime) {
    Map<String, String> headers = new HashMap<>();
    headers.put(
        "Content-Security-Policy",
        "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self'"
            + " data:; connect-src 'none'; object-src 'none'");
    headers.put("X-Content-Type-Options", "nosniff");
    return new WebResourceResponse(
        mime, "UTF-8", code, reason, headers, new ByteArrayInputStream(bytes));
  }

  static byte[] read(InputStream in, int max) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int n;
    while ((n = in.read(buffer)) != -1) {
      if (out.size() + n > max) throw new IOException("Boyut sınırı aşıldı");
      out.write(buffer, 0, n);
    }
    return out.toByteArray();
  }

  void reply(String key, int status, JSONObject body) {
    JSONObject result = new JSONObject();
    try {
      result.put("status", status).put("body", body);
    } catch (JSONException ignored) {
    }
    runOnUiThread(
        () -> {
          if (!isFinishing() && !isDestroyed())
            web.evaluateJavascript(
                "window.__nativeResult(" + JSONObject.quote(key) + "," + result + ")", null);
        });
  }

  public final class Bridge {
    @JavascriptInterface
    public String status() {
      JSONObject o = new JSONObject();
      try {
        android.content.SharedPreferences p = getSharedPreferences("connection", 0);
        o.put("url", p.getString("url", "")).put("hasToken", !p.getString("key", "").isEmpty());
      } catch (Exception ignored) {
      }
      return o.toString();
    }

    @JavascriptInterface
    public String saveConnection(String address, String key) {
      JSONObject result = new JSONObject();
      try {
        String url = Endpoint.validate(address);
        key = key.trim();
        if (key.length() > 512 || key.contains("\r") || key.contains("\n"))
          throw new IllegalArgumentException("Bağlantı anahtarı geçersiz.");
        android.content.SharedPreferences p = getSharedPreferences("connection", 0);
        String saved = p.getString("key", "");
        if (!url.equals(p.getString("url", "")) && key.isEmpty()) saved = "";
        if (!key.isEmpty()) saved = Vault.encrypt(key);
        if (!p.edit().putString("url", url).putString("key", saved).commit())
          throw new IOException();
        result.put("ok", true);
      } catch (Exception e) {
        try {
          result
              .put("ok", false)
              .put(
                  "error",
                  e instanceof IllegalArgumentException
                      ? e.getMessage()
                      : "Bağlantı ayarları güvenli kaydedilemedi.");
        } catch (Exception ignored) {
        }
      }
      return result.toString();
    }

    @JavascriptInterface
    public void clearConnection() {
      getSharedPreferences("connection", 0).edit().clear().apply();
    }

    @JavascriptInterface
    public void request(String key, String path, String method, String body) {
      if (!Arrays.asList("/api/health", "/api/sync", "/api/pairing").contains(path)
          || !Arrays.asList("GET", "POST").contains(method)
          || body.length() > 10 * 1024 * 1024) {
        reply(key, 400, error("Geçersiz istek."));
        return;
      }
      executor.execute(
          () -> {
            HttpURLConnection connection = null;
            try {
              android.content.SharedPreferences pref = getSharedPreferences("connection", 0);
              String root = Endpoint.validate(pref.getString("url", ""));
              if (root.isEmpty())
                throw new IllegalArgumentException("Önce Windows bağlantısını yapılandır.");
              connection = (HttpURLConnection) new URL(root + path).openConnection();
              connection.setInstanceFollowRedirects(false);
              connection.setConnectTimeout(8000);
              connection.setReadTimeout(12000);
              connection.setRequestMethod(method);
              connection.setRequestProperty("Accept", "application/json");
              String token = Vault.decrypt(pref.getString("key", ""));
              if (!token.isEmpty())
                connection.setRequestProperty("Authorization", "Bearer " + token);
              if (method.equals("POST")) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                try (OutputStream out = connection.getOutputStream()) {
                  out.write(body.getBytes(StandardCharsets.UTF_8));
                }
              }
              int status = connection.getResponseCode();
              if (status >= 300 && status < 400)
                throw new IllegalArgumentException(
                    "Sunucu yönlendirme döndürdü. Doğrudan bağlantı adresini gir.");
              InputStream stream =
                  status >= 400 ? connection.getErrorStream() : connection.getInputStream();
              if (stream == null) throw new IOException();
              try (InputStream in = stream) {
                reply(
                    key,
                    status,
                    new JSONObject(new String(read(in, 10 * 1024 * 1024), StandardCharsets.UTF_8)));
              }
            } catch (Exception e) {
              reply(
                  key,
                  0,
                  error(
                      e instanceof IllegalArgumentException
                          ? e.getMessage()
                          : "Windows bilgisayara ulaşılamadı. Aynı Wi-Fi ağı ve bağlantı adresini"
                                + " kontrol et. Kayıtların cihazda korunuyor."));
            } finally {
              if (connection != null) connection.disconnect();
            }
          });
    }

    @JavascriptInterface
    public void saveText(String name, String text, String mime) {
      if (text.length() > 10 * 1024 * 1024) return;
      runOnUiThread(
          () -> {
            if (pendingExport != null) {
              Toast.makeText(
                      MainActivity.this,
                      "Önce açık dosya kaydetme işlemini tamamla.",
                      Toast.LENGTH_SHORT)
                  .show();
              return;
            }
            pendingExport = text;
            try {
              startActivityForResult(
                  new Intent(Intent.ACTION_CREATE_DOCUMENT)
                      .addCategory(Intent.CATEGORY_OPENABLE)
                      .setType(
                          mime.startsWith("application/json") ? "application/json" : "text/csv")
                      .putExtra(Intent.EXTRA_TITLE, name.replaceAll("[^a-zA-Z0-9._-]", "_")),
                  12);
            } catch (Exception e) {
              pendingExport = null;
              Toast.makeText(MainActivity.this, "Dosya konumu seçilemedi.", Toast.LENGTH_LONG)
                  .show();
            }
          });
    }

    @JavascriptInterface
    public void printPage() {
      runOnUiThread(
          () -> {
            PrintManager manager = (PrintManager) getSystemService(PRINT_SERVICE);
            manager.print("Izin360-Izin-Formu", web.createPrintDocumentAdapter("Izin360"), null);
          });
    }
  }

  JSONObject error(String message) {
    JSONObject o = new JSONObject();
    try {
      o.put("error", message);
    } catch (Exception ignored) {
    }
    return o;
  }

  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request == 11) {
      if (fileCallback != null) {
        fileCallback.onReceiveValue(
            result == RESULT_OK && data != null && data.getData() != null
                ? new Uri[] {data.getData()}
                : null);
        fileCallback = null;
      }
      return;
    }
    if (request == 12) {
      String text = pendingExport;
      pendingExport = null;
      if (result == RESULT_OK && data != null && data.getData() != null && text != null) {
        try (OutputStream out = getContentResolver().openOutputStream(data.getData(), "wt")) {
          if (out == null) throw new IOException();
          out.write(text.getBytes(StandardCharsets.UTF_8));
          Toast.makeText(this, "Dosya kaydedildi.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
          Toast.makeText(
                  this, "Dosya kaydedilemedi. Depolama erişimini kontrol et.", Toast.LENGTH_LONG)
              .show();
        }
      }
    }
  }

  @Override
  public void onBackPressed() {
    web.evaluateJavascript(
        "window.__izinBack ? window.__izinBack() : false",
        result -> {
          if (!"true".equals(result))
            new AlertDialog.Builder(this)
                .setTitle("İzin360 kapatılsın mı?")
                .setMessage("Kayıtların cihazında korunur.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Kapat", (d, w) -> finish())
                .show();
        });
  }

  protected void onDestroy() {
    if (fileCallback != null) fileCallback.onReceiveValue(null);
    executor.shutdownNow();
    web.removeJavascriptInterface("Android");
    web.destroy();
    super.onDestroy();
  }
}
