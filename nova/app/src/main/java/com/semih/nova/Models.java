package com.semih.nova;

import android.content.*;
import android.net.Uri;
import android.os.StatFs;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

final class Models {
  static final long MAX = 2L * 1024 * 1024 * 1024;
  static final String REPO = "Qwen/Qwen2.5-1.5B-Instruct-GGUF";

  interface Progress {
    void update(String text);
  }

  static File folder(Context c) {
    File dir = new File(c.getFilesDir(), "models");
    if (!dir.exists() && !dir.mkdirs())
      throw new IllegalStateException("Model klasörü oluşturulamadı.");
    return dir;
  }

  static HttpURLConnection connection(String url) throws IOException {
    URL target = new URL(url);
    for (int i = 0; i < 10; i++) {
      if (!target.getProtocol().equals("https"))
        throw new IOException("Yalnızca HTTPS bağlantısı kabul edilir.");
      HttpURLConnection con = (HttpURLConnection) target.openConnection();
      con.setConnectTimeout(20000);
      con.setReadTimeout(45000);
      con.setInstanceFollowRedirects(false);
      con.setRequestProperty("User-Agent", "NovaAdas/1.0");
      int status = con.getResponseCode();
      if (status >= 300 && status < 400) {
        String loc = con.getHeaderField("Location");
        con.disconnect();
        if (loc == null) throw new IOException("Geçersiz yönlendirme.");
        target = new URL(target, loc);
        continue;
      }
      if (status != 200) {
        con.disconnect();
        throw new IOException("İndirme sunucusu yanıtı: " + status);
      }
      return con;
    }
    throw new IOException("Çok fazla yönlendirme.");
  }

  static File download(Context c, AtomicBoolean cancel, Progress progress) throws Exception {
    return download(c, cancel, progress, false);
  }

  static File download(Context c, AtomicBoolean cancel, Progress progress, boolean voice)
      throws Exception {
    String repo = voice ? "ggerganov/whisper.cpp" : REPO;
    progress.update("Resmî model dosyası ve doğrulama özeti aranıyor…");
    HttpURLConnection meta = connection("https://huggingface.co/api/models/" + repo + "/tree/main");
    String raw;
    try (InputStream in = meta.getInputStream()) {
      raw = new String(Documents.bounded(in, 1024 * 1024), java.nio.charset.StandardCharsets.UTF_8);
    } finally {
      meta.disconnect();
    }
    JSONArray list = new JSONArray(raw);
    String path = null, sha = null;
    long size = 0;
    for (int i = 0; i < list.length(); i++) {
      JSONObject item = list.getJSONObject(i);
      String name = item.optString("path");
      if ((voice
          ? name.equals("ggml-tiny.bin")
          : name.toLowerCase(Locale.ROOT).endsWith("q4_k_m.gguf"))) {
        JSONObject lfs = item.optJSONObject("lfs");
        if (lfs != null) {
          path = name;
          sha = lfs.optString("oid");
          size = item.optLong("size");
          break;
        }
      }
    }
    if (path == null || sha == null || !sha.matches("[0-9a-f]{64}") || size <= 0 || size > MAX)
      throw new IOException(
          "Resmî Q4_K_M dosyası veya SHA-256 doğrulaması bulunamadı. Güvenilir GGUF dosyasını elle"
              + " seçebilirsin.");
    if (cancel.get()) throw new IOException("İndirme durduruldu.");
    checkSpace(c, size);
    HttpURLConnection con = connection("https://huggingface.co/" + repo + "/resolve/main/" + path);
    File part = new File(folder(c), "download-" + System.currentTimeMillis() + ".part");
    try (InputStream in = con.getInputStream()) {
      return copy(in, part, size, sha, cancel, progress, voice);
    } finally {
      con.disconnect();
    }
  }

  static File imported(Context c, Uri uri, AtomicBoolean cancel, Progress progress)
      throws Exception {
    return imported(c, uri, cancel, progress, false);
  }

  static File imported(Context c, Uri uri, AtomicBoolean cancel, Progress progress, boolean voice)
      throws Exception {
    checkSpace(c, (voice ? 80L : 1200L) * 1024 * 1024);
    File part = new File(folder(c), "import-" + System.currentTimeMillis() + ".part");
    try (InputStream in = c.getContentResolver().openInputStream(uri)) {
      if (in == null) throw new IOException("Seçilen dosya açılamadı.");
      return copy(in, part, -1, null, cancel, progress, voice);
    }
  }

  static void checkSpace(Context c, long size) throws IOException {
    if (new StatFs(folder(c).getPath()).getAvailableBytes() < size + 256L * 1024 * 1024)
      throw new IOException("Model için yeterli boş depolama yok.");
  }

  static File copy(
      InputStream in,
      File part,
      long expected,
      String sha,
      AtomicBoolean cancel,
      Progress progress,
      boolean voice)
      throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    long total = 0, last = 0;
    byte[] buffer = new byte[128 * 1024];
    try (OutputStream out = new FileOutputStream(part)) {
      int n;
      while ((n = in.read(buffer)) != -1) {
        if (cancel.get())
          throw new IOException("İşlem durduruldu. Yarım dosya açık onayla temizlenebilir.");
        total += n;
        if (total > MAX || (expected > 0 && total > expected))
          throw new IOException("Model boyut sınırı aşıldı.");
        out.write(buffer, 0, n);
        digest.update(buffer, 0, n);
        if (total - last > 8 * 1024 * 1024) {
          last = total;
          progress.update(
              String.format(
                  Locale.ROOT,
                  "Model hazırlanıyor · %d MB%s",
                  total / 1048576,
                  expected > 0 ? " / " + expected / 1048576 + " MB" : ""));
        }
      }
    }
    if (expected > 0 && total != expected) throw new IOException("Model eksik indirildi.");
    if (total < 64) throw new IOException("Dosya geçerli bir GGUF modeli değil.");
    try (InputStream header = new FileInputStream(part)) {
      int a = header.read(), b = header.read(), d = header.read(), f = header.read();
      if (voice
          ? !(a == 0x6c && b == 0x6d && d == 0x67 && f == 0x67)
          : !(a == 'G' && b == 'G' && d == 'U' && f == 'F'))
        throw new IOException("Model biçimi seçilen motorla uyumlu değil.");
    }
    StringBuilder actual = new StringBuilder();
    for (byte b : digest.digest()) actual.append(String.format(Locale.ROOT, "%02x", b & 255));
    if (sha != null && !sha.equals(actual.toString()))
      throw new IOException("Model SHA-256 doğrulaması başarısız. Dosya kullanılmayacak.");
    File ready =
        new File(part.getParentFile(), part.getName().replace(".part", voice ? ".bin" : ".gguf"));
    if (!part.renameTo(ready)) throw new IOException("Model dosyası kaydedilemedi.");
    progress.update(
        sha != null
            ? "Resmî model SHA-256 ile doğrulandı."
            : "GGUF kopyalandı. Kaynak/lisans kullanıcı tarafından doğrulanmalıdır.");
    return ready;
  }
}
