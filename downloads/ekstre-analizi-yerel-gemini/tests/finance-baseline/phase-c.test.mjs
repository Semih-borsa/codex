import test from 'node:test';
import assert from 'node:assert/strict';
import {createHarness} from './source-harness.mjs';

const clone = v => JSON.parse(JSON.stringify(v));
const row = (id='old', tutar=20000) => ({id,tip:'gelir',kategori:'Maaş',tutar,tarih:'2026-10-15'});
const categories = {gelir:['Sınav ücreti'],gider:['Ediz basketbol','Öykü kreş']};
const seed = () => ({ft_islemler:JSON.stringify([row()]),ft_kartlar:JSON.stringify([{id:'world',isim:'World',ekstre:3000}]),ft_kategoriler:JSON.stringify(categories),ft_manual_backup_latest:'{"sentinel":"eski-dosya"}',unrelated:'koru'});
const packet = data => ({format:'finora-v2',data});
const apply = (h,p) => {h.context.bekleyenYedek=p;h.callback('AyarlarTab','yedekUygula');};
const userData = h => Object.fromEntries(Object.entries(h.storage.snapshot()).filter(([k])=>!k.startsWith('ft_auto_backup')));
async function upload(h,contents,size) {
  const target={value:'selected.json',files:[{size:size??Buffer.byteLength(contents),text:async()=>contents}]};
  await h.callback('AyarlarTab','yedekYukle',{target});assert.equal(target.value,'');
}

for(const format of ['finora-v2','finora-v1','butce-takip-v1']) {
  test(`C01 — ${format}: eksik işlem listesi reddedilir; mevcut kayıtlar korunur`,()=>{
    const h=createHarness({storage:seed()}),before=userData(h);
    apply(h,{format,data:{ft_kartlar:[]}});
    assert.deepEqual(userData(h),before);
    assert.ok(h.storage.getItem('ft_auto_backup_restore_before_v2'));
    assert.match(h.notices.at(-1)[0],/İşlem listesi eksik|kısmi/i);
  });
  test(`C02 — ${format}: eski yedek uygulanır; bulunmayan kartlar ve kategoriler korunur`,()=>{
    const h=createHarness({storage:seed()});
    apply(h,{format,data:{ft_islemler:[row('imported',12000)]}});
    assert.deepEqual(JSON.parse(h.storage.getItem('ft_islemler')),[row('imported',12000)]);
    assert.equal(h.storage.getItem('ft_kartlar'),seed().ft_kartlar);
    assert.equal(h.storage.getItem('ft_kategoriler'),seed().ft_kategoriler);
    assert.equal(h.storage.getItem('ft_manual_backup_latest'),seed().ft_manual_backup_latest);
    assert.equal(h.context.bekleyenYedek,null);
  });
}

test('C03 — Açık boş işlem listesi onaylı yüklemede temizlenir; eksik bölüm korunur',()=>{
  const h=createHarness({storage:seed()});apply(h,packet({ft_islemler:[]}));
  assert.equal(h.storage.getItem('ft_islemler'),'[]');
  assert.equal(h.storage.getItem('ft_kartlar'),seed().ft_kartlar);
  const recovery=JSON.parse(h.storage.getItem('ft_auto_backup_restore_before_v2'));
  assert.deepEqual(recovery.data.ft_islemler,[row()]);
});
test('C04 — İşlem listesi bulunsa bile açık kısmi paket uygulanmaz',()=>{
  const h=createHarness({storage:seed()}),before=userData(h);
  apply(h,{...packet({ft_islemler:[]}),yedekTuru:'kismi'});
  assert.deepEqual(userData(h),before);assert.match(h.notices.at(-1)[0],/kısmi/i);
});
test('C05 — Güvenlik kopyası yazılamazsa tek kullanıcı kaydı bile değişmez',()=>{
  const h=createHarness({storage:seed()}),before=h.storage.snapshot();
  h.storage.failOnce('ft_auto_backup_restore_before_v2');apply(h,packet({ft_islemler:[]}));
  assert.deepEqual(h.storage.snapshot(),before);assert.notEqual(h.context.bekleyenYedek,null);assert.match(h.context.bekleyenYedek.yuklemeHatasi,/güvenlik kopyası oluşturulamadı/);
});
test('C06 — Güvenlik kopyasının geri okuması uyuşmazsa yükleme başlamaz',()=>{
  const h=createHarness({storage:seed()}),before=userData(h),set=h.storage.setItem.bind(h.storage);
  h.storage.setItem=(k,v)=>{if(k!=='ft_auto_backup_restore_before_v2')set(k,v);};
  apply(h,packet({ft_islemler:[]}));assert.deepEqual(userData(h),before);
  assert.match(h.notices.at(-1)[0],/güvenlik kopyası doğrulanamadı/);
});
test('C07 — İkinci veri yazmasındaki hata ilk yazmayı geri alır; güvenlik ve eski dosya korunur',()=>{
  const h=createHarness({storage:seed()}),before=userData(h);
  h.storage.failOnce('ft_kartlar');apply(h,packet({ft_islemler:[row('new')],ft_kartlar:[]}));
  assert.deepEqual(userData(h),before);
  assert.deepEqual(JSON.parse(h.storage.getItem('ft_auto_backup_restore_before_v2')).data.ft_islemler,[row()]);
  assert.match(h.notices.at(-1)[0],/önceki kayıtlar geri yüklendi/);
});
test('C08 — Yazılan değer geri okunarak doğrulanır; sessiz bozuk yazma geri alınır',()=>{
  const h=createHarness({storage:seed()}),before=userData(h),set=h.storage.setItem.bind(h.storage);let fail=true;
  h.storage.setItem=(k,v)=>{if(k==='ft_islemler'&&fail){fail=false;set(k,'[]');}else set(k,v);};
  apply(h,packet({ft_islemler:[row('new')]}));assert.deepEqual(userData(h),before);
  assert.match(h.notices.at(-1)[0],/önceki kayıtlar geri yüklendi/);
});
test('C09 — Yeni anahtar yazıldıktan sonraki hatada yeni anahtar da kaldırılır',()=>{
  const h=createHarness({storage:seed()}),before=userData(h);
  h.storage.failOnce('ft_islemler');apply(h,packet({ft_odemeler:[],ft_islemler:[row('new')]}));
  assert.deepEqual(userData(h),before);assert.equal(h.storage.getItem('ft_odemeler'),null);
});
test('C10 — Bozuk JSON reddedilir, bekleyen önizleme temizlenir; kayıt/yedek yazılmaz',async()=>{
  const h=createHarness({storage:seed(),state:{bekleyenYedek:packet({ft_islemler:[]})}}),before=h.storage.snapshot();
  await upload(h,'{bozuk-json');assert.deepEqual(h.storage.snapshot(),before);
  assert.equal(h.context.bekleyenYedek,null);assert.match(h.notices.at(-1)[0],/JSON/);
});
test('C11 — Dosya seçimi iptal edilirse kayıt ve bekleyen önizleme değişmez',async()=>{
  const p=packet({ft_islemler:[]}),h=createHarness({storage:seed(),state:{bekleyenYedek:p}}),before=h.storage.snapshot();
  await h.callback('AyarlarTab','yedekYukle',{target:{value:'',files:[]}});
  assert.deepEqual(h.storage.snapshot(),before);assert.equal(h.context.bekleyenYedek,p);
});
test('C12 — 10 MB üzeri dosya reddedilir ve eski önizleme uygulanabilir kalmaz',async()=>{
  const h=createHarness({storage:seed(),state:{bekleyenYedek:packet({ft_islemler:[]})}}),before=h.storage.snapshot();
  await upload(h,'{}',10*1024*1024+1);assert.deepEqual(h.storage.snapshot(),before);
  assert.equal(h.context.bekleyenYedek,null);assert.match(h.notices.at(-1)[0],/10 MB/);
});
for(const data of [{ft_islemler:null},{ft_islemler:{}},{ft_islemler:[{...row(),tutar:'bozuk'}]},{ft_islemler:[row(),row()]},{ft_islemler:[{...row(),tarih:'2026-02-30'}]}]) {
  test('C13 — Hatalı tür/tutar/kimlik/tarih yüklemeyi güvenlik kopyasından önce durdurur: '+JSON.stringify(data),()=>{
    const h=createHarness({storage:seed()}),before=h.storage.snapshot();apply(h,packet(data));
    assert.deepEqual(h.storage.snapshot(),before);assert.equal(h.notices.at(-1)[1],'hata');
  });
}
test('C14 — Yeni tam yedeğin alan listesi eksik/bozuk içeriği yakalar',()=>{
  const h=createHarness({storage:seed()}),p=h.call('yerelYedekPaketi');
  assert.equal(p.yedekTuru,'tam');assert.deepEqual(Object.keys(p.data).sort(),[...p.anahtarlar].sort());
  assert.equal(h.call('yedekKapsami',h.call('yedekVerisiniDogrula',p),p).tam,true);
  const missing=clone(p);delete missing.data.ft_islemler;
  assert.throws(()=>h.call('yedekVerisiniDogrula',missing),/alan listesi|eksik/);
  const malformed=clone(p);malformed.anahtarlar=['ft_islemler','ft_islemler'];
  assert.throws(()=>h.call('yedekVerisiniDogrula',malformed),/alan listesi/);
});
test('C15 — Yeni, hiç işlem kaydetmemiş uygulama açık boş işlem listeli yedek üretir',()=>{
  const h=createHarness(),p=h.call('yerelYedekPaketi');
  assert.deepEqual(clone(p.data.ft_islemler),[]);assert.equal(h.call('yedekKapsami',p.data,p).tam,true);
  assert.deepEqual(h.storage.snapshot(),{});assert.doesNotThrow(()=>h.call('yedekVerisiniDogrula',p));
});
test('C16 — Önizlemeden sonra kullanıcı kaydı değişirse eski etki onayı uygulanmaz',()=>{
  const h=createHarness({storage:seed()}),mevcut=clone(h.call('yerelYedekPaketi').data);
  h.storage.setItem('ft_islemler',JSON.stringify([row('updated')]));const before=h.storage.snapshot();
  apply(h,{...packet({ft_islemler:[]}),mevcutVeri:mevcut});
  assert.deepEqual(h.storage.snapshot(),before);assert.match(h.notices.at(-1)[0],/Önizlemeden sonra/);
});
test('C17 — Etki hesabı eklenecek/silinecek/değişecek kayıtları ve eksik bölümü ayırır',()=>{
  const h=createHarness();const old={ft_islemler:[row('delete'),row('change'),row('same')],ft_kartlar:[]};
  const next={ft_islemler:[row('add'),row('change',500),row('same')]};
  const result=clone(h.call('yedekEtkiHesapla',old,next)),e=result.find(e=>e.key==='ft_islemler');
  assert.deepEqual(e.eklenecek.map(i=>i.id),['add']);assert.deepEqual(e.silinecek.map(i=>i.id),['delete']);
  assert.deepEqual(e.degisecek.map(i=>i.sonraki.id),['change']);assert.equal(e.degisecek[0].onceki.tutar,20000);
  assert.equal(result.find(e=>e.key==='ft_kartlar').korunacak,true);
});
test('C18 — Yerel bozuk JSON sessizce ezilmez; güvenlik kopyası alınamadan yükleme durur',()=>{
  const h=createHarness({storage:{...seed(),ft_islemler:'{broken'}}),before=h.storage.snapshot();
  apply(h,packet({ft_islemler:[]}));assert.deepEqual(h.storage.snapshot(),before);
});
test('C19 — Prototip alanı ve bozuk paket türü reddedilir',()=>{
  const h=createHarness();
  const dangerous=JSON.parse('{"format":"finora-v2","data":{"ft_islemler":[],"ft_extra":{"__proto__":{}}}}');
  assert.throws(()=>h.call('yedekVerisiniDogrula',dangerous),/Geçersiz kayıt alanı/);
  assert.throws(()=>h.call('yedekVerisiniDogrula',{...packet({ft_islemler:[]}),yedekTuru:'belirsiz'}),/kapsamı/);
});
test('C20 — Kredi ödeme bağlantıları, kartlar, kategoriler ve geçmiş veri eksiksiz geri yüklenir',()=>{
  const h=createHarness({storage:seed()});
  const paid={id:'payment',tip:'gider',kategori:'Kredi',tutar:5000,tarih:'2026-10-16',kaynak:'kredi_taksiti',krediId:'loan',taksitNo:1};
  const data={ft_islemler:[row('past'),paid],ft_krediler:[{id:'loan',ad:'Kredi',vade:1,taksitler:[{no:1,tutar:5000,tarih:'2026-10-20',odendi:true,odemeIslemId:'payment'}]}],ft_kartlar:[{id:'bonus',isim:'Bonus',ekstre:2000,hesapKesimTarihi:'2026-10-10'}],ft_kategoriler:categories};
  apply(h,packet(data));for(const [k,v] of Object.entries(data))assert.deepEqual(JSON.parse(h.storage.getItem(k)),v);
  assert.equal(h.storage.getItem('unrelated'),'koru');
});
for(const issue of ['missing-payment','missing-loan','duplicate-payment','amount','paid-flag','duplicate-installment']) {
  test(`C21 — Açık kredi bağlantısı bozuksa değiştirmeden reddet: ${issue}`,()=>{
    const h=createHarness({storage:seed()}),before=userData(h);
    const p={id:'pay',tip:'gider',kategori:'Kredi',tutar:5000,tarih:'2026-10-16',kaynak:'kredi_taksiti',krediId:'loan',taksitNo:1};
    const t={no:1,tarih:'2026-10-20',tutar:5000,odendi:true,odemeIslemId:'pay'};
    const data={ft_islemler:[p],ft_krediler:[{id:'loan',vade:1,taksitler:[t]}]};
    if(issue==='missing-payment')data.ft_islemler=[];
    if(issue==='missing-loan')data.ft_krediler=[];
    if(issue==='duplicate-payment')data.ft_islemler.push({...p,id:'pay2'});
    if(issue==='amount')p.tutar=4000;
    if(issue==='paid-flag')t.odendi=false;
    if(issue==='duplicate-installment'){data.ft_krediler[0].vade=2;data.ft_krediler[0].taksitler.push({...t});}
    apply(h,packet(data));assert.deepEqual(userData(h),before);assert.equal(h.notices.at(-1)[1],'hata');
  });
}
test('C22 — Eski ödendi:true taksitten otomatik ödeme türetilmez',()=>{
  const h=createHarness({storage:seed()});
  const data={ft_islemler:[row()],ft_krediler:[{id:'old-loan',vade:1,taksitler:[{no:1,tutar:5000,tarih:'2026-10-01',odendi:true}]}]};
  apply(h,packet(data));assert.deepEqual(JSON.parse(h.storage.getItem('ft_islemler')),[row()]);
});
test('C23 — Eksik kredi bölümü korununca geri yüklenecek işlemlerle bağı bozulursa yükleme engellenir',()=>{
  const loan={id:'loan',vade:1,taksitler:[{no:1,tutar:5000,tarih:'2026-10-20',odendi:true,odemeIslemId:'pay'}]};
  const h=createHarness({storage:{...seed(),ft_krediler:JSON.stringify([loan])}}),before=userData(h);
  apply(h,packet({ft_islemler:[]}));assert.deepEqual(userData(h),before);assert.match(h.notices.at(-1)[0],/kredi ödeme işlemi eksik/);
});
test('C24 — Yazıcıya doğrudan eksik veri verilse de kullanıcı kayıtları silinmez',()=>{
  const h=createHarness({storage:seed()}),before=h.storage.snapshot();
  assert.throws(()=>h.call('yedegiGuvenleYaz',{ft_kartlar:[]},h.storage),/İşlem listesi eksik/);
  assert.deepEqual(h.storage.snapshot(),before);
});

test('C25 — İkinci alan sürekli yazma hatası verse de değişmemiş alanı yeniden yazmadan geri al',()=>{
  const h=createHarness({storage:seed()}),before=userData(h),set=h.storage.setItem.bind(h.storage);
  h.storage.setItem=(k,v)=>{if(k==='ft_kartlar')throw new Error('Kalıcı yazma engeli');set(k,v);};
  apply(h,packet({ft_islemler:[],ft_kartlar:[]}));
  assert.deepEqual(userData(h),before);assert.match(h.notices.at(-1)[0],/önceki kayıtlar geri yüklendi/);
});
test('C26 — Geri alma da engellenirse başarı bildirme; eski veri güvenlik kopyası ve kurtarma dosyasında kalmalı',async()=>{
  const h=createHarness({storage:seed()}),set=h.storage.setItem.bind(h.storage);let rowWrites=0,download,blob;
  h.context.URL={createObjectURL:b=>{blob=b;return 'blob:synthetic-recovery';},revokeObjectURL:()=>{}};
  h.context.document.createElement=()=>({click(){download=this.download;}});
  h.storage.setItem=(k,v)=>{if(k==='ft_kartlar'||(k==='ft_islemler'&&++rowWrites>1))throw new Error('Kalıcı depolama engeli');set(k,v);};
  apply(h,packet({ft_islemler:[],ft_kartlar:[]}));
  assert.match(h.context.bekleyenYedek.yuklemeHatasi,/Depolama kullanılamıyor/);
  assert.equal(download,'finora-yukleme-oncesi-kurtarma.json');
  assert.deepEqual(JSON.parse(await blob.text()).data.ft_islemler,[row()]);
  assert.deepEqual(JSON.parse(h.storage.getItem('ft_auto_backup_restore_before_v2')).data.ft_islemler,[row()]);
  assert.equal(h.notices.some(([m])=>m==='Yedek geri yüklendi'),false);
});

test('C27 — Yeni/boş cihazda kredi listesi eksikse bağlantılı ödeme sahipsiz içeri aktarılamaz',()=>{
  const p=packet({ft_islemler:[{id:'pay',tip:'gider',kategori:'Kredi',tutar:5000,tarih:'2026-10-16',kaynak:'kredi_taksiti',krediId:'missing-loan',taksitNo:1}]});
  const h=createHarness(),before=userData(h);apply(h,p);
  assert.deepEqual(userData(h),before);assert.match(h.context.bekleyenYedek.yuklemeHatasi,/bağlantıları tutarsız/);
  assert.throws(()=>h.call('yedegiGuvenleYaz',p.data,h.storage),/bağlantıları tutarsız/);
  assert.deepEqual(userData(h),before);
});

test('C28 — Korunacak yerel kredi bölümü null/bozuk türdeyse bağlantı kontrolü sessizce atlanmaz',()=>{
  for(const malformed of ['null','{}','"bozuk"']) {
    const h=createHarness({storage:{...seed(),ft_krediler:malformed}}),before=userData(h);
    apply(h,packet({ft_islemler:[]}));assert.deepEqual(userData(h),before);
    assert.match(h.context.bekleyenYedek.yuklemeHatasi,/kredi listesi bozuk/);
  }
});
