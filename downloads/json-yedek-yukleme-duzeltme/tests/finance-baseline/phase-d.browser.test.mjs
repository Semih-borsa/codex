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


const seedD={ft_islemler:[{id:'income',tip:'gelir',kategori:'Maaş',tutar:20000,tarih:'2026-10-15'}],ft_kartlar:[{id:'card',isim:'World',bakiyedenDussun:true}],ft_donem_devir_log:{'2026-9-15':{tutar:0,tarih:'2026-10-15'}}};
async function plans(page){await page.getByRole('button',{name:'Menüyü aç'}).click();await page.getByRole('button',{name:'Taksitli Harcamalar',exact:true}).click();}
async function stored(page,k){return page.evaluate(k=>JSON.parse(localStorage.getItem(k)),k);}
async function createPlan(page,fields={}){
 await plans(page);await page.getByRole('button',{name:'+ Ekle',exact:true}).click();const modal=page.getByRole('dialog',{name:'Taksitli Harcama Ekle'});
 await modal.getByLabel('Ürün / Harcama Adı',{exact:true}).fill(fields.ad||'Telefon');await modal.getByLabel('Kredi kartı',{exact:true}).selectOption('card');
 await modal.getByLabel('Toplam alışveriş tutarı (₺)',{exact:true}).fill(fields.tutar||'100');
 await modal.getByLabel('Alışveriş tarihi',{exact:true}).fill('2026-10-09');await modal.getByLabel('Bankanın doğruladığı ilk ekstre tarihi',{exact:true}).fill(fields.ilk||'2026-10-10');
 await modal.getByLabel('Tercih edilen hesap kesim günü',{exact:true}).fill('10');await modal.getByLabel('Tercih edilen son ödeme günü',{exact:true}).fill('20');
 await modal.getByLabel('Toplam Taksit Sayısı (vade)',{exact:true}).fill(fields.vade||'3');await modal.getByRole('button',{name:'Kaydet',exact:true}).click();
 await modal.waitFor({state:'hidden'});
}
async function detail(page){await page.getByRole('button',{name:'Düzenle',exact:true}).first().click();return page.getByRole('dialog',{name:/Ekstre planı/});}
async function bindFirst(page){const modal=await detail(page),row=modal.getByRole('region',{name:'1. taksit ayrıntısı'});await row.getByLabel('1. taksit ekstre seçimi',{exact:true}).selectOption('bank');await row.getByRole('button',{name:'Ekstreyi bağla',exact:true}).click();return {modal,row};}
const bank={id:'card',isim:'World',bakiyedenDussun:true,ekstre:100,hesapKesimTarihi:'2026-10-10',sonOdemeTarihi:'2026-10-20',ekstreler:[{id:'bank',kartId:'card',tutar:100,hesapKesimTarihi:'2026-10-10',sonOdemeTarihi:'2026-10-20',kayitTarihi:'2026-10-10',dogrulandi:true}],aktifEkstreId:'bank'};

test('UI-D01 — Tarihli plan kayıt ve yeniden açılışta tarih/kuruşları korur; yalnız rezerv oluşturur',async()=>{
 const {context,page,errors}=await openSynthetic(seedD);
 try{
  await createPlan(page);const p=(await stored(page,'ft_taksitler'))[0];assert.deepEqual(p.taksitler.map(t=>t.tutar),[33.33,33.33,33.34]);
  assert.deepEqual(p.taksitler.map(t=>t.kesimTarihi),['2026-10-10','2026-11-10','2026-12-10']);
  await page.getByRole('button',{name:'Özet',exact:true}).click();assert.match(await page.locator('.home-balance-value').innerText(),/20\.000,00/);
  assert.match(await page.locator('.home-balance .day-totals strong').first().innerText(),/19\.966,67/);
  await page.reload();await page.locator('.home-balance-value').waitFor();assert.deepEqual((await stored(page,'ft_taksitler'))[0],p);
  assert.equal((await stored(page,'ft_islemler')).length,1);await page.getByRole('button',{name:'Raporlar',exact:true}).click();assert.match(await page.locator('.report-stat').nth(1).innerText(),/0,00/);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-D02 — İlk Kasım seçilince Ekim yükümlülüğü doğmaz; yıl değişimi doğru',async()=>{
 const {context,page,errors}=await openSynthetic(seedD);
 try{await createPlan(page,{ilk:'2026-11-10'});const p=(await stored(page,'ft_taksitler'))[0];assert.deepEqual(p.taksitler.map(t=>t.kesimTarihi),['2026-11-10','2026-12-10','2027-01-10']);
  await page.getByRole('button',{name:'Özet',exact:true}).click();assert.match(await page.locator('.home-balance .day-totals strong').first().innerText(),/20\.000,00/);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-D03 — Bankaya bağlama, kısmi pay ve ödeme geri alma nakdi mükerrer azaltmaz',async()=>{
 const data=structuredClone(seedD);data.ft_kartlar=[bank];data.ft_kart_odemeleri=[{id:'pay',kartId:'card',ekstreId:'bank',kartAdi:'World',tutar:20,tarih:'2026-10-16'}];
 const {context,page,errors}=await openSynthetic(data);
 try{await createPlan(page);const {modal,row}=await bindFirst(page);await row.getByText('Kısmi ödeme payı',{exact:true}).click();
  await row.getByLabel('1. taksit ödeme seçimi',{exact:true}).selectOption('pay');await row.getByLabel('1. taksit ödeme payı (₺)',{exact:true}).fill('20');await row.getByRole('button',{name:'Ödeme payını kaydet',exact:true}).click();
  assert.match(await row.innerText(),/Kalan: ₺13,33/);await page.keyboard.press('Escape');await page.getByRole('button',{name:'Özet',exact:true}).click();
  assert.match(await page.locator('.home-balance-value').innerText(),/19\.980,00/);assert.match(await page.locator('.home-balance .day-totals strong').first().innerText(),/19\.900,00/);
  // Real transaction-screen deletion is an explicit payment undo.
  await page.getByRole('button',{name:'İşlemler',exact:true}).click();
  const group=page.locator('.transaction-group > button');if(await group.count())await group.first().click();
  // Existing transaction delete controls have a stable accessible label.
  const deleteButton=page.locator('.transaction-row').filter({hasText:'ekstre ödemesi'}).getByRole('button',{name:'Sil',exact:true});await deleteButton.click();
  assert.equal((await stored(page,'ft_kart_odemeleri')).length,0);
  await plans(page);const again=await detail(page);assert.match(await again.getByRole('region',{name:'1. taksit ayrıntısı'}).innerText(),/Kalan: ₺33,33/);
  assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-D04 — Banka onaylı iade ve geri alma özgün ekstreyi değiştirmez',async()=>{
 const data=structuredClone(seedD);data.ft_kartlar=[bank];const {context,page,errors}=await openSynthetic(data);
 try{await createPlan(page);const {modal,row}=await bindFirst(page);const original=JSON.stringify((await stored(page,'ft_kartlar'))[0].ekstreler);
  await row.getByText('İade',{exact:true}).click();await row.getByLabel('1. taksit iade tutarı (₺)',{exact:true}).fill('10');await row.getByRole('checkbox',{name:'İade bankanın bu ekstresine yansıdı'}).check();
  await row.getByRole('button',{name:'İadeyi kaydet',exact:true}).click();assert.match(await row.innerText(),/Kalan: ₺23,33/);
  assert.equal(JSON.stringify((await stored(page,'ft_kartlar'))[0].ekstreler),original);
  await page.keyboard.press('Escape');await page.getByRole('button',{name:'Özet',exact:true}).click();assert.match(await page.locator('.home-balance .day-totals strong').first().innerText(),/19\.910,00/);
  await plans(page);const m=await detail(page);await m.getByRole('button',{name:'İadeyi geri al',exact:true}).click();await page.keyboard.press('Escape');await page.getByRole('button',{name:'Özet',exact:true}).click();assert.match(await page.locator('.home-balance .day-totals strong').first().innerText(),/19\.900,00/);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-D05 — Plan iptali geçmiş satırı korur, gelecek satırları kapatır',async()=>{
 const {context,page,errors}=await openSynthetic(seedD);
 try{await createPlan(page);const m=await detail(page);await m.getByRole('button',{name:'Planı iptal et',exact:true}).click();const p=(await stored(page,'ft_taksitler'))[0];
  assert.equal(p.durum,'iptal');assert.equal(p.taksitler[0].durum,'planlandi');assert.equal(p.taksitler[1].durum,'iptal');assert.equal(p.taksitler.length,3);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-D06 — Eski kayıt açılışta dönüştürülmez; yeni plan yedeği dosyadan geri yüklenir',async()=>{
 const data=structuredClone(seedD);data.ft_taksitler=[{id:'old',ad:'Eski',aylikTutar:100,vade:3,odenen:1,kart:'Bonus'}];
 const {context,page,errors}=await openSynthetic(data);
 try{await createPlan(page);assert.deepEqual((await stored(page,'ft_taksitler'))[0],data.ft_taksitler[0]);const expected=await stored(page,'ft_taksitler');
  await page.getByRole('button',{name:'Menüyü aç'}).click();await page.getByRole('button',{name:'Ayarlar',exact:true}).click();const promise=page.waitForEvent('download');await page.getByRole('button',{name:'Yedek Al',exact:true}).click();
  const chunks=[],stream=await(await promise).createReadStream();for await(const c of stream)chunks.push(c);const text=Buffer.concat(chunks).toString();assert.equal(JSON.parse(text).data.ft_taksitler.length,2);
  await page.locator('input[type="file"]').setInputFiles({name:'d-plan.json',mimeType:'application/json',buffer:Buffer.from(text)});const preview=page.getByRole('dialog',{name:'Yedek yükleme önizlemesi'});await preview.waitFor();
  const reload=page.waitForEvent('load');await preview.getByRole('button',{name:'Yedeği yükle ve değiştir'}).click();await reload;await page.locator('.home-balance-value').waitFor();assert.deepEqual(await stored(page,'ft_taksitler'),expected);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-D07 — İlk tarih eksikse tahmin yapılmaz ve hiçbir plan kaydedilmez',async()=>{
 const {context,page,errors}=await openSynthetic(seedD);
 try{await plans(page);await page.getByRole('button',{name:'+ Ekle',exact:true}).click();const m=page.getByRole('dialog');await m.getByLabel('Ürün / Harcama Adı',{exact:true}).fill('Eksik');await m.getByRole('button',{name:'Kaydet',exact:true}).click();
  assert.match(await m.getByRole('alert').innerText(),/doğrulanmış ilk ekstre/);assert.equal(await stored(page,'ft_taksitler'),null);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});

test('UI-D08 — Banka iadesi ikinci anahtar yazma hatasında plan ve kart birlikte geri alınır',async()=>{
 const data=structuredClone(seedD);data.ft_kartlar=[bank];const {context,page,errors}=await openSynthetic(data);
 try{await createPlan(page);const {modal,row}=await bindFirst(page);const beforePlans=await stored(page,'ft_taksitler'),beforeCards=await stored(page,'ft_kartlar');
  await row.getByText('İade',{exact:true}).click();await row.getByLabel('1. taksit iade tutarı (₺)',{exact:true}).fill('10');await row.getByRole('checkbox',{name:'İade bankanın bu ekstresine yansıdı'}).check();
  await page.evaluate(()=>{const original=Storage.prototype.setItem;let fail=true;Storage.prototype.setItem=function(k,v){if(k==='ft_kartlar'&&fail){fail=false;throw new DOMException('Sentetik hata','QuotaExceededError');}return original.call(this,k,v);};});
  await row.getByRole('button',{name:'İadeyi kaydet',exact:true}).click();assert.match(await modal.getByRole('alert').innerText(),/kaydedilemedi/);
  assert.deepEqual(await stored(page,'ft_taksitler'),beforePlans);assert.deepEqual(await stored(page,'ft_kartlar'),beforeCards);
  assert.ok(await stored(page,'ft_auto_backup_taksit_islem_oncesi_v1'));assert.deepEqual(errors,[]);
 }finally{await context.close();}
});

test('UI-D09 — Özet hızlı taksit kaydı eski takip akışını korur ve tarihli plana yönlendirir',async()=>{
 const {context,page,errors}=await openSynthetic(seedD);
 try{await page.getByRole('button',{name:'Gider ekle',exact:true}).click();const m=page.getByRole('dialog',{name:'Yeni İşlem'});await m.getByRole('button',{name:'Taksit',exact:true}).click();
  assert.match(await m.innerText(),/Tarihli ekstre planı/);await m.getByLabel('Ürün / Harcama Adı',{exact:true}).fill('Eski hızlı takip');await m.getByLabel('Aylık Taksit Tutarı (₺)',{exact:true}).fill('50');await m.getByLabel('Toplam Taksit Sayısı (vade)',{exact:true}).fill('3');await m.getByLabel('Şimdiye Kadar Ödenen Taksit',{exact:true}).fill('1');await m.getByRole('button',{name:'Kaydet',exact:true}).click();await m.waitFor({state:'hidden'});
  const p=(await stored(page,'ft_taksitler'))[0];assert.equal(p.aylikTutar,50);assert.equal(p.odenen,1);assert.equal(p.planSurumu,undefined);assert.equal(p.ilkEkstreTarihi,undefined);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
