import {windowFor,shift} from './core.mjs';
import {schedule} from './pos.mjs';
export const SHARE_COLORS=['#2671eb','#159b87','#e5a23c','#9872cf','#da6b87','#5b8baf','#6e9c46','#a17a54'];
export function registerShares(entries,records,kind,mode,anchor,cashId=null){
 const w=windowFor(mode,anchor),group=new Map();
 const add=(id,name,value)=>{const r=group.get(id)||{cashId:id,cashName:name,amount:0};r.amount+=value;group.set(id,r);};
 if(kind==='cash'){for(const r of entries)if(r.date>=w.start&&r.date<=w.end&&(!cashId||cashId===r.cashId))add(r.cashId,r.cashName,r.net);}
 else for(const l of schedule(records,w.start,w.end,cashId))add(l.cashId,l.cashName,l.amount);
 const rows=[...group.values()].sort((a,b)=>b.amount-a.amount||a.cashId.localeCompare(b.cashId)),total=rows.reduce((n,r)=>n+Math.max(0,r.amount),0),net=rows.reduce((n,r)=>n+r.amount,0);
 const pending=kind==='pos'?records.filter(r=>r.singleTotal&&!r.lines.length&&(!cashId||r.cashId===cashId)&&[16,21].some(d=>{const day=shift(r.date,d);return day>=w.start&&day<=w.end;})).length:0;
 return {w,total,net,pending,negative:rows.some(r=>r.amount<0),rows:rows.map((r,i)=>({...r,color:SHARE_COLORS[i%SHARE_COLORS.length],pct:total>0&&r.amount>=0?r.amount/total*100:null}))};
}
