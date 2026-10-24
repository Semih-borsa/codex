package com.semih.portfoyum;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.webkit.JavaScriptReplyProxy;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public final class MainActivity extends Activity {
    private static final String ORIGIN = "https://appassets.androidplatform.net";
    private static final String START = ORIGIN + "/assets/index.html";
    private static final int CREATE_FILE = 81, OPEN_FILE = 82;
    private static final int MAX_FILE_BYTES = 15_000_000, MAX_RESPONSE_BYTES = 8_000_000;
    private final ExecutorService network = Executors.newFixedThreadPool(3);
    private final ExecutorService files = Executors.newSingleThreadExecutor();
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    private WebView webView;
    private volatile String networkUserAgent = "PortfoyumAndroid/4.8.0";
    private PendingFile pendingFile;
    private OnBackInvokedCallback backCallback;

    private static final class PendingFile {
        final String requestId, text;
        final JavaScriptReplyProxy reply;
        PendingFile(String requestId, String text, JavaScriptReplyProxy reply) {
            this.requestId = requestId; this.text = text; this.reply = reply;
        }
    }

    @Override @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(244, 246, 250));
        setContentView(root);
        configureInsets(root);

        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        webView.setBackgroundColor(Color.rgb(244, 246, 250));
        WebSettings settings = webView.getSettings();
        networkUserAgent = settings.getUserAgentString();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setBuiltInZoomControls(false);
        settings.setTextZoom(100);
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        webView.setWebChromeClient(new WebChromeClient());

        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (!isBundledUrl(request.getUrl())) return blocked();
                WebResourceResponse response = loader.shouldInterceptRequest(request.getUrl());
                if (response == null) return blocked();
                // .mjs MIME sniffing varies across WebView versions; set it explicitly.
                if (request.getUrl().getPath().endsWith(".mjs")) response.setMimeType("text/javascript");
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'");
                headers.put("X-Content-Type-Options", "nosniff");
                headers.put("Cache-Control", "no-store");
                response.setResponseHeaders(headers);
                return response;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !isBundledUrl(request.getUrl());
            }
            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                root.removeView(view);
                view.destroy();
                webView = null;
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Ekran yeniden açılmalı")
                        .setMessage("Android WebView durdu. Kayıtlarınızı silmeden uygulamayı yeniden açabilirsiniz.")
                        .setPositiveButton("Yeniden aç", (d, w) -> recreate())
                        .setCancelable(false).show();
                return true;
            }
        });

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            new AlertDialog.Builder(this).setTitle("Android WebView güncellenmeli")
                    .setMessage("Güvenli telefon bağlantısı bu WebView sürümünde yok. Android System WebView veya Chrome uygulamasını güncelledikten sonra tekrar açın.")
                    .setPositiveButton("Kapat", (d, w) -> finish()).setCancelable(false).show();
            return;
        }
        // Bridge is injected only for this local origin and only accepts main-frame messages.
        WebViewCompat.addWebMessageListener(webView, "PortfoyumNative", Collections.singleton(ORIGIN),
                (view, message, sourceOrigin, isMainFrame, replyProxy) -> {
                    if (!isMainFrame || !isBundledOrigin(sourceOrigin)) return;
                    String raw = message.getData();
                    if (raw == null || raw.length() > 16_500_000) return;
                    handleMessage(raw, replyProxy);
                });

        if (Build.VERSION.SDK_INT >= 33) {
            backCallback = this::handleBack;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        }
        webView.loadUrl(START);
    }

    private boolean isBundledOrigin(Uri uri) {
        return uri != null && "https".equals(uri.getScheme())
                && "appassets.androidplatform.net".equals(uri.getHost())
                && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443);
    }

    private boolean isBundledUrl(Uri uri) {
        return isBundledOrigin(uri) && uri.getPath() != null
                && uri.getPath().startsWith("/assets/") && !uri.getPath().contains("..");
    }

    private WebResourceResponse blocked() {
        return new WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden",
                Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
    }

    private void configureInsets(FrameLayout root) {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                Insets keyboard = insets.getInsets(WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
                return WindowInsets.CONSUMED;
            }
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        root.requestApplyInsets();
    }

    private void handleMessage(String raw, JavaScriptReplyProxy proxy) {
        String id = "";
        try {
            JSONObject request = new JSONObject(raw);
            id = request.getString("id");
            if (!id.matches("[A-Za-z0-9-]{1,80}")) return;
            String action = request.getString("action");
            JSONObject args = request.optJSONObject("args");
            if (args == null) args = new JSONObject();
            final String requestId = id;
            final JSONObject arguments = args;
            switch (action) {
                case "http":
                    network.execute(() -> {
                        try { respond(proxy, requestId, http(arguments), null); }
                        catch (Exception e) { respond(proxy, requestId, null, readableError(e)); }
                    });
                    break;
                case "saveFile": createFile(id, args, proxy); break;
                case "openFile": openFile(id, args, proxy); break;
                default: respond(proxy, id, null, "Desteklenmeyen telefon işlemi.");
            }
        } catch (Exception e) { if (!id.isEmpty()) respond(proxy, id, null, readableError(e)); }
    }

    private JSONObject http(JSONObject args) throws Exception {
        String method = args.optString("method", "GET").toUpperCase(Locale.ROOT);
        URI uri = NetworkPolicy.validate(args.getString("url"), method);
        String body = args.optString("body", "");
        if (body.length() > 4096) throw new IllegalArgumentException("Fiyat isteği fazla büyük.");
        JSONObject inputHeaders = args.optJSONObject("headers");
        for (int redirects = 0; redirects <= 3; redirects++) {
            HttpsURLConnection connection = (HttpsURLConnection) uri.toURL().openConnection();
            try {
                connection.setRequestMethod(method);
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept-Encoding", "identity");
                // Resmi fiyat sayfaları Android tarayıcı trafiği beklediğinden cihazın
                // güncel System WebView kullanıcı aracısı kullanılır.
                connection.setRequestProperty("User-Agent", networkUserAgent);
                connection.setRequestProperty("Accept-Language", "tr-TR,tr;q=0.9,en;q=0.6");
                for (Map.Entry<String, List<String>> entry : cookies.get(uri, Collections.emptyMap()).entrySet()) {
                    if (entry.getKey() != null && !entry.getValue().isEmpty())
                        connection.setRequestProperty(entry.getKey(), String.join("; ", entry.getValue()));
                }
                if (inputHeaders != null) {
                    for (String name : new String[]{"Accept", "Content-Type", "Referer", "Origin", "X-Requested-With"}) {
                        String value = inputHeaders.optString(name, "");
                        if (!value.isEmpty() && value.length() < 512 && !value.contains("\r") && !value.contains("\n"))
                            connection.setRequestProperty(name, value);
                    }
                }
                if ("POST".equals(method)) {
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    connection.setDoOutput(true);
                    connection.setFixedLengthStreamingMode(bytes.length);
                    try (OutputStream stream = connection.getOutputStream()) { stream.write(bytes); }
                }
                int status = connection.getResponseCode();
                cookies.put(uri, connection.getHeaderFields());
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (!"GET".equals(method) || location == null || location.length() > 2048)
                        return new JSONObject().put("status", status).put("body", "");
                    uri = NetworkPolicy.validate(uri.resolve(location).toString(), "GET");
                    continue;
                }
                if (status < 200 || status >= 300)
                    return new JSONObject().put("status", status).put("body", "");
                byte[] bytes;
                try (InputStream stream = connection.getInputStream()) { bytes = readLimited(stream, MAX_RESPONSE_BYTES); }
                return new JSONObject().put("status", status).put("body", new String(bytes, StandardCharsets.UTF_8));
            } finally { connection.disconnect(); }
        }
        return new JSONObject().put("status", 310).put("body", "");
    }

    private void createFile(String id, JSONObject args, JavaScriptReplyProxy proxy) throws JSONException {
        if (pendingFile != null) { respond(proxy, id, null, "Önce açık dosya seçicisini tamamlayın."); return; }
        String text = args.getString("text");
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_FILE_BYTES) {
            respond(proxy, id, null, "Yedek en fazla 15 MB olabilir."); return;
        }
        String mime = args.optString("mimeType", "application/json");
        if (!mime.equals("application/json") && !mime.equals("text/csv") && !mime.equals("text/plain"))
            mime = "application/json";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType(mime).putExtra(Intent.EXTRA_TITLE, NetworkPolicy.safeFileName(args.optString("name")));
        pendingFile = new PendingFile(id, text, proxy);
        try { startActivityForResult(intent, CREATE_FILE); }
        catch (RuntimeException e) { pendingFile = null; respond(proxy, id, null, "Android dosya seçicisi açılamadı."); }
    }

    private void openFile(String id, JSONObject args, JavaScriptReplyProxy proxy) {
        if (pendingFile != null) { respond(proxy, id, null, "Önce açık dosya seçicisini tamamlayın."); return; }
        // Some document providers report JSON/CSV as octet-stream; validate content after selection.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        pendingFile = new PendingFile(id, null, proxy);
        try { startActivityForResult(intent, OPEN_FILE); }
        catch (RuntimeException e) { pendingFile = null; respond(proxy, id, null, "Android dosya seçicisi açılamadı."); }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CREATE_FILE && requestCode != OPEN_FILE) return;
        PendingFile pending = pendingFile;
        pendingFile = null;
        if (pending == null) {
            Toast.makeText(this, "Dosya işlemi tamamlanmadı; yeniden deneyin.", Toast.LENGTH_LONG).show(); return;
        }
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            respond(pending.reply, pending.requestId, null, "Dosya işlemi iptal edildi. Kayıtlar değiştirilmedi."); return;
        }
        Uri uri = data.getData();
        if (!"content".equals(uri.getScheme())) {
            respond(pending.reply, pending.requestId, null, "Geçersiz belge adresi."); return;
        }
        files.execute(() -> {
            try {
                JSONObject result = new JSONObject();
                if (requestCode == CREATE_FILE) {
                    try (OutputStream stream = getContentResolver().openOutputStream(uri, "w")) {
                        if (stream == null) throw new IllegalStateException("Dosya yazılamadı.");
                        stream.write(pending.text.getBytes(StandardCharsets.UTF_8)); stream.flush();
                    }
                    result.put("saved", true);
                } else {
                    byte[] bytes;
                    try (InputStream stream = getContentResolver().openInputStream(uri)) {
                        if (stream == null) throw new IllegalStateException("Dosya okunamadı.");
                        bytes = readLimited(stream, MAX_FILE_BYTES);
                    }
                    result.put("text", new String(bytes, StandardCharsets.UTF_8));
                }
                respond(pending.reply, pending.requestId, result, null);
            } catch (Exception e) { respond(pending.reply, pending.requestId, null, readableError(e)); }
        });
    }

    private byte[] readLimited(InputStream stream, int max) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        long deadline = System.nanoTime() + 30_000_000_000L;
        while ((read = stream.read(chunk)) != -1) {
            if (buffer.size() + read > max) throw new IllegalArgumentException("Dosya veya yanıt boyutu sınırı aşıldı.");
            if (System.nanoTime() > deadline) throw new java.net.SocketTimeoutException();
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private String readableError(Exception e) {
        if (e instanceof java.net.SocketTimeoutException) return "Kaynak zaman aşımına uğradı; eski fiyatlar korundu.";
        if (e instanceof java.net.UnknownHostException) return "İnternet veya fiyat kaynağına erişilemiyor.";
        if (e instanceof javax.net.ssl.SSLException) return "Kaynağın güvenli bağlantısı kurulamadı.";
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) return e.getMessage();
        return "İşlem tamamlanamadı. Bağlantıyı veya dosyayı kontrol edin.";
    }

    private void respond(JavaScriptReplyProxy proxy, String id, JSONObject result, String error) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed() || webView == null) return;
            try {
                JSONObject response = new JSONObject().put("id", id).put("ok", error == null);
                if (error == null) response.put("result", result == null ? new JSONObject() : result);
                else response.put("error", error);
                proxy.postMessage(response.toString());
            } catch (RuntimeException | JSONException ignored) {
                // The page may have been destroyed while a read-only request was in flight.
            }
        });
    }

    private void handleBack() {
        if (webView == null) { moveTaskToBack(true); return; }
        webView.evaluateJavascript("window.PortfoyumAppBack ? window.PortfoyumAppBack() : false", result -> {
            if (!"true".equals(result)) moveTaskToBack(true);
        });
    }

    @Override @SuppressWarnings("deprecation") public void onBackPressed() { handleBack(); }

    @Override protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null)
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        network.shutdownNow(); files.shutdownNow();
        if (webView != null) { webView.destroy(); webView = null; }
        pendingFile = null;
        super.onDestroy();
    }
}
