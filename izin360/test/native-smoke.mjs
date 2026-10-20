// Run only with a disposable Android emulator and a disposable populated test server.
// adb forward tcp:9222 localabstract:webview_devtools_remote_<app-pid>
// IZIN360_NATIVE_TEST_SERVER=http://127.0.0.1:4323 node test/native-smoke.mjs
import {writeFile,mkdir} from 'node:fs/promises';
const desktop=process.env.IZIN360_NATIVE_TEST_SERVER||'http://127.0.0.1:4323';
const phoneRoot=process.env.IZIN360_NATIVE_PHONE_SERVER||'http://10.0.2.2:4323';
const targets=await(await fetch('http://127.0.0.1:9222/json/list')).json();
const target=targets.find(t=>t.url.startsWith('https://app.izin360.local'));
if(!target)throw Error('İzin360 debug WebView target not found.');
const ws=new WebSocket(target.webSocketDebuggerUrl);await new Promise((resolve,reject)=>{ws.addEventListener('open',resolve,{once:true});ws.addEventListener('error',reject,{once:true});});
let serial=0;const pending=new Map();ws.addEventListener('message',e=>{const msg=JSON.parse(e.data);if(!msg.id)return;const p=pending.get(msg.id);if(!p)return;pending.delete(msg.id);clearTimeout(p.timer);if(msg.error)p.reject(Error(msg.error.message));else p.resolve(msg.result);});
function command(method,params){return new Promise((resolve,reject)=>{const id=++serial;const timer=setTimeout(()=>{pending.delete(id);reject(Error('Android debug command timed out: '+method));},30000);pending.set(id,{resolve,reject,timer});ws.send(JSON.stringify({id,method,params}));});}
async function evaluate(expression){const result=await command('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(result.exceptionDetails)throw Error(result.exceptionDetails.exception?.description||result.exceptionDetails.text);return result.result?.value;}
async function until(expression){for(let i=0;i<40;i++){if(await evaluate(expression))return;await new Promise(r=>setTimeout(r,1000));}throw Error('Expected Android application state was not reached.');}
try{
 await until("document.querySelector('h1')?.textContent==='Genel Bakış'");
 const before=await evaluate('JSON.parse(Android.status())');
 if(before.hasToken&&before.url!==phoneRoot)throw Error('Use a disposable test emulator; another connection is present.');
 const root=await fetch(desktop+'/');const cookie=root.headers.get('set-cookie').split(';')[0];
 const paired=await(await fetch(desktop+'/api/pairing',{headers:{Cookie:cookie}})).json();
 if(!paired.token)throw Error('Test server pairing unavailable.');
 const saved=await evaluate(`JSON.parse(Android.saveConnection(${JSON.stringify(phoneRoot)},${JSON.stringify(paired.token)}))`);
 if(!saved.ok)throw Error('Native secure connection save failed.');
 await evaluate("document.querySelector('.connection').click();undefined");
 await until("document.querySelector('.connection')?.textContent.includes('Güncel')");
 const state=await evaluate("new Promise((resolve,reject)=>{const r=indexedDB.open('izin360',1);r.onsuccess=()=>{const q=r.result.transaction('settings').objectStore('settings').get('state');q.onsuccess=()=>resolve(q.result);q.onerror=()=>reject(q.error);};})");
 if(Object.keys(state.data.employees).length<1)throw Error('Server records were not received by Android.');
 const metadata=await evaluate('JSON.parse(Android.status())');if(!metadata.hasToken||metadata.token)throw Error('Native credential status invalid.');
 const first=Object.values(state.data.employees).find(e=>!e.archived);
 await evaluate(`Array.from(document.querySelectorAll('tbody tr')).find(tr=>tr.textContent.includes(${JSON.stringify(first.name)})).querySelector('button').click();undefined`);
 await until("Boolean(document.querySelector('dialog[open]'))");
 await evaluate("(()=>{const label=Array.from(document.querySelectorAll('dialog label.field')).find(l=>l.querySelector('span')?.textContent==='Not');const input=label.querySelector('input');Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(input,'Android gerçek eşitleme testi');input.dispatchEvent(new Event('input',{bubbles:true}));Array.from(document.querySelectorAll('dialog button')).find(b=>b.textContent.includes('Personeli Kaydet')).click();})()");
 await until("!document.querySelector('dialog[open]')");
 await evaluate("document.querySelector('.connection').click();undefined");
 await until("document.querySelector('.connection')?.textContent.includes('Güncel')");
 const remote=await(await fetch(desktop+'/api/sync',{method:'POST',headers:{Cookie:cookie,'Content-Type':'application/json'},body:'{"changes":[]}'})).json();
 if(remote.records.find(r=>r.table==='employees'&&r.id===first.id)?.value.note!=='Android gerçek eşitleme testi')throw Error('Native Android edit did not reach Windows server.');
 const screenshot=await command('Page.captureScreenshot',{format:'png'});
 const output=process.env.IZIN360_NATIVE_OUTPUT||'/tmp/izin360-native';await mkdir(output,{recursive:true});await writeFile(output+'/android-emulator.png',Buffer.from(screenshot.data,'base64'));await writeFile(output+'/result.json',JSON.stringify({passed:true,employeeCount:Object.keys(state.data.employees).length,checks:['bundled offline UI startup','native URL validation','native encrypted credential save/decrypt','native HTTP sync','SQLite server records received','WebView IndexedDB persistence','actual Android edit reached Windows SQLite server'],userAgent:await evaluate('navigator.userAgent')},null,2));
 console.log('PASS: actual Android WebView startup and native authenticated synchronization.');
}finally{ws.close();}
