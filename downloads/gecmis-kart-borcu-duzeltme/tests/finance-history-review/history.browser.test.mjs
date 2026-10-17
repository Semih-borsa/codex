import test, { before, after } from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import path from 'node:path';
import { createRequire } from 'node:module';
import { source } from '../finance-baseline/source-harness.mjs';

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


import {card,current,zero,archivedZero,observation,statement,legacy,futureStatement,inconsistentArchive,seed} from './fixtures.mjs';
async function previous(page){await page.getByRole('button',{name:'Önceki dönem',exact:true}).click();}
async function next(page){await page.getByRole('button',{name:'Sonraki dönem',exact:true}).click();}
async function finances(page){return page.evaluate(()=>Object.fromEntries(['ft_islemler','ft_kartlar','ft_kart_odemeleri','ft_odemeler','ft_krediler','ft_taksitler','ft_donem_devir_log'].map(k=>[k,localStorage.getItem(k)])));}
test('UI-H01 — Doğru sıfır arşivi Eylülde 0; güncele dönünce borç/bakiye/kayıtlar aynı',async()=>{
 const {context,page,errors}=await openSynthetic(seed([card],[current],archivedZero));
 try{await page.locator('.credit-total-row').waitFor();const before=await finances(page),cash=await page.locator('.home-balance-value').innerText(),total=await page.locator('.credit-total-row').innerText();
 await previous(page);assert.match(await page.locator('.credit-total-row > .money-fit').innerText(),/0,00/);assert.doesNotMatch(await page.locator('.credit-buckets').innerText(),/15\.000/);
 await next(page);assert.equal(await page.locator('.home-balance-value').innerText(),cash);assert.equal(await page.locator('.credit-total-row').innerText(),total);assert.deepEqual(await finances(page),before);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-H02 — Geçmiş kayıt yoksa kartın ana borç alanında bilinmiyor gösterilir',async()=>{
 const {context,page,errors}=await openSynthetic(seed());
 try{await previous(page);assert.match(await page.locator('.credit-total-row').innerText(),/Geçmiş veri yok/);assert.equal(await page.locator('.credit-total-row > .money-fit').count(),0);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-H03 — Eski gövde geri dönüşü ekranda Eylül sıfır yerine 15.000 göstermemeli',async()=>{
 const {context,page,errors}=await openSynthetic(seed([card],[legacy()]));
 try{await previous(page);assert.doesNotMatch(await page.locator('.credit-total-row').innerText(),/15\.000/);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-H04 — Doğrulanmış sıfır toplamın eski alt ekstre listesi kartta 15.000 göstermemeli',async()=>{
 const {context,page,errors}=await openSynthetic(seed([card],[current],inconsistentArchive()));
 try{await previous(page);assert.match(await page.locator('.credit-total-row > .money-fit').innerText(),/0,00/);assert.doesNotMatch(await page.locator('.credit-buckets').innerText(),/15\.000/);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-H05 — Sonraki kesim tarihli erken kaydedilmiş ekstre geçmiş kartta görünmemeli',async()=>{
 const {context,page,errors}=await openSynthetic(seed([futureStatement()],[observation('zero','2026-09-30',0)]));
 try{await previous(page);assert.doesNotMatch(await page.locator('.credit-total-row').innerText(),/15\.000/);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-H06 — Sorunlu geçmiş görünümden güncele dönmek gerçek veriyi/bakiyeyi değiştirmez',async()=>{
 const {context,page,errors}=await openSynthetic(seed([card],[legacy()]));
 try{await page.locator('.credit-total-row').waitFor();const before=await finances(page),cash=await page.locator('.home-balance-value').innerText(),total=await page.locator('.credit-total-row').innerText();
 await previous(page);await next(page);assert.deepEqual(await finances(page),before);assert.equal(await page.locator('.home-balance-value').innerText(),cash);assert.equal(await page.locator('.credit-total-row').innerText(),total);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-H07 — Borç geçmişi bilinmiyorken üst toplu özette kesin sıfır toplam gösterilmemeli',async()=>{
 const {context,page}=await openSynthetic(seed());
 try{await previous(page);const summary=page.locator('.home-credit-panel > div').nth(1);assert.doesNotMatch(await summary.innerText(),/₺0,00/);}finally{await context.close();}
});
test('UI-H08 — Önceki dönemden gerçekten devreden 5.000 kartta korunur',async()=>{
 const k={...card,ekstreler:[statement('old',5000,'2026-09-10','2026-09-20','2026-09-10')]};
 const {context,page,errors}=await openSynthetic(seed([k],[observation('zero','2026-09-30',0)]));
 try{await previous(page);assert.match(await page.locator('.credit-total-row').innerText(),/5\.000,00/);await next(page);assert.match(await page.locator('.credit-total-row').innerText(),/5\.000,00/);assert.deepEqual(errors,[]);}finally{await context.close();}
});
