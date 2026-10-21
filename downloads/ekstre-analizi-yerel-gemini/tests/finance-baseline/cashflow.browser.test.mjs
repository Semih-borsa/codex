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
const cfSeed={ft_islemler:[income()],ft_kartlar:[],ft_krediler:[{id:'l',ad:'Kredi',taksitler:[{no:1,tarih:'2026-10-20',tutar:5000,odendi:false}]}],ft_donem_devir_log:{'2026-9-15':{tutar:0,tarih:'2026-10-15'}}};
test('UI-CF01 — Menü, 30/60/90 seçimi ve kredi ödeme günü ayrıntısı',async()=>{
 const {context,page,errors}=await openSynthetic(cfSeed);
 try{const before=await userData(page);await cashOpen(page);assert.match(await page.getByTestId('cf-mevcut').innerText(),/20\.000/);assert.match(await page.getByTestId('cf-son').innerText(),/15\.000/);
 for(const n of [60,90,30]){await cash(page).getByRole('button',{name:`${n} gün`,exact:true}).click();assert.equal(await cash(page).getByRole('button',{name:`${n} gün`,exact:true}).getAttribute('aria-pressed'),'true');}
 await cash(page).getByLabel('Grafikteki günü seç').fill('2026-10-20');assert.match(await page.getByTestId('cf-secili').innerText(),/15\.000/);assert.match(await cash(page).getByRole('region',{name:'Günlük nakit akışı grafiği'}).innerText(),/Kredi · 1. taksit/);assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-CF02 — Maaş varsayımı yalnız ekranda; geri açılışta ve kayıtlarda değişiklik yok',async()=>{
 const {context,page,errors}=await openSynthetic({...cfSeed,ft_maas_ayarlar:{mevcutMaas:30000}});
 try{const before=await userData(page);await cashOpen(page);await cash(page).getByText('Başlangıç bakiyesi ve gelir varsayımları',{exact:true}).click();assert.equal(await cash(page).getByLabel('Beklenen gelir tutarı').inputValue(),'30000');assert.equal(await cash(page).getByLabel('İlk gelir tarihi').inputValue(),'');
 await cash(page).getByRole('button',{name:'Geliri tahmine ekle',exact:true}).click();assert.match(await cash(page).getByRole('alert').innerText(),/ilk tarih gir/);
 await cash(page).getByLabel('İlk gelir tarihi').fill('2026-11-01');await cash(page).getByRole('button',{name:'Geliri tahmine ekle',exact:true}).click();assert.match(await page.getByTestId('cf-son').innerText(),/45\.000/);assert.deepEqual(await userData(page),before);
 await page.reload();await page.locator('.home-balance-value').waitFor();await cashOpen(page);assert.match(await page.getByTestId('cf-son').innerText(),/15\.000/);assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-CF03 — Kullanıcı başlangıç bakiyesi yalnız tahmini etkiler; geçersiz tutar kesin sonuç göstermez',async()=>{
 const {context,page,errors}=await openSynthetic(cfSeed);
 try{const before=await userData(page);await cashOpen(page);await cash(page).getByText('Başlangıç bakiyesi ve gelir varsayımları',{exact:true}).click();await cash(page).getByLabel('Bankadaki güncel bakiye (isteğe bağlı)').fill('8000');assert.match(await page.getByTestId('cf-son').innerText(),/3\.000/);assert.match(await page.getByTestId('cf-mevcut').innerText(),/20\.000/);
 await cash(page).getByLabel('Bankadaki güncel bakiye (isteğe bağlı)').fill('abc');assert.equal(await page.getByTestId('cf-son').innerText(),'—');assert.equal(await cash(page).getByRole('region',{name:'Günlük nakit akışı grafiği'}).count(),0);assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
for(const tema of ['light','dark','gray'])test(`UI-CF04-${tema} — Tema ve 320px ekranda taşma yok; çizgi grafik görünür`,async()=>{
 const {context,page,errors}=await openSynthetic({...cfSeed,ft_tema:tema});
 try{await page.setViewportSize({width:320,height:740});const before=await userData(page);await cashOpen(page);await cash(page).getByRole('button',{name:'90 gün',exact:true}).click();await cash(page).locator('.recharts-surface').waitFor();
 const size=await page.evaluate(()=>({scroll:document.documentElement.scrollWidth,width:innerWidth,tema:document.querySelector('[data-theme]').dataset.theme}));assert.ok(size.scroll<=size.width+1,JSON.stringify(size));assert.equal(size.tema,tema);assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-CF05 — Eksik vade görünür; tarih/tutar uydurulmaz',async()=>{
 const {context,page,errors}=await openSynthetic({...cfSeed,ft_kartlar:[{id:'c',isim:'Bonus',ekstre:5000}]});
 try{await cashOpen(page);assert.match(await cash(page).getByRole('region',{name:'Tahmin kapsamı ve eksik veriler'}).innerText(),/ekstre son ödeme tarihi eksik/);assert.match(await page.getByTestId('cf-son').innerText(),/15\.000/);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-CF06 — Ekran aç/kapa mevcut bakiye ve kaynak kayıtlarını korur',async()=>{
 const {context,page,errors}=await openSynthetic(cfSeed);
 try{const before=await userData(page),bakiye=await page.locator('.home-balance-value').innerText();for(let i=0;i<2;i++){await cashOpen(page);await page.getByRole('button',{name:'Menüyü aç'}).click();await page.getByRole('button',{name:'Genel Bakış',exact:true}).click();assert.equal(await page.locator('.home-balance-value').innerText(),bakiye);}assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-CF07 — Grafiğe dokununca seçili gün ve bakiye ayrıntısı değişir',async()=>{
 const {context,page,errors}=await openSynthetic(cfSeed);
 try{await cashOpen(page);const chart=cash(page).locator('.recharts-wrapper');await chart.scrollIntoViewIfNeeded();await chart.hover({position:{x:220,y:80}});await chart.click({position:{x:220,y:80}});const date=await cash(page).getByLabel('Grafikteki günü seç').inputValue();assert.ok(date>'2026-10-16',date);assert.ok(date<='2026-11-15',date);assert.ok((await page.getByTestId('cf-secili').innerText()).length>0);assert.deepEqual(errors,[]);}finally{await context.close();}
});
