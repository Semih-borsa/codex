package com.semih.sicil360;

import static com.semih.sicil360.Model.*;

import android.content.Context;
import android.security.keystore.*;
import android.util.AtomicFile;
import java.io.*;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Private authenticated storage with optimistic revisions, atomic writes and encrypted undo. */
final class Vault {
  static final Object MUTEX = new Object();
  final File dir;
  final AtomicFile file;
  final File undo;

  Vault(Context context) {
    dir = context.getFilesDir();
    file = new AtomicFile(new File(dir, "sicil360.enc"));
    undo = new File(dir, "before-change.enc");
  }

  SecretKey key() throws Exception {
    KeyStore store = KeyStore.getInstance("AndroidKeyStore");
    store.load(null);
    if (!store.containsAlias("sicil360-local-v1")) {
      KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
      generator.init(
          new KeyGenParameterSpec.Builder(
                  "sicil360-local-v1",
                  KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
              .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
              .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
              .build());
      generator.generateKey();
    }
    return (SecretKey) store.getKey("sicil360-local-v1", null);
  }

  byte[] encrypt(byte[] plain) throws Exception {
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(Cipher.ENCRYPT_MODE, key());
    c.updateAAD("S360D1".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    b.write(c.getIV());
    b.write(c.doFinal(plain));
    return b.toByteArray();
  }

  State decrypt(byte[] data) throws Exception {
    if (data.length < 28) throw new IOException("Şifreli Veri Dosyası Bozuk.");
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Arrays.copyOf(data, 12)));
    c.updateAAD("S360D1".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    return Codec.decode(c.doFinal(data, 12, data.length - 12));
  }

  State load() throws Exception {
    synchronized (MUTEX) {
      if (!file.getBaseFile().exists() && !new File(file.getBaseFile() + ".bak").exists())
        return new State();
      try (InputStream in = file.openRead()) {
        return decrypt(Excel.read(in, Codec.MAX_BYTES + 64));
      }
    }
  }

  static void atomic(AtomicFile out, byte[] bytes) throws IOException {
    FileOutputStream stream = null;
    try {
      stream = out.startWrite();
      stream.write(bytes);
      out.finishWrite(stream);
    } catch (IOException e) {
      if (stream != null) out.failWrite(stream);
      throw e;
    }
  }

  State commit(State proposed, long expected, boolean backup) throws Exception {
    synchronized (MUTEX) {
      State current = load();
      if (current.revision != expected)
        throw new IOException("Veriler Değişti. Ekranı Yenileyip Tekrar Deneyin.");
      State copy = Codec.copy(proposed);
      copy.revision = current.revision + 1;
      if (backup) atomic(new AtomicFile(undo), encrypt(Codec.encode(current)));
      atomic(file, encrypt(Codec.encode(copy)));
      return copy;
    }
  }

  State restoreUndo(long expected) throws Exception {
    synchronized (MUTEX) {
      State current = load();
      if (current.revision != expected) throw new IOException("Veriler Değişti. Yeniden Açın.");
      if (!undo.exists()) throw new IOException("Geri Alınacak İşlem Yok.");
      State before;
      try (InputStream in = new FileInputStream(undo)) {
        before = decrypt(Excel.read(in, Codec.MAX_BYTES + 64));
      }
      before.debts.clear();
      before.debts.addAll(current.debts);
      before.history.clear();
      before.history.addAll(current.history);
      before.recent.removeIf(id -> !before.people.containsKey(id));
      State result = commit(before, expected, false);
      new AtomicFile(undo).delete();
      return result;
    }
  }

  State erase(long expected) throws Exception {
    synchronized (MUTEX) {
      State cleared = commit(new State(), expected, false);
      new AtomicFile(undo).delete();
      return cleared;
    }
  }
}
