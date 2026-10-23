// Tarayıcı testi değildir: gerçek arayüz kodunu hafif DOM ile çalıştıran akış kontrolü.
import test from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import {readFile} from 'node:fs/promises';
const core=await readFile(new URL('../app/src/main/assets/core.mjs',import.meta.url),'utf8');
const app=await readFile(new URL('../app/src/main/assets/app.mjs',import.meta.url),'utf8');
function context(initial={}){
  const nodes=new Map(),storage=new Map(Object.entries(initial)),root={attrs:{},setAttribute(name,value){this.attrs[name]=value;}};
  function node(key){if(!nodes.has(key))nodes.set(key,{innerHTML:'',textContent:'',className:'',open:false,querySelector:node,addEventListener(){},showModal(){this.open=true;},close(){this.open=false;},focus(){}});return nodes.get(key);}
  const c=vm.createContext({document:{documentElement:root,querySelector:node,addEventListener(){},createElement:()=>({click(){}})},window:{addEventListener(){},scrollTo(){}},location:{protocol:'http:'},localStorage:{getItem:k=>storage.get(k)||null,setItem:(k,v)=>storage.set(k,v)},isAndroid:false,saveDocument:async()=>({saved:true}),openDocument:async()=>({text:'{}'}),refreshPrices:async()=>({quotes:{},errors:{},sources:[],checkedAt:new Date().toISOString()}),console,crypto,Intl,structuredClone,Map,Set,Date,JSON,Number,String,Blob,URL,AbortSignal,setTimeout:()=>1,clearTimeout(){},confirm:()=>true});
  vm.runInContext(core.replace(/^export /gm,'')+'\n'+app.replace(/^import[^\n]+\n/gm,''),c);
  return {run:s=>vm.runInContext(s,c),nodes,storage,root};
}
test('Altı ekran boş veri ile istisnasız oluşturulur',()=>{
  const c=context();for(const page of ['overview','holdings','deposits','banks','history','settings']){c.run(`view='${page}'; render();`);assert.match(c.nodes.get('#app').innerHTML,/portföyüm/);}
});
test('Modern özet ekranı yalnızca toplam ve dönemsel portföy kartlarını sunar',()=>{
  const c=context(),html=c.nodes.get('#app').innerHTML;
  assert.match(html,/overview-total-card/);assert.match(html,/portfolio-overview-card/);
  assert.ok(html.includes('Fiyatları güncelle'));assert.ok(html.includes('data-refresh-scope="all"'));
  for(const label of ['TOPLAM NET PORTFÖY DEĞERİ','Günlük K\/Z','Haftalık K\/Z','Aylık K\/Z','Özet','Geçmiş','Ayarlar'])assert.ok(html.includes(label));
  assert.doesNotMatch(html,/Varlık listesi|Varlık dağılımı|quick-actions/);
});
test('Özet ve portföy içi kartlar tutar, oran, renk, dağılım ve kapalı kayıtları gösterir',()=>{
  const c=context();
  c.run(`commit({...state,varliklar:[
    {id:'gain',portfoyId:'p1',tip:'hisse',sembol:'KAR',ad:'Kazanan',miktar:1,maliyetBirim:100},
    {id:'loss',portfoyId:'p1',tip:'hisse',sembol:'ZAR',ad:'Kaybeden',miktar:1,maliyetBirim:200},
    {id:'neutral',portfoyId:'p1',tip:'gram_altin',ad:'Maliyetsiz',miktar:1,maliyetBirim:0}
  ],quotes:{hisse_KAR:{bid:130,status:'manual'},hisse_ZAR:{bid:180,status:'manual'},gram_altin:{bid:50,status:'manual'}},kapaliPozisyonlar:[{id:'closed',portfoyId:'p1',tip:'hisse',sembol:'ESKI',ad:'Eski varlık',miktar:1,maliyetBirim:10,kapanisFiyat:20,kapanisTarihi:'2026-01-02',netSatis:20,kz:10,creditCash:false}]});`);
  let html=c.nodes.get('#app').innerHTML;
  assert.match(html,/Net kâr \/ zarar/);assert.match(html,/6,45%/);assert.match(html,/Gerçekleşmiş: \+₺10,00/);assert.match(html,/--portfolio-color:#3263da/);
  c.run(`pid='p1';view='holdings';render();`);html=c.nodes.get('#app').innerHTML;
  for(const value of ['portfolio-detail-hero','TOPLAM NET PORTFÖY DEĞERİ','asset-allocation','Kazanan','Kaybeden','Maliyetsiz','Kapatılan kayıtlar','Eski varlık','performance-profit','performance-loss','performance-neutral'])assert.ok(html.includes(value));
  assert.match(html,/Kazanan[\s\S]*?36,1%/);assert.match(html,/Kaybeden[\s\S]*?<strong>50%<\/strong>/);
});
test('Portföy kartının altında halka grafik ve en yüksek paylı beş varlık gösterilir',()=>{
  const c=context();c.run(`commit({...state,varliklar:[
    {id:'a1',portfoyId:'p1',tip:'try_lirasi',ad:'Varlık 1',miktar:50,maliyetBirim:1},
    {id:'a2',portfoyId:'p1',tip:'try_lirasi',ad:'Varlık 2',miktar:20,maliyetBirim:1},
    {id:'a3',portfoyId:'p1',tip:'try_lirasi',ad:'Varlık 3',miktar:10,maliyetBirim:1},
    {id:'a4',portfoyId:'p1',tip:'try_lirasi',ad:'Varlık 4',miktar:8,maliyetBirim:1},
    {id:'a5',portfoyId:'p1',tip:'try_lirasi',ad:'Varlık 5',miktar:6,maliyetBirim:1},
    {id:'a6',portfoyId:'p1',tip:'try_lirasi',ad:'Varlık 6',miktar:4,maliyetBirim:1},
    {id:'a7',portfoyId:'p1',tip:'try_lirasi',ad:'Varlık 7',miktar:2,maliyetBirim:1}
  ]});pid='p1';view='holdings';render();`);
  const html=c.nodes.get('#app').innerHTML,hero=html.indexOf('portfolio-detail-hero'),chart=html.indexOf('asset-allocation'),list=html.indexOf('Varlık listesi');
  assert.ok(hero>=0&&chart>hero&&list>chart);const chartHtml=html.slice(chart,list);
  for(const value of ['asset-donut','conic-gradient','En çok orana sahip varlıklar','İlk 5 varlık','Diğer varlıklar','Varlık 1','Varlık 5'])assert.ok(chartHtml.includes(value));
  assert.ok(!chartHtml.includes('Varlık 6'));assert.ok(!chartHtml.includes('Varlık 7'));assert.match(chartHtml,/Diğer varlıklar[\s\S]*?<b>6%<\/b>/);
});
test('Portföy dağılımının altında ve varlık ayrıntısında yumuşak fiyat grafiği görünür',()=>{
  const c=context();c.run(`let sample={...emptyState(),varliklar:[{id:'a',portfoyId:'p1',tip:'hisse',sembol:'AAAA',ad:'Grafikli varlık',miktar:2,maliyetBirim:80}],quotes:{hisse_AAAA:{bid:100,status:'manual'}}};sample=recordChartPoint(sample,'2026-09-09T08:00:00Z');sample={...sample,quotes:{hisse_AAAA:{bid:110,status:'manual'}}};sample=recordChartPoint(sample,'2026-09-09T10:00:00Z');state=sample;pid='p1';view='holdings';render();`);
  let html=c.nodes.get('#app').innerHTML,allocation=html.indexOf('asset-allocation'),trend=html.indexOf('trend-card'),list=html.indexOf('Varlık listesi');assert.ok(allocation>=0&&trend>allocation&&list>trend);
  for(const value of ['Portföy değer değişimi','trend-line','trend-area','Günlük','Haftalık','Aylık','+₺20,00'])assert.ok(html.includes(value));
  c.run(`detailModal('a')`);html=c.nodes.get('#modal').innerHTML;for(const value of ['Fiyat değişimi','trend-card compact','trend-line','110 ₺','+10 ₺'])assert.ok(html.includes(value));
});
test('Portföy silme Evet Hayır onayı verir ve otomatik yedek almadan yalnız seçili portföyü siler',()=>{
  const c=context();
  c.run(`commit({...state,varliklar:[{id:'a',portfoyId:'p1',tip:'try_lirasi',miktar:10,maliyetBirim:1},{id:'b',portfoyId:'p2',tip:'try_lirasi',miktar:20,maliyetBirim:1}],kapaliPozisyonlar:[{id:'c',portfoyId:'p1',tip:'hisse',sembol:'OLD'}]});let backupCalls=0;saveDocument=async()=>{backupCalls++;throw new Error('Yedek çağrılmamalı');};portfolioDeleteModal('p1');`);
  const html=c.nodes.get('#modal').innerHTML;
  for(const value of ['Bu portföyü silmek istediğinizden emin misiniz?','data-action="dismiss">Hayır','data-action="confirm-delete-portfolio"','Evet'])assert.ok(html.includes(value));
  c.run(`deletePortfolio('p1')`);
  assert.equal(c.run('backupCalls'),0);assert.equal(c.run('state.portfoyler.length'),1);assert.equal(c.run('state.portfoyler[0].id'),'p2');assert.equal(c.run('state.varliklar.length'),1);assert.equal(c.run('state.kapaliPozisyonlar.length'),0);
});
test('Koyu mod seçimi cihazda saklanır ve ayarlardan geri değiştirilebilir',()=>{
  const c=context();assert.equal(c.root.attrs['data-theme'],'light');
  c.run('toggleTheme()');assert.equal(c.root.attrs['data-theme'],'dark');assert.equal(c.storage.get('portfoy_theme'),'dark');
  c.run(`view='settings';render();`);assert.match(c.nodes.get('#app').innerHTML,/Koyu mod açık/);assert.match(c.nodes.get('#app').innerHTML,/Açık moda geç/);
  c.run('toggleTheme()');assert.equal(c.root.attrs['data-theme'],'light');
});
test('Portföyde varlık varken yeni varlık ekleme butonu görünür',()=>{
  const c=context();c.run(`commit({...state,varliklar:[{id:'nakit',portfoyId:'p1',tip:'try_lirasi',miktar:1000,maliyetBirim:1}]});view='holdings';render();`);
  const html=c.nodes.get('#app').innerHTML;assert.match(html,/Yeni varlık ekle/);assert.match(html,/data-action="add-asset"/);
});
test('Portföy içinde fiyat güncelleme butonu görünür ve yalnız açık portföy istenir',async()=>{
  const c=context();c.run(`commit({...state,varliklar:[{id:'a',portfoyId:'p1',tip:'hisse',sembol:'AAAA',miktar:1,maliyetBirim:10},{id:'b',portfoyId:'p2',tip:'hisse',sembol:'BBBB',miktar:1,maliyetBirim:20}]});pid='p1';view='holdings';render();isAndroid=true;let captured=[];refreshPrices=async assets=>{captured=assets;return {quotes:{},errors:{},sources:[],checkedAt:new Date().toISOString()}};`);
  const html=c.nodes.get('#app').innerHTML;assert.ok(html.includes('Fiyatları güncelle'));assert.ok(html.includes('data-refresh-scope="portfolio"'));
  await c.run(`refresh('portfolio')`);assert.equal(c.run('captured.length'),1);assert.equal(c.run('captured[0].sembol'),'AAAA');
});
test('Satış formu adet alır; parçalı satıştan sonra kalan varlık ve kapanış kaydı görünür',()=>{
  const c=context();c.run(`commit({...state,varliklar:[{id:'a',portfoyId:'p1',tip:'hisse',sembol:'AAAA',ad:'Test hissesi',miktar:10,maliyetBirim:100,alisKomisyon:10}],quotes:{hisse_AAAA:{bid:120,status:'manual'}}});closeModal('a');`);
  let html=c.nodes.get('#modal').innerHTML;for(const value of ['Satış / kapanış kaydı','Satılan miktar (adet)','Mevcut miktar','kalan miktar','Satışı kaydet'])assert.ok(html.includes(value));
  c.run(`modalState.onSubmit(new Map([['quantity','4'],['price','120'],['fee','2'],['date','2026-01-01'],['cash','on']]));pid='p1';view='holdings';render();`);
  assert.equal(c.run(`holdingSummary(state.varliklar.find(x=>x.id==='a')).quantity`),6);assert.equal(c.run('state.kapaliPozisyonlar[0].miktar'),4);assert.equal(c.run(`state.varliklar.find(x=>x.tip==='try_lirasi').miktar`),478);
  html=c.nodes.get('#app').innerHTML;assert.match(html,/Kapatılan kayıtlar/);assert.match(html,/Parçalı satış/);assert.match(html,/Satılan miktar/);assert.match(html,/>4 <small>adet<\/small>/);
});
test('Gerçek mevduat formu, faiz formu ve tüm ekran toplamları',()=>{
  const c=context();c.run(`assetModal(null,'vadeli_mevduat'); modalState.onSubmit(new Map([['portfoyId','p1'],['ad','Test mevduat'],['banka','ziraat'],['anapara','100.000,00'],['tarih','2026-01-01']]));`);
  assert.equal(c.run('totals(state).value'),100000);
  c.run(`depositEntryModal(state.varliklar[0].id); modalState.onSubmit(new Map([['tur','getiri'],['tutar','123,45'],['tarih','2026-01-02']]));`);
  assert.equal(c.run('totals(state).value'),100123.45);assert.equal(c.run('totals(state).interest'),123.45);
  assert.match(c.nodes.get('#app').innerHTML,/100\.123,45/);
  c.run(`view='deposits';render();`);assert.match(c.nodes.get('#app').innerHTML,/100\.123,45/);
  c.run(`depositEntryModal(state.varliklar[0].id,state.varliklar[0].islemler[1].id); modalState.onSubmit(new Map([['tur','getiri'],['tutar','150'],['tarih','2026-01-02']]));`);
  assert.equal(c.run('totals(state).value'),100150);
  for(const page of ['overview','holdings','deposits','banks','history','settings'])c.run(`view='${page}';render();`);
  const saved=JSON.parse(c.storage.get('portfoy_v4'));assert.equal(saved.varliklar[0].islemler[1].tutar,150);
});
test('Fon formu ve manuel fiyat formu hassasiyeti korur',()=>{
  const c=context();c.run(`assetModal(null,'tefas_fon');modalState.onSubmit(new Map([['portfoyId','p1'],['sembol','ABC'],['fonTip','YAT'],['miktar','1000'],['maliyetBirim','0,123456']]));`);
  assert.equal(c.run('totals(state).missing'),1);
  c.run(`priceModal('tefas_ABC','ABC');modalState.onSubmit(new Map([['bid','0,234567'],['date','2026-01-01'],['locked','on']]));`);
  assert.equal(c.run('totals(state).value'),234.57);assert.equal(c.run('state.quotes.tefas_ABC.bid'),.234567);
  c.run('detailModal(state.varliklar[0].id)');assert.match(c.nodes.get('#modal').innerHTML,/Elle giriş/);
});
test('TEFAS formu stopaj oranını kaydeder; kartlar brüt ve net kârı doğru gösterir',async()=>{
  const c=context();c.run(`assetModal(null,'tefas_fon')`);let html=c.nodes.get('#asset-fields').innerHTML;
  for(const value of ['Bu fonda stopaj var mı?','name="stopajOrani"','value="17,5"','id="stopaj-rate-field" hidden'])assert.ok(html.includes(value));
  c.run(`modalState.onSubmit(new Map([['portfoyId','p1'],['sembol','ABC'],['fonTip','YAT'],['miktar','100'],['maliyetBirim','1'],['stopajVar','on'],['stopajOrani','17,5']]))`);
  assert.equal(c.run('state.varliklar[0].stopajVar'),true);assert.equal(c.run('state.varliklar[0].stopajOrani'),17.5);
  c.run(`priceModal('tefas_ABC','ABC');modalState.onSubmit(new Map([['bid','2'],['date','2026-01-01'],['locked','on']]))`);
  assert.equal(c.run('totals(state).value'),182.5);assert.equal(c.run('totals(state).pnl'),82.5);
  c.run(`view='overview';render()`);html=c.nodes.get('#app').innerHTML;for(const value of ['TOPLAM NET PORTFÖY DEĞERİ','₺182,50','Net kâr / zarar','₺82,50','Tahmini stopaj düşüldü: ₺17,50'])assert.ok(html.includes(value));
  c.run(`pid='p1';view='holdings';render()`);html=c.nodes.get('#app').innerHTML;for(const value of ['TAHMİNİ NET DEĞER','Stopajsız kâr / zarar','₺100,00','Tahmini stopaj','−₺17,50'])assert.ok(html.includes(value));
  c.run(`detailModal(state.varliklar[0].id)`);html=c.nodes.get('#modal').innerHTML;for(const value of ['withholding-summary','Tahmini net değer','Net kâr / zarar','Stopajsız kâr / zarar','Tahmini stopaj (17,5%)'])assert.ok(html.includes(value));
  c.run(`let exported='';saveDocument=async(name,text)=>{exported=text;return {saved:true}}`);await c.run('exportCSV()');assert.match(c.run('exported'),/Brüt değer TL/);assert.match(c.run('exported'),/Net kar zarar TL/);assert.match(c.run('exported'),/17,5/);
  c.run(`assetModal(state.varliklar[0].id);modalState.onSubmit(new Map([['portfoyId','p1'],['sembol','ABC'],['fonTip','YAT'],['miktar','100'],['maliyetBirim','1'],['stopajOrani','17,5']]))`);assert.equal(c.run('state.varliklar[0].stopajVar'),false);assert.equal(c.run('totals(state).value'),200);
});
test('Stopajlı TEFAS kapanışı net nakdi kaydeder ve güvenle geri alınabilir',()=>{
  const c=context();c.run(`commit({...state,varliklar:[{id:'f',portfoyId:'p1',tip:'tefas_fon',sembol:'ABC',ad:'Stopajlı fon',miktar:100,maliyetBirim:1,stopajVar:true,stopajOrani:17.5}],quotes:{tefas_ABC:{bid:2,status:'manual'}}});closeModal('f');`);
  let html=c.nodes.get('#modal').innerHTML;for(const value of ['Kesilen stopaj','boşsa otomatik','Komisyon ve stopaj sonrası net satış'])assert.ok(html.includes(value));
  c.run(`modalState.onSubmit(new Map([['quantity','100'],['price','2'],['fee','0'],['date','2026-01-01'],['cash','on']]))`);assert.equal(c.run(`state.varliklar.find(a=>a.tip==='try_lirasi').miktar`),182.5);assert.equal(c.run('state.kapaliPozisyonlar[0].kz'),82.5);assert.equal(c.run('totals(state).pnl'),82.5);
  c.run(`pid='p1';view='holdings';render()`);html=c.nodes.get('#app').innerHTML;for(const value of ['Stopajsız: ₺100,00','Stopaj: −₺17,50','Geri al','Gerçekleşmiş: +₺82,50'])assert.ok(html.includes(value));
  c.run(`undoCloseModal(state.kapaliPozisyonlar[0].id)`);html=c.nodes.get('#modal').innerHTML;for(const value of ['Kapanış kaydı geri alınsın mı?','Evet, geri al'])assert.ok(html.includes(value));
  c.run(`undoClose(state.kapaliPozisyonlar[0].id)`);assert.equal(c.run('state.kapaliPozisyonlar.length'),0);assert.equal(c.run(`state.varliklar.some(a=>a.id==='f')`),true);assert.equal(c.run(`state.varliklar.some(a=>a.tip==='try_lirasi')`),false);
});
test('Depolama hatası sessiz kayıp oluşturmaz, state eski kalır',()=>{
  const c=context();c.run(`localStorage.setItem=()=>{throw new Error('full')};`);
  assert.throws(()=>c.run(`commit({...state,varliklar:[{id:'x',portfoyId:'p1',tip:'try_lirasi',miktar:5,maliyetBirim:1}]})`),/Kayıt yapılamadı/);
  assert.equal(c.run('state.varliklar.length'),0);
});
test('Bozuk ilk kayıt boş veriyle otomatik ezilmez',()=>{
  const c=context({portfoy_v4:'broken'});assert.equal(c.storage.get('portfoy_v4'),'broken');assert.match(c.nodes.get('#app').innerHTML,/Kayıt okunamadı/);
  assert.throws(()=>c.run('commit(state)'),/Kayıt sorunu/);
});
test('HTML özel karakterleri kullanıcı adı olarak güvenli gösterilir',()=>{
  const c=context();c.run(`commit({...state,portfoyler:[{id:'p1',ad:'<script>alert(1)</script>',renk:'#3263da'}]});`);
  assert.ok(!c.nodes.get('#app').innerHTML.includes('<script>alert(1)</script>'));assert.match(c.nodes.get('#app').innerHTML,/&lt;script&gt;/);
});
test('Android yedek kaydı iptal edilirse geri yükleme mevcut veriyi değiştirmez',async()=>{
  const c=context();
  c.run(`openDocument=async()=>({text:JSON.stringify({...emptyState(),varliklar:[{id:'new',portfoyId:'p1',tip:'try_lirasi',miktar:750,maliyetBirim:1}]})});saveDocument=async()=>{throw new Error('Dosya işlemi iptal edildi.')};`);
  await c.run('importBackup()');
  await assert.rejects(c.run('modalState.onSubmit()'),/iptal/);
  assert.equal(c.run('state.varliklar.length'),0);
});
test('Android geri yükleme yedek yazıldıktan sonra uygulanır',async()=>{
  const c=context();
  c.run(`openDocument=async()=>({text:JSON.stringify({...emptyState(),varliklar:[{id:'new',portfoyId:'p1',tip:'try_lirasi',miktar:750,maliyetBirim:1}]})});`);
  await c.run('importBackup()');await c.run('modalState.onSubmit()');
  assert.equal(c.run('totals(state).value'),750);
});
test('Android geri tuşu önce formu sonra sayfayı kapatır',()=>{
  const c=context();c.run("view='deposits';render();assetModal(null,'vadeli_mevduat')");
  assert.equal(c.run('window.PortfoyumAppBack()'),true);assert.equal(c.run('view'),'deposits');
  assert.equal(c.run('window.PortfoyumAppBack()'),true);assert.equal(c.run('view'),'overview');
  assert.equal(c.run('window.PortfoyumAppBack()'),false);
});

test('Bozuk ilk kayıtta normal yedek boş dosya üretmez',async()=>{
  const c=context({portfoy_v4:'broken'});c.run(`let saved=0;saveDocument=async()=>{saved++;return {saved:true}}`);
  await assert.rejects(c.run('backup()'),/normal yedek oluşturulmadı/);assert.equal(c.run('saved'),0);assert.equal(c.storage.get('portfoy_v4'),'broken');
});
test('Ham kurtarma yalnız orijinal metni kaydeder ve JSON yedeği gibi sunulmaz',async()=>{
  const c=context({portfoy_v4:'broken'});c.run(`let recovery;saveDocument=async(name,text,mime)=>{recovery={name,text,mime};return {saved:true}}`);await c.run('recoveryBackup()');
  assert.equal(c.run('recovery.text'),'broken');assert.match(c.run('recovery.name'),/HAM-KURTARMA.*\.txt$/);assert.equal(c.run('recovery.mime'),'text/plain');assert.match(c.nodes.get('#toast').textContent,/doğrulanmış JSON yedeği değildir/);
});
test('Bozuk kapanış içeren yedek önizlemeye ve depolamaya geçmez',async()=>{
  const c=context();c.run(`openDocument=async()=>({text:JSON.stringify({...emptyState(),kapaliPozisyonlar:[null]})})`);await c.run('importBackup()');assert.match(c.nodes.get('#toast').textContent,/Mevcut kayıtlar değiştirilmedi/);assert.equal(c.storage.has('portfoy_v4'),false);assert.equal(c.run('state.kapaliPozisyonlar.length'),0);
});
test('Bozuk JSON mevcut kaydı değiştirmez',async()=>{const c=context();c.run(`openDocument=async()=>({text:'bozuk-json'})`);await c.run('importBackup()');assert.match(c.nodes.get('#toast').textContent,/Yedek yüklenemedi/);assert.equal(c.storage.has('portfoy_v4'),false);});
test('Geri yüklemede depolama yazma hatası eski state ve veriyi korur',async()=>{
  const c=context();c.run(`commit({...state,varliklar:[{id:'old',portfoyId:'p1',tip:'try_lirasi',miktar:500,maliyetBirim:1}]});let before=localStorage.getItem(STORAGE);openDocument=async()=>({text:JSON.stringify(emptyState())})`);await c.run('importBackup()');c.run(`localStorage.setItem=()=>{throw new Error('disk full')}`);await assert.rejects(c.run('modalState.onSubmit()'),/cihaz depolamasına yazılamadı/);assert.equal(c.storage.get('portfoy_v4'),c.run('before'));assert.equal(c.run('totals(state).value'),500);
});
test('Dosya seçicisi açıkken değişen kayıt üzerine geri yüklenmez',async()=>{
  const c=context();c.run(`openDocument=async()=>({text:JSON.stringify(emptyState())});saveDocument=async()=>{localStorage.setItem(STORAGE,'external-change');return {saved:true}}`);await c.run('importBackup()');await assert.rejects(c.run('modalState.onSubmit()'),/Güvenlik için/);assert.equal(c.storage.get('portfoy_v4'),'external-change');
});
test('İptal edilmiş normal yedek başarı mesajı üretmez',async()=>{const c=context();c.run(`saveDocument=async()=>{throw new Error('iptal')}`);await assert.rejects(c.run('backup()'),/iptal/);assert.notEqual(c.nodes.get('#toast')?.textContent,'Yedek seçtiğin konuma kaydedildi.');});
test('Gerçek alış düzenleme formu satışın maliyetini değiştiremez',()=>{
  const c=context();c.run(`commit({...state,varliklar:[{id:'a',portfoyId:'p1',tip:'hisse',sembol:'TEST',islemler:[{id:'buy',tur:'alis',tarih:'2026-01-01',miktar:100,fiyat:10,komisyon:0}]}],quotes:{hisse_TEST:{bid:20,status:'manual'}}});commit(closeHolding(state,'a',20,0,'2026-02-01',false,50));holdingEntryModal('a','buy')`);
  assert.throws(()=>c.run(`modalState.onSubmit(new Map([['tur','alis'],['tarih','2026-01-01'],['miktar','100'],['fiyat','15'],['komisyon','0']]))`),/bağlı satış/);assert.equal(c.run('totals(state).realizedPnl'),500);assert.equal(c.run('state.varliklar[0].islemler[0].fiyat'),10);
});
test('Özet K/Z kutusu dışarıdan eklenen parayı kazanç yazmaz',()=>{
  const c=context();c.run(`state=recordSnapshot({...state,varliklar:[{id:'cash',portfoyId:'p1',tip:'try_lirasi',miktar:10000,maliyetBirim:1}]},new Date(Date.now()-86400000).toISOString());state.varliklar[0].miktar=20000;render()`);const html=c.nodes.get('#app').innerHTML;assert.match(html,/Günlük K\/Z/);assert.match(html,/₺0,00/);assert.doesNotMatch(html,/>\+₺10\.000,00<\/b>/);
});

 test('Yedek önizlemesi eklenecek, değişecek ve silinecek kayıtları gösterir',async()=>{const c=context();c.run(`commit({...state,varliklar:[{id:'old',portfoyId:'p1',tip:'try_lirasi',miktar:100,maliyetBirim:1}]});openDocument=async()=>({text:JSON.stringify({...emptyState(),varliklar:[{id:'new',portfoyId:'p1',tip:'try_lirasi',miktar:200,maliyetBirim:1}]})})`);await c.run('importBackup()');assert.match(c.nodes.get('#modal').innerHTML,/1 eklenecek · 0 değişecek · 1 silinecek/);assert.equal(c.run('totals(state).value'),100);});
