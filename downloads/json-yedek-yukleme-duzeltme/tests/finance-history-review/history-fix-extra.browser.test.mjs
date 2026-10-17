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


import {card,current,archivedZero,observation,inconsistentArchive,seed} from './fixtures.mjs';
test('UI-HF01 — Karışık kart geçmişinde yalnız bilinen toplam ve eksik kart sayısı gösterilir',async()=>{
 const other={...card,id:'other',isim:'Test Bonus'};const {context,page,errors}=await openSynthetic(seed([card,other],[current],archivedZero));
 try{await page.getByRole('button',{name:'Önceki dönem',exact:true}).click();const panel=page.locator('.home-credit-panel');assert.match(await panel.innerText(),/1 kartın ekstre bilgisi eksik/);assert.match(await panel.innerText(),/1 kartın gözlem bilgisi eksik/);assert.match(await panel.innerText(),/Geçmiş dönem borç bilgisi bulunmuyor/);assert.doesNotMatch(await panel.locator('.credit-total-row').allTextContents().then(x=>x.join(' ')),/15\.000/);assert.deepEqual(errors,[]);}finally{await context.close();}
});
test('UI-HF02 — Arşiv toplamı korunurken tutarsız ayrıntı açıklanır; arşiv diskte değişmez',async()=>{
 const {context,page,errors}=await openSynthetic(seed([card],[current],inconsistentArchive()));
 try{const get=()=>page.evaluate(()=>JSON.parse(localStorage.getItem('ft_kart_borc_ozetleri'))['2026-8-15']);const before=await get();await page.getByRole('button',{name:'Önceki dönem',exact:true}).click();assert.match(await page.locator('.home-credit-panel').innerText(),/ayrıntısı doğrulanmış toplamla tutarsız/);assert.deepEqual(await get(),before);assert.deepEqual(errors,[]);}finally{await context.close();}
});
