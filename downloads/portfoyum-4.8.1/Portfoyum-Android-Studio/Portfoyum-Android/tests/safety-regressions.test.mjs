import test from 'node:test';
import assert from 'node:assert/strict';
import {emptyState,recordSnapshot,periodPerformance,migrate,validateBackup,protectClosedCosts,protectPerformanceHistory,closeHolding,undoCloseHolding,totals} from '../app/src/main/assets/core.mjs';
const now=Date.parse('2026-09-02T12:00:00Z');
const cash=(id,n,pid='p1')=>({id,portfoyId:pid,tip:'try_lirasi',miktar:n,maliyetBirim:1});
const asset=()=>({id:'a',portfoyId:'p1',tip:'hisse',sembol:'TEST',islemler:[{id:'buy',tur:'alis',tarih:'2026-01-01',miktar:100,fiyat:10,komisyon:0}]});
const portfolio=()=>({...emptyState(),varliklar:[asset()],quotes:{hisse_TEST:{bid:20,status:'manual'}}});
const baseline=s=>recordSnapshot(s,'2026-09-01T12:00:00Z');
test('Dışarıdan eklenen 10.000 TL günlük kazanç değildir',()=>{
  const s=baseline({...emptyState(),varliklar:[cash('a',10000)]});s.varliklar.push(cash('b',10000));const r=periodPerformance(s,'p1',1,now);assert.equal(r.value,10000);assert.equal(r.profit,0);assert.equal(r.profitPercent,0);
});
test('Nakit çekimi zarar değildir',()=>{const s=baseline({...emptyState(),varliklar:[cash('a',10000)]});s.varliklar[0].miktar=5000;assert.equal(periodPerformance(s,'p1',1,now).profit,0);});
test('Fiyat kazancı nakit girişinden ayrı hesaplanır',()=>{let s=baseline(portfolio());s.quotes.hisse_TEST.bid=21;s.varliklar.push(cash('b',10000));const r=periodPerformance(s,'p1',1,now);assert.equal(r.profit,100);assert.equal(r.profitPercent,5);});
test('Portföyler arasında nakit aktarımı toplam kazancı değiştirmez',()=>{const s=baseline({...emptyState(),varliklar:[cash('a',10000)]});s.varliklar[0].miktar=6000;s.varliklar.push(cash('b',4000,'p2'));assert.equal(periodPerformance(s,'all',1,now).profit,0);assert.equal(periodPerformance(s,'p1',1,now).profit,0);});
test('Eski değer kaydından tahmini kazanç üretilmez',()=>{const s=portfolio();s.snapshots=[{ts:'2026-09-01T12:00:00Z',total:2000,byPortfolio:{p1:2000}}];assert.equal(periodPerformance(s,'p1',1,now).profit,null);});
test('30 gün önceki kayıt günlük karşılaştırmaya alınmaz',()=>{const s=recordSnapshot(portfolio(),'2026-08-03T12:00:00Z');assert.equal(periodPerformance(s,'p1',1,now),null);});
test('Haftalık ve aylık karşılaştırma kendi gününü kullanır',()=>{let s=recordSnapshot(portfolio(),'2026-08-03T12:00:00Z');s=recordSnapshot(s,'2026-08-26T12:00:00Z');s.quotes.hisse_TEST.bid=22;assert.equal(periodPerformance(s,'p1',7,now).profit,200);assert.equal(periodPerformance(s,'p1',30,now).profit,200);});
test('Eksik fiyatla kazanç karşılaştırması yapılmaz',()=>{const s=baseline(portfolio());delete s.quotes.hisse_TEST;assert.equal(periodPerformance(s,'p1',1,now).profit,null);});
test('Başlangıç maliyeti bilinmiyorsa kazanç kesin gösterilmez',()=>{let s=portfolio();s.varliklar[0].islemler[0].fiyat=0;s=baseline(s);assert.equal(periodPerformance(s,'p1',1,now).profit,null);});
test('Gerçekleşmiş satış kazancı iki kez sayılmaz',()=>{let s=baseline(portfolio());s=closeHolding(s,'a',20,0,'2026-09-02',true,50);assert.equal(periodPerformance(s,'p1',1,now).profit,0);assert.equal(totals(s).value,2000);});
test('Yeni kazanç geçmişi yedekten aynen okunur',()=>{const s=baseline(portfolio());assert.deepEqual(validateBackup(JSON.parse(JSON.stringify(s))).snapshots,s.snapshots);});
for(const [name,patch] of [
  ['null kapalı kayıt',{kapaliPozisyonlar:[null]}],
  ['dizi kapalı kayıt',{kapaliPozisyonlar:[[]]}],
  ['negatif satış tutarı',{kapaliPozisyonlar:[{id:'c',netSatis:-1}]}],
  ['bozuk kazanç',{kapaliPozisyonlar:[{id:'c',kz:'bozuk'}]}],
  ['tekrarlı kapanış kimliği',{kapaliPozisyonlar:[{id:'c'},{id:'c'}]}],
  ['kopuk portföy',{kapaliPozisyonlar:[{id:'c',portfoyId:'yok'}]}],
  ['yanlış tür',{kapaliPozisyonlar:[{id:'c',tip:'yok'}]}],
  ['bozuk arşiv tarihi',{snapshots:[{ts:'bozuk'}]}],
  ['negatif arşiv tutarı',{snapshots:[{ts:'2026-01-01',total:-1}]}],
  ['bozuk arşiv portföyü',{snapshots:[{ts:'2026-01-01',byPortfolio:[]}]}],
  ['çelişkili arşiv günü',{snapshots:[{ts:'2026-01-01T12:00:00Z',day:'2026-01-02'}]}],
  ['bozuk kazanç geçmişi',{snapshots:[{ts:'2026-01-01',performance:{p1:{pnl:'100',complete:true,revision:0}}}]}],
  ['bozuk arşiv listesi',{snapshots:{}}],
  ['null varlık',{varliklar:[null]}],
  ['null portföy',{portfoyler:[null]}],
  ['ileri tarihli kapanış',{kapaliPozisyonlar:[{id:'c',kapanisTarihi:'2099-01-01'}]}],
  ['bozuk geri alma bağlantısı',{kapaliPozisyonlar:[{id:'c',reversible:true,sourceAssetId:'a',assetBeforeClose:{id:'b'}}]}]
])test('Yedek reddi: '+name,()=>{const input={...emptyState(),...patch},before=JSON.stringify(input);assert.throws(()=>validateBackup(input));assert.equal(JSON.stringify(input),before);});
test('Eski sürümün eksik isteğe bağlı alanları korunur',()=>{const raw={portfoyler:emptyState().portfoyler,varliklar:[],kapaliPozisyonlar:[{id:'c',kz:-75}],snapshots:[{ts:123}],fiyatlar:{tefas_ABC:1.2},custom:'koru'};const n=validateBackup(raw);assert.equal(n.kapaliPozisyonlar[0].kz,-75);assert.equal(n.custom,'koru');assert.equal(n.quotes.tefas_ABC.bid,1.2);assert.equal(n.snapshots[0].ts,123);});
test('İleri tarihli alış bugünkü bakiyeye yüklenmez',()=>{const s=portfolio();s.varliklar[0].islemler[0].tarih='2099-01-01';assert.throws(()=>validateBackup(s),/İleri tarihli/);});
test('Geri alınabilir gerçek kapanış yedekten güvenle okunur',()=>{const s=closeHolding(portfolio(),'a',20,0,'2026-02-01',true,50),n=validateBackup(JSON.parse(JSON.stringify(s)));assert.equal(totals(n).value,2000);assert.equal(undoCloseHolding(n,n.kapaliPozisyonlar[0].id).kapaliPozisyonlar.length,0);});
for(const [name,mutate] of [
  ['alış fiyatı',a=>a.islemler[0].fiyat=15],
  ['alış miktarı',a=>a.islemler[0].miktar=120],
  ['alış komisyonu',a=>a.islemler[0].komisyon=10],
  ['alış tarihi',a=>a.islemler[0].tarih='2026-01-02'],
  ['alış silme',a=>a.islemler=[]],
  ['geriye tarihli yeni alış',a=>a.islemler.push({id:'back',tur:'alis',tarih:'2026-01-15',miktar:1,fiyat:10})],
  ['portföy taşıma',a=>a.portfoyId='p2']
])test('Satış maliyeti korunur: '+name,()=>{const old=closeHolding(portfolio(),'a',20,0,'2026-02-01',false,50),next=structuredClone(old);mutate(next.varliklar[0]);assert.throws(()=>protectClosedCosts(old,next),/bağlı satış/);assert.equal(totals(old).realizedPnl,500);});
test('Satış sonrası alış ve not düzenleme engellenmez',()=>{const old=closeHolding(portfolio(),'a',20,0,'2026-02-01',false,50),next=structuredClone(old);next.varliklar[0].islemler[0].not='Düzeltme';next.varliklar[0].islemler.push({id:'later',tur:'alis',tarih:'2026-03-01',miktar:5,fiyat:20});assert.doesNotThrow(()=>protectClosedCosts(old,next));});
test('Kapanışın geri alınması maliyet korumasına takılmaz',()=>{const old=closeHolding(portfolio(),'a',20,0,'2026-02-01',false,50),next=undoCloseHolding(old,old.kapaliPozisyonlar[0].id);assert.doesNotThrow(()=>protectClosedCosts(old,next));});
test('Maliyet düzeltmesi eski kazançla karşılaştırılmaz ve arşiv silinmez',()=>{const old=baseline(portfolio()),next=structuredClone(old);next.varliklar[0].islemler[0].fiyat=15;const safe=protectPerformanceHistory(old,next);assert.equal(safe.snapshots.length,1);assert.equal(periodPerformance(safe,'p1',1,now).profit,null);});
test('Yeni dış nakit kaydı kazanç karşılaştırmasını bozmaz',()=>{const old=baseline(portfolio()),next=structuredClone(old);next.varliklar.push(cash('cash',10000));const safe=protectPerformanceHistory(old,next);assert.equal(periodPerformance(safe,'p1',1,now).profit,0);});

test('JSON dizisi biçimindeki işlem tarihi güvenli şekilde reddedilir',()=>{const s=portfolio();s.varliklar[0].islemler[0].tarih=['2026-01-01'];assert.throws(()=>validateBackup(s),/İşlem tarihi/);});
