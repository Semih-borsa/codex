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
export function analysisInsights(entries,records,cashId,mode,anchor){
 const cash=entries.filter(r=>!cashId||r.cashId===cashId),pos=records.filter(r=>!cashId||r.cashId===cashId),a=cashReport(cash,null,mode,anchor),week=cashReport(cash,null,'week',anchor),s=week.stats;
 const result=[comparison('Nakit · Geçen Hafta Ortalamasına Göre',cash,'net',anchor),{title:'Bu Haftanın Kasa Dengesi',text:s.days?`${s.days} Kayıtlı Günün ${s.shortDays} Günü Açık, ${s.overDays} Günü Fazla, ${s.balancedDays} Günü Dengeli. Gün Durumu Kasaların Günlük Toplam Farkına Göredir. Kasa Bazındaki Açık Toplamı ${fmt(s.shortage)}, Fazla Toplamı ${fmt(s.surplus)}.`:'Bu Hafta Kaydedilmiş Sayım Yok.',tone:s.shortDays?'warning':'neutral'},comparison('POS Satışı · Geçen Hafta Ortalamasına Göre',pos,'total',anchor)];
 const c=a.comparison;
 result.push({title:'Nakit · Önceki Dönemle Karşılaştırma',text:c.pairs?`${c.pairs} Eşleşen Kasa/Gün Kaydında Avans Hariç Sayım ${fmt(Math.abs(c.delta))} ${c.delta<0?'Daha Düşük':c.delta>0?'Daha Yüksek':'İle Aynı'}. Yalnız İki Dönemde De Kaydı Olan Aynı Kasalar Karşılaştırıldı.`:'Önceki Dönemde Eşleşen Kasa/Gün Kaydı Yok; Değişim Hesaplanamadı.',tone:'neutral'});
 const grouped=new Map();for(const r of pos){const k=r.cashId+'_'+r.date,old=grouped.get(k);if(old)old.net+=r.total;else grouped.set(k,{cashId:r.cashId,date:r.date,net:r.total,diff:0,expected:0});}
 const pc=cashReport([...grouped.values()],null,mode,anchor).comparison;
 result.push({title:'POS Satışı · Önceki Dönemle Karşılaştırma',text:pc.pairs?`${pc.pairs} Eşleşen Kasa/Gün Kaydında POS Satışı ${fmt(Math.abs(pc.delta))} ${pc.delta<0?'Daha Düşük':pc.delta>0?'Daha Yüksek':'İle Aynı'}. Bu Karşılaştırma Satış Gününe Göredir; Bankaya Yatışla Karıştırılmaz.`:'Önceki Dönemde Eşleşen POS Kasa/Gün Kaydı Yok; Değişim Hesaplanamadı.',tone:'neutral'});
 const unallocated=pos.filter(r=>r.date>=a.w.start&&r.date<=a.w.end&&r.singleTotal&&!r.lines.length);
 if(unallocated.length)result.push({title:'POS Dağılımı Eksik',text:`${unallocated.length} Raporda Toplam ${fmt(total(unallocated,'total'))} İçin Debit/Kredi Dağılımı Bekleniyor. POS Satışına Dahil; Yatış Takvimine Dağılım Bilinmeden Tutar Eklenmez.`,tone:'warning'});
 return result;
}
