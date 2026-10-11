package com.semih.ilacim;

import android.content.Context;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

final class Vault {
  static javax.crypto.SecretKey key() throws Exception {
    KeyStore s = KeyStore.getInstance("AndroidKeyStore");
    s.load(null);
    if (!s.containsAlias("ilacim-api")) {
      KeyGenerator g = KeyGenerator.getInstance("AES", "AndroidKeyStore");
      g.init(
          new KeyGenParameterSpec.Builder(
                  "ilacim-api", KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
              .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
              .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
              .build());
      g.generateKey();
    }
    return (javax.crypto.SecretKey) s.getKey("ilacim-api", null);
  }

  static void save(Context c, String token) throws Exception {
    Cipher x = Cipher.getInstance("AES/GCM/NoPadding");
    x.init(Cipher.ENCRYPT_MODE, key());
    String value =
        Base64.encodeToString(x.getIV(), Base64.NO_WRAP)
            + ":"
            + Base64.encodeToString(
                x.doFinal(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)), Base64.NO_WRAP);
    if (!c.getSharedPreferences("api", 0).edit().putString("token", value).commit())
      throw new IllegalStateException("Anahtar kaydedilemedi");
  }

  static String read(Context c) throws Exception {
    String s = c.getSharedPreferences("api", 0).getString("token", "");
    if (s.isEmpty()) return "";
    String[] parts = s.split(":");
    Cipher x = Cipher.getInstance("AES/GCM/NoPadding");
    x.init(
        Cipher.DECRYPT_MODE,
        key(),
        new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
    return new String(
        x.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)),
        java.nio.charset.StandardCharsets.UTF_8);
  }
}
