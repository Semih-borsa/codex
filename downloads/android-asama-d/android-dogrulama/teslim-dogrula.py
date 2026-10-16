from pathlib import Path
import hashlib,json,zipfile
out=Path('/workspace/output');e=out/'android-dogrulama';h=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
entries=[]
for p,t in [(Path('/workspace/finans-upload/finans-takip/android/app/build/outputs/apk/debug/app-debug.apk'),out/'Finans-D-debug-ayni-kimlik.apk'),(Path('/workspace/android-device-validation/android/app/build/outputs/apk/debug/app-debug.apk'),out/'Finans-D-bagimsiz-test.apk')]:
 with zipfile.ZipFile(p) as z:js={n:hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if n.startswith('assets/public/') and n.endswith('.js')}
 entries.append({'derlenen_yol':str(p),'teslim_yolu':str(t),'boyut_bayt':p.stat().st_size,'boyut_MiB':round(p.stat().st_size/1048576,3),'sha256':h(p),'js_varliklari':js});assert h(p)==h(t)
assert entries[0]['js_varliklari']==entries[1]['js_varliklari']
expected=json.loads((e/'uretim-korumasi.json').read_text());assert all(h(p)==v for p,v in expected.items())
assert h(out/'App.jsx')==h('/workspace/codex/downloads/App.jsx')==h('/workspace/finans-upload/finans-takip/src/App.jsx')
dist=Path('/workspace/finans-upload/finans-takip/dist');public=[dist/'index.html',*[p for p in (dist/'assets').rglob('*') if p.is_file() and not any(a.startswith('.') for a in p.relative_to(dist).parts)]]
for a in entries:
 with zipfile.ZipFile(a['derlenen_yol']) as z:
  for p in public:assert hashlib.sha256(z.read('assets/public/'+str(p.relative_to(dist)))).hexdigest()==h(p)
(e/'apk-kanitlari.json').write_text(json.dumps({'kaynak_sha256':h(out/'App.jsx'),'uretim_dosyalarinin_hashleri_degismedi':True,'iki_apk_web_js_birebir_ayni':True,'index_ve_dist_assets_apk_ile_ayni':True,'apklar':entries},ensure_ascii=False,indent=2)+'\n')
for d in entries:print(d['teslim_yolu'], d['boyut_bayt'], d['boyut_MiB'], d['sha256'])
