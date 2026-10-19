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

test('UI-S02 — Kredi ekranından ödendi işaretlenince bakiye 20.000 TL → 15.000 TL olmalı', async () => {
  const { context, page, errors } = await openSynthetic({
    ft_islemler: [{ id: 'income', tip: 'gelir', kategori: 'Maaş', tutar: 20000, tarih: '2026-10-15' }],
    ft_krediler: [{ id: 'loan', ad: 'Test kredisi', toplamTutar: 5000, vade: 1, baslangic: '2026-10-20', taksitler: [{ no: 1, tarih: '2026-10-20', tutar: 5000, odendi: false }] }],
  });
  try {
    await page.getByRole('button', { name: 'Menüyü aç' }).click();
    await page.getByRole('button', { name: 'Krediler', exact: true }).click();
    // The existing loan row exposes its tracking toggle when the installment list opens.
    await page.getByRole('button', { name: /Taksitleri/ }).click();
    // Existing installment toggles are the 20px square controls in this screen.
    await page.locator('main button[style*="width: 20px"]').first().click();
    await page.getByRole('dialog',{name:'Ödeme Onayı',exact:true}).getByRole('button',{name:'Ödemeyi Onayla',exact:true}).click();
    assert.equal(await page.evaluate(() => JSON.parse(localStorage.getItem('ft_krediler'))[0].taksitler[0].odendi), true);
    await page.getByRole('button', { name: 'Özet', exact: true }).click();
    assert.deepEqual(errors, []);
    assert.match(await page.locator('.home-balance-value').innerText(), /15\.000,00/);
  } finally { await context.close(); }
});

test('UI-S04 — Uygulama yeni maaş döneminde açıldığında kartın ödenmemiş dönem içi borcu kaybolmamalı', async () => {
  const { context, page, errors } = await openSynthetic({
    ft_kartlar: [{ id: 'card', isim: 'World', ekstre: 0, bakiyedenDussun: true }],
    ft_islemler: [{ id: 'snapshot', tip: 'gider', kategori: 'World', kartHarcama: true, tutar: 10000, donemIci: 8000, provizyon: 2000, tarih: '2026-10-14', kartGunBaslangicToplam: 10000, kartGunBaslangicTarih: '2026-10-14', kartGunlukHareketler: {} }],
  }, '2026-10-14T12:00:00+03:00');
  try {
    assert.match(await page.locator('.credit-total-row > .money-fit').innerText(), /10\.000,00/);
    await page.waitForFunction(() => JSON.parse(localStorage.getItem('ft_kart_borc_ozetleri') || '{}')['2026-8-15']?.card?.toplam === 10000);
    await page.clock.setSystemTime(new Date('2026-10-15T12:00:00+03:00'));
    await page.reload();
    await page.locator('.credit-total-row').waitFor();
    assert.deepEqual(errors, []);
    assert.match(await page.locator('.credit-total-row > .money-fit').innerText(), /10\.000,00/);
  } finally { await context.close(); }
});

test('UI-KONTROL — Normal bekleyen ödeme ödendiğinde ve geri alındığında nakit ve ayrılan tutar uyumlu olmalı', async () => {
  const { context, page, errors } = await openSynthetic({
    ft_islemler: [{ id: 'income', tip: 'gelir', kategori: 'Maaş', tutar: 20000, tarih: '2026-10-15' }, { id: 'bill-entry', tip: 'gider', kategori: 'Elektrik', tutar: 5000, tarih: '2026-10-20', kaynak: 'odeme', odemeId: 'bill' }],
    ft_odemeler: [{ id: 'bill', islemId: 'bill-entry', kategori: 'Elektrik', aciklama: 'Test faturası', tutar: 5000, tarih: '2026-10-20', odendi: false }],
  });
  try {
    assert.match(await page.locator('.home-balance-value').innerText(), /20\.000,00/);
    assert.match(await page.locator('.home-balance .day-totals').innerText(), /15\.000,00/);
    await page.getByRole('button', { name: 'Ödemeler', exact: true }).click();
    await page.locator('.payment-group > button').click();
    await page.getByRole('button', { name: 'Ödendi olarak işaretle: Test faturası', exact: true }).click();
    await page.getByRole('dialog',{name:'Ödeme Onayı',exact:true}).getByRole('button',{name:'Ödemeyi Onayla',exact:true}).click();
    await page.getByRole('button', { name: 'Özet', exact: true }).click();
    assert.match(await page.locator('.home-balance-value').innerText(), /15\.000,00/);
    assert.match(await page.locator('.home-balance .day-totals').innerText(), /15\.000,00/);
    await page.getByRole('button', { name: 'Ödemeler', exact: true }).click();
    await page.getByRole('button', { name: 'Ödenmiş', exact: true }).click();
    await page.locator('.payment-group > button').click();
    await page.getByRole('button', { name: 'Ödemeyi geri al: Test faturası', exact: true }).click();
    await page.getByRole('button', { name: 'Özet', exact: true }).click();
    assert.match(await page.locator('.home-balance-value').innerText(), /20\.000,00/);
    assert.match(await page.locator('.home-balance .day-totals').innerText(), /15\.000,00/);
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});
