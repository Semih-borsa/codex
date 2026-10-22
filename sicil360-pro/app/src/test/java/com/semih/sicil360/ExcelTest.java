package com.semih.sicil360;

import static com.semih.sicil360.Model.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.junit.Test;

public class ExcelTest {
  File file(byte[] bytes) throws Exception {
    File f = File.createTempFile("sicil-test-", ".xlsx");
    f.deleteOnExit();
    try (FileOutputStream out = new FileOutputStream(f)) {
      out.write(bytes);
    }
    return f;
  }

  byte[] book(String sheet, String styles, String shared) throws Exception {
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    try (ZipOutputStream z = new ZipOutputStream(b)) {
      Excel.entry(z, "[Content_Types].xml", "<Types/>");
      Excel.entry(
          z,
          "xl/workbook.xml",
          "<workbook"
              + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet"
              + " name=\"Kişiler\" r:id=\"r1\"/></sheets></workbook>");
      Excel.entry(
          z,
          "xl/_rels/workbook.xml.rels",
          "<Relationships><Relationship Id=\"r1\""
              + " Target=\"worksheets/sheet1.xml\"/></Relationships>");
      Excel.entry(z, "xl/worksheets/sheet1.xml", sheet);
      if (styles != null) Excel.entry(z, "xl/styles.xml", styles);
      if (shared != null) Excel.entry(z, "xl/sharedStrings.xml", shared);
    }
    return b.toByteArray();
  }

  @Test
  public void exportRoundTripPreservesZerosTurkishAndLiteralFormulaStrings() throws Exception {
    Person p = new Person();
    p.sicil = "000123";
    p.ad = "İpek";
    p.soyad = "Işık";
    p.telefon = "05321234567";
    p.not = "=HYPERLINK(\"bad\")";
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    Excel.export(b, Arrays.asList(p));
    File f = file(b.toByteArray());
    Excel.Table t = Excel.load(f, Excel.sheets(f).get(0));
    assertEquals("000123", t.value(2, 0));
    assertEquals("05321234567", t.value(2, 3));
    assertEquals("İpek", t.value(2, 1));
    assertEquals(p.not, t.value(2, 7));
    assertFalse(t.rows.get(2).get(7).formula);
  }

  @Test
  public void numericMasksPreserveZeroPaddingAndScientificNumbers() throws Exception {
    String styles =
        "<styleSheet><numFmts><numFmt numFmtId=\"164\" formatCode=\"000000\"/><numFmt"
            + " numFmtId=\"165\" formatCode=\"00000000000\"/></numFmts><cellXfs><xf"
            + " numFmtId=\"164\"/><xf numFmtId=\"165\"/></cellXfs></styleSheet>";
    File f =
        file(
            book(
                "<worksheet><sheetData><row r=\"2\"><c r=\"A2\" s=\"0\"><v>123</v></c><c r=\"D2\""
                    + " s=\"1\"><v>5.321234567E9</v></c></row></sheetData></worksheet>",
                styles,
                null));
    Excel.Table t = Excel.load(f, Excel.sheets(f).get(0));
    assertEquals("000123", t.value(2, 0));
    assertEquals("05321234567", t.value(2, 3));
  }

  @Test
  public void formulaIdentityIsFlaggedAndFirstDataRowNotSkipped() throws Exception {
    Person p = new Person();
    p.sicil = "01";
    p.ad = "Test";
    p.telefon = "05321234567";
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    Excel.export(b, Arrays.asList(p));
    File good = file(b.toByteArray());
    Excel.Table table = Excel.load(good, Excel.sheets(good).get(0));
    int[] mapping = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    assertEquals(1, table.mapped(1, mapping, false).size());
    table.rows.get(2).get(0).formula = true;
    assertFalse(table.mapped(1, mapping, false).get(0).error.isEmpty());
  }

  @Test
  public void sharedStringsRichTextAndSparseColumns() throws Exception {
    File f =
        file(
            book(
                "<worksheet><sheetData><row r=\"7\"><c r=\"C7\""
                    + " t=\"s\"><v>0</v></c></row></sheetData></worksheet>",
                null,
                "<sst><si><r><t>İpek </t></r><r><t>Işık</t></r></si></sst>"));
    Excel.Table t = Excel.load(f, Excel.sheets(f).get(0));
    assertEquals("İpek Işık", t.value(7, 2));
    assertEquals("", t.value(7, 0));
  }

  @Test
  public void corruptAndDtdInputRejected() throws Exception {
    try {
      Excel.sheets(file("not zip".getBytes("UTF-8")));
      fail();
    } catch (Exception expected) {
    }
    File f =
        file(
            book(
                "<!DOCTYPE worksheet [<!ENTITY bad SYSTEM"
                    + " 'file:///etc/passwd'>]><worksheet>&bad;</worksheet>",
                null,
                null));
    try {
      Excel.load(f, Excel.sheets(f).get(0));
      fail();
    } catch (Exception expected) {
    }
  }

  @Test
  public void misleadingHeadersSuggestedSafely() {
    List<String> h = Arrays.asList("SİCİL NO", "ADI", "SOYADI", "Cep Telefonu", "BİRİM");
    for (int i = 0; i < 5; i++) assertEquals(i, Excel.suggest(h, i));
    assertEquals(-1, Excel.suggest(h, 6));
  }

  @Test
  public void blankRowsNotImported() throws Exception {
    Excel.Table t = new Excel.Table();
    t.rows.put(1, new TreeMap<>());
    t.rows.put(2, new TreeMap<>());
    assertTrue(t.mapped(1, new int[] {0, 1, 2, 3, -1, -1, -1, -1, -1}, false).isEmpty());
  }

  @Test
  public void oversizeAndTooManyColumnsRejected() throws Exception {
    File f = File.createTempFile("sicil-large-", ".xlsx");
    try (RandomAccessFile out = new RandomAccessFile(f, "rw")) {
      out.setLength(Excel.MAX_FILE + 1L);
    }
    try {
      Excel.sheets(f);
      fail();
    } catch (IOException expected) {
    } finally {
      f.delete();
    }
    File columns =
        file(
            book(
                "<worksheet><sheetData><row r=\"1\"><c"
                    + " r=\"ZZ1\"><v>1</v></c></row></sheetData></worksheet>",
                null,
                null));
    try {
      Excel.load(columns, Excel.sheets(columns).get(0));
      fail();
    } catch (Exception expected) {
    }
  }

  @Test
  public void twentyThousandRowWorkbookRoundTrip() throws Exception {
    List<Person> people = new ArrayList<>();
    for (int n = 0; n < 20000; n++) {
      Person p = new Person();
      p.sicil = String.format(Locale.ROOT, "%06d", n);
      p.ad = "Örnek";
      p.soyad = "Personel";
      p.telefon = "05320000000";
      people.add(p);
    }
    long started = System.nanoTime();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Excel.export(out, people);
    File f = file(out.toByteArray());
    Excel.Table table = Excel.load(f, Excel.sheets(f).get(0));
    assertEquals("000000", table.value(2, 0));
    assertEquals("019999", table.value(20001, 0));
    assertEquals(20000, table.mapped(1, new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8}, false).size());
    System.out.println(
        "20000 XLSX rows write/read/map: " + ((System.nanoTime() - started) / 1000000) + " ms");
  }
}
