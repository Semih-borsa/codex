package com.butcetakip.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Android Storage Access Framework: no broad storage permission or extra dependency. */
@CapacitorPlugin(name = "FinansBackup")
public class FinansBackupPlugin extends Plugin {
    private final AtomicBoolean pending = new AtomicBoolean(false);

    @PluginMethod
    public void save(PluginCall call) {
        String json = call.getString("json");
        String fileName = call.getString("fileName");
        try {
            JSONObject envelope = new JSONObject(json == null ? "" : json);
            String format = envelope.optString("format");
            if (!("finora-v2".equals(format) || "finora-v1".equals(format) || "butce-takip-v1".equals(format))
                || !(envelope.opt("data") instanceof JSONObject)) throw new IllegalArgumentException();
            if (fileName == null || !fileName.matches("[A-Za-z0-9_-]+\\.json")) throw new IllegalArgumentException();
        } catch (Exception ex) {
            call.reject("Yedek içeriği veya dosya adı geçersiz; kayıtlar değiştirilmedi.", "INVALID_BACKUP");
            return;
        }
        if (!pending.compareAndSet(false, true)) {
            call.reject("Bir yedek kaydetme işlemi zaten açık.", "EXPORT_BUSY");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getBridge().executeOnMainThread(() -> {
            try { startActivityForResult(call, intent, "documentCreated"); }
            catch (Exception ex) {
                pending.set(false);
                call.reject("Sistem dosya kaydetme ekranı açılamadı. Dosyalar uygulamasını kontrol et.", "PICKER_UNAVAILABLE");
            }
        });
    }

    // Do not put a potentially large financial JSON document in Android's limited state Bundle.
    // Interrupted process recreation must not silently save an empty/partial backup.
    @Override
    protected Bundle saveInstanceState() { return new Bundle(); }

    @ActivityCallback
    private void documentCreated(PluginCall call, ActivityResult result) {
        if (call == null) { pending.set(false); return; }
        if (result.getResultCode() == Activity.RESULT_CANCELED) {
            pending.set(false);
            JSObject response = new JSObject();
            response.put("status", "cancelled");
            call.resolve(response);
            return;
        }
        Uri uri = result.getData() == null ? null : result.getData().getData();
        String json = call.getString("json");
        if (result.getResultCode() != Activity.RESULT_OK || uri == null || !"content".equals(uri.getScheme()) || json == null || json.isEmpty()) {
            pending.set(false);
            call.reject("Dosya konumu alınamadı veya işlem kesintiye uğradı. Yedek Al ile yeniden dene.", "INVALID_DOCUMENT");
            return;
        }
        getBridge().execute(() -> {
            try {
                long bytes = BackupDocumentIO.writeAndVerify(json.getBytes(StandardCharsets.UTF_8),
                    () -> getContext().getContentResolver().openOutputStream(uri, "wt"),
                    () -> getContext().getContentResolver().openInputStream(uri));
                JSObject response = new JSObject();
                response.put("status", "saved");
                response.put("verified", true);
                response.put("bytes", bytes);
                response.put("fileName", call.getString("fileName"));
                call.resolve(response);
            } catch (Exception ex) {
                // No financial records are touched. Never label a partially written document successful.
                call.reject("Dosya yazılamadı veya geri okunarak doğrulanamadı. Telefon/İndirilenler gibi yerel bir konum seçip tekrar dene. Oluşmuş dosyayı geçerli yedek kabul etme.", "WRITE_OR_VERIFY_FAILED");
            } finally { pending.set(false); }
        });
    }
}
