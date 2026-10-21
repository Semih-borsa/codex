import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import crypto from 'node:crypto';
import { createHarness, sourcePath, sourceHash } from './source-harness.mjs';

// Expectations are business acceptance criteria, not a second calculation engine.
const copy = value => JSON.parse(JSON.stringify(value));
const income = (tutar = 20000, tarih = '2026-10-15') => ({ id: 'income', tip: 'gelir', kategori: 'Maaş', tutar, tarih });
const loan = (odendi = false) => ({ id: 'loan', ad: 'İhtiyaç kredisi', vade: 1, taksitler: [{ no: 1, tarih: '2026-10-20', tutar: 5000, odendi }] });
const card = { id: 'card', isim: 'World', ekstre: 0, bakiyedenDussun: true };
const cardRow = (tarih = '2026-10-16') => ({ id: 'snapshot', tip: 'gider', kategori: 'World', kartHarcama: true, tutar: 10000, donemIci: 8000, provizyon: 2000, tarih, kartGunBaslangicToplam: 10000, kartGunBaslangicTarih: tarih, kartGunlukHareketler: {} });

test('S01 — Bekleyen 5.000 TL kredi taksiti 20.000 TL bakiyeden 15.000 TL kullanılabilir bırakmalı', () => {
  const h = createHarness({ state: { islemler: [income()], krediler: [loan()] } });
  assert.equal(h.balance().gercekBakiye, 20000);
  assert.equal(h.balance().odemelerSonrasi, 15000);
});

test('S02a — Gerçek kredi ödeme işleyicisi 5.000 TL nakit çıkışını bir kez kaydetmeli', () => {
  const h = createHarness({ state: { islemler: [income()], krediler: [loan()] } });
  h.callback('KredilerTab', 'taksitToggle', 'loan', 1);
  assert.equal(h.context.krediler[0].taksitler[0].odendi, true);
  assert.equal(h.balance().gercekBakiye, 15000);
  assert.equal(h.balance().odemelerSonrasi, 15000);
});

test('S02b — Normal ödemeye bağlanmış tutar hem borç hem nakit çıkışı olarak iki kez düşmemeli', () => {
  const h = createHarness({ state: {
    islemler: [income(), { id: 'bill-entry', tip: 'gider', kategori: 'Kredi', tutar: 5000, tarih: '2026-10-20', kaynak: 'odeme', odemeId: 'bill' }],
    odemeler: [{ id: 'bill', islemId: 'bill-entry', kategori: 'Kredi', tutar: 5000, tarih: '2026-10-20', odendi: true, odendiTarih: '2026-10-16T12:00:00+03:00' }], krediler: [loan(true)],
  } });
  assert.equal(h.balance().gercekBakiye, 15000);
  assert.equal(h.balance().odemelerSonrasi, 15000);
});

test('S03a — Kredi ödeme durumunu geri alma mevcut takip bayrağını geri döndürmeli', () => {
  const h = createHarness({ state: { krediler: [loan(true)] } });
  h.callback('KredilerTab', 'taksitToggle', 'loan', 1);
  assert.equal(h.context.krediler[0].taksitler[0].odendi, false);
});

test('S03b — Krediyle ilişkilendirilmiş ödeme geri alınınca nakit iade edilmeli ve taksit yeniden ayrılmalı', () => {
  // Future integration contract: explicit linkage is supplied; legacy independent payments must NOT be guessed.
  const h = createHarness({ state: {
    islemler: [income(), { id: 'loan-payment', tip: 'gider', kategori: 'Kredi', tutar: 5000, tarih: '2026-10-16', kaynak: 'kredi_taksiti', krediId: 'loan', taksitNo: 1 }],
    krediler: [loan(true)],
  } });
  assert.equal(h.balance().gercekBakiye, 15000);
  h.callback('KredilerTab', 'taksitToggle', 'loan', 1);
  assert.equal(h.context.krediler[0].taksitler[0].odendi, false);
  assert.equal(h.balance().gercekBakiye, 20000);
  assert.equal(h.balance().odemelerSonrasi, 15000);
});

test('S04a — Maaş dönemi değişince ödenmemiş 10.000 TL kart borcu korunmalı', () => {
  const h = createHarness();
  const rows = [cardRow('2026-10-14')];
  assert.equal(h.call('kartToplamBorcHesapla', card, rows, [], '2026-8-15', 15), 10000);
  assert.equal(h.call('kartToplamBorcHesapla', card, rows, [], '2026-9-15', 15), 10000);
});

test('S04b — Önceki dönemden kalan kart dönem içi borcu yeni dönemin kullanılabilir tutarını da sınırlamalı', () => {
  const h = createHarness({ state: { islemler: [income(), cardRow('2026-10-14')], kartlar: [card] } });
  assert.equal(h.balance().gercekBakiye, 20000);
  assert.equal(h.balance().odemelerSonrasi, 10000);
});

test('S04c — Önceki dönemin ödenmemiş ekstresi ayrılan tutarda korunuyor', () => {
  const h = createHarness({ state: { islemler: [income()], kartlar: [{ ...card, ekstre: 10000, ekstreDonemKey: 'old-cycle', sonOdemeTarihi: '2026-10-14' }] } });
  assert.equal(h.balance().ekstreBekleyen, 10000);
  assert.equal(h.balance().odemelerSonrasi, 10000);
});

test('S05 — Kart alışverişi harcama, ekstre ödemesi yalnız nakit çıkışı olmalı', () => {
  const h = createHarness();
  const row = { ...cardRow(), tutar: 10500, donemIci: 8500, kartGunlukHareketler: { '2026-10-16': 500 } };
  const payment = [{ id: 'paid', kartId: 'card', kartAdi: 'World', tutar: 500, tarih: '2026-10-16' }];
  const before = h.call('gunlukFinansHareketleri', [row], [], [], '2026-10-15', '2026-11-14', '2026-10-16', 'harcama');
  const after = h.call('gunlukFinansHareketleri', [row], [], payment, '2026-10-15', '2026-11-14', '2026-10-16', 'harcama');
  assert.deepEqual(copy(after), copy(before));
  assert.equal(after.reduce((n, i) => n + i.tutar, 0), 500);
  const cash = h.call('gunlukFinansHareketleri', [row], [], payment, '2026-10-15', '2026-11-14', '2026-10-16', 'nakit');
  assert.equal(cash.length, 1);
  assert.equal(cash[0].kaynak, 'ekstre_odemesi');
  assert.equal(cash[0].tutar, 500);
});

test('S06a — Provizyondaki 2.000 TL kesinleşince toplam aynıysa yeni harcama oluşmamalı', () => {
  const h = createHarness();
  const updated = h.call('kartKaydiniGuncelle', [cardRow()], { donemIci: 10000, provizyon: 0 }, '2026-10-17');
  assert.equal(updated.tutar, 10000);
  assert.equal(h.call('kartAnalizHareketleri', updated).length, 0);
});

test('S06b — Aynı gün iki güncelleme sonunda yalnız yeni 500 TL bir kez sayılmalı', () => {
  const h = createHarness();
  let row = h.call('kartKaydiniGuncelle', [cardRow()], { donemIci: 8500, provizyon: 2000 }, '2026-10-17');
  row = h.call('kartKaydiniGuncelle', [row], { donemIci: 10500, provizyon: 0 }, '2026-10-17');
  const movements = h.call('kartAnalizHareketleri', row);
  assert.equal(movements.length, 1);
  assert.equal(movements[0].tutar, 500);
});

test('S06c — Dönem içinin ekstreye taşınması yeni alışveriş üretmemeli', () => {
  const h = createHarness();
  const updated = h.call('kartKaydiniGuncelle', [cardRow()], { donemIci: 0, provizyon: 0 }, '2026-10-17', 10000);
  assert.equal(h.call('kartAnalizHareketleri', updated).length, 0);
});

test('S07a — Kart taksit kaydı gelecekteki ekstre dönemlerine tarihli plan sağlamalı [eksik yetenek]', () => {
  const h = createHarness({ state: { form: { ad: 'Üç taksitli alışveriş', aylikTutar: '1000', vade: '3', odenen: '0', kart: 'World' } } });
  h.callback('TaksitlerTab', 'kaydet');
  assert.equal(h.context.taksitler.length, 1);
  assert.ok(Array.isArray(h.context.taksitler[0].taksitler), 'Mevcut kayıt yalnız ad/tutar/vade/ödenen/kart içeriyor; tarihli ekstre dağıtımı bulunmuyor.');
});

test('S07b — Mevcut kredi taksit planı kısa ayı ve kuruş toplamını korumalı [kontrol]', () => {
  const h = createHarness();
  const rows = h.callback('KredilerTab', 'taksitleriOlustur', { toplamTutar: '100', vade: '3', baslangic: '2027-01-31', aylikTaksit: '', ilkFarkli: false, sonFarkli: false });
  assert.deepEqual(copy(rows.map(i => i.tarih)), ['2027-01-31', '2027-02-28', '2027-03-31']);
  assert.equal(rows.reduce((n, i) => n + Math.round(i.tutar * 100), 0), 10000);
});

test('S08a — Yeni kart güncellemesi eski kayıt nesnesini yerinde değiştirmemeli', () => {
  const h = createHarness();
  const old = cardRow('2026-10-14'), before = JSON.stringify(old);
  const updated = h.call('kartKaydiniGuncelle', [old], { donemIci: 9000, provizyon: 2000 }, '2026-10-16');
  assert.equal(JSON.stringify(old), before);
  assert.notEqual(updated, old);
});

test('S08b — Bugünkü ekstre değişikliği geçmiş dönemin kart toplamını değiştirmemeli', () => {
  const h = createHarness();
  const rows = [cardRow('2026-10-14')], k = { ...card, ekstre: 3000, ekstreDonemKey: 'old-cycle' };
  const historic = h.call('kartToplamBorcHesapla', k, rows, [], '2026-8-15', 15);
  k.ekstre = 7000; k.ekstreDonemKey = 'new-cycle';
  assert.equal(h.call('kartToplamBorcHesapla', k, rows, [], '2026-8-15', 15), historic);
});

for (const format of ['butce-takip-v1', 'finora-v1', 'finora-v2']) {
  test(`S09 — ${format} yedeği kişisel kategori ve ödeme bağlarını koruyarak okunmalı`, () => {
    const h = createHarness();
    const packet = { format, data: {
      ft_islemler: [{ id: 'custom', tip: 'gider', kategori: 'Ediz basketbol', tutar: '123,45', tarih: '2026-10-16', kaynak: 'odeme', odemeId: 'bill' }],
      ft_odemeler: [{ id: 'bill', islemId: 'custom', kategori: 'Ediz basketbol', tutar: '123,45', tarih: '2026-10-16', odendi: false }],
      ft_kategoriler: { gelir: ['Sınav ücreti'], gider: ['Ediz basketbol', 'Bilge telefon faturası'] },
    } };
    const before = JSON.stringify(packet), cleaned = h.call('yedekVerisiniDogrula', packet);
    assert.equal(cleaned.ft_islemler[0].tutar, 123.45);
    assert.equal(cleaned.ft_islemler[0].odemeId, 'bill');
    assert.equal(cleaned.ft_odemeler[0].islemId, 'custom');
    assert.deepEqual(copy(cleaned.ft_kategoriler), packet.data.ft_kategoriler);
    assert.equal(JSON.stringify(packet), before);
  });
}

test('S10a — Geçersiz tutarlı yedek reddedilmeli; mevcut veri ve yedek aynı kalmalı', () => {
  const h = createHarness({ storage: { ft_islemler: JSON.stringify([income()]), ft_manual_backup_latest: '{"sentinel":"old-backup"}' } });
  const before = h.storage.snapshot();
  h.context.bekleyenYedek = { format: 'finora-v2', data: { ft_islemler: [{ ...income(), tutar: 'bozuk' }] } };
  h.callback('AyarlarTab', 'yedekUygula');
  assert.deepEqual(h.storage.snapshot(), before);
  assert.equal(h.notices.some(([, type]) => type === 'hata'), true);
});

test('S10b — Eksik çekirdek veri içeren yedek mevcut işlemleri silmemeli', () => {
  const h = createHarness({ storage: { ft_islemler: JSON.stringify([income()]), ft_kategoriler: JSON.stringify({ gelir: ['Maaş'], gider: ['Ediz basketbol'] }) } });
  h.context.bekleyenYedek = { format: 'finora-v2', data: { ft_kartlar: [] } };
  h.callback('AyarlarTab', 'yedekUygula');
  assert.ok(h.storage.getItem('ft_auto_backup_restore_before_v2'), 'Gerçek işleyici yükleme öncesi kurtarma kopyası oluşturmalı.');
  assert.equal(h.storage.getItem('ft_islemler'), JSON.stringify([income()]));
});

test('S10c — Yedek yazılırken kota hatasında önceki kayıtlar ve kişisel kategoriler geri dönmeli', () => {
  const h = createHarness({ storage: { ft_islemler: JSON.stringify([income()]), ft_kartlar: '[]', ft_kategoriler: '{"gelir":["Maaş"],"gider":["Öykü kreş"]}', ft_manual_backup_latest: 'saved-backup' } });
  const before = h.storage.snapshot();
  h.storage.failOnce('ft_kartlar');
  assert.throws(() => h.call('yedegiGuvenleYaz', { ft_islemler: [], ft_kartlar: [] }, h.storage), /önceki kayıtlar geri yüklendi/);
  assert.deepEqual(h.storage.snapshot(), before);
});

test('S10d — Okunamayan yerel JSON varsayılan listeyle ezilmemeli', () => {
  const h = createHarness({ storage: { ft_islemler: '{bozuk-json' } });
  const [, set] = h.call('useLocalStorage', 'ft_islemler', []);
  assert.equal(set([income()]), false);
  assert.equal(h.storage.getItem('ft_islemler'), '{bozuk-json');
});

test('S10e — Bağlı iki kayıt yazılırken ikinci yazma hatası ilk yazmayı da geri almalı', () => {
  const h = createHarness({ storage: { ft_islemler: '[]', ft_odemeler: '[]' } });
  const before = h.storage.snapshot();
  const [, setRows] = h.call('useLocalStorage', 'ft_islemler', []);
  const [, setBills] = h.call('useLocalStorage', 'ft_odemeler', []);
  assert.equal(setRows([income()]), true);
  h.storage.failOnce('ft_odemeler');
  assert.equal(setBills([{ id: 'bill', tutar: 5000 }]), false);
  assert.deepEqual(h.storage.snapshot(), before);
});

test('S10f — Yinelenen kimlik veya geçersiz tarih içeren yedek reddedilmeli', () => {
  const h = createHarness();
  assert.throws(() => h.call('yedekVerisiniDogrula', { format: 'finora-v2', data: { ft_islemler: [income(), income()] } }), /yinelenen/);
  assert.throws(() => h.call('yedekVerisiniDogrula', { format: 'finora-v2', data: { ft_islemler: [income(20000, '2026-02-30')] } }), /tarih/);
});

test('GÜVENLİK — Kaynak dosyanın SHA-256 değeri testler boyunca değişmemeli', () => {
  const after = crypto.createHash('sha256').update(fs.readFileSync(sourcePath)).digest('hex');
  assert.equal(after, sourceHash);
});
