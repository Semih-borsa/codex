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


const seedA = {
  ft_islemler: [{id:'income',tip:'gelir',kategori:'Maaş',tutar:20000,tarih:'2026-10-15'}],
  ft_krediler: [{id:'loan',ad:'Test kredisi',toplamTutar:5000,vade:1,baslangic:'2026-10-20',taksitler:[{no:1,tarih:'2026-10-20',tutar:5000,odendi:false}]}],
  ft_donem_devir_log: {'2026-9-15':{tutar:0,tarih:'2026-10-15'}},
};
async function toggleLoan(page) {
  await page.getByRole('button',{name:'Menüyü aç'}).click();
  await page.getByRole('button',{name:'Krediler',exact:true}).click();
  await page.getByRole('button',{name:/Taksitleri/}).click();
  await page.locator('main button[style*="width: 20px"]').first().click();
  await page.getByRole('button',{name:'Özet',exact:true}).click();
}
async function balance(page,cash,usable,pending) {
  assert.match(await page.locator('.home-balance-value').innerText(),cash);
  const fields=page.locator('.home-balance .day-totals strong');
  assert.match(await fields.nth(0).innerText(),usable);
  assert.match(await fields.nth(1).innerText(),pending);
}
test('UI-A01 — 20.000/5.000/15.000 ödeme ve geri alma, rapor, kalıcılık ve devir koruması',async()=>{
  const {context,page,errors}=await openSynthetic(seedA);
  try {
    await balance(page,/20\.000,00/,/15\.000,00/,/5\.000,00/);
    const carry=await page.evaluate(()=>localStorage.getItem('ft_donem_devir_log'));
    await toggleLoan(page);
    await balance(page,/15\.000,00/,/15\.000,00/,/^₺0,00$/);
    const rows=await page.evaluate(()=>JSON.parse(localStorage.getItem('ft_islemler')));
    const payment=rows.filter(i=>i.kaynak==='kredi_taksiti');
    assert.equal(payment.length,1);assert.equal(payment[0].tarih,'2026-10-16');assert.equal(payment[0].tutar,5000);
    assert.equal(await page.evaluate(()=>JSON.parse(localStorage.getItem('ft_krediler'))[0].taksitler[0].tarih),'2026-10-20');
    await page.reload();await page.locator('.home-balance-value').waitFor();
    await balance(page,/15\.000,00/,/15\.000,00/,/^₺0,00$/);
    await page.getByRole('button',{name:'Raporlar',exact:true}).click();
    assert.match(await page.locator('.report-stat').nth(1).innerText(),/0,00/);
    await page.getByLabel('Hesaplama görünümü').selectOption('nakit');
    assert.match(await page.locator('.report-stat').nth(1).innerText(),/5\.000,00/);
    await toggleLoan(page);
    await balance(page,/20\.000,00/,/15\.000,00/,/5\.000,00/);
    assert.equal(await page.evaluate(()=>JSON.parse(localStorage.getItem('ft_islemler')).filter(i=>i.kaynak==='kredi_taksiti').length),0);
    assert.equal(await page.evaluate(()=>localStorage.getItem('ft_donem_devir_log')),carry);
    await page.reload();await page.locator('.home-balance-value').waitFor();
    await balance(page,/20\.000,00/,/15\.000,00/,/5\.000,00/);assert.deepEqual(errors,[]);
  } finally {await context.close();}
});
test('UI-A02 — Eski ödendi kaydı açılışta gider üretmez ve manuel ödeme eşleştirilmez',async()=>{
  const data=structuredClone(seedA);data.ft_krediler[0].taksitler[0].odendi=true;
  data.ft_islemler.push({id:'manual',tip:'gider',kategori:'Kredi',tutar:5000,tarih:'2026-10-16'});
  const {context,page,errors}=await openSynthetic(data);
  try {
    await balance(page,/15\.000,00/,/15\.000,00/,/^₺0,00$/);
    assert.equal(await page.evaluate(()=>JSON.parse(localStorage.getItem('ft_islemler')).filter(i=>i.kaynak==='kredi_taksiti').length),0);
    await toggleLoan(page);
    await balance(page,/15\.000,00/,/10\.000,00/,/5\.000,00/);
    assert.equal(await page.evaluate(()=>JSON.parse(localStorage.getItem('ft_islemler')).find(i=>i.id==='manual').tutar),5000);
    assert.deepEqual(errors,[]);
  } finally {await context.close();}
});
