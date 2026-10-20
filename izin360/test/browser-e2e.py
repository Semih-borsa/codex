"""Run against an isolated test server; never use a personal data directory.
Usage: IZIN360_E2E_URL=http://127.0.0.1:4310 python3 test/browser-e2e.py
Requires Playwright and Chromium. The fixture contains synthetic sample records.
"""
import os,json,pathlib,urllib.request,urllib.error
from playwright.sync_api import sync_playwright,expect

ROOT=os.environ.get('IZIN360_E2E_URL','http://127.0.0.1:4310')
OUT=pathlib.Path(os.environ.get('IZIN360_E2E_OUTPUT','/tmp/izin360-e2e'));OUT.mkdir(parents=True,exist_ok=True)
def navigate(page,name):
    page.locator('.sidebar').get_by_role('button',name=name,exact=True).click()
def dialog(page):return page.get_by_role('dialog')
def saved(page):expect(dialog(page)).not_to_be_visible();expect(page.get_by_role('status')).to_contain_text('Kayıt kaydedildi')
def read_state(page):return page.evaluate("""() => new Promise((resolve,reject)=>{const r=indexedDB.open('izin360',1);r.onsuccess=()=>{const q=r.result.transaction('settings').objectStore('settings').get('state');q.onsuccess=()=>resolve(q.result);q.onerror=()=>reject(q.error);};})""")
def wait_sync(page):
    page.locator('.connection').click()
    expect(page.locator('.connection')).to_contain_text('Güncel',timeout=30000)

with sync_playwright() as p:
    browser=p.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH','/usr/bin/chromium'),headless=True,args=['--no-sandbox'])
    desktop=browser.new_context(viewport={'width':1440,'height':1040},locale='tr-TR',accept_downloads=True)
    page=desktop.new_page();errors=[];page.on('pageerror',lambda e:errors.append(str(e)))
    page.goto(ROOT);expect(page.get_by_role('heading',name='Genel Bakış',exact=True)).to_be_visible()
    wait_sync(page)
    assert not read_state(page)['data']['employees'],'Use a fresh test server/database.'
    page.get_by_role('button',name='Personel Ekle',exact=True).first.click();d=dialog(page)
    expect(d.get_by_label('İzin Hakkı (gün) *')).to_have_value('0')
    d.get_by_label('Ad Soyad *').fill('Ayşe Yılmaz');d.get_by_label('Bölüm',exact=True).fill('Muhasebe');d.get_by_label('İzin Hakkı (gün) *').fill('20');d.get_by_label('Devir (gün)',exact=True).fill('4');d.get_by_label('Kullanılan İzin (gün)').fill('3');d.get_by_role('button',name='Personeli Kaydet').click();saved(page)
    row=page.get_by_role('row').filter(has_text='Ayşe Yılmaz');expect(row).to_contain_text('21 gün')
    row.get_by_role('button',name='Düzenle',exact=True).click();dialog(page).get_by_label('Kullanılan İzin (gün)').fill('8');dialog(page).get_by_role('button',name='Personeli Kaydet').click();saved(page);expect(row).to_contain_text('16 gün')
    page.get_by_role('button',name='İzin Kaydı',exact=True).click();d=dialog(page);d.get_by_label('Başlangıç *').fill('2026-10-20');d.get_by_label('Bitiş *').fill('2026-10-22');d.get_by_role('button',name='Gün Hesapla').click();expect(d.get_by_label('İzin Süresi (gün) *')).to_have_value('3');d.get_by_role('button',name='Kaydet',exact=True).click();saved(page);expect(row).to_contain_text('13 gün')
    navigate(page,'İzin Kayıtları');page.get_by_role('button',name='İzin düzenle',exact=True).click();d=dialog(page);d.get_by_label('İzin Süresi (gün) *').fill('0,5');d.get_by_role('button',name='Kaydet',exact=True).click();saved(page)
    navigate(page,'Genel Bakış');expect(page.get_by_role('row').filter(has_text='Ayşe Yılmaz')).to_contain_text('15,5 gün')
    page.get_by_role('button',name='İzin Bakiyesini Düzelt',exact=True).click();d=dialog(page);d.get_by_label('Düzeltme Yöntemi').select_option('target');d.get_by_label('Olması Gereken Kalan (gün)').fill('12');d.get_by_label('Düzeltme Açıklaması *').fill('Önceki kayıt düzeltmesi');d.get_by_role('button',name='Kaydet',exact=True).click();saved(page);expect(page.get_by_role('row').filter(has_text='Ayşe Yılmaz')).to_contain_text('12 gün')
    navigate(page,'Puantaj');page.get_by_role('button',name='Puantaj Ekle',exact=True).first.click();d=dialog(page);d.get_by_label('Tarih *').fill('2026-10-20');d.get_by_label('Fazla Mesai (dakika)').fill('60');d.get_by_role('button',name='Kaydet',exact=True).click();saved(page);expect(page.locator('tbody')).to_contain_text('480 dk')
    page.get_by_role('button',name='Puantaj Ekle',exact=True).first.click();dialog(page).get_by_role('button',name='Kaydet',exact=True).click();expect(dialog(page).get_by_role('alert')).to_contain_text('ikinci puantaj');dialog(page).get_by_role('button',name='Vazgeç',exact=True).click()
    navigate(page,'Maaş / Bordro');page.get_by_role('button',name='Maaş Kaydı Ekle',exact=True).first.click();d=dialog(page);d.get_by_label('Temel Ücret (₺)').fill('30000');d.get_by_label('Ek Ödeme (₺)').fill('2500,50');d.get_by_label('Kesinti (₺)').fill('500,50');d.get_by_label('Ödenen Tutar (₺)').fill('20000');d.get_by_role('button',name='Kaydet',exact=True).click();saved(page);expect(page.locator('tbody')).to_contain_text('32.000,00');expect(page.locator('tbody')).to_contain_text('12.000,00')
    with page.expect_download() as event:page.get_by_role('button',name='Excel’e Aktar',exact=True).click()
    csv_file=pathlib.Path(event.value.path());assert 'Ayşe Yılmaz' in csv_file.read_text(encoding='utf-8-sig')
    navigate(page,'Ayarlar');page.get_by_label('İşyeri / Kurum Adı').fill('Örnek İşletme');page.get_by_role('button',name='Bilgileri Kaydet').click();expect(page.get_by_role('status')).to_contain_text('Kayıt kaydedildi');wait_sync(page)
    with page.expect_download() as event:page.get_by_role('button',name='Tam Yedek İndir',exact=True).click()
    backup_file=pathlib.Path(event.value.path());exported=json.loads(backup_file.read_text());assert exported['app']=='izin360';assert 'token' not in exported;assert len(exported['data']['employees'])==1
    pairing=desktop.request.get(ROOT+'/api/pairing').json();key=pairing['token']
    # A separate browser storage context plus a native-network bridge emulates the Android UI.
    # This is not a physical-device test of Java/WebView integration.
    phone=browser.new_context(viewport={'width':390,'height':844},locale='tr-TR',is_mobile=True,has_touch=True)
    def native_call(source,request_id,path,method,body):
        req=urllib.request.Request(ROOT+path,data=body.encode() if method=='POST' else None,method=method,headers={'Authorization':'Bearer '+key,'Content-Type':'application/json'})
        try:
            with urllib.request.urlopen(req,timeout=20) as r:return {'status':r.status,'body':json.load(r)}
        except urllib.error.HTTPError as e:return {'status':e.code,'body':json.load(e)}
    phone.expose_binding('_testNativeRequest',native_call)
    phone.add_init_script("""window.Android={status:()=>JSON.stringify({url:location.origin,hasToken:true}),request:(id,path,method,body)=>window._testNativeRequest(id,path,method,body).then(r=>window.__nativeResult(id,r)),saveConnection:()=>'{"ok":true}',clearConnection:()=>{},saveText:()=>{},printPage:()=>{}};""")
    mobile=phone.new_page();mobile.on('pageerror',lambda e:errors.append(str(e)));mobile.goto(ROOT);wait_sync(mobile);expect(mobile.get_by_role('row').filter(has_text='Ayşe Yılmaz')).to_contain_text('12 gün')
    # Disconnect this test phone, then edit on both devices to exercise a real server conflict.
    mobile.evaluate("window._originalRequest=window.Android.request;window.Android.request=(id)=>window.__nativeResult(id,{status:0,body:{error:'Test çevrimdışı'}})")
    mobile.get_by_role('row').filter(has_text='Ayşe Yılmaz').get_by_role('button',name='Düzenle',exact=True).click();dialog(mobile).get_by_label('İzin Hakkı (gün) *').fill('25');dialog(mobile).get_by_role('button',name='Personeli Kaydet').click();saved(mobile)
    navigate(page,'Genel Bakış');page.get_by_role('row').filter(has_text='Ayşe Yılmaz').get_by_role('button',name='Düzenle',exact=True).click();dialog(page).get_by_label('İzin Hakkı (gün) *').fill('30');dialog(page).get_by_role('button',name='Personeli Kaydet').click();saved(page);wait_sync(page)
    mobile.evaluate('window.Android.request=window._originalRequest;void 0');mobile.locator('.connection').click();expect(mobile.get_by_role('button',name='Sürümleri karşılaştır')).to_be_visible();mobile.get_by_role('button',name='Sürümleri karşılaştır').click();dialog(mobile).get_by_role('button',name='Sunucudakini Kullan').click();dialog(mobile).get_by_role('button',name='Formu kapat').click();wait_sync(mobile)
    assert next(iter(read_state(mobile)['data']['employees'].values()))['entitlement']==30000
    # Full restore is confirmed explicitly; then archive and undo using visible controls.
    navigate(page,'Ayarlar');page.locator('input[type=file]').set_input_files(str(backup_file));dialog(page).get_by_role('button',name='Devam Et').click();expect(dialog(page)).not_to_be_visible();expect(page.get_by_role('status')).to_contain_text('Yedek geri yüklendi')
    navigate(page,'Personeller');row=page.get_by_role('row').filter(has_text='Ayşe Yılmaz');row.get_by_role('button',name='Düzenle',exact=True).click();dialog(page).get_by_label('Personeli arşivle').check();dialog(page).get_by_role('button',name='Personeli Kaydet').click();saved(page);expect(page.get_by_role('row').filter(has_text='Ayşe Yılmaz')).to_have_count(0)
    navigate(page,'Değişiklik Geçmişi');page.get_by_role('button',name='Geri Al',exact=True).first.click();dialog(page).get_by_role('button',name='Devam Et').click();expect(dialog(page)).not_to_be_visible();navigate(page,'Personeller');expect(page.get_by_role('row').filter(has_text='Ayşe Yılmaz')).to_have_count(1)
    # Add sample staff only for screenshots; never ship these records with the app.
    for staff,department,right,carry,used in [('Mehmet Kaya','Satış','20','0','12'),('Elif Demir','Muhasebe','20','5','10')]:
        page.get_by_role('button',name='Personel Ekle',exact=True).click();d=dialog(page);d.get_by_label('Ad Soyad *').fill(staff);d.get_by_label('Bölüm',exact=True).fill(department);d.get_by_label('İzin Hakkı (gün) *').fill(right);d.get_by_label('Devir (gün)',exact=True).fill(carry);d.get_by_label('Kullanılan İzin (gün)').fill(used);d.get_by_role('button',name='Personeli Kaydet').click();saved(page)
    navigate(page,'Genel Bakış');wait_sync(page);page.screenshot(path=str(OUT/'windows-overview.png'),full_page=True);wait_sync(mobile);mobile.screenshot(path=str(OUT/'android-overview.png'),full_page=True)
    mobile.get_by_role('button',name='Personel Ekle',exact=True).first.click();expect(dialog(mobile).get_by_label('İzin Hakkı (gün) *')).to_have_value('0');mobile.screenshot(path=str(OUT/'android-new-person.png'),full_page=True)
    assert mobile.evaluate('document.documentElement.scrollWidth<=window.innerWidth'),'Mobile horizontal page overflow'
    page.evaluate('navigator.serviceWorker.ready');desktop.set_offline(True);page.reload();expect(page.get_by_role('heading',name='Genel Bakış',exact=True)).to_be_visible();expect(page.get_by_role('row').filter(has_text='Ayşe Yılmaz')).to_have_count(1);desktop.set_offline(False)
    assert not errors,errors
    (OUT/'result.json').write_text(json.dumps({'passed':True,'checks':['zero entitlement','manual used total','inclusive days','half-day editing','manual target balance','attendance','duplicate rejection','payroll precision','CSV','backup','two-device synchronization','conflict resolution','restore','archive/undo','responsive layout','offline reload'],'runtime_errors':errors},ensure_ascii=False,indent=2))
    print('PASS: 16 functional browser workflows; desktop + Android-sized view; no JS runtime errors.')
    browser.close()
