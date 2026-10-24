import test from 'node:test';
import assert from 'node:assert/strict';
import {emptyState,parseNumber,amount,percentage,depositSummary,saveDepositEntry,validateDepositLedger,totals,valuation,holdingSummary,migrate,mergeQuotes,recordSnapshot,recordChartPoint,chartSeries,periodPerformance,closingDateFloor,closeHolding,canUndoClose,undoCloseHolding,quoteQuality} from '../app/src/main/assets/core.mjs';
const base=()=>({id:'d',portfoyId:'p1',tip:'vadeli_mevduat',banka:'ziraat',islemler:[{id:'p',tur:'yatirma',tarih:'2026-01-01',tutar:100000}]});
test('Türkçe ondalık, 6 haneli fon fiyatı ve geçersiz değerler',()=>{
  assert.equal(parseNumber('1.234,56'),1234.56);assert.equal(parseNumber('0,123456'),.123456);assert.equal(parseNumber('10.123456'),10.123456);
  assert.ok(Number.isNaN(parseNumber('123abc')));assert.ok(Number.isNaN(parseNumber('1,2,3')));assert.throws(()=>amount('-3'));assert.throws(()=>amount('Infinity'));assert.equal(percentage('17,5'),17.5);assert.throws(()=>percentage('100,1'),/0 ile 100/);
});
test('Faiz yalnızca bir kez eklenir, düzenleme fark kadar etkiler, silme geri alır',()=>{
  let a=saveDepositEntry(base(),{id:'i',tur:'getiri',tarih:'2026-01-02',tutar:123.45});
  assert.deepEqual(depositSummary(a),{principal:100000,interest:123.45,withdrawals:0,balance:100123.45});
  assert.throws(()=>saveDepositEntry(a,{id:'i2',tur:'getiri',tarih:'2026-01-02',tutar:123.45}),/zaten/);
  a=saveDepositEntry(a,{id:'i',tur:'getiri',tarih:'2026-01-02',tutar:150});assert.equal(depositSummary(a).balance,100150);
  a.islemler=a.islemler.filter(t=>t.id!=='i');assert.equal(depositSummary(a).balance,100000);
});
test('Mevduat kuruşları tam toplanır; otomatik / bileşik faiz oluşmaz',()=>{
  const a={...base(),islemler:[{id:'p',tur:'yatirma',tarih:'2026-01-01',tutar:.1},{id:'i',tur:'getiri',tarih:'2026-01-02',tutar:.2}]};
  assert.equal(depositSummary(a).balance,.3);assert.equal(depositSummary(a).balance,.3);
  assert.throws(()=>saveDepositEntry(a,{tur:'getiri',tarih:'2026-01-03',tutar:.001}),/0,01/);
});
test('Mevduat yatırma / çekme ana toplamı ve faizi tutarlı tutar',()=>{
  let a=saveDepositEntry(base(),{tur:'getiri',tarih:'2026-01-02',tutar:200});
  a=saveDepositEntry(a,{tur:'cekme',tarih:'2026-01-03',tutar:50200});
  const state={...emptyState(),varliklar:[a]};const t=totals(state);
  assert.equal(t.value,50000);assert.equal(t.deposit,50000);assert.equal(t.interest,200);assert.equal(t.pnl,200);
  assert.throws(()=>saveDepositEntry(a,{tur:'cekme',tarih:'2026-01-04',tutar:50001}),/aşıyor/);
  assert.throws(()=>saveDepositEntry(a,{tur:'cekme',tarih:'2025-12-31',tutar:1}),/aşıyor/);
});
test('Tüm anapara silinince geçmiş çekim negatife inemez',()=>{
  assert.throws(()=>validateDepositLedger([{tur:'cekme',tutar:100,tarih:'2026-01-02'}]),/aşıyor/);
});
test('Eksik fiyat -%100 zarar oluşturmaz; eksik maliyet sahte kar oluşturmaz',()=>{
  const state={...emptyState(),varliklar:[{id:'a',portfoyId:'p1',tip:'hisse',sembol:'ABC',miktar:10,maliyetBirim:100},{id:'b',portfoyId:'p1',tip:'gram_altin',miktar:1,maliyetBirim:0},{...base()}],quotes:{gram_altin:{bid:7000,status:'manual'}}};
  const t=totals(state);assert.equal(t.value,107000);assert.equal(t.pnl,0);assert.equal(t.missing,1);assert.equal(t.unknownCost,1);
  assert.equal(valuation(state.varliklar[0],{}).pnl,null);
});
test('Portföy filtresi ve banka alış fiyatı',()=>{
  const state={...emptyState(),varliklar:[{id:'a',portfoyId:'p2',tip:'banka_altin',banka:'ziraat',miktar:2,maliyetBirim:5000},base()],quotes:{banka_altin_ziraat:{bid:6000,ask:6500,status:'manual'}}};
  assert.equal(totals(state,'p2').value,12000);assert.equal(totals(state,'p2').pnl,2000);assert.equal(totals(state,'p1').value,100000);
});
test('TEFAS pay miktarı küsuratını ve fiyat hassasiyetini korur',()=>{
  const a={tip:'tefas_fon',sembol:'ABC',miktar:123.456789,maliyetBirim:.111111};
  const v=valuation(a,{tefas_ABC:{bid:.123456,status:'manual'}});assert.equal(v.price,.123456);assert.equal(v.value,15.24);
});
test('TEFAS stopajı yalnız pozitif kârdan düşer; brüt ve net tutarlar ayrı korunur',()=>{
  const a={id:'a',portfoyId:'p1',tip:'tefas_fon',sembol:'ABC',miktar:100,maliyetBirim:1,stopajVar:true,stopajOrani:17.5};
  let v=valuation(a,{tefas_ABC:{bid:2,status:'manual'}});assert.equal(v.grossValue,200);assert.equal(v.grossPnl,100);assert.equal(v.withholding,17.5);assert.equal(v.value,182.5);assert.equal(v.pnl,82.5);assert.equal(v.percent,82.5);
  const t=totals({...emptyState(),varliklar:[a],quotes:{tefas_ABC:{bid:2,status:'manual'}}});assert.equal(t.grossValue,200);assert.equal(t.value,182.5);assert.equal(t.grossPnl,100);assert.equal(t.pnl,82.5);assert.equal(t.withholding,17.5);
  v=valuation(a,{tefas_ABC:{bid:.8,status:'manual'}});assert.equal(v.withholding,0);assert.equal(v.value,80);assert.equal(v.pnl,-20);
  v=valuation({...a,maliyetBirim:0},{tefas_ABC:{bid:2,status:'manual'}});assert.equal(v.withholding,null);assert.equal(v.value,200);assert.equal(v.pnl,null);
  const old=migrate({...emptyState(),varliklar:[{...a,stopajVar:undefined,stopajOrani:undefined}]});assert.equal(old.varliklar[0].stopajVar,false);assert.equal(old.varliklar[0].stopajOrani,17.5);
  assert.throws(()=>migrate({...emptyState(),varliklar:[{...a,stopajOrani:100.1}]}),/0 ile 100/);
});
test('Alım / temettü defteri v3 etkin maliyet mantığını korur',()=>{
  const h=holdingSummary({islemler:[{tur:'alis',miktar:10,fiyat:100,komisyon:10},{tur:'alis',miktar:10,fiyat:120,komisyon:10},{tur:'temettu',tutar:20}]});
  assert.equal(h.quantity,20);assert.equal(h.cost,2200);assert.equal(h.average,110);
});
test('Eski yedek değişmeden taşınır, fiyat eski işaretlenir',()=>{
  const raw={...emptyState(),schemaVersion:undefined,quotes:undefined,fiyatlar:{tefas_ABC:1.234567},varliklar:[{id:'a',portfoyId:'p1',tip:'tefas_fon',sembol:'ABC',miktar:100,maliyetBirim:1}],kapaliPozisyonlar:[{id:'c',kz:75}],snapshots:[{ts:123}]};
  const before=JSON.stringify(raw),next=migrate(raw);assert.equal(JSON.stringify(raw),before);assert.equal(next.quotes.tefas_ABC.bid,1.234567);assert.equal(next.quotes.tefas_ABC.status,'legacy');assert.equal(next.kapaliPozisyonlar[0].kz,75);assert.equal(next.snapshots[0].ts,123);
});
test('Bozuk/çelişkili yedek kabul edilmez',()=>{
  assert.throws(()=>migrate({}));assert.throws(()=>migrate({...emptyState(),schemaVersion:99}));
  assert.throws(()=>migrate({...emptyState(),varliklar:[{id:'a',portfoyId:'missing',tip:'hisse'}]}));
  assert.throws(()=>migrate({...emptyState(),varliklar:[{...base(),islemler:[{id:'i',tur:'getiri',tarih:'2026-02-31',tutar:5}]}]}));
});
test('Başarısız fiyat güncellemesi eski değeri silmez veya güncel saymaz',()=>{
  const s={...emptyState(),quotes:{hisse_ABC:{bid:100,status:'received',fetchedAt:'2026-01-01T00:00:00Z'}}};
  const n=mergeQuotes(s,{errors:{hisse_ABC:'Engellendi'},quotes:{},checkedAt:'2026-01-02T00:00:00Z'},Date.now());
  assert.equal(n.quotes.hisse_ABC.bid,100);assert.equal(n.quotes.hisse_ABC.status,'stale');assert.equal(n.quotes.hisse_ABC.fetchedAt,'2026-01-01T00:00:00Z');
});
test('Kilitli / istekten sonra girilmiş manuel fiyat yarışta ezilmez',()=>{
  const s={...emptyState(),quotes:{hisse_ABC:{bid:100,status:'manual',locked:true,fetchedAt:'2026-01-01T00:00:00Z'}}};
  const r={quotes:{hisse_ABC:{bid:110,status:'received'}},checkedAt:'2026-01-02T00:00:00Z'};
  assert.equal(mergeQuotes(s,r,Date.now()).quotes.hisse_ABC.bid,100);
  s.quotes.hisse_ABC.locked=false;assert.equal(mergeQuotes(s,r,Date.now()).quotes.hisse_ABC.bid,110);
  s.quotes.hisse_ABC.fetchedAt='2026-01-03T00:00:00Z';assert.equal(mergeQuotes(s,r,Date.parse('2026-01-02')).quotes.hisse_ABC.bid,100);
});
test('Günlük değer geçmişi portföy bazında tutulur ve aynı gün tek kayıt bırakır',()=>{
  let s={...emptyState(),varliklar:[{id:'a',portfoyId:'p1',tip:'try_lirasi',miktar:1000,maliyetBirim:1},{id:'b',portfoyId:'p2',tip:'try_lirasi',miktar:500,maliyetBirim:1}]};
  s=recordSnapshot(s,'2026-08-02T09:00:00Z');assert.equal(s.snapshots[0].total,1500);assert.equal(s.snapshots[0].byPortfolio.p1,1000);
  s.varliklar[0].miktar=1100;s=recordSnapshot(s,'2026-08-02T18:00:00Z');assert.equal(s.snapshots.length,1);assert.equal(s.snapshots[0].total,1600);
  s.varliklar[0].miktar=1200;s=recordSnapshot(s,'2026-08-31T18:00:00Z');
  s.varliklar[0].miktar=1300;
  const daily=periodPerformance(s,'p1',1,Date.parse('2026-09-01T12:00:00Z'));
  assert.equal(daily.value,100);assert.equal(Math.round(daily.percent*100)/100,8.33);
  const monthly=periodPerformance(s,'all',30,Date.parse('2026-09-01T12:00:00Z'));
  assert.equal(monthly.value,200);assert.equal(monthly.base,1600);
  assert.equal(periodPerformance(s,'missing',1,Date.parse('2026-09-01T12:00:00Z')),null);
});
test('Grafik geçmişi aynı saati tekilleştirir ve veri büyüdükçe haftalık ile aylığa geçer',()=>{
  let s={...emptyState(),varliklar:[{id:'a',portfoyId:'p1',tip:'hisse',sembol:'ABC',miktar:1,maliyetBirim:90}],quotes:{hisse_ABC:{bid:100,status:'manual'}}};
  s=recordChartPoint(s,'2026-09-01T08:00:00Z');s={...s,quotes:{hisse_ABC:{bid:105,status:'manual'}}};s=recordChartPoint(s,'2026-09-01T08:40:00Z');
  assert.equal(s.chartHistory.assets.a.length,1);assert.equal(s.chartHistory.assets.a[0].value,105);assert.equal(s.chartHistory.portfolios.p1[0].value,105);
  for(let day=2;day<=8;day++){s={...s,quotes:{hisse_ABC:{bid:100+day,status:'manual'}}};s=recordChartPoint(s,`2026-09-${String(day).padStart(2,'0')}T09:00:00Z`);}
  let asset=chartSeries(s,'asset','a'),portfolio=chartSeries(s,'portfolio','p1');assert.equal(asset.mode,'week');assert.equal(portfolio.mode,'week');assert.ok(asset.points.length>=2);
  for(let day=9;day<=29;day++){s={...s,quotes:{hisse_ABC:{bid:100+day,status:'manual'}}};s=recordChartPoint(s,`2026-09-${String(day).padStart(2,'0')}T09:00:00Z`);}
  asset=chartSeries(s,'asset','a');assert.equal(asset.mode,'month');assert.equal(asset.available.month,true);assert.equal(asset.change,24);
  s={...s,snapshots:[{ts:'bozuk-tarih',byPortfolio:{p1:999}}]};assert.doesNotThrow(()=>chartSeries(s,'portfolio','p1'));
});
test('Kapanış net nakde aktarılır; alım ve kapanış komisyonu düşer',()=>{
  const s={...emptyState(),varliklar:[{id:'a',portfoyId:'p1',tip:'hisse',sembol:'ABC',miktar:10,maliyetBirim:100,alisKomisyon:10}],quotes:{hisse_ABC:{bid:120}}};
  const n=closeHolding(s,'a',120,5,'2026-01-01',true);assert.equal(n.varliklar.length,1);assert.equal(n.varliklar[0].tip,'try_lirasi');assert.equal(totals(n).value,1195);assert.equal(n.kapaliPozisyonlar[0].kz,185);
});
test('Parçalı satış satılan maliyeti orantılı ayırır ve kalan adedi açık tutar',()=>{
  const s={...emptyState(),varliklar:[{id:'a',portfoyId:'p1',tip:'hisse',sembol:'ABC',miktar:10,maliyetBirim:100,alisKomisyon:10}],quotes:{hisse_ABC:{bid:120,status:'manual'}}};
  const n=closeHolding(s,'a',120,2,'2026-01-01',true,4),open=n.varliklar.find(a=>a.id==='a'),cash=n.varliklar.find(a=>a.tip==='try_lirasi'),closed=n.kapaliPozisyonlar[0];
  assert.equal(holdingSummary(open).quantity,6);assert.equal(holdingSummary(open).cost,606);assert.equal(cash.miktar,478);
  assert.equal(closed.miktar,4);assert.equal(closed.maliyetBirim,101);assert.equal(closed.kz,74);assert.equal(closed.parcali,true);assert.equal(closed.kalanMiktar,6);
  assert.throws(()=>closeHolding(s,'a',120,0,'2026-01-01',false,11),/eldeki miktarı aşamaz/);
  const done=closeHolding(n,'a',110,1,'2026-01-02',false,6);assert.equal(done.varliklar.some(a=>a.id==='a'),false);assert.equal(done.kapaliPozisyonlar[1].kz,53);assert.equal(done.kapaliPozisyonlar[1].parcali,false);
});
test('Maliyeti bilinmeyen parçalı satış sahte kâr üretmez',()=>{
  const s={...emptyState(),varliklar:[{id:'a',portfoyId:'p1',tip:'gram_altin',miktar:10,maliyetBirim:0}],quotes:{gram_altin:{bid:100,status:'manual'}}};
  const n=closeHolding(s,'a',100,0,'2026-01-01',false,4),open=n.varliklar.find(a=>a.id==='a');
  assert.equal(holdingSummary(open).quantity,6);assert.equal(holdingSummary(open).cost,null);assert.equal(n.kapaliPozisyonlar[0].kz,null);
});
test('Stopajlı TEFAS kapanışı net nakit, brüt ve net kârı ayrı kaydeder',()=>{
  const asset={id:'f',portfoyId:'p1',tip:'tefas_fon',sembol:'ABC',miktar:100,maliyetBirim:1,stopajVar:true,stopajOrani:17.5};
  const s={...emptyState(),varliklar:[asset],quotes:{tefas_ABC:{bid:2,status:'manual'}}},n=closeHolding(s,'f',2,0,'2026-01-01',true,100),record=n.kapaliPozisyonlar[0],t=totals(n);
  assert.equal(n.varliklar[0].miktar,182.5);assert.equal(record.brutKz,100);assert.equal(record.stopajTutari,17.5);assert.equal(record.netKz,82.5);assert.equal(record.kz,82.5);assert.equal(record.netSatis,182.5);
  assert.equal(t.value,182.5);assert.equal(t.openPnl,0);assert.equal(t.realizedPnl,82.5);assert.equal(t.pnl,82.5);assert.equal(t.realizedWithholding,17.5);
  const custom=closeHolding(s,'f',2,0,'2026-01-01',true,100,12);assert.equal(custom.varliklar[0].miktar,188);assert.equal(custom.kapaliPozisyonlar[0].kz,88);assert.equal(custom.kapaliPozisyonlar[0].stopajTahmini,false);
  const unknown={...s,varliklar:[{...asset,maliyetBirim:0}]};assert.throws(()=>closeHolding(unknown,'f',2,0,'2026-01-01',true,100),/stopaj hesaplanamıyor/);assert.doesNotThrow(()=>closeHolding(unknown,'f',2,0,'2026-01-01',true,100,0));
});
test('Gerçekleşmiş kâr ana toplamda korunur ve açık kârdan ayrılır',()=>{
  const s={...emptyState(),varliklar:[{id:'a',portfoyId:'p1',tip:'hisse',sembol:'ABC',miktar:10,maliyetBirim:100,alisKomisyon:10}],quotes:{hisse_ABC:{bid:120,status:'manual'}}};
  const partial=closeHolding(s,'a',120,2,'2026-01-01',true,4),p=totals(partial);assert.equal(p.openPnl,114);assert.equal(p.realizedPnl,74);assert.equal(p.pnl,188);assert.equal(Math.round(p.percent*100)/100,18.61);
  const full=closeHolding(s,'a',120,5,'2026-01-01',true,10),f=totals(full);assert.equal(f.openPnl,0);assert.equal(f.realizedPnl,185);assert.equal(f.pnl,185);assert.equal(Math.round(f.percent*100)/100,18.32);
});
test('Geçmiş tarihli satış yalnız o tarihte bulunan miktar ve maliyeti kullanır',()=>{
  const asset={id:'a',portfoyId:'p1',tip:'hisse',sembol:'ABC',islemler:[{id:'b1',tur:'alis',tarih:'2026-01-01',miktar:1,fiyat:100,komisyon:0},{id:'b2',tur:'alis',tarih:'2026-09-01',miktar:1,fiyat:200,komisyon:0}]};
  const s={...emptyState(),varliklar:[asset]};assert.equal(closingDateFloor(s,'a'),'2026-01-01');
  assert.throws(()=>closeHolding(s,'a',150,0,'2025-12-31',false,1),/henüz portföyde/);assert.throws(()=>closeHolding(s,'a',150,0,'2026-06-01',false,2),/henüz portföyde/);
  const n=closeHolding(s,'a',150,0,'2026-06-01',false,1);assert.equal(n.kapaliPozisyonlar[0].maliyetToplam,100);assert.equal(holdingSummary(n.varliklar[0]).cost,200);assert.equal(closingDateFloor(n,'a'),'2026-06-01');
  assert.throws(()=>closeHolding(n,'a',210,0,'2026-05-01',false,1),/önceki kapanış/);
});
test('Kapanış geri alma bağlı nakdi kaldırır ve yalnız en son kaydı geri alır',()=>{
  const original={id:'a',portfoyId:'p1',tip:'hisse',sembol:'ABC',miktar:10,maliyetBirim:100,alisKomisyon:10},s={...emptyState(),varliklar:[original]};
  const first=closeHolding(s,'a',120,2,'2026-01-01',true,4),r1=first.kapaliPozisyonlar[0];assert.equal(canUndoClose(first,r1),true);
  const second=closeHolding(first,'a',110,1,'2026-01-02',true,2),r2=second.kapaliPozisyonlar[1];assert.equal(canUndoClose(second,r1),false);assert.equal(canUndoClose(second,r2),true);assert.throws(()=>undoCloseHolding(second,r1.id),/daha yeni/);
  const backOne=undoCloseHolding(second,r2.id),backAll=undoCloseHolding(backOne,r1.id);assert.equal(backAll.kapaliPozisyonlar.length,0);assert.equal(backAll.varliklar.length,1);assert.equal(holdingSummary(backAll.varliklar[0]).quantity,10);assert.equal(holdingSummary(backAll.varliklar[0]).cost,1010);
  const full=closeHolding(s,'a',120,5,'2026-01-01',true,10),restored=undoCloseHolding(full,full.kapaliPozisyonlar[0].id);assert.deepEqual(restored.varliklar[0],original);
  const altered=closeHolding(s,'a',120,5,'2026-01-01',true,10);altered.varliklar[0].miktar=1;assert.throws(()=>undoCloseHolding(altered,altered.kapaliPozisyonlar[0].id),/değiştirilmiş/);
});
test('Eski fiyat ile yeni alınmış tarihli verinin etiketi ayrılır',()=>{
  assert.equal(quoteQuality({bid:1,status:'received',fetchedAt:'2000-01-01'}),'stale');
  assert.equal(quoteQuality({bid:1,status:'received',fetchedAt:new Date().toISOString(),priceDate:'2000-01-01'}),'dated');
});
