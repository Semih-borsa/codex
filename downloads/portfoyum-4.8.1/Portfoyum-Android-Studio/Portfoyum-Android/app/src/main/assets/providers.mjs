import {nativeFetch} from './native.mjs';
import {BANKS, TYPES, parseNumber, quoteKey, today} from './core.mjs';

export class SourceError extends Error {constructor(message,code='SOURCE_ERROR'){super(message);this.code=code;}}
const sourceCache=new Map();
const pending=new Map();
const clean=s=>String(s??'').replace(/<[^>]*>/g,'').replace(/&nbsp;|&#160;/g,' ').replace(/&amp;/g,'&').trim();
const normalize=s=>s.toLocaleLowerCase('tr-TR').replace(/[ıİ]/g,'i').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-z0-9]/g,'');
export async function getText(url,options={},fetcher=nativeFetch) {
  let res;
  try {res=await fetcher(url,{...options,redirect:'error',headers:{'Accept':'text/html, application/json;q=0.9','User-Agent':'PortfoyTakip/4.8.0 (personal price viewer)',...options.headers}});}
  catch(e){throw new SourceError(e.name==='TimeoutError'?'Kaynak zaman aşımına uğradı.':'Kaynağa bağlanılamadı. İnternet veya kaynak erişimini kontrol edin.','NETWORK');}
  if(res.status===403||res.status===429)throw new SourceError('Kaynak erişimi reddetti veya istek sınırına ulaşıldı. Daha sonra deneyin ya da fiyatı elle girin.','ACCESS_DENIED');
  if(!res.ok)throw new SourceError('Kaynak HTTP '+res.status+' yanıtı verdi.','HTTP');
  const text=await res.text();
  if(text.length>8_000_000)throw new SourceError('Kaynak yanıtı beklenenden büyük.');
  if(/requested URL was rejected|access denied|captcha|challenge-platform|cloudflare.*challenge/i.test(text))throw new SourceError('Kaynak otomatik erişime izin vermedi. Fiyat değiştirilmedi.','ACCESS_DENIED');
  return text;
}
async function cached(key,ttl,job){
  const prior=sourceCache.get(key);
  if(prior&&Date.now()-prior.at<ttl)return prior.data;
  if(pending.has(key))return pending.get(key);
  const work=job().then(data=>{sourceCache.set(key,{at:Date.now(),data});return data;}).finally(()=>pending.delete(key));
  pending.set(key,work);return work;
}
export function tefasDate(value){
  if(typeof value==='number')return value<1e11?value*1000:value;
  const s=String(value??'').trim();
  const wrapped=s.match(/^\/?Date\((-?\d+)(?:[+-]\d{4})?\)\/?$/);
  if(wrapped)return Number(wrapped[1]);
  if(/^\d{12,13}$/.test(s))return Number(s);
  if(/^\d{8}$/.test(s))return Date.parse(`${s.slice(0,4)}-${s.slice(4,6)}-${s.slice(6,8)}T12:00:00+03:00`);
  const tr=s.match(/^(\d{2})[./](\d{2})[./](\d{4})$/);
  if(tr)return Date.parse(`${tr[3]}-${tr[2]}-${tr[1]}T12:00:00+03:00`);
  return Date.parse(s);
}
const tefasRows=j=>Array.isArray(j)?j:['resultList','data','Data','result','Result','rows','items'].map(k=>j?.[k]).find(Array.isArray);
const tefasRowCode=r=>String(r?.FONKODU??r?.FONKOD??r?.fonKodu??r?.fonKod??'').trim().toUpperCase();
export function parseTefas(json,code){
  let j;try{j=typeof json==='string'?JSON.parse(json):json;}catch{throw new SourceError('TEFAS geçerli JSON döndürmedi.');}
  const data=tefasRows(j);
  if(!Array.isArray(data))throw new SourceError('TEFAS beklenen fiyat tablosunu döndürmedi.');
  const coded=data.filter(r=>tefasRowCode(r));
  const selected=coded.length?data.filter(r=>tefasRowCode(r)===code.toUpperCase()):data;
  const rows=selected
    .map(r=>({...r,price:parseNumber(r.FIYAT??r.fiyat??r.sonFiyat),time:tefasDate(r.TARIH??r.tarih??r.fiyatTarihi)}))
    .filter(r=>r.price>0&&Number.isFinite(r.time)).sort((a,b)=>b.time-a.time);
  const r=rows[0];
  if(!r)throw new SourceError('TEFAS bu fon kodu için güncel fiyat döndürmedi. Eski fiyat korundu.');
  const priceDate=new Intl.DateTimeFormat('en-CA',{timeZone:'Europe/Istanbul',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(r.time));
  return {bid:r.price,ask:r.price,priceDate,source:'TEFAS',sourceUrl:'https://www.tefas.gov.tr/tr/fon-detayli-analiz/'+code,status:'received',fetchedAt:new Date().toISOString()};
}
export function parseTefasPage(html,code){
  const raw=String(html??'').replace(/&quot;|&#34;/gi,'"').replace(/\\"/g,'"');
  const embedded=[];
  const escapedCode=code.replace(/[.*+?^${}()|[\]\\]/g,'\\$&');
  for(const pattern of[
    new RegExp(`"(?:fonKodu|FONKODU|fonKod)"\\s*:\\s*"${escapedCode}"[\\s\\S]{0,1800}?"(?:fiyat|FIYAT|sonFiyat)"\\s*:\\s*"?([0-9][0-9.,]*)`, 'gi'),
    new RegExp(`"(?:fiyat|FIYAT|sonFiyat)"\\s*:\\s*"?([0-9][0-9.,]*)"?[\\s\\S]{0,1800}?"(?:fonKodu|FONKODU|fonKod)"\\s*:\\s*"${escapedCode}"`, 'gi')
  ])for(const m of raw.matchAll(pattern)){const price=parseNumber(m[1]);if(price>0)embedded.push(price);}
  if(embedded.length)return {bid:embedded[0],ask:embedded[0],priceDate:null,source:'TEFAS • resmi fon sayfası',sourceUrl:'https://www.tefas.gov.tr/tr/fon-detayli-analiz/'+code,status:'received',fetchedAt:new Date().toISOString()};
  const page=raw.replace(/<(script|style|svg)\b[^>]*>[\s\S]*?<\/\1>/gi,' ')
    .replace(/<\/?(?:div|span|p|li|dt|dd|h[1-6]|td|tr|br)\b[^>]*>/gi,'\n');
  const lines=page.split(/\r?\n/).map(clean).filter(Boolean);
  const wanted=normalize(code);
  const codeVerified=lines.some((line,i)=>{
    const current=normalize(line);
    return current==='fonkodu'+wanted||(current==='fonkodu'&&lines.slice(i+1,i+4).some(next=>normalize(next)===wanted));
  });
  if(!codeVerified)throw new SourceError('TEFAS sayfası istenen fonu doğrulayamadı. Eski fiyat korundu.');
  const label=lines.findIndex(line=>normalize(line).startsWith('sonfiyattl'));
  if(label<0)throw new SourceError('TEFAS sayfasında son fiyat alanı bulunamadı.');
  let price=NaN;
  for(let i=label;i<Math.min(lines.length,label+5)&&!(price>0);i++){
    for(const token of lines[i].match(/[0-9][0-9.,]*/g)||[]){const n=parseNumber(token);if(n>0){price=n;break;}}
  }
  if(!(price>0))throw new SourceError('TEFAS sayfasındaki son fiyat okunamadı.');
  return {bid:price,ask:price,priceDate:null,source:'TEFAS • resmi fon sayfası',sourceUrl:'https://www.tefas.gov.tr/tr/fon-detayli-analiz/'+code,status:'received',fetchedAt:new Date().toISOString()};
}
export async function fetchTefas(code,fontip='YAT',fetcher=nativeFetch){
  if(!/^[A-Z0-9]{2,6}$/.test(code)||!['YAT','EMK','BYF'].includes(fontip))throw new SourceError('Geçersiz fon kodu veya türü.');
  const end=today(),startDate=new Date(end+'T12:00:00+03:00');startDate.setDate(startDate.getDate()-12);
  const start=new Intl.DateTimeFormat('en-CA',{timeZone:'Europe/Istanbul',year:'numeric',month:'2-digit',day:'2-digit'}).format(startDate);
  const portal=`https://www.tefas.gov.tr/tr/fon-verileri?fundType=${fontip}&startDate=${start}&endDate=${end}`;
  let apiError;
  try{
    const loadTable=async()=>{
      // Yeni Next.js portalının resmi JSON uç noktası. Önce portal açılarak aynı
      // telefon oturumunun çerezleri alınır; sonra sayfalar en fazla 1000'er kayıttır.
      await getText(portal,{method:'GET',headers:{Accept:'text/html,application/xhtml+xml,*/*;q=0.8'}},fetcher);
      const rows=[];
      for(let page=1;page<=5;page++){
        const basSira=(page-1)*1000+1;
        const body=JSON.stringify({fonTipi:fontip,fonKodu:null,aramaMetni:null,fonTurKod:null,fonGrubu:null,sfonTurKod:null,basTarih:start.replace(/-/g,''),bitTarih:end.replace(/-/g,''),basSira,bitSira:basSira+999,fonTurAciklama:null,dil:'TR',kurucuKod:null});
        const raw=await getText('https://www.tefas.gov.tr/api/funds/fonGnlBlgSiraliGetir',{method:'POST',body,headers:{Accept:'application/json','Content-Type':'application/json',Referer:portal,Origin:'https://www.tefas.gov.tr'}},fetcher);
        let json;try{json=JSON.parse(raw);}catch{throw new SourceError('TEFAS geçerli JSON döndürmedi.');}
        if(json?.errorCode||json?.errorMessage)throw new SourceError('TEFAS fiyat tablosunu döndürmedi. Eski fiyat korundu.');
        const pageRows=tefasRows(json);
        if(!Array.isArray(pageRows))throw new SourceError('TEFAS beklenen fiyat tablosunu döndürmedi.');
        rows.push(...pageRows);
        const total=Number(json?.toplamSayfa??json?.totalPages??1);
        if(pageRows.length<1000||page>=total)break;
      }
      return rows;
    };
    const table=fetcher===nativeFetch?await cached('tefas-table:'+fontip+':'+end,300000,loadTable):await loadTable();
    const exact=table.filter(row=>tefasRowCode(row)===code);
    if(!exact.length)throw new SourceError('TEFAS bu fon kodu için güncel fiyat döndürmedi. Eski fiyat korundu.');
    return parseTefas(exact,code);
  }catch(e){apiError=e;}
  try{return parseTefasPage(await getText('https://www.tefas.gov.tr/tr/fon-detayli-analiz/'+code,{method:'GET'},fetcher),code);}
  catch{throw new SourceError(apiError?.message||'TEFAS fiyatı alınamadı. Eski fiyat korundu.',apiError?.code||'SOURCE_ERROR');}
}
function attributes(s){const out={};for(const m of s.matchAll(/([\w-]+)\s*=\s*["']([^"']*)["']/g))out[m[1].toLowerCase()]=m[2];return out;}
export function socketValue(html,key,attr){
  for(const m of html.matchAll(/<([a-z0-9]+)\b([^>]*data-socket-[^>]*)>([\s\S]*?)<\/\1>/gi)){
    const a=attributes(m[2]);
    if(a['data-socket-key']===key&&a['data-socket-attr']===attr){const n=parseNumber(clean(m[3]));if(n>0)return n;}
  }
  return null;
}
const slugs={akbank:'akbank','albaraka-turk':'albaraka','alternatif-bank':'alternatifbank',anadolubank:'anadolubank',denizbank:'denizbank','dunya-katilim':'dunyakatilim','emlak-katilim':'emlakkatilim',enpara:'enpara',fibabanka:'fibabanka','garanti-bbva':'garanti','garanti-bankasi':'garanti',halkbank:'halkbank',hsbc:'hsbc',ing:'ing','ing-bank':'ing',isbankasi:'isbankasi','is-bankasi':'isbankasi','turkiye-is-bankasi':'isbankasi','kuveyt-turk':'kuveytturk',odeabank:'odeabank',qnb:'qnb','qnb-finansbank':'qnb',sekerbank:'sekerbank',teb:'teb',hadi:'tom','tom-bank':'tom','turkiye-finans':'turkiyefinans',vakifbank:'vakifbank','vakif-katilim':'vakifkatilim',yapikredi:'yapikredi','yapi-kredi':'yapikredi','ziraat-bankasi':'ziraat','ziraat-katilim':'ziraatkatilim'};
const bankDetailSlugs={akbank:'akbank',albaraka:'albaraka-turk',alternatifbank:'alternatif-bank',anadolubank:'anadolubank',denizbank:'denizbank',dunyakatilim:'dunya-katilim',emlakkatilim:'emlak-katilim',enpara:'enpara',fibabanka:'fibabanka',garanti:'garanti-bbva',halkbank:'halkbank',hsbc:'hsbc',ing:'ing',isbankasi:'isbankasi',kuveytturk:'kuveyt-turk',odeabank:'odeabank',qnb:'qnb',sekerbank:'sekerbank',teb:'teb',tom:'hadi',turkiyefinans:'turkiye-finans',vakifbank:'vakifbank',vakifkatilim:'vakif-katilim',yapikredi:'yapi-kredi',ziraat:'ziraat-bankasi',ziraatkatilim:'ziraat-katilim'};
const bankAliases={akbank:['akbank'],albaraka:['albaraka turk','albaraka'],alternatifbank:['alternatif bank','alternatifbank'],anadolubank:['anadolubank'],denizbank:['denizbank'],dunyakatilim:['dunya katilim'],emlakkatilim:['emlak katilim'],enpara:['enpara'],fibabanka:['fibabanka'],garanti:['garanti bbva','garanti'],halkbank:['halkbank'],hsbc:['hsbc'],ing:['ing bank','ing'],isbankasi:['turkiye is bankasi','is bankasi'],kuveytturk:['kuveyt turk'],odeabank:['odeabank'],qnb:['qnb finansbank','qnb'],sekerbank:['sekerbank'],teb:['turk ekonomi bankasi','teb'],tom:['tom bank','hadi'],turkiyefinans:['turkiye finans'],vakifbank:['vakifbank'],vakifkatilim:['vakif katilim'],yapikredi:['yapi kredi'],ziraat:['ziraat bankasi'],ziraatkatilim:['ziraat katilim']};
const bankFromRow=row=>{
  const text=normalize(clean(row));
  for(const [bank,aliases] of Object.entries(bankAliases))if(aliases.some(alias=>text.includes(normalize(alias))))return bank;
  return null;
};
const bankSourceUrl=(source,metal)=>{
  if(source==='Altın.app')return 'https://altin.app/';
  if(source==='CanlıAltın')return 'https://canlialtinfiyatlari.com/altin/banka';
  if(source.startsWith('m.'))return 'https://m.doviz.com/altin/'+(metal==='gold'?'banka':'gumus');
  return 'https://altin.doviz.com/'+(metal==='gold'?'gram-altin':'gumus');
};
export function parseBankPage(html,metal,source='Döviz.com'){
  const suffix=metal==='gold'?'gram-altin':'gumus',prefix=metal==='gold'?'banka_altin_':'banka_gumus_';
  const quotes={};
  const add=(bank,key,row,allowCells=true)=>{
    let bid=key?socketValue(row,key,'bid'):null,ask=key?socketValue(row,key,'ask'):null;
    if(allowCells&&!(bid>0&&ask>0)){
      const values=[...row.matchAll(/<td\b[^>]*>([\s\S]*?)<\/td>/gi)].flatMap(m=>(clean(m[1]).match(/[0-9][0-9.,]*/g)||[]).map(parseNumber)).filter(n=>n>(metal==='gold'?100:5));
      [bid,ask]=values;
    }
    if(!(bid>0))return;
    if(ask>0&&ask<bid)return;
    if(!(ask>0))ask=null;
    quotes[prefix+bank]={bid,ask,status:'received',source:source+' • banka gösterge',sourceUrl:bankSourceUrl(source,metal),fetchedAt:new Date().toISOString(),priceDate:null};
  };
  // Sadece aynı tablo satırındaki banka bağlantısı ve etiketli alış/satış hücreleri eşleştirilir.
  for(const tr of html.matchAll(/<tr\b[^>]*>([\s\S]*?)<\/tr>/gi)){
    const row=tr[1];
    for(const link of row.matchAll(/href=["'](?:https:\/\/altin\.doviz\.com)?\/([^/"']+)\/(gram-altin|gumus)["']/gi)){
      const bank=slugs[link[1]];if(!bank||link[2]!==suffix)continue;
      const key=row.match(/data-socket-key=["']([^"']+)["']/)?.[1];
      add(bank,key&&key.endsWith('-'+suffix)?key:null,row);
    }
    if(!row.match(/href=["']/i)){
      const bank=bankFromRow(row);if(bank)add(bank,null,row);
    }
  }
  // Mobil sayfa bağlantıları farklı olsa da banka adı ve iki fiyat aynı tablo satırında kalır.
  for(const tr of html.matchAll(/<tr\b[^>]*>([\s\S]*?)<\/tr>/gi)){
    const linkedMetal=tr[1].match(/href=["'][^"']*\/(gram-altin|gumus)(?:[/?"'])/i)?.[1];
    const bank=bankFromRow(tr[1]);if(bank&&(!linkedMetal||linkedMetal===suffix)&&!quotes[prefix+bank])add(bank,null,tr[1]);
  }
  // Eski dosyada tanımlı, kimliği bilinen kaynak anahtarları.
  for(const [bank,id] of Object.entries({akbank:'1',vakifbank:'5',ziraat:'7'}))if(!quotes[prefix+bank])add(bank,id+'-'+suffix,html,false);
  return quotes;
}
function pageText(html){
  const raw=String(html??''),meta=[];
  for(const m of raw.matchAll(/<meta\b([^>]*)>/gi)){const a=attributes(m[1]);if(a.content)meta.push(a.content);}
  const visible=raw.replace(/<(script|style|svg)\b[^>]*>[\s\S]*?<\/\1>/gi,' ')
    .replace(/<br\s*\/?>|<\/(?:div|p|li|h[1-6]|td|tr)>/gi,'\n');
  return (meta.join('\n')+'\n'+clean(visible)).replace(/&quot;|&#34;/gi,'"').replace(/&apos;|&#39;/gi,"'");
}
export function parseBankDetailPage(html,metal,bank){
  const slug=bankDetailSlugs[bank],suffix=metal==='gold'?'gram-altin':'gumus',prefix=metal==='gold'?'banka_altin_':'banka_gumus_';
  if(!slug||!BANKS[bank])throw new SourceError('Banka ayrıntı kaynağı tanımlı değil.');
  const text=pageText(html),normalized=normalize(text),aliases=bankAliases[bank]||[BANKS[bank]];
  if(!aliases.some(name=>normalized.includes(normalize(name))))throw new SourceError('Banka ayrıntı sayfası doğru bankayı doğrulamadı.');
  const pair=text.match(/Al[ıi]ş\s*\/\s*Sat[ıi]ş\s*([0-9][0-9.,]*)\s*\/\s*([0-9][0-9.,]*)/i)
    ||text.match(/Al[ıi]ş[^0-9]{0,50}([0-9][0-9.,]*)[\s\S]{0,180}?Sat[ıi]ş[^0-9]{0,50}([0-9][0-9.,]*)/i);
  const bid=parseNumber(pair?.[1]),ask=parseNumber(pair?.[2]);
  if(!(bid>(metal==='gold'?100:5))||!(ask>=bid))throw new SourceError('Banka ayrıntı sayfasındaki alış/satış fiyatı doğrulanamadı.');
  return {[prefix+bank]:{bid,ask,status:'received',source:'Döviz.com • '+BANKS[bank],sourceUrl:'https://altin.doviz.com/'+slug+'/'+suffix,fetchedAt:new Date().toISOString(),priceDate:null}};
}
async function fetchBankQuotes(metal,wantedBanks=[],fetcher=nativeFetch){
  const suffix=metal==='gold'?'gram-altin':'gumus';
  const mobile=metal==='gold'?'https://m.doviz.com/altin/banka':'https://m.doviz.com/altin/gumus';
  // Döviz.com bazı Android bağlantılarını dönemsel olarak 403 ile engelleyebiliyor.
  // Gram altında önce sunucu tarafında tablo döndüren kaynaklar denenir; mevcut
  // Döviz.com ve banka ayrıntı sayfaları yedek olarak korunur. Gümüş akışı değişmez.
  const pages=metal==='gold'?
    [['https://altin.app/','Altın.app'],['https://canlialtinfiyatlari.com/altin/banka','CanlıAltın'],['https://altin.doviz.com/'+suffix,'Döviz.com'],[mobile,'m.doviz.com']]:
    [['https://altin.doviz.com/'+suffix,'Döviz.com'],[mobile,'m.doviz.com']];
  let lastError,merged={};
  for(const [url,source] of pages){
    try{
      const quotes=parseBankPage(await getText(url,{headers:{Referer:source==='m.doviz.com'?'https://m.doviz.com/':'https://altin.doviz.com/'}},fetcher),metal,source);
      for(const [key,value] of Object.entries(quotes))if(!merged[key])merged[key]=value;
    }
    catch(e){lastError=e;}
  }
  const missing=[...new Set(wantedBanks)].filter(bank=>bankDetailSlugs[bank]&&!merged[(metal==='gold'?'banka_altin_':'banka_gumus_')+bank]);
  await pool(missing.map(bank=>async()=>{
    const url='https://altin.doviz.com/'+bankDetailSlugs[bank]+'/'+suffix;
    try{Object.assign(merged,parseBankDetailPage(await getText(url,{headers:{Referer:'https://altin.doviz.com/'}},fetcher),metal,bank));}
    catch(e){lastError=e;}
  }),3);
  if(Object.keys(merged).length)return merged;
  if(lastError)throw lastError;
  throw new SourceError('Banka fiyat tablosu kaynak sayfasında bulunamadı.');
}
export function parseGeneral(raw){
  const j=JSON.parse(raw),lookup=Object.fromEntries(Object.entries(j).map(([k,v])=>[normalize(k),v]));
  const sourceTime=String(j.Update_Date||j['Güncelleme Tarihi']||'');
  const sourceDay=sourceTime.match(/^(\d{4})-(\d{2})-(\d{2})(?:\s|T|$)/)?.slice(1).join('-')||null;
  const map={gram_altin:['gram-altin'],gram_gumus:['gumus'],ceyrek_altin:['ceyrek-altin'],yarim_altin:['yarim-altin'],tam_altin:['tam-altin'],cumhuriyet_altin:['cumhuriyet-altini'],resat_altin:['resat-altin','resat-altini'],bilezik_22:['22-ayar-bilezik'],bilezik_24:['24-ayar-bilezik'],altin_18:['18-ayar-altin'],euro:['EUR'],dolar:['USD']};
  const out={};
  for(const [key,candidates] of Object.entries(map)){
    const item=candidates.map(x=>lookup[normalize(x)]).find(Boolean);if(!item)continue;
    const bid=parseNumber(item['Alış']??item['Alis']??item.buying),ask=parseNumber(item['Satış']??item['Satis']??item.selling);
    if(!(bid>0))continue;
    out[key]={bid,ask:ask>=bid?ask:null,status:'received',source:'Truncgil • gösterge',sourceUrl:'https://finans.truncgil.com',fetchedAt:new Date().toISOString(),priceDate:sourceDay,sourceTime};
  }
  return out;
}
const dovizPhysicalSlugs={gram_altin:'gram-altin',gram_gumus:'gumus',ceyrek_altin:'ceyrek-altin',yarim_altin:'yarim-altin',tam_altin:'tam-altin',cumhuriyet_altin:'cumhuriyet-altini',resat_altin:'resat-altini',bilezik_22:'22-ayar-bilezik',altin_18:'18-ayar-altin'};
export function parseDovizGeneralPage(html,key,slug=dovizPhysicalSlugs[key]){
  if(!slug||!dovizPhysicalSlugs[key])throw new SourceError('Bu fiziki ürün için Döviz.com yedek sayfası tanımlı değil.');
  let bid=socketValue(html,slug,'bid')||socketValue(html,slug,'buying'),ask=socketValue(html,slug,'ask')||socketValue(html,slug,'selling');
  if(!(bid>0)){
    const plain=clean(String(html??'').replace(/<(script|style|svg)\b[^>]*>[\s\S]*?<\/\1>/gi,' ').replace(/<br\s*\/?\s*>|<\/p>|<\/div>|<\/li>/gi,'\n'));
    const pair=plain.match(/Al[ıi]ş\s*\/\s*Sat[ıi]ş\s*([0-9][0-9.,]*)\s*\/\s*([0-9][0-9.,]*)/i)
      ||plain.match(/Al[ıi]ş[^0-9]{0,40}([0-9][0-9.,]*)[\s\S]{0,160}?Sat[ıi]ş[^0-9]{0,40}([0-9][0-9.,]*)/i);
    if(pair){bid=parseNumber(pair[1]);ask=parseNumber(pair[2]);}
  }
  if(!(bid>0)||ask>0&&ask<bid)throw new SourceError('Döviz.com fiziki ürün fiyatını doğrulayamadı.');
  return {[key]:{bid,ask:ask>0?ask:null,status:'received',source:'Döviz.com • fiziki gösterge',sourceUrl:'https://altin.doviz.com/'+slug,fetchedAt:new Date().toISOString(),priceDate:null}};
}
async function fetchGeneral(keys,fetcher=nativeFetch){
  let merged={},lastError;
  try{merged=parseGeneral(await getText('https://finans.truncgil.com/today.json',{},fetcher));}catch(e){lastError=e;}
  for(const key of keys.filter(k=>!merged[k]&&dovizPhysicalSlugs[k])){
    const slug=dovizPhysicalSlugs[key];
    try{Object.assign(merged,parseDovizGeneralPage(await getText('https://altin.doviz.com/'+slug,{headers:{Referer:'https://altin.doviz.com/'}},fetcher),key,slug));}
    catch(e){lastError=e;}
  }
  if(Object.keys(merged).length)return merged;
  if(lastError)throw lastError;
  throw new SourceError('Altın / döviz fiyatı kaynaklarda bulunamadı.');
}
export async function fetchStock(code,fetcher=nativeFetch){
  const symbol=code.toUpperCase().replace(/\.IS$/,'');
  if(!/^[A-Z0-9]{2,12}$/.test(symbol))throw new SourceError('Geçersiz hisse sembolü.');
  const raw=await getText('https://query1.finance.yahoo.com/v8/finance/chart/'+symbol+'.IS?interval=1d&range=5d',{},fetcher);
  let j;try{j=JSON.parse(raw);}catch{throw new SourceError('Hisse kaynağı geçerli JSON döndürmedi.');}
  const m=j.chart?.result?.[0]?.meta;
  if(!(m?.regularMarketPrice>0)||m.currency!=='TRY'||String(m.symbol).toUpperCase()!==symbol+'.IS')throw new SourceError('Doğru sembol için TL fiyatı bulunamadı.');
  return {bid:m.regularMarketPrice,ask:null,source:'Yahoo Finance • gecikmeli olabilir',sourceUrl:'https://finance.yahoo.com/quote/'+symbol+'.IS/',status:'received',priceDate:m.regularMarketTime?new Date(m.regularMarketTime*1000).toISOString():null,fetchedAt:new Date().toISOString()};
}
export function parseAltins1Page(html){
  const raw=String(html??'');
  let price=socketValue(raw,'ALTINS1','s')||socketValue(raw,'ALTINS1','bid')||socketValue(raw,'ALTINS1','last')||socketValue(raw,'ALTINS1','price');
  if(!(price>0)){
    const plain=pageText(raw);
    const m=plain.match(/ALTINS1\s+hissesinin\s+fiyat[ıi]\s+([0-9]{1,3}(?:[.,][0-9]{2,4})?)\s+lirad[ıi]r/i)
      ||plain.match(/ALTINS1[\s\S]{0,700}?(?:Son\s*\([^)]*\)|Güncel Fiyat|Son Fiyat)[^0-9]{0,40}([0-9]{1,3}(?:[.,][0-9]{2,4})?)/i)
      ||raw.replace(/\\"/g,'"').match(/(?:"(?:symbol|code|sembol)"\s*:\s*"ALTINS1"[\s\S]{0,900}?"(?:last|price|currentPrice|close)"\s*:\s*"?)([0-9]{1,3}(?:[.,][0-9]{2,4})?)/i);
    if(m)price=parseNumber(m[1]);
  }
  if(!(price>1&&price<5000))throw new SourceError('ALTINS1 fiyatı kaynak sayfasında doğrulanamadı.');
  return {bid:price,ask:null,source:'Borsa.Döviz • ALTINS1',sourceUrl:'https://borsa.doviz.com/hisseler/altins1-darphane-altin-sertifikasi',status:'received',priceDate:null,fetchedAt:new Date().toISOString()};
}
export function parseNetdaniaAltins1(html){
  const lines=pageText(html).split(/\r?\n/).map(clean).filter(Boolean);
  const index=lines.findIndex(line=>normalize(line)==='darphanealtinsertifikasi');
  if(index<0||!lines.some(line=>line.includes('TRXDRP012213')))throw new SourceError('ALTINS1 yedek kaynağında ürün doğrulanamadı.');
  let price=NaN;
  for(let i=index+1;i<Math.min(lines.length,index+7);i++)if(/^\d{1,3}(?:[.,]\d{2,4})$/.test(lines[i])){price=parseNumber(lines[i]);break;}
  if(!(price>1&&price<5000))throw new SourceError('ALTINS1 yedek kaynağındaki fiyat okunamadı.');
  return {bid:price,ask:null,source:'NetDania • Borsa İstanbul gecikmeli',sourceUrl:'https://m.netdania.com/equities/altin-s1-ib/idc-dla',status:'received',priceDate:null,fetchedAt:new Date().toISOString()};
}
export async function fetchAltins1(fetcher=nativeFetch){
  let lastError;
  for(const [url,parser] of[
    ['https://borsa.doviz.com/hisseler/altins1-darphane-altin-sertifikasi',parseAltins1Page],
    ['https://borsa.doviz.com/hisseler',parseAltins1Page],
    ['https://m.netdania.com/equities/altin-s1-ib/idc-dla',parseNetdaniaAltins1]
  ])try{return parser(await getText(url,{headers:{Referer:new URL(url).origin+'/'}},fetcher));}catch(e){lastError=e;}
  try{return {...await fetchStock('ALTIN',fetcher),source:'Yahoo Finance • ALTIN.IS',sourceUrl:'https://finance.yahoo.com/quote/ALTIN.IS/'};}
  catch(e){lastError=e;}
  throw new SourceError(lastError?.message||'ALTINS1 fiyatı alınamadı. Eski fiyat korundu.',lastError?.code||'SOURCE_ERROR');
}
async function pool(jobs,n=3){let next=0;await Promise.all(Array.from({length:Math.min(n,jobs.length)},async()=>{while(next<jobs.length){const i=next++;await jobs[i]();}}));}
export async function refreshPrices(assets=[],banks=false){
  if(!Array.isArray(assets)||assets.length>200)throw new SourceError('En fazla 200 varlık güncellenebilir.');
  const quotes={},errors={},sources=[];
  const wanted=new Map();
  for(const a of assets){
    if(!TYPES[a.tip])throw new SourceError('Geçersiz varlık türü.');
    if(['try_lirasi','vadeli_mevduat'].includes(a.tip))continue;
    if(TYPES[a.tip].symbol&&!/^[A-Z0-9.]{2,16}$/.test(a.sembol||''))throw new SourceError('Geçersiz sembol.');
    if(TYPES[a.tip].bank&&!BANKS[a.banka])throw new SourceError('Geçersiz banka.');
    wanted.set(quoteKey(a),a);
  }
  const jobs=[];
  const attempt=async(label,keys,job)=>{try{const data=await job();Object.assign(quotes,data);for(const key of keys)if(!data[key])errors[key]='Kaynakta bu ürünün doğrulanabilir fiyatı bulunamadı. Elle girilebilir.';sources.push({name:label,ok:Object.keys(data).length>0,count:Object.keys(data).length});}catch(e){for(const key of keys)errors[key]=e.message;sources.push({name:label,ok:false,message:e.message,code:e.code});}};
  const general=[...wanted].filter(([,a])=>!['hisse','tefas_fon','altins1','banka_altin','banka_gumus'].includes(a.tip)).map(([k])=>k);
  if(general.length)jobs.push(()=>attempt('Altın / döviz',general,()=>cached('general:'+general.sort().join(','),60000,()=>fetchGeneral(general))));
  for(const metal of ['gold','silver']){
    const type=metal==='gold'?'banka_altin':'banka_gumus';
    const keys=banks?Object.keys(BANKS).filter(b=>b!=='diger').map(b=>type+'_'+b):[...wanted].filter(([,a])=>a.tip===type).map(([k])=>k);
    const heldBanks=[...wanted].filter(([,a])=>a.tip===type).map(([,a])=>a.banka);
    if(keys.length)jobs.push(()=>attempt(metal==='gold'?'Banka altın':'Banka gümüş',keys,()=>cached(metal+':'+heldBanks.sort().join(','),60000,()=>fetchBankQuotes(metal,heldBanks))));
  }
  for(const [key,a] of wanted){
    if(a.tip==='tefas_fon')jobs.push(()=>attempt('TEFAS '+a.sembol,[key],async()=>({[key]:await cached(key+':'+(a.fonTip||'YAT'),300000,()=>fetchTefas(a.sembol,a.fonTip||'YAT'))})));
    if(a.tip==='hisse')jobs.push(()=>attempt('Hisse '+a.sembol,[key],async()=>({[key]:await cached(key,60000,()=>fetchStock(a.sembol))})));
    if(a.tip==='altins1')jobs.push(()=>attempt('ALTINS1',[key],async()=>({[key]:await cached('altins1',60000,()=>fetchAltins1())})));
  }
  await pool(jobs);
  return {quotes,errors,sources,checkedAt:new Date().toISOString()};
}
