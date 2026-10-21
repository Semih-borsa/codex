import test from 'node:test';import assert from 'node:assert/strict';
import {createHarness} from '../finance-baseline/source-harness.mjs';
import {card,oldPeriod,currentPeriod,current,zero,archivedZero,observation,statement,legacy,futureStatement,inconsistentArchive} from './fixtures.mjs';
const debt=(h,k=card,rows=[current],history={},period=oldPeriod,payments=[])=>h.call('kartBorcDurumu',k,rows,payments,period,15,[k],history,'2026-10-16');
test('H01 — Doğrulanmış Eylül 0, Ekim 15.000: tam dönem özeti 0 okunur',()=>{
 const h=createHarness(),d=debt(h,card,[current],archivedZero);assert.equal(d.toplam,0);assert.equal(d.biliniyor,true);
 assert.equal(debt(h,card,[current],{},currentPeriod).toplam,15000);
});
test('H02 — Eylülde bilgi yok: güncel 15.000 geçmişe doğrudan kopyalanmaz',()=>{
 const h=createHarness(),d=debt(h);assert.equal(d.biliniyor,false);assert.equal(h.call('kartToplamBorcHesapla',card,[current],[],oldPeriod,15),null);
});
test('H03 — Gerçek Eylül 5.000 ödenmemiş ekstre Ekimde de korunur',()=>{
 const h=createHarness(),k={...card,ekstreler:[statement('old',5000,'2026-09-10','2026-09-20','2026-09-10')]};
 assert.equal(debt(h,k,[]).ekstre,5000);assert.equal(debt(h,k,[],{},currentPeriod).ekstre,5000);
});
test('H04 — Ekimdeki ödeme Eylül kapanışındaki borcu geçmişten silmez',()=>{
 const h=createHarness(),k={...card,ekstreler:[statement('old',5000,'2026-09-10','2026-09-20','2026-09-10')]};
 const pay=[{id:'pay',kartId:card.id,ekstreId:'old',tutar:5000,tarih:'2026-10-16'}];
 assert.equal(debt(h,k,[],{},oldPeriod,pay).ekstre,5000);assert.equal(debt(h,k,[],{},currentPeriod,pay).ekstre,0);
});
test('H05 — 10 Ekim kesilen ekstre, vadesi 20 Ekim olsa da 14 Ekim kapanışında mevcuttur',()=>{
 const h=createHarness(),k={...card,ekstreler:[statement('cut',5000,'2026-10-10','2026-10-20','2026-10-10')]};assert.equal(debt(h,k,[]).ekstre,5000);
});
test('H06 — Kesimi 16 Ekim olan ileri ekstre, erken kayıt tarihiyle Eylüle sızmamalı',()=>{
 const h=createHarness(),k=futureStatement(),d=debt(h,k,[observation('zero','2026-09-30',0)]);assert.equal(d.toplam,0);
});
test('H07 — Sonraki dönemde yapılan yeni alışveriş eski sıfır gözlemini değiştirmez',()=>{
 const h=createHarness(),rows=[observation('zero','2026-09-30',0),current];assert.equal(debt(h,card,rows).toplam,0);
});
test('H08 — Standart güncelleme yolunun sakladığı eski sıfır tarihçesi okunur',()=>{
 const h=createHarness(),r=h.call('kartKaydiniGuncelle',[observation('old','2026-09-30',0)],{donemIci:15000,provizyon:0},'2026-10-16');
 assert.equal(debt(h,card,[r]).toplam,0);
});
test('H09 — Eski üst tarih + yeni bakiye gövdesi, gerçek eski sıfır gözlemini ezmemeli',()=>{
 const h=createHarness(),d=debt(h,card,[legacy()]);assert.notEqual(d.toplam,15000,'Güncel gövde 30 Eylül gözlemi diye okundu');
});
test('H10 — Sonraki dönemin taksit planı geçmiş kart toplamını veya rezervini artırmaz',()=>{
 const h=createHarness(),k={id:card.id,isim:card.isim,bakiyedenDussun:true},p=h.call('kartTaksitPlaniOlustur',{id:'future-plan',kartId:k.id,ad:'Sentetik',toplamTutar:300,vade:3,alisverisTarihi:'2026-10-16',ilkEkstreTarihi:'2026-10-20',kesimGunu:20,odemeGunu:25},[k]);
 const b=h.balance({kartlar:[k],taksitler:[p],ilk:'2026-09-15',son:'2026-10-14'});assert.equal(b.taksitBekleyen,0);assert.equal(debt(h,k,[]).biliniyor,false);
});
test('H11 — D planına Ekimdeki iade Eylül kapanış rezervini geriye dönük azaltmamalı',()=>{
 const h=createHarness(),k={id:card.id,isim:card.isim,bakiyedenDussun:true},p=h.call('kartTaksitPlaniOlustur',{id:'old-plan',kartId:k.id,ad:'Sentetik',toplamTutar:100,vade:1,alisverisTarihi:'2026-09-09',ilkEkstreTarihi:'2026-09-10',kesimGunu:10,odemeGunu:20},[k]);
 const updated=h.call('kartTaksitIadesi',[p],[k],p.id,p.taksitler[0].id,100,'2026-10-16',false).planlar;
 const b=h.balance({kartlar:[k],taksitler:updated,ilk:'2026-09-15',son:'2026-10-14'});assert.equal(b.taksitBekleyen,100);
});
test('H12 — Okuma hesapları kayıtları veya doğrulanmış sıfır arşivini değiştirmez',()=>{
 const h=createHarness(),data={kart:card,islemler:[current],history:archivedZero},before=JSON.stringify(data);
 debt(h,data.kart,data.islemler,data.history);debt(h,data.kart,data.islemler,data.history,currentPeriod);assert.equal(JSON.stringify(data),before);
});
test('H13 — Eski biçimde zamansal çatışmalı ekstre doğrulayıcıdan geçebiliyor (tanı kontrolü)',()=>{
 const h=createHarness();assert.doesNotThrow(()=>h.call('yedekVerisiniDogrula',{format:'finora-v2',data:{ft_islemler:[],ft_kartlar:[futureStatement()],ft_kart_odemeleri:[]}}));
});
test('H14 — Sıfır toplamlı arşivin tutarsız alt ekstre listesi doğrulayıcıdan geçebiliyor (tanı kontrolü)',()=>{
 const h=createHarness();assert.doesNotThrow(()=>h.call('yedekVerisiniDogrula',{format:'finora-v2',data:{ft_islemler:[],ft_kart_borc_ozetleri:inconsistentArchive()}}));
});
test('H15 — Sıfır toplamlı arşivin eski alt ekstresi geçmiş kullanılabilir bakiyeden düşülmemeli',()=>{
 const h=createHarness(),b=h.balance({kartlar:[card],kartBorcOzetleri:inconsistentArchive(),ilk:'2026-09-15',son:'2026-10-14'});
 assert.equal(b.ekstreBekleyen,0);
});
