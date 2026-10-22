package com.semih.sicil360;

import java.io.*;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/** Portable password-encrypted backups; random salt/IV, authenticated version header. */
public final class Backup {
  static final byte[] MAGIC = "S360B1".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

  static SecretKeySpec key(char[] password, byte[] salt) throws Exception {
    PBEKeySpec spec = new PBEKeySpec(password, salt, 210000, 256);
    try {
      return new SecretKeySpec(
          SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(),
          "AES");
    } finally {
      spec.clearPassword();
    }
  }

  public static byte[] seal(byte[] plain, char[] password) throws Exception {
    if (password.length < 10)
      throw new IllegalArgumentException("Yedek Şifresi En Az 10 Karakter Olmalı.");
    if (plain.length > Codec.MAX_BYTES) throw new IOException("Yedek Çok Büyük.");
    byte[] salt = new byte[16], iv = new byte[12];
    SecureRandom random = new SecureRandom();
    random.nextBytes(salt);
    random.nextBytes(iv);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), new GCMParameterSpec(128, iv));
    cipher.updateAAD(MAGIC);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(MAGIC);
    out.write(salt);
    out.write(iv);
    out.write(cipher.doFinal(plain));
    return out.toByteArray();
  }

  public static byte[] open(byte[] encrypted, char[] password) throws Exception {
    if (encrypted.length < 50
        || encrypted.length > Codec.MAX_BYTES + 64
        || !Arrays.equals(MAGIC, Arrays.copyOf(encrypted, 6)))
      throw new IOException("Geçersiz Yedek Dosyası.");
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(
        Cipher.DECRYPT_MODE,
        key(password, Arrays.copyOfRange(encrypted, 6, 22)),
        new GCMParameterSpec(128, Arrays.copyOfRange(encrypted, 22, 34)));
    cipher.updateAAD(MAGIC);
    try {
      return cipher.doFinal(encrypted, 34, encrypted.length - 34);
    } catch (AEADBadTagException bad) {
      throw new IOException("Yedek Şifresi Yanlış Veya Dosya Bozuk.");
    }
  }
}
