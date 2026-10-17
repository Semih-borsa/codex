import test from 'node:test';
import assert from 'node:assert/strict';
import {createHarness} from './source-harness.mjs';
const copy=v=>JSON.parse(JSON.stringify(v));
const card={id:'world',isim:'World',ekstre:0,bakiyedenDussun:true};
const row=(tarih='2026-10-14',di=8000,pr=2000)=>({id:'bank',tip:'gider',kartHarcama:true,kategori:'World',donemIci:di,provizyon:pr,tutar:di+pr,tarih,kartGunBaslangicToplam:di+pr,kartGunBaslangicTarih:tarih,kartGunlukHareketler:{}});
const income={id:'income',tip:'gelir',kategori:'Maaş',tutar:20000,tarih:'2026-10-15'};
const period='2026-9-15';
const dated={...card,hesapKesimTarihi:'2026-10-10',sonOdemeTarihi:'2026-10-20'};
const update=(h,k,amount,date='2026-10-16',payments=[])=>h.call('kartEkstreGuncelle',k,amount,payments,date);

test('B01 — Eski kart kimliği ve kesin tek kategori bağı değişmeden yeni dönemde korunur',()=>{
  const h=createHarness({state:{kartlar:[card],islemler:[income,row()]}}),before=JSON.stringify(h.context.islemler);
  assert.equal(h.balance().kartAyrilan,10000);assert.equal(h.balance().odemelerSonrasi,10000);assert.equal(JSON.stringify(h.context.islemler),before);
});
test('B02 — Kimlikli kartın adı değişse bile eski isimdeki gözlem aynı karta aittir',()=>{
  const h=createHarness(),r={...row(),kartId:'world'},renamed={...card,isim:'VakıfBank World'};
  assert.equal(h.call('kartToplamBorcHesapla',renamed,[r],[],period,15),10000);
});
test('B03 — Aynı isimli iki kartın kimliksiz kaydı otomatik ilişkilendirilmez',()=>{
  const h=createHarness(),cards=[card,{...card,id:'other'}];
  assert.equal(h.call('kartKayitlari',card,[row()],cards).length,0);
  assert.equal(h.call('kartBorcDurumu',card,[row()],[],period,15,cards).biliniyor,false);
  assert.throws(()=>h.call('kartBorcGirdisi',[row()],cards,'World',{donemIci:100,provizyon:0},'2026-10-16'),/belirsiz/);
});
test('B04 — Aynı gün birden fazla eski gözlem birleştirilmez veya toplanmaz',()=>{
  const h=createHarness(),rows=[row(),{...row(),id:'second'}];
  assert.equal(h.call('kartSonDurum',card,rows,'2026-10-16'),null);
  assert.equal(h.call('kartToplamBorcHesapla',card,rows,[],period,15),null);
});
test('B05 — Farklı günlerin banka toplamları toplanmaz, son gözlem esas alınır',()=>{
  const h=createHarness(),rows=[row(),{...row('2026-10-16',9000,2000),id:'new'}];
  assert.equal(h.call('kartToplamBorcHesapla',card,rows,[],period,15),11000);
});
test('B06 — Dönem geçişinden sonra güncelleme yalnız yeni 500 TL alışveriş sayar',()=>{
  const h=createHarness(),r=h.call('kartBorcGirdisi',[{...row(),id:0}],[card],'World',{donemIci:8500,provizyon:2000},'2026-10-16');
  assert.equal(r[0].id,0);
  assert.equal(r.length,1);assert.equal(r[0].kartId,'world');
  const expenses=h.call('gunlukFinansHareketleri',r,[],[],'2026-10-15','2026-11-14','2026-10-16','harcama');
  assert.equal(expenses.reduce((n,i)=>n+i.tutar,0),500);assert.equal(r[0].kartDurumGecmisi['2026-10-14'].donemIci,8000);
});
test('B07 — Provizyondan dönem içine geçiş ve aynı gün tekrar güncelleme mükerrer gider üretmez',()=>{
  const h=createHarness();let r=h.call('kartBorcGirdisi',[row()],[card],'World',{donemIci:10000,provizyon:0},'2026-10-16');
  r=h.call('kartBorcGirdisi',r,[card],'World',{donemIci:10500,provizyon:0},'2026-10-16');
  assert.equal(h.call('kartAnalizHareketleri',r[0]).reduce((n,i)=>n+i.tutar,0),500);
});
test('B08 — Geçmiş gözlem, yeni güncellemenin bugünkü banka toplamından etkilenmez',()=>{
  const h=createHarness(),old=row(),r=h.call('kartKaydiniGuncelle',[old],{donemIci:14000,provizyon:0},'2026-10-16');
  assert.equal(h.call('kartToplamBorcHesapla',card,[r],[],'2026-8-15',15),10000);
  assert.equal(old.tutar,10000);assert.equal(old.kartDurumGecmisi,undefined);
});
test('B09 — Doğrulanmış kapalı dönem özeti güncel ekstre ve ödemelerden bağımsız okunur',()=>{
  const h=createHarness(),snapshot={toplam:13000,donemIci:8000,provizyon:2000,ekstre:3000,tarih:'2026-10-14',kartId:'world',dogrulandi:true,kapsam:'ekstre_donem_ici_provizyon'};
  const history={'2026-8-15':{world:snapshot}},before=JSON.stringify(history);
  assert.equal(h.call('kartToplamBorcHesapla',{...card,ekstre:7000},[row('2026-10-16',99999,0)],[],'2026-8-15',15,history),13000);
  assert.equal(JSON.stringify(history),before);
});
test('B10 — Geçmiş kayıt yoksa bugünkü ekstre geçmişe kopyalanmaz',()=>{
  const h=createHarness();assert.equal(h.call('kartToplamBorcHesapla',{...card,ekstre:7000},[row('2026-10-16')],[],'2026-8-15',15),null);
});
test('B11 — Karşılaştırma yalnız doğrulanmış aynı kapsamlı iki tutarda oluşur',()=>{
  const h=createHarness();assert.equal(h.call('kartBorcDegisimi',10000,{toplam:5000}),null);
  const prior={toplam:5000,dogrulandi:true,kapsam:'ekstre_donem_ici_provizyon'};
  assert.equal(h.call('kartBorcDegisimi',null,prior),null);assert.equal(h.call('kartBorcDegisimi',10000,prior).oran,100);
});
test('B12 — UUID/fallback yeni kimlikleri aynı saatte benzersiz üretir',()=>{
  const h=createHarness(),ids=Array.from({length:1000},()=>h.call('kartKimligi'));assert.equal(new Set(ids).size,1000);
});
test('B13 — Doğrulanmış ekstre tutarı, tarihleri ve kimliği sonradan değişmez',()=>{
  const h=createHarness(),k=update(h,dated,10000),before=JSON.stringify(k);
  assert.throws(()=>update(h,k,11000),/Kesilmiş ekstre tutarı değiştirilemez/);
  assert.equal(JSON.stringify(k),before);assert.equal(update(h,k,10000).ekstreler[0].id,k.ekstreler[0].id);
});
test('B14 — Yeni ekstre, önceki ödenmemiş ekstreyi silmez; toplam 15.000 TL kalır',()=>{
  const h=createHarness(),first=update(h,dated,10000),old=JSON.stringify(first.ekstreler[0]);
  const next=update(h,{...first,hesapKesimTarihi:'2026-11-10',sonOdemeTarihi:'2026-11-20'},5000,'2026-11-10');
  assert.equal(next.ekstreler.length,2);assert.equal(JSON.stringify(next.ekstreler[0]),old);
  assert.equal(h.call('kartEkstreBakiyeleri',next,[]).reduce((n,e)=>n+e.kalan,0),15000);
});
test('B15 — Kısmi ödeme yalnız bağlı ekstreyi azaltır; yeni ekstrenin tutarı değişmez',()=>{
  const h=createHarness(),a=update(h,dated,10000),b=update(h,{...a,hesapKesimTarihi:'2026-11-10',sonOdemeTarihi:'2026-11-20'},5000,'2026-11-10');
  const payment={id:'pay',kartId:'world',ekstreId:a.ekstreler[0].id,tutar:4000,tarih:'2026-10-16'};
  assert.deepEqual(copy(h.call('kartEkstreBakiyeleri',b,[payment]).map(e=>e.kalan)),[6000,5000]);
  assert.equal(update(h,a,6000,'2026-10-16',[payment]).ekstreler[0].tutar,10000);
});
test('B16 — Aynı tarihli/anahtarlı belirsiz eski ödeme yeni ekstrelere varsayımla dağıtılmaz',()=>{
  const h=createHarness(),k={...card,ekstreler:[{id:'a',kartId:'world',tutar:1000,eskiAnahtar:'old',sonOdemeTarihi:'2026-10-20'},{id:'b',kartId:'world',tutar:2000,eskiAnahtar:'old',sonOdemeTarihi:'2026-10-20'}]};
  const payment={kartId:'world',ekstreDonemKey:'old',tutar:500,tarih:'2026-10-16'};
  assert.deepEqual(copy(h.call('kartEkstreBakiyeleri',k,[payment]).map(e=>e.kalan)),[1000,2000]);
});
test('B17 — Eksik eski kesim/vade tarihleri tahmin edilmez',()=>{
  const h=createHarness(),k=update(h,card,1000);assert.equal(k.ekstreler[0].hesapKesimTarihi,null);assert.equal(k.ekstreler[0].sonOdemeTarihi,null);
});
test('B18 — Tarihi bilinmeyen eski ekstre mevcutta korunur fakat geçmişe taşınmaz',()=>{
  const h=createHarness(),k=h.call('kartEkstreleriKoru',{...card,ekstre:3000},'2026-10-16');
  assert.equal(k.ekstreler[0].dogrulandi,false);assert.equal(k.ekstreler[0].hesapKesimTarihi,null);
  assert.equal(h.call('kartToplamBorcHesapla',k,[],[],period,15),3000);
  assert.equal(h.call('kartToplamBorcHesapla',k,[],[],'2026-8-15',15),null);
});
test('B19 — Ekstre ödemesi nakitte bir kez sayılır; alışveriş harcaması artmaz',()=>{
  const h=createHarness(),k=update(h,dated,10000),p={id:'pay',kartId:'world',ekstreId:k.ekstreler[0].id,tutar:10000,tarih:'2026-10-16'};
  h.context.islemler=[income];h.context.kartlar=[k];h.context.kartOdemeleri=[p];
  assert.equal(h.balance().gercekBakiye,10000);assert.equal(h.balance().ekstreBekleyen,0);assert.equal(h.balance().harcama,0);
});
test('B20 — Gelecek tarihli ödeme şimdiden nakit düşmez ve borç ikinci kez ayrılmaz',()=>{
  const h=createHarness(),k=update(h,dated,10000),p={id:'pay',kartId:'world',ekstreId:k.ekstreler[0].id,tutar:10000,tarih:'2026-10-20'};
  h.context.islemler=[income];h.context.kartlar=[k];h.context.kartOdemeleri=[p];
  assert.equal(h.balance().gercekBakiye,20000);assert.equal(h.balance().ekstreBekleyen,0);assert.equal(h.balance().odemelerSonrasi,10000);
});
test('B21 — Pay ayır seçimi kapalıysa eski dönem içi borcu gerçek bakiyeyi veya ayrılanı azaltmaz',()=>{
  const h=createHarness({state:{kartlar:[{...card,bakiyedenDussun:false}],islemler:[income,row()]}});
  assert.equal(h.balance().gercekBakiye,20000);assert.equal(h.balance().kartAyrilan,0);
});
test('B22 — Yeni ekstre ve günlük gözlem alanları eski üç yedek biçiminde korunur',()=>{
  const h=createHarness(),k=update(h,dated,10000),r=h.call('kartBorcGirdisi',[row()],[k],'World',{donemIci:10500,provizyon:0},'2026-10-16');
  for(const format of ['finora-v2','finora-v1','butce-takip-v1']){
    const d=h.call('yedekVerisiniDogrula',{format,data:{ft_islemler:r,ft_kartlar:[k],ft_kart_odemeleri:[]}});
    assert.equal(d.ft_kartlar[0].ekstreler[0].id,k.ekstreler[0].id);assert.equal(d.ft_islemler[0].kartId,'world');
  }
});
test('B23 — Bozuk ekstre kimliği, tarih veya yabancı kart bağlantısı yedekte reddedilir',()=>{
  const h=createHarness(),k=update(h,dated,10000);
  for(const mutate of [k=>k.ekstreler.push({...k.ekstreler[0]}),k=>k.ekstreler[0].tutar='bozuk',k=>k.ekstreler[0].sonOdemeTarihi='2026-02-30',k=>k.ekstreler[0].kartId='other']){
    const bad=copy(k);mutate(bad);assert.throws(()=>h.call('yedekVerisiniDogrula',{format:'finora-v2',data:{ft_islemler:[],ft_kartlar:[bad]}}));
  }
});
test('B24 — Geçmiş borç gözlemine normal ekle/düzenle akışından sessiz yazılamaz',()=>{
  const h=createHarness();assert.throws(()=>h.call('kartBorcGirdisi',[row()],[card],'World',{donemIci:100,provizyon:0},'2026-10-14'),/geçmiş dönem değiştirilmedi/);
});
test('B25 — Önceki dönemin açılış borcu tahminde tek sefer sayılır',()=>{
  const h=createHarness(),r=h.call('donemSonuGiderTahmini',{islemler:[row()],kartlar:[card],baslangic:new Date(2026,9,15),bitis:new Date(2026,10,14),bugun:new Date(2026,9,16)});
  assert.equal(r.tahmin,10000);
});

test('B26 — Kesin eski ekstre anahtarıyla bağlı ödeme arşive geçerken korunur',()=>{
 const h=createHarness(),legacy={...dated,ekstre:10000,ekstreDonemKey:'old-cycle'},p={id:'old-pay',kartId:'world',ekstreDonemKey:'old-cycle',tutar:7000,tarih:'2026-10-15'};
 const archived=h.call('kartEkstreleriKoru',legacy,'2026-10-16');assert.equal(h.call('kartEkstreBakiyeleri',archived,[p])[0].kalan,3000);
 assert.equal(archived.ekstreler[0].dogrulandi,false);assert.equal(archived.ekstreler[0].sonOdemeTarihi,'2026-10-20');
});
test('B27 — Gerçek Özet ödeme işleyicisi iki ekstreye ayrı nakit kaydı yazar',()=>{
 const h=createHarness(),a=update(h,dated,10000),b=update(h,{...a,hesapKesimTarihi:'2026-10-12',sonOdemeTarihi:'2026-10-22'},5000);
 h.context.kartlar=[b];h.context.islemler=[income];h.context.kartOdemeModal={...b,odemeDuzenle:false};h.context.kartOdemeTarih='2026-10-16';h.context.sablonKimligi=()=>h.call('kartKimligi');
 h.callback('Ozet','kartOdemeEkle');assert.equal(h.context.kartOdemeleri.length,2);
 assert.equal(h.balance().gercekBakiye,5000);assert.equal(h.balance().odemelerSonrasi,5000);assert.equal(h.balance().harcama,0);
});
test('B28 — Ekstre ödeme yazması hata verirse yeni arşiv ve nakit birlikte geri alınır',()=>{
 const legacy={...dated,ekstre:5000};const h=createHarness({storage:{ft_kartlar:JSON.stringify([legacy]),ft_kart_odemeleri:'[]'},state:{kartlar:[legacy],kartOdemeModal:legacy,kartOdemeTarih:'2026-10-16'}});
 h.context.sablonKimligi=()=>h.call('kartKimligi');h.context.setKartlar=h.call('useLocalStorage','ft_kartlar',[])[1];h.context.setKartOdemeleri=h.call('useLocalStorage','ft_kart_odemeleri',[])[1];
 h.storage.failOnce('ft_kart_odemeleri');h.callback('Ozet','kartOdemeEkle');
 assert.deepEqual(JSON.parse(h.storage.getItem('ft_kartlar')),[legacy]);assert.equal(h.storage.getItem('ft_kart_odemeleri'),'[]');assert.ok(h.storage.getItem('ft_auto_backup_kart_islem_oncesi_v1'));
});
test('B29 — İptal edilen ödeme kaydı ve arşiv oluşturmaz',()=>{
 const legacy={...dated,ekstre:5000},h=createHarness({state:{kartlar:[legacy],kartOdemeModal:legacy,kartOdemeTarih:'2026-10-16',odemeOnayi:()=>false}});
 h.context.sablonKimligi=()=>h.call('kartKimligi');h.callback('Ozet','kartOdemeEkle');assert.equal(h.context.kartOdemeleri.length,0);assert.equal(h.context.kartlar[0].ekstreler,undefined);assert.deepEqual(h.storage.snapshot(),{});
});
test('B30 — Yeni ekstre ödeme bağı eksik kartla yeni cihaza yüklenemez',()=>{
 const h=createHarness();h.context.bekleyenYedek={format:'finora-v2',data:{ft_islemler:[],ft_kart_odemeleri:[{id:'pay',kartId:'missing',ekstreId:'statement',kartAdi:'World',tutar:1000,tarih:'2026-10-16'}]}};
 h.callback('AyarlarTab','yedekUygula');assert.equal(h.storage.getItem('ft_islemler'),null);assert.match(h.context.bekleyenYedek.yuklemeHatasi,/ekstre ödeme bağlantısı eksik/);
});
test('B31 — Gelecekteki kesim tarihi için kesilmiş ekstre yaratılmaz',()=>{
 const h=createHarness();assert.throws(()=>update(h,{...dated,hesapKesimTarihi:'2026-11-10',sonOdemeTarihi:'2026-11-20'},1000),/Gelecekteki kesim/);
});
test('B32 — Tutar bileşenleri tutmayan doğrulanmış dönem özeti yedekte reddedilir',()=>{
 const h=createHarness(),record={toplam:10000,donemIci:8000,provizyon:2000,ekstre:1000,kartId:'world',tarih:'2026-10-14',dogrulandi:true,kapsam:'ekstre_donem_ici_provizyon'};
 assert.throws(()=>h.call('yedekVerisiniDogrula',{format:'finora-v2',data:{ft_islemler:[],ft_kart_borc_ozetleri:{'2026-8-15':{world:record}}}}),/dönem özeti tutarsız/);
});

test('B33 — Eski maaş-dönemi anahtarlı ekstre ödemesi kesin kart/vade eşleşmesinde korunur',()=>{
 const h=createHarness(),legacy={...dated,ekstre:10000};
 const p={id:'legacy-pay',kartId:'world',ekstreDonemKey:'2026-9-15',sonOdemeTarihi:'2026-10-20',tutar:10000,tarih:'2026-10-16'};
 assert.equal(h.call('kartEkstreBakiyeleri',legacy,[p])[0].kalan,0);
 const archived=h.call('kartEkstreleriKoru',legacy,'2026-10-16');assert.equal(h.call('kartEkstreBakiyeleri',archived,[p])[0].kalan,0);
});
test('B34 — Maaş dönemi anahtarı tek başına ödeme bağlamaz; çelişen açık anahtar aşılmaz',()=>{
 const h=createHarness(),legacy={...dated,ekstre:10000};
 const p={kartId:'world',ekstreDonemKey:'2026-9-15',tutar:10000,tarih:'2026-10-16'};
 assert.equal(h.call('kartEkstreBakiyeleri',legacy,[p])[0].kalan,10000);
 assert.equal(h.call('kartEkstreBakiyeleri',{...legacy,ekstreDonemKey:'other-cycle'},[{...p,sonOdemeTarihi:'2026-10-20'}])[0].kalan,10000);
});
