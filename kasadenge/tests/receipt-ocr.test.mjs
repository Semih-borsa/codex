import test from 'node:test';import assert from 'node:assert/strict';import {parsePos,validatePhotoReview,buildPos} from '../app/src/main/assets/pos.mjs';
// Textual regression only: no customer photograph is bundled or published.
const actualOrdered=`KREDİ KARTI İŞLEMLERİ
YURTİÇi
SATIŞ
IŞLEM ADEDI: 58
TOPLAM İŞLEM TUTARI: 31.328,59 IL
VAKIFBANK
SATIŞ
iŞLEN ADEDI: 19
TOPLAN İSLEH TUTARI: 11.799, 04 L
DEBİT KART iŞLEMLER İ
YURTİçi
SATIŞ
iŞLEM ADEDI: 44
TOPLARM İŞLEM TUTARI: 15.344,81l IL
VAKIFBANK
SATIŞ
iŞLEM ADEDI: 5
TOPLAM 1ŞLEM TUTARI: 1.567,64 TL
GENEL TOPLAM
60.040,08 TL
GÜNSONU TAMAMLANDI
VAKIFBANK`;
test('real receipt OCR spelling and currency artifacts preserve all four exact amounts',()=>{const p=parsePos(actualOrdered);assert.deepEqual(p.amounts,{creditDomestic:3132859,creditOwn:1179904,debitDomestic:1534481,debitOwn:156764});assert.equal(p.sum,6004008);assert.equal(p.total,6004008);assert.equal(p.matched,true);assert.deepEqual(p.missing,[]);});
test('letter-spaced headings and dot cents can be read without relying on exact amount label',()=>{const p=parsePos('K R E D I K A R T I\nY U R T I C I\nSATIS TUTARI: 31.328.59 TL\nGENEL TOPLAM: 31.328,59 TL');assert.equal(p.amounts.creditDomestic,3132859);assert.equal(p.matched,true);});
test('corrupted monetary digits never become a truncated smaller price',()=>{const p=parsePos(actualOrdered.replace('15.344,81l IL','1S.344,81 TL'));assert.equal(p.amounts.debitDomestic,undefined);assert.ok(p.missing.includes('debitDomestic'));assert.equal(p.matched,false);assert.throws(()=>validatePhotoReview(p,{creditDomestic:'31328,59',creditOwn:'11799,04',debitOwn:'1567,64'},'60040,08'),/Tutar Boş/);});
test('photo requires explicit general total and missing printed categories cannot silently become zero',()=>{const p=parsePos('DEBIT KART\nYURTICI\nGENEL TOPLAM: 100,00 TL');assert.throws(()=>validatePhotoReview(p,{},'100'),/Tutar Boş/);assert.throws(()=>validatePhotoReview(p,{debitDomestic:'100'},''),/Genel Toplam/);assert.doesNotThrow(()=>validatePhotoReview(p,{debitDomestic:'100'},'100'));assert.doesNotThrow(()=>validatePhotoReview(null,{},''));});
test('unassigned totals and duplicated categories stay flagged instead of being guessed',()=>{let p=parsePos('TUTAR: 100,00 TL');assert.equal(p.unassigned.length,1);assert.deepEqual(p.amounts,{});p=parsePos(actualOrdered.replace('GENEL TOPLAM','YURTICI\n100,00 TL\nGENEL TOPLAM'));assert.equal(p.amounts.debitDomestic,null);assert.equal(p.matched,false);});
test('refund/commission lines are never added to sale and total mismatch still blocks saving',()=>{const p=parsePos('KREDI KART\nYURTICI\nSATIS 100,00 TL\nKOMISYON: 5,00 TL\nGENEL TOPLAM 100,00 TL');assert.equal(p.amounts.creditDomestic,10000);assert.ok(p.warnings.length);assert.throws(()=>buildPos({id:'cash',name:'Kasa'},'2026-10-01','POS',{creditDomestic:'99'},10000));});
