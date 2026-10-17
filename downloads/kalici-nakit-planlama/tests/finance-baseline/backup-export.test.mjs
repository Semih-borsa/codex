import test from 'node:test';
import assert from 'node:assert/strict';
import {createHarness} from './source-harness.mjs';
const json=JSON.stringify({format:'finora-v2',data:{ft_islemler:[{id:'i',tutar:100,aciklama:'Öykü ₺'}]}}),name='finora-yedek-2026-10-17.json';
const native=(kaydet)=>({native:true,platform:'android',kaydet});
test('EX01 — Android save ancak doğrulanmış gerçek bayt sayısıyla başarılı olur',async()=>{
 const h=createHarness(),s=h.storage.snapshot();let payload;
 const r=await h.call('yedekDosyasiniAktar',json,name,native(async p=>{payload=p;return {status:'saved',verified:true,bytes:Buffer.byteLength(json)};}));
 assert.equal(r.durum,'kaydedildi');assert.equal(payload.json,json);assert.equal(payload.fileName,name);assert.deepEqual(h.storage.snapshot(),s);
});
test('EX02 — Android iptali başarı değildir ve indirme fallback’i çalışmaz',async()=>{
 const h=createHarness();let download=false;h.context.document.createElement=()=>{download=true;throw Error();};
 const r=await h.call('yedekDosyasiniAktar',json,name,native(async()=>({status:'cancelled'})));
 assert.equal(r.durum,'iptal');assert.equal(download,false);
});
for(const result of [{status:'saved',verified:false,bytes:Buffer.byteLength(json)},{status:'saved',verified:true,bytes:1},{status:'opened'},{},null])test('EX03 — Kanıtsız native sonuç başarı değildir: '+JSON.stringify(result),async()=>{
 const h=createHarness();await assert.rejects(h.call('yedekDosyasiniAktar',json,name,native(async()=>result)),/doğrulanamadı/);
});
test('EX04 — Native yazma hatası kullanıcı akışına ulaşır, Blob fallback yok',async()=>{
 const h=createHarness();await assert.rejects(h.call('yedekDosyasiniAktar',json,name,native(async()=>{throw Error('Dosya yazılamadı');})),/Dosya yazılamadı/);
});
test('EX05 — Eski APK köprüsü yoksa açık hata; sahte başarı yok',async()=>{
 const h=createHarness();h.context.Capacitor={isPluginAvailable:()=>false};await assert.rejects(h.call('yedekDosyasiniAktar',json,name,{native:true,platform:'android'}),/güncel Android APK/);
});
test('EX06 — Tanınmayan native platformda web fallback’e sessizce düşülmez',async()=>{
 const h=createHarness();await assert.rejects(h.call('yedekDosyasiniAktar',json,name,{native:true,platform:'ios'}),/desteği bulunamadı/);
});
test('EX07 — Tarayıcı indirme başlatma hatası yutulmaz',async()=>{
 const h=createHarness();h.context.document.body={appendChild(){}};h.context.document.createElement=()=>({style:{},click(){throw Error('indirme engellendi');},remove(){}});
 await assert.rejects(h.call('yedekDosyasiniAktar',json,name,{native:false}),/indirme engellendi/);
});
test('EX08 — Native dosya seçicinin açılamaması başarı değildir',async()=>{
 const h=createHarness();await assert.rejects(h.call('yedekDosyasiniAktar',json,name,native(async()=>{throw Error('Sistem dosya kaydetme ekranı açılamadı');})),/ekranı açılamadı/);
});
test('EX09 — Native doğrulama hatası finansal kayıtlara yazmaz',async()=>{
 const h=createHarness({storage:{ft_islemler:JSON.stringify([{id:'i',tutar:20000}])}}),before=h.storage.snapshot();
 await assert.rejects(h.call('yedekDosyasiniAktar',json,name,native(async()=>({status:'saved',verified:false}))),/doğrulanamadı/);assert.deepEqual(h.storage.snapshot(),before);
});
test('EX10 — Normal tarayıcı Blob isteği tamamlanmış kaydetme diye sunulmaz',async()=>{
 const h=createHarness();h.context.document.body={appendChild(){}};h.context.document.createElement=()=>({style:{},click(){},remove(){}});
 const r=await h.call('yedekDosyasiniAktar',json,name,{native:false});assert.equal(r.durum,'istek');assert.match(r.metin,/doğrulanamıyor/);
});
