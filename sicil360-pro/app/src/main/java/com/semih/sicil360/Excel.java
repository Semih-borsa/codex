package com.semih.sicil360;

import static com.semih.sicil360.Model.*;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;
import org.xml.sax.ext.DefaultHandler2;

/** Offline XLSX reader/writer. Never evaluates formulas or opens external links. */
public final class Excel {
  public static final int MAX_FILE = 32 * 1024 * 1024,
      MAX_XML = 64 * 1024 * 1024,
      MAX_ROWS = 50001,
      MAX_CELLS = 800000;

  public static final class Cell {
    public String value = "";
    public boolean formula, unsafe;
  }

  public static final class Sheet {
    public String name, path;

    Sheet(String n, String p) {
      name = n;
      path = p;
    }
  }

  public static final class Table {
    public final TreeMap<Integer, TreeMap<Integer, Cell>> rows = new TreeMap<>();

    public String value(int row, int column) {
      Cell c = rows.containsKey(row) ? rows.get(row).get(column) : null;
      return c == null ? "" : c.value;
    }

    public List<String> headers(int row) {
      int max = rows.containsKey(row) && !rows.get(row).isEmpty() ? rows.get(row).lastKey() : 0;
      List<String> out = new ArrayList<>();
      for (int c = 0; c <= max; c++) out.add(value(row, c));
      return out;
    }

    public List<Row> mapped(int header, int[] mapping, boolean extras) {
      List<Row> result = new ArrayList<>();
      List<String> heads = headers(header);
      for (Map.Entry<Integer, TreeMap<Integer, Cell>> line : rows.entrySet()) {
        if (line.getKey() <= header
            || line.getValue().values().stream().allMatch(c -> c.value.trim().isEmpty())) continue;
        Person p = new Person();
        String[] values = new String[9];
        boolean bad = false;
        for (int i = 0; i < 9; i++) {
          int col = mapping[i];
          Cell cell = col < 0 ? null : line.getValue().get(col);
          values[i] = cell == null ? "" : cell.value.trim();
          if ((i == 0 || i == 3) && cell != null && (cell.formula || cell.unsafe)) bad = true;
        }
        p.sicil = values[0];
        p.ad = values[1];
        p.soyad = values[2];
        p.telefon = values[3];
        p.birim = values[4];
        p.unvan = values[5];
        p.email = values[6];
        p.not = values[7];
        p.durum = values[8];
        if (extras)
          for (Map.Entry<Integer, Cell> cell : line.getValue().entrySet()) {
            int col = cell.getKey();
            boolean used = false;
            for (int m : mapping) if (m == col) used = true;
            if (!used && !cell.getValue().value.isEmpty()) {
              String key = col < heads.size() ? heads.get(col) : "";
              if (key.trim().isEmpty()) key = "Sütun " + (col + 1);
              if (p.extra.containsKey(key)) key += " (" + (col + 1) + ")";
              p.extra.put(key, cell.getValue().value);
            }
          }
        Row row = new Row(line.getKey(), p);
        row.fields = new boolean[9];
        for (int i = 0; i < 9; i++) row.fields[i] = mapping[i] >= 0;
        row.extraProvided = extras;
        if (bad)
          row.error =
              "Sicil/Telefon formül içeriyor veya Excel sayı hassasiyetini aşmış; metin değer"
                  + " kullanın.";
        result.add(row);
      }
      return result;
    }
  }

  abstract static class Handler extends DefaultHandler2 {
    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
      throw new SAXException("DTD Desteklenmiyor.");
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
      throw new SAXException("Harici XML Kaynağı Yasak.");
    }
  }

  static byte[] read(InputStream in, int limit) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    int n;
    while ((n = in.read(buf)) != -1) {
      if (out.size() + n > limit) throw new IOException("Dosya Boyut Sınırı Aşıldı.");
      out.write(buf, 0, n);
    }
    return out.toByteArray();
  }

  static void parse(ZipFile zip, String path, Handler handler) throws Exception {
    ZipEntry entry = zip.getEntry(path);
    if (entry == null) throw new IOException("Excel İç Yapısı Eksik: " + path);
    if (entry.getSize() > MAX_XML) throw new IOException("Excel Sayfası Çok Büyük.");
    byte[] bytes;
    try (InputStream in = zip.getInputStream(entry)) {
      bytes = read(in, MAX_XML);
    }
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware(true);
    XMLReader reader = factory.newSAXParser().getXMLReader();
    reader.setContentHandler(handler);
    reader.setErrorHandler(handler);
    reader.setEntityResolver(handler);
    reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
    reader.parse(new InputSource(new ByteArrayInputStream(bytes)));
  }

  static void check(File file) throws IOException {
    if (file.length() > MAX_FILE) throw new IOException("XLSX Dosyası En Fazla 32 MB Olabilir.");
  }

  public static List<Sheet> sheets(File file) throws Exception {
    check(file);
    try (ZipFile zip = new ZipFile(file)) {
      if (zip.getEntry("[Content_Types].xml") == null)
        throw new IOException("Geçerli XLSX Dosyası Değil.");
      Map<String, String> rels = new HashMap<>();
      parse(
          zip,
          "xl/_rels/workbook.xml.rels",
          new Handler() {
            @Override
            public void startElement(String u, String l, String q, Attributes a)
                throws SAXException {
              if (l.equals("Relationship") && !"External".equals(a.getValue("TargetMode"))) {
                String target = a.getValue("Target");
                if (target == null) return;
                String normalized =
                    java.nio.file.Paths.get(
                            target.startsWith("/") ? target.substring(1) : "xl/" + target)
                        .normalize()
                        .toString()
                        .replace('\\', '/');
                if (normalized.startsWith("xl/")) rels.put(a.getValue("Id"), normalized);
              }
            }
          });
      List<Sheet> result = new ArrayList<>();
      parse(
          zip,
          "xl/workbook.xml",
          new Handler() {
            @Override
            public void startElement(String u, String l, String q, Attributes a)
                throws SAXException {
              if (l.equals("sheet")) {
                String id =
                    a.getValue(
                        "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
                        "id");
                if (id == null) id = a.getValue("r:id");
                String path = rels.get(id);
                if (path != null) {
                  if (result.size() >= 50) throw new SAXException("En Fazla 50 Sayfa Desteklenir.");
                  result.add(new Sheet(a.getValue("name"), path));
                }
              }
            }
          });
      if (result.isEmpty()) throw new IOException("Excel Sayfası Bulunamadı.");
      return result;
    }
  }

  public static Table load(File file, Sheet sheet) throws Exception {
    check(file);
    try (ZipFile zip = new ZipFile(file)) {
      List<String> shared = new ArrayList<>();
      final int[] characters = {0};
      if (zip.getEntry("xl/sharedStrings.xml") != null)
        parse(
            zip,
            "xl/sharedStrings.xml",
            new Handler() {
              StringBuilder text;
              boolean capture;

              @Override
              public void startElement(String u, String l, String q, Attributes a) {
                if (l.equals("si")) text = new StringBuilder();
                if (l.equals("t")) capture = true;
              }

              @Override
              public void characters(char[] c, int s, int n) throws SAXException {
                if (capture && text != null) {
                  characters[0] += n;
                  if (characters[0] > 16 * 1024 * 1024)
                    throw new SAXException("Excel Metin Sınırı Aşıldı.");
                  text.append(c, s, n);
                }
              }

              @Override
              public void endElement(String u, String l, String q) throws SAXException {
                if (l.equals("t")) capture = false;
                if (l.equals("si")) {
                  if (shared.size() > MAX_CELLS) throw new SAXException("Çok Fazla Metin Hücresi.");
                  shared.add(text.toString());
                }
              }
            });
      Map<Integer, String> formats = new HashMap<>();
      List<String> styles = new ArrayList<>();
      if (zip.getEntry("xl/styles.xml") != null)
        parse(
            zip,
            "xl/styles.xml",
            new Handler() {
              boolean xfs;

              @Override
              public void startElement(String u, String l, String q, Attributes a)
                  throws SAXException {
                if (l.equals("numFmt"))
                  formats.put(Integer.parseInt(a.getValue("numFmtId")), a.getValue("formatCode"));
                if (l.equals("cellXfs")) xfs = true;
                if (xfs && l.equals("xf")) {
                  if (styles.size() > 10000) throw new SAXException("Çok Fazla Hücre Biçimi.");
                  int id = Integer.parseInt(a.getValue("numFmtId"));
                  styles.add(formats.getOrDefault(id, ""));
                }
              }

              @Override
              public void endElement(String u, String l, String q) {
                if (l.equals("cellXfs")) xfs = false;
              }
            });
      Table table = new Table();
      parse(
          zip,
          sheet.path,
          new Handler() {
            int row, column, style, cells;
            String type;
            Cell cell;
            StringBuilder value;
            boolean capture;

            @Override
            public void startElement(String u, String l, String q, Attributes a)
                throws SAXException {
              if (l.equals("row")) {
                String n = a.getValue("r");
                row = n == null ? row + 1 : Integer.parseInt(n);
                column = -1;
                if (table.rows.size() >= MAX_ROWS)
                  throw new SAXException("En Fazla 50.000 Veri Satırı Desteklenir.");
                table.rows.put(row, new TreeMap<>());
              }
              if (l.equals("c")) {
                if (++cells > MAX_CELLS)
                  throw new SAXException("En Fazla 800.000 Hücre Desteklenir.");
                String ref = a.getValue("r");
                column = ref == null ? column + 1 : column(ref);
                if (column < 0 || column > 127)
                  throw new SAXException("En Fazla 128 Sütun Desteklenir.");
                cell = new Cell();
                value = new StringBuilder();
                type = a.getValue("t");
                String st = a.getValue("s");
                style = st == null ? 0 : Integer.parseInt(st);
              }
              if (l.equals("f") && cell != null) cell.formula = true;
              if (l.equals("v") || l.equals("t")) capture = true;
            }

            @Override
            public void characters(char[] c, int start, int length) throws SAXException {
              if (capture && value != null) {
                characters[0] += length;
                if (characters[0] > 24 * 1024 * 1024 || value.length() + length > 65536)
                  throw new SAXException("Excel Metni Çok Uzun.");
                value.append(c, start, length);
              }
            }

            @Override
            public void endElement(String u, String l, String q) throws SAXException {
              if (l.equals("v") || l.equals("t")) capture = false;
              if (l.equals("c")) {
                String raw = value.toString();
                if ("s".equals(type)) {
                  int i = Integer.parseInt(raw);
                  if (i < 0 || i >= shared.size()) throw new SAXException("Hatalı Metin Dizini.");
                  cell.value = shared.get(i);
                } else if (type == null || type.equals("n")) {
                  try {
                    BigDecimal n = new BigDecimal(raw);
                    cell.value = n.stripTrailingZeros().toPlainString();
                    String digits = cell.value.replaceAll("[^0-9]", "");
                    cell.unsafe = digits.length() > 15;
                    String mask = style < styles.size() ? styles.get(style) : "";
                    String part = mask.split(";", -1)[0];
                    if (part.matches("[0 ()+\\-]+")) {
                      int zeros = part.replaceAll("[^0]", "").length();
                      if (zeros <= 32 && cell.value.matches("[0-9]+"))
                        while (cell.value.length() < zeros) cell.value = "0" + cell.value;
                    }
                  } catch (NumberFormatException bad) {
                    cell.value = raw;
                  }
                } else cell.value = raw;
                table.rows.computeIfAbsent(row, k -> new TreeMap<>()).put(column, cell);
                cell = null;
                value = null;
              }
            }
          });
      return table;
    }
  }

  static int column(String ref) {
    int n = 0;
    for (char c : ref.toUpperCase(Locale.ROOT).toCharArray()) {
      if (c < 'A' || c > 'Z') break;
      n = n * 26 + c - 'A' + 1;
    }
    return n - 1;
  }

  public static int suggest(List<String> headers, int field) {
    String[][] names = {
      {"SICIL", "SICILNO", "SICILNUMARASI", "PERSONELNO", "PERSONELNUMARASI"},
      {"AD", "ADI", "ISIM", "ADSOYAD", "ADISOYADI"},
      {"SOYAD", "SOYADI"},
      {"TELEFON", "TELEFONNO", "TELEFONNUMARASI", "CEPTELEFONU", "GSM", "TEL", "CEP"},
      {"BIRIM", "DEPARTMAN", "SUBE"},
      {"UNVAN", "GOREV"},
      {"EPOSTA", "EMAIL"},
      {"ACIKLAMA", "NOT"},
      {"DURUM"}
    };
    for (int c = 0; c < headers.size(); c++) {
      String h = fold(headers.get(c)).replaceAll("[^A-Z0-9]", "");
      for (String n : names[field]) if (h.equals(n)) return c;
    }
    return -1;
  }

  static String xml(String s) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c < 32 && c != '\n' && c != '\r' && c != '\t') continue;
      switch (c) {
        case '&':
          out.append("&amp;");
          break;
        case '<':
          out.append("&lt;");
          break;
        case '>':
          out.append("&gt;");
          break;
        case '"':
          out.append("&quot;");
          break;
        default:
          out.append(c);
      }
    }
    return out.toString();
  }

  static void entry(ZipOutputStream z, String path, String text) throws IOException {
    z.putNextEntry(new ZipEntry(path));
    z.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    z.closeEntry();
  }

  static String col(int c) {
    String s = "";
    for (int n = c + 1; n > 0; n = (n - 1) / 26) s = (char) ('A' + (n - 1) % 26) + s;
    return s;
  }

  public static void export(OutputStream stream, Collection<Person> people) throws IOException {
    try (ZipOutputStream z = new ZipOutputStream(stream)) {
      entry(
          z,
          "[Content_Types].xml",
          "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default"
              + " Extension=\"rels\""
              + " ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default"
              + " Extension=\"xml\" ContentType=\"application/xml\"/><Override"
              + " PartName=\"/xl/workbook.xml\""
              + " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override"
              + " PartName=\"/xl/worksheets/sheet1.xml\""
              + " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
      entry(
          z,
          "_rels/.rels",
          "<Relationships"
              + " xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship"
              + " Id=\"rId1\""
              + " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\""
              + " Target=\"xl/workbook.xml\"/></Relationships>");
      entry(
          z,
          "xl/workbook.xml",
          "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
              + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet"
              + " name=\"Kişiler\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
      entry(
          z,
          "xl/_rels/workbook.xml.rels",
          "<Relationships"
              + " xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship"
              + " Id=\"rId1\""
              + " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\""
              + " Target=\"worksheets/sheet1.xml\"/></Relationships>");
      z.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
      Writer w = new OutputStreamWriter(z, java.nio.charset.StandardCharsets.UTF_8);
      w.write(
          "<worksheet"
              + " xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
      LinkedHashSet<String> keys = new LinkedHashSet<>();
      for (Person p : people) keys.addAll(p.extra.keySet());
      if (keys.size() > 119)
        throw new IOException("Özel alanlar toplamı 119 sütunu aşıyor. Alan adlarını birleştirin.");
      List<String> headers =
          new ArrayList<>(
              Arrays.asList(
                  "Sicil Numarası",
                  "Ad",
                  "Soyad",
                  "Telefon Numarası",
                  "Birim",
                  "Unvan",
                  "E-posta",
                  "Açıklama",
                  "Durum"));
      headers.addAll(keys);
      writeRow(w, 1, headers);
      int r = 2;
      for (Person p : people) {
        List<String> cells =
            new ArrayList<>(
                Arrays.asList(
                    p.sicil, p.ad, p.soyad, p.telefon, p.birim, p.unvan, p.email, p.not, p.durum));
        for (String key : keys) cells.add(p.extra.getOrDefault(key, ""));
        writeRow(w, r++, cells);
      }
      w.write("</sheetData></worksheet>");
      w.flush();
      z.closeEntry();
    }
  }

  static void writeRow(Writer w, int number, List<String> cells) throws IOException {
    w.write("<row r=\"" + number + "\">");
    for (int c = 0; c < cells.size(); c++)
      w.write(
          "<c r=\""
              + col(c)
              + number
              + "\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"
              + xml(cells.get(c))
              + "</t></is></c>");
    w.write("</row>");
  }
}
