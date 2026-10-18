package com.semih.nova;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.xmlpull.v1.*;

final class Documents {
  static final int LIMIT = 10 * 1024 * 1024;

  static final class Result {
    String name, text, coverage;
  }

  static byte[] bounded(InputStream in, int max) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] b = new byte[8192];
    int n;
    while ((n = in.read(b)) != -1) {
      if (out.size() + n > max) throw new IOException("Dosya/çıkarılan içerik sınırı aşıldı.");
      out.write(b, 0, n);
    }
    return out.toByteArray();
  }

  static Result read(Context c, Uri uri) throws Exception {
    String name = "Belge";
    if ("file".equals(uri.getScheme())) name = new File(uri.getPath()).getName();
    else
      try (Cursor cursor =
          c.getContentResolver()
              .query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
        if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
      }
    byte[] data;
    try (InputStream in = c.getContentResolver().openInputStream(uri)) {
      if (in == null) throw new IOException("Dosya açılamadı.");
      data = bounded(in, LIMIT);
    }
    Result r = new Result();
    r.name = name;
    String ext = name.toLowerCase(Locale.ROOT);
    StringBuilder out = new StringBuilder();
    if (ext.endsWith(".pdf")) {
      PDFBoxResourceLoader.init(c);
      try (PDDocument doc = PDDocument.load(data)) {
        if (doc.isEncrypted()) throw new IOException("Şifreli PDF desteklenmiyor.");
        int pages = Math.min(30, doc.getNumberOfPages());
        PDFTextStripper stripper = new PDFTextStripper();
        for (int i = 1; i <= pages && out.length() < 100000; i++) {
          stripper.setStartPage(i);
          stripper.setEndPage(i);
          out.append("\n[PDF sayfa ").append(i).append("]\n").append(stripper.getText(doc));
        }
        r.coverage =
            "İlk en fazla 30 sayfa / 100.000 karakter. Toplam "
                + doc.getNumberOfPages()
                + " sayfa. OCR yok; taranmış sayfalarda metin bulunmayabilir.";
      } catch (LinkageError e) {
        throw new IOException(
            "Bu PDF'nin şifreleme veya ek özellikleri bu sürümde desteklenmiyor.", e);
      }
    } else if (ext.endsWith(".docx") || ext.endsWith(".xlsx")) {
      Map<String, byte[]> entries = new HashMap<>();
      int total = 0, count = 0;
      try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
        ZipEntry z;
        while ((z = zip.getNextEntry()) != null) {
          if (++count > 2000) throw new IOException("Belgede çok fazla arşiv girdisi var.");
          if (z.isDirectory()) continue;
          String key = z.getName();
          byte[] bytes = bounded(zip, Math.min(16 * 1024 * 1024 - total, 4 * 1024 * 1024));
          total += bytes.length;
          if (key.equals("word/document.xml")
              || key.equals("xl/sharedStrings.xml")
              || key.equals("xl/workbook.xml")
              || key.matches("xl/worksheets/sheet[0-9]+\\.xml")) entries.put(key, bytes);
        }
      }
      if (ext.endsWith(".docx")) {
        byte[] xml = entries.get("word/document.xml");
        if (xml == null) throw new IOException("Word belge içeriği bulunamadı.");
        XmlPullParser p = parser(xml);
        int para = 0;
        while (p.next() != XmlPullParser.END_DOCUMENT && out.length() < 100000) {
          if (p.getEventType() == XmlPullParser.START_TAG && p.getName().equals("p"))
            out.append("\n[Word paragraf ").append(++para).append("] ");
          if (p.getEventType() == XmlPullParser.START_TAG && p.getName().equals("t"))
            out.append(p.nextText()).append(' ');
        }
        r.coverage =
            "Ana belge metninin ilk 100.000 karakteri; resim, dipnot ve ekler kapsam dışı.";
      } else {
        List<String> shared = new ArrayList<>();
        if (entries.containsKey("xl/sharedStrings.xml")) {
          XmlPullParser p = parser(entries.get("xl/sharedStrings.xml"));
          StringBuilder value = null;
          while (p.next() != XmlPullParser.END_DOCUMENT) {
            if (p.getEventType() == XmlPullParser.START_TAG && p.getName().equals("si"))
              value = new StringBuilder();
            if (p.getEventType() == XmlPullParser.START_TAG
                && p.getName().equals("t")
                && value != null) value.append(p.nextText());
            if (p.getEventType() == XmlPullParser.END_TAG && p.getName().equals("si")) {
              shared.add(value.toString());
              if (shared.size() > 20000) throw new IOException("Paylaşılan metin sınırı aşıldı.");
            }
          }
        }
        List<String> sheets = new ArrayList<>();
        for (String key : entries.keySet()) if (key.startsWith("xl/worksheets/")) sheets.add(key);
        Collections.sort(sheets);
        int cells = 0;
        for (String sheet : sheets.subList(0, Math.min(5, sheets.size()))) {
          XmlPullParser p = parser(entries.get(sheet));
          String cell = "", type = "";
          while (p.next() != XmlPullParser.END_DOCUMENT && cells < 2000 && out.length() < 100000) {
            if (p.getEventType() == XmlPullParser.START_TAG && p.getName().equals("c")) {
              cell = p.getAttributeValue(null, "r");
              type = p.getAttributeValue(null, "t");
            }
            if (p.getEventType() == XmlPullParser.START_TAG
                && (p.getName().equals("v") || p.getName().equals("t"))) {
              String value = p.nextText();
              if ("s".equals(type)) {
                try {
                  value = shared.get(Integer.parseInt(value));
                } catch (Exception e) {
                  value = "[Okunamayan hücre]";
                }
              }
              out.append('[')
                  .append(sheet)
                  .append('!')
                  .append(cell)
                  .append("] ")
                  .append(value)
                  .append('\n');
              cells++;
            }
          }
        }
        r.coverage =
            "İlk en fazla 5 çalışma sayfası / 2.000 hücre / 100.000 karakter. Formüller"
                + " çalıştırılmaz; kayıtlı ham değerler okunur. Tarih/biçim dönüşümü yapılmaz.";
      }
    } else if (ext.endsWith(".txt")) {
      out.append(new String(data, java.nio.charset.StandardCharsets.UTF_8));
      r.coverage = "UTF-8 metnin ilk 100.000 karakteri.";
    } else
      throw new IOException("PDF, DOCX, XLSX veya TXT seç. Eski DOC/XLS biçimleri desteklenmiyor.");
    r.text = Prompt.cut(out.toString(), 100000);
    if (r.text.trim().isEmpty())
      throw new IOException(
          "Okunabilir metin yok. Taranmış belgeler için bu sürümde OCR bulunmuyor.");
    return r;
  }

  static XmlPullParser parser(byte[] data) throws Exception {
    XmlPullParserFactory f = XmlPullParserFactory.newInstance();
    f.setNamespaceAware(true);
    XmlPullParser p = f.newPullParser();
    p.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
    p.setInput(new ByteArrayInputStream(data), "UTF-8");
    return p;
  }
}
