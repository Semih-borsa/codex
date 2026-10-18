package com.semih.nova;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class VoiceRecorder {
  interface Result {
    void done(float[] samples);

    void error(String text);
  }

  private final AtomicBoolean recording = new AtomicBoolean();
  private final AtomicBoolean discard = new AtomicBoolean();

  void cancel() {
    discard.set(true);
    recording.set(false);
  }

  void stop() {
    recording.set(false);
  }

  void start(Context c, Result result) {
    if (c.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      result.error("Mikrofon izni gerekli.");
      return;
    }
    discard.set(false);
    recording.set(true);
    new Thread(
            () -> {
              AudioRecord rec = null;
              try {
                int min =
                    AudioRecord.getMinBufferSize(
                        16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                if (min <= 0) throw new IOException("16 kHz ses kaydı desteklenmiyor.");
                rec =
                    new AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION,
                        16000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        Math.max(min, 4096));
                if (rec.getState() != AudioRecord.STATE_INITIALIZED)
                  throw new IOException("Mikrofon başlatılamadı.");
                float[] samples = new float[480000];
                short[] buffer = new short[1024];
                int count = 0;
                rec.startRecording();
                while (recording.get() && count < samples.length) {
                  int n = rec.read(buffer, 0, Math.min(buffer.length, samples.length - count));
                  if (n < 0) throw new IOException("Mikrofon okuma hatası: " + n);
                  for (int i = 0; i < n; i++) samples[count++] = buffer[i] / 32768f;
                }
                if (count < 8000) throw new IOException("En az yarım saniye konuş.");
                if (discard.get()) throw new IOException("Ses kaydı iptal edildi.");
                result.done(java.util.Arrays.copyOf(samples, count));
              } catch (Exception e) {
                result.error(e.getMessage());
              } finally {
                recording.set(false);
                if (rec != null) {
                  try {
                    rec.stop();
                  } catch (Exception ignored) {
                  }
                  rec.release();
                }
              }
            },
            "nova-microphone")
        .start();
  }
}
