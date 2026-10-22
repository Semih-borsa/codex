import test,{before,after} from 'node:test';import assert from 'node:assert/strict';import fs from 'node:fs';import path from 'node:path';import http from 'node:http';import {fileURLToPath} from 'node:url';
const {chromium}=await import(process.env.FINANS_PLAYWRIGHT||'/opt/codex/cua_node/lib/node_modules/playwright-core/index.mjs');
const dist=process.env.FINANS_PDF_DIST||'/workspace/finans-upload/finans-takip/dist',fixtures=fileURLToPath(new URL('../finance-pdf/fixtures/',import.meta.url));let browser,server,url;
before(async()=>{server=http.createServer((req,res)=>{const name=decodeURIComponent((req.url||'/').split('?')[0]),p=path.resolve(dist,'.'+(name==='/'?'/index.html':name));if(!p.startsWith(path.resolve(dist)+path.sep)||!fs.existsSync(p)||!fs.statSync(p).isFile()){res.statusCode=404;res.end();return;}res.setHeader('Content-Type',p.endsWith('.js')||p.endsWith('.mjs')?'text/javascript':p.endsWith('.css')?'text/css':'text/html; charset=utf-8');res.end(fs.readFileSync(p));});await new Promise(r=>server.listen(0,'127.0.0.1',r));url=`http://127.0.0.1:${server.address().port}`;browser=await chromium.launch({executablePath:process.env.FINANS_CHROMIUM||'/usr/bin/chromium',args:['--no-sandbox']});});
after(async()=>{await browser?.close();if(server)await new Promise(r=>server.close(r));});

const today='2026-10-18';
async function open(tema='dark',extra={},nativeConfig=null) {
 const context=await browser.newContext({viewport:{width:360,height:844},timezoneId:'Europe/Istanbul'}),page=await context.newPage(),errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 page.on('dialog',()=>{throw new Error('Ödeme onayında native dialog açıldı');});
 await page.clock.install({time:new Date(today+'T12:00:00+03:00')});await page.clock.setFixedTime(new Date(today+'T12:00:00+03:00'));
 await page.addInitScript(({tema,extra})=>{if(localStorage.getItem('synthetic'))return;localStorage.setItem('synthetic','1');const data={ft_schema_version:'1.7.0',ft_islemler:[{id:'income',tip:'gelir',kategori:'Maaş',tutar:20000,tarih:'2026-10-15'}],ft_odemeler:[],ft_kartlar:[],ft_kart_odemeleri:[],ft_krediler:[],ft_taksitler:[],ft_tekrarlayan:[],ft_tema:tema,ft_donem_devir_log:{'2026-9-15':{tutar:0,tarih:'2026-10-15'}},...extra};Object.entries(data).forEach(([k,v])=>localStorage.setItem(k,JSON.stringify(v)));},{tema,extra});
 if(nativeConfig)await page.addInitScript(()=>{
   window.androidBridge={};window.__lnCalls=[];window.__lnPending=[{id:123,extra:{finoraTip:'odeme_hatirlatma',planAnahtari:'eski-sifir'}}];
   window.Capacitor={PluginHeaders:[{name:'LocalNotifications',methods:['getPending','checkPermissions','checkExactNotificationSetting','createChannel','schedule','cancel'].map(name=>({name,rtype:'promise'}))}],nativePromise:async(name,method,options)=>{
     if(name!=='LocalNotifications')throw new Error('Beklenmeyen native köprü');
     window.__lnCalls.push({method,options});
     if(method==='getPending')return {notifications:window.__lnPending};
     if(method==='checkPermissions')return {display:'granted'};
     if(method==='checkExactNotificationSetting')return {exact_alarm:'granted'};
     if(method==='schedule')window.__lnPending.push(...options.notifications);
     if(method==='cancel')window.__lnPending=window.__lnPending.filter(n=>!options.notifications.some(o=>o.id===n.id));
     return {};
   }};
 });
 await page.goto(url);await page.getByTestId('bugun-harcanan').waitFor();return {page,context,errors};
}
const storage=p=>p.evaluate(()=>Object.fromEntries(Object.keys(localStorage).filter(k=>['ft_islemler','ft_krediler','ft_kartlar','ft_odemeler','ft_kart_odemeleri'].includes(k)).map(k=>[k,localStorage.getItem(k)])));
const income={id:'income',tip:'gelir',kategori:'Maaş',tutar:20000,tarih:'2026-10-15'};
const loan={id:'loan',ad:'Deneme kredisi',toplamTutar:5000,vade:1,baslangic:'2026-10-20',taksitler:[{no:1,tarih:'2026-10-20',tutar:5000,odendi:false}]};
async function loanButton(page) {await page.getByRole('button',{name:'Menüyü aç'}).click();await page.getByRole('button',{name:'Krediler',exact:true}).click();await page.getByRole('button',{name:/Taksitleri/}).click();return page.locator('main button[style*="width: 20px"]').first();}
for(const tema of ['light','dark','gray'])test(`UI-YENI01-${tema} — Ödeme onayı temalı; iptal/Escape kayıtsız, onay tek ödeme`,async()=>{
 const {page,context,errors}=await open(tema,{ft_krediler:[loan]});try{
 const before=await storage(page);const button=await loanButton(page);await button.click();
 const dialog=page.getByRole('dialog',{name:'Ödeme Onayı',exact:true});await dialog.waitFor();
 assert.match(await dialog.innerText(),/5\.000,00/);assert.deepEqual(await storage(page),before);
 const colors=await dialog.evaluate(el=>({bg:getComputedStyle(el).backgroundColor,text:getComputedStyle(el).color}));assert.notEqual(colors.bg,'rgba(0, 0, 0, 0)');
 if(tema==='dark')assert.notEqual(colors.bg,'rgb(255, 255, 255)');
 await dialog.getByRole('button',{name:'Vazgeç',exact:true}).click();assert.deepEqual(await storage(page),before);
 await button.click();await dialog.waitFor();await page.keyboard.press('Escape');await dialog.waitFor({state:'hidden'});assert.deepEqual(await storage(page),before);
 await button.click();await dialog.getByRole('button',{name:'Ödemeyi Onayla',exact:true}).click();await dialog.waitFor({state:'hidden'});
 assert.equal(await page.evaluate(()=>JSON.parse(localStorage.getItem('ft_islemler')).filter(i=>i.kaynak==='kredi_taksiti').length),1);
 await page.getByRole('button',{name:'Özet',exact:true}).click();assert.match(await page.locator('.home-balance-value').innerText(),/15\.000,00/);
 assert.match(await page.getByTestId('bugun-harcanan').innerText(),/₺0,00/);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
const baseCard={id:'stock',tip:'gider',kategori:'World',kartHarcama:true,kartId:'c',tarih:today,tutar:10500,donemIci:10500,provizyon:0};
for(const [name,rows,expected]of [
 ['kart farkı ve nakit',[income,{...baseCard,kartGunlukHareketler:{'2026-10-17':1000,[today]:500}},{id:'cash',tip:'gider',kategori:'Market',tutar:250,tarih:today}],'750,00'],
 ['provizyonun kesinleşmesi',[income,{...baseCard,kartGunlukHareketler:{[today]:0}}],'0,00'],
 ['ilk kart borcu harcama değildir',[income,baseCard],'0,00'],
 ['ekstre ve diğer günler hariç',[income,{id:'pay',tip:'gider',kartEkstreOdeme:true,tutar:15000,tarih:today},{id:'old',tip:'gider',tutar:100,tarih:'2026-10-17'},{id:'future',tip:'gider',tutar:300,tarih:'2026-10-19'}],'0,00'],
])test(`UI-YENI02 — Bugün Harcanan: ${name}`,async()=>{
 const{page,context,errors}=await open('gray',{ft_islemler:rows,ft_kart_odemeleri:[{id:'pay',kartAdi:'World',tutar:15000,tarih:today}]});try{
 const before=await storage(page);assert.ok((await page.getByTestId('bugun-harcanan').innerText()).includes('₺'+expected));
 assert.equal(await page.evaluate(()=>{const card=document.querySelector('[data-testid="bugun-harcanan"]'),forecast=document.querySelector('.home-forecast');return !!forecast&&(forecast.compareDocumentPosition(card)&Node.DOCUMENT_POSITION_FOLLOWING)>0;}),true);
 await page.reload();await page.getByTestId('bugun-harcanan').waitFor();assert.ok((await page.getByTestId('bugun-harcanan').innerText()).includes('₺'+expected));assert.deepEqual(await storage(page),before);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
test('UI-YENI03 — Ödemeler ekranı onayı; ödenen fatura harcama toplamında yalnız bir kez',async()=>{
 const{page,context,errors}=await open('dark',{ft_odemeler:[{id:'bill',kategori:'Fatura',aciklama:'Deneme faturası',tutar:100,tarih:today,odendi:false}]});try{
 const before=await storage(page);await page.getByRole('button',{name:'Ödemeler',exact:true}).click();
 const check=page.getByRole('button',{name:'Ödendi olarak işaretle: Deneme faturası',exact:true});
 // Bekleyen liste varsayılan kapalıysa aç.
 if(!await check.isVisible())await page.locator('.payment-group > button').click();
 await check.click();const dialog=page.getByRole('dialog',{name:'Ödeme Onayı',exact:true});await dialog.waitFor();assert.deepEqual(await storage(page),before);
 await dialog.getByRole('button',{name:'Vazgeç',exact:true}).click();assert.deepEqual(await storage(page),before);
 await check.click();await dialog.getByRole('button',{name:'Ödemeyi Onayla',exact:true}).click();
 await page.getByRole('button',{name:'Özet',exact:true}).click();assert.match(await page.getByTestId('bugun-harcanan').innerText(),/₺100,00/);
 assert.equal(await page.evaluate(()=>JSON.parse(localStorage.getItem('ft_islemler')).filter(i=>i.odemeId==='bill').length),1);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
for(const tema of ['light','dark','gray'])test(`UI-KALDIR-${tema} — PDF analizi menüden kaldırıldı; özet ve kayıtlar korunur`,async()=>{
 const{page,context,errors}=await open(tema);try{
 const before=await storage(page);await page.getByRole('button',{name:'Menüyü aç'}).click();
 assert.equal(await page.getByRole('button',{name:'Ekstre Analizi',exact:true}).count(),0);
 await page.getByRole('button',{name:'Genel Bakış',exact:true}).click();await page.getByTestId('bugun-harcanan').waitFor();
 assert.deepEqual(await storage(page),before);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
for(const tema of ['light','dark','gray'])test(`UI-HAFTA-${tema} — Haftanın Günleri özet trendinin altında, toplam ve kart farkı korunur`,async()=>{
 const{page,context,errors}=await open(tema,{ft_islemler:[income,{id:'cash',tip:'gider',kategori:'Market',tutar:300,tarih:today},{...baseCard,kartGunlukHareketler:{[today]:500}}]});try{
 const before=await storage(page),card=page.locator('.weekday-card');await card.waitFor();
 const row=card.locator('.weekday-row').filter({has:page.getByText('Pazar',{exact:true})});assert.match(await row.innerText(),/800,00/);
 assert.equal(await card.evaluate(el=>el.previousElementSibling.textContent.includes('Harcama Trendin')),true);
 await page.getByRole('button',{name:'Raporlar',exact:true}).click();await page.getByRole('tab',{name:'Trend',exact:true}).click();
 assert.equal(await page.locator('.reports-page .weekday-card').count(),0);assert.equal(await card.isVisible(),false);assert.deepEqual(await storage(page),before);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
for(const tema of ['light','dark','gray'])for(const [oran,renk]of [[35,'#34d399'],[70,'#fbbf24'],[95,'#ff626a']])test(`UI-HALKA-${tema}-${oran} — Renkli halka, şeffaf iç, kontur ve kart düzeni korunur`,async()=>{
 const{page,context,errors}=await open(tema,{ft_grafik_tipi:'halka',ft_islemler:[{...income,tutar:10000},{id:'expense',tip:'gider',kategori:'Market',tutar:oran*100,tarih:today}]});try{
 const before=await storage(page);await page.clock.runFor(3600);
 const ring=page.locator('.budget-ring'),arc=ring.locator('[data-ring-progress]');assert.equal(await arc.getAttribute('stroke'),renk);assert.ok(Math.abs(Number(await arc.getAttribute('data-ring-progress'))-oran)<.1);
 assert.equal(await ring.locator('circle:not([fill="none"])').count(),0);assert.equal(await ring.locator('circle[stroke-width="13"]').count(),1);
 assert.equal(await page.locator('.finance-hero').getAttribute('data-status'),oran<50?'iyi':oran<80?'orta':'kotu');assert.deepEqual(await storage(page),before);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
for(const [label,total,paid,expected]of [['sıfır',0,0,0],['tam ödenmiş',5000,5000,0],['kısmi ödenmiş',5000,2000,2]])test(`UI-BILDIRIM-${label} — Native köprü planlama/iptal, saatler ve kayıt güvenliği`,async()=>{
 const cards=[{id:'c',isim:'Yeni Kart',ekstre:total,sonOdemeTarihi:today,hesapKesimTarihi:'2026-10-10',ekstreler:[{id:'e',tutar:total,hesapKesimTarihi:'2026-10-10',sonOdemeTarihi:today}]}];
 const payments=paid?[{id:'p',kartId:'c',ekstreId:'e',tutar:paid,tarih:'2026-10-17'}]:[];
 const{page,context,errors}=await open('dark',{ft_kartlar:cards,ft_kart_odemeleri:payments,ft_bildirim_ayarlari:{gunler:[0],saat:'15:00',saatler:['15:00','18:00']}},true);try{
 await page.waitForFunction(()=>{const p=JSON.parse(localStorage.getItem('ft_son_bildirim_planlama')||'null');return p&&p.sayi!=null;});
 const state=await page.evaluate(()=>({calls:window.__lnCalls,pending:window.__lnPending,status:JSON.parse(localStorage.getItem('ft_son_bildirim_planlama'))}));
 assert.ok(state.calls.some(c=>c.method==='cancel'&&c.options.notifications.some(n=>n.id===123)));
 assert.equal(state.pending.length,expected);assert.equal(state.status.sayi,expected);
 if(expected){assert.ok(state.calls.some(c=>c.method==='schedule'));assert.deepEqual(state.pending.map(n=>new Date(n.schedule.at).toLocaleTimeString('tr-TR',{timeZone:'Europe/Istanbul',hour:'2-digit',minute:'2-digit'})),['15:00','18:00']);}
 else assert.equal(state.calls.filter(c=>c.method==='schedule').length,0);
 assert.deepEqual(await page.evaluate(()=>JSON.parse(localStorage.getItem('ft_kartlar'))),cards);assert.deepEqual(await page.evaluate(()=>JSON.parse(localStorage.getItem('ft_kart_odemeleri'))),payments);assert.deepEqual(errors,[]);
 }finally{await context.close();}
});
