package com.semih.sicil360;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

public class PaymentsTest {
  Payments.Account account(String code, long original) {
    Payments.Account a = new Payments.Account();
    a.code = code;
    a.first = "İpek";
    a.last = "Işık";
    a.original = original;
    return a;
  }

  Payments.Ledger ledger() {
    Payments.Ledger l = new Payments.Ledger();
    l.name = "Ekim";
    l.accounts.put("0001", account("0001", 100000));
    l.accounts.put("0002", account("0002", 100000));
    return l;
  }

  void cell(Excel.Table t, int row, int col, String value, boolean numeric) {
    Excel.Cell c = new Excel.Cell();
    c.value = value;
    c.numeric = numeric;
    t.rows.computeIfAbsent(row, k -> new TreeMap<>()).put(col, c);
  }

  Excel.Table table() {
    Excel.Table t = new Excel.Table();
    String[] h = {"Sıra No", "Cari Kodu", "Ticari Unvanı", "Adı", "Soyadı", "Bakiye"};
    for (int c = 0; c < 6; c++) cell(t, 1, c, h[c], false);
    cell(t, 2, 0, "1", true);
    cell(t, 2, 1, "0001", false);
    cell(t, 2, 2, "Birim", false);
    cell(t, 2, 3, "İpek", false);
    cell(t, 2, 4, "Işık", false);
    cell(t, 2, 5, "1234.56", true);
    return t;
  }

  @Test
  public void exactTurkishAndNumericAmounts() {
    assertEquals(123456, Payments.money("1.234,56 TL", false));
    assertEquals(123456, Payments.money("1234.56", true));
    assertEquals(123400, Payments.money("1.234", false));
    assertEquals(123400, Payments.money("1234", true));
    assertEquals(30, Payments.money("0,10", false) + Payments.money("0,20", false));
    assertEquals(100, Payments.money("1.000", true));
  }

  @Test
  public void rejectsNegativeAmbiguousAndExcessPrecision() {
    for (String s : new String[] {"-1", "1,234", "1.23.4", "NaN", "1e100000000", "", "1,00,00"}) {
      try {
        Payments.money(s, false);
        fail(s);
      } catch (IllegalArgumentException expected) {
      }
    }
    try {
      Payments.money("0.001", true);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void partialPaymentLeavesOriginalAndUnpaidPeople() {
    Payments.Ledger l = ledger();
    Payments.add(l.accounts.get("0001"), 25000, "");
    Payments.Totals t = Payments.totals(l);
    assertEquals(200000, t.original);
    assertEquals(25000, t.paid);
    assertEquals(175000, t.remaining);
    assertEquals(75000, l.accounts.get("0001").remaining());
    assertEquals(0, t.closed);
  }

  @Test
  public void fullPaymentKeepsSameRecordAndOrder() {
    Payments.Ledger l = ledger();
    Payments.Account a = l.accounts.get("0001");
    Payments.add(a, a.remaining(), "");
    assertEquals(0, a.remaining());
    assertEquals(100000, a.original);
    assertSame(a, l.accounts.get("0001"));
    assertEquals(Arrays.asList("0001", "0002"), new ArrayList<>(l.accounts.keySet()));
    assertEquals(1, Payments.totals(l).closed);
  }

  @Test
  public void overpaymentNeverOffsetsAnotherPerson() {
    Payments.Ledger l = ledger();
    Payments.add(l.accounts.get("0001"), 110000, "");
    Payments.Totals t = Payments.totals(l);
    assertEquals(200000, t.original);
    assertEquals(110000, t.paid);
    assertEquals(100000, t.remaining);
    assertEquals(10000, t.excess);
    assertEquals(t.original, t.paid + t.remaining - t.excess);
  }

  @Test
  public void cancelPreservesHistoryAndReopensDebt() {
    Payments.Account a = account("01", 10000);
    Payments.add(a, 12000, "Fazla ödeme");
    a.payments.get(0).cancelled = true;
    assertEquals(10000, a.remaining());
    assertEquals(0, a.excess());
    assertEquals(1, a.payments.size());
    assertEquals(0, a.paid());
  }

  @Test
  public void paymentBounds() {
    Payments.Account a = account("01", 10000);
    for (long n : new long[] {0, -1, Payments.MAX + 1})
      try {
        Payments.add(a, n, "");
        fail();
      } catch (IllegalArgumentException expected) {
      }
    Payments.add(a, Payments.MAX, "");
    try {
      Payments.add(a, 1, "");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void firstDataRowAndZeroCodeAndOptionalPhone() throws Exception {
    Payments.Ledger l = Payments.read(table());
    assertEquals(1, l.accounts.size());
    assertEquals("İpek Işık", l.accounts.get("0001").name());
    assertEquals(123456, l.accounts.get("0001").original);
  }

  @Test
  public void duplicateCodesRejectedInsteadOfMerged() throws Exception {
    Excel.Table t = table();
    t.rows.put(3, new TreeMap<>(t.rows.get(2)));
    try {
      Payments.read(t);
      fail();
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("Mükerrer"));
    }
  }

  @Test
  public void summaryRowsNotPeopleAndMismatchRejected() throws Exception {
    Excel.Table t = table();
    cell(t, 4, 0, "GENEL TOPLAM", false);
    cell(t, 4, 5, "1.234,56", false);
    assertEquals(1, Payments.read(t).accounts.size());
    cell(t, 4, 5, "1,00", false);
    try {
      Payments.read(t);
      fail();
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("Toplam"));
    }
  }

  @Test
  public void formulasAndUnsafeCodesRejected() throws Exception {
    for (int col : new int[] {1, 5}) {
      Excel.Table t = table();
      t.rows.get(2).get(col).formula = true;
      try {
        Payments.read(t);
        fail();
      } catch (IllegalArgumentException expected) {
      }
    }
    Excel.Table t = table();
    t.rows.get(2).get(1).unsafe = true;
    try {
      Payments.read(t);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void ledgerSurvivesCodecAndEncryptedBackup() throws Exception {
    Model.State s = new Model.State();
    Payments.Ledger l = ledger();
    Payments.add(l.accounts.get("0001"), 120000, "Not");
    l.accounts.get("0001").payments.get(0).cancelled = true;
    Payments.add(l.accounts.get("0001"), 30000, "");
    s.debts.add(l);
    byte[] bytes = Backup.seal(Codec.encode(s), "ornek-sifre-123".toCharArray());
    Model.State copy = Codec.decode(Backup.open(bytes, "ornek-sifre-123".toCharArray()));
    assertEquals(30000, copy.debts.get(0).accounts.get("0001").paid());
    assertEquals(200000, Payments.totals(copy.debts.get(0)).original);
    assertEquals(2, copy.debts.get(0).accounts.get("0001").payments.size());
  }

  @Test
  public void oldV2PayloadStillLoads() throws Exception {
    byte[] current = Codec.encode(new Model.State());
    byte[] old = Arrays.copyOf(current, current.length - 4);
    old[3] = 0x32;
    assertTrue(Codec.decode(old).debts.isEmpty());
  }

  @Test
  public void duplicateFingerprintIgnoresRowOrderButNotBalances() throws Exception {
    Excel.Table t = table();
    Payments.Ledger first = Payments.read(t);
    Excel.Table b = table();
    b.rows.put(5, b.rows.remove(2));
    assertEquals(first.fingerprint, Payments.read(b).fingerprint);
    cell(b, 5, 5, "100", true);
    assertNotEquals(first.fingerprint, Payments.read(b).fingerprint);
  }

  Model.State months(int... offsets) {
    Model.State s = new Model.State();
    for (int offset : offsets) {
      Payments.Ledger l = ledger();
      l.period = java.time.YearMonth.of(2026, 10).minusMonths(offset).toString();
      s.debts.add(l);
    }
    return s;
  }

  @Test
  public void threeConsecutiveMonthsWarnOnlyWhenNoPaymentEntered() {
    Model.State s = months(0, 1, 2);
    Payments.Trend t = Payments.trend(s, "0001", java.time.YearMonth.of(2026, 10));
    assertEquals(3, t.appearances);
    assertEquals(3, t.unpaidStreak);
    assertEquals(0, t.percent);
    Payments.add(s.debts.get(1).accounts.get("0001"), 100, "");
    assertEquals(1, Payments.trend(s, "0001", java.time.YearMonth.of(2026, 10)).unpaidStreak);
  }

  @Test
  public void missingMonthDoesNotInventNonpayment() {
    Model.State s = months(0, 2, 4);
    Payments.Trend t = Payments.trend(s, "0001", java.time.YearMonth.of(2026, 10));
    assertEquals(2, t.appearances);
    assertEquals(1, t.unpaidStreak);
  }

  @Test
  public void coverageIsDebtWeightedAndOverpaymentCapped() {
    Model.State s = months(0, 1, 2);
    for (Payments.Ledger l : s.debts) Payments.add(l.accounts.get("0001"), 80000, "");
    assertEquals(80, Payments.trend(s, "0001", java.time.YearMonth.of(2026, 10)).percent);
    Payments.add(s.debts.get(0).accounts.get("0001"), 90000, "");
    assertEquals(87, Payments.trend(s, "0001", java.time.YearMonth.of(2026, 10)).percent);
  }

  @Test
  public void differentCariCodesAndFutureListsDoNotContaminateHistory() {
    Model.State s = months(0, 1, 2, -1);
    Payments.add(s.debts.get(0).accounts.get("0002"), 100000, "");
    Payments.Trend t = Payments.trend(s, "0001", java.time.YearMonth.of(2026, 10));
    assertEquals(3, t.appearances);
    assertEquals(300000, t.original);
    assertEquals(0, t.percent);
    assertEquals(0, Payments.trend(s, "missing", java.time.YearMonth.of(2026, 10)).appearances);
  }

  @Test
  public void periodIsPreservedInBackup() throws Exception {
    Model.State s = months(4);
    assertEquals("2026-06", Codec.copy(s).debts.get(0).period);
  }

  @Test
  public void editKeepsPaymentHistoryOrderAndRecalculatesTotals() {
    Model.State s = new Model.State();
    Payments.Ledger l = ledger();
    s.debts.add(l);
    Payments.add(l.accounts.get("0001"), 25000, "Not");
    Payments.refreshFingerprint(l);
    String before = l.fingerprint;
    Payments.edit(s, l.id, "0001", "0003", "Yeni", "İsim", "Yeni Birim", 150000);
    assertEquals(Arrays.asList("0003", "0002"), new ArrayList<>(l.accounts.keySet()));
    assertEquals(25000, l.accounts.get("0003").paid());
    assertEquals(125000, l.accounts.get("0003").remaining());
    assertEquals("Not", l.accounts.get("0003").payments.get(0).note);
    assertEquals(250000, Payments.totals(l).original);
    assertNotEquals(before, l.fingerprint);
  }

  @Test
  public void duplicateCodeEditIsRejectedWithoutChangingAccount() {
    Model.State s = new Model.State();
    Payments.Ledger l = ledger();
    s.debts.add(l);
    try {
      Payments.edit(s, l.id, "0001", "0002", "Yeni", "İsim", "", 100);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    assertEquals("İpek", l.accounts.get("0001").first);
    assertEquals(100000, l.accounts.get("0001").original);
  }

  @Test
  public void removePersonOnlyAffectsSelectedMonthAndRefreshesTotals() {
    Model.State s = months(0, 1);
    Payments.Ledger selected = s.debts.get(0);
    Payments.add(selected.accounts.get("0001"), 25000, "");
    Payments.removeAccount(s, selected.id, "0001");
    assertEquals(100000, Payments.totals(selected).original);
    assertEquals(0, Payments.totals(selected).paid);
    assertTrue(s.debts.get(1).accounts.containsKey("0001"));
    assertEquals(1, Payments.trend(s, "0001", java.time.YearMonth.of(2026, 10)).appearances);
  }

  @Test
  public void ledgerPeriodCannotCreateSamePeriodDuplicate() {
    Model.State s = months(0, 1);
    Payments.Ledger second = s.debts.get(1);
    try {
      Payments.editLedger(s, second.id, "Test", "2026-10");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    assertEquals("2026-09", second.period);
    Payments.editLedger(s, second.id, "Düzeltilmiş", "2026-08");
    assertEquals("Düzeltilmiş", second.name);
  }

  @Test
  public void deleteWholeLedgerLeavesOtherMonthIntact() throws Exception {
    Model.State s = months(0, 1);
    String other = s.debts.get(1).id;
    Payments.removeLedger(s, s.debts.get(0).id);
    assertEquals(1, s.debts.size());
    assertEquals(other, Codec.copy(s).debts.get(0).id);
  }

  @Test
  public void allAccountsCanBeDeletedAndEmptyLedgerStillLoads() throws Exception {
    Model.State s = new Model.State();
    Payments.Ledger l = ledger();
    s.debts.add(l);
    Payments.removeAccount(s, l.id, "0001");
    Payments.removeAccount(s, l.id, "0002");
    assertEquals(0, Payments.totals(l).original);
    assertTrue(Codec.copy(s).debts.get(0).accounts.isEmpty());
  }
}
