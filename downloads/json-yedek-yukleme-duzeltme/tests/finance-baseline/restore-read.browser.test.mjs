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


test('UI-R01 — UTF-8 BOM başlıklı geçerli yedek önizlenir; kayıt değişmez',async()=>{
 const {context,page,errors}=await openSynthetic(seedC);
 try{await settings(page);const before=await userData(page);await choose(page,'\uFEFF'+JSON.stringify(packet(seedC)));await dialog(page).waitFor();assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-R02 — Yerel bozuk kayıt dosya JSON hatası olarak sunulmaz',async()=>{
 const {context,page,errors}=await openSynthetic(seedC);
 try{await settings(page);await page.evaluate(()=>localStorage.setItem('ft_synthetic_corrupt','{invalid'));const before=await userData(page);await choose(page,packet(seedC));await page.getByText(/Cihazdaki mevcut kayıtlar okunamadı/).waitFor();assert.equal(await dialog(page).count(),0);assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-R03 — Eski WebView File.text yoksa FileReader ile geçerli dosya açılır',async()=>{
 const {context,page,errors}=await openSynthetic(seedC);
 try{await settings(page);await page.evaluate(()=>Object.defineProperty(Blob.prototype,'text',{value:undefined,configurable:true}));const before=await userData(page);await choose(page,packet(seedC));await dialog(page).waitFor();assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-R04 — Dosya okuma izni hatası JSON hatasından ayrılır',async()=>{
 const {context,page,errors}=await openSynthetic(seedC);
 try{await settings(page);await page.evaluate(()=>Object.defineProperty(Blob.prototype,'text',{value:()=>Promise.reject(new DOMException('denied','NotReadableError')),configurable:true}));const before=await userData(page);await choose(page,packet(seedC));await page.getByText(/Yedek dosyası okunamadı/).waitFor();assert.equal(await dialog(page).count(),0);assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-R05 — Değişmeyen kayıtların yedeği değişiklik yok olarak yüklenebilir',async()=>{
 const {context,page,errors}=await openSynthetic(seedC);
 try{await settings(page);const before=await userData(page);const unchanged=await page.evaluate(()=>Object.fromEntries(Object.keys(localStorage).filter(k=>k.startsWith('ft_')&&!k.startsWith('ft_auto_backup')&&!k.startsWith('ft_manual_backup')).map(k=>[k,JSON.parse(localStorage.getItem(k))])));await choose(page,{format:'finora-v2',yedekTuru:'tam',anahtarlar:Object.keys(unchanged),data:unchanged});await dialog(page).waitFor();assert.match(await dialog(page).innerText(),/Eklenecek: 0 · Silinecek: 0 · Değişecek: 0/);assert.equal(await dialog(page).getByRole('button',{name:'Yedeği yükle ve değiştir'}).isEnabled(),true);await dialog(page).getByRole('button',{name:'Vazgeç',exact:true}).click();assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
