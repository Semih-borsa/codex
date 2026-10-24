import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import vm from 'node:vm';
const source=(await readFile(new URL('../app/src/main/assets/native.mjs',import.meta.url),'utf8')).replace(/^export /gm,'');
function context(enabled=true){
  const sent=[],timers=new Map();let counter=0;
  const native={postMessage:s=>sent.push(JSON.parse(s))};
  const c=vm.createContext({window:enabled?{PortfoyumNative:native}:{},crypto,Map,JSON,Promise,Error,setTimeout:f=>{const id=++counter;timers.set(id,f);return id;},clearTimeout:id=>timers.delete(id)});
  vm.runInContext(source,c);
  return {sent,timers,run:s=>vm.runInContext(s,c),reply:r=>native.onmessage({data:JSON.stringify(r)})};
}
test('Eşzamanlı Android yanıtları doğru istekle eşleşir',async()=>{
  const c=context(),one=c.run("callNative('http',{url:'one'})"),two=c.run("callNative('http',{url:'two'})");
  c.reply({id:c.sent[1].id,ok:true,result:{status:202}});assert.equal((await two).status,202);
  c.reply({id:c.sent[0].id,ok:true,result:{status:200}});assert.equal((await one).status,200);assert.equal(c.timers.size,0);
});
test('HTTP 403 otomatik kaynakta başarılı yanıta dönüştürülmez',async()=>{
  const c=context(),result=c.run("nativeFetch('https://www.tefas.gov.tr/tr/fon-detayli-analiz/ABC',{method:'GET'})");
  assert.equal(c.sent[0].args.method,'GET');assert.equal(c.sent[0].args.body,'');
  c.reply({id:c.sent[0].id,ok:true,result:{status:403,body:''}});
  const r=await result;assert.equal(r.ok,false);assert.equal(r.status,403);
});
test('Yedek başarı bildirimi Android yazmayı tamamlayana kadar bekler',async()=>{
  const c=context(),save=c.run("saveDocument('test.json','{}','application/json')");let done=false;save.then(()=>{done=true;});
  await Promise.resolve();assert.equal(done,false);assert.equal(c.sent[0].action,'saveFile');
  c.reply({id:c.sent[0].id,ok:true,result:{saved:true}});assert.equal((await save).saved,true);
});
test('Android dosya seçimi iptali başarı sayılmaz',async()=>{
  const c=context(),p=c.run("openDocument('.json')");
  c.reply({id:c.sent[0].id,ok:false,error:'Dosya işlemi iptal edildi.'});await assert.rejects(p,/iptal/);
});
test('Android köprüsü yoksa veya süre aşılırsa açık hata verir',async()=>{
  const c=context(false);await assert.rejects(c.run("callNative('http')"),/Android/);
  const d=context(),p=d.run("callNative('http')");[...d.timers.values()][0]();await assert.rejects(p,/zaman aşımına/);
});
