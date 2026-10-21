import fs from 'node:fs';import path from 'node:path';import http from 'node:http';
const {chromium}=await import(process.env.FINANS_PLAYWRIGHT||'/opt/codex/cua_node/lib/node_modules/playwright-core/index.mjs');
let browser,server,url;const dist=process.env.FINANS_PDF_DIST||'/workspace/finans-upload/finans-takip/dist';
export async function start(){server=http.createServer((req,res)=>{const name=decodeURIComponent((req.url||'/').split('?')[0]),p=path.resolve(dist,'.'+(name==='/'?'/index.html':name));if(!p.startsWith(path.resolve(dist)+path.sep)||!fs.existsSync(p)||!fs.statSync(p).isFile()){res.statusCode=404;res.end();return;}res.setHeader('Content-Type',p.endsWith('.js')||p.endsWith('.mjs')?'text/javascript':p.endsWith('.css')?'text/css':'text/html; charset=utf-8');res.end(fs.readFileSync(p));});await new Promise(r=>server.listen(0,'127.0.0.1',r));url=`http://127.0.0.1:${server.address().port}`;browser=await chromium.launch({executablePath:process.env.FINANS_CHROMIUM||'/usr/bin/chromium',args:['--no-sandbox']});}
export async function stop(){await browser?.close();if(server)await new Promise(r=>server.close(r));}
export async function open({tema='gray',extra={},native=false,width=390}={}){const context=await browser.newContext({viewport:{width,height:844},timezoneId:'Europe/Istanbul'}),page=await context.newPage(),errors=[],requests=[];page.on('pageerror',e=>errors.push(e.message));page.on('request',r=>requests.push(r.url()));page.on('dialog',d=>d.accept());await page.clock.install({time:new Date('2026-10-18T12:00:00+03:00')});await page.clock.setFixedTime(new Date('2026-10-18T12:00:00+03:00'));
 await page.route('https://**/*',r=>r.abort());
 await page.addInitScript(({tema,extra,native})=>{if(!localStorage.getItem('__seeded')){localStorage.setItem('__seeded','1');const seed={ft_schema_version:'1.7.0',ft_islemler:[{id:'income',tip:'gelir',kategori:'Maaş',tutar:20000,tarih:'2026-10-15'}],ft_odemeler:[],ft_kartlar:[],ft_kart_odemeleri:[],ft_krediler:[],ft_taksitler:[],ft_tekrarlayan:[],ft_tema:tema,ft_donem_devir_log:{'2026-9-15':{tutar:0,tarih:'2026-10-15'}},...extra};for(const[k,v]of Object.entries(seed))localStorage.setItem(k,JSON.stringify(v));}
 if(native){window.androidBridge={};window.__aiCalls=[];window.Capacitor={PluginHeaders:[{name:'FinansGemini',methods:['status','saveKey','deleteKey','request','cancel'].map(name=>({name,rtype:'promise'}))}],nativePromise:async(name,method,options)=>{
 if(name!=='FinansGemini')throw Error('Unsupported mock');window.__aiCalls.push({method,options});
 if(method==='status')return {configured:localStorage.getItem('__configured')==='yes'};
 if(method==='saveKey'){localStorage.setItem('__configured','yes');return{configured:true};}
 if(method==='deleteKey'){localStorage.removeItem('__configured');return{};}
 if(method==='cancel')return{};
 if(method==='request'){
  if(options.action==='models')return{status:200,body:JSON.stringify({models:[{name:'models/gemini-3.5-flash-lite',supportedGenerationMethods:['generateContent']}]})};
  const q=localStorage.getItem('__quota');if(q)return{status:429,body:JSON.stringify({error:{message:'secret-must-not-render',status:'RESOURCE_EXHAUSTED'}})};
  const isTest=options.body.includes('bağlantı testidir');const invalid=localStorage.getItem('__invalid');
  const report={surum:1,bolumler:['genel','odemeler','borclar','harcamalar','tasarruf','adimlar','eksikler'].map(id=>({id,paragraflar:['Ayrıntılı örnek '+id+' değerlendirmesi. Veriler hesaplanmış özetten alınmıştır.']}))};
  return{status:200,body:JSON.stringify({candidates:[{finishReason:invalid?'MAX_TOKENS':'STOP',content:{parts:[{text:isTest?'TAMAM':JSON.stringify(report)}]}}]})};
 }
 return{};}};}
 },{tema,extra,native});await page.goto(url);await page.locator('.home-balance-value').waitFor();return{page,context,errors,requests};}
export async function nav(page,name){await page.getByRole('button',{name:'Menüyü aç'}).click();await page.getByRole('button',{name,exact:true}).click();}
export async function finances(page){return page.evaluate(()=>Object.fromEntries(['ft_islemler','ft_odemeler','ft_kartlar','ft_kart_odemeleri','ft_krediler','ft_taksitler','ft_donem_devir_log'].map(k=>[k,localStorage.getItem(k)])));}
