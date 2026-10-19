package com.semih.nobet;

import static com.semih.nobet.Roster.*;

import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class PdfExport {
  static String person(State s, Week w, LocalDate date, String place) {
    return w.duties.stream()
        .filter(d -> d.date.equals(date) && d.place.equals(place))
        .map(d -> s.name(d.person))
        .findFirst()
        .orElse("—");
  }

  static File create(Context context, State s, Week w) throws IOException {
    File dir = new File(context.getCacheDir(), "pdf");
    if (!dir.exists() && !dir.mkdirs()) throw new IOException();
    File[] files = dir.listFiles();
    if (files != null)
      for (File f : files)
        if (f.lastModified() < System.currentTimeMillis() - 7L * 86400000) f.delete();
    File out =
        new File(
            dir,
            "Nobet_"
                + w.monday.toString().replace("-", "_")
                + "_"
                + System.currentTimeMillis()
                + ".pdf");
    PdfDocument pdf = new PdfDocument();
    try {
      List<Place> shown = displayPlaces(s, w);
      int pages = (shown.size() + 2) / 3;
      for (int part = 0; part < pages; part++) {
        List<Place> locations = shown.subList(part * 3, Math.min(shown.size(), part * 3 + 3));
        int columnWidth = 470 / locations.size();
        PdfDocument.Page page =
            pdf.startPage(new PdfDocument.PageInfo.Builder(700, 358, part + 1).create());
        Canvas c = page.getCanvas();
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.rgb(16, 35, 57));
        p.setTextSize(21);
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        c.drawText("Nöbet Çizelgesi", 18, 34, p);
        p.setTextSize(13);
        p.setTypeface(Typeface.DEFAULT);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        c.drawText(w.monday.format(fmt) + " – " + w.monday.plusDays(6).format(fmt), 18, 55, p);
        p.setColor(Color.rgb(42, 111, 177));
        c.drawRect(18, 70, 682, 100, p);
        p.setColor(Color.WHITE);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(12);
        c.drawText("Tarih / Gün", 26, 90, p);
        for (int col = 0; col < locations.size(); col++)
          fit(c, p, locations.get(col).name, 208 + col * columnWidth, 90, columnWidth - 12);
        DateTimeFormatter day = DateTimeFormatter.ofPattern("dd.MM • EEEE", new Locale("tr", "TR"));
        for (int i = 0; i < 7; i++) {
          LocalDate date = w.monday.plusDays(i);
          int y = 101 + i * 34;
          p.setColor(i % 2 == 0 ? Color.rgb(240, 246, 249) : Color.WHITE);
          c.drawRect(18, y, 682, y + 33, p);
          p.setColor(Color.rgb(16, 35, 57));
          p.setTypeface(Typeface.DEFAULT);
          p.setTextSize(12);
          c.drawText(titleDay(date.format(day)), 26, y + 22, p);
          for (int col = 0; col < locations.size(); col++)
            fit(
                c,
                p,
                person(s, w, date, locations.get(col).id),
                208 + col * columnWidth,
                y + 22,
                columnWidth - 12);
        }
        pdf.finishPage(page);
      }
      try (FileOutputStream stream = new FileOutputStream(out)) {
        pdf.writeTo(stream);
      }
    } finally {
      pdf.close();
    }
    return out;
  }

  static List<String> statisticsLabels(State s) {
    List<String> labels = new ArrayList<>(Arrays.asList("Hafta İçi", "Hafta Sonu"));
    for (Place place : statisticPlaces(s))
      labels.add(place.name + (place.deleted ? " (Silindi)" : ""));
    return labels;
  }

  static List<String> holidayLabels(State s) {
    List<String> labels = new ArrayList<>();
    for (Place place : Holidays.places(s))
      labels.add(place.name + (place.deleted ? " (Silindi)" : ""));
    return labels;
  }

  static File createHolidayStatistics(Context context, State s, LocalDate today)
      throws IOException {
    return createStatistics(context, s, today, true);
  }

  static File createStatistics(Context context, State s, LocalDate today) throws IOException {
    return createStatistics(context, s, today, false);
  }

  private static File createStatistics(Context context, State s, LocalDate today, boolean holidays)
      throws IOException {
    File dir = new File(context.getCacheDir(), "pdf");
    if (!dir.exists() && !dir.mkdirs()) throw new IOException();
    File[] old = dir.listFiles();
    if (old != null)
      for (File f : old)
        if (f.lastModified() < System.currentTimeMillis() - 7L * 86400000) f.delete();
    File out =
        new File(
            dir,
            (holidays ? "Resmi_Tatiller_" : "Istatistik_")
                + today.toString().replace("-", "_")
                + "_"
                + System.currentTimeMillis()
                + ".pdf");
    List<String> labels = holidays ? holidayLabels(s) : statisticsLabels(s);
    PdfDocument document = new PdfDocument();
    int number = 0;
    try {
      for (boolean future : new boolean[] {false, true}) {
        List<StatRow> rows =
            holidays ? Holidays.statistics(s, today, future) : statistics(s, today, future);
        boolean any = false;
        for (StatRow row : rows) for (long count : row.counts) if (count > 0) any = true;
        if (future && !any) continue;
        for (int first = 0; first < labels.size(); first += 4) {
          int end = Math.min(first + 4, labels.size());
          int width = 220 + (end - first) * 110;
          for (int start = 0; start < Math.max(1, rows.size()); start += 18) {
            int stop = Math.min(start + 18, rows.size());
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(11);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            List<List<String>> headers = new ArrayList<>();
            headers.add(wrap("Personel", paint, 204));
            int headerHeight = 28;
            for (int col = first; col < end; col++) {
              List<String> lines = wrap(labels.get(col), paint, 94);
              headers.add(lines);
              headerHeight = Math.max(headerHeight, lines.size() * 14 + 12);
            }
            paint.setTypeface(Typeface.DEFAULT);
            List<List<String>> names = new ArrayList<>();
            int height = 24 + headerHeight;
            for (int row = start; row < stop; row++) {
              Person person = rows.get(row).person;
              List<String> lines =
                  wrap(person.name + (person.active ? "" : " · Pasif"), paint, 204);
              names.add(lines);
              height += Math.max(26, lines.size() * 14 + 10);
            }
            PdfDocument.Page page =
                document.startPage(
                    new PdfDocument.PageInfo.Builder(width + 24, height + 24, ++number).create());
            Canvas canvas = page.getCanvas();
            // The period is a table band; no introduction, explanations, signatures or footer.
            paint.setColor(Color.rgb(42, 111, 177));
            canvas.drawRect(12, 12, 12 + width, 12 + 24 + headerHeight, paint);
            paint.setColor(Color.WHITE);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(
                (holidays ? "Resmî Tatiller • " : "")
                    + (future ? "Gelecek Atamalar" : "Bugün Ve Öncesi"),
                20,
                29,
                paint);
            int x = 20;
            for (int col = 0; col < headers.size(); col++) {
              drawLines(canvas, paint, headers.get(col), x, 52);
              x += col == 0 ? 220 : 110;
            }
            int y = 36 + headerHeight;
            paint.setTypeface(Typeface.DEFAULT);
            for (int row = start; row < stop; row++) {
              List<String> lines = names.get(row - start);
              int rowHeight = Math.max(26, lines.size() * 14 + 10);
              paint.setColor((row - start) % 2 == 0 ? Color.rgb(237, 245, 253) : Color.WHITE);
              canvas.drawRect(12, y, 12 + width, y + rowHeight, paint);
              paint.setColor(Color.rgb(16, 35, 57));
              drawLines(canvas, paint, lines, 20, y + 17);
              for (int col = first; col < end; col++) {
                String value =
                    holidays
                        ? Holidays.days(rows.get(row).counts[col])
                        : Long.toString(rows.get(row).counts[col]);
                canvas.drawText(
                    value,
                    12 + 220 + (col - first) * 110 + (110 - paint.measureText(value)) / 2,
                    y + 17,
                    paint);
              }
              y += rowHeight;
            }
            document.finishPage(page);
          }
        }
      }
      try (FileOutputStream stream = new FileOutputStream(out)) {
        document.writeTo(stream);
      }
    } finally {
      document.close();
    }
    return out;
  }

  private static List<String> wrap(String value, Paint paint, int width) {
    List<String> lines = new ArrayList<>();
    String remaining = value;
    while (!remaining.isEmpty()) {
      int count = paint.breakText(remaining, true, width, null);
      count = Math.max(1, count);
      if (count < remaining.length()) {
        int space = remaining.lastIndexOf(' ', count);
        if (space > 0) count = space;
      }
      lines.add(remaining.substring(0, count));
      remaining = remaining.substring(count).trim();
    }
    if (lines.isEmpty()) lines.add("");
    return lines;
  }

  private static void drawLines(
      Canvas canvas, Paint paint, List<String> lines, int x, int baseline) {
    for (String line : lines) {
      canvas.drawText(line, x, baseline, paint);
      baseline += 14;
    }
  }

  private static String titleDay(String text) {
    int start = text.indexOf("• ") + 2;
    return text.substring(0, start)
        + text.substring(start, start + 1).toUpperCase(new Locale("tr", "TR"))
        + text.substring(start + 1);
  }

  private static void fit(Canvas c, Paint p, String text, int x, int y, int width) {
    p.setTextSize(12);
    while (p.measureText(text) > width && p.getTextSize() > 8) p.setTextSize(p.getTextSize() - 1);
    if (p.measureText(text) > width) {
      while (text.length() > 1 && p.measureText(text + "…") > width)
        text = text.substring(0, text.length() - 1);
      text += "…";
    }
    c.drawText(text, x, y, p);
  }

  static void share(Context ctx, File f) {
    Uri u = Uri.parse("content://com.semih.nobet.pdf/" + f.getName());
    Intent i =
        new Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, u)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    i.setClipData(ClipData.newRawUri("Nöbet Çizelgesi PDF", u));
    ctx.startActivity(Intent.createChooser(i, "PDF Paylaş • WhatsApp veya Başka Uygulama"));
  }
}
