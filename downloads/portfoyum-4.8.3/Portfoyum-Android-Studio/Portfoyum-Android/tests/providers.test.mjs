import test from 'node:test';
import assert from 'node:assert/strict';
import {parseTefas,parseTefasPage,tefasDate,fetchTefas,parseBankPage,parseBankDetailPage,parseGeneral,parseDovizGeneralPage,fetchStock,parseAltins1Page,parseNetdaniaAltins1,fetchAltins1,getText} from '../app/src/main/assets/providers.mjs';
test('TEFAS doğru fonun en yeni tarihini seçer, dizi sırasına güvenmez',()=>{
  const q=parseTefas({data:[{FONKODU:'ABC',FIYAT:'1,123456',TARIH:'31.01.2026'},{FONKODU:'ZZZ',FIYAT:999,TARIH:'02.02.2026'},{FONKODU:'ABC',FIYAT:'1.234567',TARIH:'01.02.2026'}]},'ABC');
  assert.equal(q.bid,1.234567);assert.equal(q.priceDate,'2026-02-01');
  assert.equal(tefasDate('/Date(1769904000000)/'),1769904000000);
});
test('Fon kodu bulunmazsa başka fonun fiyatı kullanılmaz',()=>{
  assert.throws(()=>parseTefas({data:[{FONKODU:'XYZ',FIYAT:5,TARIH:'01.01.2026'}]},'ABC'),/Eski fiyat korundu/);
  assert.throws(()=>parseTefas({data:[{FONKODU:'ABC',FIYAT:5,TARIH:'bozuk'}]},'ABC'));
});
test('TEFAS tek fon yanıtında kod alanı yoksa istenen tablonun fiyatı okunur',()=>{
  const q=parseTefas({data:[{FIYAT:'2,345678',TARIH:'02.02.2026'}]},'ABC');
  assert.equal(q.bid,2.345678);assert.equal(q.priceDate,'2026-02-02');
});
test('TEFAS Next.js portalı resmi güncel API üzerinden istenen fonu doğrular',async()=>{
  const requests=[];
  const mock=async(url,opts)=>{requests.push({url,opts});return url.includes('/api/funds/')
    ?new Response(JSON.stringify({resultList:[{fonKodu:'THF',fiyat:'1,234567',tarih:'20260901'}]}),{status:200})
    :new Response('<html>TEFAS fon verileri</html>',{status:200});};
  const q=await fetchTefas('THF','YAT',mock);assert.equal(q.bid,1.234567);assert.equal(q.priceDate,'2026-09-01');
  assert.match(requests[0].url,/\/tr\/fon-verileri\?fundType=YAT/);assert.equal(requests[0].opts.method,'GET');
  assert.equal(requests[1].url,'https://www.tefas.gov.tr/api/funds/fonGnlBlgSiraliGetir');assert.equal(requests[1].opts.method,'POST');
  const body=JSON.parse(requests[1].opts.body);assert.equal(body.fonKodu,null);assert.equal(body.aramaMetni,null);assert.equal(body.fonTipi,'YAT');assert.equal(body.basSira,1);
});
test('TEFAS sayfalı tabloda fon kodunu uygulama içinde birebir bulur',async()=>{
  let posts=0;
  const mock=async(url,opts)=>{
    if(!url.includes('/api/funds/'))return new Response('<html>portal</html>');
    posts++;const body=JSON.parse(opts.body);
    if(body.basSira===1)return new Response(JSON.stringify({toplamSayfa:2,resultList:Array.from({length:1000},(_,i)=>({fonKodu:'X'+i,fiyat:1,tarih:'20260901'}))}));
    return new Response(JSON.stringify({toplamSayfa:2,resultList:[{fonKodu:'THF',fiyat:'2,345678',tarih:'20260901'}]}));
  };
  const q=await fetchTefas('THF','YAT',mock);assert.equal(q.bid,2.345678);assert.equal(posts,2);
});
test('TEFAS fon sayfası görünen ve gömülü Next.js verisinde başka kodu kullanmaz',()=>{
  const page='<main><div>Fon Kodu</div><strong>THF</strong><div>Son Fiyat (TL)</div><strong>1.234,567890</strong></main>';
  assert.equal(parseTefasPage(page,'THF').bid,1234.56789);assert.throws(()=>parseTefasPage(page,'ABC'),/doğrulayamadı/);
  const embedded='<script>self.__next_f.push([1,"{\\"fonKodu\\":\\"THF\\",\\"fiyat\\":\\"0,987654\\"}"])</script>';
  assert.equal(parseTefasPage(embedded,'THF').bid,.987654);assert.throws(()=>parseTefasPage(embedded,'ABC'));
});
test('403 ve erişim reddi HTML başarı sayılmaz, yeniden deneme yapmaz',async()=>{
  let n=0;await assert.rejects(()=>getText('https://example.test',{},async()=>{n++;return new Response('Denied',{status:403});}),/reddetti/);assert.equal(n,1);
  await assert.rejects(()=>getText('https://example.test',{},async()=>new Response('The requested URL was rejected.')),/izin vermedi/);
});
test('Banka alış/satış aynı satırın doğru anahtarından gelir; sponsor rakamları alınmaz',()=>{
  const html=`<div>Garanti 98765 TL</div><table><tr><td><a href="/garanti-bbva/gram-altin">Garanti</a></td><td data-socket-key="8-gram-altin" data-socket-attr="bid"><span>6.123,45</span></td><td data-socket-attr="ask" data-socket-key="8-gram-altin">6.234,56</td></tr></table>`;
  const q=parseBankPage(html,'gold');assert.equal(q.banka_altin_garanti.bid,6123.45);assert.equal(q.banka_altin_garanti.ask,6234.56);assert.equal(Object.keys(q).length,1);
  assert.deepEqual(parseBankPage(html,'silver'),{});
});
test('Yeni banka tablosunda etiket yoksa aynı satırdaki ilk iki fiyat okunur',()=>{
  const html='<table><tr><th>Banka</th><th>Alış</th><th>Satış</th><th>Makas</th></tr><tr><td><a href="/ziraat-bankasi/gumus">Ziraat Bankası</a></td><td>98,75</td><td>113,15</td><td>14,40</td><td>%14,58</td></tr></table>';
  const q=parseBankPage(html,'silver');assert.equal(q.banka_gumus_ziraat.bid,98.75);assert.equal(q.banka_gumus_ziraat.ask,113.15);
});
test('m.doviz mobil banka satırları altın ve gümüş için banka adıyla okunur',()=>{
  const gold='<table><tr><td>Ziraat Bankası</td><td>6.900,10</td><td>7.010,20</td></tr></table>';
  const silver='<table><tr><td>Garanti BBVA</td><td>98,75</td><td>113,15</td></tr></table>';
  const g=parseBankPage(gold,'gold','m.doviz.com'),s=parseBankPage(silver,'silver','m.doviz.com');
  assert.equal(g.banka_altin_ziraat.bid,6900.1);assert.equal(g.banka_altin_ziraat.ask,7010.2);assert.match(g.banka_altin_ziraat.source,/m\.doviz/);
  assert.equal(s.banka_gumus_garanti.bid,98.75);assert.equal(s.banka_gumus_garanti.ask,113.15);
});
test('Döviz.com engellendiğinde alternatif gram altın banka tabloları okunur',()=>{
  const altinApp='<table><tr><th>Banka</th><th>Alış</th><th>Satış</th><th>Makas</th></tr><tr><td><a href="/ziraat-bankasi">ZİRAAT BANKASI</a> • 18:17</td><td>6.496,23</td><td>7.436,16</td><td>%12,64</td></tr></table>';
  const canli='<table><tr><td><a href="/banka/akbank.html">Akbank</a> 00:40:11</td><td>6546.59</td><td>7104.37</td><td>557.78 7.85%</td></tr></table>';
  const z=parseBankPage(altinApp,'gold','Altın.app'),a=parseBankPage(canli,'gold','CanlıAltın');
  assert.equal(z.banka_altin_ziraat.bid,6496.23);assert.equal(z.banka_altin_ziraat.ask,7436.16);assert.equal(z.banka_altin_ziraat.sourceUrl,'https://altin.app/');
  assert.equal(a.banka_altin_akbank.bid,6546.59);assert.equal(a.banka_altin_akbank.ask,7104.37);assert.match(a.banka_altin_akbank.source,/CanlıAltın/);
});
test('Toplu tablo alınamazsa seçili bankanın ayrıntı sayfası okunur',()=>{
  const gold='<title>Ziraat Bankası Gram Altın</title><meta name="description" content="Gram Altın Ziraat Bankası Gram Altın. Son (19:04). 7.436,16. Alış / Satış 6.496,23 / 7.436,16">';
  const silver='<h1>Garanti BBVA Gram Gümüş</h1><div>Alış / Satış 98,75 / 113,15</div>';
  const g=parseBankDetailPage(gold,'gold','ziraat'),s=parseBankDetailPage(silver,'silver','garanti');
  assert.equal(g.banka_altin_ziraat.bid,6496.23);assert.equal(g.banka_altin_ziraat.ask,7436.16);
  assert.equal(s.banka_gumus_garanti.bid,98.75);assert.equal(s.banka_gumus_garanti.ask,113.15);
  assert.throws(()=>parseBankDetailPage(gold,'gold','akbank'),/doğru bankayı/);
});
test('Banka fiyat etiketi yoksa ve ters makas varsa fiyat uydurulmaz',()=>{
  assert.deepEqual(parseBankPage('<tr><a href="/akbank/gram-altin">Akbank</a>6.000,00 7.000,00</tr>','gold'),{});
  const h='<tr><a href="/akbank/gram-altin">A</a><td data-socket-key="1-gram-altin" data-socket-attr="bid">7000</td><td data-socket-key="1-gram-altin" data-socket-attr="ask">6000</td></tr>';
  assert.deepEqual(parseBankPage(h,'gold'),{});
});
test('Fiziki altın türleri birbirinden türetilmez, Reşat Ata ile karıştırılmaz',()=>{
  const q=parseGeneral(JSON.stringify({'gram-altin':{'Alış':'6.000,50','Satış':'6.020,50'},'ata-altin':{'Alış':'45.000,00'},EUR:{'Alış':'50,12','Satış':'50,24'}}));
  assert.equal(q.gram_altin.bid,6000.5);assert.equal(q.euro.bid,50.12);assert.equal(q.resat_altin,undefined);assert.equal(q.cumhuriyet_altin,undefined);assert.equal(q.bilezik_24,undefined);
});
test('Canlı kaynak biçimindeki fiziki altın, gümüş ve tarih alanları okunur',()=>{
  const q=parseGeneral(JSON.stringify({Update_Date:'2026-09-01 00:15:02','gram-altin':{'Alış':'6.902,41','Satış':'6.903,21'},'ceyrek-altin':{'Alış':'10.866,25','Satış':'11.116,48'},'cumhuriyet-altini':{'Alış':'45.276,00','Satış':'45.960,00'},'resat-altin':{'Alış':'44.823,30','Satış':'45.961,71'},'22-ayar-bilezik':{'Alış':'6.193,77','Satış':'6.200,75'},gumus:{'Alış':'103,21','Satış':'103,30'}}));
  assert.equal(q.gram_altin.bid,6902.41);assert.equal(q.ceyrek_altin.ask,11116.48);assert.equal(q.cumhuriyet_altin.bid,45276);assert.equal(q.resat_altin.bid,44823.3);assert.equal(q.bilezik_22.ask,6200.75);assert.equal(q.gram_gumus.bid,103.21);assert.equal(q.gram_altin.priceDate,'2026-09-01');
});
test('Truncgil yoksa Döviz.com fiziki ürün sayfasındaki alış/satış okunur',()=>{
  const socket='<span data-socket-key="gram-altin" data-socket-attr="bid">6.806,10</span><span data-socket-key="gram-altin" data-socket-attr="ask">6.807,25</span>';
  const q=parseDovizGeneralPage(socket,'gram_altin');assert.equal(q.gram_altin.bid,6806.1);assert.equal(q.gram_altin.ask,6807.25);
  const visible='<main><h1>Çeyrek Altın</h1><p>Alış / Satış 10.950,25 / 11.125,50</p></main>';
  assert.equal(parseDovizGeneralPage(visible,'ceyrek_altin').ceyrek_altin.ask,11125.5);
  assert.throws(()=>parseDovizGeneralPage('<p>Rastgele 99.999</p>','gram_altin'));
});
test('Hisse yanlış sembol, para birimi veya eski kapanışla güncel sayılmaz',async()=>{
  const mock=m=>async()=>new Response(JSON.stringify({chart:{result:[{meta:m}]}}));
  await assert.rejects(()=>fetchStock('ABC',mock({symbol:'XYZ.IS',currency:'TRY',regularMarketPrice:10})));
  await assert.rejects(()=>fetchStock('ABC',mock({symbol:'ABC.IS',currency:'USD',regularMarketPrice:10})));
  await assert.rejects(()=>fetchStock('ABC',mock({symbol:'ABC.IS',currency:'TRY',previousClose:10})));
  assert.equal((await fetchStock('ABC',mock({symbol:'ABC.IS',currency:'TRY',regularMarketPrice:10}))).bid,10);
});
test('ALTINS1 Borsa.Döviz meta açıklaması, soket ve JSON verisinden okunur',()=>{
  const meta='<meta name="description" content="ALTINS1 hissesinin fiyatı 73,12 liradır. Önceki kapanışa göre düşmüştür.">';
  const socket='<span data-socket-key="ALTINS1" data-socket-attr="s">75,12</span>';
  const json='<script>{"symbol":"ALTINS1","last":"74.48"}</script>';
  assert.equal(parseAltins1Page(meta).bid,73.12);assert.equal(parseAltins1Page(socket).bid,75.12);assert.equal(parseAltins1Page(json).bid,74.48);
});
test('ALTINS1 detail/list engellenirse NetDania Borsa İstanbul yedeğine geçer',async()=>{
  const net='<h1>DARPHANE ALTIN SERTIFIKASI</h1><div class="price">73.05</div><div>01-September-26 17:09:58</div><div>ISIN</div><div>TRXDRP012213</div>';
  assert.equal(parseNetdaniaAltins1(net).bid,73.05);
  let calls=0;const fallback=async url=>{calls++;return url.includes('netdania')?new Response(net):new Response('Denied',{status:403});};
  const q=await fetchAltins1(fallback);assert.equal(q.bid,73.05);assert.match(q.source,/NetDania/);assert.equal(calls,3);
});
