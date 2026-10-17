import test from 'node:test';
import assert from 'node:assert/strict';
import {createHarness} from './source-harness.mjs';
const clone=v=>JSON.parse(JSON.stringify(v));
const card={id:'card',isim:'World',bakiyedenDussun:true};
const f={id:'plan',ad:'Telefon',kartId:'card',toplamTutar:'100',vade:'3',alisverisTarihi:'2026-10-09',ilkEkstreTarihi:'2026-10-10',kesimGunu:'10',odemeGunu:'20'};
const income={id:'income',tip:'gelir',kategori:'Maaş',tutar:20000,tarih:'2026-10-15'};
const make=(h,fields={})=>h.call('kartTaksitPlaniOlustur',{...f,...fields},[card]);
const withBank=(h,amount=100)=>({...card,ekstre:amount,hesapKesimTarihi:'2026-10-10',sonOdemeTarihi:'2026-10-20',ekstreler:[{id:'bank',kartId:'card',tutar:amount,hesapKesimTarihi:'2026-10-10',sonOdemeTarihi:'2026-10-20',kayitTarihi:'2026-10-10',dogrulandi:true}]});
const bind=(h,plan,k)=>h.call('kartTaksitBagla',[plan],[k],plan.id,plan.taksitler[0].id,'bank')[0];
const summary=(h,p,k,payments=[],plans=[p])=>h.call('kartTaksitSatirOzeti',p,p.taksitler[0],[k],payments,'2026-10-16',plans);
const pay=(amount=20)=>({id:'payment',kartId:'card',ekstreId:'bank',kartAdi:'World',tutar:amount,tarih:'2026-10-16'});
for(const n of [1,2,3,6,12])test(`D01 — ${n} taksit: tarih, benzersiz satır, kuruş toplamı`,()=>{
 const h=createHarness(),p=make(h,{vade:String(n)});
 assert.equal(p.taksitler.length,n);assert.equal(new Set(p.taksitler.map(t=>t.id)).size,n);
 assert.equal(p.taksitler.reduce((s,t)=>s+Math.round(t.tutar*100),0),10000);
 assert.equal(new Set(p.taksitler.map(t=>t.planliEkstreId)).size,n);assert.doesNotThrow(()=>h.call('kartTaksitPlaniniDogrula',p));
});
test('D02 — Ekim ilk ekstre Ekim/Kasım/Aralık; 100 TL tam kuruş dağıtılır',()=>{
 const h=createHarness(),p=make(h);assert.deepEqual(clone(p.taksitler.map(t=>t.kesimTarihi)),['2026-10-10','2026-11-10','2026-12-10']);
 assert.deepEqual(clone(p.taksitler.map(t=>t.tutar)),[33.33,33.33,33.34]);
});
test('D03 — İlk banka ekstresi Kasım ise Ekim kaydı üretilmez; yıl değişir',()=>{
 const h=createHarness(),p=make(h,{ilkEkstreTarihi:'2026-11-10'});
 assert.deepEqual(clone(p.taksitler.map(t=>t.kesimTarihi)),['2026-11-10','2026-12-10','2027-01-10']);
});
for(const [date,gun,expected] of [
 ['2027-01-31',31,['2027-01-31','2027-02-28','2027-03-31']],
 ['2028-01-31',31,['2028-01-31','2028-02-29','2028-03-31']],
 ['2027-01-30',30,['2027-01-30','2027-02-28','2027-03-30']],
 ['2028-01-29',29,['2028-01-29','2028-02-29','2028-03-29']],
 ['2027-02-28',31,['2027-02-28','2027-03-31','2027-04-30']],
])test(`D04 — ${date}, tercih ${gun}: kısa ay kayması kalıcı değildir`,()=>{
 const h=createHarness(),p=make(h,{alisverisTarihi:'2027-01-01',ilkEkstreTarihi:date,kesimGunu:String(gun),odemeGunu:'5'});
 assert.deepEqual(clone(p.taksitler.map(t=>t.kesimTarihi)),expected);
});
test('D05 — Ödeme günü 31 de bağımsız ay sonuna iner ve yeniden 31 olur',()=>{
 const h=createHarness(),p=make(h,{alisverisTarihi:'2027-01-01',ilkEkstreTarihi:'2027-01-10',odemeGunu:'31'});
 assert.deepEqual(clone(p.taksitler.map(t=>t.sonOdemeTarihi)),['2027-01-31','2027-02-28','2027-03-31']);
});
test('D06 — Doğrulanmış ilk ödeme tarihi ay geçişini belirler',()=>{
 const h=createHarness(),p=make(h,{odemeGunu:'5',ilkSonOdemeTarihi:'2026-11-05'});
 assert.deepEqual(clone(p.taksitler.map(t=>t.sonOdemeTarihi)),['2026-11-05','2026-12-05','2027-01-05']);
});
test('D07 — Kesim sınırında ilk bankanın bildirdiği dönem esas alınır',()=>{
 const h=createHarness();assert.equal(make(h,{alisverisTarihi:'2026-10-10'}).taksitler[0].kesimTarihi,'2026-10-10');
 assert.equal(make(h,{alisverisTarihi:'2026-10-10',ilkEkstreTarihi:'2026-11-10'}).taksitler[0].kesimTarihi,'2026-11-10');
 assert.throws(()=>make(h,{alisverisTarihi:'2026-10-11'}),/İlk ekstre alışverişten önce/);
});
for(const fields of [{ilkEkstreTarihi:''},{kartId:'missing'},{kesimGunu:''},{odemeGunu:'',ilkSonOdemeTarihi:''},{toplamTutar:'0.02',vade:'3'},{toplamTutar:'100.001'},{ilkSonOdemeTarihi:'2026-10-05'}])test('D08 — Eksik/bozuk alan tahmin edilmez: '+JSON.stringify(fields),()=>{
 const h=createHarness();assert.throws(()=>make(h,fields));assert.deepEqual(h.storage.snapshot(),{});
});
test('D09 — Plan oluşturmak yeni harcama veya nakit işlemi üretmez',()=>{
 const h=createHarness({state:{islemler:[income],kartlar:[card]}}),before=JSON.stringify(h.context.islemler);h.context.taksitler=[make(h)];
 const b=h.balance();assert.equal(b.gercekBakiye,20000);assert.equal(b.harcama,0);assert.equal(b.taksitBekleyen,33.33);assert.equal(b.odemelerSonrasi,19966.67);assert.equal(JSON.stringify(h.context.islemler),before);
});
test('D10 — Banka aynı yükümlülüğü içeriyorsa plan bakiye rezervine ikinci kez eklenmez',()=>{
 const h=createHarness({state:{islemler:[income],kartlar:[withBank(null,100)]}});h.context.taksitler=[make(h)];
 assert.equal(h.balance().taksitBekleyen,0);assert.equal(h.balance().ekstreBekleyen,100);assert.equal(h.balance().odemelerSonrasi,19900);
});
test('D11 — Bankanın dönem içi/provizyon toplamı varsa plan ayrıca sayılmaz',()=>{
 const h=createHarness({state:{kartlar:[card],islemler:[income,{id:'bankrow',kartId:'card',tip:'gider',kategori:'World',tutar:100,donemIci:50,provizyon:50,kartHarcama:true,tarih:'2026-10-16'}]}});h.context.taksitler=[make(h)];
 assert.equal(h.balance().ayrilan,100);assert.equal(h.balance().taksitBekleyen,0);
});
test('D12 — Birden fazla kart/plan ve gelecek dönem vadeleri ayrı kalır',()=>{
 const h=createHarness(),p=make(h),q=h.call('kartTaksitPlaniOlustur',{...f,id:'other',kartId:'other',toplamTutar:60},[{id:'other',isim:'Bonus'}]);
 const r=h.call('kartTaksitYukumlulukleri',[p,q],[card,{id:'other',isim:'Bonus'}],[],[],'2026-10-15','2026-11-14','2026-10-16');
 assert.equal(r.satirlar.length,6);assert.equal(r.ayrilan,53.33);assert.equal(r.satirlar.filter(t=>t.tarih==='2026-11-20').length,2);
});
test('D13 — Yalnız aynı kartın aynı kesim tarihli kesin ekstresine tek bağ kurulabilir',()=>{
 const h=createHarness(),p=make(h),k=withBank(h),b=bind(h,p,k);assert.equal(b.taksitler[0].ekstreId,'bank');assert.equal(p.taksitler[0].ekstreId,null);
 assert.throws(()=>h.call('kartTaksitBagla',[p],[{...k,ekstreler:k.ekstreler.map(e=>({...e,hesapKesimTarihi:'2026-11-10'}))}],p.id,p.taksitler[0].id,'bank'),/Kesin kart/);
 assert.throws(()=>h.call('kartTaksitBagla',[b],[{...k,ekstreler:[...k.ekstreler,{...k.ekstreler[0],id:'different'}]}],p.id,p.taksitler[0].id,'different'),/zaten bir ekstreye/);
});
test('D14 — Kısmi ödeme payı gerçek nakit kaydını ikinci kez oluşturmaz',()=>{
 const h=createHarness(),k=withBank(h),p=bind(h,make(h),k),o=pay(20);
 const plans=h.call('kartTaksitOdemePayi',[p],[k],[o],p.id,p.taksitler[0].id,o.id,20);
 assert.equal(summary(h,plans[0],k,[o],plans).kalan,13.33);
 h.context.islemler=[income];h.context.kartlar=[k];h.context.kartOdemeleri=[o];h.context.taksitler=plans;
 assert.equal(h.balance().gercekBakiye,19980);assert.equal(h.balance().harcama,0);assert.equal(h.balance().odemelerSonrasi,19900);
});
test('D15 — Aynı ödeme payı iki plana dağıtılıp ödeme tutarını aşamaz',()=>{
 const h=createHarness(),k=withBank(h),p=bind(h,make(h),k),q=bind(h,make(h,{id:'second'}),k),o=pay(20);
 const first=h.call('kartTaksitOdemePayi',[p,q],[k],[o],p.id,p.taksitler[0].id,o.id,20);
 assert.throws(()=>h.call('kartTaksitOdemePayi',first,[k],[o],q.id,q.taksitler[0].id,o.id,1),/Ödeme payı/);
});
test('D16 — Ödeme geri alınınca pay sayılmaz, nakit ve borç birlikte geri döner',()=>{
 const h=createHarness(),k=withBank(h),p=bind(h,make(h),k),o=pay(20),plans=h.call('kartTaksitOdemePayi',[p],[k],[o],p.id,p.taksitler[0].id,o.id,20);
 assert.equal(summary(h,plans[0],k,[],plans).kalan,33.33);
 h.context.islemler=[income];h.context.kartlar=[k];h.context.taksitler=plans;assert.equal(h.balance().gercekBakiye,20000);assert.equal(h.balance().odemelerSonrasi,19900);
});
test('D17 — Tam ekstre ödemesi bağlı taksiti tamamlar; farklı tutarda planı tahminle kapatmaz',()=>{
 const h=createHarness(),k=withBank(h),p=bind(h,make(h),k);assert.equal(summary(h,p,k,[pay(100)]).kalan,0);
 const small=withBank(h,10),b=bind(h,make(h),small);assert.equal(summary(h,b,small,[pay(10)]).kalan,33.33);
});
test('D18 — Aynı ekstrede iki plan toplamı bankadan fazlaysa tam ödeme ikisini varsayımla kapatmaz',()=>{
 const h=createHarness(),k=withBank(h,40),p=bind(h,make(h),k),q=bind(h,make(h,{id:'second'}),k);
 assert.equal(summary(h,p,k,[pay(40)],[p,q]).kalan,33.33);assert.equal(summary(h,q,k,[pay(40)],[p,q]).kalan,33.33);
});
test('D19 — Yalnız plan iadesi banka borcunu veya nakdi değiştirmez',()=>{
 const h=createHarness(),k=withBank(h),p=bind(h,make(h),k),r=h.call('kartTaksitIadesi',[p],[k],p.id,p.taksitler[0].id,10,'2026-10-16',false);
 assert.equal(summary(h,r.planlar[0],r.kartlar[0]).net,23.33);assert.equal(h.call('kartEkstreBakiyeleri',r.kartlar[0],[])[0].kalan,100);
});
test('D20 — Banka onaylı iade özgün ekstreyi değiştirmez; nakit gelir üretmez',()=>{
 const h=createHarness(),k=withBank(h),original=JSON.stringify(k.ekstreler),p=bind(h,make(h),k),r=h.call('kartTaksitIadesi',[p],[k],p.id,p.taksitler[0].id,10,'2026-10-16',true);
 assert.equal(JSON.stringify(r.kartlar[0].ekstreler),original);assert.equal(h.call('kartEkstreBakiyeleri',r.kartlar[0],[])[0].kalan,90);
 h.context.islemler=[income];h.context.kartlar=r.kartlar;h.context.taksitler=r.planlar;assert.equal(h.balance().gercekBakiye,20000);assert.equal(h.balance().odemelerSonrasi,19910);
});
test('D21 — İade geri alındığında borç/plan eski tutara döner; diğer kart değişmez',()=>{
 const h=createHarness(),k=withBank(h),p=bind(h,make(h),k),other={id:'other',isim:'Bonus'};
 const r=h.call('kartTaksitIadesi',[p],[k,other],p.id,p.taksitler[0].id,10,'2026-10-16',true),id=r.planlar[0].taksitler[0].iadeler[0].id;
 const back=h.call('kartTaksitIadeGeriAl',r.planlar,r.kartlar,p.id,p.taksitler[0].id,id);
 assert.equal(summary(h,back.planlar[0],back.kartlar[0]).kalan,33.33);assert.equal(h.call('kartEkstreBakiyeleri',back.kartlar[0],[])[0].kalan,100);assert.deepEqual(back.kartlar[1],other);
});
test('D22 — İade taksiti/ekstreyi aşamaz; bağsız plan bankaya kredi yazamaz',()=>{
 const h=createHarness(),k=withBank(h),p=make(h);assert.throws(()=>h.call('kartTaksitIadesi',[p],[k],p.id,p.taksitler[0].id,34,'2026-10-16',false),/aşamaz/);
 assert.throws(()=>h.call('kartTaksitIadesi',[p],[k],p.id,p.taksitler[0].id,10,'2026-10-16',true),/kesin ekstreyi bağla/);
});
test('D23 — İptal yalnız gelecek bağlı olmayan satırları kapatır; eski ekstre ve geçmiş taksit kalır',()=>{
 const h=createHarness(),k=withBank(h),p=bind(h,make(h),k),r=h.call('kartTaksitIptal',[p],p.id,'2026-10-16')[0];
 assert.equal(r.durum,'iptal');assert.equal(r.taksitler[0].ekstreId,'bank');assert.equal(r.taksitler[0].durum,'planlandi');assert.equal(r.taksitler[1].durum,'iptal');assert.equal(p.taksitler[1].durum,'planlandi');
});
test('D24 — Alışveriş öncesi tarihe iade yazılmaz',()=>{
 const h=createHarness(),p=make(h);assert.throws(()=>h.call('kartTaksitIadesi',[p],[card],p.id,p.taksitler[0].id,1,'2026-10-01',false),/Geçerli iade/);
});
for(const format of ['finora-v2','finora-v1','butce-takip-v1'])test(`D25 — ${format}: tarihli plan/ekstre/iade/bağlantı kaybolmadan okunur`,()=>{
 const h=createHarness(),k=withBank(h),p=bind(h,make(h),k),r=h.call('kartTaksitIadesi',[p],[k],p.id,p.taksitler[0].id,10,'2026-10-16',true);
 const d=h.call('yedekVerisiniDogrula',{format,data:{ft_islemler:[],ft_kartlar:r.kartlar,ft_taksitler:r.planlar,ft_kart_odemeleri:[]}});
 assert.deepEqual(clone(d.ft_taksitler),clone(r.planlar));assert.deepEqual(clone(d.ft_kartlar),clone(r.kartlar));
});
test('D26 — Eski takip kaydına kendiliğinden tarih veya nakit eklenmez',()=>{
 const h=createHarness(),old={id:'legacy',ad:'Eski telefon',aylikTutar:1000,vade:3,odenen:1,kart:'World'};const raw=JSON.stringify(old);
 const d=h.call('yedekVerisiniDogrula',{format:'finora-v1',data:{ft_islemler:[],ft_taksitler:[old]}});assert.equal(JSON.stringify(d.ft_taksitler[0]),raw);
 assert.equal(h.call('kartTaksitYukumlulukleri',[old],[card],[],[],'2026-10-15','2026-11-14').ayrilan,0);
});
test('D27 — Tarihi bilinmeyen yeni takip listesi tarih_bekliyor olarak kalır (S07a koşulu)',()=>{
 const h=createHarness({state:{form:{ad:'Eski biçim yeni takip',aylikTutar:'1000',vade:'3',odenen:'0',kart:'World'}}});h.callback('TaksitlerTab','kaydet');
 assert.equal(h.context.taksitler[0].taksitler.length,3);assert.equal(h.context.taksitler[0].taksitler[0].kesimTarihi,null);assert.equal(h.context.taksitler[0].taksitler[0].durum,'tarih_bekliyor');
});
for(const change of [p=>p.taksitler[0].tutar=34,p=>p.taksitler[0].kesimTarihi='2026-09-10',p=>p.taksitler[0].ekstreDonemi='2026-11',p=>p.taksitler[1].id=p.taksitler[0].id])test('D28 — Bozuk plan yedeği reddedilir: '+change.toString(),()=>{
 const h=createHarness(),p=make(h);change(p);assert.throws(()=>h.call('yedekVerisiniDogrula',{format:'finora-v2',data:{ft_islemler:[],ft_kartlar:[card],ft_taksitler:[p]}}));
});
test('D29 — Yeni cihaza eksik kart veya bozuk ekstre bağıyla geri yükleme yapılmaz',()=>{
 const h=createHarness(),p=make(h);h.context.bekleyenYedek={format:'finora-v2',data:{ft_islemler:[],ft_taksitler:[p]}};h.callback('AyarlarTab','yedekUygula');assert.equal(h.storage.getItem('ft_islemler'),null);assert.match(h.context.bekleyenYedek.yuklemeHatasi,/kredi kartı eksik/);
});
test('D30 — Silinmiş ödeme payı yedekte korunur; geri alınmış ödeme olarak sayılmaz',()=>{
 const h=createHarness(),k=withBank(h),p=bind(h,make(h),k),o=pay(20),r=h.call('kartTaksitOdemePayi',[p],[k],[o],p.id,p.taksitler[0].id,o.id,20);
 const d=h.call('yedekVerisiniDogrula',{format:'finora-v2',data:{ft_islemler:[],ft_kartlar:[k],ft_taksitler:r,ft_kart_odemeleri:[]}});
 assert.equal(summary(h,d.ft_taksitler[0],k).kalan,33.33);
});
test('D31 — Tarihli plan rezervi tahminde tek sefer sayılır, günlere çoğaltılmaz',()=>{
 const h=createHarness(),p=make(h),r=h.call('donemSonuGiderTahmini',{islemler:[],kartlar:[card],taksitler:[p],baslangic:new Date(2026,9,15),bitis:new Date(2026,10,14),bugun:new Date(2026,9,16)});
 assert.equal(r.tahmin,33.33);assert.equal(r.degiskenGider,0);
});

test('D32 — Eski bankanın önceki ekstresi gelecekteki ayrı taksiti gizlemez',()=>{
 const h=createHarness(),k=withBank(h),p=make(h);
 const r=h.call('kartTaksitYukumlulukleri',[p],[k],[],[],'2026-11-15','2026-12-14','2026-11-16');
 assert.equal(r.satirlar[1].bankada,false);assert.equal(r.satirlar[1].ayrilacak,33.33);
});
test('D33 — Alışverişten önceki banka gözlemi yeni taksiti içeriyor sayılmaz',()=>{
 const h=createHarness(),p=make(h),old={id:'old',kartId:'card',tip:'gider',kartHarcama:true,kategori:'World',tutar:100,donemIci:100,provizyon:0,tarih:'2026-10-01'};
 const r=h.call('kartTaksitYukumlulukleri',[p],[card],[old],[],'2026-10-15','2026-11-14');assert.equal(r.ayrilan,33.33);
});
test('D34 — Tek taksit tam ödeme sonrası tamamlanır, geri almada aktif olur',()=>{
 const h=createHarness(),k=withBank(h,100),p=bind(h,make(h,{vade:1}),k);
 assert.equal(h.call('kartTaksitPlanDurumu',p,[k],[pay(100)]),'tamamlandi');assert.equal(h.call('kartTaksitPlanDurumu',p,[k],[]),'aktif');
});
test('D35 — Kuruş altı iade ve gelecek tarihli iade reddedilir',()=>{
 const h=createHarness(),p=make(h);assert.throws(()=>h.call('kartTaksitIadesi',[p],[card],p.id,p.taksitler[0].id,'0.001','2026-10-16',false));
 assert.throws(()=>h.call('kartTaksitIadesi',[p],[card],p.id,p.taksitler[0].id,1,'2026-10-17',false));
});
test('D36 — Pay dağıtılmış banka ödemesi pay toplamının altına düzenlenemez',()=>{
 const h=createHarness(),k=withBank(h),p=bind(h,make(h),k),o=pay(20);
 h.context.taksitler=h.call('kartTaksitOdemePayi',[p],[k],[o],p.id,p.taksitler[0].id,o.id,20);
 h.context.duzenleHedef={kartEkstreOdeme:true,kartOdemeId:o.id};h.context.duzenleForm={tutar:'10',tarih:'2026-10-16'};
 h.callback('IslemlerTab','islemDuzenle');assert.match(h.notices.at(-1)[0],/paylarının altına/);assert.equal(h.calls.length,0);
});

test('D37 — Bankanın doğruladığı istisnai ilk kesim/vade tercih edilen günden önceliklidir; sonraki ay kuralına döner',()=>{
 const h=createHarness(),p=make(h,{ilkEkstreTarihi:'2026-10-12',kesimGunu:'10',ilkSonOdemeTarihi:'2026-10-22',odemeGunu:'20'});
 assert.deepEqual(Array.from(p.taksitler,t=>t.kesimTarihi),['2026-10-12','2026-11-10','2026-12-10']);
 assert.deepEqual(Array.from(p.taksitler,t=>t.sonOdemeTarihi),['2026-10-22','2026-11-20','2026-12-20']);
 assert.doesNotThrow(()=>h.call('kartTaksitPlaniniDogrula',p));
});

for(const [tercih,mod,beklenen] of [[undefined,'nakit',0],[undefined,'kartlar_dahil',33.33],[true,'nakit',33.33],[false,'kartlar_dahil',0]])test(`D38 — Kart tercihi ${tercih} / bakiye modu ${mod} ortak motor ve tahminde korunur`,()=>{
 const h=createHarness(),p=make(h),k={...card};if(tercih===undefined)delete k.bakiyedenDussun;else k.bakiyedenDussun=tercih;
 assert.equal(h.balance({islemler:[income],kartlar:[k],taksitler:[p],bakiyeHesapModu:mod}).taksitBekleyen,beklenen);
 const tahmin=h.call('donemSonuGiderTahmini',{islemler:[],kartlar:[k],taksitler:[p],baslangic:new Date('2026-10-15T12:00:00+03:00'),bitis:new Date('2026-11-14T12:00:00+03:00'),bugun:new Date('2026-10-16T12:00:00+03:00'),bakiyeHesapModu:mod});
 const bos=h.call('donemSonuGiderTahmini',{islemler:[],kartlar:[k],taksitler:[],baslangic:new Date('2026-10-15T12:00:00+03:00'),bitis:new Date('2026-11-14T12:00:00+03:00'),bugun:new Date('2026-10-16T12:00:00+03:00'),bakiyeHesapModu:mod});
 assert.equal(Math.round((tahmin.tahmin-bos.tahmin)*100),Math.round(beklenen*100));
});
