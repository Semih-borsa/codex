import fs from 'node:fs';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import {createRequire} from 'node:module';
const require=createRequire('/workspace/finans-upload/finans-takip/package.json');
const parser=require('@babel/parser');
const priorPath='/workspace/phase-c-backup/App.before-phase-c.jsx';
const path='/workspace/codex/downloads/App.jsx';
const old=fs.readFileSync(priorPath,'utf8'),now=fs.readFileSync(path,'utf8');
const parse=s=>parser.parse(s,{sourceType:'module',plugins:['jsx']});
function map(s){return new Map(parse(s).program.body.map((node,i)=>{const n=node.declaration||node;return [n.id?.name||n.declarations?.[0]?.id?.name||`node-${i}`,s.slice(node.start,node.end)];}));}
const a=map(old),b=map(now);
const allowed=['yerelYedekPaketi','YedekOnizleme','yedekVerisiniDogrula','yedegiGuvenleYaz','AyarlarTab'];
// Key unnamed imports by their source, to avoid index shifts after helper additions.
for(const [k,v] of [...a])if(k.startsWith('node-')){assert.ok(now.includes(v),'Değişmiş kapsam dışı kaynak: '+k);a.delete(k);}
const changed=[...a.keys()].filter(k=>a.get(k)!==b.get(k));
assert.deepEqual(changed.filter(k=>!allowed.includes(k)),[]);
const styles=s=>[...s.matchAll(/<style[^>]*>[\s\S]*?<\/style>/g)].map(m=>m[0]);
assert.deepEqual(styles(now),styles(old));
const hash=s=>crypto.createHash('sha256').update(s).digest('hex');
const paths=[path,'/workspace/codex/downloads/finans-takip-bildirim/src/App.jsx','/workspace/finans-upload/finans-takip/src/App.jsx'];
const copies=paths.map(path=>({path,sha256:hash(fs.readFileSync(path))}));assert.equal(new Set(copies.map(p=>p.sha256)).size,1);
const finance=['krediOdemeKaydiMi','gunlukFinansHareketleri','donemBakiyeHesapla','donemSonuGiderTahmini','KredilerTab','Ozet','AnalizTab','donemDevriHesapla','useLocalStorage'];
for(const name of finance)assert.equal(a.get(name),b.get(name),name+' değişti');
console.log(JSON.stringify({priorSha256:hash(old),changedExistingDeclarations:changed,newHelpers:[...b.keys()].filter(k=>!a.has(k)&&!k.startsWith('node-')),financeAndStageAUnchanged:finance,stylesUnchanged:true,copies},null,2));
