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


const seedB={
 ft_islemler:[{id:'income',tip:'gelir',kategori:'Maaş',tutar:20000,tarih:'2026-10-15'},{id:'bank',tip:'gider',kategori:'World',kartHarcama:true,tutar:10000,donemIci:8000,provizyon:2000,tarih:'2026-10-14',kartGunBaslangicToplam:10000,kartGunBaslangicTarih:'2026-10-14',kartGunlukHareketler:{}}],
 ft_kartlar:[{id:'world',isim:'World',ekstre:0,bakiyedenDussun:true,hesapKesimTarihi:'2026-10-10',sonOdemeTarihi:'2026-10-20'}],
 ft_donem_devir_log:{'2026-8-15':{tutar:0,tarih:'2026-09-15'},'2026-9-15':{tutar:0,tarih:'2026-10-15'}}
};
async function editCard(page,amount,di,pr){
 await page.getByRole('button',{name:'Tutarları güncelle',exact:true}).click();
 const modal=page.getByRole('dialog');
 await modal.getByLabel('Kalan Ekstre Borcu (₺)').fill(String(amount));
 await modal.getByLabel('Dönem İçi (₺)').fill(String(di));await modal.getByLabel('Provizyon (₺)').fill(String(pr));
 await modal.getByRole('button',{name:'Kaydet',exact:true}).click();
}
async function settings(page){await page.getByRole('button',{name:'Menüyü aç'}).click();await page.getByRole('button',{name:'Ayarlar',exact:true}).click();}
async function stored(page,key){return page.evaluate(k=>JSON.parse(localStorage.getItem(k)),key);}
async function makeTwoStatements(page){
 await editCard(page,10000,0,0);
 const first=(await stored(page,'ft_kartlar'))[0].ekstreler[0];
 await page.clock.setSystemTime(new Date('2026-11-10T12:00:00+03:00'));await page.reload();await page.locator('.home-balance-value').waitFor();
 await settings(page);await page.getByLabel('World hesap kesim tarihi',{exact:true}).fill('2026-11-10');
 await page.getByLabel('World son ödeme tarihi',{exact:true}).fill('2026-11-20');
 await page.getByLabel('World hesap kesim tarihi',{exact:true}).locator('..').locator('..').locator('..').getByRole('button',{name:'Kaydet',exact:true}).click();
 await page.getByRole('button',{name:'Özet',exact:true}).click();await editCard(page,5000,0,0);
 return first;
}
test('UI-B01 — Dönem geçişi borcu korur; yeni 500 TL güncelleme kapalı dönemi değiştirmez',async()=>{
 const {context,page,errors}=await openSynthetic(seedB,'2026-10-14T12:00:00+03:00');
 try{
  await page.waitForFunction(()=>JSON.parse(localStorage.getItem('ft_kart_borc_ozetleri')||'{}')['2026-8-15']?.world?.toplam===10000);
  const old=JSON.stringify((await stored(page,'ft_kart_borc_ozetleri'))['2026-8-15']);
  await page.clock.setSystemTime(new Date('2026-10-16T12:00:00+03:00'));await page.reload();await page.locator('.credit-total-row').waitFor();
  assert.match(await page.locator('.credit-total-row > .money-fit').innerText(),/10\.000,00/);
  assert.match(await page.locator('.home-balance .day-totals strong').first().innerText(),/10\.000,00/);
  await editCard(page,0,8500,2000);
  assert.match(await page.locator('.credit-total-row > .money-fit').innerText(),/10\.500,00/);
  assert.equal(JSON.stringify((await stored(page,'ft_kart_borc_ozetleri'))['2026-8-15']),old);
  assert.equal((await stored(page,'ft_islemler')).find(i=>i.id==='bank').kartGunlukHareketler['2026-10-16'],500);
  await page.getByRole('button',{name:'Önceki dönem',exact:true}).click();
  assert.match(await page.locator('.credit-total-row > .money-fit').innerText(),/10\.000,00/);
  await page.getByRole('button',{name:'Sonraki dönem',exact:true}).click();
  await page.getByRole('button',{name:'Raporlar',exact:true}).click();
  assert.match(await page.locator('.report-stat').nth(1).innerText(),/500,00/);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-B02 — Kesilmiş ekstre değiştirilemez; aynı kartın yeni ekstresi eski borcu ve kimliği korur',async()=>{
 const {context,page,errors}=await openSynthetic(seedB);
 try{
  await editCard(page,10000,0,0);const first=(await stored(page,'ft_kartlar'))[0].ekstreler[0];
  const rows=await stored(page,'ft_islemler');await editCard(page,11000,0,0);
  assert.match(await page.getByRole('dialog').getByRole('alert').innerText(),/Kesilmiş ekstre tutarı değiştirilemez/);
  assert.deepEqual((await stored(page,'ft_kartlar'))[0].ekstreler[0],first);assert.deepEqual(await stored(page,'ft_islemler'),rows);
  await page.keyboard.press('Escape');
  await page.clock.setSystemTime(new Date('2026-11-10T12:00:00+03:00'));await page.reload();await page.locator('.home-balance-value').waitFor();
  await settings(page);await page.getByLabel('World hesap kesim tarihi',{exact:true}).fill('2026-11-10');await page.getByLabel('World son ödeme tarihi',{exact:true}).fill('2026-11-20');
  await page.getByLabel('World hesap kesim tarihi',{exact:true}).locator('..').locator('..').locator('..').getByRole('button',{name:'Kaydet',exact:true}).click();
  await page.getByRole('button',{name:'Özet',exact:true}).click();await editCard(page,5000,0,0);
  const cards=await stored(page,'ft_kartlar');assert.equal(cards[0].ekstreler.length,2);assert.deepEqual(cards[0].ekstreler[0],first);
  assert.match(await page.locator('.credit-total-row > .money-fit').innerText(),/15\.000,00/);
  await settings(page);const promise=page.waitForEvent('download');await page.getByRole('button',{name:'Yedek Al',exact:true}).click();
  const stream=await(await promise).createReadStream(),chunks=[];for await(const c of stream)chunks.push(c);
  const backup=JSON.parse(Buffer.concat(chunks).toString());assert.deepEqual(backup.data.ft_kartlar,cards);
  await page.locator('input[type="file"]').setInputFiles({name:'b-test.json',mimeType:'application/json',buffer:Buffer.from(JSON.stringify(backup))});
  const preview=page.getByRole('dialog',{name:'Yedek yükleme önizlemesi'});await preview.waitFor();
  const reload=page.waitForEvent('load');await preview.getByRole('button',{name:'Yedeği yükle ve değiştir'}).click();await reload;await page.locator('.home-balance-value').waitFor();
  assert.deepEqual(await stored(page,'ft_kartlar'),cards);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-B03 — İki açık ekstre tek onayla ayrı kimlikli ödenir; yeniden alışveriş gideri oluşmaz',async()=>{
 const {context,page,errors}=await openSynthetic(seedB);
 try{
  const first=await makeTwoStatements(page);
  await page.getByRole('button',{name:'Ödendi olarak işaretle',exact:true}).click();
  await page.getByRole('dialog').getByRole('button',{name:'Ödendi Olarak İşaretle',exact:true}).click();
    await page.getByRole('dialog',{name:'Ödeme Onayı',exact:true}).getByRole('button',{name:'Ödemeyi Onayla',exact:true}).click();
  const payments=await stored(page,'ft_kart_odemeleri');assert.equal(payments.length,2);assert.equal(new Set(payments.map(p=>p.ekstreId)).size,2);
  assert.equal(payments.reduce((n,p)=>n+p.tutar,0),15000);assert.equal(payments.every(p=>p.tarih==='2026-11-10'),true);
  assert.deepEqual((await stored(page,'ft_kartlar'))[0].ekstreler[0],first);
  assert.match(await page.locator('.credit-total-row > .money-fit').innerText(),/^₺0,00$/);
  await page.getByRole('button',{name:'Raporlar',exact:true}).click();assert.match(await page.locator('.report-stat').nth(1).innerText(),/0,00/);
  await page.getByLabel('Hesaplama görünümü').selectOption('nakit');assert.match(await page.locator('.report-stat').nth(1).innerText(),/15\.000,00/);
  assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-B04 — Geçmiş veri yoksa bugünkü tutar geçmiş kartta gösterilmez',async()=>{
 const data=structuredClone(seedB);data.ft_islemler[1].tarih='2026-10-16';data.ft_islemler[1].kartGunBaslangicTarih='2026-10-16';
 const {context,page,errors}=await openSynthetic(data);
 try{await page.getByRole('button',{name:'Önceki dönem',exact:true}).click();
  assert.match(await page.locator('.credit-total-row').innerText(),/Geçmiş veri yok/);
  assert.equal(await page.locator('.credit-total-row > .money-fit').count(),0);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-B05 — Yeni kart UUID ile eklenir; mevcut kart kimliği ve kişisel kategori korunur',async()=>{
 const {context,page,errors}=await openSynthetic(seedB);
 try{await settings(page);await page.getByPlaceholder('Kart adı (World, Yapı Kredi World, Bonus...)').fill('Yapı Kredi World');
  await page.getByLabel('Yeni kart hesap kesim tarihi',{exact:true}).fill('2026-10-10');await page.getByLabel('Yeni kart son ödeme tarihi',{exact:true}).fill('2026-10-20');
  await page.getByRole('button',{name:'+ Kart Ekle',exact:true}).click();const cards=await stored(page,'ft_kartlar');
  assert.equal(cards.length,2);assert.equal(cards[0].id,'world');assert.match(cards[1].id,/^[0-9a-f]{8}-[0-9a-f-]+$/i);
  assert.ok((await stored(page,'ft_kategoriler')).gider.includes('Yapı Kredi World'));assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
