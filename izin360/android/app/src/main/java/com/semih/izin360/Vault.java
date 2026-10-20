package com.semih.izin360;

import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

final class Vault {
  static javax.crypto.SecretKey key() throws Exception {
    KeyStore s = KeyStore.getInstance("AndroidKeyStore");
    s.load(null);
    if (!s.containsAlias("izin360-connection")) {
      KeyGenerator g = KeyGenerator.getInstance("AES", "AndroidKeyStore");
      g.init(
          new KeyGenParameterSpec.Builder(
                  "izin360-connection",
                  KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
              .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
              .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
              .build());
      g.generateKey();
    }
    return (javax.crypto.SecretKey) s.getKey("izin360-connection", null);
  }

  static String encrypt(String text) throws Exception {
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(Cipher.ENCRYPT_MODE, key());
    return Base64.encodeToString(c.getIV(), Base64.NO_WRAP)
        + ":"
        + Base64.encodeToString(
            c.doFinal(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)), Base64.NO_WRAP);
  }

  static String decrypt(String saved) throws Exception {
    if (saved.isEmpty()) return "";
    String[] parts = saved.split(":");
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(
        Cipher.DECRYPT_MODE,
        key(),
        new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
    return new String(
        c.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)),
        java.nio.charset.StandardCharsets.UTF_8);
  }
}
