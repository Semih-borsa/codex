import {protectClosedCosts,protectPerformanceHistory,validateBackup,STORAGE,COLORS,TYPES,BANKS,uid,today,money,num,round,amount,percentage,validDay,emptyState,migrate,assetName,quoteKey,depositSummary,holdingSummary,valuation,totals,quoteQuality,saveDepositEntry,validateDepositLedger,seedHolding,mergeQuotes,recordSnapshot,recordChartPoint,chartSeries,periodPerformance,closingDateFloor,closeHolding,canUndoClose,undoCloseHolding} from './core.mjs';
import {isAndroid,saveDocument,openDocument} from './native.mjs';
import {refreshPrices} from './providers.mjs';

const $=(s,p=document)=>p.querySelector(s);
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const icons={home:'M3 10 12 3l9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z',wallet:'M3 6h17v14H3z M3 6V4h14v2 M15 12h6v5h-6z',bank:'m3 9 9-6 9 6H3m2 3v7m5-7v7m4-7v7m5-7v7M3 22h18',chart:'M4 20V11m6 9V4m6 16v-9m6 9V7',history:'M3 12a9 9 0 1 0 3-7M3 3v5h5m4-1v6l4 2',settings:'M4 6h16M4 12h16M4 18h16M8 3v6m8 0v6m-6 0v6',plus:'M12 5v14M5 12h14',refresh:'M20 8a8 8 0 0 0-14-3L3 8m0-5v5h5m-4 8a8 8 0 0 0 14 3l3-3m0 5v-5h-5',arrow:'M5 12h14m-5-5 5 5-5 5',download:'M12 3v12m-5-5 5 5 5-5M4 17v4h16v-4',check:'m5 12 4 4L19 6',close:'m6 6 12 12M6 18 18 6',edit:'m4 16 12-12 4 4L8 20H4Zm10-10 4 4',shield:'M12 3 3 6v6c0 5 9 9 9 9s9-4 9-9V6Zm-5 9 3 3 6-6',search:'M21 21l-5-5m2-6a7 7 0 1 1-14 0 7 7 0 0 1 14 0',trash:'M4 6h16M9 6V3h6v3M6 6l1 15h10l1-15M10 10v7m4-7v7',moon:'M20 15.5A8.5 8.5 0 0 1 8.5 4 8.5 8.5 0 1 0 20 15.5Z',sun:'M12 3v2m0 14v2M3 12h2m14 0h2M5.6 5.6 7 7m10 10 1.4 1.4M18.4 5.6 17 7M7 17l-1.4 1.4M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z'};
const icon=(n,c='')=>`<svg class="icon ${c}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${icons[n]||icons.wallet}"/></svg>`;
const action=(a,label,cls='secondary',data='')=>`<button class="btn ${cls}" data-action="${a}" ${data}>${label}</button>`;
const refreshButton=(scope='all',cls='secondary')=>action('refresh',icon('refresh',busy?'spin':'')+(busy?'Güncelleniyor…':'Fiyatları güncelle'),cls,`data-refresh-scope="${scope}" ${busy?'disabled':''}`);
const dateLabel=s=>s&&Number.isFinite(Date.parse(s))?new Intl.DateTimeFormat('tr-TR',{dateStyle:'medium'}).format(new Date(String(s).length===10?s+'T12:00:00':s)):'Tarih yok';
const stamp=s=>s&&Number.isFinite(Date.parse(s))?new Intl.DateTimeFormat('tr-TR',{dateStyle:'short',timeStyle:'short',timeZone:'Europe/Istanbul'}).format(new Date(s)):'Henüz güncellenmedi';
const labels={manual:'Elle girildi',missing:'Fiyat yok',stale:'Eski / doğrulanmadı',dated:'Tarihli veri',estimated:'Tahmini',received:'Kaynak alındı',fixed:'Sabit TL'};
const badge=q=>{const v=typeof q==='string'?q:quoteQuality(q);return `<span class="badge ${v}"><i></i>${labels[v]||esc(v)}</span>`;};
const pnl=(v,pct)=>v==null?'<span class="muted">Maliyet bilgisi yok</span>':`<span class="${v>=0?'positive':'negative'}">${v>0?'+':''}${money(v)}${pct!=null?` <small>(${pct>0?'+':''}${num(pct,2)}%)</small>`:''}</span>`;
const assetPnl=v=>!v.withholdingEnabled?pnl(v.pnl,v.percent):`<span class="tax-pnl"><span><em>Net kâr / zarar</em>${pnl(v.pnl,v.percent)}</span><span><em>Stopajsız kâr / zarar</em>${pnl(v.grossPnl,v.grossPercent)}</span><span><em>Tahmini stopaj</em>${v.withholding==null?'<i>Hesaplanamadı</i>':(v.withholding>0?'−':'')+money(v.withholding)+' ('+num(v.withholdingRate,2)+'%)'}</span></span>`;
const THEME_STORAGE='portfoy_theme';
let state,loadError='',view='overview',pid='all',search='',bankMetal='gold',busy=false,sourceReport=[],toastTimer,refreshEpoch=0,theme='light';
try{theme=localStorage.getItem(THEME_STORAGE)==='dark'?'dark':'light';}catch{}
function applyTheme(){document.documentElement?.setAttribute?.('data-theme',theme);}
function toggleTheme(){theme=theme==='dark'?'light':'dark';try{localStorage.setItem(THEME_STORAGE,theme);}catch{}applyTheme();render();toast(theme==='dark'?'Koyu mod açıldı.':'Açık mod açıldı.');}
applyTheme();
try {const raw=localStorage.getItem(STORAGE)||localStorage.getItem('portfoy_v3');state=raw?migrate(JSON.parse(raw)):emptyState();}
catch(e){state=emptyState();loadError='Kayıt okunamadı: '+e.message+' Orijinal kayıt değiştirilmedi. Yedekten geri yükleyebilirsiniz.';}
function toast(message,error=false){const e=$('#toast');e.textContent=message;e.className=error?'show error':'show';clearTimeout(toastTimer);toastTimer=setTimeout(()=>e.className='',6000);}
function commit(next,track=true,historyAt=new Date().toISOString()){
  if(loadError)throw new Error('Kayıt sorunu çözülmeden yeni veri kaydedilemez. Önce yedeğinizi içe aktarın.');
  protectClosedCosts(state,next);
  const migrated=migrate(protectPerformanceHistory(state,next)),valid=track?recordChartPoint(migrated,historyAt):migrated;
  try{localStorage.setItem(STORAGE,JSON.stringify(valid));}catch{throw new Error('Kayıt yapılamadı. Tarayıcı depolaması dolu veya kapalı. Verinizi yedekleyin.');}
  state=valid;render();
}
function commitAsset(a){commit({...state,varliklar:state.varliklar.some(x=>x.id===a.id)?state.varliklar.map(x=>x.id===a.id?a:x):[...state.varliklar,a]});}
const assetById=id=>state.varliklar.find(a=>a.id===id);
const portfolioName=id=>state.portfoyler.find(p=>p.id===id)?.ad||'Portföy';
const scopeAssets=()=>state.varliklar.filter(a=>pid==='all'||a.portfoyId===pid);
const input=(name,label,value='',extra='')=>`<label class="field"><span>${label}</span><input name="${name}" value="${esc(value)}" ${extra}></label>`;
const decimal=(name,label,value='',required=true)=>input(name,label,value,`inputmode="decimal" autocomplete="off" placeholder="0,00" ${required?'required':''}`);
const select=(name,label,options,value)=>`<label class="field"><span>${label}</span><select name="${name}">${Object.entries(options).map(([k,v])=>`<option value="${esc(k)}" ${k===value?'selected':''}>${esc(v)}</option>`).join('')}</select></label>`;
const note=t=>`<div class="notice">${t}</div>`;
const empty=(title,desc,button='')=>`<div class="empty"><div class="empty-icon">${icon('wallet')}</div><h3>${title}</h3><p>${desc}</p>${button}</div>`;

function shell(content){
  const navs=[['overview','home','Özet'],['holdings','wallet','Varlıklar'],['deposits','bank','Mevduat'],['banks','chart','Kurlar'],['history','history','Geçmiş'],['settings','settings','Ayarlar']];
  const titles={overview:['Portföy özeti','Toplam değer ile günlük, haftalık ve aylık değişim.'],holdings:['Varlıklarım','Hisse, fon, döviz ve kıymetli maden kayıtların.'],deposits:['Vadeli mevduat','Faizi sen gir. Toplama biz ekleyelim.'],banks:['Altın & gümüş banka kurları','Bankaların alış ve satış fiyatlarını ayrı incele.'],history:['İşlem geçmişi','Kayıtlarını gör, faiz ve alım kayıtlarını düzenle.'],settings:['Yedek ve ayarlar','Verinin kontrolü sende. Düzenli yedek al.']};
  const currentTitle=view==='holdings'&&pid!=='all'?portfolioName(pid):titles[view][0];
  const headActions=view==='overview'
    ?refreshButton('all')
    :refreshButton(view==='holdings'&&pid!=='all'?'portfolio':'all')+action('add-asset',icon('plus')+'Varlık ekle','primary');
  const scopebar=view==='overview'?'':`<div class="scopebar"><div class="portfolio-switch"><span class="mini-label">PORTFÖY</span><select id="scope" aria-label="Görüntülenen portföy"><option value="all">Tüm portföyler</option>${state.portfoyler.map(p=>`<option value="${esc(p.id)}" ${pid===p.id?'selected':''}>${esc(p.ad)}</option>`).join('')}</select></div><span class="updated">Son deneme: ${stamp(state.sonGuncelleme)}</span></div>`;
  return `<div class="app-layout view-${view}"><aside class="sidebar"><a class="brand" href="#overview" data-action="nav" data-view="overview"><img src="./icon.svg" alt=""><span>portföyüm<small>KİŞİSEL VARLIK TAKİBİ</small></span></a><div class="nav-label">ÇALIŞMA ALANIM</div><nav>${navs.map(([key,i,name])=>`<button data-action="nav" data-view="${key}" class="nav-item ${view===key?'active':''}">${icon(i)}<span>${name}</span>${view===key?'<i></i>':''}</button>`).join('')}</nav><div class="sidebar-bottom">${icon('shield')}<b>Verilerin bu cihazda</b><p>Banka hesabına bağlanmaz.<br>Alım veya satım yapmaz.</p><small>Android · 4.8.3</small></div></aside><main><header class="topbar"><span class="breadcrumb">Çalışma alanım <span>/</span> ${esc(currentTitle)}</span><div class="top-right"><button class="btn icon-btn theme-toggle" data-action="toggle-theme" aria-label="${theme==='dark'?'Açık moda geç':'Koyu moda geç'}">${icon(theme==='dark'?'sun':'moon')}</button><span class="local-indicator"><i></i> Telefonda kayıt</span><div class="avatar">P</div></div></header><div class="workspace"><div class="page-head"><div><div class="eyebrow">${new Intl.DateTimeFormat('tr-TR',{day:'numeric',month:'long',year:'numeric'}).format(new Date())}</div><h1>${esc(currentTitle)}</h1><p>${titles[view][1]}</p></div><div class="head-actions">${headActions}</div></div>${loadError?`<div class="alert danger">${esc(loadError)}</div>`:''}${scopebar}${content}<footer>Portföyüm · Yalnızca kişisel kayıt ve değerleme. Fiyatlar gecikmeli olabilir; bankanın işlem ekranı esas alınır.</footer></div></main></div>`;
}
function periodBox(label,result){
  if(!result||result.profit==null)return `<div class="period-box"><span>${label}</span><b>—</b><small>${result?'Getiri için veri eksik':'Geçmiş oluşuyor'}</small></div>`;
  const cls=result.profit>=0?'positive':'negative',sign=result.profit>0?'+':result.profit<0?'−':'';
  return `<div class="period-box" title="${esc(dateLabel(result.at))} başlangıç değeriyle karşılaştırma"><span>${label}</span><b class="${cls}">${sign}${money(Math.abs(result.profit))}</b><small class="${cls}">${result.profitPercent==null?'—':(result.profitPercent>0?'+':'')+num(result.profitPercent,2)+'%'}</small></div>`;
}
function periods(id){return `<div class="period-grid">${periodBox('Günlük K/Z',periodPerformance(state,id,1))}${periodBox('Haftalık K/Z',periodPerformance(state,id,7))}${periodBox('Aylık K/Z',periodPerformance(state,id,30))}</div>`;}
function warnings(t){return (t.missing?`<div class="alert">${icon('shield')}<span><b>${t.missing} varlığın fiyatı yok.</b> Bu varlıklar toplam ve kâr/zarar hesabına katılmadı. Gerçek toplam daha farklı olabilir.</span></div>`:'')+(t.unverified?`<div class="subtle-warning">${t.unverified} varlıkta eski, tarihli veya eksik fiyat var. Kaynak tarihlerini kontrol et.</div>`:'');}
const portfolioColor=p=>/^#[0-9a-f]{6}$/i.test(p?.renk||'')?p.renk:COLORS[0];
const performanceClass=v=>v==null||v===0?'performance-neutral':v>0?'performance-profit':'performance-loss';
const DISTRIBUTION_COLORS=['#0b554e','#477a72','#78a098','#afc2be','#d8e1df','#95a1a8'];
function performance(value,percent,label='Net kâr / zarar',unknownCost=0,withholding=0,details=null){
  const cls=performanceClass(percent==null?null:value);
  const result=percent==null?'<b>— <small>Maliyet bilgisi yok</small></b>':`<b>${value>0?'+':''}${money(value)} <small>${percent>0?'+':''}${num(percent,2)}%</small></b>`;
  const breakdown=details?.realizedKnownCount?`Açık: ${details.openPnl>0?'+':''}${money(details.openPnl)} · Gerçekleşmiş: ${details.realizedPnl>0?'+':''}${money(details.realizedPnl)}`:'';
  const taxNotes=details?[details.estimatedWithholding>0?'Tahmini stopaj düşüldü: '+money(details.estimatedWithholding):'',details.realizedWithholding>0?'Kapanış stopajı: '+money(details.realizedWithholding):'']:[withholding>0?'Stopaj: '+money(withholding):''];
  const notes=[breakdown,unknownCost?unknownCost+' kayıtta maliyet eksik':'',...taxNotes].filter(Boolean).join(' · ');
  return `<div class="performance-summary ${cls}"><span>${label}</span>${result}${notes?`<em>${notes}</em>`:''}</div>`;
}
function assetRows(assets){return assets.map(a=>{
  const v=valuation(a,state.quotes),type=TYPES[a.tip];
  return `<tr class="asset-performance ${performanceClass(v.pnl)}"><td><button class="asset-link" data-action="detail" data-id="${esc(a.id)}"><span class="asset-icon ${esc(type.group==='Altın'?'gold':type.group==='Mevduat'?'mint':'')}">${esc(a.sembol?a.sembol.slice(0,2):type.group==='Altın'?'Au':type.group==='Gümüş'?'Ag':type.group==='Mevduat'?'₺':type.group==='Nakit'?'₺':type.name.slice(0,2))}</span><span><b>${esc(assetName(a))}</b><small>${esc(type.name)}${a.banka?' · '+esc(BANKS[a.banka]||a.banka):''}${v.withholdingEnabled?' · Stopajlı':''}</small></span></button></td><td class="desktop-only muted">${esc(portfolioName(a.portfoyId))}</td><td class="numeric">${a.tip==='vadeli_mevduat'?'1 hesap':num(v.quantity,6)+' <small>'+type.unit+'</small>'}</td><td class="numeric">${a.tip==='vadeli_mevduat'?'—':v.price==null?'—':num(v.price,a.tip==='tefas_fon'?6:4)+' ₺'}<small class="cell-sub">${a.tip==='tefas_fon'&&v.quote?.priceDate?dateLabel(v.quote.priceDate):''}</small></td><td class="numeric"><b>${v.value==null?'—':money(v.value)}</b>${v.withholdingEnabled?'<small class="net-value-label">TAHMİNİ NET DEĞER</small>':''}<small class="cell-sub">${assetPnl(v)}</small></td><td>${badge(v.quality)}</td><td>${action('detail',icon('arrow'),'icon-btn',`aria-label="${esc(assetName(a))} ayrıntıları" data-id="${esc(a.id)}"`)}</td></tr>`;
}).join('');}
function assetsTable(assets){return `<div class="table-wrap"><table><thead><tr><th>Varlık</th><th class="desktop-only">Portföy</th><th class="numeric">Miktar</th><th class="numeric">Birim fiyat</th><th class="numeric">Net değer / Kâr-Zarar</th><th>Fiyat durumu</th><th></th></tr></thead><tbody>${assetRows(assets)}</tbody></table></div>`;}
function allocation(t){
  const groups=Object.entries(t.groups).filter(([,v])=>v>0).sort((a,b)=>b[1]-a[1]);
  let cum=0;const segments=groups.map(([,v],i)=>{const start=cum;cum+=v/t.value*100;return `${COLORS[i%COLORS.length]} ${start}% ${cum}%`;});
  return `<section class="panel distribution"><div class="panel-head"><h2>Varlık dağılımı</h2><span class="muted">TL bazında</span></div><div class="donut-wrap"><div class="donut" style="background:conic-gradient(${segments.length?segments.join(','):'#edf0f6 0% 100%'})"><div><b>${groups.length}</b><small>varlık sınıfı</small></div></div></div><div class="legend">${groups.length?groups.map(([g,v],i)=>`<div><span><i style="background:${COLORS[i%COLORS.length]}"></i>${g}</span><b>${num(v/t.value*100,1)}%</b></div>`).join(''):'<p class="muted">Varlık eklediğinde dağılım burada görünür.</p>'}</div></section>`;
}
function overview(){
  const all=totals(state,'all');
  const cards=state.portfoyler.map(p=>{const t=totals(state,p.id),color=portfolioColor(p);return `<button class="portfolio-overview-card" style="--portfolio-color:${color}" data-action="open-portfolio" data-id="${esc(p.id)}"><div class="portfolio-card-head"><span><i style="background:${color}"></i>${esc(p.ad)}</span>${icon('arrow')}</div><div class="portfolio-value"><small>TOPLAM NET PORTFÖY DEĞERİ</small><strong>${money(t.value)}</strong>${t.missing?`<em>${t.missing} fiyat eksik; toplama katılmadı</em>`:''}</div>${performance(t.pnl,t.percent,'Net kâr / zarar (toplam)',t.unknownCost,t.withholding,t)}${periods(p.id)}</button>`;}).join('');
  return `<section class="overview-total-card"><div><span>TÜM PORTFÖYLER · NET DEĞER</span><strong>${money(all.value)}</strong>${performance(all.pnl,all.percent,'Net kâr / zarar (toplam)',all.unknownCost,all.withholding,all)}${all.missing?`<small>${all.missing} fiyat eksik; toplam yalnızca fiyatı olan varlıkları içeriyor.</small>`:'<small>Son deneme: '+stamp(state.sonGuncelleme)+'</small>'}<div class="overview-refresh-action">${refreshButton('all','hero-refresh')}</div></div>${periods('all')}</section><div class="overview-section-head"><div><span class="eyebrow">PORTFÖYLER</span><h2>Ayrı portföy görünümü</h2></div><small>Karta dokununca varlık ayrıntıları açılır</small></div><div class="portfolio-overview-grid">${cards||empty('Henüz portföy yok','Ayarlar bölümünden yeni bir portföy ekleyebilirsin.',action('nav','Ayarlara git','primary','data-view="settings"'))}</div>`;
}
function portfolioHero(p,t){
  const color=portfolioColor(p);
  return `<section class="portfolio-detail-hero" style="--portfolio-color:${color}"><div><span>SEÇİLİ PORTFÖY</span><h2>${esc(p.ad)}</h2><small>${t.count} açık varlık${t.missing?' · '+t.missing+' fiyat eksik':''}</small></div><div class="portfolio-detail-value"><span>TOPLAM NET PORTFÖY DEĞERİ</span><strong>${money(t.value)}</strong>${performance(t.pnl,t.percent,'Net kâr / zarar (toplam)',t.unknownCost,t.withholding,t)}</div></section>`;
}
const chartHash=value=>[...String(value)].reduce((n,c)=>(n*31+c.charCodeAt(0))>>>0,7).toString(36);
const smoothLinePath=points=>points.length?points.slice(1).reduce((path,current,i)=>{const previous=points[i],middle=(previous.x+current.x)/2;return path+` C ${middle.toFixed(2)} ${previous.y.toFixed(2)}, ${middle.toFixed(2)} ${current.y.toFixed(2)}, ${current.x.toFixed(2)} ${current.y.toFixed(2)}`;},`M ${points[0].x.toFixed(2)} ${points[0].y.toFixed(2)}`):'';
const chartRanges=new Map();
const chartRangeKey=(scope,id)=>JSON.stringify([scope,id]);
function trendChart(scope,id,title,compact=false){
  const data=chartSeries(state,scope,id,chartRanges.get(chartRangeKey(scope,id))),a=scope==='asset'?assetById(id):null,isValue=scope==='portfolio'||['vadeli_mevduat','try_lirasi'].includes(a?.tip),digits=a?.tip==='tefas_fon'?6:4;
  const format=value=>isValue?money(value):num(value,digits)+' ₺',last=data.points[data.points.length-1],ranges=[['day','Günlük'],['week','Haftalık'],['month','Aylık']];
  const rangeHtml=`<div class="trend-ranges" role="group" aria-label="Grafik dönemi">${ranges.map(([key,label])=>`<button type="button" data-action="chart-range" data-scope="${scope}" data-id="${esc(id)}" data-range="${key}" class="${data.mode===key?'active':''}" aria-pressed="${data.mode===key}">${label}</button>`).join('')}</div>`;
  const chartAttrs=`data-chart-scope="${scope}" data-chart-id="${esc(id)}" data-chart-title="${esc(title)}" data-chart-compact="${compact}"`;
  const change=data.change,changeHtml=change==null?'<span class="muted">İkinci kayıt bekleniyor</span>':`<span class="${change>=0?'positive':'negative'}">${change>0?'+':''}${format(change)}${data.percent!=null?` <small>(${data.percent>0?'+':''}${num(data.percent,2)}%)</small>`:''}</span>`;
  const head=`<div class="trend-head"><div><span class="eyebrow">FİYAT GEÇMİŞİ</span><h3>${esc(title)}</h3><p>${last?'Son kayıt: '+esc(stamp(last.ts)):'Fiyatlar yenilendikçe cihazda oluşur'}</p></div><div class="trend-current"><small>${data.label} değişim</small><b>${last?format(last.value):'—'}</b>${changeHtml}</div></div>${rangeHtml}`;
  if(data.points.length<2)return `<section class="trend-card ${compact?'compact':''}" ${chartAttrs}>${head}<div class="trend-empty">${icon('chart')}<div><b>Grafik için veri birikiyor</b><p>Seçili aralıkta en az iki farklı zamanda kayıt gerekli. Diğer dönemleri de seçebilirsin.</p></div></div></section>`;
  const width=760,height=220,padX=18,padY=18,values=data.points.map(p=>p.value),rawMin=Math.min(...values),rawMax=Math.max(...values),gap=rawMax-rawMin||Math.max(Math.abs(rawMax)*.02,.01),min=rawMin-gap*.12,max=rawMax+gap*.12;
  const points=data.points.map((point,i)=>({x:padX+i*(width-padX*2)/(data.points.length-1),y:padY+(max-point.value)/(max-min)*(height-padY*2),...point}));
  const finalPoint=points[points.length-1],path=smoothLinePath(points),baseY=points[0].y.toFixed(2),area=`${path} L ${finalPoint.x.toFixed(2)} ${height-padY} L ${points[0].x.toFixed(2)} ${height-padY} Z`,color=change<0?'#d65364':'#0b9d88',gradient=`trend-${scope}-${chartHash(id)}`;
  const label=value=>new Intl.DateTimeFormat('tr-TR',data.mode==='day'?{hour:'2-digit',minute:'2-digit',timeZone:'Europe/Istanbul'}:{day:'2-digit',month:'short',timeZone:'Europe/Istanbul'}).format(new Date(value));
  const middle=data.points[Math.floor((data.points.length-1)/2)];
  return `<section class="trend-card ${compact?'compact':''}" ${chartAttrs} style="--trend-color:${color}">${head}${data.complete?'':'<p class="trend-coverage">Dönemin tamamı için geçmiş yok; yalnız kayıtlı tarihler gösteriliyor.</p>'}<div class="trend-plot"><div class="trend-y-labels"><span>${format(rawMax)}</span><span>${format(rawMin)}</span></div><svg viewBox="0 0 ${width} ${height}" role="img" aria-label="${esc(title+' '+data.label+' değişim grafiği')}" preserveAspectRatio="none"><defs><linearGradient id="${gradient}" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stop-color="${color}" stop-opacity=".3"/><stop offset="1" stop-color="${color}" stop-opacity=".02"/></linearGradient></defs><line class="trend-grid" x1="${padX}" x2="${width-padX}" y1="${padY}" y2="${padY}"/><line class="trend-grid" x1="${padX}" x2="${width-padX}" y1="${height/2}" y2="${height/2}"/><line class="trend-grid" x1="${padX}" x2="${width-padX}" y1="${height-padY}" y2="${height-padY}"/><path class="trend-area" d="${area}" fill="url(#${gradient})"/><line class="trend-baseline" x1="${padX}" x2="${width-padX}" y1="${baseY}" y2="${baseY}"/><path class="trend-line" d="${path}"/><circle class="trend-dot" cx="${finalPoint.x}" cy="${finalPoint.y}" r="4.5"/></svg><div class="trend-x-labels"><span>${label(data.points[0].ts)}</span><span>${label(middle.ts)}</span><span>${label(data.points[data.points.length-1].ts)}</span></div></div></section>`;
}
function assetAllocation(assets,portfolio){
  const valued=assets.map(a=>({a,v:valuation(a,state.quotes)})).filter(x=>x.v.value>0).sort((a,b)=>b.v.value-a.v.value);
  const total=valued.reduce((sum,x)=>sum+x.v.value,0),missing=assets.length-valued.length,leaders=valued.slice(0,5),rest=valued.slice(5);
  const slices=leaders.map((x,i)=>({value:x.v.value,color:DISTRIBUTION_COLORS[i]}));
  const otherValue=rest.reduce((sum,x)=>sum+x.v.value,0);if(otherValue>0)slices.push({value:otherValue,color:DISTRIBUTION_COLORS[5]});
  let cumulative=0;const segments=slices.map(s=>{const start=cumulative;cumulative+=s.value/total*100;return `${s.color} ${start.toFixed(4)}% ${cumulative.toFixed(4)}%`;});
  const otherShare=otherValue>0?otherValue/total*100:0;
  const donutLabel=`${portfolio.ad} varlık dağılımı. ${leaders.map(({a,v})=>assetName(a)+' yüzde '+num(v.value/total*100,1)).join(', ')}${otherValue>0?', diğer varlıklar yüzde '+num(otherShare,1):''}.`;
  return `<section class="panel asset-allocation"><div class="allocation-report-head"><div><span class="eyebrow">PORTFÖY DAĞILIMI</span><h2>Varlıkların yüzdesel dağılımı</h2><p>Tahmini net TL değerine göre portföydeki pay oranları</p></div><span class="allocation-count">${valued.length}<small>değerlenen varlık</small></span></div>${valued.length?`<div class="allocation-report"><div class="allocation-chart-side"><div class="asset-donut" role="img" aria-label="${esc(donutLabel)}" style="background:conic-gradient(${segments.join(',')})"><div class="asset-donut-inner"><b>${esc(portfolio.ad)}</b><small>Net dağılım</small><span>${money(total)}</span></div></div>${otherValue>0?`<div class="allocation-other"><i style="background:${DISTRIBUTION_COLORS[5]}"></i><span>Diğer varlıklar</span><b>${num(otherShare,1)}%</b></div>`:''}</div><div class="allocation-leaders"><div class="allocation-leaders-title"><span class="allocation-list-icon">${icon('chart')}</span><div><h3>En çok orana sahip varlıklar</h3><small>İlk ${leaders.length} varlık</small></div></div><div class="allocation-leader-list">${leaders.map(({a,v},i)=>`<button class="allocation-asset-row" data-action="detail" data-id="${esc(a.id)}"><span class="allocation-rank" style="--slice-color:${DISTRIBUTION_COLORS[i]};--rank-ink:${i>=3?'#183b37':'#fff'}">${i+1}</span><span class="allocation-asset-name"><b>${esc(assetName(a))}</b><small>${esc(TYPES[a.tip].name)} · ${money(v.value)}</small></span><strong>${num(v.value/total*100,1)}%</strong></button>`).join('')}</div></div></div>`:empty('Dağılım oluşturulamadı','Değeri hesaplanabilen varlık eklendiğinde halka grafik burada görünür.')}${missing?`<div class="allocation-note">${missing} varlık, fiyatı veya değeri olmadığı için dağılıma katılmadı.</div>`:''}</section>`;
}
const closeCost=a=>Number.isFinite(a.maliyetToplam)?a.maliyetToplam:a.miktar>0&&a.maliyetBirim>0?a.miktar*a.maliyetBirim:null;
const closeNetPnl=a=>Number.isFinite(a.netKz)?a.netKz:Number.isFinite(a.kz)?a.kz:null;
function closedProfit(a){
  const net=closeNetPnl(a),cost=closeCost(a),percent=net!=null&&cost>0?net/cost*100:null,gross=Number.isFinite(a.brutKz)?a.brutKz:Number.isFinite(a.stopajsizKz)?a.stopajsizKz:net,tax=Number.isFinite(a.stopajTutari)?a.stopajTutari:0;
  return `${pnl(net,percent)}${tax>0?`<small class="close-tax-detail">Stopajsız: ${money(gross)} · Stopaj: −${money(tax)}${a.stopajTahmini?' (tahmini)':''}</small>`:''}`;
}
const undoCloseButton=a=>canUndoClose(state,a)?action('undo-close',icon('history')+'Geri al','text-btn',`type="button" data-id="${esc(a.id)}" aria-label="${esc(assetName(a))} kapanışını geri al"`):'<span class="muted">—</span>';
function closedRecordsSection(id){
  const closed=state.kapaliPozisyonlar.filter(a=>a.portfoyId===id).sort((a,b)=>(b.kapanisTarihi||'').localeCompare(a.kapanisTarihi||''));
  return `<section class="panel spaced portfolio-closed"><div class="panel-head"><div><h2>Kapatılan kayıtlar</h2><p>Gerçekleşmiş net kâr/zarar ve güvenli geri alma</p></div><span class="count">${closed.length}</span></div>${closed.length?`<div class="table-wrap"><table><thead><tr><th>Kapanış tarihi</th><th>Varlık</th><th class="numeric">Satılan miktar</th><th class="numeric">Net satış tutarı</th><th class="numeric">Net kâr / zarar</th><th>Nakit kaydı</th><th>İşlem</th></tr></thead><tbody>${closed.map(a=>`<tr><td>${dateLabel(a.kapanisTarihi?.slice(0,10))}</td><td><b>${esc(assetName(a))}</b><small class="cell-sub">${a.parcali?'Parçalı satış':'Tam kapanış'}</small></td><td class="numeric">${num(a.miktar,6)} <small>${esc(TYPES[a.tip]?.unit||'')}</small></td><td class="numeric">${money(a.netSatis??(a.miktar*a.kapanisFiyat-(a.komisyon||0)))}</td><td class="numeric">${closedProfit(a)}</td><td>${a.creditCash?'TL varlığına aktarıldı':'Aktarılmadı'}</td><td>${undoCloseButton(a)}</td></tr>`).join('')}</tbody></table></div>`:empty('Kapatılan kayıt yok','Bu portföyde kapatılan varlıklar burada gösterilecek.')}</section>`;
}
function holdings(){
  const allAssets=scopeAssets(),t=totals(state,pid),portfolio=pid==='all'?null:state.portfoyler.find(p=>p.id===pid);
  let a=allAssets;if(search)a=a.filter(x=>(assetName(x)+' '+TYPES[x.tip].name+' '+(BANKS[x.banka]||'')).toLocaleLowerCase('tr').includes(search.toLocaleLowerCase('tr')));
  const refreshScope=pid==='all'?'all':'portfolio';
  return (portfolio?portfolioHero(portfolio,t)+assetAllocation(allAssets,portfolio)+trendChart('portfolio',pid,'Portföy değer değişimi'):'')+warnings(t)+`<section class="panel"><div class="panel-head holdings-panel-head"><h2>Varlık listesi <span class="count">${a.length}</span></h2><div class="holdings-tools"><label class="search">${icon('search')}<input id="asset-search" placeholder="Varlık veya banka ara" aria-label="Varlık ara" value="${esc(search)}"></label><div class="holdings-buttons">${refreshButton(refreshScope)}${action('add-asset',icon('plus')+'Yeni varlık ekle','primary')}</div></div></div>${a.length?assetsTable(a):empty('Henüz kayıt yok','Bu portföye bir varlık ekleyebilirsin.')}</section>${portfolio?closedRecordsSection(pid):''}`;
}
function deposits(){
  const a=scopeAssets().filter(x=>x.tip==='vadeli_mevduat');
  return `<div class="deposit-intro"><div>${icon('bank')}<span><b>Anapara + kaydedilen faiz − çekilen tutar</b><small>Faiz oranı, stopaj veya bileşik getiri hesaplanmaz. Bankadaki net artışı kaydet.</small></span></div>${action('add-deposit',icon('plus')+'<span>Hesap ekle</span>','primary deposit-add','aria-label="Mevduat hesabı ekle"')}</div><div class="deposit-grid">${a.map(x=>{const d=depositSummary(x),todayEntry=x.islemler.find(i=>i.tur==='getiri'&&i.tarih.slice(0,10)===today());return `<section class="panel deposit-card"><div class="deposit-head"><div class="asset-icon mint">${icon('bank')}</div><div><h2>${esc(assetName(x))}</h2><small>${esc(BANKS[x.banka]||x.banka)} · ${esc(portfolioName(x.portfoyId))}</small></div>${action('edit-asset',icon('edit'),'icon-btn',`data-id="${esc(x.id)}" aria-label="Hesabı düzenle"`)}</div><span class="mini-label">GÜNCEL MEVDUAT BAKİYESİ</span><strong class="balance">${money(d.balance)}</strong><div class="deposit-split"><div><span>Yatırılan anapara</span><b>${money(d.principal)}</b></div><div><span>Kaydedilen faiz</span><b class="positive">+${money(d.interest)}</b></div></div>${d.withdrawals?`<p class="muted">Toplam çekim: ${money(d.withdrawals)}</p>`:''}<div class="today-status ${todayEntry?'done':''}">${icon(todayEntry?'check':'history')}${todayEntry?'Bugünün faizi kaydedildi: '+money(todayEntry.tutar):'Bugün henüz faiz kaydı yok'}</div><div class="card-actions">${action('interest',icon(todayEntry?'edit':'plus')+(todayEntry?'Bugünkü faizi düzenle':'Günlük faiz ekle'),'primary',`data-id="${esc(x.id)}" ${todayEntry?`data-entry="${esc(todayEntry.id)}"`:''}`)}${action('detail','Hesap hareketleri','secondary',`data-id="${esc(x.id)}"`)}</div></section>`;}).join('')}</div>${a.length?'':empty('İlk mevduat hesabını ekle','Anaparanı kaydet; her gün gelen faiz tutarını ayrı ekle.',action('add-deposit','Mevduat hesabı ekle','primary'))}`;
}
function bankPage(){
  const type=bankMetal==='gold'?'banka_altin':'banka_gumus';
  const entries=Object.entries(BANKS).filter(([k])=>k!=='diger');
  const available=entries.filter(([k])=>state.quotes[type+'_'+k]?.bid).length;
  return `<div class="alert">${icon('shield')}<span>Listede ${entries.length} banka var. <b>Her banka altın ve gümüş kuru yayımlamayabilir.</b> Otomatik kapsam kaynakla sınırlıdır; eksik kurlar elle girilebilir. Bunlar bankanın kesin işlem fiyatı değildir.</span></div><section class="panel blue-surface bank-prices-panel"><div class="panel-head"><div class="segmented"><button data-action="metal" data-metal="gold" class="${bankMetal==='gold'?'selected':''}">Gram altın</button><button data-action="metal" data-metal="silver" class="${bankMetal==='silver'?'selected':''}">Gram gümüş</button></div><span class="muted">${available} / ${entries.length} kayıtlı fiyat</span></div><div class="table-wrap"><table><thead><tr><th>Banka</th><th class="numeric">Banka alış <small>(sen satarsan)</small></th><th class="numeric">Banka satış <small>(sen alırsan)</small></th><th class="numeric">Makas</th><th>Durum / Alınma zamanı</th><th></th></tr></thead><tbody>${entries.map(([k,name])=>{const key=type+'_'+k,q=state.quotes[key];return `<tr><td><div class="bank-name"><span class="bank-monogram">${esc(name.slice(0,2).toLocaleUpperCase('tr-TR'))}</span><b>${name}</b></div></td><td class="numeric">${q?.bid?money(q.bid):'—'}</td><td class="numeric">${q?.ask?money(q.ask):'—'}</td><td class="numeric">${q?.ask&&q?.bid?num((q.ask-q.bid)/q.ask*100,2)+'%':'—'}</td><td>${badge(q)}<small class="cell-sub">${q?.fetchedAt?stamp(q.fetchedAt):'Veri alınmadı'}</small></td><td>${action('price',icon('edit'),'icon-btn',`data-key="${key}" data-label="${name} ${bankMetal==='gold'?'altın':'gümüş'}" aria-label="${name} fiyatını gir"`)}</td></tr>`;}).join('')}</tbody></table></div></section><p class="fineprint">Makas = (satış − alış) / satış × 100. Portföy değerlemesinde banka alış kuru kullanılır. Kaynak alış veya satış tarafını doğrulayamazsa sayı gösterilmez.</p>${sourceReport.length?report():''}`;
}
function report(){return `<section class="panel report"><h2>Son bağlantı denemesi</h2>${sourceReport.map(s=>`<div><span class="badge ${s.ok?'received':'missing'}">${s.ok?'Alındı':'Alınamadı'}</span><b>${esc(s.name)}</b><small>${esc(s.ok?s.count+' fiyat kaydı':s.message)}</small></div>`).join('')}</section>`;}
function history(){
  const list=scopeAssets().flatMap(a=>(a.islemler||[]).map(i=>({a,i}))).sort((x,y)=>y.i.tarih.localeCompare(x.i.tarih));
  const closed=state.kapaliPozisyonlar.filter(a=>pid==='all'||a.portfoyId===pid).sort((a,b)=>(b.kapanisTarihi||'').localeCompare(a.kapanisTarihi||''));
  const name={alis:'Alım kaydı',temettu:'Temettü',yatirma:'Anapara yatırma',getiri:'Faiz girişi',cekme:'Para çekme'};
  return `<section class="panel blue-surface history-movements-panel"><div class="panel-head"><h2>Hesap hareketleri</h2><span class="muted">${list.length} kayıt</span></div>${list.length?`<div class="table-wrap"><table><thead><tr><th>Tarih</th><th>Varlık / Hesap</th><th>İşlem</th><th class="numeric">Tutar</th><th></th></tr></thead><tbody>${list.map(({a,i})=>`<tr><td>${dateLabel(i.tarih.slice(0,10))}</td><td><b>${esc(assetName(a))}</b><small class="cell-sub">${esc(portfolioName(a.portfoyId))}</small></td><td>${name[i.tur]}</td><td class="numeric ${['getiri','temettu'].includes(i.tur)?'positive':''}">${money(i.tur==='alis'?i.miktar*i.fiyat+(i.komisyon||0):i.tutar)}</td><td>${action('edit-entry',icon('edit'),'icon-btn',`data-id="${esc(a.id)}" data-entry="${esc(i.id)}" aria-label="Kaydı düzenle"`)}</td></tr>`).join('')}</tbody></table></div>`:empty('Henüz işlem kaydı yok','Alım, temettü ve mevduat hareketleri burada listelenir.')}</section><section class="panel spaced blue-surface history-closed-panel"><div class="panel-head"><h2>Kapatılmış varlık kayıtları</h2><span class="muted">${closed.length} kayıt</span></div>${closed.length?`<div class="table-wrap"><table><thead><tr><th>Kapanış tarihi</th><th>Varlık</th><th class="numeric">Satılan miktar</th><th class="numeric">Net satış tutarı</th><th class="numeric">Net kâr / zarar</th><th>Nakit kaydı</th><th>İşlem</th></tr></thead><tbody>${closed.map(a=>`<tr><td>${dateLabel(a.kapanisTarihi?.slice(0,10))}</td><td>${esc(assetName(a))}<small class="cell-sub">${a.parcali?'Parçalı satış':'Tam kapanış'}</small></td><td class="numeric">${num(a.miktar,6)} <small>${esc(TYPES[a.tip]?.unit||'')}</small></td><td class="numeric">${money(a.netSatis??(a.miktar*a.kapanisFiyat-(a.komisyon||0)))}</td><td class="numeric">${closedProfit(a)}</td><td>${a.creditCash?'TL varlığına aktarıldı':'Aktarılmadı'}</td><td>${undoCloseButton(a)}</td></tr>`).join('')}</tbody></table></div>`:empty('Kapalı kayıt yok','Kapatılan varlıkların geçmişi burada korunur.')}</section>`;
}
function settings(){return `<div class="settings-grid"><section class="panel padded"><div class="section-icon">${icon('download')}</div><h2>Yedekle ve geri yükle</h2><p class="muted">Kayıtlar bu uygulamada saklanır. Uygulamayı silmek veya verilerini temizlemek kayıtları silebilir. Düzenli JSON yedeği al.</p><div class="stack">${action('backup',icon('download')+'JSON yedeği kaydet','primary')}${loadError?action('recovery-backup','Ham kurtarma kopyasını kaydet'):''}${action('import-backup','Eski / yeni JSON yedeğini yükle')}${action('export-csv','Varlıkları CSV olarak dışa aktar')}</div><small class="fineprint">v3 yedeğindeki portföyler, varlıklar, işlem defteri ve kapalı kayıtlar korunur. Eski fiyatlar doğrulanmadı etiketiyle taşınır.</small></section><section class="panel padded"><div class="section-icon">${icon('wallet')}</div><h2>Portföylerini yönet</h2><div class="portfolio-settings">${state.portfoyler.map(p=>`<div><b>${esc(p.ad)}</b><span>${action('edit-portfolio',icon('edit'),'icon-btn',`data-id="${esc(p.id)}" aria-label="Portföyü düzenle"`)}${action('delete-portfolio',icon('trash'),'icon-btn danger-text',`data-id="${esc(p.id)}" aria-label="Portföyü sil"`)}</span></div>`).join('')}</div>${action('add-portfolio',icon('plus')+'Yeni portföy')}</section><section class="panel padded"><div class="section-icon">${icon('chart')}</div><h2>Fiyat bağlantısı</h2><p>TEFAS ve banka istekleri doğrudan telefonun HTTPS bağlantısıyla gönderilir. Bilgisayar veya ayrı sunucu gerekmez. Kaynağın erişim engeli ya da kesintisi yine de olabilir.</p><p>Fon fiyatları anlık borsa fiyatı değildir. Resmi sayfadaki fon kodu ve son fiyat alanı birlikte kontrol edilir.</p><div class="stack">${action('import-prices','Fon fiyatlarını CSV ile içe aktar')}${action('refresh',icon('refresh')+'Bağlantıyı dene','secondary',busy?'disabled':'')}</div><p class="fineprint">CSV başlıkları: <code>Fon Kodu;Fiyat;Tarih</code><br>Örnek: <code>ABC;1,234567;31.08.2026</code><br>Yalnızca kayıtlı fonlar güncellenir; önizleme ve onay gösterilir.</p></section><section class="panel padded"><div class="section-icon">${icon(theme==='dark'?'sun':'moon')}</div><h2>Görünüm</h2><p>${theme==='dark'?'Koyu mod açık. Yazılar, butonlar ve kartlar koyu zeminde yüksek karşıtlıkla gösteriliyor.':'Açık mod kullanılıyor. İstersen koyu görünüme geçebilirsin.'}</p><div class="stack">${action('toggle-theme',icon(theme==='dark'?'sun':'moon')+(theme==='dark'?'Açık moda geç':'Koyu modu aç'),'secondary')}</div></section><section class="panel padded"><div class="section-icon">${icon('shield')}</div><h2>Hesaplama kuralları</h2><ul class="rules"><li>Dönemsel K/Z, karşılaştırılabilir toplam kazanç kayıtları arasındaki farktır; para yatırma kazanç sayılmaz. Oran, dönem başı net değere göredir; zaman ağırlıklı yatırım getirisi değildir. Eski veya eksik geçmişte tahmin gösterilmez.</li><li>Mevduat = yatırılan anapara + girilen faiz − çekim.</li><li>Aynı hesap ve günde tek faiz kaydı vardır. İkinci giriş yerine eski kayıt düzenlenir.</li><li>Vadeli mevduatta faiz oranı, stopaj veya otomatik bileşik getiri hesaplanmaz.</li><li>Stopaj işaretli TEFAS fonunda yalnız pozitif kâra, kullanıcı tarafından kaydedilen oran uygulanır; portföy toplamları tahmini net değeri gösterir.</li><li>Banka madenlerinde değerleme alış fiyatıyla yapılır.</li><li>Maliyet bilinmiyorsa ilgili varlık kâr/zarara katılmaz.</li><li>Önceki sürümle uyumlu olarak temettü, etkin maliyetten düşülür; ayrıca nakde eklenmez.</li><li>Fiyat bulunamazsa toplamın eksik olduğu belirtilir.</li></ul></section></div>${sourceReport.length?report():''}`;}
function render(){
  if(pid!=='all'&&!state.portfoyler.some(p=>p.id===pid))pid='all';
  const pages={overview,holdings,deposits,banks:bankPage,history,settings};
  $('#app').innerHTML=shell(pages[view]());
  document.querySelectorAll?.('table').forEach(table=>{const headers=Array.from(table.querySelectorAll('th'),h=>h.textContent);table.querySelectorAll('tbody tr').forEach(row=>{Array.from(row.children).forEach((cell,i)=>cell.dataset.label=headers[i]||'');});});
}

const modal=$('#modal');let modalState=null;
function showModal(title,body,onSubmit,wide=false){
  modal.className=wide?'wide':'';
  modal.innerHTML=`<form id="modal-form"><div class="modal-head"><div><span class="eyebrow">PORTFÖYÜM</span><h2>${esc(title)}</h2></div><button type="button" class="btn icon-btn" data-action="dismiss" aria-label="Kapat">${icon('close')}</button></div><div class="modal-body">${body}<div class="form-error" role="alert"></div></div></form>`;
  modalState={onSubmit};if(!modal.open)modal.showModal();
  const f=$('#modal-form');f.onsubmit=async e=>{e.preventDefault();const submit=f.querySelector('button[type="submit"]');if(submit)submit.disabled=true;try{await modalState.onSubmit?.(new FormData(f));}catch(err){$('.form-error',modal).textContent=err.message;}finally{if(submit)submit.disabled=false;}};
}
const saveButton=(label='Kaydet')=>`<div class="form-footer"><button type="button" class="btn secondary" data-action="dismiss">Vazgeç</button><button type="submit" class="btn primary">${icon('check')}${label}</button></div>`;
const val=(f,k)=>String(f.get(k)||'').trim();
const optionalAmount=(f,k,label)=>val(f,k)?amount(val(f,k),label,true):0;
function portfolioDeleteModal(id){
  const portfolio=state.portfoyler.find(p=>p.id===id);if(!portfolio)throw new Error('Portföy bulunamadı.');
  const assetCount=state.varliklar.filter(a=>a.portfoyId===id).length;
  const closedCount=state.kapaliPozisyonlar.filter(a=>a.portfoyId===id).length;
  showModal('Bu portföyü silmek istediğinizden emin misiniz?',`<div class="delete-confirm"><div class="delete-confirm-icon">${icon('trash')}</div><div><b>${esc(portfolio.ad)}</b><p>Bu portföy, içindeki ${assetCount} varlık ve ${closedCount} kapalı kayıt silinecek. Bu işlem geri alınamaz.</p></div></div><div class="form-footer"><button type="button" class="btn secondary" data-action="dismiss">Hayır</button><button type="button" class="btn danger-confirm" data-action="confirm-delete-portfolio" data-id="${esc(id)}">${icon('trash')}Evet</button></div>`,null);
}
function undoCloseModal(id){
  const record=state.kapaliPozisyonlar.find(r=>r.id===id);if(!record)throw new Error('Kapanış kaydı bulunamadı.');
  showModal('Kapanış kaydı geri alınsın mı?',`<div class="delete-confirm"><div class="delete-confirm-icon">${icon('history')}</div><div><b>${esc(assetName(record))}</b><p>Satılan ${num(record.miktar,6)} ${esc(TYPES[record.tip]?.unit||'')} yeniden açık varlığa eklenecek${record.creditCash&&record.netSatis>0?' ve bu satıştan oluşan '+money(record.netSatis)+' TL nakit kaydı kaldırılacak':''}. Sonraki işlemlerin korunması için yalnız en son kapanış geri alınabilir.</p></div></div><div class="form-footer"><button type="button" class="btn secondary" data-action="dismiss">Hayır</button><button type="button" class="btn danger-confirm" data-action="confirm-undo-close" data-id="${esc(id)}">${icon('history')}Evet, geri al</button></div>`,null);
}
function undoClose(id){commit(undoCloseHolding(state,id));modal.close();toast('Kapanış geri alındı; varlık ve bağlı nakit kaydı düzeltildi.');}
function deletePortfolio(id){
  if(!state.portfoyler.some(p=>p.id===id))throw new Error('Portföy bulunamadı.');
  commit({...state,portfoyler:state.portfoyler.filter(x=>x.id!==id),varliklar:state.varliklar.filter(x=>x.portfoyId!==id),kapaliPozisyonlar:state.kapaliPozisyonlar.filter(x=>x.portfoyId!==id)});
  if(pid===id)pid='all';modal.close();toast('Portföy silindi.');
}
function assetModal(id=null,forcedType=null){
  const old=assetById(id);let type=old?.tip||forcedType||'hisse';
  if(!state.portfoyler.length){portfolioModal();return;}
  const body=()=>`${select('portfoyId','Portföy',Object.fromEntries(state.portfoyler.map(p=>[p.id,p.ad])),old?.portfoyId||(pid==='all'?state.portfoyler[0].id:pid))}${old?`<input type="hidden" name="tip" value="${type}"><div class="type-fixed">${TYPES[type].name}</div>`:select('tip','Varlık türü',Object.fromEntries(Object.entries(TYPES).map(([k,v])=>[k,v.name])),type)}<div id="asset-fields"></div>`;
  showModal(old?'Varlığı düzenle':type==='vadeli_mevduat'?'Mevduat hesabı ekle':'Yeni varlık ekle',body()+saveButton(),f=>{
    const a={...(old||{}),id:old?.id||uid(),portfoyId:val(f,'portfoyId'),tip:type,ad:val(f,'ad')};
    if(TYPES[type].bank)a.banka=val(f,'banka');
    if(TYPES[type].symbol){a.sembol=val(f,'sembol').toUpperCase().replace(/\.IS$/,'');if(!/^[A-Z0-9]{2,12}$/.test(a.sembol))throw new Error('Geçerli bir sembol girin.');}
    if(type==='tefas_fon'){
      a.fonTip=val(f,'fonTip');a.stopajVar=f.has('stopajVar');
      a.stopajOrani=a.stopajVar?percentage(val(f,'stopajOrani'),'Stopaj oranı'):percentage(old?.stopajOrani??17.5,'Stopaj oranı');
    }else {delete a.stopajVar;delete a.stopajOrani;}
    if(type==='vadeli_mevduat'){
      if(!old){const d=val(f,'tarih');if(!validDay(d)||d>today())throw new Error('Geçerli bir başlangıç tarihi seçin.');a.islemler=[{id:uid(),tur:'yatirma',tutar:round(amount(val(f,'anapara'),'Anapara')),tarih:d}];a.miktar=1;}
    }else if(!old?.islemler?.length&&!old?.satilanMiktar){
      a.miktar=amount(val(f,'miktar'),'Miktar');a.maliyetBirim=type==='try_lirasi'?1:optionalAmount(f,'maliyetBirim','Alış fiyatı');a.alisKomisyon=optionalAmount(f,'alisKomisyon','Komisyon');
    }
    a.not=val(f,'not');commitAsset(a);modal.close();toast('Varlık kaydedildi.');
  });
  const fields=()=>{
    $('#asset-fields').innerHTML=`${TYPES[type].symbol?input('sembol',type==='tefas_fon'?'Fon kodu':'Hisse sembolü',old?.sembol||'','required maxlength="12" placeholder="'+(type==='tefas_fon'?'AFA':'THYAO')+'"'):''}${type==='tefas_fon'?select('fonTip','Fon türü',{YAT:'Yatırım fonu',EMK:'Emeklilik fonu',BYF:'Borsa yatırım fonu'},old?.fonTip||'YAT'):''}${input('ad',type==='vadeli_mevduat'?'Hesap adı':'Görünen ad (isteğe bağlı)',old?.ad||'','maxlength="100" '+(type==='vadeli_mevduat'?'required placeholder="Örneğin: Günlük mevduat"':''))}${TYPES[type].bank?select('banka','Banka',BANKS,old?.banka||'ziraat'):''}${type==='vadeli_mevduat'?(old?note('Bakiyeyi ve faizi değiştirmek için hesap hareketlerini kullan.'):decimal('anapara','Başlangıç anaparası (TL)')+input('tarih','Başlangıç tarihi',today(),'type="date" required max="'+today()+'"')+note('Faiz sadece sen girdiğinde eklenecek. Faiz oranı hesaplanmaz.')):(old?.islemler?.length||old?.satilanMiktar)?note('Miktar ve maliyet; işlem kayıtları ile parçalı satışlardan hesaplanıyor. Varlık ayrıntılarından yeni işlem veya satış kaydı ekleyebilirsin.'):`<div class="form-grid">${decimal('miktar','Miktar ('+TYPES[type].unit+')',old?.miktar??'')}${type==='try_lirasi'?'':decimal('maliyetBirim','Birim alış fiyatı (TL, isteğe bağlı)',old?.maliyetBirim||'',false)}</div>${type==='try_lirasi'?'':decimal('alisKomisyon','Toplam alış komisyonu (TL)',old?.alisKomisyon||'',false)}`}${type==='tefas_fon'?`<div class="withholding-settings"><label class="check-field"><input type="checkbox" name="stopajVar" ${old?.stopajVar?'checked':''}> Bu fonda stopaj var mı?</label><div id="stopaj-rate-field" ${old?.stopajVar?'':'hidden'}>${input('stopajOrani','Stopaj oranı (%)',old?.stopajOrani??'17,5','inputmode="decimal" autocomplete="off" placeholder="17,5"')}</div><small>Yalnızca pozitif kâr üzerinden tahmini net değer hesaplanır. Kesin kesinti için yatırım kuruluşu kaydını esas al.</small></div>`:''}${input('not','Not (isteğe bağlı)',old?.not||'','maxlength="250"')}`;
    const withholdingToggle=$('[name="stopajVar"]',modal),rateField=$('#stopaj-rate-field',modal);
    if(withholdingToggle&&rateField){const sync=()=>{rateField.hidden=!withholdingToggle.checked;};withholdingToggle.onchange=sync;sync();}
  };
  fields();$('[name="tip"]',modal)?.addEventListener('change',e=>{type=e.target.value;fields();});
}
function portfolioModal(id){
  const p=state.portfoyler.find(x=>x.id===id);
  showModal(p?'Portföyü düzenle':'Yeni portföy',input('ad','Portföy adı',p?.ad||'','required maxlength="100"')+input('renk','Renk',p?.renk||COLORS[state.portfoyler.length%COLORS.length],'type="color"')+saveButton(),f=>{
    const name=val(f,'ad');if(!name)throw new Error('Portföy adı boş olamaz.');const n={id:p?.id||uid(),ad:name,renk:val(f,'renk')};
    commit({...state,portfoyler:p?state.portfoyler.map(x=>x.id===p.id?n:x):[...state.portfoyler,n]});modal.close();toast('Portföy kaydedildi.');
  });
}
function priceModal(key,label){
  const q=state.quotes[key],bank=key.startsWith('banka_');
  showModal('Fiyatı elle güncelle',`<h3>${esc(label)}</h3><div class="form-grid">${decimal('bid',bank?'Banka alış (TL / gr)':'Birim fiyat (TL)',q?.bid??'')}${bank?decimal('ask','Banka satış (TL / gr, isteğe bağlı)',q?.ask??'',false):''}</div>${input('date','Fiyat tarihi',q?.priceDate?.slice(0,10)||today(),'type="date" required max="'+today()+'"')}<label class="check-field"><input type="checkbox" name="locked" ${q?.locked!==false?'checked':''}> Otomatik güncelleme bu elle girilen fiyatı değiştirmesin</label>${note('Fiyat elle girildi olarak işaretlenir. Otomatik fiyatlara dönmek için kilidi kaldırıp fiyatları yenile.')}${saveButton()}`,f=>{
    const bid=amount(val(f,'bid'),'Alış fiyatı'),ask=bank&&val(f,'ask')?amount(val(f,'ask'),'Satış fiyatı'):null;
    if(ask!=null&&ask<bid)throw new Error('Banka satış fiyatı alıştan küçük olamaz.');
    const d=val(f,'date');if(!validDay(d)||d>today())throw new Error('Fiyat tarihi geçersiz.');
    commit({...state,quotes:{...state.quotes,[key]:{bid,ask,priceDate:d,fetchedAt:new Date().toISOString(),status:'manual',source:'Elle giriş',locked:f.has('locked')}}});modal.close();toast('Fiyat kaydedildi; tüm toplamlar güncellendi.');
  });
}
function depositEntryModal(id,entryId=null,type='getiri'){
  const a=assetById(id);if(!a)throw new Error('Mevduat hesabı bulunamadı.');const entry=a.islemler?.find(i=>i.id===entryId);type=entry?.tur||type;
  showModal(entry?'Mevduat kaydını düzenle':type==='getiri'?'Günlük faiz ekle':'Mevduat hareketi',`<div class="account-context"><b>${esc(assetName(a))}</b><span>Mevcut bakiye: ${money(depositSummary(a).balance)}</span></div>${select('tur','İşlem türü',{getiri:'Faiz girişi',yatirma:'Anapara yatırma',cekme:'Para çekme'},type)}${decimal('tutar','Eklenecek / çekilecek net tutar (TL)',entry?.tutar??'')}${input('tarih','Kayıt tarihi',entry?.tarih.slice(0,10)||today(),'type="date" required max="'+today()+'"')}${input('not','Not (isteğe bağlı)',entry?.not||'','maxlength="250"')}${note('Faiz girişi için birikmiş toplam faizi değil, o gün eklenen tutarı yaz. Aynı gün ikinci faiz kaydı oluşturulmaz.')}${saveButton(entry?'Kaydı güncelle':'Tutarı kaydet')}`,f=>{
    const current=assetById(id);const updated=saveDepositEntry(current,{id:entry?.id,tur:val(f,'tur'),tarih:val(f,'tarih'),tutar:val(f,'tutar'),not:val(f,'not')});
    commitAsset(updated);modal.close();toast('Kaydedildi. Mevduat ve portföy toplamı güncellendi.');
  });
}
function holdingEntryModal(id,entryId=null,type='alis'){
  const a=assetById(id),entry=a?.islemler?.find(i=>i.id===entryId);if(!a)throw new Error('Varlık bulunamadı.');type=entry?.tur||type;
  const fields=()=>type==='alis'?`<div class="form-grid">${decimal('miktar','Miktar',entry?.miktar??'')}${decimal('fiyat','Birim alış fiyatı (TL)',entry?.fiyat??'')}</div>${decimal('komisyon','Komisyon (TL)',entry?.komisyon??'',false)}`:decimal('tutar','Alınan net temettü (TL)',entry?.tutar??'')+note('Temettü etkin maliyeti düşürür; ayrıca nakit varlığına eklenmez.');
  showModal(entry?'İşlem kaydını düzenle':'İşlem ekle',`<h3>${esc(assetName(a))}</h3>${select('tur','İşlem türü',{alis:'Alım kaydı',temettu:'Temettü'},type)}<div id="entry-fields">${fields()}</div>${input('tarih','İşlem tarihi',entry?.tarih.slice(0,10)||today(),'type="date" required max="'+today()+'"')}${input('not','Not',entry?.not||'','maxlength="250"')}${saveButton()}`,f=>{
    const current=assetById(id);const d=val(f,'tarih');if(!validDay(d)||d>today())throw new Error('Tarih geçersiz.');
    const t={id:entry?.id||uid(),tur:type,tarih:d,not:val(f,'not')};
    if(type==='alis'){t.miktar=amount(val(f,'miktar'),'Miktar');t.fiyat=amount(val(f,'fiyat'),'Alış fiyatı');t.komisyon=optionalAmount(f,'komisyon','Komisyon');}else t.tutar=round(amount(val(f,'tutar')));
    const list=seedHolding(current).filter(i=>i.id!==t.id).concat(t);commitAsset({...current,islemler:list});modal.close();toast('İşlem kaydedildi.');
  });
  $('[name="tur"]',modal).onchange=e=>{type=e.target.value;$('#entry-fields').innerHTML=fields();};
}
function detailModal(id){
  const a=assetById(id);if(!a)throw new Error('Varlık bulunamadı.');const d=a.tip==='vadeli_mevduat',v=valuation(a,state.quotes);
  const entryNames={alis:'Alım',temettu:'Temettü',getiri:'Faiz',yatirma:'Anapara',cekme:'Çekim'};
  const list=[...(a.islemler||[])].sort((a,b)=>b.tarih.localeCompare(a.tarih));
  const summary=v.withholdingEnabled?`<div class="detail-summary withholding-summary"><div><span>Tahmini net değer</span><b>${money(v.value)}</b></div><div><span>Net kâr / zarar</span><b>${pnl(v.pnl,v.percent)}</b></div><div><span>Stopajsız kâr / zarar</span><b>${pnl(v.grossPnl,v.grossPercent)}</b></div><div><span>Tahmini stopaj (${num(v.withholdingRate,2)}%)</span><b>${v.withholding==null?'—':(v.withholding>0?'−':'')+money(v.withholding)}</b></div></div>`:`<div class="detail-summary"><div><span>Kayıtlı değer</span><b>${money(v.value)}</b></div><div><span>${d?'Toplam faiz':'Kâr / zarar'}</span><b>${pnl(v.pnl,v.percent)}</b></div></div>`;
  showModal(assetName(a),`${summary}<div class="detail-meta"><span>${esc(portfolioName(a.portfoyId))}${v.withholdingEnabled?' · Stopajlı TEFAS':''}</span>${badge(v.quality)}</div>${trendChart('asset',a.id,d||a.tip==='try_lirasi'?'Değer değişimi':'Fiyat değişimi',true)}${!d&&a.tip!=='try_lirasi'?`<p class="fineprint">Fiyat kaynağı: ${esc(v.quote?.source||'Henüz yok')} · Fiyat tarihi: ${v.quote?.priceDate?dateLabel(v.quote.priceDate.slice(0,10)):'Kaynak tarihi yok'}<br>Alınma: ${stamp(v.quote?.fetchedAt)}${v.quote?.error?'<br>'+esc(v.quote.error):''}</p>`:''}<div class="detail-tools">${action(d?'interest':'add-entry',icon('plus')+(d?'Faiz ekle':'İşlem ekle'),'primary',`type="button" data-id="${esc(id)}"`)}${d?action('deposit-move','Yatır / çek','secondary',`type="button" data-id="${esc(id)}"`):a.tip!=='try_lirasi'?action('price','Fiyat gir','secondary',`type="button" data-key="${esc(quoteKey(a))}" data-label="${esc(assetName(a))}"`):''}${action('edit-asset','Düzenle','secondary',`type="button" data-id="${esc(id)}"`)}${!d&&a.tip!=='try_lirasi'?action('close-holding','Satış / kapanış','secondary',`type="button" data-id="${esc(id)}"`):''}</div>${!d&&a.maliyetManuel?note('Bu varlıkta önceki sürümden gelen elle maliyet ayarı açık. İşlemleri değiştirmek etkin maliyeti otomatik değiştirmez.')+action('auto-cost','Otomatik maliyete dön','secondary',`type="button" data-id="${esc(id)}"`):''}<h3 class="ledger-title">İşlem defteri</h3>${list.length?`<div class="ledger">${list.map(i=>`<div><span><b>${entryNames[i.tur]}</b><small>${dateLabel(i.tarih.slice(0,10))}${i.not?' · '+esc(i.not):''}${i.tur==='alis'?' · '+num(i.miktar,6)+' × '+num(i.fiyat,6)+' ₺':''}</small></span><b>${money(i.tur==='alis'?i.miktar*i.fiyat+(i.komisyon||0):i.tutar)}</b><div>${action('edit-entry',icon('edit'),'icon-btn',`type="button" data-id="${esc(id)}" data-entry="${esc(i.id)}" aria-label="İşlemi düzenle"`)}${action('delete-entry',icon('trash'),'icon-btn danger-text',`type="button" data-id="${esc(id)}" data-entry="${esc(i.id)}" aria-label="İşlemi sil"`)}</div></div>`).join('')}</div>`:note('Bu varlık başlangıç bakiyesi olarak kayıtlı. İşlem eklediğinde başlangıç bilgileri de deftere aktarılır.')}<div class="detail-delete">${action('delete-asset',icon('trash')+'Varlığı sil','text-btn danger-text',`type="button" data-id="${esc(id)}"`)}</div>`,null,true);
}
function closeModal(id){
  const a=assetById(id);if(!a)throw new Error('Varlık bulunamadı.');const h=holdingSummary(a);if(!(h.quantity>0))throw new Error('Kapatılabilecek varlık miktarı bulunamadı.');const unit=TYPES[a.tip].unit,minDate=closingDateFloor(state,id),taxed=a.tip==='tefas_fon'&&a.stopajVar===true;
  const taxField=taxed?`${decimal('withholding','Kesilen stopaj (TL, boşsa otomatik)','',false)}${note(`Boş bırakırsan pozitif kâr üzerinden %${num(a.stopajOrani??17.5,4)} stopaj hesaplanır. Yatırım kuruluşunun kestiği kesin tutar farklıysa buraya yazabilirsin.`)}`:'';
  showModal('Satış / kapanış kaydı',`<h3>${esc(assetName(a))}</h3><div class="account-context"><b>Mevcut miktar</b><span>${num(h.quantity,6)} ${unit}</span></div><div class="form-grid">${decimal('quantity','Satılan miktar ('+unit+')',num(h.quantity,6))}${decimal('price','Birim satış fiyatı (TL)',state.quotes[quoteKey(a)]?.bid??'')}</div>${decimal('fee','Bu satışın toplam komisyonu (TL)','',false)}${taxField}${input('date','Satış tarihi',today(),'type="date" required '+(minDate?'min="'+minDate+'" ':'')+'max="'+today()+'"')}<label class="check-field"><input type="checkbox" name="cash" checked> Komisyon ve stopaj sonrası net satış tutarını aynı portföye TL nakit olarak ekle</label>${note('Satılan miktar ve maliyet satış tarihindeki alışlara göre hesaplanır. Bir kısmını satarsan kalan miktar açık kalır. Hatalı kaydı daha sonra Kapatılan kayıtlar bölümünden güvenle geri alabilirsin. Bu yalnızca kayıt işlemidir; gerçek emir gönderilmez.')}${saveButton('Satışı kaydet')}`,f=>{const quantity=amount(val(f,'quantity'),'Satılan miktar'),partial=quantity<h.quantity-1e-8,tax=val(f,'withholding')?amount(val(f,'withholding'),'Kesilen stopaj',true):null;commit(closeHolding(state,id,val(f,'price'),optionalAmount(f,'fee','Komisyon'),val(f,'date'),f.has('cash'),quantity,tax));modal.close();toast(partial?'Parçalı satış kaydedildi; kalan miktar portföyde tutuldu.':'Varlığın tamamı kapatıldı.');});
}
async function download(name,text,type='application/json'){
  const result=await saveDocument(name,text,type);
  if(!result?.saved)throw new Error('Dosya kaydedilmedi.');
}
async function backup(){if(loadError)throw new Error('Kayıt okunamadığı için normal yedek oluşturulmadı. Mevcut kaydınız korunuyor. Ayarlardan ham kurtarma kopyasını kaydedin.');validateBackup(state);await download('Portfoyum-yedek-'+today()+'.json',JSON.stringify(state,null,2));toast('Yedek seçtiğin konuma kaydedildi.');}
async function recoveryBackup(){
  if(!loadError)throw new Error('Ham kurtarma kopyası yalnız kayıt sorunu olduğunda kullanılır.');
  const raw=localStorage.getItem(STORAGE)||localStorage.getItem('portfoy_v3');
  if(!raw)throw new Error('Kurtarılacak yerel kayıt bulunamadı.');
  await download('Portfoyum-HAM-KURTARMA-'+Date.now()+'.txt',raw,'text/plain');
  toast('Ham kayıt koruma kopyası kaydedildi. Bu dosya doğrulanmış JSON yedeği değildir.');
}
async function pickFile(accept,job){
  try{const result=await openDocument(accept);if(typeof result?.text!=='string')throw new Error('Dosya okunamadı.');await job(result.text.replace(/^\uFEFF/,''));}
  catch(e){toast(e.message,true);}
}
function importBackup(){return pickFile('.json',async text=>{
  let data;try{data=validateBackup(JSON.parse(text));}catch(e){throw new Error('Yedek yüklenemedi: '+e.message+' Mevcut kayıtlar değiştirilmedi.');}
  const effects=['portfoyler','varliklar','kapaliPozisyonlar'].reduce((total,key)=>{const before=new Map(state[key].map(x=>[x.id,x])),after=new Map(data[key].map(x=>[x.id,x]));for(const [id,item] of after){if(!before.has(id))total.added++;else if(JSON.stringify(item)!==JSON.stringify(before.get(id)))total.changed++;}for(const id of before.keys())if(!after.has(id))total.removed++;return total;},{added:0,changed:0,removed:0});
  showModal('Yedeği geri yükle',`<div class="import-summary"><b>${data.portfoyler.length} portföy</b><b>${data.varliklar.length} varlık</b><b>${data.kapaliPozisyonlar.length} kapalı kayıt</b></div><p class="restore-effects">Portföy, varlık ve kapanış kayıtları: <b>${effects.added} eklenecek · ${effects.changed} değişecek · ${effects.removed} silinecek</b>.</p>${note('Fiyatlar, işlem defterleri ve grafik geçmişi de seçtiğiniz yedekten alınır. Mevcut kayıtlar bu yedekle değiştirilecek. Devam etmeden önce mevcut verilerin yedeğini kaydetmen istenir. Kaydetmeyi iptal edersen geri yükleme yapılmaz.')}${saveButton('Yedeği geri yükle')}`,async()=>{
    const expected=localStorage.getItem(STORAGE),expectedLegacy=localStorage.getItem('portfoy_v3'),epoch=refreshEpoch;
    busy=true;refreshEpoch++;
    try{
    await download('Geri-yukleme-oncesi-'+Date.now()+'.json',loadError?(localStorage.getItem(STORAGE)||localStorage.getItem('portfoy_v3')||'{}'):JSON.stringify(state,null,2));
    if(localStorage.getItem(STORAGE)!==expected||localStorage.getItem('portfoy_v3')!==expectedLegacy||refreshEpoch!==epoch+1)throw new Error('Kayıtlar yedekleme sırasında değişti. Güvenlik için geri yükleme iptal edildi.');
    const checked=validateBackup(data);try{localStorage.setItem(STORAGE,JSON.stringify(checked));}catch{throw new Error('Yedek geri yüklenemedi: cihaz depolamasına yazılamadı. Mevcut kayıtlar değiştirilmedi.');}loadError='';state=checked;pid='all';modal.close();render();toast('Yedek geri yüklendi.');
    }finally{busy=false;render();}
  });
});}
async function exportCSV(){
  const cell=s=>'"'+String(s??'').replace(/^[=+@-]/,"'$&").replace(/"/g,'""')+'"';
  const rows=[['Portföy','Varlık','Tür','Miktar','Birim fiyat TL','Brüt değer TL','Tahmini stopaj TL','Net değer TL','Stopajsız kar zarar TL','Net kar zarar TL','Stopaj oranı %','Fiyat durumu']];
  for(const a of scopeAssets()){const v=valuation(a,state.quotes);rows.push([portfolioName(a.portfoyId),assetName(a),TYPES[a.tip].name,num(v.quantity,8),v.price==null?'':num(v.price,8),v.grossValue==null?'':num(v.grossValue,2),v.withholding==null?'':num(v.withholding,2),v.value==null?'':num(v.value,2),v.grossPnl==null?'':num(v.grossPnl,2),v.pnl==null?'':num(v.pnl,2),v.withholdingEnabled?num(v.withholdingRate,4):'',labels[v.quality]]);}
  await download('Portfoyum-'+today()+'.csv','\uFEFF'+rows.map(r=>r.map(cell).join(';')).join('\r\n'),'text/csv;charset=utf-8');
}
function importPrices(){return pickFile('.csv,.tsv,.txt',text=>{
  const lines=text.replace(/^\uFEFF/,'').trim().split(/\r?\n/);if(lines.length<2)throw new Error('CSV başlığı ve en az bir kayıt olmalı.');
  const sep=lines[0].includes(';')?';':lines[0].includes('\t')?'\t':',';
  const split=line=>{const out=[];let s='',quoted=false;for(let i=0;i<line.length;i++){const c=line[i];if(c==='"'){if(quoted&&line[i+1]==='"'){s+='"';i++;}else quoted=!quoted;}else if(c===sep&&!quoted){out.push(s.trim());s='';}else s+=c;}if(quoted)throw new Error('CSV tırnak işareti hatası.');out.push(s.trim());return out;};
  const norm=s=>s.toLocaleLowerCase('tr-TR').replace(/ı/g,'i').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-z]/g,'');
  const head=split(lines[0]).map(norm),ki=head.findIndex(h=>['fonkodu','fonkod','kod','symbol','code'].includes(h)),pi=head.findIndex(h=>['fiyat','birimpaydegeri','price','birimfiyat'].includes(h)),di=head.findIndex(h=>['tarih','date'].includes(h));
  if(ki<0||pi<0||di<0)throw new Error('Başlıklar Fon Kodu;Fiyat;Tarih olmalı.');
  const codes=new Set(state.varliklar.filter(a=>a.tip==='tefas_fon').map(a=>a.sembol)),updates={};let ignored=0;
  for(const line of lines.slice(1)){if(!line.trim())continue;const cols=split(line),code=cols[ki]?.toUpperCase();if(!codes.has(code)){ignored++;continue;}
    const bid=amount(cols[pi],'CSV fiyatı');let date=cols[di];if(/^\d{2}\.\d{2}\.\d{4}$/.test(date))date=date.split('.').reverse().join('-');if(!validDay(date)||date>today())throw new Error(code+' için CSV tarihi geçersiz.');
    const key='tefas_'+code;if(!updates[key]||date>updates[key].priceDate)updates[key]={bid,ask:bid,priceDate:date,fetchedAt:new Date().toISOString(),status:'manual',source:'CSV içe aktarma',locked:true};
  }
  if(!Object.keys(updates).length)throw new Error('Kayıtlı fonlarla eşleşen fiyat yok. Önce fonlarını ekle.');
  showModal('Fon fiyatlarını kontrol et',`<p>${ignored} satır kayıtlı fonlarla eşleşmedi ve atlandı.</p><div class="csv-preview">${Object.entries(updates).map(([k,q])=>`<div><b>${esc(k.slice(6))}</b><span>${num(q.bid,6)} ₺</span><small>${dateLabel(q.priceDate)}</small></div>`).join('')}</div>${note('Bu fiyatlar elle/CSV girişi olarak işaretlenir ve otomatik güncellemeye kilitlenir.')}${saveButton('Fiyatları uygula')}`,()=>{commit({...state,quotes:{...state.quotes,...updates}});modal.close();toast('Fon fiyatları içe aktarıldı.');});
});}
async function refresh(scope='all'){
  if(busy)return;
  const targetPid=scope==='portfolio'&&pid!=='all'?pid:null;
  const targetAssets=targetPid?state.varliklar.filter(a=>a.portfoyId===targetPid):state.varliklar;
  busy=true;render();const startedAt=Date.now(),epoch=refreshEpoch;
  try{
    if(!isAndroid)throw new Error('Android bağlantısı bulunamadı. Uygulamayı APK olarak aç.');
    const assets=targetAssets.map(({tip,sembol,banka,fonTip})=>({tip,sembol,banka,fonTip}));
    const result=await refreshPrices(assets,view==='banks');
    if(epoch!==refreshEpoch)return;
    const n=Object.keys(result.quotes||{}).length,fail=Object.keys(result.errors||{}).length;
    sourceReport=result.sources||[];
    const merged=mergeQuotes(state,result,startedAt);
    commit(n?recordSnapshot(merged,result.checkedAt):merged,n>0,result.checkedAt);
    toast(n?`${n} fiyat alındı.${fail?' Bazı fiyatlar alınamadı; eski kayıtlar korundu.':''}`:'Fiyat alınamadı. Kayıtlar korundu; bağlantı durumunu ayarlardan görebilirsin.',!n);
  }catch(e){if(epoch!==refreshEpoch)return;sourceReport=[{name:'Telefon fiyat bağlantısı',ok:false,message:e.message}];toast(e.message,true);}
  finally{busy=false;render();}
}

document.addEventListener('click',async e=>{
  const b=e.target.closest('[data-action]');if(!b)return;
  // Modal araç düğmeleri formu yanlışlıkla göndermesin.
  e.preventDefault();const {action:a,id,entry,key,label}=b.dataset;
  try{
    if(a==='dismiss')modal.close();
    else if(a==='nav'){view=b.dataset.view;if(view==='overview')pid='all';search='';render();window.scrollTo({top:0});}
    else if(a==='open-portfolio'){pid=id;view='holdings';search='';render();window.scrollTo({top:0});}
    else if(a==='scope'){pid=id;render();}
    else if(a==='metal'){bankMetal=b.dataset.metal;render();}
    else if(a==='toggle-theme')toggleTheme();
    else if(a==='chart-range'){
      const {scope,range}=b.dataset,card=b.closest('.trend-card');
      if(!card||!['day','week','month'].includes(range)||!['portfolio','asset'].includes(scope))return;
      chartRanges.set(chartRangeKey(scope,id),range);
      const wrapper=document.createElement('div');wrapper.innerHTML=trendChart(scope,id,card.dataset.chartTitle,card.dataset.chartCompact==='true');
      const replacement=wrapper.firstElementChild;card.replaceWith(replacement);
      replacement.querySelector(`[data-range="${range}"]`)?.focus({preventScroll:true});
    }
    else if(a==='refresh')refresh(b.dataset.refreshScope||'all');
    else if(a==='add-asset')assetModal();
    else if(a==='add-deposit')assetModal(null,'vadeli_mevduat');
    else if(a==='edit-asset')assetModal(id);
    else if(a==='add-portfolio')portfolioModal();
    else if(a==='edit-portfolio')portfolioModal(id);
    else if(a==='detail')detailModal(id);
    else if(a==='price')priceModal(key,label);
    else if(a==='interest')depositEntryModal(id,entry);
    else if(a==='deposit-move')depositEntryModal(id,null,'yatirma');
    else if(a==='add-entry')holdingEntryModal(id);
    else if(a==='edit-entry'){assetById(id)?.tip==='vadeli_mevduat'?depositEntryModal(id,entry):holdingEntryModal(id,entry);}
    else if(a==='delete-entry'){
      const item=assetById(id);if(!confirm('Bu işlem silinecek ve bütün toplamlar yeniden hesaplanacak. Devam edilsin mi?'))return;
      const list=item.islemler.filter(i=>i.id!==entry);if(item.tip==='vadeli_mevduat')validateDepositLedger(list);
      commitAsset({...item,islemler:list,...(!list.length?{miktar:0,maliyetBirim:0,alisKomisyon:0}:{})});detailModal(id);toast('İşlem silindi; toplamlar güncellendi.');
    }
    else if(a==='auto-cost'){const item=assetById(id);commitAsset({...item,maliyetManuel:false});detailModal(id);}
    else if(a==='delete-asset'){
      if(!confirm('Bu varlık ve işlem geçmişi silinecek. Gerçek banka hesabınız etkilenmez. Devam edilsin mi?'))return;
      commit({...state,varliklar:state.varliklar.filter(x=>x.id!==id)});modal.close();toast('Varlık silindi.');
    }
    else if(a==='delete-portfolio'){
      portfolioDeleteModal(id);
    }
    else if(a==='confirm-delete-portfolio')deletePortfolio(id);
  else if(a==='close-holding')closeModal(id);
  else if(a==='undo-close')undoCloseModal(id);
  else if(a==='confirm-undo-close')undoClose(id);
    else if(a==='backup')await backup();
    else if(a==='recovery-backup')await recoveryBackup();
    else if(a==='import-backup')importBackup();
    else if(a==='export-csv')await exportCSV();
    else if(a==='import-prices')importPrices();
  }catch(err){toast(err.message,true);}
});
document.addEventListener('change',e=>{if(e.target.id==='scope'){pid=e.target.value;render();}if(e.target.id==='asset-search'){search=e.target.value;render();$('#asset-search').focus();}});
window.addEventListener('storage',e=>{if(e.key===STORAGE){toast('Başka bir sekmede veri değişti. Çakışmayı önlemek için bu sayfayı yenile.',true);loadError='Başka sekmede kayıt değişti. Devam etmek için sayfayı yenile.';refreshEpoch++;render();}});
modal.addEventListener('click',e=>{if(e.target===modal){const r=modal.getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)modal.close();}});
render();
window.PortfoyumAppBack=()=>{
  if(modal.open){modal.close();return true;}
  if(view!=='overview'){view='overview';search='';render();window.scrollTo({top:0});return true;}
  return false;
};
if(isAndroid && state.varliklar.length && !loadError)setTimeout(()=>refresh(),700);
