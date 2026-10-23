export const VERSION = 4;
export const STORAGE = 'portfoy_v4';
export const COLORS = ['#3263da','#00a28b','#ca8c28','#9063d5','#dc6489','#438ca8'];
export const TYPES = {
  hisse:{name:'Hisse senedi',unit:'adet',symbol:true,group:'Hisseler'},
  tefas_fon:{name:'TEFAS fonu',unit:'pay',symbol:true,group:'Fonlar'},
  vadeli_mevduat:{name:'Vadeli mevduat',unit:'TL',bank:true,group:'Mevduat'},
  banka_altin:{name:'Banka gram altın',unit:'gr',bank:true,group:'Altın'},
  banka_gumus:{name:'Banka gram gümüş',unit:'gr',bank:true,group:'Gümüş'},
  gram_altin:{name:'24 ayar gram altın',unit:'gr',group:'Altın'},
  gram_altin_22:{name:'22 ayar gram altın',unit:'gr',group:'Altın'},
  gram_gumus:{name:'Gram gümüş',unit:'gr',group:'Gümüş'},
  ceyrek_altin:{name:'Çeyrek altın',unit:'adet',group:'Altın'},
  yarim_altin:{name:'Yarım altın',unit:'adet',group:'Altın'},
  tam_altin:{name:'Tam altın',unit:'adet',group:'Altın'},
  cumhuriyet_altin:{name:'Cumhuriyet altını',unit:'adet',group:'Altın'},
  resat_altin:{name:'Reşat altını',unit:'adet',group:'Altın'},
  bilezik_22:{name:'22 ayar bilezik',unit:'gr',group:'Altın'},
  bilezik_24:{name:'24 ayar bilezik',unit:'gr',group:'Altın'},
  altin_18:{name:'18 ayar altın',unit:'gr',group:'Altın'},
  altins1:{name:'ALTINS1 sertifika',unit:'adet',group:'Altın'},
  euro:{name:'Euro',unit:'EUR',group:'Döviz'},
  dolar:{name:'ABD doları',unit:'USD',group:'Döviz'},
  try_lirasi:{name:'Türk lirası',unit:'TL',group:'Nakit'}
};
// Kur yayını her bankada / her üründe bulunmayabilir. Olmayan kur türetilmez.
export const BANKS = {
  akbank:'Akbank', albaraka:'Albaraka Türk', alternatifbank:'Alternatif Bank', anadolubank:'Anadolubank',
  denizbank:'DenizBank', dunyakatilim:'Dünya Katılım', emlakkatilim:'Emlak Katılım', enpara:'Enpara',
  fibabanka:'Fibabanka', garanti:'Garanti BBVA', halkbank:'Halkbank', hsbc:'HSBC', ing:'ING',
  isbankasi:'İş Bankası', kuveytturk:'Kuveyt Türk', odeabank:'Odeabank', qnb:'QNB', sekerbank:'Şekerbank',
  teb:'TEB', tom:'TOM Bank Hadi', turkiyefinans:'Türkiye Finans', vakifbank:'VakıfBank',
  vakifkatilim:'Vakıf Katılım', yapikredi:'Yapı Kredi', ziraat:'Ziraat Bankası', ziraatkatilim:'Ziraat Katılım', diger:'Diğer banka'
};
export const uid = () => globalThis.crypto.randomUUID();
export const today = () => new Intl.DateTimeFormat('en-CA',{timeZone:'Europe/Istanbul',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
export const money = n => n == null ? '—' : new Intl.NumberFormat('tr-TR',{style:'currency',currency:'TRY',maximumFractionDigits:2}).format(n);
export const num = (n,d=4) => n == null ? '—' : new Intl.NumberFormat('tr-TR',{maximumFractionDigits:d}).format(n);
export const cents = n => Math.round((n + Number.EPSILON) * 100);
export const round = n => cents(n)/100;
export function parseNumber(input) {
  if (typeof input === 'number') return Number.isFinite(input) ? input : NaN;
  let s=String(input??'').trim().replace(/[\s₺]/g,'').replace(/TL$/i,'');
  if (!s) return NaN;
  if (s.includes(',')) {
    if (!/^[+-]?(?:\d+|\d{1,3}(?:\.\d{3})+),\d+$/.test(s)) return NaN;
    s=s.replace(/\./g,'').replace(',','.');
  }
  if (!/^[+-]?(?:\d+(?:\.\d+)?|\.\d+)$/.test(s)) return NaN;
  return Number(s);
}
export function amount(input,label='Tutar',zero=false) {
  const n=parseNumber(input);
  if(!Number.isFinite(n)||n<0||(!zero&&n===0)||n>1e12) throw new Error(label+' geçerli, '+(zero?'negatif olmayan':'sıfırdan büyük')+' bir sayı olmalı.');
  return n;
}
export function percentage(input,label='Oran') {
  const n=amount(input,label,true);
  if(n>100)throw new Error(label+' 0 ile 100 arasında olmalı.');
  return Math.round(n*10000)/10000;
}
export function validDay(d) {
  return /^\d{4}-\d{2}-\d{2}$/.test(d) && Number.isFinite(Date.parse(d)) && new Date(d).toISOString().slice(0,10)===d;
}
export function emptyState() {
  return {schemaVersion:VERSION,portfoyler:[{id:'p1',ad:'Kendi Portföyüm',renk:COLORS[0]},{id:'p2',ad:'Çocukların Portföyü',renk:COLORS[1]}],varliklar:[],kapaliPozisyonlar:[],quotes:{},snapshots:[],chartHistory:{portfolios:{},assets:{}},sonGuncelleme:null};
}
export function assetName(a) {return a.ad || a.sembol || TYPES[a.tip]?.name || a.tip;}
export function quoteKey(a) {
  if(a.tip==='hisse')return 'hisse_'+a.sembol.toUpperCase().replace(/\.IS$/,'');
  if(a.tip==='tefas_fon')return 'tefas_'+a.sembol.toUpperCase();
  if(a.tip==='banka_altin'||a.tip==='banka_gumus')return a.tip+'_'+a.banka;
  return a.tip;
}
export function depositSummary(a) {
  let principal=0,interest=0,withdrawals=0;
  for(const t of a.islemler||[]) {
    if(t.tur==='getiri')interest+=cents(t.tutar);
    else if(t.tur==='cekme')withdrawals+=cents(t.tutar);
    else if(t.tur==='yatirma')principal+=cents(t.tutar);
  }
  return {principal:principal/100,interest:interest/100,withdrawals:withdrawals/100,balance:(principal+interest-withdrawals)/100};
}
export function holdingSummary(a) {
  const soldQuantity=a.satilanMiktar||0,soldCost=a.satilanMaliyet;
  if(!a.islemler?.length) {
    const quantity=(a.miktar||0)-soldQuantity;
    const originalCost=a.maliyetBirim>0?round((a.miktar||0)*a.maliyetBirim+(a.alisKomisyon||0)):null;
    const cost=originalCost==null||(soldQuantity>0&&soldCost==null)?null:round(originalCost-(soldCost||0));
    return {quantity,cost,dividend:0,average:quantity>0&&cost!=null?cost/quantity:a.maliyetBirim||0};
  }
  let quantity=0,gross=0,fees=0,dividend=0,known=true;
  for(const t of a.islemler) {
    if(t.tur==='temettu') {dividend+=cents(t.tutar);continue;}
    if(t.tur!=='alis') continue;
    quantity+=t.miktar; gross+=t.miktar*(t.fiyat||0); fees+=t.komisyon||0;
    if(!(t.fiyat>0))known=false;
  }
  quantity-=soldQuantity;
  if(soldQuantity>0&&soldCost==null)known=false;
  const effective=round(gross+fees-dividend/100-(soldCost||0));
  const cost=a.maliyetManuel?round(quantity*a.maliyetBirim):known?effective:null;
  return {quantity,cost,dividend:dividend/100,average:quantity&&cost!=null?cost/quantity:null};
}
function closedCost(a) {
  if(Number.isFinite(a?.maliyetToplam)&&a.maliyetToplam>=0)return round(a.maliyetToplam);
  return Number.isFinite(a?.miktar)&&a.miktar>0&&Number.isFinite(a?.maliyetBirim)&&a.maliyetBirim>0?round(a.miktar*a.maliyetBirim):null;
}
function closedNetPnl(a) {
  return Number.isFinite(a?.netKz)?round(a.netKz):Number.isFinite(a?.kz)?round(a.kz):null;
}
function closedGrossPnl(a) {
  return Number.isFinite(a?.brutKz)?round(a.brutKz):Number.isFinite(a?.stopajsizKz)?round(a.stopajsizKz):closedNetPnl(a);
}
function datedHoldingSummary(a,date) {
  if(!a.islemler?.length)return holdingSummary(a);
  let quantity=0,gross=0,fees=0,dividend=0,known=true;
  for(const t of a.islemler) {
    if(String(t.tarih||'').slice(0,10)>date)continue;
    if(t.tur==='temettu'){dividend+=cents(t.tutar);continue;}
    if(t.tur!=='alis')continue;
    quantity+=t.miktar;gross+=t.miktar*(t.fiyat||0);fees+=t.komisyon||0;
    if(!(t.fiyat>0))known=false;
  }
  const soldQuantity=a.satilanMiktar||0,soldCost=a.satilanMaliyet;
  quantity-=soldQuantity;
  if(soldQuantity>0&&soldCost==null)known=false;
  const effective=round(gross+fees-dividend/100-(soldCost||0));
  const cost=a.maliyetManuel?round(quantity*(a.maliyetBirim||0)):known?effective:null;
  return {quantity,cost,dividend:dividend/100,average:quantity>0&&cost!=null?cost/quantity:null};
}
export function closingDateFloor(state,id) {
  const a=state.varliklar.find(x=>x.id===id);if(!a)return null;
  const purchases=(a.islemler||[]).filter(t=>t.tur==='alis'&&validDay(String(t.tarih||'').slice(0,10))).map(t=>t.tarih.slice(0,10)).sort();
  const closes=(state.kapaliPozisyonlar||[]).filter(r=>r.sourceAssetId===id&&validDay(String(r.kapanisTarihi||'').slice(0,10))).map(r=>r.kapanisTarihi.slice(0,10)).sort();
  const candidates=[purchases[0],closes[closes.length-1]].filter(Boolean).sort();
  return candidates[candidates.length-1]||null;
}
export function valuation(a,quotes) {
  if(a.tip==='vadeli_mevduat') {
    const s=depositSummary(a);
    const percent=s.principal>0?s.interest/s.principal*100:null;
    return {quantity:1,price:s.balance,value:s.balance,grossValue:s.balance,cost:s.principal-s.withdrawals,pnl:s.interest,grossPnl:s.interest,percent,grossPercent:percent,withholding:0,withholdingRate:0,withholdingEnabled:false,missing:false,quality:'manual',...s};
  }
  const s=holdingSummary(a),q=quotes[quoteKey(a)];
  const price=a.tip==='try_lirasi'?1:q?.bid>0?q.bid:null;
  const grossValue=price==null?null:round(s.quantity*price);
  const cost=a.tip==='try_lirasi'?grossValue:s.cost;
  const grossPnl=grossValue!=null&&cost!=null?round(grossValue-cost):null;
  const withholdingEnabled=a.tip==='tefas_fon'&&a.stopajVar===true;
  const withholdingRate=a.tip==='tefas_fon'?percentage(a.stopajOrani??17.5,'Stopaj oranı'):0;
  const withholding=withholdingEnabled?(grossPnl==null?null:round(Math.max(grossPnl,0)*withholdingRate/100)):0;
  const value=grossValue==null?null:withholding==null?grossValue:round(grossValue-withholding);
  const pnl=grossPnl==null?null:withholding==null?grossPnl:round(grossPnl-withholding);
  return {...s,cost,price,value,grossValue,pnl,grossPnl,percent:pnl!=null&&cost>0?pnl/cost*100:null,grossPercent:grossPnl!=null&&cost>0?grossPnl/cost*100:null,withholding,withholdingRate,withholdingEnabled,missing:price==null,quality:a.tip==='try_lirasi'?'fixed':quoteQuality(q),quote:q};
}
export function quoteQuality(q) {
  if(!q?.bid)return 'missing';
  if(q.status==='manual')return 'manual';
  if(q.status==='stale'||q.status==='legacy')return 'stale';
  if(q.status==='estimated')return 'estimated';
  if(!q.fetchedAt||!Number.isFinite(Date.parse(q.fetchedAt))||Date.now()-Date.parse(q.fetchedAt)>24*3600000)return 'stale';
  if(q.priceDate && q.priceDate.slice(0,10)<today())return 'dated';
  return 'received';
}
export function totals(state,pid='all') {
  const assets=state.varliklar.filter(a=>pid==='all'||a.portfoyId===pid);
  const closed=(state.kapaliPozisyonlar||[]).filter(a=>pid==='all'||a.portfoyId===pid);
  let value=0,grossValue=0,pnl=0,grossPnl=0,withholding=0,estimatedWithholding=0,realizedWithholding=0,matchedCost=0,openPnl=0,openGrossPnl=0,openMatchedCost=0,realizedPnl=0,realizedGrossPnl=0,realizedMatchedCost=0,realizedKnownCount=0,missing=0,unknownCost=0,deposit=0,interest=0,unverified=0;
  const groups={};
  for(const a of assets) {
    const v=valuation(a,state.quotes);
    if(v.missing) missing++;
    else {
      value+=cents(v.value);grossValue+=cents(v.grossValue);withholding+=cents(v.withholding||0);estimatedWithholding+=cents(v.withholding||0);const group=TYPES[a.tip].group;
      groups[group]=(groups[group]||0)+cents(v.value);
      if(v.pnl!=null){const net=cents(v.pnl),gross=cents(v.grossPnl),cost=a.tip==='try_lirasi'?0:cents(v.cost);pnl+=net;grossPnl+=gross;matchedCost+=cost;openPnl+=net;openGrossPnl+=gross;openMatchedCost+=cost;}
      else unknownCost++;
    }
    if(a.tip==='vadeli_mevduat'){deposit+=cents(v.value);interest+=cents(v.interest);}
    if(!['received','fixed','manual'].includes(v.quality)) unverified++;
  }
  for(const a of closed) {
    const net=closedNetPnl(a),gross=closedGrossPnl(a),cost=closedCost(a),tax=Number.isFinite(a.stopajTutari)?Math.max(0,a.stopajTutari):0;
    realizedWithholding+=cents(tax);withholding+=cents(tax);
    if(net==null){unknownCost++;continue;}
    const netCents=cents(net),grossCents=cents(gross??net);pnl+=netCents;grossPnl+=grossCents;realizedPnl+=netCents;realizedGrossPnl+=grossCents;realizedKnownCount++;
    if(cost!=null){const costCents=cents(cost);matchedCost+=costCents;realizedMatchedCost+=costCents;}else unknownCost++;
  }
  return {value:value/100,grossValue:grossValue/100,pnl:pnl/100,grossPnl:grossPnl/100,withholding:withholding/100,estimatedWithholding:estimatedWithholding/100,realizedWithholding:realizedWithholding/100,matchedCost:matchedCost/100,openPnl:openPnl/100,openGrossPnl:openGrossPnl/100,openMatchedCost:openMatchedCost/100,realizedPnl:realizedPnl/100,realizedGrossPnl:realizedGrossPnl/100,realizedMatchedCost:realizedMatchedCost/100,realizedKnownCount,percent:matchedCost>0?pnl/matchedCost*100:null,grossPercent:matchedCost>0?grossPnl/matchedCost*100:null,missing,unknownCost,unverified,deposit:deposit/100,interest:interest/100,groups:Object.fromEntries(Object.entries(groups).map(([k,v])=>[k,v/100])),count:assets.length};
}
const snapshotDay=value=>new Intl.DateTimeFormat('en-CA',{timeZone:'Europe/Istanbul',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(value));
export function recordSnapshot(state,timestamp=new Date().toISOString()) {
  const time=Date.parse(timestamp);
  if(!Number.isFinite(time))throw new Error('Değer geçmişi tarihi geçersiz.');
  const summaries=Object.fromEntries(state.portfoyler.map(p=>[p.id,totals(state,p.id)]));
  const byPortfolio=Object.fromEntries(Object.entries(summaries).map(([id,t])=>[id,t.value]));
  // Değer ile kazanç ayrı saklanır. Nakit yatırma/çekme kazanç değildir.
  const performance=Object.fromEntries(Object.entries(summaries).map(([id,t])=>[id,{pnl:t.pnl,complete:t.missing===0&&t.unknownCost===0,revision:state.performanceRevisions?.[id]||0}]));
  const snapshot={ts:new Date(time).toISOString(),day:snapshotDay(time),total:round(Object.values(byPortfolio).reduce((sum,value)=>sum+value,0)),byPortfolio,performance};
  const cutoff=time-370*86400000;
  const prior=(state.snapshots||[]).filter(s=>Number.isFinite(Date.parse(s?.ts))&&Date.parse(s.ts)>=cutoff&&(s.day||snapshotDay(s.ts))!==snapshot.day);
  return {...state,snapshots:[...prior,snapshot].sort((a,b)=>Date.parse(a.ts)-Date.parse(b.ts)).slice(-500)};
}
const CHART_HOUR=3600000;
const cleanChartSeries=(series,limit,label)=>{
  if(series==null)return [];
  if(!Array.isArray(series))throw new Error(label+' grafik geçmişi geçersiz.');
  const clean=series.map(point=>{
    const time=Date.parse(point?.ts),value=Number(point?.value);
    if(!Number.isFinite(time)||!Number.isFinite(value)||value<0||value>1e15)throw new Error(label+' grafik noktası geçersiz.');
    return {ts:new Date(time).toISOString(),value};
  }).sort((a,b)=>Date.parse(a.ts)-Date.parse(b.ts));
  return clean.slice(-limit);
};
const appendChartPoint=(series,point,limit)=>{
  const bucket=Math.floor(Date.parse(point.ts)/CHART_HOUR);
  return [...(series||[]).filter(p=>Math.floor(Date.parse(p.ts)/CHART_HOUR)!==bucket),point].sort((a,b)=>Date.parse(a.ts)-Date.parse(b.ts)).slice(-limit);
};
export function recordChartPoint(state,timestamp=new Date().toISOString()) {
  const time=Date.parse(timestamp);if(!Number.isFinite(time))throw new Error('Grafik tarihi geçersiz.');
  const ts=new Date(time).toISOString(),old=state.chartHistory||{},portfolios={},assets={};
  const portfolioLimit=Math.max(3,Math.min(800,Math.floor(5000/Math.max(1,state.portfoyler.length))));
  const assetLimit=Math.max(3,Math.min(800,Math.floor(30000/Math.max(1,state.varliklar.length))));
  for(const p of state.portfoyler){const t=totals(state,p.id);if(t.count&&t.missing===0)portfolios[p.id]=appendChartPoint(old.portfolios?.[p.id],{ts,value:round(t.value)},portfolioLimit);else if(old.portfolios?.[p.id])portfolios[p.id]=old.portfolios[p.id].slice(-portfolioLimit);}
  for(const a of state.varliklar){const v=valuation(a,state.quotes),balance=['vadeli_mevduat','try_lirasi'].includes(a.tip),value=balance?v.value:v.price;if(value!=null&&Number.isFinite(value)&&value>=0)assets[a.id]=appendChartPoint(old.assets?.[a.id],{ts,value:balance?round(value):Math.round(value*1e6)/1e6},assetLimit);else if(old.assets?.[a.id])assets[a.id]=old.assets[a.id].slice(-assetLimit);}
  return {...state,chartHistory:{portfolios,assets}};
}
const lastByDay=series=>{
  const days=new Map();for(const point of series)days.set(snapshotDay(point.ts),point);
  return [...days.values()].sort((a,b)=>Date.parse(a.ts)-Date.parse(b.ts));
};
const thinSeries=(series,max=64)=>{
  if(series.length<=max)return series;const result=[];
  for(let i=0;i<max;i++)result.push(series[Math.round(i*(series.length-1)/(max-1))]);
  return result.filter((point,i)=>i===0||point.ts!==result[i-1].ts);
};
export function chartSeries(state,scope,id) {
  let raw=scope==='portfolio'?state.chartHistory?.portfolios?.[id]:state.chartHistory?.assets?.[id];
  if(scope==='portfolio'){
    const merged=new Map();
    for(const point of [...(state.snapshots||[]).map(s=>({ts:s.ts,value:s.byPortfolio?.[id]})).filter(p=>Number.isFinite(p.value)&&Number.isFinite(Date.parse(p.ts))),...(raw||[])])merged.set(Math.floor(Date.parse(point.ts)/CHART_HOUR),point);
    raw=[...merged.values()];
  }
  const series=cleanChartSeries(raw,800,'Değer');
  if(!series.length)return {mode:'day',label:'Günlük',points:[],available:{day:true,week:false,month:false},change:null,percent:null};
  const lastTime=Date.parse(series[series.length-1].ts),span=lastTime-Date.parse(series[0].ts),week=span>=6*86400000,month=span>=27*86400000;
  const mode=month?'month':week?'week':'day',windowMs=mode==='month'?30*86400000:mode==='week'?7*86400000:86400000;
  let points=series.filter(p=>Date.parse(p.ts)>=lastTime-windowMs);
  if(points.length<2&&series.length>1)points=series.slice(-2);
  if(mode!=='day')points=lastByDay(points);points=thinSeries(points);
  const first=points[0]?.value,last=points[points.length-1]?.value,change=points.length>1?last-first:null;
  return {mode,label:mode==='month'?'Aylık':mode==='week'?'Haftalık':'Günlük',points,available:{day:true,week,month},change,percent:change!=null&&first>0?change/first*100:null};
}
export function periodPerformance(state,pid='all',days=1,now=Date.now()) {
  const current=totals(state,pid).value;
  const target=new Date(Number(now));
  if(!Number.isFinite(target.getTime())||!Number.isInteger(days)||days<1)return null;
  target.setUTCDate(target.getUTCDate()-days);
  const targetDay=snapshotDay(target);
  const prior=(state.snapshots||[]).filter(s=>{
    if(!Number.isFinite(Date.parse(s?.ts)))return false;
    const day=s.day||snapshotDay(s.ts),value=pid==='all'?s.total:s.byPortfolio?.[pid];
    return day===targetDay&&Date.parse(s.ts)<=Number(now)&&Number.isFinite(value)&&value>=0;
  }).sort((a,b)=>Date.parse(b.ts)-Date.parse(a.ts))[0];
  if(!prior)return null;
  const base=pid==='all'?prior.total:prior.byPortfolio[pid];
  const value=round(current-base);
  const currentSummary=totals(state,pid),scope=state.portfoyler.filter(p=>pid==='all'||p.id===pid);
  const comparable=scope.length>0&&currentSummary.missing===0&&currentSummary.unknownCost===0&&scope.every(p=>{
    const saved=prior.performance?.[p.id];
    return saved?.complete===true&&Number.isFinite(saved.pnl)&&saved.revision===(state.performanceRevisions?.[p.id]||0);
  })&&Object.keys(prior.byPortfolio||{}).filter(id=>pid==='all'||id===pid).length===scope.length;
  const profit=comparable?round(currentSummary.pnl-scope.reduce((sum,p)=>sum+prior.performance[p.id].pnl,0)):null;
  // value eski değer-değişimi API'sidir; arayüz K/Z için yalnız profit kullanır.
  return {value,percent:base>0?value/base*100:null,base,at:prior.ts,profit,profitPercent:profit!=null&&base>0?profit/base*100:null};
}
export function saveDepositEntry(a,entry) {
  if(a.tip!=='vadeli_mevduat')throw new Error('Vadeli mevduat hesabı seçilmeli.');
  if(!['getiri','yatirma','cekme'].includes(entry.tur))throw new Error('Geçersiz işlem.');
  if(!validDay(entry.tarih)||entry.tarih>today())throw new Error('Bugün veya geçmiş bir tarih seçin.');
  const t={...entry,id:entry.id||uid(),tutar:round(amount(entry.tutar))};
  if(t.tutar<=0)throw new Error('Tutar en az 0,01 TL olmalı.');
  if((a.islemler||[]).some(i=>i.tur==='getiri'&&t.tur==='getiri'&&i.tarih.slice(0,10)===t.tarih&&i.id!==t.id))throw new Error('Bu gün için faiz zaten girilmiş. Mevcut kaydı düzenleyin.');
  const list=(a.islemler||[]).filter(i=>i.id!==t.id).concat(t);
  validateDepositLedger(list);
  return {...a,islemler:list};
}
export function validateDepositLedger(list) {
  let bal=0; const days=new Set();
  const rank={yatirma:0,getiri:1,cekme:2};
  const sorted=[...list].sort((a,b)=>a.tarih.slice(0,10).localeCompare(b.tarih.slice(0,10))||rank[a.tur]-rank[b.tur]);
  for(const t of sorted) {
    if(t.tur==='getiri') {
      const day=t.tarih.slice(0,10);
      if(days.has(day))throw new Error('Aynı gün için birden çok faiz kaydı var.');
      days.add(day);
    }
    bal+=cents(t.tutar)*(t.tur==='cekme'?-1:1);
    if(bal<0)throw new Error('Çekilen tutar o tarihteki mevduat bakiyesini aşıyor.');
  }
}
export function seedHolding(a) {
  if(a.islemler?.length)return a.islemler;
  return a.miktar>0?[{id:uid(),tur:'alis',tarih:today(),miktar:a.miktar,fiyat:a.maliyetBirim||0,komisyon:a.alisKomisyon||0,not:'Önceki sürümden başlangıç kaydı'}]:[];
}
export function mergeQuotes(state,result,startedAt) {
  const quotes={...state.quotes};
  for(const [key,next] of Object.entries(result.quotes||{})) {
    const old=quotes[key];
    if(old?.status==='manual'&&(old.locked||Date.parse(old.fetchedAt)>startedAt))continue;
    if(next.bid>0&&Number.isFinite(next.bid))quotes[key]=next;
  }
  for(const [key,error] of Object.entries(result.errors||{})) {
    if(quotes[key]?.status==='manual')continue;
    if(!result.quotes?.[key])quotes[key]={...quotes[key],status:quotes[key]?.bid?'stale':'missing',error,lastAttemptAt:result.checkedAt};
  }
  return {...state,quotes,sonGuncelleme:result.checkedAt};
}
export function migrate(raw) {
  if(!raw||typeof raw!=='object'||Array.isArray(raw))throw new Error('Yedek dosyası bir nesne olmalı.');
  if(raw.schemaVersion>VERSION)throw new Error('Bu yedek daha yeni bir sürüme ait.');
  if(!Array.isArray(raw.portfoyler)||!Array.isArray(raw.varliklar))throw new Error('Portföy ve varlık listeleri bulunamadı.');
  if(raw.portfoyler.length>200||raw.varliklar.length>10000)throw new Error('Yedek dosyası çok büyük.');
  const state={...emptyState(),...structuredClone(raw),schemaVersion:VERSION};
  const ids=new Set();
  for(const p of state.portfoyler) {
    if(!p||typeof p!=='object'||Array.isArray(p))throw new Error('Portföy kaydı geçersiz.');
    if(typeof p.id!=='string'||!p.id||ids.has(p.id)||typeof p.ad!=='string'||!p.ad.trim())throw new Error('Portföy kimliği veya adı geçersiz.');
    ids.add(p.id); p.ad=p.ad.slice(0,100); p.renk=/^#[0-9a-f]{6}$/i.test(p.renk)?p.renk:COLORS[0];
  }
  const assetIds=new Set();
  for(const a of state.varliklar) {
    if(!a||typeof a!=='object'||Array.isArray(a))throw new Error('Varlık kaydı geçersiz.');
    if(['mevduat','vadeli'].includes(a.tip))a.tip='vadeli_mevduat';
    if(!TYPES[a.tip]||!ids.has(a.portfoyId)||typeof a.id!=='string'||!a.id||assetIds.has(a.id))throw new Error('Bir varlığın tipi, portföyü veya kimliği geçersiz.');
    assetIds.add(a.id);
    if(TYPES[a.tip].symbol&&!/^[A-Z0-9.]{2,16}$/i.test(a.sembol||''))throw new Error('Varlık sembolü geçersiz.');
    if(a.sembol)a.sembol=a.sembol.toUpperCase();
    if(TYPES[a.tip].bank&&typeof a.banka!=='string')a.banka='diger';
    if(a.tip==='tefas_fon'){
      a.stopajVar=a.stopajVar===true;
      a.stopajOrani=a.stopajVar?percentage(a.stopajOrani??17.5,'Stopaj oranı'):(Number.isFinite(parseNumber(a.stopajOrani))&&parseNumber(a.stopajOrani)>=0&&parseNumber(a.stopajOrani)<=100?percentage(a.stopajOrani,'Stopaj oranı'):17.5);
    }else {delete a.stopajVar;delete a.stopajOrani;}
    for(const k of ['miktar','maliyetBirim','alisKomisyon'])if(a[k]!=null)a[k]=amount(a[k],k,true);
    if(a.satilanMiktar!=null)a.satilanMiktar=amount(a.satilanMiktar,'Satılan miktar',true);
    if(a.satilanMaliyet!=null)a.satilanMaliyet=amount(a.satilanMaliyet,'Satılan maliyet',true);
    if(a.islemler!=null&&!Array.isArray(a.islemler))throw new Error('İşlem listesi geçersiz.');
    const tids=new Set();
    for(const t of a.islemler||[]) {
      if(!t||typeof t!=='object'||Array.isArray(t))throw new Error('İşlem kaydı geçersiz.');
      if(!t.id)t.id=uid();
      if(typeof t.id!=='string')throw new Error('İşlem kimliği geçersiz.');
      if(tids.has(t.id))throw new Error('Tekrarlanan işlem kimliği.'); tids.add(t.id);
      const allowed=a.tip==='vadeli_mevduat'?['getiri','yatirma','cekme']:['alis','temettu'];
      if(!allowed.includes(t.tur))throw new Error('Bilinmeyen işlem türü: '+t.tur);
      if(typeof t.tarih!=='string'||!validDay(t.tarih.slice(0,10)))throw new Error('İşlem tarihi geçersiz.');
      if(String(t.tarih).slice(0,10)>today())throw new Error('İleri tarihli işlem gerçek portföye yüklenemez.');
      if(t.tur==='alis'){t.miktar=amount(t.miktar,'Miktar');t.fiyat=amount(t.fiyat??0,'Fiyat',true);t.komisyon=amount(t.komisyon??0,'Komisyon',true);}
      else t.tutar=amount(t.tutar,'Tutar',true);
    }
    if(a.tip==='vadeli_mevduat')validateDepositLedger(a.islemler||[]);
    else {
      const summary=holdingSummary(a);
      if(summary.quantity<-1e-8)throw new Error('Satılan miktar toplam varlık miktarını aşıyor.');
      if(summary.cost!=null&&summary.cost<-.01)throw new Error('Satılan maliyet toplam varlık maliyetini aşıyor.');
    }
  }
  if(!Array.isArray(state.kapaliPozisyonlar))throw new Error('Kapalı kayıt listesi geçersiz.');
  const closedIds=new Set();
  const finiteFields=['miktar','maliyetBirim','maliyetToplam','alisKomisyon','satilanMiktar','satilanMaliyet','kapanisFiyat','brutSatis','komisyon','komisyonSonrasiSatis','stopajTutari','netSatis','kalanMiktar'];
  for(const record of state.kapaliPozisyonlar){
    if(!record||typeof record!=='object'||Array.isArray(record)||typeof record.id!=='string'||!record.id||closedIds.has(record.id))throw new Error('Kapalı kayıt veya kimliği geçersiz.');
    closedIds.add(record.id);
    if(record.portfoyId!=null&&!ids.has(record.portfoyId))throw new Error('Kapalı kaydın portföy bağlantısı bulunamadı.');
    if(record.tip!=null&&!TYPES[record.tip])throw new Error('Kapalı kaydın varlık türü geçersiz.');
    for(const key of finiteFields)if(record[key]!=null&&(!Number.isFinite(record[key])||record[key]<0||record[key]>1e15))throw new Error('Kapalı kayıt tutarı geçersiz: '+key);
    for(const key of ['kz','netKz','brutKz','stopajsizKz'])if(record[key]!=null&&(!Number.isFinite(record[key])||Math.abs(record[key])>1e15))throw new Error('Kapalı kayıt kazancı geçersiz.');
    if(record.kapanisTarihi!=null&&(!validDay(record.kapanisTarihi)||record.kapanisTarihi>today()))throw new Error('Kapalı kayıt tarihi geçersiz.');
    if(record.reversible===true){
      if(typeof record.sourceAssetId!=='string'||!record.assetBeforeClose||record.assetBeforeClose.id!==record.sourceAssetId||record.assetBeforeClose.portfoyId!==record.portfoyId)throw new Error('Kapanış geri alma bağlantısı geçersiz.');
      migrate({...emptyState(),portfoyler:state.portfoyler,varliklar:[record.assetBeforeClose]});
      if(record.creditCash&&record.netSatis>0&&typeof record.cashAssetId!=='string')throw new Error('Kapanış nakit bağlantısı geçersiz.');
    }
  }
  if(!Array.isArray(state.snapshots))throw new Error('Değer geçmişi listesi geçersiz.');
  for(const point of state.snapshots){
    if(!point||typeof point!=='object'||Array.isArray(point)||!Number.isFinite(Date.parse(point.ts)))throw new Error('Değer geçmişi tarihi geçersiz.');
    if(point.day!=null&&(!validDay(point.day)||point.day!==snapshotDay(point.ts)))throw new Error('Değer geçmişi gün ve saat bilgisi çelişiyor.');
    if(point.total!=null&&(!Number.isFinite(point.total)||point.total<0||point.total>1e15))throw new Error('Değer geçmişi tutarı geçersiz.');
    if(point.byPortfolio!=null){
      if(typeof point.byPortfolio!=='object'||Array.isArray(point.byPortfolio))throw new Error('Değer geçmişi portföy bilgisi geçersiz.');
      for(const v of Object.values(point.byPortfolio))if(!Number.isFinite(v)||v<0||v>1e15)throw new Error('Değer geçmişi portföy tutarı geçersiz.');
    }
    if(point.performance!=null){
      if(typeof point.performance!=='object'||Array.isArray(point.performance))throw new Error('Kazanç geçmişi geçersiz.');
      for(const v of Object.values(point.performance))if(!v||!Number.isFinite(v.pnl)||Math.abs(v.pnl)>1e15||typeof v.complete!=='boolean'||!Number.isSafeInteger(v.revision)||v.revision<0)throw new Error('Kazanç geçmişi alanları geçersiz.');
    }
  }
  if(state.performanceRevisions!=null){
    if(typeof state.performanceRevisions!=='object'||Array.isArray(state.performanceRevisions)||Object.values(state.performanceRevisions).some(v=>!Number.isSafeInteger(v)||v<0))throw new Error('Kazanç karşılaştırma bilgisi geçersiz.');
  }
  const rawHistory=state.chartHistory&&typeof state.chartHistory==='object'&&!Array.isArray(state.chartHistory)?state.chartHistory:{};
  const portfolioLimit=Math.max(3,Math.min(800,Math.floor(5000/Math.max(1,state.portfoyler.length))));
  const assetLimit=Math.max(3,Math.min(800,Math.floor(30000/Math.max(1,state.varliklar.length))));
  state.chartHistory={portfolios:{},assets:{}};
  for(const p of state.portfoyler)if(rawHistory.portfolios?.[p.id]!=null)state.chartHistory.portfolios[p.id]=cleanChartSeries(rawHistory.portfolios[p.id],portfolioLimit,'Portföy');
  for(const a of state.varliklar)if(rawHistory.assets?.[a.id]!=null)state.chartHistory.assets[a.id]=cleanChartSeries(rawHistory.assets[a.id],assetLimit,'Varlık');
  state.quotes=state.quotes&&typeof state.quotes==='object'&&!Array.isArray(state.quotes)?state.quotes:{};
  for(const [key,q] of Object.entries(state.quotes)) {
    if(!q||typeof q!=='object')throw new Error('Fiyat kaydı geçersiz.');
    if(q.bid!=null)q.bid=amount(q.bid,'Fiyat');
    if(q.ask!=null)q.ask=amount(q.ask,'Satış fiyatı');
    if(q.priceDate!=null&&(typeof q.priceDate!=='string'||!Number.isFinite(Date.parse(q.priceDate))))throw new Error('Fiyat tarihi geçersiz.');
    if(q.fetchedAt!=null&&(typeof q.fetchedAt!=='string'||!Number.isFinite(Date.parse(q.fetchedAt))))throw new Error('Fiyat alınma tarihi geçersiz.');
  }
  for(const [key,v] of Object.entries(raw.fiyatlar||{})) {
    if(!state.quotes[key]&&typeof v==='number'&&v>0&&Number.isFinite(v))state.quotes[key]={bid:v,status:'legacy',source:'Önceki sürüm',fetchedAt:raw.sonGuncelleme||null};
  }
  return state;
}
export function closeHolding(state,id,price,fee,date,creditCash=true,quantity=null,actualWithholding=null) {
  const a=state.varliklar.find(x=>x.id===id);
  if(!a||a.tip==='vadeli_mevduat'||a.tip==='try_lirasi')throw new Error('Bu varlık kapatılamaz.');
  const h=holdingSummary(a);price=amount(price,'Kapanış fiyatı');fee=amount(fee||0,'Komisyon',true);
  if(!(h.quantity>0))throw new Error('Kapatılabilecek varlık miktarı bulunamadı.');
  const soldQuantity=quantity==null?h.quantity:amount(quantity,'Satılan miktar');
  if(soldQuantity>h.quantity+1e-8)throw new Error('Satılan miktar eldeki miktarı aşamaz.');
  if(!validDay(date)||date>today())throw new Error('Kapanış tarihi geçersiz.');
  const previousCloseDates=(state.kapaliPozisyonlar||[]).filter(r=>r.sourceAssetId===id&&validDay(String(r.kapanisTarihi||'').slice(0,10))).map(r=>r.kapanisTarihi.slice(0,10)).sort(),latestClose=previousCloseDates[previousCloseDates.length-1];
  if(latestClose&&date<latestClose)throw new Error('Satış tarihi önceki kapanış kaydından eski olamaz. Önce en son satışı kaydedin.');
  const dated=datedHoldingSummary(a,date);
  if(dated.quantity<-1e-8||soldQuantity>dated.quantity+1e-8)throw new Error('Seçilen satış tarihinde bu miktar henüz portföyde bulunmuyor. Alış tarihlerini ve satılan miktarı kontrol edin.');
  const remainingQuantity=Math.max(0,Math.round((h.quantity-soldQuantity)*1e8)/1e8);
  const soldCost=dated.cost==null?null:round(dated.cost*(soldQuantity/dated.quantity));
  const grossSale=round(soldQuantity*price),saleAfterFee=round(grossSale-fee);
  if(saleAfterFee<0)throw new Error('Komisyon satış tutarından büyük olamaz.');
  const grossPnl=soldCost==null?null:round(saleAfterFee-soldCost),withholdingEnabled=a.tip==='tefas_fon'&&a.stopajVar===true;
  let withholding=0,withholdingEstimated=false;
  if(actualWithholding!=null){
    withholding=round(amount(actualWithholding,'Kesilen stopaj',true));
    if(!withholdingEnabled&&withholding>0)throw new Error('Stopaj tutarı yalnızca stopajlı TEFAS fonlarında girilebilir.');
  }else if(withholdingEnabled){
    if(grossPnl==null)throw new Error('Maliyet bilinmediği için stopaj hesaplanamıyor. Yatırım kuruluşunun kestiği stopaj tutarını girin.');
    withholding=round(Math.max(grossPnl,0)*percentage(a.stopajOrani??17.5,'Stopaj oranı')/100);withholdingEstimated=true;
  }
  if(withholding>saleAfterFee)throw new Error('Stopaj tutarı komisyon sonrası satış tutarını aşamaz.');
  const proceeds=round(saleAfterFee-withholding),netPnl=grossPnl==null?null:round(grossPnl-withholding),recordId=uid(),cashAssetId=creditCash&&proceeds>0?uid():null;
  let assets=state.varliklar.map(x=>x.id!==id?x:remainingQuantity>0?{...a,satilanMiktar:Math.round(((a.satilanMiktar||0)+soldQuantity)*1e8)/1e8,satilanMaliyet:soldCost==null?null:round((a.satilanMaliyet||0)+soldCost)}:null).filter(Boolean);
  if(cashAssetId) {
    // Ayrı nakit kaydı, işlemin izini ve tutarını korur.
    assets.push({id:cashAssetId,portfoyId:a.portfoyId,tip:'try_lirasi',ad:assetName(a)+' kapanış nakdi',miktar:proceeds,maliyetBirim:1,not:'Kapanış kaydından aktarıldı: '+date,sourceCloseId:recordId});
  }
  const record={...a,id:recordId,miktar:soldQuantity,maliyetBirim:soldCost==null?0:soldCost/soldQuantity,maliyetToplam:soldCost,alisKomisyon:0,satilanMiktar:0,satilanMaliyet:0,kapanisFiyat:price,kapanisTarihi:date,brutSatis:grossSale,komisyon:fee,komisyonSonrasiSatis:saleAfterFee,stopajsizKz:grossPnl,brutKz:grossPnl,stopajTutari:withholding,stopajTahmini:withholdingEstimated,netKz:netPnl,kz:netPnl,netSatis:proceeds,creditCash,parcali:remainingQuantity>0,kalanMiktar:remainingQuantity,sourceAssetId:a.id,cashAssetId,reversible:true,assetBeforeClose:structuredClone(a)};
  return {...state,varliklar:assets,kapaliPozisyonlar:[...state.kapaliPozisyonlar,record]};
}
export function canUndoClose(state,record) {
  if(!record?.reversible||!record.sourceAssetId||!record.assetBeforeClose)return false;
  const index=(state.kapaliPozisyonlar||[]).findIndex(r=>r.id===record.id);
  return index>=0&&!(state.kapaliPozisyonlar||[]).slice(index+1).some(r=>r.sourceAssetId===record.sourceAssetId);
}
export function undoCloseHolding(state,recordId) {
  const records=state.kapaliPozisyonlar||[],index=records.findIndex(r=>r.id===recordId),record=records[index];
  if(!record)throw new Error('Kapanış kaydı bulunamadı.');
  if(!record.reversible||!record.sourceAssetId||!record.assetBeforeClose)throw new Error('Bu eski kapanış kaydı güvenli biçimde geri alınamaz.');
  if(records.slice(index+1).some(r=>r.sourceAssetId===record.sourceAssetId))throw new Error('Önce bu varlığın daha yeni kapanış kaydını geri alın.');
  let assets=[...state.varliklar];
  if(record.creditCash&&record.netSatis>0){
    const cashIndex=assets.findIndex(a=>a.id===record.cashAssetId),cash=assets[cashIndex];
    if(cashIndex<0||cash.tip!=='try_lirasi'||cash.sourceCloseId!==record.id||round(cash.miktar)!==round(record.netSatis))throw new Error('Kapanıştan oluşan nakit kaydı değiştirilmiş veya silinmiş. Veri kaybını önlemek için geri alma durduruldu.');
    assets.splice(cashIndex,1);
  }
  const assetIndex=assets.findIndex(a=>a.id===record.sourceAssetId);
  if(record.parcali){
    if(assetIndex<0)throw new Error('Parçalı satıştan kalan varlık bulunamadı; güvenli geri alma yapılamadı.');
    const current=assets[assetIndex],soldQuantity=(current.satilanMiktar||0)-record.miktar;
    if(soldQuantity<-.00000001)throw new Error('Satılan miktar kaydı değişmiş; güvenli geri alma yapılamadı.');
    const restored={...current,satilanMiktar:Math.max(0,Math.round(soldQuantity*1e8)/1e8)};
    if(record.maliyetToplam==null)restored.satilanMaliyet=restored.satilanMiktar>0?null:0;
    else {
      const soldCost=(current.satilanMaliyet||0)-record.maliyetToplam;
      if(soldCost<-.01)throw new Error('Satılan maliyet kaydı değişmiş; güvenli geri alma yapılamadı.');
      restored.satilanMaliyet=Math.max(0,round(soldCost));
    }
    assets[assetIndex]=restored;
  }else {
    if(assetIndex>=0)throw new Error('Aynı varlık kimliği yeniden kullanılmış; güvenli geri alma yapılamadı.');
    assets.push(structuredClone(record.assetBeforeClose));
  }
  return {...state,varliklar:assets,kapaliPozisyonlar:records.filter(r=>r.id!==recordId)};
}

// Satışın dayandığı alış/maliyet kaydı değiştirilmez; önce ilgili satış geri alınır.
const costIdentity=a=>JSON.stringify([a.portfoyId,a.tip,a.sembol||'',a.banka||'',a.miktar||0,a.maliyetBirim||0,a.alisKomisyon||0,!!a.maliyetManuel]);
const ledgerIdentity=t=>JSON.stringify([t.id,t.tur,String(t.tarih).slice(0,10),t.miktar??null,t.fiyat??null,t.komisyon||0,t.tutar??null]);
export function protectClosedCosts(previous,next){
  for(const old of previous.varliklar){
    const current=next.varliklar.find(a=>a.id===old.id);
    if(!current)continue;
    const closes=next.kapaliPozisyonlar.filter(r=>r.sourceAssetId===old.id),sold=closes.length>0||(old.satilanMiktar||0)>0;
    if(!sold)continue;
    const latest=closes.map(r=>r.kapanisTarihi).filter(validDay).sort().at(-1);
    const changedBasis=costIdentity(old)!==costIdentity(current);
    const before=old.islemler||[],after=current.islemler||[];
    const changedLedger=JSON.stringify(before.map(ledgerIdentity))!==JSON.stringify(after.map(ledgerIdentity));
    if(changedBasis||(changedLedger&&(!latest||!before.length||before.filter(t=>t.tarih.slice(0,10)<=latest).some(t=>!after.some(n=>ledgerIdentity(n)===ledgerIdentity(t)))||after.filter(t=>t.tarih.slice(0,10)<=latest).some(t=>!before.some(n=>ledgerIdentity(n)===ledgerIdentity(t))))))throw new Error('Bu maliyet kaydına bağlı satış var. Önce ilgili satışları en yeniden başlayarak geri alın; sonra alış kaydını düzeltin.');
  }
}
// Maliyet düzeltmesi/silme geçmiş kazançla karşılaştırılamaz. Eski arşiv korunur.
export function protectPerformanceHistory(previous,next){
  const revisions={...(next.performanceRevisions||{})},affected=new Set();
  const closedBefore=new Map(previous.kapaliPozisyonlar.map(r=>[r.id,r]));
  for(const r of previous.kapaliPozisyonlar)if(!next.kapaliPozisyonlar.some(n=>n.id===r.id))affected.add(r.portfoyId);
  for(const a of previous.varliklar){
    if(['try_lirasi'].includes(a.tip))continue;
    const n=next.varliklar.find(x=>x.id===a.id);
    if(!n){if(!next.kapaliPozisyonlar.some(r=>!closedBefore.has(r.id)&&r.sourceAssetId===a.id))affected.add(a.portfoyId);continue;}
    if(costIdentity(a)!==costIdentity(n)){affected.add(a.portfoyId);affected.add(n.portfoyId);}
    for(const t of a.islemler||[])if(!(n.islemler||[]).some(i=>ledgerIdentity(i)===ledgerIdentity(t)))affected.add(a.portfoyId);
  }
  for(const id of affected)if(id)revisions[id]=(previous.performanceRevisions?.[id]||0)+1;
  return {...next,performanceRevisions:revisions};
}
export function validateBackup(raw){
  const checked=migrate(raw);
  for(const p of checked.portfoyler){totals(checked,p.id);chartSeries(checked,'portfolio',p.id);}
  totals(checked);for(const a of checked.varliklar)chartSeries(checked,'asset',a.id);
  return checked;
}
