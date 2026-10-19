import test, { before, after } from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import path from 'node:path';
import { createRequire } from 'node:module';
import { source } from './source-harness.mjs';

// Compile unchanged source entirely in memory. No Vite startup, dist output,
// production storage, working source copy, or installed package is changed.
const dependencyRoot = process.env.FINANS_DEPENDENCY_ROOT || '/workspace/finans-upload/finans-takip';
const requireExisting = createRequire(path.join(dependencyRoot, 'package.json'));
const { build } = requireExisting('esbuild');
const { chromium } = await import(process.env.FINANS_PLAYWRIGHT || '/opt/codex/cua_node/lib/node_modules/playwright-core/index.mjs');
let browser, server, url;

before(async () => {
  const result = await build({
    stdin: { contents: `${source}\nimport {createRoot as __testCreateRoot} from 'react-dom/client';\n__testCreateRoot(document.getElementById('root')).render(<App/>);`, loader: 'jsx', resolveDir: path.join(dependencyRoot, 'src') },
    bundle: true, write: false, format: 'esm', platform: 'browser', jsx: 'automatic',
    define: { 'process.env.NODE_ENV': '"production"' }, logLevel: 'silent',
  });
  const js = result.outputFiles[0].text;
  server = http.createServer((req, res) => {
    res.setHeader('Content-Type', req.url === '/app.js' ? 'text/javascript' : 'text/html; charset=utf-8');
    res.end(req.url === '/app.js' ? js : '<!doctype html><html lang="tr"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><div id="root"></div><script type="module" src="/app.js"></script></html>');
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  url = `http://127.0.0.1:${server.address().port}`;
  browser = await chromium.launch({ executablePath: process.env.FINANS_CHROMIUM || '/usr/bin/chromium', args: ['--no-sandbox'] });
});
after(async () => {
  await browser?.close();
  if (server) await new Promise(resolve => server.close(resolve));
});

async function openSynthetic(data, time = '2026-10-16T12:00:00+03:00') {
  const context = await browser.newContext({ viewport: { width: 390, height: 844 }, timezoneId: 'Europe/Istanbul' });
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(e.message));
  page.on('dialog', dialog => dialog.accept());
  // Native onay yerine aynı kullanıcı onayını tema uyumlu pencerede ver.
  await page.addLocatorHandler(page.getByRole('dialog',{name:'Ödeme Onayı',exact:true}), async dialog => { await dialog.getByRole('button',{name:'Ödemeyi Onayla',exact:true}).click(); });
  await page.clock.install({ time: new Date(time) });
  await page.addInitScript(data => {
    if (localStorage.getItem('synthetic-test-seeded')) return;
    localStorage.setItem('synthetic-test-seeded', 'true');
    Object.entries(data).forEach(([key, value]) => localStorage.setItem(key, JSON.stringify(value)));
  }, { ft_schema_version: '1.7.0', ...data });
  await page.goto(url);
  await page.locator('.home-balance-value').waitFor();
  return { context, page, errors };
}


const income=(id='income',amount=20000)=>({id,tip:'gelir',kategori:'Maaş',tutar:amount,tarih:'2026-10-15'});
const seedC={ft_islemler:[income()],ft_kartlar:[{id:'world',isim:'World',ekstre:0}],ft_krediler:[],ft_kategoriler:{gelir:['Maaş','Sınav ücreti'],gider:['Ediz basketbol','Öykü kreş']},ft_donem_devir_log:{'2026-9-15':{tutar:0,tarih:'2026-10-15'}}};
const packet=data=>({format:'finora-v2',data});
async function settings(page){await page.getByRole('button',{name:'Menüyü aç'}).click();await page.getByRole('button',{name:'Ayarlar',exact:true}).click();}
async function choose(page,p){await page.locator('input[type="file"]').setInputFiles({name:'sentetik-yedek.json',mimeType:'application/json',buffer:Buffer.from(typeof p==='string'?p:JSON.stringify(p))});}
async function userData(page){return page.evaluate(()=>Object.fromEntries(Object.keys(localStorage).filter(k=>k.startsWith('ft_')&&!k.startsWith('ft_auto_backup')&&!k.startsWith('ft_manual_backup')).sort().map(k=>[k,localStorage.getItem(k)])));}
const dialog=page=>page.getByRole('dialog',{name:'Yedek yükleme önizlemesi'});
async function failure(page,key){await page.evaluate(key=>{const original=Storage.prototype.setItem;let once=true;Storage.prototype.setItem=function(k,v){if(k===key&&once){once=false;throw new DOMException('Sentetik kota hatası','QuotaExceededError');}return original.call(this,k,v);};},key);}


const cash=page=>page.getByTestId('nakit-akisi');
async function cashOpen(page){await page.getByRole('button',{name:'Menüyü aç'}).click();await page.getByRole('button',{name:'Nakit Akışı',exact:true}).click();await cash(page).waitFor();}
const cfSeed={ft_islemler:[income()],ft_kartlar:[],ft_krediler:[{id:'l',ad:'Kredi',vade:1,toplamTutar:5000,taksitler:[{no:1,tarih:'2026-10-20',tutar:5000,odendi:false}]}],ft_donem_devir_log:{'2026-9-15':{tutar:0,tarih:'2026-10-15'}}};
const planning=page=>cash(page).getByRole('region',{name:'Planlama Ayarları'});
const finances=async page=>Object.fromEntries(Object.entries(await userData(page)).filter(([k])=>k!=='ft_nakit_planlama_v1'));
const planValue=async page=>page.evaluate(()=>JSON.parse(localStorage.getItem('ft_nakit_planlama_v1')));
async function openEditor(page){await planning(page).getByRole('button',{name:'Planları düzenle',exact:true}).click();}
async function addPlan(page,{name='Kira tahmini',amount='1000',type='gelir',repeat='aylik',date='2026-10-20'}={}){
 const r=planning(page);await r.getByLabel('Plan türü',{exact:true}).selectOption(type);await r.getByLabel('Plan açıklaması',{exact:true}).fill(name);await r.getByLabel('Plan tutarı',{exact:true}).fill(amount);await r.getByLabel('Tekrar sıklığı',{exact:true}).selectOption(repeat);await r.getByLabel('Plan başlangıç tarihi',{exact:true}).fill(date);await r.getByRole('button',{name:'Planı taslağa ekle',exact:true}).click();
}
async function save(page){await planning(page).getByRole('button',{name:'Kaydet',exact:true}).click();await planning(page).getByRole('button',{name:'Planları düzenle',exact:true}).waitFor();}
const def=()=>({surum:1,maas:{aktif:false,gun:null,baslangic:null,tutarKaynagi:'mevcut',tutar:null},planlar:[],baglar:[]});
const one=()=>({...def(),planlar:[{id:'p',tip:'gelir',aciklama:'Ek gelir',tutar:1000,tekrar:'tek',baslangic:'2026-10-20',aktif:true}]});
test('UI-P01 — Plan taslağı kaydedilmezse açılışta yok; Kaydet sonrası hatırlanır',async()=>{
 const {context,page,errors}=await openSynthetic(cfSeed);
 try{const before=await finances(page);await cashOpen(page);await openEditor(page);await addPlan(page);assert.match(await page.getByTestId('cf-son').innerText(),/16\.000/);assert.equal(await planValue(page),null);await planning(page).getByRole('button',{name:'Vazgeç',exact:true}).click();assert.match(await page.getByTestId('cf-son').innerText(),/15\.000/);assert.equal(await planValue(page),null);
 await openEditor(page);await addPlan(page);await save(page);assert.equal((await planValue(page)).planlar.length,1);await page.reload();await page.locator('.home-balance-value').waitFor();await cashOpen(page);assert.match(await page.getByTestId('cf-son').innerText(),/16\.000/);assert.deepEqual(await finances(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-P02 — Plan düzenleme, pasif/aktif ve silme yalnız plan alanını yazar',async()=>{
 const {context,page,errors}=await openSynthetic({...cfSeed,ft_nakit_planlama_v1:one()});
 try{const before=await finances(page);await cashOpen(page);await openEditor(page);await planning(page).getByRole('button',{name:'Düzenle',exact:true}).click();await planning(page).getByLabel('Plan tutarı',{exact:true}).fill('2000');await planning(page).getByRole('button',{name:'Taslağı güncelle',exact:true}).click();await save(page);assert.equal((await planValue(page)).planlar[0].tutar,2000);
 await openEditor(page);await planning(page).getByRole('button',{name:'Pasif yap',exact:true}).click();await save(page);assert.equal((await planValue(page)).planlar[0].aktif,false);assert.match(await page.getByTestId('cf-son').innerText(),/15\.000/);
 await openEditor(page);await planning(page).getByRole('button',{name:'Aktif yap',exact:true}).click();await save(page);assert.match(await page.getByTestId('cf-son').innerText(),/17\.000/);
 await openEditor(page);await planning(page).getByRole('button',{name:'Sil',exact:true}).click();await save(page);assert.equal((await planValue(page)).planlar.length,0);assert.deepEqual(await finances(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-P03 — Maaş yatış günü saklanır, mevcut maaş değişince tahmin güncellenir',async()=>{
 const {context,page,errors}=await openSynthetic({...cfSeed,ft_maas_ayarlar:{mevcutMaas:3000,hesapYil:'2026',hesapDonem:'ocak',zamYil1:'2026'}});
 try{await cashOpen(page);await openEditor(page);await planning(page).getByLabel('Maaşı tahmine dahil et',{exact:true}).check();await planning(page).getByLabel('Maaş yatış günü',{exact:true}).fill('1');await planning(page).getByLabel('Maaş planı başlangıç tarihi',{exact:true}).fill('2026-10-16');await save(page);assert.match(await page.getByTestId('cf-son').innerText(),/18\.000/);await page.reload();await page.locator('.home-balance-value').waitFor();await cashOpen(page);await openEditor(page);assert.equal(await planning(page).getByLabel('Maaş yatış günü',{exact:true}).inputValue(),'1');await planning(page).getByRole('button',{name:'Vazgeç',exact:true}).click();
 await page.getByRole('button',{name:'Menüyü aç'}).click();await page.getByRole('button',{name:'Maaş Hesaplama',exact:true}).click();await page.getByLabel('Mevcut güncel maaş',{exact:true}).fill('4000');await page.getByRole('button',{name:'Kaydet',exact:true}).click();await cashOpen(page);assert.match(await page.getByTestId('cf-son').innerText(),/19\.000/);assert.equal((await planValue(page)).maas.tutarKaynagi,'mevcut');assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-P04 — Açık bağ çift sayımı giderir; tarih/tutar benzerliği yalnız uyarıdır',async()=>{
 const data={...cfSeed,ft_islemler:[income(),{id:'actual',tip:'gelir',kategori:'Ek gelir',tutar:1000,tarih:'2026-10-20'}],ft_nakit_planlama_v1:one()};const {context,page,errors}=await openSynthetic(data);
 try{const before=await finances(page);await cashOpen(page);assert.match(await page.getByTestId('cf-son').innerText(),/17\.000/);assert.match(await cash(page).getByText(/Tahmin belirsiz:/).innerText(),/1 plan/);await openEditor(page);await planning(page).getByLabel('Bağlanacak plan olayı',{exact:true}).selectOption(JSON.stringify(['p','2026-10-20']));await planning(page).getByLabel('Bağlanacak kayıt',{exact:true}).selectOption(JSON.stringify(['islem','actual','']));await planning(page).getByRole('button',{name:'Bağlantıyı taslağa ekle',exact:true}).click();await save(page);assert.match(await page.getByTestId('cf-son').innerText(),/16\.000/);await page.reload();await page.locator('.home-balance-value').waitFor();await cashOpen(page);assert.match(await page.getByTestId('cf-son').innerText(),/16\.000/);assert.deepEqual(await finances(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-P05 — Bozuk planlama alanı korunur, düzenleme ve üzerine yazma engellenir',async()=>{
 const invalid={...one(),surum:99};const {context,page,errors}=await openSynthetic({...cfSeed,ft_nakit_planlama_v1:invalid});
 try{const before=await userData(page);await cashOpen(page);assert.match(await planning(page).getByRole('alert').innerText(),/Planlama verisi geçersiz/);assert.equal(await planning(page).getByRole('button',{name:'Planları düzenle'}).isDisabled(),true);assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-P06 — Planlama yazma kota hatasında eski plan ve gerçek kayıtlar korunur',async()=>{
 const {context,page,errors}=await openSynthetic({...cfSeed,ft_nakit_planlama_v1:one()});
 try{const before=await userData(page);await cashOpen(page);await openEditor(page);await planning(page).getByRole('button',{name:'Pasif yap',exact:true}).click();await failure(page,'ft_nakit_planlama_v1');await planning(page).getByRole('button',{name:'Kaydet',exact:true}).click();await page.getByRole('alert').getByText(/Depolama alanı dolu veya yazma izni kapalı/).waitFor();assert.deepEqual(await userData(page),before);assert.equal(await planning(page).getByRole('button',{name:'Kaydet',exact:true}).count(),1);assert.deepEqual(errors,[]);}finally{await context.close();}
});
for(const tema of ['light','dark','gray'])test(`UI-P07-${tema} — Plan düzenleyici 320px genişlikte temaya uyar, taşmaz`,async()=>{
 const {context,page,errors}=await openSynthetic({...cfSeed,ft_tema:tema,ft_nakit_planlama_v1:one()});
 try{await page.setViewportSize({width:320,height:740});await cashOpen(page);await openEditor(page);const m=await page.evaluate(()=>({w:innerWidth,s:document.documentElement.scrollWidth,theme:document.querySelector('[data-theme]').dataset.theme}));assert.ok(m.s<=m.w+1,JSON.stringify(m));assert.equal(m.theme,tema);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-P08 — Tam dosya yedeği planı içerir; eski yedekte eksik alan korunur',async()=>{
 const {context,page,errors}=await openSynthetic({...cfSeed,ft_nakit_planlama_v1:one()});
 try{const before=await finances(page);await settings(page);const dp=page.waitForEvent('download');await page.getByRole('button',{name:'Yedek Al',exact:true}).click();const chunks=[];for await(const c of await(await dp).createReadStream())chunks.push(c);const exported=JSON.parse(Buffer.concat(chunks).toString());assert.deepEqual(exported.data.ft_nakit_planlama_v1,one());
 await choose(page,{format:'finora-v2',data:{ft_islemler:cfSeed.ft_islemler}});await dialog(page).waitFor();assert.match(await dialog(page).getByRole('region',{name:'Nakit akışı planlama ayarları yükleme etkisi'}).innerText(),/korunacak/);const reload=page.waitForEvent('load');await dialog(page).getByRole('button',{name:'Yedeği yükle ve değiştir'}).click();await reload;await page.locator('.home-balance-value').waitFor();assert.deepEqual(await planValue(page),one());assert.deepEqual(await finances(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-P09 — Plan-only kısmi yedek önizlemesi yüklemeyi engeller, iptal korur',async()=>{
 const {context,page,errors}=await openSynthetic({...cfSeed,ft_nakit_planlama_v1:one()});
 try{const before=await userData(page);await settings(page);await choose(page,{format:'finora-v2',yedekTuru:'kismi',data:{ft_nakit_planlama_v1:def()}});await dialog(page).waitFor();assert.equal(await dialog(page).getByRole('button',{name:'Yedeği yükle ve değiştir'}).isDisabled(),true);assert.match(await dialog(page).innerText(),/yalnız|Yalnız/);await dialog(page).getByRole('button',{name:'Vazgeç',exact:true}).click();assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-P10 — Geçici gelir Kaydet/Vazgeç; açıkça kaydedilen senaryo hatırlanır',async()=>{
 const {context,page,errors}=await openSynthetic(cfSeed);
 try{const before=await finances(page);await cashOpen(page);await cash(page).getByText('Başlangıç bakiyesi ve gelir varsayımları',{exact:true}).click();await cash(page).getByLabel('Beklenen gelir tutarı',{exact:true}).fill('1000');await cash(page).getByLabel('İlk gelir tarihi',{exact:true}).fill('2026-10-20');await cash(page).getByRole('button',{name:'Geliri tahmine ekle',exact:true}).click();await cash(page).getByRole('button',{name:'Gelir senaryosundan vazgeç',exact:true}).click();assert.equal(await planValue(page),null);await cash(page).getByRole('button',{name:'Geliri tahmine ekle',exact:true}).click();await cash(page).getByRole('button',{name:'Geçici gelirleri kaydet',exact:true}).click();assert.equal((await planValue(page)).planlar.length,1);assert.match(await page.getByTestId('cf-son').innerText(),/16\.000/);assert.deepEqual(await finances(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});

test('UI-P11 — Bozuk JSON yerel planlama kaydı sıfırlanmaz',async()=>{
 const {context,page,errors}=await openSynthetic(cfSeed);
 try{await page.evaluate(()=>localStorage.setItem('ft_nakit_planlama_v1','{broken'));const before=await userData(page);await cashOpen(page);assert.match(await planning(page).getByRole('alert').innerText(),/Planlama kaydı okunamadı/);assert.equal(await planning(page).getByRole('button',{name:'Planları düzenle'}).isDisabled(),true);assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-P12 — Sessiz yazma başarısızlığı doğrulanır, eski plan ve finans korunur',async()=>{
 const {context,page,errors}=await openSynthetic({...cfSeed,ft_nakit_planlama_v1:one()});
 try{const before=await userData(page);await cashOpen(page);await openEditor(page);await planning(page).getByRole('button',{name:'Pasif yap',exact:true}).click();await page.evaluate(()=>{const original=Storage.prototype.setItem;Storage.prototype.setItem=function(k,v){if(k==='ft_nakit_planlama_v1')return;return original.call(this,k,v);};});await planning(page).getByRole('button',{name:'Kaydet',exact:true}).click();await page.getByRole('alert').getByText(/Depolama alanı dolu veya yazma izni kapalı/).waitFor();assert.deepEqual(await userData(page),before);assert.equal(await planning(page).getByRole('button',{name:'Kaydet',exact:true}).count(),1);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-P13 — Düzenleme sırasında plan alanı değişirse eski taslak üzerine yazamaz',async()=>{
 const {context,page,errors}=await openSynthetic({...cfSeed,ft_nakit_planlama_v1:one()});
 try{await cashOpen(page);await openEditor(page);await planning(page).getByRole('button',{name:'Pasif yap',exact:true}).click();await page.evaluate(()=>{const p=JSON.parse(localStorage.getItem('ft_nakit_planlama_v1'));p.planlar[0].tutar=7000;localStorage.setItem('ft_nakit_planlama_v1',JSON.stringify(p));});const before=await userData(page);await planning(page).getByRole('button',{name:'Kaydet',exact:true}).click();await page.getByText(/Planlama kaydı düzenleme sırasında değişmiş/).waitFor();assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-P14 — Bozuk planlı yedek dosyası önizleme ve yazmadan reddedilir',async()=>{
 const {context,page,errors}=await openSynthetic({...cfSeed,ft_nakit_planlama_v1:one()});
 try{const before=await userData(page);await settings(page);await choose(page,{format:'finora-v2',data:{ft_islemler:[],ft_nakit_planlama_v1:{...one(),surum:9}}});await page.getByText(/Planlama verisi geçersiz/).waitFor();assert.equal(await dialog(page).count(),0);assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-P15 — Formdaki plan taslağa eklenmediyse Kaydet sessizce yok saymaz',async()=>{
 const {context,page,errors}=await openSynthetic(cfSeed);
 try{const before=await userData(page);await cashOpen(page);await openEditor(page);await planning(page).getByLabel('Plan açıklaması',{exact:true}).fill('Henüz eklenmemiş');await planning(page).getByRole('button',{name:'Kaydet',exact:true}).click();assert.match(await planning(page).getByRole('alert').innerText(),/Önce planı taslağa ekle/);assert.deepEqual(await userData(page),before);await planning(page).getByRole('button',{name:'Vazgeç',exact:true}).click();assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
