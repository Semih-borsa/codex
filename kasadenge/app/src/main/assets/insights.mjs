import {fmt,windowFor,shift} from './core.mjs';
import {cashReport} from './reports.mjs';
const total=(rows,key)=>rows.reduce((n,r)=>n+r[key],0);
const scope=rows=>[...new Set(rows.map(r=>r.cashId))].sort().join('|');
function comparison(label,rows,field,anchor){
 const today=rows.filter(r=>r.date===anchor),week=windowFor('week',anchor),start=shift(week.start,-7),end=shift(week.start,-1);
 if(!today.length)return {title:label,text:'Seçilen Gün İçin Kaydedilmiş Veri Yok. Taslaklar Yoruma Katılmaz.',tone:'neutral'};
 const prior=rows.filter(r=>r.date>=start&&r.date<=end),days=[...new Set(prior.map(r=>r.date))].filter(day=>scope(prior.filter(r=>r.date===day))===scope(today));
 const value=total(today,field);
 if(!days.length)return {title:label,text:`Seçilen Gün ${fmt(value)}. Geçen Hafta Aynı Kasa Kapsamında Karşılaştırılabilir Gün Yok.`,tone:'neutral'};
 const avg=days.reduce((n,day)=>n+total(prior.filter(r=>r.date===day),field),0)/days.length,delta=Math.round(value-avg),pct=avg>0?Math.abs(delta)/avg*100:null;
 return {title:label,text:`Seçilen Gün ${fmt(value)}; Geçen Haftanın ${days.length} Kayıtlı Gün Ortalaması ${fmt(Math.round(avg))}. ${delta===0?'Ortalama İle Aynı.':`${fmt(Math.abs(delta))}${pct===null?'':` (%${pct.toLocaleString('tr-TR',{maximumFractionDigits:1})})`} ${delta<0?'Altında':'Üstünde'}.`} ${days.length<3?'Veri Az; Bu Fark Bir Eğilim Olarak Yorumlanmamalı.':''}`,tone:'neutral'};
}
export function analysisInsights(entries,records,cashId,mode,anchor,boxes=[]){
 const cash=entries.filter(r=>!cashId||r.cashId===cashId),pos=records.filter(r=>!cashId||r.cashId===cashId),a=cashReport(cash,null,mode,anchor),week=cashReport(cash,null,'week',anchor),s=week.stats;
 const result=[comparison('Nakit · Geçen Hafta Ortalamasına Göre',cash,'net',anchor),{title:'Bu Haftanın Kasa Dengesi',text:s.days?`${s.days} Kayıtlı Günün ${s.shortDays} Günü Açık, ${s.overDays} Günü Fazla, ${s.balancedDays} Günü Dengeli. Gün Durumu Kasaların Günlük Toplam Farkına Göredir. Kasa Bazındaki Açık Toplamı ${fmt(s.shortage)}, Fazla Toplamı ${fmt(s.surplus)}.`:'Bu Hafta Kaydedilmiş Sayım Yok.',tone:s.shortDays?'warning':'neutral'},comparison('POS Satışı · Geçen Hafta Ortalamasına Göre',pos,'total',anchor)];
 const c=a.comparison;
 result.push({title:'Nakit · Önceki Dönemle Karşılaştırma',text:c.pairs?`${c.pairs} Eşleşen Kasa/Gün Kaydında Avans Hariç Sayım ${fmt(Math.abs(c.delta))} ${c.delta<0?'Daha Düşük':c.delta>0?'Daha Yüksek':'İle Aynı'}. Yalnız İki Dönemde De Kaydı Olan Aynı Kasalar Karşılaştırıldı.`:'Önceki Dönemde Eşleşen Kasa/Gün Kaydı Yok; Değişim Hesaplanamadı.',tone:'neutral'});
 const grouped=new Map();for(const r of pos){const k=r.cashId+'_'+r.date,old=grouped.get(k);if(old)old.net+=r.total;else grouped.set(k,{cashId:r.cashId,date:r.date,net:r.total,diff:0,expected:0});}
 const pc=cashReport([...grouped.values()],null,mode,anchor).comparison;
 result.push({title:'POS Satışı · Önceki Dönemle Karşılaştırma',text:pc.pairs?`${pc.pairs} Eşleşen Kasa/Gün Kaydında POS Satışı ${fmt(Math.abs(pc.delta))} ${pc.delta<0?'Daha Düşük':pc.delta>0?'Daha Yüksek':'İle Aynı'}. Bu Karşılaştırma Satış Gününe Göredir; Bankaya Yatışla Karıştırılmaz.`:'Önceki Dönemde Eşleşen POS Kasa/Gün Kaydı Yok; Değişim Hesaplanamadı.',tone:'neutral'});
 result.push(...registerInsights(cash,'net','Nakit',mode,anchor,boxes),...registerInsights(pos,'total','POS',mode,anchor,boxes));
 const unallocated=pos.filter(r=>r.date>=a.w.start&&r.date<=a.w.end&&r.singleTotal&&!r.lines.length);
 if(unallocated.length)result.push({title:'POS Dağılımı Eksik',text:`${unallocated.length} Raporda Toplam ${fmt(total(unallocated,'total'))} İçin Debit/Kredi Dağılımı Bekleniyor. POS Satışına Dahil; Yatış Takvimine Dağılım Bilinmeden Tutar Eklenmez.`,tone:'warning'});
 return result;
}

export function registerInsights(rows,field,type,mode,anchor,boxes=[]){
 const w=windowFor(mode,anchor),current=rows.filter(r=>r.date>=w.start&&r.date<=w.end),previous=rows.filter(r=>r.date>=w.previousStart&&r.date<=w.previousEnd),ids=[...new Set([...current,...previous].map(r=>r.cashId))],label=({day:'Seçilen Gün',week:'Bu Hafta',lastweek:'Geçen Hafta',month:'Bu Ay',lastmonth:'Geçen Ay'})[mode]||'Seçilen Dönem';
 const name=id=>boxes.find(b=>b.id===id)?.name||rows.find(r=>r.cashId===id&&r.cashName)?.cashName||id;
 const stats=ids.map(id=>{const now=current.filter(r=>r.cashId===id),old=previous.filter(r=>r.cashId===id);return {id,name:name(id),now:total(now,field),before:total(old,field),days:new Set(now.map(r=>r.date)).size,priorDays:new Set(old.map(r=>r.date)).size};}).sort((a,b)=>b.now-a.now||a.name.localeCompare(b.name,'tr'));
 if(!stats.length)return [];
 const recorded=stats.filter(r=>r.days),best=recorded.length?Math.max(...recorded.map(r=>r.now)):null,leaders=recorded.filter(r=>r.now===best);
 const result=[];
 if(leaders.length)result.push({title:`${type} · En Yüksek Kasa Tutarı`,text:`${label} Kaydedilen Kasalar Arasında En Yüksek ${type==='Nakit'?'Avans Hariç Nakit Tutarı':'POS Satışı'} ${leaders.map(r=>r.name).join(', ')}: ${fmt(best)}${leaders.length>1?' (Eşit Tutar).':'.'} ${type==='POS'?'İşlem Gününe Göredir; Bankaya Yatış Tutarı Değildir.':'Sayım Tutarıdır; Satış Kârı Değildir.'}`,tone:'neutral'});
 const lines=stats.map(r=>{if(!r.days)return `${r.name}: Bu Dönemde Kayıt Yok; Önceki Dönem ${fmt(r.before)}. Düşüş Varsayılmadı.`;if(!r.priorDays)return `${r.name}: ${fmt(r.now)}. Önceki Dönemde Kayıt Yok; Yüzde Hesaplanamadı.`;const delta=r.now-r.before,pct=r.before>0?Math.abs(delta)/r.before*100:null;return `${r.name}: ${fmt(r.now)} · Önceki Dönem ${fmt(r.before)}. ${delta===0?'Değişmedi.':pct===null?`${fmt(Math.abs(delta))} ${delta>0?'Arttı':'Azaldı'}; Önceki Tutar Sıfır/Negatif, Yüzde Hesaplanamadı.`:`%${pct.toLocaleString('tr-TR',{minimumFractionDigits:1,maximumFractionDigits:1})} ${delta>0?'Arttı':'Düştü'} (${fmt(Math.abs(delta))}).`} ${r.days} / ${r.priorDays} Kayıtlı Gün${r.days!==r.priorDays?' · Gün Sayısı Farklı; Kısmi Veriyle Karşılaştırma.':'.'}`;});
 result.push({title:`${type} · Kasa Bazında Değişim`,text:lines.join('\n')+`\nKarşılaştırma: ${w.start.split('-').reverse().join('.')}–${w.end.split('-').reverse().join('.')} / ${w.previousStart.split('-').reverse().join('.')}–${w.previousEnd.split('-').reverse().join('.')}. Eksik Günler Sıfır Sayılmaz; Mevcut Kayıtlı Tutarlar Karşılaştırılır.`,tone:'neutral'});
 return result;
}
