// Hazırlık modülü: üretim uygulamasına henüz bağlanmadı.
// Bakiye hesaplamaz; mevcut motorun doğrulanmış sonuçlarını karşılaştırır.
const day=d=>typeof d==='string'&&/^\d{4}-\d{2}-\d{2}$/.test(d)&&Number.isFinite(Date.parse(d))&&new Date(d).toISOString().slice(0,10)===d;
const finite=n=>typeof n==='number'&&Number.isFinite(n);
const round=n=>Math.round((n+Number.EPSILON)*100)/100;
const fail=reason=>({status:'unavailable',reason});
export function validateSeries(points,kind='daily'){
  if(!['daily','monthly'].includes(kind)||!Array.isArray(points)||!points.length)throw new Error('Karşılaştırma serisi bulunamadı.');
  const seen=new Set();
  const rows=points.map(p=>{
    if(!p||!day(p.date)||!finite(p.value)||p.value<=0)throw new Error('Karşılaştırma veri noktası geçersiz.');
    const key=kind==='monthly'?p.date.slice(0,7):p.date;
    if(seen.has(key))throw new Error('Aynı dönem için çelişebilecek birden fazla kayıt var.');seen.add(key);
    return {date:p.date,value:p.value};
  });
  return rows.sort((a,b)=>a.date.localeCompare(b.date));
}
export function referenceAt(points,date,{kind='daily',maxGapDays=7}={}){
  if(!day(date))throw new Error('Karşılaştırma tarihi geçersiz.');
  const rows=validateSeries(points,kind);
  if(kind==='monthly')return rows.find(p=>p.date.slice(0,7)===date.slice(0,7))||null;
  if(!Number.isInteger(maxGapDays)||maxGapDays<0||maxGapDays>31)throw new Error('Veri boşluğu sınırı geçersiz.');
  const found=rows.filter(p=>p.date<=date).at(-1);
  return found&&(Date.parse(date)-Date.parse(found.date))/86400000<=maxGapDays?found:null;
}
export function compareDatedFlows({flows,terminal,points,kind='daily',maxGapDays=7}){
  if(!terminal||!day(terminal.date)||!finite(terminal.value)||terminal.value<0)throw new Error('Mevcut motorun doğrulanmış değer ve tarihi gerekli.');
  if(!Array.isArray(flows)||!flows.length)return fail('Tarihli başlangıç/para hareketi bulunmuyor.');
  const rows=validateSeries(points,kind),ids=new Set();
  const ordered=flows.map(f=>{
    if(!f||typeof f.id!=='string'||!f.id||ids.has(f.id))throw new Error('Hareket kimliği eksik veya tekrarlı.');ids.add(f.id);
    if(!day(f.date)||f.date>terminal.date||!['contribution','withdrawal'].includes(f.type)||!finite(f.amount)||f.amount<=0)throw new Error('Tarihli nakit hareketi geçersiz.');
    return {...f};
  }).sort((a,b)=>a.date.localeCompare(b.date));
  let units=0,netContribution=0;const references=[];
  for(const f of ordered){
    const ref=referenceAt(rows,f.date,{kind,maxGapDays});
    if(!ref)return fail(f.date+' için doğrulanmış referans veri yok.');
    const sign=f.type==='withdrawal'?-1:1;
    units+=sign*f.amount/ref.value;netContribution+=sign*f.amount;
    if(!Number.isFinite(units)||!Number.isFinite(netContribution))return fail('Tutar veya referans değeri güvenilir hesaplama sınırını aşıyor.');
    if(units < -1e-9)return fail('Alternatif yatırımda çekimi karşılayacak birim yok; finansman varsayımı yapılmadı.');
    units=Math.max(0,units);references.push({id:f.id,requestedDate:f.date,usedDate:ref.date,value:ref.value});
  }
  const end=referenceAt(rows,terminal.date,{kind,maxGapDays});
  if(!end)return fail('Bitiş tarihinde doğrulanmış referans veri yok.');
  const rawExpected=units*end.value,expected=round(rawExpected),difference=round(terminal.value-rawExpected);
  if(!Number.isFinite(rawExpected)||!Number.isFinite(expected)||!Number.isFinite(difference))return fail('Karşılaştırma sonucu güvenilir hesaplama sınırını aşıyor.');
  return {status:'ready',actual:round(terminal.value),expected,difference,percent:rawExpected>0?(terminal.value/rawExpected-1)*100:null,netContribution:round(netContribution),actualGain:round(terminal.value-netContribution),referenceGain:round(rawExpected-netContribution),references,endReference:{requestedDate:terminal.date,usedDate:end.date,value:end.value}};
}
export function derivedSpotGramGold(goldUsdPerOunce,usdTry){
  const gold=validateSeries(goldUsdPerOunce),fx=new Map(validateSeries(usdTry).map(p=>[p.date,p.value]));
  // Birimler yalnız aynı tarihli kaynaklarla dönüştürülür. Banka/ziynet fiyatı değildir.
  return gold.filter(p=>fx.has(p.date)).map(p=>({date:p.date,value:p.value*fx.get(p.date)/31.1034768}));
}
