import test from 'node:test';
import assert from 'node:assert/strict';
import { createHarness } from './source-harness.mjs';

const copy = x => JSON.parse(JSON.stringify(x));
const income = { id: 'income', tip: 'gelir', kategori: 'Maaş', tutar: 20000, tarih: '2026-10-15' };
const loan = { id: 'loan', ad: 'Test kredisi', toplamTutar: 5000, vade: 1, taksitler: [{ no: 1, tarih: '2026-10-20', tutar: 5000, odendi: false }] };
const create = extra => createHarness({ state: { islemler: [copy(income)], krediler: [copy(loan)], ...extra } });
const toggle = h => h.callback('KredilerTab', 'taksitToggle', 'loan', 1);
const values = h => { const b = h.balance(); return [b.gercekBakiye, b.krediBekleyen, b.odemelerSonrasi]; };

test('A01 — Tam 20.000 / 5.000 / 15.000 → ödeme → geri alma senaryosu', () => {
  const h = create();
  assert.deepEqual(values(h), [20000, 5000, 15000]);
  toggle(h);
  assert.deepEqual(values(h), [15000, 0, 15000]);
  const payment = h.context.islemler.find(i => i.kaynak === 'kredi_taksiti');
  assert.equal(payment.tarih, '2026-10-16');
  assert.notEqual(payment.tarih, loan.taksitler[0].tarih);
  assert.equal(h.context.krediler[0].taksitler[0].odemeIslemId, payment.id);
  toggle(h);
  assert.deepEqual(values(h), [20000, 5000, 15000]);
  assert.equal(h.context.islemler.length, 1);
});

test('A02 — Aynı ödeme eski UI durumu ile yeniden işlenirse ikinci nakit çıkışı oluşmamalı', () => {
  const h = create(); toggle(h);
  h.context.krediler = [copy(loan)]; // Simulate retry with a stale unpaid checkbox and the committed ledger.
  toggle(h);
  assert.equal(h.context.islemler.filter(i => i.kaynak === 'kredi_taksiti').length, 1);
  assert.deepEqual(values(h), [15000, 0, 15000]);
});

test('A03 — Ödeme/geri alma/yeniden ödeme tek ödeme kimliğiyle bir kez etkili olmalı', () => {
  const h = create(); toggle(h);
  const id = h.context.krediler[0].taksitler[0].odemeIslemId;
  toggle(h); toggle(h);
  assert.equal(h.context.krediler[0].taksitler[0].odemeIslemId, id);
  assert.deepEqual(values(h), [15000, 0, 15000]);
});

test('A04 — Eski odendi:true kayıt için geçmiş gider üretilmemeli; manuel ödeme geri almada silinmemeli', () => {
  const legacy = copy(loan); legacy.taksitler[0].odendi = true;
  const manual = { id: 'manual', tip: 'gider', kategori: 'Kredi', tutar: 5000, tarih: '2026-10-16', aciklama: 'Eski manuel kayıt' };
  const h = create({ krediler: [legacy], islemler: [copy(income), manual] });
  assert.deepEqual(values(h), [15000, 0, 15000]);
  assert.equal(h.context.islemler.length, 2);
  toggle(h);
  assert.deepEqual(values(h), [15000, 5000, 10000]);
  assert.deepEqual(copy(h.context.islemler[1]), manual);
  assert.equal(h.context.krediler[0].taksitler[0].eskiTakipOdendi, true);
});

test('A05 — Aynı tutar/kategori/tarihli bağımsız manuel kayıt otomatik eşleştirilmemeli', () => {
  const manual = { id: 'manual', tip: 'gider', kategori: 'Kredi', tutar: 5000, tarih: '2026-10-16' };
  const h = create({ islemler: [copy(income), manual] }); toggle(h);
  assert.deepEqual(copy(h.context.islemler.find(i => i.id === 'manual')), manual);
  assert.equal(h.context.islemler.filter(i => i.kaynak === 'kredi_taksiti').length, 1);
});

test('A06 — Onay iptalinde hiçbir ödeme veya takip durumu yazılmamalı', () => {
  const h = create(); h.context.odemeOnayi = () => false;
  const before = JSON.stringify([h.context.islemler, h.context.krediler]); toggle(h);
  assert.equal(JSON.stringify([h.context.islemler, h.context.krediler]), before);
  assert.equal(h.calls.length, 0);
});

test('A07 — Güvenlik kopyası yazılamazsa kredi ödemesi başlamamalı', () => {
  const h = create(); h.storage.failOnce('ft_auto_backup_kredi_islem_oncesi_v1');
  toggle(h);
  assert.deepEqual(values(h), [20000, 5000, 15000]);
  assert.equal(h.calls.length, 0);
  assert.equal(h.notices.some(([, type]) => type === 'hata'), true);
});

for (const paid of [false, true]) test(`A08 — ${paid ? 'Geri almada' : 'Ödemede'} ikinci kayıt yazılamazsa ilk değişiklik geri dönmeli`, () => {
  const k = copy(loan); k.taksitler[0].odendi = paid;
  const rows = [copy(income), ...(paid ? [{ id: 'kredi-odeme:loan:1', tip: 'gider', kategori: 'Kredi', tutar: 5000, tarih: '2026-10-16', kaynak: 'kredi_taksiti', krediId: 'loan', taksitNo: 1 }] : [])];
  const h = createHarness({ storage: { ft_islemler: JSON.stringify(rows), ft_krediler: JSON.stringify([k]) }, state: { islemler: rows, krediler: [k] } });
  const beforeRows = h.storage.getItem('ft_islemler'), beforeLoans = h.storage.getItem('ft_krediler');
  [, h.context.setIslemler] = h.call('useLocalStorage', 'ft_islemler', []);
  [, h.context.setKrediler] = h.call('useLocalStorage', 'ft_krediler', []);
  h.storage.failOnce('ft_krediler'); toggle(h);
  assert.equal(h.storage.getItem('ft_islemler'), beforeRows);
  assert.equal(h.storage.getItem('ft_krediler'), beforeLoans);
  assert.equal(h.storage.getItem('ft_kayit_gecmisi'), null);
  assert.ok(h.storage.getItem('ft_auto_backup_kredi_islem_oncesi_v1'));
});

test('A09 — Geçersiz taksit tutarı veya tarihi ödeme kaydı oluşturmamalı', () => {
  for (const change of [{ tutar: -1 }, { tutar: 'bozuk' }, { tarih: '2026-02-30' }]) {
    const k = copy(loan); Object.assign(k.taksitler[0], change);
    const h = create({ krediler: [k] }); toggle(h);
    assert.equal(h.context.islemler.length, 1);
    assert.equal(h.context.krediler[0].taksitler[0].odendi, false);
  }
});

test('A10 — Çakışan ödeme kimliği normal manuel kaydı ezmemeli', () => {
  const manual = { id: 'kredi-odeme:loan:1', tip: 'gider', kategori: 'Market', tutar: 200, tarih: '2026-10-16' };
  const h = create({ islemler: [copy(income), manual] }); toggle(h);
  assert.equal(h.context.islemler.length, 2);
  assert.equal(h.context.krediler[0].taksitler[0].odendi, false);
  assert.deepEqual(copy(h.context.islemler[1]), manual);
});

test('A11 — Yalnız vadesi dönem sonuna kadar gelen ödenmemiş kredi taksitleri ayrılmalı', () => {
  const k = copy(loan); k.taksitler.push({ no: 2, tarih: '2026-10-01', tutar: 2000, odendi: false }, { no: 3, tarih: '2026-11-20', tutar: 3000, odendi: false }, { no: 4, tarih: '2026-10-19', tutar: 1000, odendi: true });
  const h = create({ krediler: [k] });
  assert.deepEqual(values(h), [20000, 7000, 13000]);
});

test('A12 — İleri tarihli açık bağlı ödeme zaten ayrılmışsa taksit ikinci kez ayrılmamalı', () => {
  const row = { id: 'future-payment', tip: 'gider', kategori: 'Kredi', kaynak: 'kredi_taksiti', krediId: 'loan', taksitNo: 1, tutar: 5000, tarih: '2026-10-20' };
  const h = create({ islemler: [copy(income), row] });
  assert.equal(h.balance().gercekBakiye, 20000);
  assert.equal(h.balance().ayrilan, 5000);
  assert.equal(h.balance().odemelerSonrasi, 15000);
});

test('A13 — Yeni kredi ödemesi yalnız nakit raporuna girmeli; harcama toplamı artmamalı', () => {
  const h = create(); toggle(h);
  assert.equal(h.balance().harcama, 0);
  assert.equal(h.balance().nakitCikis, 5000);
  const rows = h.call('gunlukFinansHareketleri', h.context.islemler, [], [], '2026-10-15', '2026-11-14', '2026-10-16', 'nakit');
  assert.equal(rows.filter(i => i.kaynak === 'kredi_taksiti').length, 1);
});

test('A14 — Yeni kredi nakit çıkışı tahminde günlük harcama gibi aya yayılmamalı', () => {
  const h = create(); toggle(h);
  const D = h.context.Date;
  const forecast = h.call('donemSonuGiderTahmini', { islemler: h.context.islemler, baslangic: new D(2026, 9, 15), bitis: new D(2026, 10, 14), bugun: new D(2026, 9, 16) });
  assert.equal(forecast.tahmin, 5000);
  assert.equal(forecast.gunlukOrt, 0);
});

test('A15 — Yeni kredi ödeme bağları mevcut yedek biçimiyle korunmalı', () => {
  const h = create(); toggle(h);
  const data = h.call('yedekVerisiniDogrula', { format: 'finora-v2', data: { ft_islemler: h.context.islemler, ft_krediler: h.context.krediler, ft_kategoriler: { gelir: ['Maaş'], gider: ['Ediz basketbol'] } } });
  assert.equal(data.ft_islemler[1].krediId, 'loan');
  assert.equal(data.ft_krediler[0].taksitler[0].odemeIslemId, data.ft_islemler[1].id);
  assert.deepEqual(copy(data.ft_kategoriler.gider), ['Ediz basketbol']);
});

test('A16 — Genel işlem düzenleme/silme kredi ödeme bağını bozmamalı', () => {
  const h = create(); toggle(h);
  const payment = h.context.islemler.find(i => i.kaynak === 'kredi_taksiti');
  const before = JSON.stringify([h.context.islemler, h.context.krediler]);
  h.context.duzenleHedef = payment;
  h.callback('IslemlerTab', 'islemDuzenle');
  h.callback('IslemlerTab', 'islemSil', payment);
  h.callback('KredilerTab', 'krediSil', 'loan');
  assert.equal(JSON.stringify([h.context.islemler, h.context.krediler]), before);
});

test('A17 — Birden fazla açık bağlı ödeme varsa otomatik silme/birleştirme yapılmamalı', () => {
  const h = create(); toggle(h);
  h.context.islemler.push({ ...h.context.islemler[1], id: 'duplicate' });
  const before = JSON.stringify([h.context.islemler, h.context.krediler]); toggle(h);
  assert.equal(JSON.stringify([h.context.islemler, h.context.krediler]), before);
});
