import test from 'node:test';import assert from 'node:assert/strict';import {parsePos,buildPos,schedule} from '../app/src/main/assets/pos.mjs';
const receipt=`KREDİ KARTI İŞLEMLERİ
YURTİÇİ
SATIŞ
TOPLAM İŞLEM TUTARI:
31.328,59 TL
VAKIFBANK
SATIŞ
TOPLAM İŞLEM TUTARI: 11.799,04 TL
DEBİT KART İŞLEMLERİ
YURTİÇİ SATIŞ
TOPLAM İŞLEM TUTARI: 15.344,81 TL
VAKIFBANK SATIŞ
TOPLAM İŞLEM TUTARI: 1.567,64 TL
GENEL TOPLAM
60.040,08 TL`;
test('uploaded receipt categories total 60040.08 with general total excluded',()=>{const p=parsePos(receipt);assert.deepEqual(p.amounts,{creditDomestic:3132859,creditOwn:1179904,debitDomestic:1534481,debitOwn:156764});assert.equal(p.total,6004008);assert.deepEqual(p.warnings,[]);});
const cash={id:'cash1',name:'Kasa 1'};
test('all credit/World/foreign debit +21, domestic debit +15 across month boundary',()=>{let r=buildPos(cash,'2026-10-22','POS',{creditDomestic:'43127,63',debitDomestic:'16912,45',world:'1',debitForeign:'2'},6004308);assert.equal(r.lines.find(l=>l.type==='creditDomestic').due,'2026-11-12');assert.equal(r.lines.find(l=>l.type==='debitDomestic').due,'2026-11-06');assert.ok(r.lines.filter(l=>['world','debitForeign'].includes(l.type)).every(l=>l.due==='2026-11-12'));assert.equal(buildPos(cash,'2026-10-21','POS',{world:'1'}).lines[0].due,'2026-11-11');assert.equal(buildPos(cash,'2024-02-09','POS',{world:'1'}).lines[0].due,'2024-03-01');});
test('invalid/mismatched receipt cannot be submitted',()=>{assert.throws(()=>buildPos(cash,'2026-10-21','POS',{world:'10'},999));assert.throws(()=>buildPos(cash,'2026-02-30','POS',{world:'10'}));assert.throws(()=>buildPos(cash,'2026-10-21','POS',{}));});
test('range inclusive and filtered by cash, no advance or general total counted',()=>{const a=buildPos(cash,'2026-10-01','POS',{world:'10',debitDomestic:'20'}),b=buildPos({id:'other',name:'Kasa 2'},'2026-10-01','POS',{world:'40'});assert.equal(schedule([a,b],'2026-10-16','2026-10-22','cash1').reduce((n,l)=>n+l.amount,0),3000);assert.equal(schedule([a,b],'2026-10-22','2026-10-22').length,2);});
test('duplicate and unlabeled OCR amounts are not guessed',()=>{const p=parsePos(receipt.replace('GENEL TOPLAM','YURTİÇİ\nTOPLAM İŞLEM TUTARI: 10,00\nGENEL TOPLAM'));assert.equal(p.amounts.debitDomestic,null);assert.ok(p.warnings.length);assert.deepEqual(parsePos('TOPLAM İŞLEM TUTARI: 100,00').amounts,{});});
