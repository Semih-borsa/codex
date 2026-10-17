import test from 'node:test';import assert from 'node:assert/strict';
import {createHarness} from '../finance-baseline/source-harness.mjs';
import {card,oldPeriod,currentPeriod,current,zero,archivedZero,observation,statement,inconsistentArchive} from './fixtures.mjs';
const closing={ilk:'2026-09-15',son:'2026-10-14',bugun:'2026-10-16'};
const income={id:'income-old',tip:'gelir',kategori:'Maaş',tutar:20000,tarih:'2026-09-15'};
const debt=(h,k,rows=[],history={})=>h.call('kartBorcDurumu',k,rows,[],oldPeriod,15,[k],history,'2026-10-16');
const plan=h=>h.call('kartTaksitPlaniOlustur',{id:'dated',kartId:'plan-card',ad:'Sentetik',toplamTutar:100,vade:1,alisverisTarihi:'2026-09-09',ilkEkstreTarihi:'2026-09-10',kesimGunu:10,odemeGunu:20},[{id:'plan-card',isim:'Test',bakiyedenDussun:true}]);
test('HF01 — Doğrulanmış sıfır / eski ayrıntı: gerçek 20.000, rezerv 0, kalan 20.000; arşiv değişmez',()=>{
 const h=createHarness(),a=inconsistentArchive(),before=JSON.stringify(a),b=h.balance({...closing,islemler:[income],kartlar:[card],kartBorcOzetleri:a});
 assert.equal(b.gercekBakiye,20000);assert.equal(b.ayrilan,0);assert.equal(b.odemelerSonrasi,20000);assert.equal(JSON.stringify(a),before);assert.ok(debt(h,card,[],a).uyarilar.length);
});
test('HF02 — Ekstre 10 Ekimde vardır, vadesi 20 Ekim: 14 Ekim borcu 5.000 fakat vadeli rezerv 0',()=>{
 const h=createHarness(),k={...card,ekstreler:[statement('s',5000,'2026-10-10','2026-10-20','2026-10-10')]};
 assert.equal(debt(h,k).ekstre,5000);assert.equal(h.balance({...closing,kartlar:[k]}).ekstreBekleyen,0);
});
test('HF03 — Kaydı/tarihi bilinmeyen eski ekstre geçmişe aktarılmaz, güncel borç korunur',()=>{
 const h=createHarness(),k={...card,ekstre:5000};const d=debt(h,k);assert.equal(d.biliniyor,false);assert.equal(d.ekstreBiliniyor,false);assert.ok(d.uyarilar.length);
 assert.equal(h.call('kartBorcDurumu',k,[],[],currentPeriod,15,[k],{},'2026-10-16').ekstre,5000);
});
test('HF04 — Çelişkili ekstre kart kimliği geçmişte kesin borç oluşturmaz',()=>{
 const h=createHarness(),k={...card,ekstreler:[{...statement('s',5000,'2026-09-10','2026-09-20','2026-09-10'),kartId:'other'}]};assert.equal(debt(h,k).biliniyor,false);
});
test('HF05 — Sıfır arşivinin kimliği/tarihi çelişkiliyse bugünkü borçla doldurulmaz',()=>{
 const h=createHarness();for(const edit of [{kartId:'other'},{tarih:'2026-10-16'},{donemIci:1}]){
  const d=debt(h,card,[current],{[oldPeriod]:{[card.id]:{...zero,...edit}}});assert.equal(d.biliniyor,false);assert.ok(d.uyarilar.length);
 }
});
test('HF06 — Eski uyumlu tarihçesiz 5.000 gözlemi dönem değişince kaybolmaz',()=>{
 const h=createHarness();assert.equal(debt(h,card,[observation('old','2026-09-20',5000)]).donemIci,5000);
});
test('HF07 — Tarihçeden daha yeni güncel gövde korunur; geçmişte açık sıfır tercih edilir',()=>{
 const h=createHarness(),r={...observation('body','2026-10-16',15000),kartDurumGecmisi:{'2026-09-20':{donemIci:0,provizyon:0}}};
 assert.equal(h.call('kartSonDurum',card,[r],'2026-10-16').donemIci,15000);assert.equal(h.call('kartSonDurum',card,[r],'2026-10-14').donemIci,0);
});
test('HF08 — Gelecek hareket kanıtı bulunan tarihçesiz eski gövde geçmişte kesin kabul edilmez',()=>{
 const h=createHarness(),r={...observation('old','2026-09-20',15000),kartGunlukHareketler:{'2026-10-16':15000}};assert.equal(debt(h,card,[r]).biliniyor,false);
});
test('HF09 — Geçmiş kapanıştan sonraki D iadesi/ödeme geçmiş rezervi azaltmaz',()=>{
 const h=createHarness(),p=plan(h),k={id:'plan-card',isim:'Test',bakiyedenDussun:true,ekstreler:[{...statement('s',100,'2026-09-10','2026-09-20','2026-09-10'),kartId:'plan-card'}]};
 p.taksitler[0].ekstreId='s';p.taksitler[0].odemePaylari=[{id:'a',odemeId:'p',tutar:100}];p.taksitler[0].iadeler=[{id:'r',tutar:10,tarih:'2026-10-16',bankaOnayli:false}];
 const b=h.balance({...closing,kartlar:[k],taksitler:[p],kartOdemeleri:[{id:'p',kartId:'plan-card',ekstreId:'s',tutar:100,tarih:'2026-10-16'}]});
 assert.equal(b.ekstreBekleyen,100);assert.equal(b.taksitBekleyen,0);assert.equal(b.ayrilan,100);
});
test('HF10 — Kapanıştan önceki D iadesi yalnız kayıtlı tarihinden itibaren etkili',()=>{
 const h=createHarness(),p=plan(h),k={id:'plan-card',isim:'Test',bakiyedenDussun:true};p.taksitler[0].iadeler=[{id:'r',tutar:30,tarih:'2026-09-25',bankaOnayli:false}];
 assert.equal(h.balance({...closing,kartlar:[k],taksitler:[p]}).taksitBekleyen,70);
});
for(const [date,expected] of [['2026-10-16',100],['2026-09-25',0]])test('HF11 — İptal etkisi yalnız kayıtlı tarihte: '+date,()=>{
 const h=createHarness(),p=plan(h),k={id:'plan-card',isim:'Test',bakiyedenDussun:true};p.durum='iptal';p.iptalTarihi=date;p.taksitler[0].durum='iptal';
 assert.equal(h.balance({...closing,kartlar:[k],taksitler:[p]}).taksitBekleyen,expected);
});
test('HF12 — Tarihsiz eski iptal geçmişte kesin aktif/ödenmiş sayılmaz, eksiklik bildirilir',()=>{
 const h=createHarness(),p=plan(h),k={id:'plan-card',isim:'Test',bakiyedenDussun:true};p.durum='iptal';p.taksitler[0].durum='iptal';
 const r=h.call('kartTaksitYukumlulukleri',[p],[k],[],[],closing.ilk,closing.son,closing.son);assert.equal(r.eksik,true);assert.equal(r.satirlar[0].durum,'belirsiz');
});
test('HF13 — Doğrudan D rezerv çağrısı da bugünü geçmiş kapanışına sınırlar',()=>{
 const h=createHarness(),p=plan(h),k={id:'plan-card',isim:'Test',bakiyedenDussun:true};p.taksitler[0].iadeler=[{id:'r',tutar:100,tarih:'2026-10-16',bankaOnayli:false}];
 const d=h.call('kartTaksitYukumlulukleri',[p],[k],[],[],closing.ilk,closing.son,'2026-10-16');assert.equal(d.ayrilan,100);
});
