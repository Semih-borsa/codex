import test from 'node:test';
import assert from 'node:assert/strict';
import {createHarness} from './source-harness.mjs';
const copy=x=>JSON.parse(JSON.stringify(x));
const income=(tutar=20000,tarih='2026-10-15',id='income')=>({id,tip:'gelir',kategori:'Maaş',tutar,tarih});
const base=()=>({bugun:'2026-10-17',islemler:[income()],donemGun:15});
function flow(extra={}){return createHarness().call('nakitAkisiHesapla',{...base(),...extra});}
const at=(r,d)=>r.gunler.find(g=>g.tarih===d)?.bakiye;
const card=()=>({id:'c',isim:'World',bakiyedenDussun:true,ekstreler:[{id:'e',kartId:'c',tutar:5000,hesapKesimTarihi:'2026-10-10',sonOdemeTarihi:'2026-10-25',kayitTarihi:'2026-10-10',dogrulandi:true}]});
function plan(h=createHarness(),extra={}) {return h.call('kartTaksitPlaniOlustur',{id:'p',ad:'Bilgisayar',kartId:'c',toplamTutar:300,vade:3,alisverisTarihi:'2026-10-16',ilkEkstreTarihi:'2026-11-10',kesimGunu:10,ilkSonOdemeTarihi:'2026-11-20',...extra},[{id:'c',isim:'World'}]);}
for(const [n,expected] of [[30,21000],[60,22000],[90,23000]])test(`CF01-${n} — ${n} gün, tarihli aylık gelir`,()=>{
 const r=flow({gunSayisi:n,beklenenGelirler:[{id:'salary',ad:'Maaş',tutar:1000,tarih:'2026-11-01',aylik:true}]});assert.equal(r.gunler.length,n+1);assert.equal(r.sonBakiye,expected);assert.equal(at(r,'2026-10-31'),20000);assert.equal(at(r,'2026-11-01'),21000);
});
test('CF02 — Maaş dönemi başlangıcı kendiliğinden maaş geliri üretmez',()=>{const r=flow({gunSayisi:60});assert.equal(r.sonBakiye,20000);assert.equal(r.hareketler.length,0);assert.ok(r.uyarilar.some(s=>s.includes('maaş kendiliğinden')));});
test('CF03 — Bekleyen kredi 20.000 gerçek / 15.000 kullanılabilir; ödeme gününde bir kez',()=>{
 const r=flow({krediler:[{id:'l',ad:'Kredi',taksitler:[{no:1,tutar:5000,tarih:'2026-10-20',odendi:false}]}]});assert.equal(r.mevcutBakiye,20000);assert.equal(r.mevcutKullanilabilir,15000);assert.equal(at(r,'2026-10-19'),20000);assert.equal(at(r,'2026-10-20'),15000);assert.equal(r.sonBakiye,15000);
});
test('CF04 — Ödenmiş kredi ve bağlı nakit kaydı tekrar sayılmaz',()=>{
 const r=flow({islemler:[income(),{id:'paid',tip:'gider',kaynak:'kredi_taksiti',krediId:'l',taksitNo:1,tutar:5000,tarih:'2026-10-17'}],krediler:[{id:'l',taksitler:[{no:1,tutar:5000,tarih:'2026-10-20',odendi:true,odemeIslemId:'paid'}]}]});assert.equal(r.mevcutBakiye,15000);assert.equal(r.sonBakiye,15000);assert.equal(r.hareketler.length,0);
});
test('CF05 — Ekstre son ödeme tarihinde tam kalan bir kez düşer',()=>{const r=flow({kartlar:[card()]});assert.equal(at(r,'2026-10-24'),20000);assert.equal(at(r,'2026-10-25'),15000);assert.equal(r.hareketler.filter(h=>h.tip==='gider').length,1);});
test('CF06 — Kısmi ödenen ekstre başlangıç nakdi ve kalanıyla çift sayılmaz',()=>{
 const r=flow({kartlar:[card()],kartOdemeleri:[{id:'o',kartId:'c',ekstreId:'e',tutar:2000,tarih:'2026-10-16'}]});assert.equal(r.mevcutBakiye,18000);assert.equal(r.sonBakiye,15000);assert.equal(r.hareketler[0].tutar,3000);
});
test('CF07 — Planlı erken ekstre ödemesi ve kalan borç ortak toplamdan bir kez düşer',()=>{
 const r=flow({kartlar:[card()],kartOdemeleri:[{id:'o',kartId:'c',ekstreId:'e',tutar:2000,tarih:'2026-10-21'}]});assert.equal(at(r,'2026-10-21'),18000);assert.equal(at(r,'2026-10-25'),15000);assert.equal(r.sonBakiye,15000);
});
test('CF08 — Vade sonrası ödeme borcun vadesini gizlemez, ikinci kez düşmez',()=>{const r=flow({kartlar:[card()],kartOdemeleri:[{id:'o',kartId:'c',ekstreId:'e',tutar:5000,tarih:'2026-11-01'}]});assert.equal(at(r,'2026-10-25'),15000);assert.equal(at(r,'2026-11-01'),15000);assert.ok(r.uyarilar.some(s=>s.includes('vade sonrasına')));});
test('CF09 — Tarihli taksitler Kasım/Aralık/Ocak vadelerine dağılır',()=>{const r=flow({gunSayisi:90,kartlar:[{id:'c',isim:'World'}],taksitler:[plan()]});assert.equal(at(r,'2026-11-19'),20000);assert.equal(at(r,'2026-11-20'),19900);assert.equal(at(r,'2026-12-20'),19800);assert.equal(r.sonBakiye,19800);});
test('CF10 — Açık bağlı banka ekstresi ile plan aynı borcu iki kez oluşturmaz',()=>{
 const p=plan(undefined,{toplamTutar:15000,ilkEkstreTarihi:'2026-10-16',ilkSonOdemeTarihi:'2026-10-25',alisverisTarihi:'2026-10-15'});p.taksitler[0].ekstreId='e';const k=card();k.ekstreler[0].hesapKesimTarihi='2026-10-16';
 const r=flow({kartlar:[k],taksitler:[p]});assert.equal(r.sonBakiye,15000);assert.equal(r.hareketler.length,1);
});
test('CF11 — Banka/plan farkı açıklanır; belirsiz eşleşme birleştirilmez',()=>{const p=plan(undefined,{toplamTutar:300,ilkEkstreTarihi:'2026-10-10',alisverisTarihi:'2026-10-01',ilkSonOdemeTarihi:'2026-10-25'});const r=flow({kartlar:[card()],taksitler:[p]});assert.equal(r.sonBakiye,15000);assert.ok(r.uyarilar.some(s=>s.includes('fark 4900.00')));assert.equal(p.taksitler[0].ekstreId,null);});
test('CF12 — Provizyon/dönem içi stoku nakit hareketi gibi tekrar düşülmez',()=>{
 const r=flow({kartlar:[card()],islemler:[income(),{id:'stock',tip:'gider',kategori:'World',kartId:'c',kartHarcama:true,tarih:'2026-10-17',donemIci:10000,provizyon:5000,tutar:15000}]});assert.equal(r.sonBakiye,15000);assert.ok(r.uyarilar.some(s=>s.includes('dönem içi/provizyon')));
});
test('CF13 — Ödemeler ve açık bağlı işlem bir kez; kredi ödeme bağlantısı da bir kez',()=>{
 const r=flow({odemeler:[{id:'o',islemId:'i',tutar:1000,tarih:'2026-10-20',odendi:false},{id:'lo',kaynak:'kredi_taksiti',krediId:'l',taksitNo:1,tutar:2000,tarih:'2026-10-25',odendi:false}],islemler:[income(),{id:'i',tip:'gider',odemeId:'o',kaynak:'odeme',tutar:1000,tarih:'2026-10-20'}],krediler:[{id:'l',taksitler:[{no:1,tutar:2000,tarih:'2026-10-25',odendi:false}]}]});assert.equal(r.sonBakiye,17000);assert.equal(r.hareketler.length,2);
});
test('CF14 — Gelecek tek sefer gelir/gider kayıtları aynı gün netleşir',()=>{const r=flow({islemler:[income(),income(1000,'2026-10-20','future'),{id:'cost',tip:'gider',tutar:300,tarih:'2026-10-20'}]});assert.equal(r.sonBakiye,20700);assert.equal(r.hareketler.length,2);});
test('CF15 — İlk negatif gün ve en düşük bakiye',()=>{
 const r=flow({odemeler:[{id:'a',tutar:25000,tarih:'2026-10-20',odendi:false}],beklenenGelirler:[{id:'s',tutar:10000,tarih:'2026-11-01',ad:'Maaş'}]});assert.equal(r.ilkAcik,'2026-10-20');assert.equal(r.enDusuk,-5000);assert.equal(r.sonBakiye,5000);
});
test('CF16 — Bilinmeyen tarih/tutar sıfır veya uydurma tarihe dönüşmez',()=>{
 const r=flow({odemeler:[{id:'a',tutar:1000,odendi:false}],beklenenGelirler:[{id:'x',ad:'Belirsiz gelir',tutar:1000}],kartlar:[{id:'c',isim:'Bonus',ekstre:5000}],krediler:[{id:'l',ad:'Kredi',taksitler:[{no:1,tutar:1000,odendi:false}]}]});assert.equal(r.sonBakiye,20000);assert.equal(r.hareketler.length,0);assert.ok(r.uyarilar.some(s=>s.includes('Belirsiz gelir')));assert.ok(r.uyarilar.some(s=>s.includes('ekstre son ödeme tarihi')));
});
test('CF17 — Aktif sabit gider, başlamamış/bitmiş ve kaydedilmiş aynı dönem mükerrerliği',()=>{
 const r=flow({tekrarlayan:[{id:'s',aktif:true,gun:20,tutar:1000,baslangic:'2026-10-01',bitis:'2026-12-31',aciklama:'Aidat'}],odemeler:[{id:'o',sabitId:'s',tutar:1000,tarih:'2026-10-20',odendi:false}],gunSayisi:60});assert.equal(r.sonBakiye,18000);assert.equal(r.hareketler.length,2);assert.equal(r.hareketler[0].kesin,true);assert.equal(r.hareketler[1].kesin,false);
});
test('CF18 — Ayın 31i: şubat/artık yıl ve geri 31, kuruş toplamı korunur',()=>{
 const r=flow({bugun:'2028-01-30',islemler:[income(100,'2028-01-15')],gunSayisi:90,beklenenGelirler:[{id:'s',tutar:0.01,tarih:'2028-01-31',aylik:true}]});assert.deepEqual(Array.from(r.hareketler,h=>h.tarih),['2028-01-31','2028-02-29','2028-03-31']);assert.equal(r.sonBakiye,100.03);
});
test('CF19 — Maaş dönemi değişse de tarihli borç tarihte kalır',()=>{const a=flow({donemGun:15,kartlar:[card()]}),b=flow({donemGun:20,kartlar:[card()]});assert.equal(a.hareketler[0].tarih,b.hareketler[0].tarih);assert.equal(a.hareketler[0].tutar,b.hareketler[0].tutar);});
test('CF20 — Tahmin kaynağı, arşiv, bağlantı, depolama değişmez',()=>{
 const h=createHarness(),p=plan(h),args={...base(),kartlar:[card()],taksitler:[p],tekrarlayan:[{id:'s',gun:22,tutar:100,aktif:true}]};const before=JSON.stringify(args),storage=h.storage.snapshot();h.call('nakitAkisiHesapla',args);assert.equal(JSON.stringify(args),before);assert.deepEqual(h.storage.snapshot(),storage);assert.equal(h.calls.length,0);
});
test('CF21 — Banka bakiyesi varsayımı yalnız tahmini değiştirir',()=>{const r=flow({baslangicBakiyesi:5000,odemeler:[{id:'p',tarih:'2026-10-20',tutar:1000}]});assert.equal(r.mevcutBakiye,20000);assert.equal(r.baslangicBakiye,5000);assert.equal(r.sonBakiye,4000);});
test('CF22 — Geçmiş vade bugüne taşınır ve varsayım açıkça işaretlenir',()=>{const r=flow({odemeler:[{id:'p',tutar:1000,tarih:'2026-10-01',odendi:false}]});assert.equal(at(r,'2026-10-17'),19000);assert.equal(r.hareketler[0].tarihVarsayimi,true);assert.equal(r.hareketler[0].asilTarih,'2026-10-01');});
test('CF23 — İade ve iptal D kalanını kullanır; yeni gider yazılmaz',()=>{const p=plan();p.taksitler[0].iadeler.push({id:'r',tutar:30,tarih:'2026-10-17',bankaOnayli:false});p.taksitler[1].durum='iptal';p.iptalTarihi='2026-10-17';const r=flow({gunSayisi:90,kartlar:[{id:'c',isim:'World'}],taksitler:[p]});assert.equal(r.sonBakiye,19930);});
test('CF24 — Aynı gün giriş/çıkışta gün içi negatif sıra uydurulmaz',()=>{const r=flow({baslangicBakiyesi:0,odemeler:[{id:'p',tutar:100,tarih:'2026-10-20'}],beklenenGelirler:[{id:'s',tutar:100,tarih:'2026-10-20'}]});assert.equal(r.ilkAcik,null);assert.equal(r.enDusuk,0);});
