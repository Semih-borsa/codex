import fs from 'node:fs';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import {createRequire} from 'node:module';
const require=createRequire('/workspace/finans-upload/finans-takip/package.json');
const parser=require('@babel/parser');
const old=fs.readFileSync('/workspace/phase-b-backup/App.before-phase-b.jsx','utf8');
const now=fs.readFileSync('/workspace/codex/downloads/App.jsx','utf8');
function declarations(s){const ast=parser.parse(s,{sourceType:'module',plugins:['jsx']});return new Map(ast.program.body.map((node,i)=>{const n=node.declaration||node;return [n.id?.name||n.declarations?.[0]?.id?.name||`node-${i}`,s.slice(node.start,node.end)];}));}
const a=declarations(old),b=declarations(now);
for(const [key,value]of [...a])if(key.startsWith('node-')){assert.ok(now.includes(value),'Import/diğer kaynak değişti');a.delete(key);}
const changed=[...a.keys()].filter(n=>a.get(n)!==b.get(n));
const allowed=['kartToplamBorcHesapla','kartBorcDegisimi','kartKaydiniGuncelle','islemKaynagi','yedekVerisiniDogrula','yedekBaglariniDogrula','yedegiGuvenleYaz','Ozet','donemSonuGiderTahmini','donemBakiyeHesapla','IslemlerTab','AyarlarTab','App'];
assert.deepEqual(changed.filter(n=>!allowed.includes(n)),[]);
const protectedNames=['gunlukFinansHareketleri','gunlukGiderHareketleri','krediOdemeKaydiMi','KredilerTab','useLocalStorage','donemDevriHesapla','kartAnalizHareketleri','analizIslemleri','yedekKapsami','yedekEtkiHesapla','yedekGuvenlikKopyasiOlustur','YedekOnizleme','yerelYedekPaketi','EsnekRapor','YillikKategoriKiyas'];
for(const n of protectedNames)assert.equal(a.get(n),b.get(n),n+' değişti');
const style=s=>[...s.matchAll(/<style[^>]*>[\s\S]*?<\/style>/g)].map(m=>m[0]);assert.deepEqual(style(old),style(now));
const hash=s=>crypto.createHash('sha256').update(s).digest('hex');
const paths=['/workspace/codex/downloads/App.jsx','/workspace/codex/downloads/finans-takip-bildirim/src/App.jsx','/workspace/finans-upload/finans-takip/src/App.jsx'];
const copies=paths.map(path=>({path,sha256:hash(fs.readFileSync(path))}));assert.equal(new Set(copies.map(p=>p.sha256)).size,1);
console.log(JSON.stringify({beforeSha256:hash(old),changedExistingDeclarations:changed,newHelpers:[...b.keys()].filter(n=>!a.has(n)&&!n.startsWith('node-')),protectedUnchanged:protectedNames,stylesUnchanged:true,copies},null,2));
