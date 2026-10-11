package com.semih.ilacim;

import static org.junit.Assert.*;

import java.time.*;
import java.util.*;
import org.junit.Test;

public class RulesTest {
  private String decode(com.google.zxing.BarcodeFormat format, String text) throws Exception {
    com.google.zxing.common.BitMatrix bits = new com.google.zxing.MultiFormatWriter().encode(text, format, 400, 200);
    final int width=bits.getWidth()+80, height=bits.getHeight()+80;
    final byte[] pixels = new byte[width * height];Arrays.fill(pixels,(byte)255);
    for (int y = 0; y < bits.getHeight(); y++) for (int x = 0; x < bits.getWidth(); x++) pixels[(y+40) * width + x+40] = (byte)(bits.get(x, y) ? 0 : 255);
    com.google.zxing.LuminanceSource source = new com.google.zxing.LuminanceSource(width, height) {
      public byte[] getMatrix() { return pixels; }
      public byte[] getRow(int y, byte[] row) { return Arrays.copyOfRange(pixels, y * getWidth(), (y + 1) * getWidth()); }
    };
    return new com.google.zxing.MultiFormatReader().decode(new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source))).getText();
  }
  @Test public void decodesActualEanBarcode() throws Exception { assertEquals("4006381333931", decode(com.google.zxing.BarcodeFormat.EAN_13,"4006381333931")); }
  @Test public void decodesActualMedicineMatrix() throws Exception { assertEquals("04006381333931", Rules.barcode(decode(com.google.zxing.BarcodeFormat.DATA_MATRIX,"01040063813339311728123121TESTSERIAL"))); }
  @Test
  public void halfTabletExactStock() {
    assertEquals(500, Rules.quantity("0,5"));
    assertEquals(1250, Rules.quantity("1.25"));
    assertEquals(20000 - 500, Rules.quantity("20") - Rules.quantity("0.5"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeQuantity() {
    Rules.quantity("-1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void excessivePrecision() {
    Rules.quantity("0.0001");
  }

  @Test
  public void monthIncludesLastDay() {
    assertFalse(Rules.expired("12/2026", LocalDate.of(2026, 12, 31)));
    assertTrue(Rules.expired("12/2026", LocalDate.of(2027, 1, 1)));
  }

  @Test
  public void leapYearExpiry() {
    assertEquals(LocalDate.of(2028, 2, 29), Rules.expiry("02/2028").atEndOfMonth());
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidMonth() {
    Rules.expiry("13/2026");
  }

  @Test(expected = IllegalArgumentException.class)
  public void ambiguousExpiry() {
    Rules.expiry("2/26");
  }

  @Test
  public void sortedUniqueTimes() {
    assertEquals(Arrays.asList("08:00", "20:00"), Rules.times("20:00, 08:00, 08:00"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidTime() {
    Rules.times("24:00");
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyTime() {
    Rules.times("08:00,");
  }

  @Test
  public void gs1ScannerPrefix() {
    assertEquals("08691234567890", Rules.barcode("]d201086912345678901726123121SERIAL"));
  }

  @Test
  public void gs1HumanReadable() {
    assertEquals("08691234567890", Rules.barcode("(01)08691234567890(17)261231"));
  }

  @Test
  public void validBarcodeCheckDigit() {
    assertTrue(Rules.validGtin("4006381333931"));
    assertFalse(Rules.validGtin("4006381333932"));
  }

  @Test
  public void doseSlotSeparatesDatesAndTimes() {
    assertNotEquals(
        Rules.slot("p", LocalDate.of(2026, 12, 1), "08:00"),
        Rules.slot("p", LocalDate.of(2026, 12, 2), "08:00"));
    assertNotEquals(
        Rules.slot("p", LocalDate.of(2026, 12, 1), "08:00"),
        Rules.slot("p", LocalDate.of(2026, 12, 1), "20:00"));
  }
}
