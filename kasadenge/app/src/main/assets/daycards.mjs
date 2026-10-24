import {shift} from './core.mjs';
import {dueByCash} from './reports.mjs';
export function changePercent(current,previous,comparable=true){if(!comparable||previous==null)return null;if(previous<=0)return previous===0&&current===0?0:null;return (current-previous)/previous*100;}
const sameScope=(a,b)=>a.length>0&&b.length>0&&a.map(r=>r.cashId).sort().join('|')===b.map(r=>r.cashId).sort().join('|');
const sum=(rows,k)=>rows.reduce((n,r)=>n+r[k],0);
export function cashDay(entries,day){const rows=entries.filter(r=>r.date===day),old=entries.filter(r=>r.date===shift(day,-7)),map=new Map(old.map(r=>[r.cashId,r]));return {rows:rows.map(r=>({...r,pct:changePercent(r.net,map.get(r.cashId)?.net)})),total:sum(rows,'net'),diff:sum(rows,'diff'),pct:changePercent(sum(rows,'net'),sum(old,'net'),sameScope(rows,old)),scopeMismatch:rows.length>0&&old.length>0&&!sameScope(rows,old)};}
export function posDay(records,day){
 const priorDay=shift(day,-7),rows=dueByCash(records,day),old=dueByCash(records,priorDay),map=new Map(old.map(r=>[r.cashId,r]));
 const pending=(cashId,type)=>records.some(r=>(!cashId||r.cashId===cashId)&&r.singleTotal&&!r.lines.length&&[day,priorDay].some(d=>(type!=='credit'&&shift(r.date,16)===d)||(type!=='debit'&&shift(r.date,21)===d)));
 const reason=(value,previous,comparable,incomplete)=>incomplete?'Dağılım Bekleniyor':!comparable?'Kasa Kapsamı Farklı':previous==null?'Geçen Hafta Kaydı Yok':previous<=0&&!(previous===0&&value===0)?'Önceki Tutar Sıfır Veya Negatif':null;
 function compare(r,p,comparable=true,cashId=null){const changes={},reasons={};for(const k of ['total','credit','debit']){const why=reason(r[k],p?.[k],comparable,pending(cashId,k));reasons[k]=why;changes[k]=changePercent(r[k],p?.[k],!why);}return {changes,reasons};}
 const total=Object.fromEntries(['total','credit','debit'].map(k=>[k,sum(rows,k)])),prior=old.length?Object.fromEntries(['total','credit','debit'].map(k=>[k,sum(old,k)])):null;
 const overall=compare(total,prior,!old.length||sameScope(rows,old));return {rows:rows.map(r=>({...r,...compare(r,map.get(r.cashId),true,r.cashId)})),...total,...overall,incomplete:pending(null,'total'),scopeMismatch:rows.length>0&&old.length>0&&!sameScope(rows,old)};
}
