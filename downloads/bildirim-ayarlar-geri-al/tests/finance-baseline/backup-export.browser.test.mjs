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

async function openSynthetic(data, time = '2026-10-16T12:00:00+03:00', nativeConfig={enabled:true}) {
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
  if(nativeConfig.enabled)await page.addInitScript(config=>{
    window.androidBridge={};window.__exports=[];
    window.Capacitor={PluginHeaders:config.missing?[]:[{name:'FinansBackup',methods:[{name:'save',rtype:'promise'}]}],
      nativePromise:(name,method,options)=>{
        if(name!=='FinansBackup')return Promise.reject(new Error('Only synthetic backup bridge is available'));
        window.__exports.push(options);
        return new Promise((resolve,reject)=>{window.__backupResolve=resolve;window.__backupReject=message=>reject(new Error(message));});
      }};
  },nativeConfig);
  await page.goto(url);
  await page.locator('.home-balance-value').waitFor();
  return { context, page, errors };
}


const seed={ft_islemler:[{id:'income',tip:'gelir',kategori:'Maaş',aciklama:'Türkçe Öykü ₺',tutar:20000,tarih:'2026-10-15'}],ft_kartlar:[],ft_kart_odemeleri:[],ft_krediler:[],ft_taksitler:[],ft_odemeler:[],ft_donem_devir_log:{'2026-9-15':{tutar:0,tarih:'2026-10-15'}}};
async function settings(page){await page.getByRole('button',{name:'Menüyü aç'}).click();await page.getByRole('button',{name:'Ayarlar',exact:true}).click();}
async function records(page){return page.evaluate(()=>Object.fromEntries(['ft_islemler','ft_kartlar','ft_kart_odemeleri','ft_krediler','ft_taksitler','ft_odemeler','ft_donem_devir_log'].map(k=>[k,localStorage.getItem(k)])));}
async function clickExport(page){await page.getByRole('button',{name:'Yedek Al',exact:true}).click();await page.waitForFunction(()=>window.__exports.length===1);}
async function resolveSaved(page,extra={}){await page.evaluate(extra=>{const p=window.__exports[0];window.__backupResolve({status:'saved',verified:true,bytes:new Blob([p.json]).size,...extra});},extra);}

test('UI-EX01 — Native kaydetme beklenirken görünür durum/tek işlem; yalnız doğrulama sonrası başarı',async()=>{
 const {context,page,errors}=await openSynthetic(seed);
 try{await settings(page);const before=await records(page);await clickExport(page);
  assert.equal(await page.getByRole('button',{name:'Kaydetme bekleniyor…'}).isDisabled(),true);assert.match(await page.getByRole('status').innerText(),/hazırlanıyor/);
  assert.equal(await page.getByRole('status').getByText(/kaydedildi ve doğrulandı/).count(),0);
  await page.evaluate(()=>document.querySelector('button[aria-busy="true"]').click());assert.equal(await page.evaluate(()=>window.__exports.length),1);
  const payload=await page.evaluate(()=>window.__exports[0]);const p=JSON.parse(payload.json);assert.equal(p.format,'finora-v2');assert.equal(p.yedekTuru,'tam');assert.deepEqual(p.data.ft_islemler,seed.ft_islemler);
  await resolveSaved(page);await page.getByRole('button',{name:'Yedek Al',exact:true}).waitFor();assert.match(await page.getByRole('status').innerText(),/kaydedildi ve doğrulandı/);assert.deepEqual(await records(page),before);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-EX02 — Sistem iptali nötr mesaj; kayıt değişmez, başarı/Blob indirmesi yok',async()=>{
 const {context,page,errors}=await openSynthetic(seed);let downloads=0;page.on('download',()=>downloads++);
 try{await settings(page);const before=await records(page);await clickExport(page);await page.evaluate(()=>window.__backupResolve({status:'cancelled'}));
  await page.getByRole('button',{name:'Yedek Al',exact:true}).waitFor();assert.match(await page.getByRole('status').innerText(),/iptal edildi/);assert.doesNotMatch(await page.getByRole('status').innerText(),/kaydedildi ve doğrulandı/);assert.equal(downloads,0);assert.deepEqual(await records(page),before);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-EX03 — Native yazma hatası görünür Türkçe hata ve tekrar deneme; kayıt değişmez',async()=>{
 const {context,page,errors}=await openSynthetic(seed);
 try{await settings(page);const before=await records(page);await clickExport(page);await page.evaluate(()=>window.__backupReject('Dosya yazılamadı. İzni kontrol et.'));
  assert.match(await page.getByRole('alert').innerText(),/Yedek kaydedilemedi.*Dosya yazılamadı/);assert.equal(await page.getByRole('button',{name:'Yedek Al',exact:true}).isEnabled(),true);assert.deepEqual(await records(page),before);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-EX04 — Eski APK’da plugin yoksa APK güncellemesi gerektiği açıkça görünür',async()=>{
 const {context,page,errors}=await openSynthetic(seed,undefined,{enabled:true,missing:true});let downloads=0;page.on('download',()=>downloads++);
 try{await settings(page);await page.getByRole('button',{name:'Yedek Al',exact:true}).click();assert.match(await page.getByRole('alert').innerText(),/güncel Android APK/);assert.equal(downloads,0);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-EX05 — İnternetsiz native dışa aktarma sözleşmesi çalışır',async()=>{
 const {context,page,errors}=await openSynthetic(seed);
 try{await settings(page);await context.setOffline(true);await clickExport(page);await resolveSaved(page);assert.match(await page.getByRole('status').innerText(),/kaydedildi ve doğrulandı/);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-EX06 — Yerel ek kopya kota hatası gerçek dış dosya başarısını engellemez/ayrı açıklar',async()=>{
 const {context,page,errors}=await openSynthetic(seed);
 try{await settings(page);await page.evaluate(()=>{const original=Storage.prototype.setItem;Storage.prototype.setItem=function(k,v){if(k==='ft_manual_backup_latest')throw new DOMException('Quota','QuotaExceededError');return original.call(this,k,v);};});
  await clickExport(page);await resolveSaved(page);assert.match(await page.getByRole('status').innerText(),/kaydedildi ve doğrulandı.*ek kopya yazılamadı/);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-EX07 — Eksik/bozuk veri dosya seçiciye gönderilmez, kayıtlar korunur',async()=>{
 const {context,page,errors}=await openSynthetic(seed);
 try{await settings(page);const before=await records(page);await page.evaluate(()=>localStorage.setItem('ft_test_bozuk','{bad'));await page.getByRole('button',{name:'Yedek Al',exact:true}).click();
  assert.match(await page.locator('main').getByRole('alert').innerText(),/okunamıyor/);assert.equal(await page.evaluate(()=>window.__exports.length),0);assert.deepEqual(await records(page),before);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-EX08 — Native doğrulama tutarsızlığı sahte başarı yerine hata olur',async()=>{
 const {context,page,errors}=await openSynthetic(seed);
 try{await settings(page);await clickExport(page);await resolveSaved(page,{bytes:1});assert.match(await page.getByRole('alert').innerText(),/doğrulanamadı/);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-EX09 — Son dosya yedeği yalnız doğrulanmış native başarıda kaydedilir',async()=>{
 const {context,page}=await openSynthetic(seed);try{await settings(page);assert.match(await page.getByTestId('son-dosya-yedegi').innerText(),/Henüz/);await clickExport(page);await resolveSaved(page);await page.getByRole('status').filter({hasText:/kaydedildi ve doğrulandı/}).waitFor();assert.ok(await page.evaluate(()=>!!localStorage.getItem('finora_son_dosya_yedegi_v1')));assert.match(await page.getByTestId('son-dosya-yedegi').innerText(),/Son doğrulanmış/);}finally{await context.close();}
});
test('UI-EX10 — İptal edilen kaydetme son başarılı yedek tarihini değiştirmez',async()=>{
 const {context,page}=await openSynthetic(seed);try{await settings(page);await clickExport(page);await page.evaluate(()=>window.__backupResolve({status:'cancelled'}));await page.getByRole('status').filter({hasText:/iptal/}).waitFor();assert.equal(await page.evaluate(()=>localStorage.getItem('finora_son_dosya_yedegi_v1')),null);}finally{await context.close();}
});
