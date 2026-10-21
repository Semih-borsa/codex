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

test('UI-C01 — Eksik işlem listeli paket önizlenir ama tam geri yükleme düğmesi kapalıdır',async()=>{
  const {context,page,errors}=await openSynthetic(seedC);
  try {
    await settings(page);const before=await userData(page);
    await choose(page,packet({ft_kartlar:[]}));await dialog(page).waitFor();
    assert.equal(await dialog(page).getByRole('button',{name:'Yedeği yükle ve değiştir'}).isDisabled(),true);
    assert.match(await dialog(page).innerText(),/Kısmi veri paketi/);
    assert.match(await dialog(page).getByRole('region',{name:'İşlemler yükleme etkisi'}).innerText(),/Mevcut bölüm korunacak/);
    await dialog(page).getByRole('button',{name:'Vazgeç',exact:true}).click();
    assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);
  }finally{await context.close();}
});
test('UI-C02 — Ekleme/silme/değiştirme etkileri görünür; Vazgeç ve Escape kayıtları korur',async()=>{
  const data=structuredClone(seedC);data.ft_islemler=[income('delete'),income('change'),income('same')];
  const {context,page,errors}=await openSynthetic(data);
  try {
    await settings(page);const before=await userData(page),p=packet({ft_islemler:[income('add'),income('change',10000),income('same')]});
    await choose(page,p);await dialog(page).waitFor();
    const section=dialog(page).getByRole('region',{name:'İşlemler yükleme etkisi'});
    assert.match(await section.innerText(),/Eklenecek: 1 · Silinecek: 1 · Değişecek: 1/);
    await section.getByText('Değişecek (1)',{exact:true}).click();assert.match(await section.innerText(),/20\.000,00.*10\.000,00/s);
    assert.match(await dialog(page).getByRole('region',{name:'Kişisel kategoriler yükleme etkisi'}).innerText(),/korunacak/);
    await dialog(page).getByRole('button',{name:'Vazgeç',exact:true}).click();
    assert.deepEqual(await userData(page),before);
    await choose(page,p);await dialog(page).waitFor();await page.keyboard.press('Escape');
    assert.equal(await dialog(page).count(),0);assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);
  }finally{await context.close();}
});
test('UI-C03 — Gerçek Yedek Al → dosyadan geri yükle → kredi ödemesini geri al akışı',async()=>{
  const data=structuredClone(seedC);
  data.ft_islemler.push({id:'paid',tip:'gider',kategori:'Kredi',kaynak:'kredi_taksiti',krediId:'loan',taksitNo:1,tutar:5000,tarih:'2026-10-16'});
  data.ft_krediler=[{id:'loan',ad:'Test kredisi',toplamTutar:5000,vade:1,taksitler:[{no:1,tarih:'2026-10-20',tutar:5000,odendi:true,odemeIslemId:'paid'}]}];
  const {context,page,errors}=await openSynthetic(data);
  try {
    await settings(page);
    const downloadPromise=page.waitForEvent('download');await page.getByRole('button',{name:'Yedek Al',exact:true}).click();
    const stream=await (await downloadPromise).createReadStream(),chunks=[];for await(const chunk of stream)chunks.push(chunk);
    const exported=JSON.parse(Buffer.concat(chunks).toString());
    assert.equal(exported.yedekTuru,'tam');assert.deepEqual([...exported.anahtarlar].sort(),Object.keys(exported.data).sort());
    assert.equal(exported.data.ft_islemler.find(i=>i.id==='paid').krediId,'loan');
    // A different synthetic current state, never a real WebView/user store.
    await page.evaluate(()=>{localStorage.setItem('ft_islemler',JSON.stringify([{id:'different',tip:'gelir',kategori:'Maaş',tutar:30000,tarih:'2026-10-15'}]));localStorage.setItem('ft_krediler','[]');});
    await page.reload();await page.locator('.home-balance-value').waitFor();await settings(page);
    await choose(page,exported);await dialog(page).waitFor();
    assert.equal(await dialog(page).getByRole('button',{name:'Yedeği yükle ve değiştir'}).isEnabled(),true);
    const reloaded=page.waitForEvent('load');await dialog(page).getByRole('button',{name:'Yedeği yükle ve değiştir'}).click();await reloaded;
    await page.locator('.home-balance-value').waitFor();assert.match(await page.locator('.home-balance-value').innerText(),/15\.000,00/);
    const restored=await page.evaluate(()=>JSON.parse(localStorage.getItem('ft_krediler')));
    assert.equal(restored[0].taksitler[0].odemeIslemId,'paid');
    const recovery=await page.evaluate(()=>JSON.parse(localStorage.getItem('ft_auto_backup_restore_before_v2')));
    assert.equal(recovery.data.ft_islemler[0].id,'different');
    assert.deepEqual(await page.evaluate(()=>JSON.parse(localStorage.getItem('ft_kategoriler'))),data.ft_kategoriler);
    await page.getByRole('button',{name:'Menüyü aç'}).click();await page.getByRole('button',{name:'Krediler',exact:true}).click();
    await page.getByRole('button',{name:/Taksitleri/}).click();await page.locator('main button[style*="width: 20px"]').first().click();
    await page.getByRole('button',{name:'Özet',exact:true}).click();assert.match(await page.locator('.home-balance-value').innerText(),/20\.000,00/);
    assert.match(await page.locator('.home-balance .day-totals strong').nth(0).innerText(),/15\.000,00/);assert.deepEqual(errors,[]);
  }finally{await context.close();}
});
test('UI-C04 — Hatalı JSON dosyası mevcut veriyi değiştirmez',async()=>{
  const {context,page,errors}=await openSynthetic(seedC);
  try{await settings(page);const before=await userData(page);await choose(page,'{bozuk-json');
    await page.getByText('Dosya geçerli JSON değil; mevcut kayıtlar değiştirilmedi.',{exact:true}).waitFor();
    assert.equal(await dialog(page).count(),0);assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);
  }finally{await context.close();}
});
test('UI-C05 — Güvenlik kopyası kota hatası geri yüklemeyi başlatmaz',async()=>{
  const {context,page,errors}=await openSynthetic(seedC);
  try{await settings(page);const before=await userData(page);await choose(page,packet({ft_islemler:[]}));await dialog(page).waitFor();
    await failure(page,'ft_auto_backup_restore_before_v2');await dialog(page).getByRole('button',{name:'Yedeği yükle ve değiştir'}).click();
    assert.deepEqual(await userData(page),before);assert.equal(await dialog(page).count(),1);assert.match(await dialog(page).getByRole('alert').innerText(),/güvenlik kopyası oluşturulamadı/);assert.deepEqual(errors,[]);
  }finally{await context.close();}
});
test('UI-C06 — İkinci alan yazma hatasında kayıtlar geri alınır, kurtarma kopyası saklanır',async()=>{
  const {context,page,errors}=await openSynthetic(seedC);
  try{await settings(page);const before=await userData(page);await choose(page,packet({ft_islemler:[],ft_kartlar:[]}));await dialog(page).waitFor();
    await failure(page,'ft_kartlar');await dialog(page).getByRole('button',{name:'Yedeği yükle ve değiştir'}).click();
    assert.deepEqual(await userData(page),before);
    assert.deepEqual(await page.evaluate(()=>JSON.parse(localStorage.getItem('ft_auto_backup_restore_before_v2')).data.ft_islemler),seedC.ft_islemler);
    assert.equal(await dialog(page).count(),1);assert.match(await dialog(page).getByRole('alert').innerText(),/önceki kayıtlar geri yüklendi/);assert.deepEqual(errors,[]);
  }finally{await context.close();}
});
test('UI-C07 — Önizleme açıkken kayıt değişirse yükleme reddedilir',async()=>{
  const {context,page,errors}=await openSynthetic(seedC);
  try{await settings(page);await choose(page,packet({ft_islemler:[]}));await dialog(page).waitFor();
    await page.evaluate(()=>localStorage.setItem('ft_islemler',JSON.stringify([{id:'new-current',tip:'gelir',kategori:'Maaş',tutar:25000,tarih:'2026-10-15'}])));
    const before=await userData(page);await dialog(page).getByRole('button',{name:'Yedeği yükle ve değiştir'}).click();
    assert.deepEqual(await userData(page),before);assert.equal(await dialog(page).count(),1);assert.deepEqual(errors,[]);
  }finally{await context.close();}
});
test('UI-C08 — Açıkça boş liste, silme etkisi gösterilip onaylandıktan sonra uygulanır',async()=>{
  const {context,page,errors}=await openSynthetic(seedC);
  try{await settings(page);await choose(page,packet({ft_islemler:[]}));await dialog(page).waitFor();
    assert.match(await dialog(page).getByRole('region',{name:'İşlemler yükleme etkisi'}).innerText(),/Silinecek: 1/);
    const reloaded=page.waitForEvent('load');await dialog(page).getByRole('button',{name:'Yedeği yükle ve değiştir'}).click();await reloaded;
    assert.deepEqual(await page.evaluate(()=>JSON.parse(localStorage.getItem('ft_islemler'))),[]);
    assert.deepEqual(await page.evaluate(()=>JSON.parse(localStorage.getItem('ft_kategoriler'))),seedC.ft_kategoriler);assert.deepEqual(errors,[]);
  }finally{await context.close();}
});
test('UI-C09 — Tam işaretli ancak alanı kaybolmuş paket yükleme önizlemesine ulaşamaz',async()=>{
  const {context,page,errors}=await openSynthetic(seedC);
  try{await settings(page);const before=await userData(page);
    await choose(page,{format:'finora-v2',yedekTuru:'tam',anahtarlar:['ft_islemler','ft_kartlar'],data:{ft_kartlar:[]}});
    await page.getByText(/Yedeğin alan listesi dosya içeriğiyle eşleşmiyor/).waitFor();
    assert.equal(await dialog(page).count(),0);assert.deepEqual(await userData(page),before);assert.deepEqual(errors,[]);
  }finally{await context.close();}
});
