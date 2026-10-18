package com.semih.nova;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.os.*;
import android.speech.tts.*;
import java.util.*;

public final class ReminderSpeechService extends Service {
  static final String STOP = "com.semih.nova.STOP_REMINDER_SPEECH",
      CHANNEL = "nova_spoken_reminders";
  static final int ID = 910001;
  private final Handler ui = new Handler(Looper.getMainLooper());
  private TextToSpeech tts;
  private boolean ready, playing;
  private long playbackToken;
  private int initStatus = -99;
  private final ArrayDeque<String> queue = new ArrayDeque<>();
  private final Set<Long> seen = new HashSet<>();
  private AudioManager audio;
  private AudioFocusRequest focus;
  private PowerManager.WakeLock wakeLock;

  private Notification notice(String text) {
    NotificationManager manager = getSystemService(NotificationManager.class);
    manager.createNotificationChannel(
        new NotificationChannel(
            CHANNEL, "Sesli hatırlatma durumu", NotificationManager.IMPORTANCE_LOW));
    PendingIntent stop =
        PendingIntent.getService(
            this,
            0,
            new Intent(this, ReminderSpeechService.class).setAction(STOP),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    PendingIntent open =
        PendingIntent.getActivity(
            this,
            0,
            new Intent(this, MainActivity.class).putExtra("tasks", true),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    return new Notification.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.ic_nova)
        .setContentTitle("Adaş · sesli hatırlatma")
        .setContentText(text)
        .setContentIntent(open)
        .setVisibility(Notification.VISIBILITY_PRIVATE)
        .addAction(new Notification.Action.Builder(null, "Sesi durdur", stop).build())
        .setOngoing(true)
        .build();
  }

  @Override
  public void onCreate() {
    super.onCreate();
    if (Build.VERSION.SDK_INT >= 29)
      startForeground(
          ID,
          notice("Hatırlatma hazırlanıyor…"),
          ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
    else startForeground(ID, notice("Hatırlatma hazırlanıyor…"));
    wakeLock =
        getSystemService(PowerManager.class)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nova:reminder-speech");
    wakeLock.acquire(45000);
    audio = getSystemService(AudioManager.class);
    tts =
        new TextToSpeech(
            this,
            result ->
                ui.post(
                    () -> {
                      initStatus = result;
                      if (result != TextToSpeech.SUCCESS) {
                        fail("Türkçe ses motoru hazır değil; hatırlatma bildirimi korunuyor.");
                        return;
                      }
                      Voice voice = OfflineSpeech.find(tts);
                      if (voice == null || tts.setVoice(voice) != TextToSpeech.SUCCESS) {
                        fail("Çevrimdışı Türkçe ses yok; hatırlatma bildirimi korunuyor.");
                        return;
                      }
                      ready = true;
                      tts.setAudioAttributes(
                          new AudioAttributes.Builder()
                              .setUsage(AudioAttributes.USAGE_ALARM)
                              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                              .build());
                      if (!seen.isEmpty()) next();
                    }));
    tts.setOnUtteranceProgressListener(
        new UtteranceProgressListener() {
          public void onStart(String id) {}

          public void onDone(String id) {
            ui.post(
                () -> {
                  playing = false;
                  if (focus != null) {
                    audio.abandonAudioFocusRequest(focus);
                    focus = null;
                  }
                  next();
                });
          }

          public void onError(String id) {
            ui.post(() -> fail("Sesli okuma tamamlanamadı; bildirimi kontrol et."));
          }
        });
    ui.postDelayed(
        () -> {
          if (!ready) fail("Ses motoru zamanında hazırlanamadı; bildirimi kontrol et.");
        },
        12000);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null || STOP.equals(intent.getAction())) {
      stopSelf();
      return START_NOT_STICKY;
    }
    long id = intent.getLongExtra("id", -1);
    String text = intent.getStringExtra("text");
    if (text == null || id < 0 || text.isEmpty()) {
      stopSelf();
      return START_NOT_STICKY;
    }
    if (seen.add(id) && queue.size() < 10) queue.add(Prompt.cut(text, 200));
    if (initStatus == TextToSpeech.ERROR) {
      stopSelf();
      return START_NOT_STICKY;
    }
    next();
    return START_NOT_STICKY;
  }

  private void next() {
    if (!ready || playing) return;
    if (queue.isEmpty()) {
      stopSelf();
      return;
    }
    NotificationManager notifications = getSystemService(NotificationManager.class);
    if (audio.getRingerMode() == AudioManager.RINGER_MODE_SILENT
        || audio.getStreamVolume(AudioManager.STREAM_ALARM) == 0
        || notifications.getCurrentInterruptionFilter()
            != NotificationManager.INTERRUPTION_FILTER_ALL) {
      fail("Sessiz mod veya Rahatsız Etmeyin: sesli okuma yapılmadı.");
      return;
    }
    AudioAttributes attrs =
        new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build();
    focus =
        new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener(
                change -> {
                  if (change == AudioManager.AUDIOFOCUS_LOSS) stopSelf();
                })
            .build();
    if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      fail("Ses çıkışı kullanılamadı; bildirimi kontrol et.");
      return;
    }
    playing = true;
    long token = ++playbackToken;
    String text = queue.remove();
    notifications.notify(ID, notice("Hatırlatma seslendiriliyor"));
    if (tts.speak(
            "Adaş hatırlatması. " + text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "reminder-" + System.nanoTime())
        != TextToSpeech.SUCCESS) fail("Ses başlatılamadı; bildirimi kontrol et.");
    ui.postDelayed(
        () -> {
          if (playing && playbackToken == token) fail("Sesli uyarı süre sınırında durduruldu.");
        },
        30000);
  }

  private void fail(String message) {
    NotificationManager manager = getSystemService(NotificationManager.class);
    manager.notify(
        "speech_status",
        ID + 1,
        new Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_nova)
            .setContentTitle("Adaş · sesli uyarı durumu")
            .setContentText(message)
            .setStyle(new Notification.BigTextStyle().bigText(message))
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build());
    stopSelf();
  }

  @Override
  public void onDestroy() {
    ui.removeCallbacksAndMessages(null);
    if (tts != null) {
      tts.stop();
      tts.shutdown();
    }
    if (audio != null && focus != null) audio.abandonAudioFocusRequest(focus);
    if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    stopForeground(STOP_FOREGROUND_REMOVE);
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent i) {
    return null;
  }
}
