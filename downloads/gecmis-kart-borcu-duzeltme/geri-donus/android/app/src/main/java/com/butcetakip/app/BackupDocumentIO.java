package com.butcetakip.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/** Writes a document, closes it, then verifies the actual bytes through the provider. */
public final class BackupDocumentIO {
    private BackupDocumentIO() {}
    public interface Output { OutputStream open() throws IOException; }
    public interface Input { InputStream open() throws IOException; }
    public static long writeAndVerify(byte[] expected, Output output, Input input) throws IOException {
        if (expected == null || expected.length == 0) throw new IOException("Yedek içeriği boş.");
        try (OutputStream stream = output.open()) {
            if (stream == null) throw new IOException("Dosya yazmak için açılamadı.");
            stream.write(expected);
            stream.flush();
        }
        final MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException ex) { throw new IOException("Dosya doğrulaması başlatılamadı.", ex); }
        long count = 0;
        try (InputStream stream = input.open()) {
            if (stream == null) throw new IOException("Dosya doğrulamak için açılamadı.");
            byte[] buffer = new byte[8192];
            int n;
            while ((n = stream.read(buffer)) != -1) {
                count += n;
                if (count > expected.length) throw new IOException("Dosya boyutu yedekle eşleşmiyor.");
                digest.update(buffer, 0, n);
            }
        }
        byte[] wanted;
        try { wanted = MessageDigest.getInstance("SHA-256").digest(expected); }
        catch (NoSuchAlgorithmException ex) { throw new IOException(ex); }
        if (count != expected.length || !Arrays.equals(wanted, digest.digest())) {
            throw new IOException("Kaydedilen dosyanın içeriği doğrulanamadı.");
        }
        return count;
    }
}
