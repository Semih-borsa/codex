package com.semih.nobet;

import android.content.Context;
import android.security.keystore.*;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/**
 * Device-bound encrypted sessions/caches. No plaintext fallback; original local roster is separate.
 */
final class CloudVault {
  private final File directory;
  private static final String ALIAS = "nobet-cloud-v1";

  CloudVault(Context context) {
    directory = new File(context.getFilesDir(), "cloud");
    directory.mkdirs();
  }

  private SecretKey key() throws Exception {
    KeyStore store = KeyStore.getInstance("AndroidKeyStore");
    store.load(null);
    if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
    KeyGenerator generator =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
    generator.init(
        new KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build());
    return generator.generateKey();
  }

  private AtomicFile file(String name) {
    if (!name.matches("[a-zA-Z0-9_-]+")) throw new IllegalArgumentException();
    return new AtomicFile(new File(directory, name + ".bin"));
  }

  synchronized void write(String name, JSONObject json) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key());
    byte[] iv = cipher.getIV(),
        encrypted = cipher.doFinal(json.toString().getBytes(StandardCharsets.UTF_8));
    AtomicFile file = file(name);
    FileOutputStream out = null;
    try {
      out = file.startWrite();
      out.write(iv.length);
      out.write(iv);
      out.write(encrypted);
      file.finishWrite(out);
    } catch (Exception e) {
      if (out != null) file.failWrite(out);
      throw e;
    }
  }

  synchronized JSONObject read(String name) throws Exception {
    AtomicFile file = file(name);
    if (!file.getBaseFile().exists() && !new File(file.getBaseFile() + ".bak").exists())
      return null;
    byte[] bytes = file.readFully();
    int length = bytes[0] & 255;
    if (length != 12 || bytes.length < length + 17) throw new IOException("Oturum açılamadı");
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, bytes, 1, length));
    return new JSONObject(
        new String(
            cipher.doFinal(bytes, 1 + length, bytes.length - 1 - length), StandardCharsets.UTF_8));
  }

  synchronized void delete(String name) {
    file(name).delete();
  }
}
