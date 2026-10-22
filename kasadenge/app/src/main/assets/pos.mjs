import {money,shift,key,fmt} from './core.mjs';
export const TYPES=[['creditDomestic','Yurt içi kredi kartı',21],['creditOwn','VakıfBank kredi kartı',21],['world','World puan satış',21],['creditForeign','Yurt dışı kredi kartı',21],['debitForeign','Yurt dışı debit kart',21],['debitDomestic','Yurt içi debit kart',15],['debitOwn','VakıfBank debit kart',15]];
export function schedule(records,start,end,cashId){return records.filter(r=>!cashId||r.cashId===cashId).flatMap(r=>r.lines.map(l=>({...l,cashId:r.cashId,cashName:r.cashName,terminal:r.terminal,date:r.date,id:r.id}))).filter(l=>l.due>=start&&l.due<=end).sort((a,b)=>a.due.localeCompare(b.due)||a.cashName.localeCompare(b.cashName));}
export function parsePos(text){
 const amounts={},warnings=[],seenTypes=new Set(),unassigned=[];
 let section='',sub='',total=null,totalPending=false;
 const fold=s=>s.toLocaleUpperCase('tr-TR').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/İ/g,'I');
 const type=()=>sub==='world'?'world':section&&sub?section+sub:null;
 const add=(kind,value)=>{if(Object.hasOwn?Object.hasOwn(amounts,kind):Object.prototype.hasOwnProperty.call(amounts,kind)){amounts[kind]=null;warnings.push('Tekrarlı Kategori · Tutarı Kontrol Edin: '+TYPES.find(t=>t[0]===kind)[1]);}else amounts[kind]=value;};
 for(const raw of String(text||'').split(/\r?\n/)){
  const s=fold(raw),compact=s.replace(/[^A-Z0-9]/g,''),label=compact.replace(/1/g,'I').replace(/0/g,'O');
  const isGeneral=/GENELTOPLAM|GRANDTOTAL/.test(label);
  const isCredit=/KRED[IL]*KART/.test(label),isDebit=/DEB[ITL]*KART/.test(label);
  if(isGeneral){totalPending=true;sub='';}
  else if(isDebit){section='debit';sub='';totalPending=false;}
  else if(isCredit){section='credit';sub='';totalPending=false;}
  if(!isGeneral){if(/WORLD|PUAN/.test(label))sub='world';else if(/YURTDIS/.test(label))sub='Foreign';else if(/YURTIC/.test(label))sub='Domestic';else if(/VAKIF/.test(label))sub='Own';}
  const kind=type();if(kind&&TYPES.some(t=>t[0]===kind))seenTypes.add(kind);
  // Require a complete currency token. Never read the tail of a corrupted number as a price.
  const found=[];const re=/(?:^|[\s:;₺])([0-9]+(?:[. ]\d{3})*[,.]\s*\d{2})(?=\s*(?:[TIL1|]{1,3}|TRY|₺)?(?:\s|$))/g;let m;
  while((m=re.exec(s))!==null){try{let token=m[1].replace(/\s/g,'');const i=Math.max(token.lastIndexOf(','),token.lastIndexOf('.'));const numeric=token.slice(0,i).replace(/[.,]/g,'')+','+token.slice(i+1);found.push({value:money(numeric),raw:m[1]});}catch{}}
  if(!found.length)continue;
  if(/KOMISYON|IADE|IPTAL|BAKIYE/.test(label)){warnings.push('İade / İptal / Kesinti Satırı Otomatik Eklenmedi: '+raw);continue;}
  if(found.length!==1){warnings.push('Aynı Satırda Birden Fazla Tutar Var: '+raw);unassigned.push(raw);continue;}
  if(totalPending){if(total!==null){total=null;warnings.push('Birden Fazla Genel Toplam Okundu.');}else total=found[0].value;totalPending=false;sub='';section='';continue;}
  if(kind&&TYPES.some(t=>t[0]===kind))add(kind,found[0].value);
  else{unassigned.push(raw);warnings.push('Kart Türü Belirlenemeyen Tutar: '+found[0].raw);}
 }
 const missing=[...seenTypes].filter(t=>amounts[t]===undefined||amounts[t]===null);
 const sum=Object.values(amounts).reduce((s,v)=>s+(v||0),0),matched=total!==null&&!missing.length&&!unassigned.length&&sum===total;
 return {amounts,total,warnings,seenTypes:[...seenTypes],missing,unassigned,sum,matched};
}
export function buildPos(cash,date,terminal,values,receiptTotal=null){if(!/^\d{4}-\d{2}-\d{2}$/.test(date)||key(new Date(date+'T12:00:00'))!==date||date>key())throw Error('İşlem tarihi bugün veya geçmiş bir gün olmalı.');if(!terminal.trim()||terminal.length>60)throw Error('POS/terminal adı gerekli (en fazla 60 karakter).');const lines=TYPES.map(([type,label,delay])=>({type,label,delay,amount:money(values[type]||'0'),due:shift(date,delay)})).filter(l=>l.amount>0);if(!lines.length)throw Error('En az bir işlem tutarı girin.');const total=lines.reduce((s,l)=>s+l.amount,0);if(!Number.isSafeInteger(total)||total>9000000000000)throw Error('Toplam sınırı aşıldı.');if(receiptTotal!==null&&receiptTotal!==total)throw Error('Kategori toplamı '+fmt(total)+'; fiş genel toplamı '+fmt(receiptTotal)+'. Tutarları kontrol edin.');return {cashId:cash.id,cashName:cash.name,date,terminal:terminal.trim(),lines,total};}

export function validatePhotoReview(read,values,totalText){if(!read)return;if(!String(totalText||'').trim())throw Error('Fiş Genel Toplamı Okunamadı. Fotoğraftaki Genel Toplamı Girin Veya Daha Net Fotoğraf Seçin.');for(const type of read.seenTypes||[])if(!String(values[type]??'').trim())throw Error(TYPES.find(t=>t[0]===type)[1]+': Fişte Bu Bölüm Var Ama Tutar Boş. Tutarı Kontrol Edin; Gerçekten Sıfırsa 0 Girin.');}
