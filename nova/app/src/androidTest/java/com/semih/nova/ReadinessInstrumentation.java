package com.semih.nova;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.*;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.*;

public final class ReadinessInstrumentation extends Instrumentation {
  private int passed;

  public void onCreate(Bundle arguments) {
    super.onCreate(arguments);
    start();
  }

  private void check(boolean result, String name) {
    if (!result) throw new AssertionError(name);
    passed++;
    Bundle s = new Bundle();
    s.putString("stream", "PASS " + name + "\n");
    sendStatus(0, s);
  }

  private File file(String name) throws IOException {
    File dir =
        new File(getTargetContext().getFilesDir(), "readiness-" + System.currentTimeMillis());
    if (!dir.mkdirs()) throw new IOException("Test klasörü");
    return new File(dir, name);
  }

  private File zip(String name, Map<String, String> content) throws Exception {
    File f = file(name);
    try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(f))) {
      for (Map.Entry<String, String> e : content.entrySet()) {
        z.putNextEntry(new ZipEntry(e.getKey()));
        z.write(e.getValue().getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
      }
    }
    return f;
  }

  @Override
  public void onStart() {
    Bundle result = new Bundle();
    try {
      Context c = getTargetContext();
      String database = "readiness-" + System.currentTimeMillis() + ".db";
      long memory, task;
      try (Store db = new Store(c, database)) {
        memory = db.saveMemory(0, "Tercih", "Türkçe kısa cevaplar");
        db.message("user", "Merhaba Adaş");
        task = db.saveTask(0, "Tanılama görevi", System.currentTimeMillis() + 3600000);
        check(db.memories().size() == 1, "Memory save");
        check(db.messages().get(0).body.equals("Merhaba Adaş"), "Chat save");
        check(!db.task(task).done, "Task initially pending");
        db.done(task, true);
        check(db.task(task).done, "Task completed");
        db.done(task, false);
        check(!db.task(task).done, "Task reopen");
      }
      try (Store db = new Store(c, database)) {
        check(db.memories().get(0).id == memory, "SQLite survives reopen");
        check(db.tasks().get(0).id == task, "Task survives reopen");
      }
      File doc =
          zip(
              "sample.docx",
              Collections.singletonMap(
                  "word/document.xml",
                  "<w:document xmlns:w=\"w\"><w:body><w:p><w:r><w:t>Merhaba Adas belge"
                      + " testi</w:t></w:r></w:p></w:body></w:document>"));
      Documents.Result word = Documents.read(c, Uri.fromFile(doc));
      check(word.text.contains("Merhaba Adas"), "DOCX text");
      check(word.text.contains("Word paragraf 1"), "DOCX source");
      Map<String, String> sheets = new HashMap<>();
      sheets.put("xl/sharedStrings.xml", "<sst><si><t>Gelir</t></si></sst>");
      sheets.put(
          "xl/worksheets/sheet1.xml",
          "<worksheet><sheetData><row><c r=\"A1\" t=\"s\"><v>0</v></c><c"
              + " r=\"B1\"><v>5000</v></c></row></sheetData></worksheet>");
      Documents.Result excel = Documents.read(c, Uri.fromFile(zip("sample.xlsx", sheets)));
      check(excel.text.contains("Gelir"), "XLSX shared string");
      check(excel.text.contains("B1] 5000"), "XLSX cell reference");
      PDFBoxResourceLoader.init(c);
      File pdf = file("sample.pdf");
      try (PDDocument d = new PDDocument()) {
        PDPage page = new PDPage();
        d.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(d, page)) {
          content.beginText();
          content.setFont(PDType1Font.HELVETICA, 14);
          content.newLineAtOffset(40, 700);
          content.showText("NOVA local PDF analysis");
          content.endText();
        }
        d.save(pdf);
      }
      Documents.Result pdfResult = Documents.read(c, Uri.fromFile(pdf));
      check(pdfResult.text.contains("NOVA local PDF"), "PDF real text extraction");
      check(pdfResult.text.contains("PDF sayfa 1"), "PDF source reference");
      try {
        Documents.bounded(new ByteArrayInputStream(new byte[20]), 10);
        throw new AssertionError("Size limit absent");
      } catch (IOException expected) {
        check(true, "Document byte limit");
      }
      File invalid = file("invalid.gguf");
      try (OutputStream out = new FileOutputStream(invalid)) {
        out.write(new byte[100]);
      }
      try {
        NativeBridge.reset();
        NativeBridge.generate(
            invalid.getPath(), "hello".getBytes(StandardCharsets.UTF_8), bytes -> {});
        throw new AssertionError("Invalid model accepted");
      } catch (IllegalStateException expected) {
        check(true, "Native invalid GGUF rejected");
      }
      File tiny = file("diagnostic.gguf");
      try (InputStream in = getContext().getAssets().open("diagnostic.gguf");
          OutputStream out = new FileOutputStream(tiny)) {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
      }
      NativeBridge.reset();
      byte[] generated =
          NativeBridge.generate(
              tiny.getPath(), "Merhaba".getBytes(StandardCharsets.UTF_8), bytes -> {});
      check(
          generated != null,
          "Native CPU token inference (untrained diagnostic fixture; not Turkish quality)");
      NativeBridge.reset();
      try {
        NativeBridge.transcribe(invalid.getPath(), new float[16000]);
        throw new AssertionError("Invalid audio model accepted");
      } catch (IllegalStateException expected) {
        check(true, "Whisper invalid model rejected");
      }
      java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Europe/Istanbul"));
      String forecast =
          "{\"timezone\":\"Europe/Istanbul\",\"daily\":{\"time\":[\""
              + today
              + "\",\""
              + today.plusDays(1)
              + "\"],\"temperature_2m_max\":[18,26],\"temperature_2m_min\":[8,14],\"weather_code\":[0,3],\"precipitation_probability_max\":[10,20]}}";
      final String geodata =
          "{\"results\":[{\"name\":\"İstanbul\",\"country_code\":\"TR\",\"country\":\"Türkiye\",\"latitude\":41,\"longitude\":29}]}";
      Weather.Result weather =
          Weather.get(
              new WeatherQuery("İstanbul", 1),
              url -> url.contains("geocoding") ? geodata : forecast);
      check(
          weather.answer.contains("26") && weather.answer.contains("14"),
          "Tomorrow weather uses tomorrow values from source fixture");
      check(
          weather.answer.contains("kapalı") && weather.answer.contains("yüzde 20"),
          "Weather description and rain probability");
      check(
          weather.answer.contains("Open-Meteo")
              && weather.answer.contains(
                  today
                      .plusDays(1)
                      .format(
                          java.time.format.DateTimeFormatter.ofPattern(
                              "d MMMM yyyy", new Locale("tr")))),
          "Weather date and provenance");
      try {
        Weather.get(
            new WeatherQuery("İstanbul", 1),
            url -> url.contains("geocoding") ? geodata : forecast.replace("[18,26]", "[18,null]"));
        throw new AssertionError("Missing temperature fabricated");
      } catch (IOException expected) {
        check(true, "Missing weather data rejected");
      }
      MainActivity activity =
          (MainActivity)
              startActivitySync(
                  new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      Store voiceFixture = new Store(c, "voice-readiness-" + System.currentTimeMillis() + ".db");
      runOnMainSync(
          () -> {
            try {
              java.lang.reflect.Field dbField = MainActivity.class.getDeclaredField("db");
              dbField.setAccessible(true);
              Store old = (Store) dbField.get(activity);
              dbField.set(activity, voiceFixture);
              old.close();
              java.lang.reflect.Field prefsField = MainActivity.class.getDeclaredField("prefs");
              prefsField.setAccessible(true);
              prefsField.set(
                  activity,
                  c.getSharedPreferences(
                      "voice-readiness-" + System.currentTimeMillis(), Context.MODE_PRIVATE));
              java.lang.reflect.Field convField =
                  MainActivity.class.getDeclaredField("conversation");
              convField.setAccessible(true);
              ((Conversation) convField.get(activity)).start();
              Bundle spoken = new Bundle();
              spoken.putStringArrayList(
                  android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
                  new ArrayList<>(Collections.singletonList("Yarın hava nasıl?")));
              activity.onResults(spoken);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
      waitForIdleSync();
      check(
          voiceFixture.messages().size() == 2
              && voiceFixture.messages().get(0).body.equals("Yarın hava nasıl?"),
          "Recognized speech automatically sends without pressing Send");
      check(
          voiceFixture.messages().get(1).body.contains("Hangi şehir"),
          "Weather without city asks instead of model hallucination");
      runOnMainSync(activity::finish);
      NativeBridge.release();
      AtomicBoolean cancelled = new AtomicBoolean(true);
      File partial = file("cancelled.part");
      try {
        Models.copy(
            new ByteArrayInputStream(new byte[128]),
            partial,
            128,
            null,
            cancelled,
            text -> {},
            false);
        throw new AssertionError("Cancelled copy accepted");
      } catch (IOException expected) {
        check(true, "Model copy cancellation");
      }
      result.putString(
          "stream", "OK (" + passed + " Android checks). Fixtures isolated from user database.\n");
      finish(Activity.RESULT_OK, result);
    } catch (Throwable e) {
      result.putString(
          "stream",
          "FAILED after " + passed + ": " + e + "\n" + android.util.Log.getStackTraceString(e));
      finish(Activity.RESULT_CANCELED, result);
    }
  }
}
