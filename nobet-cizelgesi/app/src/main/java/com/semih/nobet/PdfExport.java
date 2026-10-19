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
      int pages = (s.places.size() + 2) / 3;
      for (int part = 0; part < pages; part++) {
        List<Place> locations = s.places.subList(part * 3, Math.min(s.places.size(), part * 3 + 3));
        int columnWidth = 570 / locations.size();
        PdfDocument.Page page =
            pdf.startPage(new PdfDocument.PageInfo.Builder(842, 595, part + 1).create());
        Canvas c = page.getCanvas();
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.rgb(16, 35, 57));
        p.setTextSize(26);
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        c.drawText("NÖBET ÇİZELGESİ", 36, 52, p);
        p.setTextSize(13);
        p.setTypeface(Typeface.DEFAULT);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        c.drawText(w.monday.format(fmt) + " – " + w.monday.plusDays(6).format(fmt), 36, 77, p);
        p.setColor(Color.rgb(31, 90, 137));
        c.drawRect(36, 98, 806, 131, p);
        p.setColor(Color.WHITE);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(12);
        c.drawText("TARİH / GÜN", 48, 119, p);
        for (int col = 0; col < locations.size(); col++)
          fit(c, p, locations.get(col).name, 238 + col * columnWidth, 119, columnWidth - 12);
        DateTimeFormatter day = DateTimeFormatter.ofPattern("dd.MM • EEEE", new Locale("tr", "TR"));
        for (int i = 0; i < 7; i++) {
          LocalDate date = w.monday.plusDays(i);
          int y = 132 + i * 43;
          p.setColor(i % 2 == 0 ? Color.rgb(240, 246, 249) : Color.WHITE);
          c.drawRect(36, y, 806, y + 42, p);
          p.setColor(Color.rgb(16, 35, 57));
          p.setTypeface(Typeface.DEFAULT);
          p.setTextSize(12);
          c.drawText(date.format(day), 48, y + 26, p);
          for (int col = 0; col < locations.size(); col++)
            fit(
                c,
                p,
                person(s, w, date, locations.get(col).id),
                238 + col * columnWidth,
                y + 26,
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
    ctx.startActivity(Intent.createChooser(i, "PDF paylaş • WhatsApp veya başka uygulama"));
  }
}
