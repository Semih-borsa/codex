// Kaynak şeması hazırlığıdır; canlı veri/seri doğrulaması henüz yapılmadı.
import {validateSeries} from './comparison.mjs';
export function parseFredCsv(text,expectedSeries){
  if(typeof text!=='string'||text.length>2_000_000||!text.trim())throw new Error('CSV boş veya çok büyük.');
  if(typeof expectedSeries!=='string'||!/^[A-Z0-9_]+$/.test(expectedSeries))throw new Error('Seri kimliği doğrulanmalı.');
  const lines=text.replace(/^\uFEFF/,'').trim().split(/\r?\n/);
  const cols=line=>line.split(',').map(x=>x.trim().replace(/^"([^"\n]*)"$/,'$1'));
  const header=cols(lines.shift());
  if(header.length!==2||!['DATE','observation_date'].includes(header[0])||header[1]!==expectedSeries)throw new Error('CSV beklenen tarihsel seriye ait değil.');
  const points=[];
  for(const line of lines){
    if(!line.trim())continue;const row=cols(line);
    if(row.length!==2)throw new Error('CSV satırı geçersiz.');
    if(row[1]==='.'||row[1]==='')continue;
    if(!/^(?:\d+(?:\.\d+)?|\.\d+)$/.test(row[1]))throw new Error('CSV değeri geçersiz.');
    points.push({date:row[0],value:Number(row[1])});
  }
  return validateSeries(points);
}
export function assessCoverage(points,requiredDates,{kind='daily'}={}){
  const rows=validateSeries(points,kind),valid=requiredDates.filter(d=>typeof d==='string'),first=rows[0].date,last=rows.at(-1).date;
  return {first,last,requestedCount:requiredDates.length,outsideRange:valid.filter(d=>kind==='monthly'?(d.slice(0,7)<first.slice(0,7)||d.slice(0,7)>last.slice(0,7)):(d<first||d>last)),hasUnknownDates:valid.length!==requiredDates.length};
}
