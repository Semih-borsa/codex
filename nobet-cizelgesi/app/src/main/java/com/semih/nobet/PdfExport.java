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
    i.setClipData(ClipData.newRawUri("Haftalık nöbet PDF", u));
    ctx.startActivity(Intent.createChooser(i, "PDF Paylaş • WhatsApp veya Başka Uygulama"));
  }
}
